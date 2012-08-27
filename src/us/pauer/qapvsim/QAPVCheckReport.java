package us.pauer.qapvsim;

import org.dcm4che2.data.DicomObject;

public interface QAPVCheckReport {
	
	public void setOverallCheckPassed(String value) throws QualityReportComplianceException;
	
	public void setCompletionDateTime(String dataTime);
	
	public void setSummaryOfResult(String result);
	
	public void setPlanInstance(String planUID);
	
	public void addCheckResult(String checkLabel, String checkPassed, String checkDescription ) 
			throws QualityReportComplianceException;
	
	public void setUpsInstance(String upsUID);
	
	public void setCheckPerfomerKey(String key);
	
	public DicomObject getReportAsDicomObject();
}
