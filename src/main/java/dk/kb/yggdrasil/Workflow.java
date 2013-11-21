package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PushbackInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.Preservation;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.json.PreservationResponse;

/**
 * The class handling the workflow, and the updates being sent back to Valhal.
 * We have currently two kind of workflows envisioned.
 *  - A content and Metadata workflow, where we package metadata and content into one warcfile.
 *  - A metadata workflow, where the metadata is the only content. 
 */
public class Workflow {
    
    private MQ mq;
    private StateDatabase sd;
    private Bitrepository bitrepository;
    
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Workflow.class.getName());

    /**
     * Constructor for the Workflow class.
     * @param rabbitconnector
     * @param states
     * @param bitrepository
     */
    public Workflow(MQ rabbitconnector, StateDatabase states, Bitrepository bitrepository) {
        ArgumentCheck.checkNotNull(rabbitconnector, "MQ rabbitconnector");
        ArgumentCheck.checkNotNull(rabbitconnector, "StateDatabase states");
        ArgumentCheck.checkNotNull(rabbitconnector, "Bitrepository bitrepository");
        this.mq = rabbitconnector;
        this.sd = states;
        this.bitrepository = bitrepository;
    }
    
    /**
     * Run this method infinitely.
     * @throws YggdrasilException
     */
    public void run() throws YggdrasilException {
        while (true) {
            String currentUUID = null;
            PreservationRequest request = getNextRequest();
            logger.info("Preservation request received.");
            PreservationRequestState prs = null;
            /* Validate message content. */
            if (!request.isMessageValid()) {
                logger.error("Skipping invalid message");
            } else {
                prs = new PreservationRequestState(request, 
                        State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
                currentUUID = request.UUID;
                updateRemotePreservationState(prs, State.PRESERVATION_REQUEST_RECEIVED);
                sd.put(currentUUID, prs);   
            }

            if (currentUUID != null) { // Message received is valid. proceed with workflow
                if (prs.getRequest().Content_URI != null) {
                    try {
                        doContentAndMetadataWorkflow(currentUUID, prs);
                    } catch (YggdrasilException e) {
                        currentUUID = null;
                    }
                } else {
                    try {
                        doMetadataWorkflow(currentUUID, prs);
                    } catch (YggdrasilException e) {
                        currentUUID = null;
                    }
                }
            }
            logger.info("Finished processing the preservation request");
        } // end while loop
    }
        
    private void updateRemotePreservationState(PreservationRequestState prs,
            State newPreservationstate) throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.preservation = new Preservation();
        response.preservation.preservation_state = newPreservationstate.name();
        response.preservation.preservation_details = newPreservationstate.getDescription();
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        logger.info("Preservation status updated to '" + newPreservationstate.name() 
                +  "' using the updateURI.");
    }

    private void doMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        logger.info("Starting a metadata workflow for UUID '" + currentUUID + "'");
        transformMetadata();
        writeToWarc(currentUUID, prs);
        uploadToBitrepository(currentUUID, prs);
        //TODO update the remote preservation metadata with a packageId
        logger.info("Finished the metadata workflow for UUID '" + currentUUID + "' successfully");
    }

    /**
     * Attempt to upload the packageFile to the bitrepository
     * @param currentUUID the currentUUID
     * @param prs The preservationRequest being processed.
     * @throws YggdrasilException
     */
    private void uploadToBitrepository(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        PreservationRequest pr = prs.getRequest();
        File packageFile = prs.getUploadPackage();
        boolean success = bitrepository.uploadFile(packageFile, pr.Preservation_profile);
        logger.info(success + "");
        if (success) {
            prs.setState(State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS);
            updateRemotePreservationState(prs, State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS);
            sd.put(currentUUID, prs);
        } else {
            prs.setState(State.PRESERVATION_PACKAGE_UPLOAD_FAILURE);
            updateRemotePreservationState(prs, State.PRESERVATION_PACKAGE_UPLOAD_FAILURE);
            sd.put(currentUUID, prs);
        }
    }

    /**
     * Write the contentPaylod and transformed metadata to a warc-file.
     * The produced warc-file is 
     * @param currentUUID
     * @param prs
     */
    private void writeToWarc(String currentUUID, PreservationRequestState prs) {
        // TODO Auto-generated method stub
        
    }

    private void transformMetadata() {
        // TODO Auto-generated method stub
        
    }

    private void doContentAndMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        logger.info("Starting a Content metadata workflow for UUID '" + currentUUID + "'");  
        fetchContent(currentUUID, prs);
        transformMetadata();
        writeToWarc(currentUUID, prs);
        uploadToBitrepository(currentUUID, prs);
        //TODO update the remote preservation metadata with a packageId 
        logger.info("Finished the content metadata workflow for UUID '" + currentUUID + "' successfully");
    }

    
    /**
     * Try and download the content using the Content_URI.
     * @param currentUUID
     * @param prs
     * @throws YggdrasilException
     */
    private void fetchContent(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        // Try to download ressource from Content_URI
        File tmpFile = null;
        PreservationRequest pr = prs.getRequest();
        logger.info("Attempting to download resource from '" 
                + pr.Content_URI + "'");
        HttpPayload payload = HttpCommunication.get(pr.Content_URI);
        if (payload != null) {
            try {
                tmpFile = payload.writeToFile();
            } catch (IOException e) {
                updateRemotePreservationStateAndThrowException(prs, 
                        "Unable to write content to local file: " + e);
            }
            prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
            prs.setContentPayload(tmpFile);
            updateRemotePreservationState(prs, State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
            sd.put(currentUUID, prs);
        } else {
            prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE);
            updateRemotePreservationState(prs, State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE);
            sd.put(currentUUID, prs);
        }   
    }
    
    private void updateRemotePreservationStateAndThrowException(
            PreservationRequestState prs, String reason) throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.preservation = new Preservation();
        response.preservation.preservation_state = State.PRESERVATION_REQUEST_FAILED.name();
        response.preservation.preservation_details = reason;
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        logger.info("Preservation status updated to '" + State.PRESERVATION_REQUEST_FAILED.name() 
                +  "' using the updateURI.");
        throw new YggdrasilException(reason);
    }

    public PreservationRequest getNextRequest() throws YggdrasilException {
        // TODO Should there be a timeout here?
        byte[] requestBytes = mq.receiveMessageFromQueue(
                mq.getSettings().getPreservationDestination());
        PreservationRequest request = JSONMessaging.getPreservationRequest(
                new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4));
        return request;
    }
    
}
