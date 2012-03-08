package us.pauer.qapvsim.test;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Executor;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.ExtQueryTransferCapability;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
import org.dcm4che2.net.service.CMoveSCP;
import org.dcm4che2.net.service.CStoreSCP;
import org.dcm4che2.net.service.DicomService;



public class PlanHolderSim  {
	Device device;
	Executor executor;
	NetworkApplicationEntity scp;
	NetworkConnection scpConn;
	ActionListener aListener;
	

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };
    private static String planUID = "empty";
	
	

    private static class TPHVerificationService extends DicomService implements CEchoSCP {

        private static final String[] sopClasses = { UID.VerificationSOPClass };

        public TPHVerificationService() {
            super(sopClasses, null);
        }

        // has to implement this method signature…
        public void cecho(Association as, int pcid, DicomObject cmd)
                throws IOException {
            as.writeDimseRSP(pcid, CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS));
            System.out.println("C-Echo has responded");
        }
    }

    private static class TPHPlanStorageService extends DicomService implements CStoreSCP {

        private static final String[] sopClasses = { UID.RTPlanStorage };

        public TPHPlanStorageService() {
            super(sopClasses, null);
        }


		@Override
		public void cstore(Association as, int pcid, DicomObject cmd,
	            PDVInputStream dataStream, String tsuid) throws DicomServiceException, IOException {
			System.out.println("C-Store was called.");
			DicomObject rsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
			onCStoreRQ(as, pcid, cmd, dataStream, tsuid, rsp);
			as.writeDimseRSP(pcid, rsp);
		}

	    protected void onCStoreRQ(Association as, int pcid, DicomObject rq,
	            PDVInputStream dataStream, String tsuid, DicomObject rsp)
	            throws DicomServiceException, IOException {
	    	DicomObject plan = dataStream.readDataset();
	    	planUID = plan.getString(Tag.SOPInstanceUID);
	    }
    }


    
	
	public PlanHolderSim(String aeTitle, String server, int port) {
		device = new Device(aeTitle);
		executor = new NewThreadExecutor(aeTitle);
		scp = new NetworkApplicationEntity();
		scpConn = new NetworkConnection();
		
		scp.setInstalled(true);
		scp.setAssociationAcceptor(true);
		scp.setAETitle(aeTitle);
		scpConn.setHostname(server);
		scpConn.setPort(port);
		scp.setNetworkConnection(scpConn);
		
		device.setNetworkApplicationEntity(scp);
		device.setNetworkConnection(scpConn);
		
		setServices(scp);
		setTransferCapabilities(scp);
		
	}
	
	private void setTransferCapabilities(NetworkApplicationEntity ae) {
		TransferCapability[] tc = new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.RTPlanStorage, ONLY_DEF_TS, TransferCapability.SCP)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new TPHVerificationService());
		ae.register(new TPHPlanStorageService());
	}
	
    public void start() throws IOException {
        device.startListening(executor);
        System.out.println("Start TPH Server listening on port " + scpConn.getPort());
    }

    public void stop() {
        if (device != null)
            device.stopListening();

        if (scpConn != null)
            System.out.println("Stop TPH Server listening on port " + scpConn.getPort());
        else
            System.out.println("Stop TPH Server");
    }

	public String getAeTitle() {
		return scp.getAETitle();
	}

	public String getPlanUID() {
		return planUID;
	}
	

}
