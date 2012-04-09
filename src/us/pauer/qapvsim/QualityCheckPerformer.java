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

import javax.swing.JComboBox;
import javax.swing.JRadioButton;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.SingleDimseRSP;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
import org.dcm4che2.net.service.CStoreSCP;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.NActionSCP;
import org.dcm4che2.net.service.NCreateSCP;
import org.dcm4che2.util.UIDUtils;

import us.pauer.qapvsim.BaseQualityCheck.connectionInfo;





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
	Executor executor;
	NetworkApplicationEntity ae;
	NetworkApplicationEntity remoteAE;
	NetworkConnection aeConn;

	// window title
	static final String WINDOW_TITLE = "Performer";

	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/QCP/";
	static final String DEFAULT_DCM_TRACK_LOCATION = DEFAULT_PERSIST_LOCATION+"TRAFFIC/";
	static final String SUBSCRIBE_FILE_NAME = "SUBSCRIBED.DATA";
	
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	
	String lastCreatedUid = "";
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
						"/UPS."+instanceUid));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
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
			    out.write(uid+"$"+ae);
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
					if (str.lastIndexOf(uid)!=-1 && str.lastIndexOf(ae)!=-1) {
						found = true;
					} else {
						subscriptions.add(str);
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
				new TransferCapability(UID.RTIonPlanStorage, ONLY_DEF_TS, TransferCapability.SCP)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new QCPVerificationService());
		ae.register(new QCPNCreateService());
		ae.register(new QCPNActionService());
		ae.register(new QCPCStoreService());
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
    			updateUI("Beginning fetch of workitem", "Fetching...");
    			requestWorkitem();
    			ui.setActionButtonEnabled(true);
    			ui.setActionListEnabled(true);
    			break;
    		case 1:
    			//do quality check
    			break;
		default:
			break;
		}
        
    }


	private void requestWorkitem() {
		updateUI("Formatting RT Plan C-Move request...","Formatting request");
		String sopClassUid = UID.StudyRootQueryRetrieveInformationModelMOVE;
		DicomObject keys = setAttributesForCMove();
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

	        /*// Do some move error checking
	        Exception e = getAssocException();
	        if (e != null) {
	            ui.setLastMessage("Exception ocurred on the association while retrieving:"+e.getMessage());
	            throw new Exception("Exception ocurred on the association while retrieving.", e);
	        } else if (isAbortingMove(moveStatus)) {
	            StringBuffer str = new StringBuffer("Query/Retrieve SCP terminated move prematurally. Move status = ");
	            str.append(Integer.toHexString(moveStatus));
	            if (moveError != null) {
	                str.append(", Move error = " + moveError);
	            }
	            log.error(fn + str.toString());
	            throw new DcmMoveException(str.toString());
	        }*/

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


	private DicomObject setAttributesForCMove() {
		DicomObject CMoveObject = new BasicDicomObject();
		CMoveObject.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
		CMoveObject.putString(Tag.StudyInstanceUID, VR.UI, "1.2.34.78");
		CMoveObject.putString(Tag.SeriesInstanceUID, VR.UI, "1.2.34.78.1");
		CMoveObject.putString(Tag.ReferencedSOPClassUID, VR.UI, UID.RTPlanStorage);
		CMoveObject.putString(Tag.ReferencedSOPInstanceUID, VR.UI, "1.2.34.56");

		return CMoveObject;
	}



}
