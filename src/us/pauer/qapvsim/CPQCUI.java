package us.pauer.qapvsim;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CPQCUI extends JFrame {

        
		private Collection actionSet = new ArrayList<String>(); 
		private JComboBox actionBox;
		private String nextAction = "default";
        private String lastMessage = "<empty>";
        private String currentState = "Starting...";
        private StringBuffer fullmessage = new StringBuffer("Starting...\n");
        private JTextArea stateArea = new JTextArea(5, 10);
        private JTextArea messageArea = new JTextArea(10, 10);
        private JScrollPane scrollArea = new JScrollPane(messageArea);
        private Box mainBox = new Box(BoxLayout.Y_AXIS);
        private Box topBox = new Box(BoxLayout.X_AXIS);
        private JButton actionButton = new JButton();
        private JButton resetButton = new JButton();
        private JButton echoButton = new JButton("Verify SCP");

        


        public CPQCUI(String appTitle, ArrayList actionSet, boolean echoButtonNeeded) {
                super();
                setActionArray(actionSet);
                actionBox = new JComboBox(actionSet.toArray());
                topBox.add(actionBox);
                actionButton.setSize(50, 10);
                //default text will be overridden by other calls if need be
                actionButton.setText("Execute");
                topBox.add(actionButton);
                resetButton.setSize(50, 10);
                resetButton.setText("Reset");
                topBox.add(resetButton);
                resetButton.setEnabled(true);
                if (echoButtonNeeded) {
                	echoButton.setSize(50,100);
                	topBox.add(echoButton);
                }
                mainBox.add(topBox);
                mainBox.add(scrollArea);
                mainBox.add(stateArea);
                this.getContentPane().add(mainBox);
                this.setSize(300,300);
                this.setTitle(appTitle);
                this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
        
        public void setActionArray(ArrayList actionStrings) {
        	actionSet.addAll(actionStrings);
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
        
        public void setActionButtonListener(ActionListener aListener) {
        	actionButton.addActionListener(aListener);
        }
        
        public void setActionButtonEnabled(boolean enabled) {
        	actionButton.setEnabled(enabled);
        }
        
        public void setActionListEnabled(boolean enabled) {
        	actionBox.setEnabled(enabled);
        }

        public void setResetButtonListener(ActionListener aListener) {
                resetButton.addActionListener(aListener);
        }

		public void setActionBoxListener(ActionListener aListener) {
			actionBox.addActionListener(aListener);
			
		}

}