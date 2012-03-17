package us.pauer.qapvsim;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class QCUI extends JFrame {

	private String nextAction = "default";
	private String lastMessage = "<empty>";
	private String currentState = "Starting...";
	private StringBuffer fullmessage = new StringBuffer("Starting...\n");
	
	private JTextArea messageArea = new JTextArea(10, 10);
	private JScrollPane scrollArea = new JScrollPane(messageArea);
	private Box mainBox = new Box(BoxLayout.Y_AXIS);
	//private Box topBox = new Box(BoxLayout.X_AXIS);
	
	
	private Box infoBox = new Box(BoxLayout.X_AXIS);
	private JLabel localAETitleLabel = new JLabel("Local AE Title: ");
	private JLabel localIPLabel = new JLabel("IP: ");
	private JLabel localPortLabel = new JLabel("PORT: ");
	
	private JTextField localAETitle = new JTextField(10);
	private JTextField localIP = new JTextField(10);
	private JTextField localPort = new JTextField(10);
	

	private JPanel remotePanel = new JPanel();
	
	private JLabel remoteAETitleLabel = new JLabel("Remote AE Title: ");
	private JLabel remoteIPLabel = new JLabel("IP: ");
	private JLabel remotePortLabel = new JLabel("PORT: ");
	private JTextField remoteAETitle = new JTextField(10); // remote AE Title
	private JTextField remoteIP = new JTextField(10);   // remote IP
	private JTextField remotePort = new JTextField(5);  // remote Port
	
	private Box actionBox = new Box(BoxLayout.X_AXIS);
	private JComboBox action = new JComboBox();
	private JButton execute = new JButton("Execute");
	
	
	private Box stateBox = new Box(BoxLayout.X_AXIS);
	private JLabel stateLabel = new JLabel("State: ");
	private JLabel state = new JLabel("waiting for user...");
	
	private JButton mainButton = new JButton();
	private JButton resetButton = new JButton();

	
	public QCUI(String appTitle) {
		this(appTitle, true);
	}

	public QCUI()
	{
		this("unknown", true);
	}
	
	public QCUI(String appTitle, boolean actionButtonNeeded) {
		super();
		infoBox.add( Box.createHorizontalStrut(10));
		infoBox.add(localAETitleLabel);
		infoBox.add(localAETitle);
		infoBox.add( Box.createHorizontalStrut(10));
		infoBox.add(new JSeparator(SwingConstants.VERTICAL));
		infoBox.add( Box.createHorizontalStrut(1));
		infoBox.add(localIPLabel);
		infoBox.add(localIP);
		infoBox.add( Box.createHorizontalStrut(5));
		infoBox.add(new JSeparator(SwingConstants.VERTICAL));
		infoBox.add( Box.createHorizontalStrut(5));
		infoBox.add(localPortLabel);
		infoBox.add(localPort);
		infoBox.add( Box.createHorizontalStrut(10));
		
		remotePanel.add(remoteAETitleLabel);
		remotePanel.add(remoteAETitle);
		remotePanel.add(remoteIPLabel);
		remotePanel.add(remoteIP);
		remotePanel.add(remotePortLabel);
		remotePanel.add(remotePort);
		
		actionBox.add(action);
		actionBox.add(execute);
		
		
		stateBox.add(stateLabel);
		stateBox.add(state);
		
		mainBox.add(infoBox);
		mainBox.add(remotePanel);
		mainBox.add(actionBox);
		mainBox.add(stateBox);
		mainBox.add(scrollArea);
		
		
		
		this.getContentPane().add(mainBox);
		this.setSize(500,300);
		this.setTitle(appTitle);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public String getNextAction() {
		return mainButton.getText();
	}
	
	public void setNextAction(String nextAction) {
		if (nextAction.equalsIgnoreCase("")) {
			//button should be disabled
			mainButton.setEnabled(false);
		} else {
			mainButton.setEnabled(true);
			mainButton.setText(nextAction);
			
		}
	}
	
	public String getLastMessage() {
		return lastMessage;
	}
	
	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
		fullmessage.append(lastMessage+"\n");
		messageArea.setText(fullmessage.toString());
	}
	
	public String getCurrentState() {
		return currentState;
	}
	
	public String getActionToExecute()
	{
		return action.getSelectedItem().toString();
	}
	
	
	public void setCurrentState(String currentState) {
		//stateArea.setText(currentState);
	}
	
	public void setExecuteButtonListener(ActionListener aListener) {
		execute.addActionListener(aListener);
	}

	public void setResetButtonListener(ActionListener aListener) {
		resetButton.addActionListener(aListener);
	}
	
	public String getRemoteAETitle()
	{
		return remoteAETitle.getText();
	}
	
	public void setRemoteAETitle(String aetitle){
		remoteAETitle.setText(aetitle);
	}
	
	public String getRemoteIP(){
		return remoteIP.getText();
	}
	
	public void setRemoteIP(String IP){
		remoteIP.setText(IP);
	}
	
	public String getRemotePortIP(){
		return remoteIP.getText();
	}
	
	public void setRemotePort(String Port){
		remotePort.setText(Port);
	}
	
	public String getLocalAETitle()
	{
		return localAETitle.getText();
	}
	
	public void setLocalAETitle(String aetitle){
		localAETitle.setText(aetitle);
	}
	
	public String getLocalIP(){
		return localIP.getText();
	}
	
	public void setLocalIP(String IP){
		localIP.setText(IP);
	}
	
	public String getLocalPort(){
		return localIP.getText();
	}
	
	public void setLocalPort(String Port){
		localPort.setText(Port);
	}
	
	public void addTextToComboBox(String actionString)
	{
		action.addItem(actionString);
	}
	
	public void setState(String stateString){
		state.setText(stateString);
	}
	
	public String getState(){
		return state.getText();
	}
	
	public void setButtonToExecute()
	{
		execute.setText("Execute");
	}
	
	public void setButtonToCancel()
	{
		execute.setText("Cancel");
	}
	
	public String getExecuteButtonText()
	{
		return execute.getText();
	}
}
