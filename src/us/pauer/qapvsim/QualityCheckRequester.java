package us.pauer.qapvsim;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;

import javax.swing.JComboBox;

import junit.framework.TestCase;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
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

public class QualityCheckRequester  {

	public CPQCUI ui;
	
	// connection information
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	connectionInfo local = new connectionInfo();
	connectionInfo remote = new connectionInfo();

	
	
		

	/**
	 * Enums that document the states that the QCR will go through, listed in happy path order.
	 * Using an Enum will allow us to adhere the action text, button text, log and state messages 
	 * in the UI with the state of the QCR at that time.
	 * @author Chris Pauer
	 *
	 */
	private enum QcrState {
		WAITING_FOR_CREATE_BUTTON_CLICK("Waiting for create button click", "Create QC UPS", 
				"Watiing for create button click", "Waiting..."),
		WAITING_FOR_REQUEST_BUTTON_CLICK("Waiting for subscribe button click", "Subscribe to UPS", 
				"Response received...\nWaiting for Subscribe...", "Waiting for subscribe button click");
		
		private final String actionText;  // value that is displayed next to action button
		private final String buttonText;  // value that is displayed on the action button
		private final String logText;  //value to put in the log area when this state is loaded
		private final String stateText; //value to put in the state message box
		QcrState(String pActionText, String pButtonText, String pLogText, String pStateText) {
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
	int stateCounter = 0;
	
	// window title
	static final String WINDOW_TITLE = "Requester";
	
	Executor executor;
	NetworkApplicationEntity ae;
	NetworkApplicationEntity remoteAE;
	String upsUid;
	
	
   	/**
	 *  order of execution:
	 *   1) QCR and UI are at some state....
	 *   2a) Button is active and Button is clicked
	 *      - or -
	 *   2b) QCR, in a wait state, receives some kind of response
	 *      - or -
	 *   2c) QCR itself finishes one set of instructions and decides to
	 *       transition to a new state...
	 *   3) If this is a state change originating at the UI, the button is disabled
	 *   3) The stateCounter is incremented    
	 *   4) The UI is updated with the next state
	 *   5) The current state processing is followed through on, if appropriate
	 *  
	 *   
	 *   This order of processing is dependant on the assumption that internal processing states,
	 *   where the UI will be only informational, and not be allowed to interact with the user, are going to
	 *   be populated in the state enumeration.  Then the QCR will act on the current state, because it will be an
	 *   internal processing state...
	 */

	
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
        }
    };


	/*
	 * CONSTRUCTOR
	 */
	public QualityCheckRequester(String localAETitle, String localIP, String localPort, 
			String remoteAETitle, String remoteIP, String remotePort)  
	{	
		ui = new CPQCUI(WINDOW_TITLE, getActionList());
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
		updateUI("Waiting for Create UPS action", "Waiting for Create UPS action");
	}
	

	private ArrayList getActionList() {
		ArrayList<String> actions = new ArrayList<String>();
		for (QcrState qcrs:QcrState.values())
		{
			actions.add(qcrs.buttonText);
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
    			upsUid = createUPS();
    	        ui.setLastMessage("UPS that was created has UID of "+upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
    		case 1:
    			subscribeToUPSProgressUpdate(upsUid);
    	        updateUI("Waiting for next request", "Waiting for next request");
    	        ui.setActionButtonEnabled(true);
    	        ui.setActionListEnabled(true);
    			break;
		default:
			break;
		}
        
    }

/*	public void testEchoOnSCP() 
	{
		// we will need this string array with the valid UIDs for supported transfer syntaxes for the SCU/QCR AE…
		String[] DEF_TS = { UID.ImplicitVRLittleEndian };
		QualityCheckPerformer scp = new QualityCheckPerformer("QCP", "localhost", 40405);
		try {
			scp.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		

		// We need a name for our local server or SCU….and then instantiate a Device and a thread handler with that name…
		String name = "TestSCUEcho";
		Device device = new Device(name);
		Executor executor = new NewThreadExecutor(name);

		// next we define a local AE and the local connection….
		NetworkApplicationEntity ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();

		// We tell the device about those items….
		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);

		// The local SCU AE needs to know about the connection…
		ae.setNetworkConnection(localConn);
		
		// Here we say, basically, that the local AE is an SCU…if an SCP, we would setAssociationAcceptor(true)
		ae.setAssociationInitiator(true);
		ae.setAETitle(name);
		
		//to create valid associations, we need to tell this AE what Transfer Capabilities are possible
        // note that the Transfer Capability matches the Verification SOP Class (C-Echo, with the valid transfer
        // syntaxes defined above, with the SCU marker…
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass,
						DEF_TS, TransferCapability.SCU)
		});

		//Now our local SCU needs to know about the remote AE….
		NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
		NetworkConnection remoteConn = new NetworkConnection();
		
		// Here we say describe the remote AE….Note we use the AE Title we will establish for the
        // QualityCheckPerformer when that is finally built…
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname("localhost");
		remoteConn.setPort(40405);
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle("QCP");
		
		//  We now start to try to connect, and do our C-Echo…
		Association assoc = null;

		try {
			assoc = ae.connect(remoteAE, executor);
		} catch (ConfigurationException e) {
			assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (InterruptedException e) {
			assertTrue(false);
		}

		try {
			assoc.cecho().next();
		} catch (IOException e) {
			assertTrue(false);
		} catch (InterruptedException e) {
			assertTrue(false);
		}
        scp.stop();

	}
*/
	private void initialize() {
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};
		String name = WINDOW_TITLE;
		Device device = new Device(name);
		
		executor = new NewThreadExecutor(name);
		ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();
	
		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);
	
