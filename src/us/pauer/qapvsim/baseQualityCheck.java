package us.pauer.qapvsim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

// Extend this class to do action
/*
 * Anticipated states for Requester
 *   1) Waiting for User signal to Send Create UPS command to Perf
 *   T) User clicks button
 *   2) Waits for response that UPS was created
 *   T) Perf responds with UPS created
 *   
 *   
 * 
 * 
 * Anticipated states for Performer
 *   1) Waiting for UPS create command from Req
 *   T) Receives UPS Create Command
 *   2) Creating the UPS
 *   T) UPS is created
 *   3) Respond to Requester that UPS is created
 */

// This base class hooks the UI to the model
public class BaseQualityCheck extends Thread {
	protected CPQCUI ui;
	
	// connection information
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	protected connectionInfo local = new connectionInfo();
	protected connectionInfo remote = new connectionInfo();

	
	
	public BaseQualityCheck(String windowTitle, String localAETitle, String localIP, String localPort, 
							String remoteAETitle, String remoteIP, String remotePort) {
		
		ui = new CPQCUI(windowTitle);
		
		// Set connection information.
		local.aetitle = localAETitle;
		local.ip = localIP;
		local.port = localPort;
		remote.aetitle = remoteAETitle;
		remote.ip = remoteIP;
		remote.port = remotePort;		
		
	}



}
