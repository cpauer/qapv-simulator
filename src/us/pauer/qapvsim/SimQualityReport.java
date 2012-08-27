package us.pauer.qapvsim;  


import java.util.ArrayList;
import java.util.Date;    
import org.dcm4che2.data.BasicDicomObject;  
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;  
import org.dcm4che2.data.Tag;  
import org.dcm4che2.data.VR;  
import org.dcm4che2.util.UIDUtils;        

@SuppressWarnings("serial")
public class SimQualityReport extends BasicDicomObject implements QAPVCheckReport {                      
	private String patientName;          
	private String patientId;          
	private String DOB;          
	private String sex;   
	private String studyUid;
	private String seriesUid;
	private String overallCheckPassed;
	private String completionDateTime;
	private String summayrOfResult;
	private String planInstanceUid;
	private ArrayList checkList;
	private String upsInstanceUid;
	private String checkPerformerKey;
	private String UPSCreatorAE;
	//boolean marks if tags need to be set or reset
	boolean dicomFileIsDirtyOrUnspecified = true;
	private String planTypeUid;
	static final int SERIESNUMBER = 1;          
	static final String SRDOCUMENT = "SR Document";          
	static final String SRSOPCLASS = "1.2.840.10008.5.1.4.1.1.88.11";          
	static final int INSTANCENUMBER = 1;          
	static final String COMPLETIONSTRING = "COMPLETE"; // COMPLETE or PARTIAL          
	static final String VERIFICATIONFLAG = "UNVERIFIED"; // UNVERIFIED or VERIFIED          
	static final String TEMPLATEIDENTIFIER = "111111111";                    
	
