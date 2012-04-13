package us.pauer.qapvsim;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DataWriter;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.DimseRSPHandler;


public class AssociationWithLog {
	
	Association a;
	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/QCR/";
	static final String DEFAULT_DCM_TRACK_LOCATION = DEFAULT_PERSIST_LOCATION+"TRAFFIC/";

	String logDirectory = DEFAULT_DCM_TRACK_LOCATION;
	


	public AssociationWithLog(Association connect) {
		a = connect;
	}

	public void setLogDirectory(String defaultDcmTrackLocation) {
		logDirectory = defaultDcmTrackLocation;
	}

	public DimseRSP cecho() throws IOException, InterruptedException {
		DimseRSP returnObject = a.cecho();
		log("echo",returnObject);
		return returnObject;
	}

	public void log(String message, Object returnObject) {
		try {
			DicomObject command = null;
			DicomObject dataset = null;
			String timeSig = Long.toString(Calendar.getInstance().getTimeInMillis()).substring(5);
			if (returnObject instanceof DicomObject) {
				dataset = (DicomObject)returnObject;
			}
			if (returnObject instanceof DimseRSP) {
				DimseRSP dr = (DimseRSP)returnObject;
				command = dr.getCommand();
				dataset = dr.getDataset();
			}
			if (command!=null) {
				String instanceUid = command.getString(Tag.SOPInstanceUID);
				String objectType = "Command";
				DicomOutputStream  outStream = new DicomOutputStream(new File(logDirectory+
						message+"."+objectType+"."+timeSig+".dcm"));
				outStream.writeDataset(command, UID.ImplicitVRLittleEndian);
				outStream.close();
				
			}
			if (dataset!=null) {
				String instanceUid = dataset.getString(Tag.SOPInstanceUID);
				String objectType = "dataset";
				DicomOutputStream  outStream = new DicomOutputStream(new File(logDirectory+
						message+"."+objectType+"."+timeSig+".dcm"));
				outStream.writeDataset(dataset, UID.ImplicitVRLittleEndian);
				outStream.close();
				
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
	}

	public DimseRSP ncreate(String abstractSyntaxUID, String sopClassUid,
			String instanceUid, DicomObject attrs, String transferSyntaxUid) throws IOException, InterruptedException {
		log("ncreate",attrs);
		DimseRSP rObj = a.ncreate(abstractSyntaxUID, sopClassUid, instanceUid, attrs, transferSyntaxUid);
		log("ncreate",rObj);
		return rObj;
	}

	public void release(boolean b) throws InterruptedException {
		a.release(b);
		
	}

	public DimseRSP naction(String abstractSyntaxUID, String sopClassUid,
			String uidForSubscription, int i, DicomObject subscribeObject,
			String transferSyntaxUid) throws IOException, InterruptedException {
		log("naction", subscribeObject);
		DimseRSP dr = a.naction(abstractSyntaxUID, sopClassUid, uidForSubscription, i, subscribeObject, transferSyntaxUid);
		log("naction",dr);
		return dr;
	}

	public DimseRSP cmove(String abstractSyntaxUID, String sopClassUid, int i,
			DicomObject attrs, String transferSyntaxUid, String aetitle) throws IOException, InterruptedException {
		log("cmove", attrs);
		DimseRSP dr = a.cmove(abstractSyntaxUID, sopClassUid, i, attrs, transferSyntaxUid, aetitle);
		log("cmove", dr);
		return dr;
	}
	
	public void cmove(String sopClassUid, int i, DicomObject keys, String transferSyntaxUid, String aetitle,
			DimseRSPHandler handler) throws IOException, InterruptedException {
		log("cmove", keys);
		a.cmove(sopClassUid, i, keys, transferSyntaxUid, aetitle, handler);
	}

	public void waitForDimseRSP() throws InterruptedException {
		a.waitForDimseRSP();
	}

	public DimseRSP cstore(String sopClassUid, String instanceUid,
			int priority, String moveOriginatorAET, int moveOriginatorMsgId,
			DataWriter storeWriter, String transferSyntaxUid) throws IOException, InterruptedException {
		return a.cstore(sopClassUid, instanceUid, priority, moveOriginatorAET, moveOriginatorMsgId,  storeWriter, transferSyntaxUid);
	}

	public void nevent(String sopClassUid, String instanceUid, int i,
			DicomObject attrs, String transferSyntaxUid, DimseRSPHandler handler) 
					throws IOException, InterruptedException {
		log("nevent", attrs);
		a.nevent(sopClassUid, instanceUid, i, attrs, transferSyntaxUid, handler);
	}

	public DimseRSP nget(String sopClassUid, String upsUid, int[] tags) throws IOException, InterruptedException {
		return a.nget(sopClassUid, upsUid, tags);
	}

}
