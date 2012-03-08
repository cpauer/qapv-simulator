package us.pauer.qapvsim.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Executor;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.DataWriterAdapter;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.ExtQueryTransferCapability;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
import org.dcm4che2.net.service.CMoveSCP;
import org.dcm4che2.net.service.DicomService;

import us.pauer.qapvsim.PersistenceUtils;



public class PlanArchiveSim  {
	static Device device;
	static Executor executor;
	static NetworkApplicationEntity scp;
	static NetworkConnection scpConn;
	static NetworkApplicationEntity remoteAE;
	static NetworkConnection remoteConn;
	
	static ActionListener aListener;
	

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };
    
    private static DicomObject cMoveQR = null;
	
	

    private static class TPAVerificationService extends DicomService implements CEchoSCP {

        private static final String[] sopClasses = { UID.VerificationSOPClass };

        public TPAVerificationService() {
            super(sopClasses, null);
        }

        // has to implement this method signature…
        public void cecho(Association as, int pcid, DicomObject cmd)
                throws IOException {
            as.writeDimseRSP(pcid, CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS));
            System.out.println("C-Echo has responded");
        }
    }

    private static class TPAPlanQueryService extends DicomService implements CMoveSCP {

        private static final String[] sopClasses = { UID.StudyRootQueryRetrieveInformationModelMOVE };

        public TPAPlanQueryService() {
            super(sopClasses, null);
        }


		@Override
		public void cmove(Association as, int pcid, DicomObject cmd,
				DicomObject data) throws DicomServiceException, IOException {
			System.out.println("C-Move was called.");
			cMoveQR = data;
			requestStorage(cmd, data);
	        DicomObject rsp = CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS);
	        as.writeDimseRSP(pcid, rsp);
		}

		
		public void requestStorage(DicomObject cmd, DicomObject data) {
			String destination = cmd.getString(Tag.MoveDestination);
			if (destination.equalsIgnoreCase("PHS")) {
				//**
				//start CStore Association
				//**
				String abstractSyntaxUID = UID.RTPlanStorage;
				String transferSyntaxUid = UID.ImplicitVRLittleEndian;
				String instanceUid = data.getString(Tag.SOPInstanceUID);
			    DimseRSP rsp = null;
			    //get plan
			    PersistenceUtils pUtils = new PersistenceUtils();
			    DicomObject[] dObjects;
				try {
					dObjects = pUtils.readMatchingObjects(instanceUid);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
			    if (dObjects.length==0) {
			    	System.out.println("No matching objects found on C-Move request");
			    	return;
			    }
			    	
			    

			    Association assoc = null;
				try {
					assoc = scp.connect(remoteAE, executor);
				} catch (ConfigurationException e) {
					System.out.println(e.getMessage());
				} catch (IOException e) {
					System.out.println(e.getMessage());
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				}
				try {
					rsp = assoc.cstore(abstractSyntaxUID, instanceUid, 0, new DataWriterAdapter(dObjects[0]), transferSyntaxUid);
			        while (!rsp.next()){}
				} catch (IOException e) {
					System.out.println(e.getMessage());
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				}
				
			} else {
				System.out.println("something is wrong, destination for store not correct");
			}

		}
		

    }


    
	
	public PlanArchiveSim(String aeTitle, String server, int port) {
		device = new Device(aeTitle);
		executor = new NewThreadExecutor(aeTitle);
		scp = new NetworkApplicationEntity();
		scpConn = new NetworkConnection();
		
		scp.setInstalled(true);
		scp.setAssociationAcceptor(true);
		scp.setAssociationInitiator(true);
		scp.setAETitle(aeTitle);
		scpConn.setHostname(server);
		scpConn.setPort(port);
		scp.setNetworkConnection(scpConn);
		
		device.setNetworkApplicationEntity(scp);
		device.setNetworkConnection(scpConn);

		//Now our local SCU needs to know about the remote AE….
		remoteAE = new NetworkApplicationEntity();
		remoteConn = new NetworkConnection();
		
		// Here we say describe the remote AE….Note we use the AE Title we will establish for the
        // UPSSCP when that is finally built…
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteConn.setHostname("localhost");
		remoteConn.setPort(40407);
		remoteAE.setNetworkConnection(remoteConn);
		remoteAE.setAETitle("PHS");
		
		
		setServices(scp);
		setTransferCapabilities(scp);
		
	}
	
	private void setTransferCapabilities(NetworkApplicationEntity ae) {
		TransferCapability[] tc = new TransferCapability[] {
				new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new ExtQueryTransferCapability(UID.StudyRootQueryRetrieveInformationModelMOVE, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.RTPlanStorage, ONLY_DEF_TS, TransferCapability.SCU)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new TPAVerificationService());
		ae.register(new TPAPlanQueryService());
	}
	
    public void start() throws IOException {
        device.startListening(executor);
        System.out.println("Start TPA Server listening on port " + scpConn.getPort());
    }

    public void stop() {
        if (device != null)
            device.stopListening();

        if (scpConn != null)
            System.out.println("Stop TPA Server listening on port " + scpConn.getPort());
        else
            System.out.println("Stop TPA Server");
    }

	public String getAeTitle() {
		return scp.getAETitle();
	}


}
