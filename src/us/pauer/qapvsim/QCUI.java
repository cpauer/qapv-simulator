package us.pauer.qapvsim;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
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

@SuppressWarnings("serial")
public class QCUI extends JFrame {

        
		private Collection<String> actionSet = new ArrayList<String>(); 
		private JComboBox actionBox;
        private String lastMessage = "<empty>";
        private String currentState = "Starting...";
        private StringBuffer fullmessage = new StringBuffer("Starting...\n");
        private JTextArea stateArea = new JTextArea(3, 2);
        private JTextArea messageArea = new JTextArea(20, 5);
        private JScrollPane scrollArea = new JScrollPane(messageArea);
        private Box mainBox = new Box(BoxLayout.Y_AXIS);
        private Box controlBox = new Box(BoxLayout.X_AXIS);
        private Box topBox = new Box(BoxLayout.X_AXIS);
        private JButton actionButton = new JButton();
        private JButton resetButton = new JButton();

        


        public QCUI(String appTitle, ArrayList<String> actionSet, int leftPosition) {
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
                mainBox.add(topBox);
                mainBox.add(controlBox);
                mainBox.add(scrollArea);
                mainBox.add(stateArea);
                this.getContentPane().add(mainBox);
                this.setSize(300,300);
                this.setTitle(appTitle);
                this.setDefaultCloseOperation(EXIT_ON_CLOSE);
                Toolkit tk = Toolkit.getDefaultToolkit();
                Dimension screenSize = tk.getScreenSize();
                final int height = screenSize.height;
                // Setup the frame accordingly
                this.setSize(450, 450);
                this.setLocation(leftPosition,height/4);                
        }
        
        public void setActionArray(ArrayList<String> actionStrings) {
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
		public void setControls(Collection<Component> controls) {
			for (Component cmpnt:controls) {
				controlBox.add(cmpnt);
			}
		}
		
		public void setEntryMessage(String action) {
			setLastMessage(">>>>>>>>>>>>>>>>>>>>>"+action+">>>>>>>>>>>>>>>>>>");
		}
		
		public void setExitMessage(String action) {
			setLastMessage("<<<<<<<<<<<<<<<<<<<<<"+action+"<<<<<<<<<<<<<<<<<<");
		}

}