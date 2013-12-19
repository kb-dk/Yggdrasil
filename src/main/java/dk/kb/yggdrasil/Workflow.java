package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.jwat.common.ContentType;
import org.jwat.common.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.Preservation;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.json.PreservationResponse;
import dk.kb.yggdrasil.warc.WarcWriterWrapper;
import dk.kb.yggdrasil.xslt.Models;
import dk.kb.yggdrasil.xslt.XmlValidationResult;
import dk.kb.yggdrasil.xslt.XmlValidator;
import dk.kb.yggdrasil.xslt.XslErrorListener;
import dk.kb.yggdrasil.xslt.XslTransformer;
import dk.kb.yggdrasil.xslt.XslUriResolver;

/**
 * The class handling the workflow, and the updates being sent back to Valhal.
 * We have currently two kind of workflows envisioned.
 *  - A content and metadata workflow, where we package metadata and content into one warcfile.
 *  - A metadata workflow, where the metadata is the only content. 
 */
public class Workflow {
    /** The RabbitMQ connection used by this workflow. */
    private MQ mq;
    
    /** The StateDatase instance used by this workflow. */
    private StateDatabase sd;
    
    /** The Bitrepository connection used by this workflow. */
    private Bitrepository bitrepository;
    
    /** The general settings used by Yggdrasil. */
    private Config config;
    
