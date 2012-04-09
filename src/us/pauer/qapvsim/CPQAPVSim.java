/* ***** BEGIN LICENSE BLOCK *****
* 
*	QAPVSim is a program to simulate and test the 
*	Quality Assurance with Plan Veto profile of IHE-RO
*	
*	
*    Copyright (C) 2012  Chris Pauer
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
** ***** END LICENSE BLOCK ***** */

package us.pauer.qapvsim;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;


public class CPQAPVSim extends JFrame{

	private static final String TYPE_REQUESTER = "REQUESTER";
	private static final String TYPE_PROVIDER = "PROVIDER";
	
	private static final String REQ_AE_DEFAULT = "QCR";
	private static final String REQ_IP_DEFAULT = "localhost";
	private static final String REQ_PORT_DEFAULT = "40404";
	
	private static final String PER_AE_DEFAULT = "QCP";
	private static final String PER_IP_DEFAULT = "localhost";
	private static final String PER_PORT_DEFAULT = "40405";

	
	private String _scuAe;
	private String _scuAddress;
	private String _scuPort;
	private String _scpAe;
	private String _scpAddress;
	private String _scpPort;
	private String _actorType;
	
	private QualityCheckRequester _QCR;
	private QualityCheckPerformer _QCP;


	private JButton startRequestorButton = new JButton("Start Requestor");
	private JButton startPerformerButton = new JButton("Start Performer");
	private Box buttonBox = new Box(BoxLayout.X_AXIS);
	
	private Box mainBox = new Box(BoxLayout.Y_AXIS);
	private Box text1Box = new Box(BoxLayout.X_AXIS);
	private Box text2Box = new Box(BoxLayout.X_AXIS);
	
	private Box infoBox = new Box(BoxLayout.Y_AXIS);
	private JTextField reqAETextField = new JTextField(REQ_AE_DEFAULT, 20);
	private JTextField reqIPTextField = new JTextField(REQ_IP_DEFAULT, 10);
	private JTextField reqPortTextField = new JTextField(REQ_PORT_DEFAULT, 10);
	
	private JTextField perAETextField = new JTextField(PER_AE_DEFAULT, 20);
	private JTextField perIPTextField = new JTextField(PER_IP_DEFAULT, 10);
	private JTextField perPortTextField = new JTextField(PER_PORT_DEFAULT, 10);
	
	
	private JLabel aetitleText = new JLabel("AE Title");
	private JLabel ipText = new JLabel("IP");
	private JLabel portText = new JLabel("Port");
	
	private JLabel aetitleText2 = new JLabel("AE Title");
	private JLabel ipText2 = new JLabel("IP");
	private JLabel portText2 = new JLabel("Port");
	
	ActionListener requestorActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			_QCR = new QualityCheckRequester(reqAETextField.getText(),
					reqIPTextField.getText(),
					reqPortTextField.getText(),
					perAETextField.getText(),
					perIPTextField.getText(),
					perPortTextField.getText());
			_QCR.start();
		}
	};
	ActionListener performerActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			_QCP = new QualityCheckPerformer(
					perAETextField.getText(),
					perIPTextField.getText(),
					perPortTextField.getText(),
					reqAETextField.getText(),
					reqIPTextField.getText(),
					reqPortTextField.getText());
			_QCP.start();
		
		}
	};
	
	
	public CPQAPVSim() {
		this.setSize(300,100);
		this.setTitle("QAPV Simulator");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final int width = screenSize.width;
        final int height = screenSize.height;
        // Setup the frame accordingly
        this.setLocation(width/2,height/2);                
		
		
		startRequestorButton.addActionListener(requestorActionListener);
		startPerformerButton.addActionListener(performerActionListener);
		buttonBox.add(startRequestorButton);
		
		
		buttonBox.add(startPerformerButton);
		
		text1Box.add(aetitleText);
		text1Box.add(reqAETextField);
		text1Box.add(ipText);
		text1Box.add(reqIPTextField);
		text1Box.add(portText);
		text1Box.add(reqPortTextField);
		
		text2Box.add(aetitleText2);
		text2Box.add(perAETextField);
		text2Box.add(ipText2);
		text2Box.add(perIPTextField);
		text2Box.add(portText2);
		text2Box.add(perPortTextField);
		
		infoBox.add(buttonBox);
		infoBox.add(text1Box);
		infoBox.add(text2Box);
		
		this.getContentPane().add(infoBox);
		
		this.setVisible(true);
		
		
		
		// TODO Auto-generated constructor stub
	}
	
	
	public static void main(String[] args)
	{
		CPQAPVSim qa = new CPQAPVSim();
		

	}
	

	public String getActorType() {
		return _actorType;
	}

	public String getSCUAE() {
		return _scuAe;
	}

	public String getSCUAddress() {
		return _scuAddress;
	}

	public String getSCUPort() {
		return _scuPort;
	}

	public String getSCPAE() {
		return _scpAe;
	}

	public String getSCPAddress() {
		return _scpAddress;
	}

	public String getSCPPort() {
		return _scpPort;
	}
	public void setActorType(String _actorType) {
		this._actorType = _actorType;
	}

	public void setScuAe(String _scuAe) {
		this._scuAe = _scuAe;
	}

	public void setScuAddress(String _scuAddress) {
		this._scuAddress = _scuAddress;
	}

	public void setScuPort(String _scuPort) {
		this._scuPort = _scuPort;
	}

	public void setScpAe(String _scpAe) {
		this._scpAe = _scpAe;
	}

	public void setScpAddress(String _scpAddress) {
		this._scpAddress = _scpAddress;
	}

	public void setScpPort(String _scpPort) {
		this._scpPort = _scpPort;
	}



	
}