	public SimQualityReport() {                  
	}                    
	
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setDOB(String dOB) {
		DOB = dOB;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setSex(String sex) {
		this.sex = sex;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setStudyUid(String studyUid) {
		this.studyUid = studyUid;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setSeriesUid(String seriesUid) {
		this.seriesUid = seriesUid;
		dicomFileIsDirtyOrUnspecified = true;
	}
	

	public void setOverallCheckPassed(String value)
			throws QualityReportComplianceException {
		if (!value.equalsIgnoreCase("YES") || !value.equalsIgnoreCase("NO")) {
			throw new QualityReportComplianceException(value+" does not match required type \"Yes\" or \"No\"");
		}
		this.overallCheckPassed = value;
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public void setCompletionDateTime(String dateTime) {
		this.completionDateTime = dateTime;
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public void setSummaryOfResult(String result) {
		this.summayrOfResult = result;
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public void setPlanInstance(String planUID) {
		this.planInstanceUid = planUID;
		dicomFileIsDirtyOrUnspecified = true;
	}

	public void setPlanType(String planClassUid) {
		this.planTypeUid = planClassUid;
		dicomFileIsDirtyOrUnspecified = true;
		
	}

	public void setUPSCreatorAe(String uPSCreatorAE2) {
		this.UPSCreatorAE = uPSCreatorAE2;
		dicomFileIsDirtyOrUnspecified = true;
	}

	
	public void addCheckResult(String checkLabel, String checkPassed,
			String checkDescription) throws QualityReportComplianceException {
		if (!checkPassed.equalsIgnoreCase("YES") || !checkPassed.equalsIgnoreCase("NO")) {
			throw new QualityReportComplianceException(checkPassed+" does not match required type \"Yes\" or \"No\"");
		}
		String[] result = new String[3];
		result[0] = checkLabel;
		result[1] = checkPassed;
		result[2] = checkDescription;
		this.checkList.add(result);
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public void setUpsInstance(String upsUID) {
		this.upsInstanceUid = upsUID;
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public void setCheckPerfomerKey(String key) {
		this.checkPerformerKey = key;
		dicomFileIsDirtyOrUnspecified = true;
	}
	
	public DicomObject getReportAsDicomObject() {
		if (dicomFileIsDirtyOrUnspecified) {
			dicomFileIsDirtyOrUnspecified = false;

			putString(Tag.SOPClassUID,VR.UI, SRSOPCLASS);                  
			putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID());          

			putDate(Tag.ContentDate, VR.DA, new Date());                  
			putDate(Tag.ContentTime, VR.DA, new Date());                  

			putString(Tag.PatientName,VR.PN,patientName);                  
			putString(Tag.PatientID, VR.LO, patientId);                  
			putString(Tag.PatientBirthDate,VR.DA, DOB);                  
			putString(Tag.PatientSex, VR.CS, sex);       
			
			if (studyUid==null) {
				putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());          
			} else {
				putString(Tag.StudyInstanceUID, VR.UI, studyUid);          
			}
			putString(Tag.StudyDate, VR.SH, "");
			putString(Tag.StudyTime, VR.SH, "");
			putString(Tag.ReferringPhysicianName, VR.LO, "");
			putString(Tag.StudyID, VR.SH, "");
			putString(Tag.AccessionNumber, VR.SH, "");
			
			putString(Tag.Manufacturer, VR.LO, "QAPVSimulator");            

			putString(Tag.Modality,VR.CS,SRDOCUMENT); 
			
			if (seriesUid==null) {
				putString(Tag.SeriesInstanceUID, VR.UI,UIDUtils.createUID() );                  
			} else {
				putString(Tag.SeriesInstanceUID, VR.UI, seriesUid);                  
			}
			putInt(Tag.SeriesNumber, VR.IS, SERIESNUMBER);                 

			
			putInt(Tag.InstanceNumber, VR.IS, INSTANCENUMBER);     
			putString(Tag.CompletionFlag, VR.CS, COMPLETIONSTRING); 
			putString(Tag.VerificationFlag, VR.CS, VERIFICATIONFLAG);

			DicomObject refSopSeq = new BasicDicomObject();
			refSopSeq.putString(Tag.ReferencedSOPClassUID, VR.UI, this.planTypeUid);
			refSopSeq.putString(Tag.SOPInstanceUID, VR.UI, this.planInstanceUid);
			DicomObject refSeriesSeq = new BasicDicomObject();
			refSeriesSeq.putString(Tag.SeriesInstanceUID, VR.UI, this.seriesUid);
			refSeriesSeq.putSequence(Tag.ReferencedSOPSequence);
			refSeriesSeq.putNestedDicomObject(Tag.ReferencedSOPSequence, refSopSeq);
			DicomObject pertSeq = new BasicDicomObject();
			pertSeq.putString(Tag.StudyInstanceUID, VR.UI, this.studyUid);
			pertSeq.putSequence(Tag.ReferencedSeriesSequence);
			pertSeq.putNestedDicomObject(Tag.ReferencedSeriesSequence, refSeriesSeq);
			putSequence(Tag.PertinentOtherEvidenceSequence);
			putNestedDicomObject(Tag.PertinentOtherEvidenceSequence, pertSeq);

			DicomObject refPerfProcSeq = new BasicDicomObject();
			putSequence(Tag.ReferencedPerformedProcedureStepSequence); 
			putNestedDicomObject(Tag.ReferencedPerformedProcedureStepSequence, refPerfProcSeq);
			
			putSequence(Tag.PerformedProcedureCodeSequence);
			
			putDate(Tag.ContentDate, VR.DA, new Date());
			putDate(Tag.ContentTime, VR.TM, new Date());
			
			putString(Tag.ValueType, VR.CS, "CONTAINER");
			DicomObject conceptNameCodeSequence = new BasicDicomObject();
			conceptNameCodeSequence.putString(Tag.CodeValue, VR.SH, "XXXX");
			conceptNameCodeSequence.putString(Tag.CodingSchemeDesignator, VR.SH, "IHE-RO");
			conceptNameCodeSequence.putString(Tag.CodeMeaning, VR.LO, "Radiotherapy Treatment Plan " +
					"Check Request Result");
			putSequence(Tag.ConceptNameCodeSequence);
			putNestedDicomObject(Tag.ConceptNameCodeSequence, conceptNameCodeSequence);
			putString(Tag.ContinuityOfContent, VR.CS, "SEPARATE"); 
			
			//content template sequence
			DicomObject contentTemplateSequence = new BasicDicomObject();
			contentTemplateSequence.putString(Tag.MappingResource, VR.CS, "IHE-RO");
			contentTemplateSequence.putString(Tag.TemplateIdentifier, VR.CS, "XXXX");
			putSequence(Tag.ContentTemplateSequence);
			putNestedDicomObject(Tag.ContentTemplateSequence, contentTemplateSequence);

			DicomElement contentseq = putSequence(Tag.ContentSequence, 10);
			DicomObject contentItem1 = createCodeContentItem("CONTAINS", "100000",
					"IHE-RO", "CRITICAL ISSUES FOUND", "R-0039", "SRT", "No");
			contentseq.addDicomObject(contentItem1);
			
			contentseq.addDicomObject(createDateTimeContentItem("CONTAINS", "100001",
					"IHE-RO", "DATETIME WHEN CHECK WAS COMPLETED", completionDateTime));
			
			contentseq.addDicomObject(createTextContentItem("CONTAINS", "100002",
					"IHE-RO", "SUMMARY OF RESULT", "Only one Informational issue: Primary Meterset out of bounds"));
			
			contentseq.addDicomObject(createUidContentItem("CONTAINS", "100003",
					"IHE-RO", "PLAN SOP INSTANCE UID", planInstanceUid));
			
			contentseq.addDicomObject(createTextContentItem("CONTAINS", "100004",
					"IHE-RO", "CHECK RESULTS", "1"));
			
			contentseq.addDicomObject(buildExampleDetailReportSequence());
			
			contentseq.addDicomObject(createUidContentItem("CONTAINS", "100005",
					"IHE-RO", "UPS SOP INSTANCE UID", upsInstanceUid));
			
			contentseq.addDicomObject(createTextContentItem("CONTAINS", "100006",
					"IHE-RO", "REQUESTER AETITLE", UPSCreatorAE));
			
			contentseq.addDicomObject(createTextContentItem("CONTAINS", "100007",
					"IHE-RO", "CHECK PERFORMER RESULT KEY", "QAPVCHECK01"));
			add(contentseq);
			
			
		}
		return this;
		
	}
	
	private DicomObject buildExampleDetailReportSequence() {
		DicomObject detailSeq = new BasicDicomObject();
		//example 1 - Upper Bound
		//example 2 - Lower Bound
		//example 3 - Range
		//example 4 - Tag Inconsistency - this is the critical issue
		//example 5 - equality
		//example 6 - Existence
		//example 7 - Non-Specific
		
		//example 1

		DicomElement contentseq = putSequence(Tag.ContentSequence, 10);
		
		contentseq.addDicomObject(createDateTimeContentItem("CONTAINS", "100001",
				"IHE-RO", "DATETIME WHEN CHECK WAS COMPLETED", completionDateTime));
		detailSeq.putSequence(Tag.ContentSequence);
		DicomObject contentTemplateSequence = new BasicDicomObject();
		contentTemplateSequence.putString(Tag.MappingResource, VR.CS, "IHE-RO");
		contentTemplateSequence.putString(Tag.TemplateIdentifier, VR.CS, "YYYY");
		detailSeq.putSequence(Tag.ContentTemplateSequence);
		detailSeq.putNestedDicomObject(Tag.ContentTemplateSequence, contentTemplateSequence);
		
		contentseq.addDicomObject( createUpperBoundExample());
		detailSeq.add(contentseq);
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createLowerBoundExample());
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createRangeExample());
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createTagInconsitencyExample());
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createEqualityExample());
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createExistenceExample());
	//	detailSeq.putNestedDicomObject(Tag.ContentSequence, createNonSpecificExample());
		return detailSeq;
	}



	private DicomObject createUpperBoundExample() {
		DicomElement detailSeq = putSequence(Tag.ContentSequence, 6);
		DicomObject upperBoundItemContentSeq = new BasicDicomObject();
		
		detailSeq.addDicomObject(createCodeContentItem("CONTAINS", "100101",
				"IHE-RO", "CRITICAL ISSUE FOUND", "R-0039", "SRT", "No"));

		detailSeq.addDicomObject(createCodeContentItem("CONTAINS", "100106",
				"IHE-RO", "INFORMATAIONAL ISSUE FOUND", "R-0038D", "SRT", "YES"));
		
		detailSeq.addDicomObject(createCodeContentItem("CONTAINS", "100117",
				"IHE-RO", "MAXIMUM ISSUE SEVERITY", "zzzz", "IHE-RO", "INFORMATIONAL"));

		detailSeq.addDicomObject(createTextContentItem("CONTAINS", "100102",
				"IHE-RO", "ASSESSMENT CODE", "74839"));

		detailSeq.addDicomObject(createCodeContentItem("CONTAINS", "100103",
				"IHE-RO", "ASSESSMENT REPORTING TYPE", "IHEROUB", "IHE-RO", "Upper Bound"));
		detailSeq.addDicomObject(createUBAssessSeq());
		
		upperBoundItemContentSeq.add(detailSeq);
		return upperBoundItemContentSeq;
	}



	private DicomObject createUBAssessSeq() {
		DicomObject assessSeq = new BasicDicomObject();
		assessSeq.putSequence(Tag.ContentSequence);
		
		assessSeq.putString(Tag.RelationshipType, VR.CS, "CONTAINS");
		
		DicomElement assessDetailseq = putSequence(Tag.ContentSequence, 7);

		assessDetailseq.addDicomObject(createCodeContentItem("CONTAINS", "100111", "IHE-RO", "VALUE UNITS", 
				"second", "UCUM", "second"));

		assessDetailseq.addDicomObject(createTextContentItem("CONTAINS", "100112", "IHE-RO", "UPPER BOUND VALUE", 
				"52"));
				
		assessDetailseq.addDicomObject(createTextContentItem("CONTAINS", "100114", "IHE-RO", "SUPPLIED ATTRIBUTE NAME", 
				"Specified Primary Meterset"));
				
		assessDetailseq.addDicomObject(createTextContentItem("CONTAINS", "100115", "IHE-RO", "SUPPLIED ATTRIBUTE TAG", 
				"(3008,0032)"));
				
		assessDetailseq.addDicomObject(createTextContentItem("CONTAINS", "100116", "IHE-RO", "SUPPLIED ATTRIBUTE VALUE", 
				"53.1"));
		assessSeq.add(assessDetailseq);
			
		return assessSeq;
	}

/*
	private DicomObject createNonSpecificExample() {
		// TODO Auto-generated method stub
		return null;
	}


	private DicomObject createExistenceExample() {
		// TODO Auto-generated method stub
		return null;
	}


	private DicomObject createTagInconsitencyExample() {
		// TODO Auto-generated method stub
		return null;
	}


	private DicomObject createEqualityExample() {
		// TODO Auto-generated method stub
		return null;
	}


	private DicomObject createRangeExample() {
		// TODO Auto-generated method stub
		return null;
	}


	private DicomObject createLowerBoundExample() {
		// TODO Auto-generated method stub
		return null;
	}
*/
	private DicomObject createCodeContentItem(String relation, String nameCodeVal, String nameCodeDesig, 
			String nameCodeMeaning, String codeVal,	String codeDesig, String codeMeaning) {
		DicomObject contentItem = new BasicDicomObject();
		contentItem.putString(Tag.RelationshipType, VR.CS, relation);
		contentItem.putString(Tag.ValueType, VR.CS, "CODE");
		DicomObject conceptNameCodeSequence = getConceptSequence(nameCodeVal, nameCodeDesig, nameCodeMeaning);
		contentItem.putSequence(Tag.ConceptNameCodeSequence);
		contentItem.putNestedDicomObject(Tag.ConceptNameCodeSequence, conceptNameCodeSequence);
		DicomObject conceptCodeSequence = getConceptSequence(codeVal, codeDesig, codeMeaning);
		contentItem.putSequence(Tag.ConceptCodeSequence);
		contentItem.putNestedDicomObject(Tag.ConceptCodeSequence, conceptCodeSequence);
		return contentItem;
	}

	private DicomObject createTextContentItem(String relation, String nameCodeVal, String nameCodeDesig, 
			String nameCodeMeaning, String codeVal) {
		DicomObject contentItem = new BasicDicomObject();
		contentItem.putString(Tag.RelationshipType, VR.CS, relation);
		contentItem.putString(Tag.ValueType, VR.CS, "TEXT");
		DicomObject conceptNameCodeSequence = getConceptSequence(nameCodeVal, nameCodeDesig, nameCodeMeaning);
		contentItem.putSequence(Tag.ConceptNameCodeSequence);
		contentItem.putNestedDicomObject(Tag.ConceptNameCodeSequence, conceptNameCodeSequence);
		contentItem.putString(Tag.TextValue, VR.UT, codeVal);
		return contentItem;
	}
	
	private DicomObject createDateTimeContentItem(String relation, String nameCodeVal, String nameCodeDesig, 
			String nameCodeMeaning, String codeVal) {
		DicomObject contentItem = new BasicDicomObject();
		contentItem.putString(Tag.RelationshipType, VR.CS, relation);
		contentItem.putString(Tag.ValueType, VR.CS, "TEXT");
		DicomObject conceptNameCodeSequence = getConceptSequence(nameCodeVal, nameCodeDesig, nameCodeMeaning);
		contentItem.putSequence(Tag.ConceptNameCodeSequence);
		contentItem.putNestedDicomObject(Tag.ConceptNameCodeSequence, conceptNameCodeSequence);
		contentItem.putString(Tag.DateTime, VR.DT, codeVal);
		return contentItem;
	}

	private DicomObject createUidContentItem(String relation, String nameCodeVal, String nameCodeDesig, 
			String nameCodeMeaning, String codeVal) {
		DicomObject contentItem = new BasicDicomObject();
		contentItem.putString(Tag.RelationshipType, VR.CS, relation);
		contentItem.putString(Tag.ValueType, VR.CS, "TEXT");
		DicomObject conceptNameCodeSequence = getConceptSequence(nameCodeVal, nameCodeDesig, nameCodeMeaning);
		contentItem.putSequence(Tag.ConceptNameCodeSequence);
		contentItem.putNestedDicomObject(Tag.ConceptNameCodeSequence, conceptNameCodeSequence);
		contentItem.putString(Tag.DateTime, VR.DT, codeVal);
		return contentItem;
	}
	
	
	private DicomObject getConceptSequence(String codeValue, String scheme, String meaning) {
		DicomObject conceptSeq = new BasicDicomObject();
		conceptSeq.putString(Tag.CodeValue, VR.SH, codeValue);
		conceptSeq.putString(Tag.CodingSchemeDesignator, VR.SH, scheme);
		conceptSeq.putString(Tag.CodeMeaning, VR.LO, meaning);
		return conceptSeq;
	}

}    


