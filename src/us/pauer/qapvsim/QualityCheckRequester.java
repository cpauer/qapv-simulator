package us.pauer.qapvsim;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;

import javax.swing.JComboBox;

import junit.framework.TestCase;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.DataWriter;
import org.dcm4che2.net.DataWriterAdapter;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.SingleDimseRSP;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
import org.dcm4che2.net.service.CMoveSCP;
import org.dcm4che2.net.service.CStoreSCP;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.NEventReportSCU;
import org.dcm4che2.util.UIDUtils;

import us.pauer.qapvsim.QualityCheckPerformer;
import us.pauer.qapvsim.BaseQualityCheck.connectionInfo;





/* ***** BEGIN LICENSE BLOCK *****
* 
*	
*    Copyright (C) 2012  Chris Pauer, Koua Yang
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
** ***** END LICENSE BLOCK ***** */

public class QualityCheckRequester extends Thread  {

	static CPQCUI ui;
	
	// connection information
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	connectionInfo local = new connectionInfo();
	connectionInfo remote = new connectionInfo();

		

	private ArrayList<String> QcrAction  = new ArrayList<String>(Arrays.asList(new String[]{
			"Echo QCP", 
			"Create QC UPS", 
			"Subscribe to UPS", 
			"Get Progress/Output Info",
			"Get Quality Check Report",
			"Unsubscribe"
			}));
		
	
	// counter for the state to make sure we know where we currently are in the workflow lifecycle
	int stateCounter = 0;
	
	// window title
	static final String WINDOW_TITLE = "Requester";
	static final String DEFAULT_PLAN_STORAGE = "C:/QAPVSIM/POOL/";
	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/QCR/";
	static final String DEFAULT_DCM_TRACK_LOCATION = DEFAULT_PERSIST_LOCATION+"TRAFFIC/";
	
	static Executor executor;
	Device device;
	static NetworkApplicationEntity ae;
	static NetworkApplicationEntity remoteAE;
	String upsUid;
	String qReportUid;
	DicomObject upsOutputSequence;
	
	

