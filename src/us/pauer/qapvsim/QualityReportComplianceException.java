package us.pauer.qapvsim;

public class QualityReportComplianceException extends Exception {
	private String message;
	
	public QualityReportComplianceException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

}
