package us.pauer.qapvsim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

// Extend this class to do action
abstract class baseActionClass
{
	public void doAction(){
		System.out.println("Implement Action");
	}
	
	public void cancelAction(){
		System.out.println("Implment Cancel Action");
	}
}

// This base class hooks the UI to the model
public class baseQualityCheck {
	private QCUI ui;
	
	// connection information
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	private connectionInfo local = new connectionInfo();
	private connectionInfo remote = new connectionInfo();
	
	// Map description to action
	private Map<String,baseActionClass> event = new LinkedHashMap<String,baseActionClass>();
	
	// ActionListner to receive events from the execute button.
	ActionListener _executeButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		
			String actionToExecute = ui.getActionToExecute();			
			baseActionClass action = event.get(actionToExecute);
	
			action.doAction();
		}
	};
	
	// base constructor.  Create just the UI.
	public baseQualityCheck()
	{	
		ui = new QCUI();
	}
	
	public baseQualityCheck(String localAETitle, String localIP, String localPort, 
							String remoteAETitle, String remoteIP, String remotePort){
		
		// call base constructor which creates UI
		this();
		
		// Set connection information.
		ui.setLocalAETitle(localAETitle);
		ui.setLocalIP(localIP);
		ui.setLocalPort(localPort);
		ui.setRemoteAETitle(remoteAETitle);
		ui.setRemoteIP(remoteIP);
		ui.setRemotePort(remotePort);		
		
	}

	public void start() {
		
		ui.setExecuteButtonListener(_executeButtonListener);
		ui.setVisible(true);
		
	}
	public void setFrameTitle(String title)
	{
		ui.setTitle(title);
	}
	
	public QCUI getUI() {
		return ui;
	}
		
	// Adds the description and event 
	public void addAction(String stringAction, baseActionClass myClass)
	{
		event.put(stringAction, myClass);
		ui.addTextToComboBox(stringAction);
	}
}
