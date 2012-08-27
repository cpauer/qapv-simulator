package us.pauer.qapvsim.test;

import us.pauer.qapvsim.oldQAPVSim;
import us.pauer.qapvsim.QCP;
import us.pauer.qapvsim.QCR;
import us.pauer.qapvsim.QCUI;

import junit.framework.TestCase;

/**
 * UI Description
 * 1) User invokes the QAPV Simulator
 * 1.a) if no properties file is found to indicate settings, prompts appear
 * 1.a.1) prompt for Which type (requester, provider)
 * 1.a.2) prompt for ae title, port, other ip, port, ae
 * 2) Start appropriate process
 * 3) for each type of process, top part is control interface (dialog, buttons, middle is logging, bottom is status.
 * 4) if requester, steps 
 * 	a) send UPS create
 * 	b) send subscribe request
 * 	c) wait for final progress
 *  d) request final status
 *  e) send unsubscribe
 * 5) if provider, steps
 *  a) wait for requests
 *  b) if other than create, error
 *  c) create ups, request, create UPS
 *  d) handle subscribe event
 *  e) do fake check
 *  f) supply progress updates
 *  g) handle n get requests
 *  h) handle all unsubscribe requests
 *  i) if all UPS has been performed and all unsubscribed, delete UPS, or archive
 * 
 * @author Chris Pauer
 *
 */




public class TestSimInterface extends TestCase {
	
	public void testInvoke() {
		oldQAPVSim sim =  new oldQAPVSim();
		assertTrue(sim!=null);

		sim = new oldQAPVSim();
		assertTrue(sim!=null);
		
		sim = new oldQAPVSim();
		assertTrue(sim!=null);
	}
	
	public void testMembers() {
		String[] args = getDefualtRequesterSetup();
		oldQAPVSim sim = new oldQAPVSim();
		assertTrue(sim.getActorType().equalsIgnoreCase("Requester"));
		assertTrue(sim.getSCUAE().equalsIgnoreCase("QCR"));
		assertTrue(sim.getSCUAddress().equalsIgnoreCase("192.168.0.4"));
		assertTrue(sim.getSCUPort().equalsIgnoreCase("40404"));
		assertTrue(sim.getSCPAE().equalsIgnoreCase("QCP"));
		assertTrue(sim.getSCPAddress().equalsIgnoreCase("192.168.0.4"));
		assertTrue(sim.getSCPPort().equalsIgnoreCase("40405"));
	}
	public void testSCUCreate() {
		String[] args = getDefualtRequesterSetup();
		oldQAPVSim simR = new oldQAPVSim();
		assertTrue(simR.getQCR()!=null);
		QCR qcr = simR.getQCR();
		QCUI ui = qcr.getUI();
		assertTrue(ui!=null);
		ui.setNextAction("nextAction");
		assertTrue(ui.getNextAction().equalsIgnoreCase("nextAction"));
		ui.setLastMessage("lastmessage");
		assertTrue(ui.getLastMessage().equalsIgnoreCase("lastmessage"));
	}
	
	
	public void testSCPCreate() {
		String[] args = getDefualtRequesterSetup();
		args[0] = "Provider";
		oldQAPVSim simP = new oldQAPVSim();
		QCP qcp = simP.getQCP();
		QCUI ui = qcp.getUI();
		assertTrue(ui!=null);
		ui.setNextAction("nextAction");
		assertTrue(ui.getNextAction().equalsIgnoreCase("nextAction"));
		ui.setLastMessage("lastmessage");
		assertTrue(ui.getLastMessage().equalsIgnoreCase("lastmessage"));
	}

	
	public void testPropertyInvocation() {
		assertTrue(true);
		//worry about this later
	}
	private String[] getDefualtRequesterSetup() {
		String[] args = new String[7];
		args[0] = "Requester";
		args[1] = "QCR";
		args[2] = "192.168.0.4";
		args[3] = "40404";
		args[4] = "QCP";
		args[5] = "192.168.0.4";
		args[6] = "40405";
		return args;
	}

}
