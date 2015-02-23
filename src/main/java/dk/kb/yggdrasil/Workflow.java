package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
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
import org.jwat.warc.WarcDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.Preservation;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.json.PreservationResponse;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.warc.Digest;
import dk.kb.yggdrasil.warc.WarcWriterWrapper;
import dk.kb.yggdrasil.xslt.Models;
import dk.kb.yggdrasil.xslt.XmlErrorHandler;
import dk.kb.yggdrasil.xslt.XmlValidationResult;
import dk.kb.yggdrasil.xslt.XmlValidator;
import dk.kb.yggdrasil.xslt.XslErrorListener;
import dk.kb.yggdrasil.xslt.XslTransformer;
import dk.kb.yggdrasil.xslt.XslUriResolver;
import dk.kb.yggdrasil.xslt.extension.Agent;

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
    private final static int PUSHBACKBUFFERSIZE = 4;

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Workflow.class.getName());

    public final static String RABBITMQ_CONF_FILE = "./config/rabbitmq.yml";
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
     * @throws RabbitException When message queue connection fails.
     */
    public void run() throws YggdrasilException, FileNotFoundException, RabbitException {
        boolean shutdown = false;
        while (!shutdown) {
            PreservationRequest request = null;
            try {
                request = getNextRequest();
            } catch (YggdrasilException e) {
                logger.error("Caught exception while retrieving message from rabbitmq. Skipping message", e);
                continue;
            }

            if (request == null) {
                logger.info("Received shutdown message. Shutting down. ");
                shutdown = true;
                continue;
            }

            handleRequest(request);
        } 
    }

    /**
     * Handles the PreservationRequest.
     * @param request The preservation request to handle.
     * @throws YggdrasilException if anything goes wrong.
     * @throws FileNotFoundException If the data cannot be retrieved or handled (e.g. not enough space left on device).
     */
    private void handleRequest(PreservationRequest request) throws YggdrasilException, FileNotFoundException {
        String currentUUID = null;

        logger.info("Preservation request received.");
        PreservationRequestState prs = null;
        /* Validate message content. */
        if (!request.isMessageValid()) {
            logger.error("Skipping invalid message");
        } else {
            prs = new PreservationRequestState(request,
                    State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
            // Add check about whether profile is a known collectionID or not known
            String preservationProfile = prs.getRequest().Preservation_profile;
            List<String> possibleCollections = bitrepository.getKnownCollections();
            if (!possibleCollections.contains(preservationProfile)) {
                String errMsg = "The given preservation profile '" + preservationProfile
                        + "' does not match a known collection ID. ";
                logger.error(errMsg);
                updateRemotePreservationStateToFailState(prs, State.PRESERVATION_REQUEST_FAILED,
                        errMsg);
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
    }

    /**
     * Attempt to upload the packageFile to the bitrepository.
     * @param currentUUID the currentUUID
     * @param prs The preservationRequest being processed.
     * @throws YggdrasilException
     */
    private void uploadToBitrepository(String currentUUID, PreservationRequestState prs) throws YggdrasilException, FileNotFoundException {
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
            Digest digestor = new Digest("SHA-1");
            File writeDirectory = config.getTemporaryDir();
            WarcWriterWrapper w3 = WarcWriterWrapper.getWriter(writeDirectory, packageId.toString());
            String warcInfoPayload = getWarcInfoPayload();
            byte[] warcInfoPayloadBytes = warcInfoPayload.getBytes("UTF-8");
            w3.writeWarcinfoRecord(warcInfoPayloadBytes,
                    digestor.getDigestOfBytes(warcInfoPayloadBytes));

            File resource = prs.getContentPayload();
            File metadata = prs.getMetadataPayload();
            InputStream in;
            if (resource != null) {
                in = new FileInputStream(resource);
                WarcDigest blockDigest = digestor.getDigestOfFile(resource);
                resourceId = w3.writeResourceRecord(in, resource.length(),
                        ContentType.parseContentType("application/binary"), blockDigest, prs.getRequest().File_UUID);
                in.close();
            }
            if (metadata != null) {
                in = new FileInputStream(metadata);
                WarcDigest blockDigest = digestor.getDigestOfFile(metadata);
                metadataId = w3.writeMetadataRecord(in, metadata.length(),
                        ContentType.parseContentType("text/xml"), resourceId, blockDigest,
                        prs.getRequest().UUID);
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
     * @param prs The current request
     * @param currentUUID The UUID of the current request
     * @throws YggdrasilException
     *
     */
    private void transformMetadata(String currentUUID, PreservationRequestState prs) throws YggdrasilException, FileNotFoundException {
        String theMetadata = prs.getRequest().metadata;
        String modelToUse = prs.getRequest().Model.toLowerCase();

        if (!metadataModel.getMapper().containsKey(modelToUse)) {
            final String errMsg = "The given metadata-model'" + modelToUse
                    + "' is unknown";
            updateRemotePreservationStateToFailState(prs, State.PRESERVATION_REQUEST_FAILED,
                    errMsg);
            throw new YggdrasilException(errMsg);
        }
        File xsltDir = new File(config.getConfigDir(), "xslt");
        if (!xsltDir.isDirectory()) {
            final String errMsg = "The xslt directory '" + xsltDir.getAbsolutePath()
                    + "' does not exist!";
            updateRemotePreservationStateToFailState(prs, State.PRESERVATION_REQUEST_FAILED,
                    errMsg);
            throw new YggdrasilException(errMsg);
        }
        File xslFile = new File(xsltDir, metadataModel.getMapper().get(modelToUse));
        if (!xslFile.isFile()) {
            final String errMsg = "The needed xslt-script '" + xslFile.getAbsolutePath()
                    + "' does not exist!";
            updateRemotePreservationStateToFailState(prs, State.PRESERVATION_REQUEST_FAILED,
                    errMsg);
            throw new YggdrasilException(errMsg);
        }

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
            XmlErrorHandler errorHandler = new XmlErrorHandler();
            XmlValidationResult result = new XmlValidationResult();
            boolean bValid = new XmlValidator().testDefinedValidity(new FileInputStream(xmlFile), entityResolver, errorHandler, result);
            if (!bValid) {
                updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
                String errMsg = "The output metadata is invalid: ";
                try {
                    errMsg += FileUtils.readFileToString(xmlFile);
                } catch (IOException e) {
                    logger.warn("Exception while reading output file:", e);
                }
                // Add errors/warnings to errmsg, so Valhal gets to see them.
                if (errorHandler.hasErrors()) {
                    if (!errorHandler.errors.isEmpty()) {
                        errMsg += "Errors: \n";
                        for (String error: errorHandler.errors) {
                            errMsg += error + "\n";
                        }
                        errMsg += "\n";
                    }
                    if (!errorHandler.fatalErrors.isEmpty()) {
                        errMsg += "Fatal errors: \n";
                        for (String fatalerror: errorHandler.fatalErrors) {
                            errMsg += fatalerror + "\n";
                        }
                    }
                    if (!errorHandler.warnings.isEmpty()) {
                        errMsg += "Warnings: \n";
                        for (String warning: errorHandler.warnings) {
                            errMsg += warning + "\n";
                        }
                    }
                }
                updateRemotePreservationStateToFailState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE,
                        errMsg);
                throw new YggdrasilException(errMsg);
            } else {
                prs.setMetadataPayload(outputFile);
                updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY);
            }
        } catch (TransformerConfigurationException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            updateRemotePreservationStateToFailState(
                    prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE, errMsg);
            throw new YggdrasilException(errMsg);
        } catch (TransformerException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
            throw new YggdrasilException(errMsg);
        } catch (FileNotFoundException e) {
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
    private void doContentAndMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException, FileNotFoundException {
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
    private void doMetadataWorkflow(String currentUUID, PreservationRequestState prs) throws YggdrasilException, FileNotFoundException {
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
    private void fetchContent(String currentUUID, PreservationRequestState prs) throws YggdrasilException, FileNotFoundException {
        // Try to download resource from Content_URI
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
                updateRemotePreservationStateToFailState(prs, State.PRESERVATION_REQUEST_FAILED, reason);
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
     * Set remote preservation state to a failstate with a given reason.
     * @param prs a given preservationrequeststate
     * @param failState The given failstate.
     * @param reason The reason for failing this request
     * @throws YggdrasilException
     */
    private void updateRemotePreservationStateToFailState(PreservationRequestState prs,
            State failState,
            String reason) throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.id = prs.getRequest().Valhal_ID;
        response.model = prs.getRequest().Model;
        response.preservation = new Preservation();
        response.preservation.preservation_state = failState.name();
        response.preservation.preservation_details = reason;
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        //HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        sendPreservationResponseToMQ(responseBytes);
        logger.info("Preservation status updated to '" + failState.name()
                +  "' using the updateURI. Reason: " + reason );
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
        response.id = prs.getRequest().Valhal_ID;
        response.model = prs.getRequest().Model;
        response.preservation = new Preservation();
        response.preservation.preservation_state = newPreservationstate.name();
        response.preservation.preservation_details = newPreservationstate.getDescription();
        if (prs.getUploadPackage() != null) {
            response.preservation.warc_id = prs.getUploadPackage().getName();
        }
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);

        /* send to RabbitMQ */
        sendPreservationResponseToMQ(responseBytes);

        //HttpCommunication.post(prs.getRequest().Update_URI, responseBytes, "application/json");
        logger.info("Preservation status updated to '" + newPreservationstate.name()
                +  "' using the updateURI.");
    }

    /**
     * Sends a PreservationResponse message on the MessageQueue. 
     * @param responseBytes The bytes of the message to send.
     * @throws YggdrasilException If issue with connecting to MQ or sending the message. 
     */
    private void sendPreservationResponseToMQ(byte[] responseBytes) throws YggdrasilException {
        mq.publishOnQueue(mq.getSettings().getPreservationResponseDestination(), responseBytes, MQ.PRESERVATIONRESPONSE_MESSAGE_TYPE);
    }

    /**
     * Wait until the next request arrives on the queue, and then return the request.
     * @return the next request from the queue (returns null, if shutdown message)
     * @throws YggdrasilException If bad messagetype
     * @throws RabbitException When message queue connection fails.
     */
    private PreservationRequest getNextRequest() throws YggdrasilException, RabbitException {
        // TODO Should there be a timeout here?
        MqResponse requestContent = mq.receiveMessageFromQueue(
                mq.getSettings().getPreservationDestination());
        final String messageType = requestContent.getMessageType();
        if (messageType == null) {
            throw new YggdrasilException("'null' messagetype is not handled. message ignored ");
        } else if (messageType.equalsIgnoreCase(MQ.SHUTDOWN_MESSAGE_TYPE)) {
            logger.warn("Shutdown message received");
            // Shutdown message received
            return null;
        } else if (messageType.equalsIgnoreCase(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE)) {
            PreservationRequest request = JSONMessaging.getPreservationRequest(
                    new PushbackInputStream(new ByteArrayInputStream(requestContent.getPayload())
                    , PUSHBACKBUFFERSIZE));
            return request;
        } else {
            throw new YggdrasilException("The message type '"
                    + messageType + "' is not handled by Yggdrasil.");
        }
    }

    /**
     * Generate the WarcInfoPayload that Yggdrasil inserts into the warcfiles being
     * produced.
     *
        WARC/1.0
        WARC-Type: warcinfo
        WARC-Date: 2013-05-27T16:34:07Z
        WARC-Record-ID: <urn:uuid:7c9cb0b0-c6da-11e2-aa30-005056887b70>
        Content-Type: application/warc-fields
        Content-Length: 85

        description: http://id.kb.dk/authorities/agents/kbDkDomsBmIngest.html
        revision: 2079
        conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
     *
     * @return the WarcInfoPayload that Yggdrasil inserts into the warcfiles being
     * produced
     */
    public String getWarcInfoPayload() {
        // make Warc-metadata record (WARC-INFO RECORD) containing
        // link to program description, archiverRevision, and conformsTo "ISO"
        //
        final String LF = "\n";
        final String COLON = ":";
        final String SPACE = " ";

        // 1. description: http://id.kb.dk/authorities/agents/kbDkYggdrasilIngest.html
        String descriptionKey = "description";
        String descriptionValue = Agent.getIngestAgentURL();
        // 2. archiverRevision:
        String revisionKey = "revision";
        String revisionValue = "1.0.0";

        // 3. conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
        String conformsToKey = "conformsTo";
        String conformsToValue = "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf";

        StringBuilder sb = new StringBuilder();
        sb.append(descriptionKey + COLON + SPACE + descriptionValue + LF);
        sb.append(revisionKey + COLON + SPACE + revisionValue + LF);
        sb.append(conformsToKey + COLON + SPACE + conformsToValue + LF);
        
        return sb.toString();
    }

}
