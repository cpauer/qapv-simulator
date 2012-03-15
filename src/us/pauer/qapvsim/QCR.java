package us.pauer.qapvsim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;




public class QCR {
	private QCUI ui;
	
	
	private static final int STATE_INIT = 0;
	private static final int STATE_CREATING = 1;
	private static final int STATE_CREATED = 2;
	private static final int STATE_SUBSCRIBING = 3;
	/*private static final int STATE_DETECT_COMPLETE = 4;
	private static final int STATE_GETTING_CHECK_STATUS = 5; */
	private static final int STATE_TOTAL = 4;
	
	int stateCounter = 0;
	
	String[] actions = new String[]{"Create UPS","", "Subscribe", ""};
	String[] messages = new String[]{"SCU will request a UPS create " +
			"when button is pushed", "Creating...",
			"SCU will subscribe to last created UPS when button is pushed.",
			"Subscribing..."};
	String[] stateMessages = new String[] {"Wating for User Input", "Creating UPS",
			"Waiting for User Input", "Subscribing"};

	ActionListener _qcrButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//stateCounter = (stateCounter+1) % STATE_TOTAL;
			System.out.println("IM HERE");
			updateUI();
			followThroughOnAction();
		}
		

		private void followThroughOnAction() {
			switch (stateCounter) {
			case STATE_CREATING: 
				for (int i=0; i<100000000; i++);
				stateCounter = (stateCounter+1) % STATE_TOTAL;
				updateUI();
				break;
			case STATE_SUBSCRIBING:
				for (int i=0; i<100000000; i++);
				stateCounter = (stateCounter+1) % STATE_TOTAL;
				updateUI();
				break;
			default:return;
			}
			
		}
	};

	ActionListener _qcrRestButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			stateCounter = 0;
			updateUI();
		}
	};

	
	

	public QCR(String scuae, String scuAddress, String scuPort, String scpae,
			String scpAddress, String scpPort) {
		ui = new QCUI("Quality Check Requester");
		ui.setExecuteButtonListener(_qcrButtonListener);
		updateUI();
		
	}

	public QCUI getUI() {
		return ui;
	}

	private void updateUI() {
		ui.setVisible(false);
		ui.setNextAction(actions[stateCounter]);
		ui.setLastMessage(messages[stateCounter]);
		ui.setCurrentState(stateMessages[stateCounter]);
		ui.setVisible(true);
	}

}
