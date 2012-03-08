package us.pauer.qapvsim.test;

import java.io.File;
import java.io.IOException;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.media.DicomDirReader;

import us.pauer.qapvsim.PersistenceUtils;

import junit.framework.TestCase;

public class TestSimPersistence extends TestCase {
	
	static final String PERSIST_DIRECTORY = "C:\\QAPVSIM\\PERSIST";
	
	
	public void testDICOMObjectPersistRetrieveDelete() {
		PersistenceUtils pUtils = new PersistenceUtils();
		assertTrue(pUtils.PERSIST_DIRECTORY.equalsIgnoreCase("C:\\QAPVSIM\\PERSIST"));
		assertTrue(pUtils.checkForPersistenceDirectory());
		File persist = pUtils.getPersistDirectory();
		//read all object
		DicomObject[] allObjects = null;
		DicomObject[] planObjects = null;		
		try {
			allObjects = pUtils.readAllObjects();
			assertTrue(allObjects.length>0);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		try {
			planObjects = pUtils.readAllObjectsOfType(UID.RTPlanStorage);
			assertTrue(planObjects.length>0);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		DicomObject plan = planObjects[0];
		plan.putString(Tag.SOPInstanceUID, VR.UI, "1.2.34.56");
		//get location to persist
		try {
			pUtils.writeObject(plan);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		try {
			plan = pUtils.readMatchingObjects("1.2.34.56")[0];
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		String[] allFiles = null;
		String[] planFiles = null;		
		try {
			allFiles = pUtils.readAllFileNames();
			assertTrue(allFiles.length>0);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		try {
			planFiles = pUtils.readAllFileNamesOfType(UID.RTPlanStorage);
			assertTrue(planFiles.length>0);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}


}
