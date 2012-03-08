package us.pauer.qapvsim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;

public class PersistenceUtils {
	public static final String PERSIST_DIRECTORY = "C:\\QAPVSIM\\PERSIST";
	
	
	

	public boolean checkForPersistenceDirectory() {
		boolean existenceValue = false;
		File persistDir = getPersistDirectory();
		if (!persistDir.exists()) {
			persistDir.mkdir();
			existenceValue = true;
		} else {
			existenceValue = true;
		}
		return existenceValue;
	}



	public File getPersistDirectory() {
		return new File(PERSIST_DIRECTORY);
	}
	
	public DicomObject[] readAllObjects() throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		DicomObject[] outObjects = new DicomObject[fileNames.length];
		int counter = 0;
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			outObjects[counter] = inStream.readDicomObject();
			counter++;
			inStream.close();
			
		}
		return outObjects;
		
	}
	
	public DicomObject[] readAllObjectsOfType(String sopClassUID) throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		Collection<DicomObject> outObjects = new ArrayList<DicomObject>();
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			DicomObject dObject = inStream.readDicomObject();
			if (dObject.getString(Tag.SOPClassUID).equalsIgnoreCase(sopClassUID))
			{
				outObjects.add(dObject);
			}
			inStream.close();
		}
		return outObjects.toArray(new DicomObject[outObjects.size()]);
	}
	
	public DicomObject[] readMatchingObjects(String sopInstanceUID) throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		Collection<DicomObject> outObjects = new ArrayList<DicomObject>();
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			DicomObject dObject = inStream.readDicomObject();
			if (dObject.getString(Tag.SOPInstanceUID).equalsIgnoreCase(sopInstanceUID))
			{
				outObjects.add(dObject);
			}
			inStream.close();
		}
		return outObjects.toArray(new DicomObject[outObjects.size()]);
	}

	
	public String[] readAllFileNames() throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		return fileNames;
		
	}
	
	public String[] readAllFileNamesOfType(String sopClassUID) throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		Collection<String> matchedFiles = new ArrayList<String>();
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			DicomObject dObject = inStream.readDicomObject();
			if (dObject.getString(Tag.SOPClassUID).equalsIgnoreCase(sopClassUID))
			{
				matchedFiles.add(thisFile.getName());
			}
			inStream.close();
		}
		return matchedFiles.toArray(new String[matchedFiles.size()]);
	}
	
	public String[] readMatchingFileNames(String sopInstanceUID) throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		Collection<String> matchedFiles = new ArrayList<String>();
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			DicomObject dObject = inStream.readDicomObject();
			if (dObject.getString(Tag.SOPInstanceUID).equalsIgnoreCase(sopInstanceUID))
			{
				matchedFiles.add(thisFile.getName());
			}
			inStream.close();
		}
		return matchedFiles.toArray(new String[matchedFiles.size()]);
	}
	
	
	public boolean writeObject(DicomObject dObject) throws IOException {
		String instanceUid = dObject.getString(Tag.SOPInstanceUID);
		DicomOutputStream  outStream = new DicomOutputStream(new File(PERSIST_DIRECTORY+"\\FILE."+instanceUid));
		outStream.writeDataset(dObject, UID.ImplicitVRLittleEndian);
		outStream.close();
		return true;
	}
	
	public boolean deleteObject(String SOPinstanceUID) throws IOException {
		File persistDir = getPersistDirectory();
		String[] fileNames = persistDir.list();
		DicomObject[] outObjects = new DicomObject[fileNames.length];
		int counter = 0;
		for (String filename:fileNames) {
			File thisFile = new File(PERSIST_DIRECTORY+"\\"+filename);
			DicomInputStream inStream = new DicomInputStream(thisFile);
			DicomObject dObject = inStream.readDicomObject();
			if (dObject.getString(Tag.SOPInstanceUID).equalsIgnoreCase(SOPinstanceUID))
			{
				return thisFile.delete();
			}
		}
		return false;
	}

}