	/**
	 * Action Listeners for the button on the UI...so that the response is handled here in the QCR,
	 * where the DICOM management and business logic lives.
	 */
    ActionListener _qcrActionButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        		//turn off button so multiple actions are prevented
        		//  the state engine will set the button to enabled if needed
        		ui.setActionButtonEnabled(false);
        		ui.setActionListEnabled(false);
        		followThroughOnAction(stateCounter);
        }
    };

    ActionListener _qcrActionBoxListener = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		JComboBox cb = (JComboBox)e.getSource();
    		stateCounter = cb.getSelectedIndex();
    	}
    };

    ActionListener _qcrRestButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                stateCounter = 0;
                upsUid = "";
                ui.setActionListEnabled(true);
                ui.setActionListEnabled(true);
        }
    };

    private static class QCRNEventService extends DicomService implements NEventReportSCU {
        private static final String[] sopClasses = { UID.UnifiedProcedureStepEventSOPClass };

        public QCRNEventService() {
            super(sopClasses, null);
        }

		public void neventReport(Association as, int pcid, DicomObject cmd,
				DicomObject data) throws DicomServiceException, IOException {
			
        	ui.setEntryMessage("N-Event");
			ui.setLastMessage("Received N-Event Report...");
        	ui.setCurrentState("Checking report...");
            DicomObject cmdrsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
            int eventType = cmd.getInt(Tag.EventTypeID);
            if (eventType==1) {
            	String state = data.getString(Tag.UnifiedProcedureStepState);
            	ui.setCurrentState("UPS is "+state);
            	ui.setLastMessage("Received update from Check Performer: Step is "+state);
            	if (state.equalsIgnoreCase("COMPLETED") || state.equalsIgnoreCase("CANCELED")) {
        		    	ui.setActionButtonEnabled(true);
        		    	ui.setActionListEnabled(true);
        		    	ui.setLastMessage("Waiting for service request, or action...");
        		    	ui.setCurrentState("Waiting for request or action...");
            	}
            }
            as.writeDimseRSP(pcid, cmdrsp, data);
        	ui.setExitMessage("N-Event");
        }

    }
    private static class QCRCMoveService extends DicomService implements CMoveSCP {

        private static final String[] sopClasses = { UID.StudyRootQueryRetrieveInformationModelMOVE };

        public QCRCMoveService() {
            super(sopClasses, null);
        }

        public void cmove(Association as, int pcid, DicomObject cmd, DicomObject data)
                throws DicomServiceException, IOException {
        	ui.setEntryMessage("C-Move");
        	ui.setLastMessage("Received C-Move Request...");
        	ui.setCurrentState("Received C-Move request...");
            DicomObject cmdrsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
            DimseRSP rsp = doCMove(as, pcid, cmd, data, cmdrsp);
            try {
                rsp.next();
            } catch (InterruptedException e) {
                throw new DicomServiceException(cmd, Status.ProcessingFailure);
            }
            as.writeDimseRSP(pcid, cmdrsp, rsp.getDataset());
            ui.setExitMessage("C-Move");
        }

        protected DimseRSP doCMove(Association as, int pcid, DicomObject cmd,
                DicomObject data, DicomObject cmdrsp) throws DicomServiceException {
        	ui.setCurrentState("Doing C-Move...");
        	ui.setLastMessage("Executing C-Move...");
        	DicomObject rsp = doCStore(cmd, data, cmdrsp, pcid);
            return new SingleDimseRSP(rsp);
        }

		private DicomObject doCStore(DicomObject cmd, DicomObject data,
				DicomObject rsp, int pcid) {
			DicomObject storeObject = null;
			try {
				ui.setEntryMessage("Calling C-Store");
				storeObject = fetchStoreObject(data);
				rsp = cstoreObject(storeObject, cmd, data, pcid);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				rsp = CommandUtils.mkRSP(rsp, CommandUtils.C_MOVE_RSP);
			}
			ui.setExitMessage("Calling C-Store");
			return rsp;
		}

		private DicomObject cstoreObject(DicomObject storeObject, DicomObject cmd, DicomObject data, int pcid) {
			ui.setCurrentState("Formatting Plan C-Store...");
			ui.setLastMessage("Formatting C-Store");
			String sopClassUid = UID.RTPlanStorage;
			String instanceUid = storeObject.getString(Tag.SOPInstanceUID);   //let the scp assign the instance uid
			DataWriter storeWriter = new DataWriterAdapter(storeObject);
			int priority = cmd.getInt(Tag.Priority);
			String moveOriginatorAET = cmd.getString(Tag.MoveDestination);
			int moveOriginatorMsgId = pcid;
			String transferSyntaxUid = UID.ImplicitVRLittleEndian;
			DicomObject storeRsp = null;
		
		    AssociationWithLog assoc = null;
			try {
				assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
				assoc.setLogDirectory(DEFAULT_DCM_TRACK_LOCATION);
				ui.setLastMessage("Sending Plan Object");
				ui.setCurrentState("Sending...");
				DimseRSP rsp = assoc.cstore(sopClassUid, instanceUid, priority, moveOriginatorAET, moveOriginatorMsgId, 
						storeWriter, transferSyntaxUid);
		        while (!rsp.next()){}
		        assoc.release(false);
			} catch (ConfigurationException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			return storeRsp;
		
		}

		private DicomObject fetchStoreObject(DicomObject data) throws IOException {
			String findUID = data.getString(Tag.ReferencedSOPInstanceUID);
			DicomObject returnObject = null;
			File dir = new File(DEFAULT_PLAN_STORAGE);

			String[] children = dir.list();
			if (children == null) {
			    // Either dir does not exist or is not a directory
			} else {
			    for (int i=0; i<children.length; i++) {
			        // Get filename of file or directory
			        String filename = children[i];
			        DicomInputStream inStream = new DicomInputStream(new File(DEFAULT_PLAN_STORAGE+"/"+filename));
			        DicomObject checkObject = inStream.readDicomObject();
			        if (checkObject.getString(Tag.SOPInstanceUID).trim().equalsIgnoreCase(findUID)) {
			        	returnObject = checkObject;
			        	break;
			        }
					inStream.close();
			    }
			}
			return returnObject;
		}
    }

    private static class QCRCStoreService extends DicomService implements CStoreSCP {

        private static final String[] sopClasses = { UID.BasicTextSRStorage };

        public QCRCStoreService() {
            super(sopClasses, null);
        }
		@Override
		public void cstore(Association as, int pcid, DicomObject cmd,
				PDVInputStream dataStream, String tsuid)
				throws DicomServiceException, IOException {
        	ui.setEntryMessage("C-Store");
        	QualityCheckPerformer.ui.setLastMessage("Starting c-store of quality report");
		    DicomObject rsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
		    DicomObject storeObject = dataStream.readDataset();
		    String classUid = storeObject.getString(Tag.SOPClassUID);
		    String instanceUid = storeObject.getString(Tag.SOPInstanceUID);
		    QualityCheckPerformer.ui.setLastMessage("Class type is "+classUid+";  Instance is "+instanceUid);
		    persistReport(storeObject);
		    ui.setLastMessage("******"+storeObject.get)
		    as.writeDimseRSP(pcid, rsp);
        	ui.setExitMessage("C-Store");

		}

		private void persistReport(DicomObject upsObject) {
			try {
				String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
						"/QREPORT."+instanceUid));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
	    }
    }
    

	/*
	 * CONSTRUCTOR
	 */
	public QualityCheckRequester(String localAETitle, String localIP, String localPort, 
			String remoteAETitle, String remoteIP, String remotePort)  
	{	
		ui = new CPQCUI(WINDOW_TITLE, getActionList(), 200);
		ui.setResetButtonListener(_qcrRestButtonListener);
		ui.setActionButtonListener(_qcrActionButtonListener);
		ui.setActionBoxListener(_qcrActionBoxListener);
		
		// Set connection information.
		local.aetitle = localAETitle;
		local.ip = localIP;
		local.port = localPort;
		remote.aetitle = remoteAETitle;
		remote.ip = remoteIP;
		remote.port = remotePort;		
		
		//do initial setting of UI
		initialize();
		updateUI("Waiting for action selection...", "Waiting for action...");
	}

	private void initialize() {
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};
		String name = WINDOW_TITLE;
		device = new Device(name);
		
		executor = new NewThreadExecutor(name);
		ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();
	
		ae.setInstalled(true);
		ae.setAssociationInitiator(true);
		ae.setAssociationAcceptor(true);
		ae.setAETitle(local.aetitle);
		localConn.setHostname(local.ip);
		localConn.setPort(Integer.parseInt(local.port));
		ae.setNetworkConnection(localConn);

		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);

		setServices(ae);
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE,
						DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.RTPlanStorage, DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.RTIonPlanStorage, DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepEventSOPClass, DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.BasicTextSRStorage, DEF_TS, TransferCapability.SCP)
		});  

	
	
		remoteAE = new NetworkApplicationEntity();
		NetworkConnection remoteConn = new NetworkConnection();
		
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname(remote.ip);
		remoteConn.setPort(Integer.parseInt(remote.port));
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle(remote.aetitle);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new QCRCMoveService());
		ae.register(new QCRNEventService());
		ae.register(new QCRCStoreService());
	}
	

	private ArrayList getActionList() {
		ArrayList<String> actions = new ArrayList<String>();
		for (String qcrs:QcrAction)
		{
			actions.add(qcrs);
		}
		return actions;
	}


	private void updateUI(String stateText, String logText) {
		ui.setCurrentState(stateText);
		ui.setLastMessage(logText);
		ui.setVisible(true);
	}
	
	
    private void followThroughOnAction(int stateCounter) {
    	switch (stateCounter) {
    		case 0:
    			ui.setLastMessage("Verification being sent....");
    			echoOnSCP();
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
    		case 1:
    			upsUid = createUPS();
    	        ui.setLastMessage("UPS that was created has UID of "+upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
    		case 2:
    			subscribeToUPSProgressUpdate(upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(false);
    	        ui.setActionListEnabled(false);
    			break;
    		case 3:
    			getUPSOutputInfo(upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
    		case 4:
    			getQualityReport(upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
    		case 5:
    			unsubscribeToUPSProgressUpdate(upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    	        break;
		default:
			break;
		}
    }
	public void run()  {
        try {
			device.startListening(executor);
			updateUI("Starting "+local.aetitle+" server on port "+local.port+"...", "Server starting...");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }



    public void echoOnSCP() 
	{
		//  We now start to try to connect, and do our C-Echo…
		AssociationWithLog assoc = null;

		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			assoc.setLogDirectory(DEFAULT_DCM_TRACK_LOCATION);
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}

		try {
			assoc.cecho().next();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}




	public String createUPS() 
	{
		
		updateUI("Formatting UPS N-CREATE request...","Formatting request");
		String abstractSyntaxUID = UID.UnifiedProcedureStepPushSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String instanceUid = null;   //let the scp assign the instance uid
		DicomObject attrs = setInitialUPSAttributesForNCreate();
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	    DimseRSP rsp = null;
	
	    AssociationWithLog assoc = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			assoc.setLogDirectory(DEFAULT_DCM_TRACK_LOCATION);
			updateUI("Sending UPS N-CREATE", "Sending...");
	        rsp =  assoc.ncreate(abstractSyntaxUID, sopClassUid, instanceUid, attrs,
	                transferSyntaxUid);
			updateUI("Waiting for response on N-Create", "Waiting for response from QCP");
	        while (!rsp.next()){}
	        upsUid = rsp.getCommand().getString(Tag.AffectedSOPInstanceUID);
	        assoc.release(false);
	        updateUI("Response received...", "Response received");
	        return upsUid;
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		return upsUid;
	
	}
		
	

	public void subscribeToUPSProgressUpdate(String uidForSubscription)
	{

		//Start Subscribe association
		ui.setLastMessage("Starting subscription request for UID "+upsUid);
		String abstractSyntaxUID = UID.UnifiedProcedureStepWatchSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
		DicomObject subscribeObject = new BasicDicomObject();
		
		// These are required attributes for the N-Action subscribe as per Annex CC
		subscribeObject.putString(Tag.ReceivingAE, VR.AE, local.aetitle);
		subscribeObject.putString(Tag.DeletionLock, VR.LO, "TRUE");
	    AssociationWithLog assoc = null;
	    DimseRSP rsp = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “3” for the subscribe action…
			rsp = assoc.naction(abstractSyntaxUID, sopClassUid, uidForSubscription,
					3, subscribeObject, transferSyntaxUid); 
			while (!rsp.next()){}
			rsp.getDataset();
			ui.setLastMessage("Subscription request completed");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		}

	}
	
	
	private void getUPSOutputInfo(String upsUid) {
		//Start Subscribe association
		ui.setLastMessage("Starting N-Get of progress / output for UPS "+upsUid);
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
		DicomObject ngetObject = new BasicDicomObject();
		
		// These are required attributes for the N-Action subscribe as per Annex CC
		int[] tags = new int[] {
				Tag.UnifiedProcedureStepProgressInformationSequence,
				Tag.UnifiedProcedureStepPerformedProcedureSequence
		};
	    AssociationWithLog assoc = null;
	    DimseRSP rsp = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “3” for the subscribe action…
			rsp = assoc.nget(sopClassUid, upsUid, tags);
			while (!rsp.next()){}
			DicomObject specifiedSections = rsp.getDataset();
			upsOutputSequence = specifiedSections.getNestedDicomObject(Tag.UnifiedProcedureStepPerformedProcedureSequence);
			upsOutputSequence = upsOutputSequence.getNestedDicomObject(Tag.OutputInformationSequence);
			ui.setLastMessage("Nget request completed");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		}

	}


    private void getQualityReport(String upsUid) {
		updateUI("Formatting request", "Formatting Quality Check Report C-Move request...");
		String sopClassUid = UID.StudyRootQueryRetrieveInformationModelMOVE;
		DicomObject keys;
		keys = setAttributesForReportCMove(upsOutputSequence);
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	
	    AssociationWithLog assoc = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			updateUI("Sending C-Move Request for UID "+
					keys.getString(Tag.ReferencedSOPInstanceUID), "Sending...");
	        DimseRSPHandler rspHandler = new DimseRSPHandler() {
	            @Override
	            public void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
	                QualityCheckRequester.this.onMoveRSP(as, cmd, data);
	            }
	        };
	        assoc.cmove(sopClassUid, 1, keys, transferSyntaxUid, local.aetitle, rspHandler);
	        //rsp =  assoc.cmove(abstractSyntaxUID, sopClassUid, 1, attrs, 
	        //		transferSyntaxUid, local.aetitle);
			updateUI("Waiting for response on C-Move", "Waiting for response from "+remote.aetitle);

	        assoc.waitForDimseRSP();
			updateUI("Response received", "Response received");

		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}

	private void onMoveRSP(Association as, DicomObject cmd,
			DicomObject data) {
		System.out.println("got response");
        /*if (!CommandUtils.isPending(cmd)) {
            moveStatus = cmd.getInt(Tag.Status);
            if (isAbortingMove(moveStatus)) {
                checkError(cmd);
            } else {
                completed += cmd.getInt(Tag.NumberOfCompletedSuboperations);
                warning += cmd.getInt(Tag.NumberOfWarningSuboperations);
                failed += cmd.getInt(Tag.NumberOfFailedSuboperations);
            }

            fireStudyObjectMovedEvent();
        }*/
	}

	
	
	private DicomObject setAttributesForReportCMove(DicomObject oisSequence) {
		DicomObject CMoveObject = new BasicDicomObject();
		CMoveObject.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
		CMoveObject.putString(Tag.StudyInstanceUID, VR.UI, 
				oisSequence.getString(Tag.StudyInstanceUID));
		CMoveObject.putString(Tag.SeriesInstanceUID, VR.UI, 
				oisSequence.getString(Tag.SeriesInstanceUID));
		DicomObject refSopSequence = oisSequence.getNestedDicomObject(Tag.ReferencedSOPSequence);
		CMoveObject.putString(Tag.ReferencedSOPClassUID, VR.UI, 
				refSopSequence.getString(Tag.ReferencedSOPClassUID));
		CMoveObject.putString(Tag.ReferencedSOPInstanceUID, VR.UI, 
				refSopSequence.getString(Tag.ReferencedSOPInstanceUID));

		return CMoveObject;
	}

	public void unsubscribeToUPSProgressUpdate(String uidForUnsubscribe)
	{
		//start Create association
		ui.setLastMessage("Starting unsubscription request for UID "+upsUid);
		String abstractSyntaxUID = UID.UnifiedProcedureStepWatchSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
		DicomObject unSubscribeObject = new BasicDicomObject();

		// These are required attributes for the N-Action subscribe as per Annex CC
		unSubscribeObject.putString(Tag.ReceivingAE, VR.AE, local.aetitle);
	    AssociationWithLog assoc = null;
	    DimseRSP rsp = null;

		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “4” for the unsubscribe action…
			rsp = assoc.naction(abstractSyntaxUID, sopClassUid, uidForUnsubscribe,
					4, unSubscribeObject, transferSyntaxUid); 
			while (!rsp.next()){}
			rsp.getDataset();
			ui.setLastMessage("Unsubscription request finished.");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
		}

	}

	
	
	
	/** Not all attributes as described in table CC.2.5-3 are populated here
	 * @return DicomObject   contains initial set of attributes for UPS
	 */
	
	private DicomObject setInitialUPSAttributesForNCreate() {
		DicomObject returnObject = new BasicDicomObject();
		//UPS Relationship Module Attributes
		returnObject.putString(Tag.PatientName, VR.PN, "Patient^Test");
		returnObject.putString(Tag.PatientID, VR.LO, "MRN0001");
		//UPS Scheduled Procedure Information Module Attributes (no Input Info Sequence yet)
		returnObject.putString(Tag.ScheduledProcedureStepPriority, VR.CS, "HIGH");
		returnObject.putString(Tag.ProcedureStepLabel, VR.LO, "External Device DOSE Verification");
		returnObject.putString(0x00404041, VR.CS, "READY");
		returnObject.putSequence(Tag.ScheduledWorkitemCodeSequence);
		DicomObject workitemSequence = new BasicDicomObject();
		workitemSequence.putString(Tag.CodeValue, VR.SH, "121729");
		workitemSequence.putString(Tag.CodingSchemeDesignator, VR.SH, "DCM");
		workitemSequence.putString(Tag.CodeMeaning, VR.LO, "RT Treatment QA with External Verification");
		returnObject.putNestedDicomObject(Tag.ScheduledWorkitemCodeSequence, workitemSequence);
		returnObject.putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
		DicomObject inputSeq = getInputInfoSequence();
      	if (inputSeq==null) System.out.println("iis is " );		

		returnObject.putSequence(Tag.InputInformationSequence);
		returnObject.putNestedDicomObject(Tag.InputInformationSequence, inputSeq);
		return returnObject;

	}
	
	private DicomObject getInputInfoSequence() {
		DicomObject inputSequence = new BasicDicomObject();
		inputSequence.putString(0x0040E020, VR.CS, "DICOM");
		inputSequence.putString(Tag.StudyInstanceUID, VR.UI, "1.2.34.78");
		inputSequence.putString(Tag.SeriesInstanceUID, VR.UI, "1.2.34.78.1");
		DicomObject sopSequence = new BasicDicomObject();
		sopSequence.putString(Tag.ReferencedSOPClassUID, VR.UI, UID.RTPlanStorage);
		sopSequence.putString(Tag.ReferencedSOPInstanceUID, VR.UI, "1.2.34.56");
		inputSequence.putSequence(Tag.ReferencedSOPSequence);
		inputSequence.putNestedDicomObject(Tag.ReferencedSOPSequence, sopSequence);
		DicomObject retrieveSequence = new BasicDicomObject();
		retrieveSequence.putString(Tag.RetrieveAETitle, VR.AE, local.aetitle);
		inputSequence.putSequence(0x0040E021);
		inputSequence.putNestedDicomObject(0x0040E021, retrieveSequence);
		return inputSequence;
	}
	
}
