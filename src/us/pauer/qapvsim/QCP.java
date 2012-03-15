/* ***** BEGIN LICENSE BLOCK *****
* 
*	QCP class simulates the roel of a Quality Check Performer
*   actor in the Quality Assurance with Plan Veto profile of 
*   the IHE-RO
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;

public class QCP {
	private QCUI ui;
	
	/** 
	 * The UPS SCP has a default waiting for input mode.
	 * The recognized inputs:
	 * 	Create UPS
	 * 	Subscribe to UPS
	 * 	N-Get of an attribute of the UPS
	 *  Unsubscribe
	 *  C-Move structured report.
	 * Even if the request is valid, it may not be serviced:
	 * 	Create UPS not allowed if subscribed UPS exists
	 *  Subscribe to UPS not allowed if UPS not Created or Completed
	 *  N-Get not allowed if UPS not created
	 *  Unsubscribe if no subscriptiongs
	 *  C-Move if UPS is not complete or not found.
	 *  
	 */

	public class baseClass
	{
		public void doAction(){
			System.out.println("Implement me!");
		}
	}
	
	public class createUPS extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("createUPS");
		}
	}
	
	public class subscribe extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("subscribe");
		}
	}
	
	public class nget extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("nget");
		}
	}
	
	public class unsubscribe extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("unsubscribe");
		}
	}
	
	public class cmove extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("cmove");
		}
	}
	
	public class other extends baseClass
	{
		public void doAction()
		{ 
				System.out.println("other");
		}
	}
	
	
	enum states { WAIT_USER, WAIT_SUBSCRIBE, WAIT_OTHER};
	
	class events {
		public actions action;
		public String stringAction;
	};
	
	class connectionInfo{
		public String aetitle;
		public String ip;
		public String port;
	}
	
	private connectionInfo local = new connectionInfo();
	private connectionInfo remote = new connectionInfo();
	
	private Map<String,baseClass> event = new LinkedHashMap<String,baseClass>();
	
	ActionListener _executeButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			System.out.println("IN executeButtonListener");
			
			String actionToExecute = ui.getActionToExecute();			
			baseClass action = event.get(actionToExecute);
			action.doAction();
		
		}
	};

	
	
	public QCP(String localAETitle, String localIP, String localPort, 
				String remoteAETitle, String remoteIP, String remotePort) {
		
		// Copy connection information.
		local.aetitle = localAETitle;
		local.ip = localIP;
		local.port = localPort;
		remote.aetitle = localAETitle;
		remote.ip = remoteIP;
		remote.port = remotePort;
	
		ui = new QCUI("Quality Check Provider");
		
		CreateActions();
		
	}

	public QCUI getUI() {
		return ui;
	}
	
	
	public void start() {
		
		ui.setExecuteButtonListener(_executeButtonListener);
		ui.setVisible(true);
	}

	public void CreateActions(){
		addAction("Create UPS", new createUPS());
		addAction("SubScribe", new subscribe());
		addAction("NGET", new nget());
		addAction("Unsubscribe", new unsubscribe());
		addAction("CMOVE", new cmove());
		addAction("Other", new other());
		
	}
	// Adds the description 
	public void addAction(String stringAction, baseClass myClass)
	{
		event.put(stringAction, myClass);
		ui.addTextToComboBox(stringAction);
	}
}
