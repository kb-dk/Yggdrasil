package dk.kb.yggdrasil.preservation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;

import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.HttpPayload;
import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.xslt.Models;
import dk.kb.yggdrasil.xslt.XmlErrorHandler;
import dk.kb.yggdrasil.xslt.XmlValidationResult;
import dk.kb.yggdrasil.xslt.XmlValidator;
import dk.kb.yggdrasil.xslt.XslErrorListener;
import dk.kb.yggdrasil.xslt.XslTransformer;
import dk.kb.yggdrasil.xslt.XslUriResolver;

public class PreservationRequestHandler {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** The class reading the mapping between models and xslt scripts used for the metadata transformation. */
    private final Models metadataModel;
    /** Context for this preservation. */
    private final PreservationContext context;
    /** Preservation packaging manager. */
    private final PreservationPackagingManager preservationManager;
    
    /**
     * Constructor.
     * @param rabbitconnector The rabbitmq connector object
     * @param states the StateDatabase
     * @param bitrepository the interface with bitrepository
     * @param config general configuration
     * @param models metadatamodelMapper
     */
    public PreservationRequestHandler(PreservationContext context, Models models) {
        ArgumentCheck.checkNotNull(context, "PreservationContext context");
        ArgumentCheck.checkNotNull(models, "Models models");
        this.metadataModel = models;
        this.context = context;
        this.preservationManager = new PreservationPackagingManager(context);
    }

    /**
     * Handles the PreservationRequest.
     * @param request The preservation request to handle.
     * @throws YggdrasilException if anything goes wrong.
     * @throws FileNotFoundException If the data cannot be retrieved or handled (e.g. not enough space left on device).
     */
    public void handleRequest(PreservationRequest request) throws YggdrasilException, FileNotFoundException {
        String currentUUID = null;

        logger.info("Preservation request received.");
        PreservationRequestState prs = null;
        /* Validate message content. */
        if (!request.isMessageValid()) {
            logger.error("Skipping invalid message");
        } else {
            prs = new PreservationRequestState(request,
                    State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
            // Add check about whether the profile is a known collectionID or not known
            String preservationProfile = prs.getRequest().Preservation_profile;
            List<String> possibleCollections = context.getBitrepository().getKnownCollections();
            if (!possibleCollections.contains(preservationProfile)) {
                String errMsg = "The given preservation profile '" + preservationProfile
                        + "' does not match a known collection ID. Expected one of: " + possibleCollections;
                logger.error(errMsg);
                context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, 
                        State.PRESERVATION_REQUEST_FAILED, errMsg);
            } else {
                currentUUID = request.UUID;
                context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_REQUEST_RECEIVED);
                context.getStateDatabase().put(currentUUID, prs);
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
     * Performs the Content and Metadata workflow.
     * @param currentUUID The UUID of the current request
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void doContentAndMetadataWorkflow(String currentUUID, PreservationRequestState prs) 
            throws YggdrasilException, FileNotFoundException {
        try {
            logger.info("Starting a Content and metadata workflow for UUID '" + currentUUID + "'");
            fetchContent(currentUUID, prs);
            transformMetadata(currentUUID, prs);
            preservationManager.addToWarcFile(prs.getRequest().Preservation_profile, prs);
            logger.info("Finished the content metadata workflow for UUID '" + currentUUID + "' successfully");
        } catch (YggdrasilException e) {
            logger.error("An exception occurred during the workflow for UUID: "
                    + currentUUID, e);
        }
    }

    /**
     * Performs the Metadata workflow. This is currently a method stub.
     * @param currentUUID The UUID of the element
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void doMetadataWorkflow(String currentUUID, PreservationRequestState prs) 
            throws YggdrasilException, FileNotFoundException {
        logger.info("Starting a metadata workflow for UUID '" + currentUUID + "'");
        transformMetadata(currentUUID, prs);
        preservationManager.addToWarcFile(prs.getRequest().Preservation_profile, prs);
        logger.info("Finished the metadata workflow for UUID '" + currentUUID + "' successfully");
    }

    /**
     * Transform the metadata included with the request to the proper METS preservation format.
     * @param prs The current request
     * @param currentUUID The UUID of the current request
     * @throws YggdrasilException
     *
     */
    private void transformMetadata(String currentUUID, PreservationRequestState prs) 
            throws YggdrasilException, FileNotFoundException {
        String theMetadata = prs.getRequest().metadata;
        String modelToUse = prs.getRequest().Model.toLowerCase();

        if (!metadataModel.getMapper().containsKey(modelToUse)) {
            final String errMsg = "The given metadata-model'" + modelToUse
                    + "' is unknown. Expected one of: " + metadataModel.getMapper().keySet();
            context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_REQUEST_FAILED, errMsg);
            throw new YggdrasilException(errMsg);
        }
        File xsltDir = new File(context.getConfig().getConfigDir(), "xslt");
        if (!xsltDir.isDirectory()) {
            final String errMsg = "The xslt directory '" + xsltDir.getAbsolutePath()
                    + "' does not exist!";
            context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_REQUEST_FAILED, errMsg);
            throw new YggdrasilException(errMsg);
        }
        File xslFile = new File(xsltDir, metadataModel.getMapper().get(modelToUse));
        if (!xslFile.isFile()) {
            final String errMsg = "The needed xslt-script '" + xslFile.getAbsolutePath()
                    + "' does not exist!";
            context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_REQUEST_FAILED, errMsg);
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
            outputFile = new File(context.getConfig().getTemporaryDir(), UUID.randomUUID().toString());
            Result outputTarget = new StreamResult(outputFile);
            xsltransform.transform(xmlSource, uriResolver, errorListener, outputTarget);
            EntityResolver entityResolver = null;
            File xmlFile = outputFile;
            XmlErrorHandler errorHandler = new XmlErrorHandler();
            XmlValidationResult result = new XmlValidationResult();
            boolean bValid = new XmlValidator().testDefinedValidity(new FileInputStream(xmlFile), entityResolver, errorHandler, result);
            if (!bValid) {
                context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
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
                context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, 
                        State.PRESERVATION_METADATA_PACKAGED_FAILURE, errMsg);
                throw new YggdrasilException(errMsg);
            } else {
                prs.setMetadataPayload(outputFile);
                context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY);
            }
        } catch (TransformerConfigurationException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE, 
                    errMsg);
            throw new YggdrasilException(errMsg);
        } catch (TransformerException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
            throw new YggdrasilException(errMsg);
        } catch (FileNotFoundException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + currentUUID + "'";
            logger.error(errMsg, e);
            context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_METADATA_PACKAGED_FAILURE);
            throw new YggdrasilException(errMsg);
        }
    }

    /**
     * Try and download the content using the Content_URI.
     * @param currentUUID The UUID of the current request
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void fetchContent(String currentUUID, PreservationRequestState prs) 
            throws YggdrasilException, FileNotFoundException {
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
                context.getRemotePreservationStateUpdater().updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_REQUEST_FAILED, reason);
                throw new YggdrasilException(reason, e);
            }
            prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
            prs.setContentPayload(tmpFile);
            context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
            context.getStateDatabase().put(currentUUID, prs);
        } else {
            prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE);
            context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE);
            context.getStateDatabase().put(currentUUID, prs);
        }
    }
}
