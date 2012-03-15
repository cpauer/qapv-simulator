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
	
	private JLabel localAEText = new JLabel("AETITLE");
	private JLabel localIPText = new JLabel("localhost");
	private JLabel localPortText = new JLabel("4545");
	

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


	public QCUI(String appTitle, boolean actionButtonNeeded) {
		super();
		infoBox.add( Box.createHorizontalStrut(10));
		infoBox.add(localAETitleLabel);
		infoBox.add(localAEText);
		infoBox.add( Box.createHorizontalStrut(10));
		infoBox.add(new JSeparator(SwingConstants.VERTICAL));
		infoBox.add( Box.createHorizontalStrut(1));
		infoBox.add(localIPLabel);
		infoBox.add(localIPText);
		infoBox.add( Box.createHorizontalStrut(5));
		infoBox.add(new JSeparator(SwingConstants.VERTICAL));
		infoBox.add( Box.createHorizontalStrut(5));
		infoBox.add(localPortLabel);
		infoBox.add(localPortText);
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

	public void addTextToComboBox(String actionString)
	{
		System.out.println("ADDING:" + actionString);
		action.addItem(actionString);
	}
}
