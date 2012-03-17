package us.pauer.qapvsim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;

import us.pauer.qapvsim.QCP.cmove;
import us.pauer.qapvsim.QCP.createUPS;
import us.pauer.qapvsim.QCP.nget;
import us.pauer.qapvsim.QCP.other;
import us.pauer.qapvsim.QCP.subscribe;
import us.pauer.qapvsim.QCP.unsubscribe;




public class QCR extends baseQualityCheck{

	
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

	public class waitForRequest extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("waitForRequest");
				
		}
	}
	
	public class waitForSubscribe extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("waitForSubscribe");
		}
	}
	
	public class subscribe extends baseActionClass
	{
		public void doAction()
		{ 
			System.out.println("subscribe");
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
	
	public QCR(){
		// Implement me
	}
	
	public QCR(String localAETitle, String localIP, String localPort, 
				String remoteAETitle, String remoteIP, String remotePort)  
	{	
		super(localAETitle, localIP, localPort,
			  remoteAETitle, remoteIP, remotePort);
		setFrameTitle("Quality Check Requestor");
		CreateActions();
		
	}

	public void CreateActions(){
		addAction("Wait for Request", new waitForRequest());
		addAction("Wait for Subscrube", new waitForSubscribe());
		addAction("subscribe", new subscribe());
		addAction("Unsubscribe", new unsubscribe());
		addAction("CMOVE", new cmove());
		addAction("Other", new other());
		
	}
	
}
