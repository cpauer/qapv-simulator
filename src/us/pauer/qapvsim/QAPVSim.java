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


public class QAPVSim {

	private static final String TYPE_REQUESTER = "REQUESTER";
	private static final String TYPE_PERFORMER = "PERFORMER";

	
	private String _scuAe;
	private String _scuAddress;
	private String _scuPort;
	private String _scpAe;
	private String _scpAddress;
	private String _scpPort;
	private String _actorType;
	
	private QCR _QCR;
	private QCP _QCP;


	

	public QAPVSim() {
		// TODO Auto-generated constructor stub
	}

	public QAPVSim(String string) {
		// TODO Auto-generated constructor stub
	}

	public QAPVSim(String[] strings) {
		setActorType(strings[0]);
		setScuAe(strings[1]);
		setScuAddress(strings[2]);
		setScuPort(strings[3]);
		setScpAe(strings[4]);
		setScpAddress(strings[5]);
		setScpPort(strings[6]);
		if (getActorType().equalsIgnoreCase(TYPE_REQUESTER)) {
			invokeAndRunQCR();
		}
		else {
			if (getActorType().equalsIgnoreCase(TYPE_PERFORMER)) {
				invokeAndRunQCP();
			}
			else {
				//error!
			}
		}
	}

	private void invokeAndRunQCP() {
		_QCP = new QCP(getSCUAE(), getSCUAddress(), getSCUPort(), getSCPAE(), getSCPAddress(), getSCPPort());
		_QCP.start();
	}

	private void invokeAndRunQCR() {
		_QCR = new QCR(getSCUAE(), getSCUAddress(), getSCUPort(), getSCPAE(), getSCPAddress(), getSCPPort());
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

	public QCR getQCR() {
		// TODO Auto-generated method stub
		return _QCR;
	}

	public QCP getQCP() {
		// TODO Auto-generated method stub
		return _QCP;
	}

	
}
