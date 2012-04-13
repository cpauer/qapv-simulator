/* ***** BEGIN LICENSE BLOCK *****
* 
*	QualityCheckPerformer is a Service Class Provider implementation of
*   the Quality Check Provider actor of the Quality 
*   Assurance with Plan Veto profile of the IHE-RO*	
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
import java.util.concurrent.Executor;
import java.util.logging.Handler;

import javax.swing.JComboBox;

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
import org.dcm4che2.net.service.NActionSCP;
import org.dcm4che2.net.service.NCreateSCP;
import org.dcm4che2.net.service.NGetSCP;
import org.dcm4che2.util.UIDUtils;





public class QualityCheckPerformer extends Thread {

	static CPQCUI ui;
	
	// connection information
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	 connectionInfo local = new connectionInfo();
	 connectionInfo remote = new connectionInfo();

	
	
		
		
	/**
	 * Enums that document the states that the QCP will go through, listed in happy path order.
	 * Using an Enum will allow us to adhere the action text, button text, log and state messages 
	 * in the UI with the state of the QCP at that time.
	 * @author Chris Pauer
	 *
	 */
	private ArrayList<String> QcpAction = new ArrayList<String>(Arrays.asList(new String[] {
		"Fetch Workitem",
		"Do Quality Check"
	}));
		
	
	// counter for the state to make sure we know where we currently are in the workflow lifecycle
	int stateCounter = 0;

	Device device;
	static Executor executor;
	static NetworkApplicationEntity ae;
	static NetworkApplicationEntity remoteAE;
	NetworkConnection aeConn;

	// window title
	static final String WINDOW_TITLE = "Performer";

	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/QCP/";
	static final String DEFAULT_DCM_TRACK_LOCATION = DEFAULT_PERSIST_LOCATION+"TRAFFIC/";
	static final String CANCELLED_UPS_LOCATION = DEFAULT_PERSIST_LOCATION+"CANCELLED/";
	static final String COMPLETED_UPS_LOCATION = DEFAULT_PERSIST_LOCATION+"COMPLETED/";
	static final String QA_REPORT_LOCATION = DEFAULT_PERSIST_LOCATION+"REPORTS/";
	static final String SUBSCRIBE_FILE_NAME = "SUBSCRIBED.DATA";
	
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	
	static String lastCreatedUid = "";
	boolean subscriptionsExist = false;
	

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };
	
	/**
	 * Action Listeners for the button on the UI...so that the response is handled here in the QCR,
	 * where the DICOM management and business logic lives.
	 */
    ActionListener _qcpActionButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        		//turn off button so multiple actions are prevented
        		//  the state engine will set the button to enabled if needed
    		ui.setActionButtonEnabled(false);
    		ui.setActionListEnabled(false);
    		followThroughOnAction(stateCounter);
        }
    };
    
    ActionListener _qcpActionBoxListener = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		JComboBox cb = (JComboBox)e.getSource();
    		stateCounter = cb.getSelectedIndex();
    	}
    };

    ActionListener _qcpRestButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                stateCounter = -1;
        }
    };
    //some components specific to the performer to put on the ui
	

    private static class QCPVerificationService extends DicomService implements CEchoSCP {

        private static final String[] sopClasses = { UID.VerificationSOPClass };

        public QCPVerificationService() {
            super(sopClasses, null);
        }

        // has to implement this method signature…
        public void cecho(Association as, int pcid, DicomObject cmd)
                throws IOException {
            as.writeDimseRSP(pcid, CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS));
            QualityCheckPerformer.ui.setLastMessage("C-Echo has responded");
        }
    }

    private static class QCPNCreateService extends DicomService implements NCreateSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNCreateService() {
            super(sopClasses, null);
        }

        public void ncreate(Association as, int pcid, DicomObject rq,
		        DicomObject data) throws DicomServiceException, IOException {
        	QualityCheckPerformer.ui.setLastMessage("Starting n-create on server");
		    DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
		    String inputReadiness = data.getString(0x00404041);
		    if (!inputReadiness.equalsIgnoreCase("READY")) {}  // error 
		    String iuid = data.getString(Tag.AffectedSOPInstanceUID);
		    
		    if (iuid == null) {
		        iuid = UIDUtils.createUID();
		        ui.setLastMessage("UID for UPS is "+iuid);
		        rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		        rsp.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		    }
		    as.writeDimseRSP(pcid, rsp, doNCreate(as, pcid, rq, data, rsp));
		}
        
        private DicomObject doNCreate(Association as, int pcid, DicomObject rq,
                DicomObject data, DicomObject rsp) throws IOException {
        	DicomObject UPSObject = createUPS(rq, data);
        	persistUPS(UPSObject);
    		return UPSObject;
            
        }

        private DicomObject createUPS(DicomObject rq, DicomObject data) {
        	DicomObject outObject = new BasicDicomObject();
        	//SOP Common
        	outObject.putString(Tag.SOPClassUID, VR.UI, UID.UnifiedProcedureStepPushSOPClass);
        	
        	outObject.putString(Tag.SOPInstanceUID, VR.UI, rq.getString(Tag.AffectedSOPInstanceUID));
        	String stuff = data.getString(Tag.AffectedSOPInstanceUID);
        	outObject.putDate(Tag.InstanceCreationDate, VR.DA, new Date());
        	outObject.putDate(Tag.InstanceCreationTime, VR.TM, new Date());
        	//add appropriate items
        	outObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "SCHEDULED");
        	outObject.putString(0x00404041, VR.CS, "READY");
        	outObject.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
        	
        	DicomObject progressSeq = new BasicDicomObject();
        	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "0");
        	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Scheduled");
        	outObject.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
    		DicomObject inputSequenceFromRequest = data.getNestedDicomObject(Tag.InputInformationSequence);
          	outObject.putSequence(Tag.InputInformationSequence);
          	outObject.putNestedDicomObject(Tag.InputInformationSequence, inputSequenceFromRequest);
			return outObject;
		}

        
		private void persistUPS(DicomObject upsObject) {
    		try {
    			String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
						"/UPS."+instanceUid+".dcm"));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
				lastCreatedUid = instanceUid;
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

    }

    private static class QCPNActionService extends DicomService implements NActionSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNActionService() {
            super(sopClasses, null);
        }

        public void naction(Association as, int pcid, DicomObject command,
		        DicomObject data) throws DicomServiceException, IOException {
        	QualityCheckPerformer.ui.setLastMessage("Starting n-action on server");
		    DicomObject rsp = CommandUtils.mkRSP(command, CommandUtils.SUCCESS);
		    // capture the incoming uid
		    int actionType = command.getInt(Tag.ActionTypeID);
		    QualityCheckPerformer.ui.setLastMessage("Action id is "+(actionType==3?"Subscribe":"Unsubscribe"));
		    String iuid = command.getString(Tag.RequestedSOPInstanceUID);
		    String receivingAE = data.getString(Tag.ReceivingAE);
		    if (actionType == 3) {
		    	QualityCheckPerformer.ui.setLastMessage("Received subscribe request for uid "+iuid+" from "+receivingAE);
		    	String lockDeletion = data.getString(Tag.DeletionLock);
		    // We don’t implement an actual subscription persistence mechanism here, but you can see
            // we have the data available to do so….
		    	QualityCheckPerformer.ui.setLastMessage("AE "+receivingAE+" is subscribed to SOP Class Instance "
		    		+iuid+" with Delete Lock set to "+lockDeletion);
		    	subscribe(iuid, receivingAE);
		    	ui.setActionButtonEnabled(true);
		    	ui.setActionListEnabled(true);
		    	ui.setLastMessage("Waiting for service request, or action...");
		    	ui.setCurrentState("Waiting for request or action...");
		    } else {
		    	QualityCheckPerformer.ui.setLastMessage("Received unsubscribe request for uid "+iuid+" from "+receivingAE);
		    	QualityCheckPerformer.ui.setLastMessage("AE "+receivingAE+" is unsubscribed to SOP Class Instance "
		    		+iuid);
		    	unsubscribe(iuid, receivingAE);
		    }
		    as.writeDimseRSP(pcid, rsp, data);
		}

		private void subscribe(String uid, String ae) {
			ArrayList<String> subscriptions = new ArrayList();
			try {
			    File subfile = new File(DEFAULT_PERSIST_LOCATION+SUBSCRIBE_FILE_NAME);

			    // Create file if it does not exist
			    boolean success = subfile.createNewFile();
			    if (success) {
			        // File did not exist and was created
			    } else {
			        // File already exists
			    }
			    //read in subscribe persistence
			    BufferedWriter out = new BufferedWriter(new FileWriter(subfile, true));
			    out.write(uid+"$"+ae+"\n");
			    out.newLine();
			    out.close();
			} catch (IOException e) {
			}
		}
		private void unsubscribe(String uid, String ae) {
			ArrayList<String> subscriptions = new ArrayList<String>();
			boolean found = false;
			try {
				BufferedReader in = new BufferedReader(new FileReader(DEFAULT_PERSIST_LOCATION+SUBSCRIBE_FILE_NAME));
				String str;
				while ((str = in.readLine()) != null) {
					if (uid==null) {       //implement unsubscribe all behavior to aid in testing
						found = true;
						if (str.lastIndexOf(ae)==-1) {
							subscriptions.add(str);
						}
					} else {
						if (str.lastIndexOf(uid)!=-1 && str.lastIndexOf(ae)!=-1) {
							found = true;
						} else {
							subscriptions.add(str);
						}
					}
				}
				in.close();
				if (found) {
					boolean success = (new File(DEFAULT_PERSIST_LOCATION+SUBSCRIBE_FILE_NAME)).delete();
					if (!success) {
						throw new IOException("File cannot be written to for subscription");
					}
					BufferedWriter out = new BufferedWriter(new FileWriter(DEFAULT_PERSIST_LOCATION+SUBSCRIBE_FILE_NAME));
					for (String outS:subscriptions) {
						out.write(outS);
					}
					out.close();
				} else {
					//error
				}
			} catch (IOException e) {
				
			}
		}
    }

    
    private static class QCPCStoreService extends DicomService implements CStoreSCP {

        private static final String[] sopClasses = { UID.RTPlanStorage, UID.RTIonPlanStorage };

        public QCPCStoreService() {
            super(sopClasses, null);
        }
		@Override
		public void cstore(Association as, int pcid, DicomObject cmd,
				PDVInputStream dataStream, String tsuid)
				throws DicomServiceException, IOException {
        	QualityCheckPerformer.ui.setLastMessage("Starting c-store of plan");
		    DicomObject rsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
		    DicomObject storeObject = dataStream.readDataset();
		    String classUid = storeObject.getString(Tag.SOPClassUID);
		    String instanceUid = storeObject.getString(Tag.SOPInstanceUID);
		    QualityCheckPerformer.ui.setLastMessage("Class type is "+classUid+";  Instance is "+instanceUid);
		    persistPlan(storeObject);
		    as.writeDimseRSP(pcid, rsp);
		}

		private void persistPlan(DicomObject upsObject) {
			try {
				String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
						"/RTPLAN."+instanceUid));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
	    }
    }

    private static class QCPCMoveService extends DicomService implements CMoveSCP {

        private static final String[] sopClasses = { UID.StudyRootQueryRetrieveInformationModelMOVE };

        public QCPCMoveService() {
            super(sopClasses, null);
        }

        public void cmove(Association as, int pcid, DicomObject cmd, DicomObject data)
                throws DicomServiceException, IOException {
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
				storeObject = fetchStoreObject(data);
				rsp = cstoreObject(storeObject, cmd, data, pcid);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				rsp = CommandUtils.mkRSP(rsp, CommandUtils.C_MOVE_RSP);
			}
			return rsp;
		}

		private DicomObject cstoreObject(DicomObject storeObject, DicomObject cmd, DicomObject data, int pcid) {
			ui.setCurrentState("Formatting Report C-Store...");
			ui.setLastMessage("Formatting C-Store");
			String sopClassUid = UID.BasicTextSRStorage;
			String instanceUid = storeObject.getString(Tag.SOPInstanceUID);   
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
				ui.setLastMessage("Sending Report Object");
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
			File dir = new File(QA_REPORT_LOCATION);

			String[] children = dir.list();
			if (children == null) {
			    // Either dir does not exist or is not a directory
			} else {
			    for (int i=0; i<children.length; i++) {
			        // Get filename of file or directory
			        String filename = children[i];
			        DicomInputStream inStream = new DicomInputStream(new File(QA_REPORT_LOCATION+"/"+filename));
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

    
    private static class QCPNGetService extends DicomService implements NGetSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNGetService() {
            super(sopClasses, null);
        }

		@Override
		public void nget(Association as, int pcid, DicomObject cmd,
				DicomObject data) throws DicomServiceException, IOException {
        	ui.setLastMessage("Received N-Get Request...");
        	ui.setCurrentState("Received N-Get request...");
            DicomObject cmdrsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
            DicomObject getDataset = doNGet(as, pcid, cmd, data, cmdrsp);
            as.writeDimseRSP(pcid, cmdrsp, getDataset);
        }

        protected DicomObject doNGet(Association as, int pcid, DicomObject cmd,
                DicomObject data, DicomObject cmdrsp) throws IOException {
        	ui.setCurrentState("Doing N-Get...");
        	ui.setLastMessage("Executing N-Get...");
        	DicomObject returnObject = new BasicDicomObject();
        	DicomObject UPS =  fetchStoredUPS(cmd.getString(Tag.RequestedSOPInstanceUID));
        	int[] requestedAttributes = cmd.getInts(Tag.AttributeIdentifierList);
        	for (int i:requestedAttributes) {
        		returnObject.add(UPS.get(i));
        	}
            return returnObject;
        }

		private DicomObject fetchStoredUPS(String upsUid) throws IOException {
			DicomObject returnObject = null;
			File dir = new File(DEFAULT_PERSIST_LOCATION);

			String[] children = dir.list();
			if (children == null) {
			    // Either dir does not exist or is not a directory
			} else {
			    for (int i=0; i<children.length; i++) {
			        // Get filename of file or directory
			        if (children[i].lastIndexOf(upsUid)!=-1 && children[i].lastIndexOf(".dcm")!=-1) {
				        DicomInputStream inStream = new DicomInputStream(new File(DEFAULT_PERSIST_LOCATION+"/"+children[i]));
				        DicomObject checkObject = inStream.readDicomObject();
				        if (checkObject.getString(Tag.SOPInstanceUID).trim().equalsIgnoreCase(upsUid)) {
				        	returnObject = checkObject;
				        	break;
				        }
					    inStream.close();
			        }
			    }
			}
		return returnObject;
		}
    }
    
	
	public QualityCheckPerformer(String localAETitle, String localIP, String localPort, 
			String remoteAETitle, String remoteIP, String remotePort) {
		ui = new CPQCUI(WINDOW_TITLE, getActionList(), 600);
		ui.setActionButtonEnabled(false);
		ui.setActionListEnabled(false);
		ui.setResetButtonListener(_qcpRestButtonListener);
		ui.setActionButtonListener(_qcpActionButtonListener);
		ui.setActionBoxListener(_qcpActionBoxListener);
		
		// Set connection information.
		local.aetitle = localAETitle;
		local.ip = localIP;
		local.port = localPort;
		remote.aetitle = remoteAETitle;
		remote.ip = remoteIP;
		remote.port = remotePort;		
		initialize();
			//do initial setting of UI
		updateUI("Waiting for request", "Waiting for request");
	}
	

	
	private void initialize() {

		String name = WINDOW_TITLE;
		device = new Device(name);
		executor = new NewThreadExecutor(name);
		ae = new NetworkApplicationEntity();
		aeConn = new NetworkConnection();
		
		ae.setInstalled(true);
		ae.setAssociationAcceptor(true);
		ae.setAssociationInitiator(true);
		ae.setAETitle(local.aetitle);
		aeConn.setHostname(local.ip);
		aeConn.setPort(Integer.parseInt(local.port));
		ae.setNetworkConnection(aeConn);
		
		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(aeConn);
		
		setServices(ae);
		setTransferCapabilities(ae);
		
		remoteAE = new NetworkApplicationEntity();
		NetworkConnection remoteConn = new NetworkConnection();
		
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname(remote.ip);
		remoteConn.setPort(Integer.parseInt(remote.port));
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle(remote.aetitle);
	}

	private ArrayList getActionList() {
		ArrayList<String> actions = new ArrayList<String>();
		for (String qcrs:QcpAction)
		{
			actions.add(qcrs);
		}
		return actions;
	}
		
	
	private void setTransferCapabilities(NetworkApplicationEntity ae) {
		TransferCapability[] tc = new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE, ONLY_DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.RTPlanStorage, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.RTIonPlanStorage, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepEventSOPClass, ONLY_DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE, ONLY_DEF_TS, TransferCapability.SCP)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new QCPVerificationService());
		ae.register(new QCPNCreateService());
		ae.register(new QCPNActionService());
		ae.register(new QCPCStoreService());
		ae.register(new QCPCMoveService());
		ae.register(new QCPNGetService());
	}
	
    public void run()  {
        try {
			device.startListening(executor);
		} catch (IOException e) {
			e.printStackTrace();
		}
        QualityCheckPerformer.ui.setLastMessage("Start Server listening on port " + aeConn.getPort());
    }
    
   
	protected void updateUI(String stateText, String logText) {
		ui.setCurrentState(stateText);
		ui.setLastMessage(logText);
		ui.setVisible(true);
	}
	
	
    private void followThroughOnAction(int stateCounter) {
    	switch (stateCounter) {
    		case 0:
    			updateUI("Fetching workitem...", "Beginning fetch of workitem...");
    			requestWorkitem();
    			ui.setActionButtonEnabled(true);
    			ui.setActionListEnabled(true);
    			break;
    		case 1:
    			updateUI("Starting Quality Check...", "Updating...");
    			performAndReportOnQualityCheck();
    			ui.setActionButtonEnabled(false);
    			ui.setActionListEnabled(false);
    			updateUI("Waiting for request", "Finished with Quality Check...waitin for request...");
    			break;
		default:
			break;
		}
        
    }




	private void requestWorkitem() {
		updateUI("Formatting request", "Formatting RT Plan C-Move request...");
		String sopClassUid = UID.StudyRootQueryRetrieveInformationModelMOVE;
		DicomObject keys;
		try {
			keys = setAttributesForCMove();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			updateUI("Exception on move Workitem", "Exception thrown:"+e1.getMessage());
			return;
		}
		if (keys==null) {
			updateUI("Work item info not found", "Work item info not found...UPS not subscribed or \n" +
					"    some issue with input info sequnce of UPS");
			return;
		}
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	
	    AssociationWithLog assoc = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			updateUI("Sending C-Move Request for UID "+
					keys.getString(Tag.ReferencedSOPInstanceUID), "Sending...");
	        DimseRSPHandler rspHandler = new DimseRSPHandler() {
	            @Override
	            public void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
	                QualityCheckPerformer.this.onMoveRSP(as, cmd, data);
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

	
	private void performAndReportOnQualityCheck() {
		updateUI("Beginning Quality Check...first update progress to subscribers..","Updating subscribers...");
		String sopClassUid = UID.UnifiedProcedureStepEventSOPClass;
		DicomObject schedAttrs = setAttributesForNEventScheduledUpdate();
		DicomObject inProgAttrs = setAttributesForNEventInProgressUpdate();
		DicomObject compAttrs = setAttributesForNEventCompletedUpdate();
		String instanceUid = lastCreatedUid;
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	
	    AssociationWithLog assoc = null;
		try {
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
	        DimseRSPHandler rspHandler = new DimseRSPHandler() {
	            @Override
	            public void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
	                QualityCheckPerformer.this.onNeventRSP(as, cmd, data);
	            }
	        };
			updateUI("Sending...","Sending N-Event Scheduled Update for UPS");
	        assoc.nevent(sopClassUid, instanceUid, 1, schedAttrs, transferSyntaxUid, rspHandler);
			updateUI("Waiting for response from "+remote.aetitle,"Waiting for response on UPS Scheduled Update");
	        assoc.waitForDimseRSP();
			updateUI("Response received", "Response received");
			assoc.release(true);

			updateUpsToInProgress(lastCreatedUid);
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			updateUI("Sending...", "Sending N-Event In Progress Update for UPS");
	        assoc.nevent(sopClassUid, instanceUid, 1, inProgAttrs, transferSyntaxUid, rspHandler);
			updateUI("Waiting for response on UPS In Progress Update", "Waiting for response from "+remote.aetitle);
	        assoc.waitForDimseRSP();
			updateUI("Response received", "Response received");
			assoc.release(true);
			
			doQualityCheck();

			updateUpsToComplete(lastCreatedUid);
			assoc = new AssociationWithLog(ae.connect(remoteAE, executor));
			updateUI("Sending...","Sending N-Event Completed Update for UPS");
	        assoc.nevent(sopClassUid, instanceUid, 1, compAttrs, transferSyntaxUid, rspHandler);
			updateUI("Waiting for response on UPS Completed Update", "Waiting for response from "+remote.aetitle);
	        assoc.waitForDimseRSP();
			updateUI("Response received", "Response received");
			assoc.release(true);
			
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}

	}

	


	private void updateUpsToInProgress(String lastCreatedUid) throws IOException {
		DicomObject ups = getPersistedUPSOnUid(lastCreatedUid);
		ups.putString(Tag.UnifiedProcedureStepState, VR.CS, "IN PROGRESS");
    	DicomObject progressSeq = new BasicDicomObject();
    	ups.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
    	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "20");
    	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Started");
    	ups.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
		updateUPS(ups);
	}



	private void updateUpsToComplete(String lastCreatedUid) throws IOException {
		DicomObject ups = getPersistedUPSOnUid(lastCreatedUid);
		ups.putString(Tag.UnifiedProcedureStepState, VR.CS, "COMPLETE");
    	DicomObject progressSeq = new BasicDicomObject();
    	ups.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
    	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "100");
    	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Completed");
    	ups.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
    	DicomObject performedSeq = new BasicDicomObject();
    	ups.putSequence(Tag.UnifiedProcedureStepPerformedProcedureSequence);
    	DicomObject outputInfoSeq = new BasicDicomObject();
    	outputInfoSeq.putString(0x0040E020, VR.CS, "DICOM");
    	DicomObject inputInfoSeq = ups.getNestedDicomObject(Tag.InputInformationSequence);
    	String seriesUid = inputInfoSeq.getString(Tag.StudyInstanceUID);
    	String studyUid = inputInfoSeq.getString(Tag.SeriesInstanceUID);
    	outputInfoSeq.putString(Tag.StudyInstanceUID, VR.UI, studyUid);
    	outputInfoSeq.putString(Tag.SeriesInstanceUID, VR.UI, seriesUid);
    	DicomObject sopSequence = new BasicDicomObject();
		sopSequence.putString(Tag.ReferencedSOPClassUID, VR.UI, UID.BasicTextSRStorage);
		StructuredReport bsr = new StructuredReport(studyUid, seriesUid);
		bsr.persistMe(QA_REPORT_LOCATION);
		sopSequence.putString(Tag.ReferencedSOPInstanceUID, VR.UI, bsr.getString(Tag.SOPInstanceUID));
		DicomObject retrieveSequence = new BasicDicomObject();
		retrieveSequence.putString(Tag.RetrieveAETitle, VR.AE, local.aetitle);
		outputInfoSeq.putSequence(0x0040E021);
		outputInfoSeq.putNestedDicomObject(0x0040E021, retrieveSequence);
		outputInfoSeq.putSequence(Tag.ReferencedSOPSequence);
		outputInfoSeq.putNestedDicomObject(Tag.ReferencedSOPSequence, sopSequence);
		performedSeq.putSequence(Tag.OutputInformationSequence);
		performedSeq.putNestedDicomObject(Tag.OutputInformationSequence, outputInfoSeq);
		ups.putNestedDicomObject(Tag.UnifiedProcedureStepPerformedProcedureSequence, performedSeq);
		updateUPS(ups);
	}




	private void doQualityCheck() throws InterruptedException {
		updateUI("Checking...", "Starting Quality Check...");
		// report was already persisted while updating ups to 
		updateUI("End Of Check", "Completed Quality Check...");
		//create structured report
	}



	private DicomObject setAttributesForNEventInProgressUpdate() {
		DicomObject eventObject = new BasicDicomObject();
		eventObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "IN PROGRESS");
		eventObject.putString(0x00404041, VR.CS, "READY");
		return eventObject;
	}

	private DicomObject setAttributesForNEventCompletedUpdate() {
		DicomObject eventObject = new BasicDicomObject();
		eventObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "COMPLETED");
		eventObject.putString(0x00404041, VR.CS, "READY");
		return eventObject;
	}


	private DicomObject setAttributesForNEventScheduledUpdate() {
		DicomObject eventObject = new BasicDicomObject();
		eventObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "SCHEDULED");
		eventObject.putString(0x00404041, VR.CS, "READY");
		return eventObject;
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

	protected void onNeventRSP(Association as, DicomObject cmd, DicomObject data) {
	System.out.println("got nevent response");
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



	private DicomObject setAttributesForCMove() throws IOException {
		DicomObject subscribedUPS = getSubscribedUPS(remote.aetitle);
		DicomObject iisSequence = subscribedUPS.getNestedDicomObject(Tag.InputInformationSequence);
		DicomObject CMoveObject = new BasicDicomObject();
		CMoveObject.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
		CMoveObject.putString(Tag.StudyInstanceUID, VR.UI, 
				iisSequence.getString(Tag.StudyInstanceUID));
		CMoveObject.putString(Tag.SeriesInstanceUID, VR.UI, 
				iisSequence.getString(Tag.SeriesInstanceUID));
		DicomObject refSopSequence = iisSequence.getNestedDicomObject(Tag.ReferencedSOPSequence);
		CMoveObject.putString(Tag.ReferencedSOPClassUID, VR.UI, 
				refSopSequence.getString(Tag.ReferencedSOPClassUID));
		CMoveObject.putString(Tag.ReferencedSOPInstanceUID, VR.UI, 
				refSopSequence.getString(Tag.ReferencedSOPInstanceUID));

		return CMoveObject;
	}



	private DicomObject getSubscribedUPS(String aetitle) throws IOException {
		DicomObject returnUPS = null;
		String upsUid = getSubscribedUid(aetitle);
		returnUPS = getPersistedUPSOnUid(upsUid);
		return returnUPS;
	}



	private DicomObject getPersistedUPSOnUid(String upsUid) throws IOException {
		DicomObject returnUPS = null;
		String stuff = DEFAULT_PERSIST_LOCATION+"UPS."+upsUid+".dcm";

		File dir = new File(DEFAULT_PERSIST_LOCATION);

		String[] children = dir.list();
		if (children == null) {
		    // Either dir does not exist or is not a directory
		} else {
		    for (int i=0; i<children.length; i++) {
		        // Get filename of file or directory
		        String filename = children[i];
		        if (filename.lastIndexOf("dcm")!=-1) {
			        DicomInputStream inStream = new DicomInputStream(new File(DEFAULT_PERSIST_LOCATION+"/"+filename));
			        DicomObject checkObject = inStream.readDicomObject();
			        if (checkObject.getString(Tag.SOPInstanceUID).trim().equalsIgnoreCase(upsUid)) {
			        	returnUPS = checkObject;
			        	break;
			        }
					inStream.close();
		        }
		    }
		}
		
		return returnUPS;
	}



	private String getSubscribedUid(String aetitle) throws IOException {
		String subscribedUid = null;
		BufferedReader in = new BufferedReader(new FileReader(DEFAULT_PERSIST_LOCATION+SUBSCRIBE_FILE_NAME));
		String str;
		while ((str = in.readLine()) != null) {
			if (str.lastIndexOf(aetitle)!=-1) {
					subscribedUid = str.substring(0, str.lastIndexOf("$"));
					break;
			}
		}
		in.close();
		return subscribedUid;
	}
	
	private void updateUPS(DicomObject upsObject) {
		try {
			String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
			DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
					"UPS."+instanceUid+".dcm"));
			outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
			outStream.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}


}
