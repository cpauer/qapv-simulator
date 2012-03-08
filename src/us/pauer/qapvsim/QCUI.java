package us.pauer.qapvsim;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class QCUI extends JFrame {

	private String nextAction = "default";
	private String lastMessage = "<empty>";
	private String currentState = "Starting...";
	private StringBuffer fullmessage = new StringBuffer("Starting...\n");
	private JTextArea stateArea = new JTextArea(5, 10);
	private JTextArea messageArea = new JTextArea(10, 10);
	private JScrollPane scrollArea = new JScrollPane(messageArea);
	private Box mainBox = new Box(BoxLayout.Y_AXIS);
	private Box topBox = new Box(BoxLayout.X_AXIS);
	private JButton mainButton = new JButton();
	private JButton resetButton = new JButton();
	
		// Client A
	private JComboBox ActionComboBox = new JComboBox();
	private JLabel ActionLabel = new JLabel("Action: ");
	private JPanel NorthPanelA = new JPanel();
	private JButton ExecuteButtonA = new JButton("Execute");
	private JLabel StateLabelA = new JLabel("State: ");
	private GridLayout NorthGridA = new GridLayout(2,3);
	
	
	private JButton StartButton = new JButton("Start");
	private JButton StopButton = new JButton("Stop");
	private JPanel Panel_Button = new JPanel();
	
	private JLabel AETitleLabel = new JLabel("AE Title: ");
	private JTextField AETitleTextField = new JTextField(10);
	private JLabel PortLabel = new JLabel("Port: ");
	private JTextField PortTextField = new JTextField(5);
	private JPanel Panel_SCU = new JPanel();
	
	private JPanel Panel_SCP = new JPanel();
	private JLabel IPLabel_SCP = new JLabel("IP: ");
	private JTextField IPTextField_SCP = new JTextField(10);
	private JLabel AETitleLabel_SCP = new JLabel("AE Title: ");
	private JTextField AETitleTextField_SCP = new JTextField(10);
	private JLabel PortLabel_SCP = new JLabel("Port: ");
	private JTextField PortTextField_SCP = new JTextField(5);
	
	
	private JPanel StatePanel = new JPanel();
	private JLabel StateLabel = new JLabel("State: ");
	private JTextField StateTextField = new JTextField();
	
	private Box Box_SCP = new Box(BoxLayout.X_AXIS);
	private Box Box_SCU = new Box(BoxLayout.X_AXIS);
	private Box NorthButton = new Box(BoxLayout.X_AXIS);
	
	public QCUI(String appTitle) {
		this(appTitle, true);
	}


	public QCUI(String appTitle, boolean actionButtonNeeded) {
		super();
		
		
		Panel_Button.add(StartButton);
		Panel_Button.add(StopButton);
		
		//SCUInfoPanel.add(AETitleLabel);
		//SCUInfoPanel.add(AETitleTextField);
		//SCUInfoPanel.add(PortLabel);
		//SCUInfoPanel.add(PortTextField);
		Panel_SCU.add(AETitleLabel);
		Panel_SCU.add(AETitleTextField);
		Panel_SCU.add(PortLabel);
		Panel_SCU.add(PortTextField);
		
		//NorthButton.add(StartButton);
		//NorthButton.add(Box.createHorizontalStrut(50));
		//NorthButton.add(StopButton);
		
		Panel_SCP.add(IPLabel_SCP);
		Panel_SCP.add(IPTextField_SCP);
		Panel_SCP.add(AETitleLabel_SCP, BorderLayout.SOUTH);
		Panel_SCP.add(AETitleTextField_SCP);
		Panel_SCP.add(PortLabel_SCP);
		Panel_SCP.add(PortTextField_SCP);
		
		StatePanel.add(StateLabel);
		StatePanel.add(StateTextField);
		
		mainBox.add(Panel_Button);
		mainBox.add(Panel_SCP);
		mainBox.add(Panel_SCU);
		
		//mainBox.add(Panel_SCP);
		
		//mainBox.add(topBox);
		//mainBox.add(scrollArea);
		//mainBox.add(stateArea);
		//mainBox.add(Button);
		//mainBox.add(SCUInfoPanel,BorderLayout.SOUTH);
		//mainBox.add(Panel_SCP, BorderLayout.WEST);
		//mainBox.add(StateBox);
		add(mainBox);
		//add(SCUInfoPanel,BorderLayout.NORTH);
		//add(Panel_SCP);
		
		this.setSize(500,200);
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
	
	public void setCurrentState(String currentState) {
		stateArea.setText(currentState);
	}
	
	public void setMainButtonListener(ActionListener aListener) {
		mainButton.addActionListener(aListener);
	}

	public void setResetButtonListener(ActionListener aListener) {
		resetButton.addActionListener(aListener);
	}

}
