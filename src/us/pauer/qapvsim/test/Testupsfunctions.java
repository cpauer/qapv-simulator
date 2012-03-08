package us.pauer.qapvsim.test;



import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;

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

import us.pauer.qapvsim.UPSSCP;




/* ***** BEGIN LICENSE BLOCK *****
* 
*	This set of Junits is provided to test the
*   UPSSCP class.  	
*	
*    Copyright (C) 2012  Chris Pauer
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

public class Testupsfunctions extends TestCase {
	
	

	public void testEchoOnSCP() 
	{
		// we will need this string array with the valid UIDs for supported transfer syntaxes for the SCU/QCR AE…
		String[] DEF_TS = { UID.ImplicitVRLittleEndian };
		UPSSCP scp = new UPSSCP("QCP", "localhost", 40405);
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
        // UPSSCP when that is finally built…
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

	
	public void testCreateUPS() 
	{
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};
		UPSSCP scp = new UPSSCP("QCP", "localhost", 40405);
		try {
			scp.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		String name = "TestSCUCreate";
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
		finally {
			scp.stop();
		}

	}
		
	
/*	public void testUPSCreationAndPersistence() {
		try {
			DicomObject upsObject = setInitialAttributes();
			assertTrue(upsObject!=null);
	    	//SOP Common
	    	upsObject.putString(Tag.SOPClassUID, VR.UI, UID.UnifiedProcedureStepPushSOPClass);
	    	upsObject.putString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
	    	upsObject.putDate(Tag.InstanceCreationDate, VR.DA, new Date());
	    	upsObject.putDate(Tag.InstanceCreationTime, VR.TM, new Date());
	    	//add appropriate items
	    	upsObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "SCHEDULED");
	    	upsObject.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
	    	DicomObject progressSeq = new BasicDicomObject();
	    	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "0");
	    	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Scheduled");
	    	upsObject.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
			assertTrue(upsObject!=null);
	    	try {
				String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				assertTrue(instanceUid!=null);
				File outFile = new File("C:/QAPVSIM/SCU/NCREATE."+instanceUid);
				outFile.createNewFile();
				DicomOutputStream  outStream = new DicomOutputStream(outFile);
				assertTrue(outStream!=null);
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		}
	}
*/

	public void testSubscribeToUPSProgressUpdate()
	{
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};
		UPSSCP scp = new UPSSCP("QCP", "localhost", 40405);
		try {
			scp.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		String name = "TestSCUSubscribe";
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
		subscribeObject.putString(Tag.ReceivingAE, VR.AE, "TestSCUSubscribe");
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
		finally {
			scp.stop();
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
	
	public void testUnsubscribeToUPSProgressUpdate()
	{
		assertTrue(true);
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