		ae.setNetworkConnection(localConn);
		ae.setAssociationInitiator(true);
		ae.setAssociationAcceptor(false);
		ae.setAETitle(name);
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass,
						DEF_TS, TransferCapability.SCU)
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

	public String createUPS() 
	{
		
		updateUI("Formatting UPS N-CREATE request...","Formatting request");
		String abstractSyntaxUID = UID.UnifiedProcedureStepPushSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String instanceUid = null;   //let the scp assign the instance uid
		DicomObject attrs = setInitialUPSAttributesForNCreate();
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	    DimseRSP rsp = null;
	
	    Association assoc = null;
		try {
			assoc = ae.connect(remoteAE, executor);
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
		String abstractSyntaxUID = UID.UnifiedProcedureStepWatchSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
		DicomObject subscribeObject = new BasicDicomObject();
		
		// These are required attributes for the N-Action subscribe as per Annex CC
		subscribeObject.putString(Tag.ReceivingAE, VR.AE, "TestSCUSubscribe");
		subscribeObject.putString(Tag.DeletionLock, VR.LO, "TRUE");
	    Association assoc = null;
	    DimseRSP rsp = null;
		try {
			assoc = ae.connect(remoteAE, executor);
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “3” for the subscribe action…
			rsp = assoc.naction(abstractSyntaxUID, sopClassUid, uidForSubscription,
					3, subscribeObject, transferSyntaxUid); 
			while (!rsp.next()){}
			//while (true){}
			//check for valid response
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		}

	}
	
/*	
	public void testUpdateOnUPSProgress()
	{
		assertTrue(true);
	}
	
	public void testRetrievePassFailStatus()
	{
		assertTrue(true);
	}
*/	
	/*
	public void testUnsubscribeToUPSProgressUpdate()
	{
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};
		QualityCheckPerformer scp = new QualityCheckPerformer("QCP", "localhost", 40405);
		try {
			scp.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		String name = "TestSCUUnsub";
		Device device = new Device(name);
		
		Executor executor = new NewThreadExecutor(name);
		NetworkApplicationEntity ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();

		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);

		ae.setNetworkConnection(localConn);
		ae.setAssociationInitiator(true);
		ae.setAssociationAcceptor(false);
		ae.setAETitle(name);
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass,
						DEF_TS, TransferCapability.SCU),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass,
						DEF_TS, TransferCapability.SCU)
		});  

		NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
		NetworkConnection remoteConn = new NetworkConnection();
		
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname("localhost");
		remoteConn.setPort(40405);
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle("QCP");
		//start Create association
		String abstractSyntaxUID = UID.UnifiedProcedureStepPushSOPClass;
		String sopClassUid = UID.UnifiedProcedureStepPushSOPClass;
		String instanceUid = null;   //let the scp assign the instance uid
		DicomObject attrs = setInitialUPSAttributesForNCreate();
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	    DimseRSP rsp = null;

	    Association assoc = null;
		try {
			assoc = ae.connect(remoteAE, executor);
	        rsp =  assoc.ncreate(abstractSyntaxUID, sopClassUid, instanceUid, attrs,
	                transferSyntaxUid);
	        while (!rsp.next()){}
	        assertTrue(rsp!=null);
	        assertTrue(rsp.getDataset()!=null);
	        // here we capture the instance uid returned from the SCP….
	        instanceUid = rsp.getCommand().getString(Tag.AffectedSOPInstanceUID);
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		//Start Subscribe association
		abstractSyntaxUID = UID.UnifiedProcedureStepWatchSOPClass;
		// sop class uid stays the same
		//instance uid we received back from the create transaction
		DicomObject subscribeObject = new BasicDicomObject();
		
		// These are required attributes for the N-Action subscribe as per Annex CC
		subscribeObject.putString(Tag.ReceivingAE, VR.AE, "TestSCUUnsub");
		subscribeObject.putString(Tag.DeletionLock, VR.LO, "TRUE");
		try {
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “3” for the subscribe action…
			rsp = assoc.naction(abstractSyntaxUID, sopClassUid, instanceUid,
					3, subscribeObject, transferSyntaxUid); 
			while (!rsp.next()){}
			assertTrue(rsp!=null);
			assertTrue(rsp.getCommand()!=null);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		}

		// sop class uid stays the same
		//instance uid we received back from the create transaction
		DicomObject unsubscribeObject = new BasicDicomObject();
		
		// These are required attributes for the N-Action subscribe as per Annex CC
		unsubscribeObject.putString(Tag.ReceivingAE, VR.AE, "TestSCUUnsub");
		try {
			// The Association class has an method signature for the subscribe and
            // unsubscribe actions…note the “4” for the unsubscribe action…
			rsp = assoc.naction(abstractSyntaxUID, sopClassUid, instanceUid,
					4, subscribeObject, transferSyntaxUid); 
			while (!rsp.next()){}
			assertTrue(rsp!=null);
			assertTrue(rsp.getCommand()!=null);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		finally {
			scp.stop();
		}

	}
*/
	
	
	
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
		retrieveSequence.putString(Tag.RetrieveAETitle, VR.AE, "TestObjectStore");
		inputSequence.putSequence(0x0040E021);
		inputSequence.putNestedDicomObject(0x0040E021, retrieveSequence);
		return inputSequence;
	}
}
