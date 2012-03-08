package us.pauer.qapvsim;

import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

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

	
	public QCUI(String appTitle) {
		this(appTitle, true);
	}


	public QCUI(String appTitle, boolean actionButtonNeeded) {
		super();
		if (actionButtonNeeded) {
			mainButton.setSize(50, 10);
			topBox.add(mainButton);
		}
		resetButton.setSize(50, 10);
		resetButton.setText("Reset");
		topBox.add(resetButton);
		resetButton.setEnabled(true);
		mainBox.add(topBox);
		mainBox.add(scrollArea);
		mainBox.add(stateArea);
		this.getContentPane().add(mainBox);
		this.setSize(300,300);
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
