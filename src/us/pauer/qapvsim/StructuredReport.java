package us.pauer.qapvsim;  

import java.io.File;  
import java.io.IOException;  
import java.util.Date;    
import java.util.Locale;

import org.dcm4che2.data.BasicDicomObject;  
import org.dcm4che2.data.DicomObject;  
import org.dcm4che2.data.Tag;  
import org.dcm4che2.data.UID;  
import org.dcm4che2.data.VR;  
import org.dcm4che2.io.DicomOutputStream;  
 
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
	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/SCP/";                    
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	
	public void persistMe(String location) {          
		System.out.println("in persistMe");                  
		try {                          
			String instanceUid = this.getString(Tag.SOPInstanceUID);                          
			DicomOutputStream  outStream = new DicomOutputStream(new File(location
					+"/SR"+instanceUid+".dcm"));                          
			outStream.writeDataset(this, UID.ImplicitVRLittleEndian);                          
			outStream.close();                  
			System.out.println("out persistMe");                  
		} catch (IOException e) {                          
			System.out.println(e.getMessage());                  
		}          
	}                    
	
	public StructuredReport(String studyUid, String seriesUid) {                  
		putPatient();                  
		putGeneralStudy(studyUid);                  
		putSRDocumentSeries(seriesUid);                  
		putGeneralEquipment();                  
		putSOPCommon();                  
		putSRDocumentGeneral();                  
		putSRDocumentContent();                  
	}                    
	
	
	public void putPatient() {                  
		putString(Tag.PatientName,VR.PN,PATIENTNAME);                  
		putString(Tag.PatientID, VR.SS, PATIENTID);                  
		putString(Tag.PatientBirthDate,VR.DA,DOB);                  
		putString(Tag.PatientSex, VR.CS, SEX);          
	}                    
	
	
	public void putGeneralStudy(String studyUid) {
		if (studyUid==null) {
			putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());          
		} else {
			putString(Tag.StudyInstanceUID, VR.UI, studyUid);          
		}
	}                    
	
	
	public void putSRDocumentSeries(String seriesUid) {                  
		putString(Tag.Modality,VR.CS,SRDOCUMENT);       
		if (seriesUid==null) {
			putString(Tag.SeriesInstanceUID, VR.UI,UIDUtils.createUID() );                  
		} else {
			putString(Tag.SeriesInstanceUID, VR.UI, seriesUid);                  
		}
		putInt(Tag.SeriesNumber, VR.IS, SERIESNUMBER);                 
	}                    
	
	
	public void putGeneralEquipment() {                  
		putString(Tag.Manufacturer, VR.LO, "QAPV");            
	}            
	
	
	public void putSOPCommon() {                  
		putString(Tag.SOPClassUID,VR.UI, SRSOPCLASS);                  
		//bd.putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID(SRSOPCLASS));                  
		putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID());          
	}       
	
	public void putSRDocumentGeneral()  {                                    
		putInt(Tag.InstanceNumber, VR.IS, INSTANCENUMBER);                  
		putString(Tag.CompletionFlag, VR.CS, VERIFICATIONFLAG);                  
		putDate(Tag.ContentDate, VR.DA, new Date());                  
		putDate(Tag.ContentTime, VR.DA, new Date());                  
		putSequence(Tag.PerformedProcedureCodeSequence); 
	}                    
	
	public void putContainerMacro(DicomObject bd)  {                  
		bd.putString(Tag.ContinuityOfContent, VR.CS, "SEPERATE");                  
		bd.putSequence(Tag.ContentTemplateSequence); 
		DicomObject contentTemplateSequence = new BasicDicomObject();  
		contentTemplateSequence.putString(Tag.MappingResource, VR.CS, "DCMR"); 
		contentTemplateSequence.putString(Tag.TemplateIdentifier, VR.CS, TEMPLATEIDENTIFIER);
		bd.putNestedDicomObject(Tag.ContentTemplateSequence, contentTemplateSequence);
	} 
	
	public void putCodeSequenceMacro(DicomObject bd, String CodeValue, String CodingSchemeDesignator, String CodeMeaning)  {                    
		bd.putString(Tag.CodeValue, VR.SH, CodeValue);              
		bd.putString(Tag.CodingSchemeDesignator, VR.SH, CodingSchemeDesignator);              
		bd.putString(Tag.CodeMeaning, VR.LO, CodeMeaning);    
	}                    
	
	
	public void putDocumentContentMacro(DicomObject bd, String ValueType, 
			String CodeValue, String CodingSchemeDesignator, String CodeMeaning) {                                    
		bd.putString(Tag.ValueType, VR.CS, ValueType);                  
		bd.putSequence(Tag.ConceptNameCodeSequence);                  
		BasicDicomObject ConceptNameCodeSequence = new BasicDicomObject();                                    
		putCodeSequenceMacro(ConceptNameCodeSequence, CodeValue, CodingSchemeDesignator, CodeMeaning);                                    
		bd.putNestedDicomObject(Tag.ConceptNameCodeSequence, ConceptNameCodeSequence);                                    
		if (ValueType == "CONTAINER")  {                          
			putContainerMacro(bd);                    
		}                                              
	}                    
	
	
	public void addContent(DicomObject bd, String Relationship, String ValueType, 
			String CodeValue, String CodingSchemeDesignator, String CodeMeaning)   {                                    
		DicomObject ContentSequence = new BasicDicomObject();                                    
		ContentSequence.putString(Tag.RelationshipType, VR.CS,Relationship);                  
		putDocumentContentMacro(ContentSequence,ValueType,CodeValue, CodingSchemeDesignator, CodeMeaning);                  
		bd.putNestedDicomObject(Tag.ContentSequence,ContentSequence);                            
	}                    
	
	
	public void putSRDocumentContent()  {                                    
		putDocumentContentMacro(this, "CONTAINER", "99999", "IHE-RO", "Root Container");                                    
		putSequence(Tag.ContentSequence);                                    
		addContent(this,"HAS CONCEPT MOD", "CODE", "100000", "IHE-RO", "Check Passed");                  
		//addContent(bd,"HAS CONCEPT MOD", "DATETIME", "100001", "IHE-RO", new Date());                                                     
	}                       
}    

