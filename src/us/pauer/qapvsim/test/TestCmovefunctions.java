package us.pauer.qapvsim.test;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Executor;

import junit.framework.TestCase;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.UIDUtils;

import us.pauer.qapvsim.UPSSCP;




/* ***** BEGIN LICENSE BLOCK *****
* 
*	This set of Junits is provided to test the
*   UPSSCP class.  	
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

public class TestCmovefunctions extends TestCase  {
	
	

	
	public void testRetrieveInputObjectsFromObjectStore()
	{
	    String[] DEF_TS = { UID.ImplicitVRLittleEndian ,
	    		UID.ExplicitVRLittleEndian};

	    PlanArchiveSim pas = new PlanArchiveSim("PAS", "localhost", 40406);
		try {
			pas.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		
		PlanHolderSim phs = new PlanHolderSim("PHS", "localhost", 40407);
		try {
			phs.start();
		} catch (IOException e1) {
			e1.printStackTrace();
			assertTrue(false);
		}
		
		
		String name = "TestSCUCMovePlan";
		Device device = new Device(name);
		
		Executor executor = new NewThreadExecutor(name);
		NetworkApplicationEntity ae = new NetworkApplicationEntity();
		NetworkConnection localConn = new NetworkConnection();

		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(localConn);

		ae.setNetworkConnection(localConn);
		ae.setAssociationInitiator(true);
		ae.setAssociationAcceptor(false);
		ae.setAETitle(name);
		ae.setTransferCapability(new TransferCapability[] {
				new TransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE,
						DEF_TS, TransferCapability.SCU)
		});  

		NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
		NetworkConnection remoteConn = new NetworkConnection();
		
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname("localhost");
		remoteConn.setPort(40406);
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle("PAS");
		
		//**
		//start Cmove Association
		//**
		String abstractSyntaxUID = UID.StudyRootQueryRetrieveInformationModelMOVE;
		DicomObject attrs = setCmoveAttributes();
		String transferSyntaxUid = UID.ImplicitVRLittleEndian;
	    DimseRSP rsp = null;
		String movedUID = "";

	    Association assoc = null;
		try {
			assoc = ae.connect(remoteAE, executor);
		} catch (ConfigurationException e) {
			assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (InterruptedException e) {
			assertTrue(false);
		}
		movedUID = phs.getPlanUID();
		assertTrue(movedUID.equalsIgnoreCase("empty"));
		try {
			rsp = assoc.cmove(abstractSyntaxUID, 0, attrs, transferSyntaxUid, "PHS");
	        while (!rsp.next()){}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		movedUID = phs.getPlanUID();
		
		assertTrue(movedUID.equalsIgnoreCase("1.2.34.56"));
		phs.stop();
		pas.stop();

	}
	/** Set query attributes for C-Move
	 *  @return DicomObject   contains initial set of attributes for query
	 */
	
	private DicomObject setCmoveAttributes() {
		DicomObject returnObject = new BasicDicomObject();
		returnObject.putString(Tag.SOPClassUID, VR.UI, UID.RTPlanStorage);
		returnObject.putString(Tag.SOPInstanceUID, VR.UI, "1.2.34.56");
		returnObject.putString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
		returnObject.putString(Tag.PatientID, VR.LO, "");
		returnObject.putString(Tag.StudyInstanceUID, VR.UI, "1.2.1.1");
		returnObject.putString(Tag.SeriesInstanceUID, VR.UI, "1.2.1.2");
		return returnObject;
	}
}
