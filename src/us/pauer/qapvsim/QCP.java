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

public class QCP extends BaseQualityCheck{
	
	
	/** 
	 * Thin or mock implementation of the QCP...see QualityCheckPerformer class for full
	 * functionality.
	 * 
	 * 
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

	
	
	public class createUPS extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("createUPS");
				
		}
	}
	
	public class subscribe extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("subscribe");
		}
	}
	
	public class nget extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("nget");
		}
	}
	
	public class unsubscribe extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("unsubscribe");
		}
	}
	
	public class cmove extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("cmove");
		}
	}
	
	public class other extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("other");
		}
	}
	
	public QCP(){
		// Implement me
		
	}
	
	public QCP(String localAETitle, String localIP, String localPort, 
				String remoteAETitle, String remoteIP, String remotePort)  
	{
		super(localAETitle, localIP, localPort,
			  remoteAETitle, remoteIP, remotePort);
		setFrameTitle("Quality Check Provider");
		CreateActions();
		
	}

	public void CreateActions(){
		addAction("Create UPS", new createUPS());
		addAction("SubScribe", new subscribe());
		addAction("NGET", new nget());
		addAction("Unsubscribe", new unsubscribe());
		addAction("CMOVE", new cmove());
		addAction("Other", new other());
		
	}
	
}