    /** The class reading the mapping between models and xslt scripts used for 
     * the metadata transformation.
     */
    private Models metadataModel;
    /** Size of pushback buffer for determining the encoding of the json message. */ 
    private static int PUSHBACKBUFFERSIZE = 4;
    
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Workflow.class.getName());

    /**
     * Constructor for the Workflow class.
     * @param rabbitconnector The rabbitmq connector object
     * @param states the StateDatabase
     * @param bitrepository the interface with bitrepository
     * @param config general configuration
     * @param models metadatamodelMapper
     */
    public Workflow(MQ rabbitconnector, StateDatabase states, Bitrepository bitrepository, Config config,
            Models models) {
        ArgumentCheck.checkNotNull(rabbitconnector, "MQ rabbitconnector");
        ArgumentCheck.checkNotNull(states, "StateDatabase states");
        ArgumentCheck.checkNotNull(bitrepository, "Bitrepository bitrepository");
        ArgumentCheck.checkNotNull(config, "Config config");
        ArgumentCheck.checkNotNull(models, "Models models");
        this.mq = rabbitconnector;
        this.sd = states;
        this.bitrepository = bitrepository;
        this.config = config;
        this.metadataModel = models;
    }
    
    /**
     * Run this method infinitely.
     * @throws YggdrasilException
     */
    public void run() throws YggdrasilException {
        while (true) {
            String currentUUID = null;
            PreservationRequest request = null;
            try {
                request = getNextRequest();
            } catch (YggdrasilException e) {
                logger.error("Caught exception while retrieving message from rabbitmq. Skipping message", e);
                continue;
            }
            logger.info("Preservation request received.");
            PreservationRequestState prs = null;
            /* Validate message content. */
            if (!request.isMessageValid()) {
                logger.error("Skipping invalid message");
            } else {
                prs = new PreservationRequestState(request, 
                        State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
                // Add check about whether profle is a known collectionID or not known
                String preservationProfile = prs.getRequest().Preservation_profile;
                List<String> possibleCollections = bitrepository.getKnownCollections();
                if (!possibleCollections.contains(preservationProfile)) {
                    String errMsg = "The given preservation profile '" + preservationProfile  
                            + "' does not match a known collection ID. "; 
                    logger.error(errMsg);
                    updateRemotePreservationStateToFailState(prs, errMsg);
                } else {
                    currentUUID = request.UUID;
                    updateRemotePreservationState(prs, State.PRESERVATION_REQUEST_RECEIVED);
                    sd.put(currentUUID, prs);
                }
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

    /**
     * Attempt to upload the packageFile to the bitrepository. 
     * @param currentUUID the currentUUID
     * @param prs The preservationRequest being processed.
     * @throws YggdrasilException
     */
    private void uploadToBitrepository(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        PreservationRequest pr = prs.getRequest();
        File packageFile = prs.getUploadPackage();
        String collectionID = pr.Preservation_profile;
        boolean success = bitrepository.uploadFile(packageFile, collectionID);
        if (success) {
            prs.setState(State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS);
            updateRemotePreservationState(prs, State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS);
            sd.put(currentUUID, prs);
            logger.info("Upload to bitrepository for UUID '" + currentUUID 
                    + "' of package '" + packageFile.getName() + "' was successful.");
        } else {
            prs.setState(State.PRESERVATION_PACKAGE_UPLOAD_FAILURE);
            if (packageFile.exists()) { // Delete here to reset the warcID to null in Valhal
                packageFile.delete();
            }
            prs.resetUploadPackage(); // reset warcId to null
            updateRemotePreservationState(prs, State.PRESERVATION_PACKAGE_UPLOAD_FAILURE);
            sd.put(currentUUID, prs);
            logger.warn("Upload to bitrepository for UUID '" + currentUUID 
                    + "' of package '" + packageFile.getName() + "' failed.");
        }
    }

    /**
     * Write the contentPaylod and transformed metadata to a warc-file.
     * The produced warc-file is attached to the current request. 
     * @param currentUUID The UUID of the current request 
     * @param prs The current request
     */
    private void writeToWarc(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        try {
            UUID packageId = UUID.randomUUID();
            Uri resourceId = null;
            Uri metadataId = null;
            File writeDirectory = config.getTemporaryDir(); 
            WarcWriterWrapper w3 = WarcWriterWrapper.getWriter(writeDirectory, packageId.toString());
            // FIXME Write some WARC fields. 
            w3.writeWarcinfoRecord(new byte[0], null);
            File resource = prs.getContentPayload();
            File metadata = prs.getMetadataPayload();
            InputStream in;
            if (resource != null) {
                in = new FileInputStream(resource);
                resourceId = w3.writeResourceRecord(in, resource.length(), ContentType.parseContentType("application/binary"), null);
                in.close();
            }
            if (metadata != null) {
                in = new FileInputStream(metadata);
                metadataId = w3.writeMetadataRecord(in, metadata.length(), ContentType.parseContentType("application/json"), resourceId, null);
                in.close();
            }
            w3.close();
            prs.setUploadPackage(new File(writeDirectory, packageId.toString()));
        } catch (FileNotFoundException e) {
            throw new YggdrasilException("Horrible exception while writing WARC record!", e);
        } catch (IOException e) {
            throw new YggdrasilException("Horrible exception while writing WARC record!", e);
        }
    }

    /**
     * Transform the metadata included with the request to the proper METS preservation format.
     * @param prs 
     * @param currentUUID 
     * @throws YggdrasilException 
     * 
     */
    private void transformMetadata(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        String theMetadata = prs.getRequest().metadata;
        String modelToUse = prs.getRequest().Model.toLowerCase();
        
        if (!metadataModel.getMapper().containsKey(modelToUse)) {
            final String errMsg = "The given metadata-model'" + modelToUse 
                    + "' is unknown";
            updateRemotePreservationStateToFailState(prs, errMsg);
            throw new YggdrasilException(errMsg);
        }
        
        URL url = this.getClass().getClassLoader().getResource("xslt/" 
                + metadataModel.getMapper().get(modelToUse));
        File xslFile = new File(url.getFile());
        InputStream metadataInputStream = null;
        File outputFile = null;
        try {
            XslTransformer xsltransform = XslTransformer.getTransformer(xslFile);
            XslUriResolver uriResolver = new XslUriResolver();
            XslErrorListener errorListener = new XslErrorListener();
            metadataInputStream = new ByteArrayInputStream(theMetadata.getBytes());
            Source xmlSource = new StreamSource(metadataInputStream);
            outputFile = new File(UUID.randomUUID().toString());
            Result outputTarget = new StreamResult(outputFile);
            xsltransform.transform(xmlSource, uriResolver, errorListener, outputTarget);
            EntityResolver entityResolver = null;
            File xmlFile = outputFile;
            ErrorHandler errorHandler = null;
            XmlValidationResult r = new XmlValidator().validate(xmlFile, entityResolver, 
                    errorHandler);
            if (!r.bValidate) {
                updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
                String errMsg = "The output metadata is invalid: ";
                try {
                    errMsg += FileUtils.readFileToString(xmlFile); 
                } catch (IOException e) {
                    logger.warn("Exception while reading output file:", e);
                }
                throw new YggdrasilException(errMsg);
            } else {
                prs.setMetadataPayload(outputFile);
                updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY);
            }
        } catch (TransformerConfigurationException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
            throw new YggdrasilException(errMsg);
        } catch (TransformerException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
            throw new YggdrasilException(errMsg);
        }
    }

    /**
     * Performs the Content and Metadata workflow.
     * @param currentUUID The UUID of the current request  
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void doContentAndMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        try {
            logger.info("Starting a Content and metadata workflow for UUID '" + currentUUID + "'");  
            fetchContent(currentUUID, prs);
            transformMetadata(currentUUID, prs);
            writeToWarc(currentUUID, prs);
            uploadToBitrepository(currentUUID, prs);
            logger.info("Finished the content metadata workflow for UUID '" + currentUUID + "' successfully");
        } catch (YggdrasilException e) {
            logger.error("An exception occurred during the workflow for UUID: " 
                    + currentUUID, e);
        } finally {
           if (sd.hasEntry(currentUUID)) {
               sd.delete(currentUUID);
           }
           // Cleanup
           prs.cleanup();
        }
        
    }
    
    /**
     * Performs the Metadata workflow. This is currently a method stub.
     * @param currentUUID The UUID of the element  
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void doMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException {
        try {
            logger.info("Starting a metadata workflow for UUID '" + currentUUID + "'");
            transformMetadata(currentUUID, prs);
            writeToWarc(currentUUID, prs);
            uploadToBitrepository(currentUUID, prs);
            logger.info("Finished the metadata workflow for UUID '" + currentUUID + "' successfully");
        } finally {
            if (sd.hasEntry(currentUUID)) {
                sd.delete(currentUUID);
            }
            // Cleanup
            prs.cleanup();
        }
    }
    
    
    /**
     * Try and download the content using the Content_URI.
     * @param currentUUID The UUID of the current request
     * @param prs The current request
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
                String reason = "Unable to write content to local file: " + e;
                updateRemotePreservationStateToFailState(prs, reason);
                throw new YggdrasilException(reason, e);
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
    
    /**
     * Set remote preservation state to PRESERVATION_REQUEST_FAILED with a given reason.      
     * @param prs a given preservationrequeststate
     * @param reason The reason for failing this request
     * @throws YggdrasilException
     */
    private void updateRemotePreservationStateToFailState(
            PreservationRequestState prs, String reason) throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.preservation = new Preservation();
        response.preservation.preservation_state = State.PRESERVATION_REQUEST_FAILED.name();
        response.preservation.preservation_details = reason;
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        logger.info("Preservation status updated to '" + State.PRESERVATION_REQUEST_FAILED.name() 
                +  "' using the updateURI.");
    }
    
    /**
     * Update the remote preservationState corresponding with this current request
     * using the Update_URI field.
     * @param prs The current request
     * @param newPreservationstate The new Preservation State
     * @throws YggdrasilException
     */
    private void updateRemotePreservationState(PreservationRequestState prs,
            State newPreservationstate) throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.preservation = new Preservation();
        response.preservation.preservation_state = newPreservationstate.name();
        response.preservation.preservation_details = newPreservationstate.getDescription();
        if (prs.getUploadPackage() != null) {
            response.preservation.warc_id = prs.getUploadPackage().getName();
        }
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        logger.info("Preservation status updated to '" + newPreservationstate.name() 
                +  "' using the updateURI.");
    }
    
    
    /**
     * Wait until the next request arrives on the queue, and then return the request.
     * @return the next request from the queue
     * @throws YggdrasilException
     */
    private PreservationRequest getNextRequest() throws YggdrasilException {
        // TODO Should there be a timeout here?
        byte[] requestBytes = mq.receiveMessageFromQueue(
                mq.getSettings().getPreservationDestination());
        PreservationRequest request = JSONMessaging.getPreservationRequest(
                new PushbackInputStream(new ByteArrayInputStream(requestBytes), PUSHBACKBUFFERSIZE));
        return request;
    }
    
}
