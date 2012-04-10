package us.pauer.qapvsim;
import java.io.File;
import java.io.IOException;
import java.util.Date;

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



public class StructuredReport extends BasicDicomObject{

	
	static String PATIENTNAME = "Simpson^Homer^Jay";
	static String PATIENTID = "123456789";
	static String DOB = "19560510";
	static String SEX = "M";
	static int SERIESNUMBER = 1;
	static String SRDOCUMENT = "SR Document";
	static String SRSOPCLASS = "1.2.840.10008.5.1.4.1.1.88.11";
	static int INSTANCENUMBER = 1;
	static String COMPLETIONSTRING = "COMPLETE"; // COMPLETE or PARTIAL
	static String VERIFICATIONFLAG = "UNVERIFIED"; // UNVERIFIED or VERIFIED
	static String TEMPLATEIDENTIFIER = "111111111";
	
	public DicomObject srIOD;
	
	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/SCP/";
	
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	
	
	private void persistUPS(DicomObject upsObject) {
    	System.out.println("in persistUPS");
		try {
			String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
			DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
					"/SR."+instanceUid+".dcm"));
			outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
			outStream.close();
        	System.out.println("out persistUPS");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public StructuredReport()
	{
		srIOD = new BasicDicomObject();
		putPatient(srIOD);
		putGeneralStudy(srIOD);
		putSRDocumentSeries(srIOD);
		putGeneralEquipment(srIOD);
		putSOPCommon(srIOD);
		putSRDocumentGeneral(srIOD);
		putSRDocumentContent(srIOD);
		//persistUPS(srIOD);
		
	}
	
	public void putPatient(DicomObject bd)
	{
		bd.putString(Tag.PatientName,VR.PN,PATIENTNAME);
		bd.putString(Tag.PatientID, VR.SS, PATIENTID);
		bd.putString(Tag.PatientBirthDate,VR.DA,DOB);
		bd.putString(Tag.PatientSex, VR.CS, SEX);
	}
	
	public void putGeneralStudy(DicomObject bd)
	{
		bd.putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
	}
	
	public void putSRDocumentSeries(DicomObject bd)
	{
		bd.putString(Tag.Modality,VR.CS,SRDOCUMENT);
		bd.putString(Tag.SeriesInstanceUID, VR.UI,UIDUtils.createUID() );
		bd.putInt(Tag.SeriesNumber, VR.IS, SERIESNUMBER);	
	}
	
	public void putGeneralEquipment(DicomObject bd)
	{
		bd.putString(Tag.Manufacturer, VR.LO, "QAPV");	
	}

	public void putSOPCommon(DicomObject bd)
	{
		bd.putString(Tag.SOPClassUID,VR.UI, SRSOPCLASS);
		//bd.putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID(SRSOPCLASS));
		bd.putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID());
	}
	
	public void putSRDocumentGeneral(DicomObject bd)
	{
		
		bd.putInt(Tag.InstanceNumber, VR.IS, INSTANCENUMBER);
		bd.putString(Tag.CompletionFlag, VR.CS, VERIFICATIONFLAG);
		bd.putDate(Tag.ContentDate, VR.DA, new Date());
		bd.putDate(Tag.ContentTime, VR.DA, new Date());
		bd.putSequence(Tag.PerformedProcedureCodeSequence);
		
	}
	
	public void putContainerMacro(DicomObject bd)
	{
		bd.putString(Tag.ContinuityOfContent, VR.CS, "SEPERATE");
		bd.putSequence(Tag.ContentTemplateSequence);
		
		DicomObject ContentTemplateSequence = new BasicDicomObject();
		ContentTemplateSequence.putString(Tag.MappingResource, VR.CS, "DCMR");
		ContentTemplateSequence.putString(Tag.TemplateIdentifier, VR.CS, TEMPLATEIDENTIFIER);
		
	}
	
	public void putCodeSequenceMacro(DicomObject bd, String CodeValue, String CodingSchemeDesignator, String CodeMeaning)
	{

		bd.putString(Tag.CodeValue, VR.SH, CodeValue);
	    bd.putString(Tag.CodingSchemeDesignator, VR.SH, CodingSchemeDesignator);
	    bd.putString(Tag.CodeMeaning, VR.LO, CodeMeaning);
	}
	
	public void putDocumentContentMacro(DicomObject bd, String ValueType, String CodeValue, String CodingSchemeDesignator, String CodeMeaning)
	{
		
		bd.putString(Tag.ValueType, VR.CS, ValueType);
		bd.putSequence(Tag.ConceptNameCodeSequence);
		BasicDicomObject ConceptNameCodeSequence = new BasicDicomObject();
		
		putCodeSequenceMacro(ConceptNameCodeSequence, CodeValue, CodingSchemeDesignator, CodeMeaning);
		
		bd.putNestedDicomObject(Tag.ConceptNameCodeSequence, ConceptNameCodeSequence);
		
		if (ValueType == "CONTAINER")
		{
			putContainerMacro(bd);	
		}
			
	
	}
	
	public void addContent(DicomObject bd, String Relationship, String ValueType, String CodeValue, String CodingSchemeDesignator, String CodeMeaning)
	{
		
		DicomObject ContentSequence = new BasicDicomObject();
		
		ContentSequence.putString(Tag.RelationshipType, VR.CS,Relationship);
		putDocumentContentMacro(ContentSequence,ValueType,CodeValue, CodingSchemeDesignator, CodeMeaning);
		bd.putNestedDicomObject(Tag.ContentSequence,ContentSequence);
		
	}
	
	public void putSRDocumentContent(DicomObject bd)
	{
		
		putDocumentContentMacro(bd,"CONTAINER", "99999", "IHE-RO", "Root Container");
		
		bd.putSequence(Tag.ContentSequence);
		
		addContent(bd,"HAS CONCEPT MOD", "CODE", "100000", "IHE-RO", "Check Passed");
		//addContent(bd,"HAS CONCEPT MOD", "DATETIME", "100001", "IHE-RO", new Date());
		
		       
	}
	
	 
}
