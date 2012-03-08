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

	private static final int STATE_WAIT = 0;
	private static final int STATE_PROCESSING_CREATE = 1;
	private static final int STATE_PROCESSING_SUBSCRIBE = 2;
	private static final int STATE_PROCESSING_NGET = 3;
	private static final int STATE_PROCESSING_UNSUBSCRIBE = 4;
	private static final int STATE_PROCESSING_CMOVE = 5;
	private static final int STATE_TOTAL = 6;
	
	

	int currentState = 0;
	

	
	ActionListener _qcpButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		}
	};

	ActionListener _qcpResetButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		}
	};
	
	

	public QCP(String scuae, String scuAddress, String scuPort, String scpae,
			String scpAddress, String scpPort) {
		ui = new QCUI("Quality Check Provider");
	}

	public QCUI getUI() {
		return ui;
	}
	
	public void start() {
		ui.setMainButtonListener(_qcpButtonListener);
		ui.setResetButtonListener(_qcpResetButtonListener);
		ui.setNextAction("");
		ui.setVisible(true);
	}

}
