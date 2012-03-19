/* ***** BEGIN LICENSE BLOCK *****
* 
*	UPSSCP is a Service Class Provider implementation of
*   the Quality Check Provider actor of the Quality 
*   Assurance with Plan Veto profile of the IHE-RO*	
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.CEchoSCP;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.NActionSCP;
import org.dcm4che2.net.service.NCreateSCP;
import org.dcm4che2.util.UIDUtils;

public class UPSSCP {

	Device device;
	Executor executor;
	NetworkApplicationEntity scp;
	NetworkConnection scpConn;
	
	static final String DEFAULT_PERSIST_LOCATION = "C:/QAPVSIM/SCP/";
	
	String persistLocation = DEFAULT_PERSIST_LOCATION;
	

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };
	
	

    private static class QCPVerificationService extends DicomService implements CEchoSCP {

        private static final String[] sopClasses = { UID.VerificationSOPClass };

        public QCPVerificationService() {
            super(sopClasses, null);
        }

        // has to implement this method signature…
        public void cecho(Association as, int pcid, DicomObject cmd)
                throws IOException {
            as.writeDimseRSP(pcid, CommandUtils.mkRSP(cmd, CommandUtils.SUCCESS));
            System.out.println("C-Echo has responded");
        }
    }

    private static class QCPNCreateService extends DicomService implements NCreateSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNCreateService() {
            super(sopClasses, null);
        }

        public void ncreate(Association as, int pcid, DicomObject rq,
		        DicomObject data) throws DicomServiceException, IOException {
			System.out.println("Starting n-create on server");
		    DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
		    String iuid = data.getString(Tag.AffectedSOPInstanceUID);
		    if (iuid == null) {
		        iuid = UIDUtils.createUID();
		        rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		        rsp.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
		    }
		    as.writeDimseRSP(pcid, rsp, doNCreate(as, pcid, rq, data, rsp));
		}
        
        private DicomObject doNCreate(Association as, int pcid, DicomObject rq,
                DicomObject data, DicomObject rsp) throws IOException {
        	System.out.println("in N-Create");
        	DicomObject UPSObject = createUPS(rq, data);
        	persistUPS(UPSObject);
    		return UPSObject;
            
        }

        private DicomObject createUPS(DicomObject rq, DicomObject data) {
        	System.out.println("in createUPS");
        	DicomObject outObject = new BasicDicomObject();
        	//SOP Common
        	outObject.putString(Tag.SOPClassUID, VR.UI, UID.UnifiedProcedureStepPushSOPClass);
        	outObject.putString(Tag.SOPInstanceUID, VR.UI, data.getString(Tag.AffectedSOPInstanceUID));
        	System.out.println("affect sop instance:"+data.getString(Tag.AffectedSOPInstanceUID));
        	outObject.putDate(Tag.InstanceCreationDate, VR.DA, new Date());
        	outObject.putDate(Tag.InstanceCreationTime, VR.TM, new Date());
        	//add appropriate items
        	outObject.putString(Tag.UnifiedProcedureStepState, VR.CS, "SCHEDULED");
        	outObject.putSequence(Tag.UnifiedProcedureStepProgressInformationSequence);
        	
        	DicomObject progressSeq = new BasicDicomObject();
        	progressSeq.putString(Tag.UnifiedProcedureStepProgress, VR.DS, "0");
        	progressSeq.putString(Tag.UnifiedProcedureStepProgressDescription, VR.ST, "Scheduled");
        	outObject.putNestedDicomObject(Tag.UnifiedProcedureStepProgressInformationSequence, progressSeq);
          	System.out.println("before IIS");
    		DicomObject inputSequenceFromRequest = data.getNestedDicomObject(Tag.InputInformationSequence);
          	if (inputSequenceFromRequest==null) System.out.println("iis is " );		
          	outObject.putSequence(Tag.InputInformationSequence);
          	outObject.putNestedDicomObject(Tag.InputInformationSequence, inputSequenceFromRequest);
        	System.out.println("out createUPS");
			return outObject;
		}

		private void persistUPS(DicomObject upsObject) {
        	System.out.println("in persistUPS");
    		try {
    			String instanceUid = upsObject.getString(Tag.SOPInstanceUID);
				DicomOutputStream  outStream = new DicomOutputStream(new File(DEFAULT_PERSIST_LOCATION+
						"/NCREATE."+instanceUid));
				outStream.writeDataset(upsObject, UID.ImplicitVRLittleEndian);
				outStream.close();
	        	System.out.println("out persistUPS");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

    }

    private static class QCPNActionService extends DicomService implements NActionSCP {

        private static final String[] sopClasses = { UID.UnifiedProcedureStepPushSOPClass };

        public QCPNActionService() {
            super(sopClasses, null);
        }

        public void naction(Association as, int pcid, DicomObject command,
		        DicomObject data) throws DicomServiceException, IOException {
			System.out.println("Starting n-action on server");
		    DicomObject rsp = CommandUtils.mkRSP(command, CommandUtils.SUCCESS);
		    // capture the incoming uid
		    int stuff = command.getInt(Tag.ActionTypeID);
		    System.out.println("Action id is "+stuff);
		    String iuid = command.getString(Tag.RequestedSOPInstanceUID);
		    String receivingAE = data.getString(Tag.ReceivingAE);
		    String lockDeletion = data.getString(Tag.DeletionLock);
		    // We don’t implement an actual subscription persistence mechanism here, but you can see
            // we have the data available to do so….
		    System.out.println("AE "+receivingAE+" is subscribed to SOP Class Instance "
		    		+iuid+" with Delete Lock set to "+lockDeletion);
		    as.writeDimseRSP(pcid, rsp, data);
		}
    }

    
	
	public UPSSCP(String aeTitle, String server, int port) {
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
				new TransferCapability(UID.UnifiedProcedureStepPushSOPClass, ONLY_DEF_TS, TransferCapability.SCP),
				new TransferCapability(UID.UnifiedProcedureStepWatchSOPClass, ONLY_DEF_TS, TransferCapability.SCP)};
		ae.setTransferCapability(tc);
		
	}

	private void setServices(NetworkApplicationEntity ae) {
		ae.register(new QCPVerificationService());
		ae.register(new QCPNCreateService());
		ae.register(new QCPNActionService());
	}
	
    public void start() throws IOException {
        device.startListening(executor);
        System.out.println("Start Server listening on port " + scpConn.getPort());
    }

    public void stop() {
        if (device != null)
            device.stopListening();

        if (scpConn != null)
            System.out.println("Stop Server listening on port " + scpConn.getPort());
        else
            System.out.println("Stop Server");
    }

	public String getAeTitle() {
		return scp.getAETitle();
	}



}
