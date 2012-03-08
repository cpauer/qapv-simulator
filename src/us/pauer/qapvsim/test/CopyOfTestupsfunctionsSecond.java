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
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.SingleDimseRSP;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.UIDUtils;

import us.pauer.qapvsim.UPSSCP;




public class CopyOfTestupsfunctionsSecond extends TestCase {
	
	

	public void testEchoOnSCP() 
	{
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian };
		UPSSCP scp = new UPSSCP("QCP", "localhost", 40405);
		try {
			scp.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		

		String name = "TestSCUEcho";
		Device device = new Device(name);
		Executor executor = new NewThreadExecutor(name);

		NetworkApplicationEntity ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();

		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);

		ae.setNetworkConnection(localConn);
		ae.setAssociationInitiator(true);
		ae.setAETitle(name);
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass,
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
		DicomObject attrs = setInitialAttributes();
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
		
	/** Not all attributes as described in table CC.2.5-3 are populated here
	 * only a subset to get us started.
	 * @return DicomObject   contains initial set of attributes for UPS
	 */
	
	private DicomObject setInitialAttributes() {
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
		return returnObject;
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
		DicomObject attrs = setInitialAttributes();
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
		subscribeObject.putString(Tag.ReceivingAE, VR.AE, "TestSCUSubscribe");
		subscribeObject.putString(Tag.DeletionLock, VR.LO, "TRUE");
		try {
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
	
/*	public void testRetrieveInputObjectsFromObjectStore()
	{
		assertTrue(true);
	}
	
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
}
