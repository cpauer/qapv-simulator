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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;

import javax.swing.JComboBox;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
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
	private enum QcpState {
		WAITING_FOR_CREATE_REQUEST("Waiting for create request", "", 
				"Waiting for create request", "Waiting..."),
		CREATING_UPS("Creating UPS", "", "Creating UPS for Quality Check...",
				"Creating UPS for Qulity Check..."),
		WAITING_FOR_SUBSCRIBE_REQUEST("Waiting for subscribe request", "", "Waiting for subscribe...", "Waiting for " +
				"subscribe....."),
		ADD_SUBSCRIPTION_TO_UPS("Subscribing", "", "Add subscriber to UPS...",
				"Adding subscriber to UPS..."),
		WAITING_FOR_NEXT_REQUEST("Waiting for next request", "", "Waiting for next...", "Waiting for " +
				"next.....");
		
		private final String actionText;  // value that is displayed next to action button
		private final String buttonText;  // value that is displayed on the action button
		private final String logText;  //value to put in the log area when this state is loaded
		private final String stateText; //value to put in the state message box
		QcpState(String pActionText, String pButtonText, String pLogText, String pStateText) {
			this.actionText = pActionText;
			this.buttonText = pButtonText;
			this.logText = pLogText;
			this.stateText = pStateText;
		}
		private String getActionText() { return actionText; }
		private String getButtonText() { return buttonText; }
		private String getLogText() { return logText; }
		private String getStateText() { return stateText; }
	}
	
	// counter for the state to make sure we know where we currently are in the workflow lifecycle
	static int stateCounter = 0;
	
	Device device;
	Executor executor;
	NetworkApplicationEntity scp;
	NetworkConnection scpConn;

	// window title
	static final String WINDOW_TITLE = "Performer";

	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/SCP/";
	
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	

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
        		changeToNextState();
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
	

    private static class QCPVerificationService extends DicomService implements CEchoSCP {

        private static final String[] sopClasses = { UID.VerificationSOPClass };

        public QCPVerificationService() {
            super(sopClasses, null);
        }

        // has to implement this method signature…
        public void cecho(Association as, int pcid, DicomObject cmd)
                throws IOException {
            as.writeDimseRSP(pcid, CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS));
            System.out.println("C-Echo has responded");
        }
    }

    private static class QCPNCreateService extends DicomService implements NCreateSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNCreateService() {
            super(sopClasses, null);
        }

        public void ncreate(Association as, int pcid, DicomObject rq,
		        DicomObject data) throws DicomServiceException, IOException {
			System.out.println("Starting n-create on server");
			changeToNextState();
		    DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
		    String iuid = data.getString(Tag.AffectedSOPInstanceUID);
		    if (iuid == null) {
		        iuid = UIDUtils.createUID();
		        ui.setLastMessage("UID for UPS is "+iuid);
		        rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		        rsp.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		    }
		    as.writeDimseRSP(pcid, rsp, doNCreate(as, pcid, rq, data, rsp));
		    changeToNextState();
		}
        
        private DicomObject doNCreate(Association as, int pcid, DicomObject rq,
                DicomObject data, DicomObject rsp) throws IOException {
        	System.out.println("in N-Create");
        	DicomObject UPSObject = createUPS(rq, data);
        	persistUPS(UPSObject);
    		return UPSObject;
            
        }

        private DicomObject createUPS(DicomObject rq, DicomObject data) {
        	System.out.println("in createUPS");
        	DicomObject outObject = new BasicDicomObject();
        	//SOP Common
        	outObject.putString(Tag.SOPClassUID, VR.UI, UID.UnifiedProcedureStepPushSOPClass);
        	outObject.putString(Tag.SOPInstanceUID, VR.UI, data.getString(Tag.AffectedSOPInstanceUID));
        	System.out.println("affect sop instance:"+data.getString(Tag.AffectedSOPInstanceUID));
        	outObject.putDate(Tag.InstanceCreationDate, VR.DA, new Date());
        	outObject.putDate(Tag.InstanceCreationTime, VR.TM, new Date());
        	//add appropriate items
        	outObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "SCHEDULED");
        	outObject.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
        	
        	DicomObject progressSeq = new BasicDicomObject();
        	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "0");
        	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Scheduled");
        	outObject.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
          	System.out.println("before IIS");
    		DicomObject inputSequenceFromRequest = data.getNestedDicomObject(Tag.InputInformationSequence);
          	if (inputSequenceFromRequest==null) System.out.println("iis is " );		
          	outObject.putSequence(Tag.InputInformationSequence);
          	outObject.putNestedDicomObject(Tag.InputInformationSequence, inputSequenceFromRequest);
        	System.out.println("out createUPS");
			return outObject;
		}

		private void persistUPS(DicomObject upsObject) {
        	System.out.println("in persistUPS");
    		try {
    			String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
						"/NCREATE."+instanceUid));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
	        	System.out.println("out persistUPS");
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
			System.out.println("Starting n-action on server");
			changeToNextState();
		    DicomObject rsp = CommandUtils.mkRSP(command, CommandUtils.SUCCESS);
		    // capture the incoming uid
		    int stuff = command.getInt(Tag.ActionTypeID);
		    System.out.println("Action id is "+stuff);
		    String iuid = command.getString(Tag.RequestedSOPInstanceUID);
		    String receivingAE = data.getString(Tag.ReceivingAE);
		    ui.setLastMessage("Received subscribe request for uid "+iuid+" from "+receivingAE);
		    String lockDeletion = data.getString(Tag.DeletionLock);
		    // We don’t implement an actual subscription persistence mechanism here, but you can see
            // we have the data available to do so….
		    System.out.println("AE "+receivingAE+" is subscribed to SOP Class Instance "
		    		+iuid+" with Delete Lock set to "+lockDeletion);
		    as.writeDimseRSP(pcid, rsp, data);
		    changeToNextState();
		}
    }

    
	
	public QualityCheckPerformer(String localAETitle, String localIP, String localPort, 
			String remoteAETitle, String remoteIP, String remotePort) {
		ui = new CPQCUI(WINDOW_TITLE, getActionList());
		ui.setActionButtonEnabled(false);
		
		// Set connection information.
		local.aetitle = localAETitle;
		local.ip = localIP;
		local.port = localPort;
		remote.aetitle = remoteAETitle;
		remote.ip = remoteIP;
		remote.port = remotePort;		
		initialize();
			//do initial setting of UI
		ui.setResetButtonListener(_qcpRestButtonListener);
		ui.setActionButtonListener(_qcpActionButtonListener);
		ui.setActionBoxListener(_qcpActionBoxListener);
		updateUI("Waiting for UPS request", "Waiting for UPS request");
	}

	private ArrayList getActionList() {
		ArrayList<String> actions = new ArrayList<String>();
		for (QcpState qcrs:QcpState.values())
		{
			actions.add(qcrs.buttonText);
		}
		return actions;
	}

	
	private void initialize() {

		String name = WINDOW_TITLE;
		device = new Device(name);
		executor = new NewThreadExecutor(name);
		scp = new NetworkApplicationEntity();
		scpConn = new NetworkConnection();
		
		scp.setInstalled(true);
		scp.setAssociationAcceptor(true);
		scp.setAETitle(local.aetitle);
		scpConn.setHostname(local.ip);
		scpConn.setPort(Integer.parseInt(local.port));
		scp.setNetworkConnection(scpConn);
		
		device.setNetworkApplicationEntity(scp);
		device.setNetworkConnection(scpConn);
		
		setServices(scp);
		setTransferCapabilities(scp);
	}
		
	
	private void setTransferCapabilities(NetworkApplicationEntity ae) {
		TransferCapability[] tc = new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass, ONLY_DEF_TS, TransferCapability.SCP)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new QCPVerificationService());
		ae.register(new QCPNCreateService());
		ae.register(new QCPNActionService());
	}
	
    public void run()  {
        try {
			device.startListening(executor);
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("Start Server listening on port " + scpConn.getPort());
    }
    
	private static void changeToNextState() {
        stateCounter = (stateCounter+1) % QcpState.values().length;
	}
   
	private void updateUI(String stateText, String logText) {
		ui.setCurrentState(stateText);
		ui.setLastMessage(logText);
		ui.setVisible(true);
	}
	
	
    private void followThroughOnAction(int stateCounter) {
    	QcpState currentState = QcpState.values()[stateCounter];
    	switch (currentState) {
    		case WAITING_FOR_CREATE_REQUEST:
    			//do nothing, waiting for UI interaction
    			break;
    		case CREATING_UPS:
    			break;
    			
    		case WAITING_FOR_SUBSCRIBE_REQUEST:
    			//do nothing, waiting for request
    			break;
    		case ADD_SUBSCRIPTION_TO_UPS:
    			break;
    		case WAITING_FOR_NEXT_REQUEST:
    			break;

		default:
			break;
		}
        
    }

    /*public void stop() {
        if (device != null)
            device.stopListening();

        if (scpConn != null)
            System.out.println("Stop Server listening on port " + scpConn.getPort());
        else
            System.out.println("Stop Server");
    }*/

	public String getAeTitle() {
		return scp.getAETitle();
	}



}
