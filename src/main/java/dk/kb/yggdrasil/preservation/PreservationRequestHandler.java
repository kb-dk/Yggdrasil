package dk.kb.yggdrasil.preservation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
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
import dk.kb.yggdrasil.exceptions.PreservationException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.xslt.Models;
import dk.kb.yggdrasil.xslt.XmlErrorHandler;
import dk.kb.yggdrasil.xslt.XmlValidationResult;
import dk.kb.yggdrasil.xslt.XmlValidator;
import dk.kb.yggdrasil.xslt.XslErrorListener;
import dk.kb.yggdrasil.xslt.XslTransformer;
import dk.kb.yggdrasil.xslt.XslUriResolver;

/**
 * The handler class for preservation requests.
 */
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
     * @param context The context for the preservation.
     * @param models The metadatamodel mapper.
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
     */
    public void handleRequest(PreservationRequest request) throws YggdrasilException {
        logger.info("Preservation request received.");
        if (!request.isMessageValid()) {
            logger.error("Skipping invalid message");
            return;
        }
        PreservationRequestState prs = new PreservationRequestState(request,
                State.PRESERVATION_REQUEST_RECEIVED, request.UUID);

        try {
            if (validateMessage(prs)) {
                preserveRequest(prs);
            }
        } catch (PreservationException e) {
            // Fault barrier to ensure, that failures will send update and remove stuff.
            logger.warn("Preservation message handling fault barrier caught exception.", e);
            context.getRemotePreservationStateUpdater().sendPreservationResponseWithSpecificDetails(prs, 
                    e.getState(), e.getMessage());
            context.getStateDatabase().delete(prs.getUUID());
            throw new YggdrasilException(e.getMessage(), e);
        }
        logger.info("Finished processing the preservation request");
    }

    /**
     * Validates whether the content of the request is valid.
     * @param prs The preservation request state.
     * @return Whether or not is is valid.
     * @throws YggdrasilException
     */
    private boolean validateMessage(PreservationRequestState prs) throws YggdrasilException {
        // Add check about whether the profile is a known collectionID or not known
        String preservationProfile = prs.getRequest().Preservation_profile;
        List<String> possibleCollections = context.getBitrepository().getKnownCollections();
        if (!possibleCollections.contains(preservationProfile)) {
            String errMsg = "The given preservation profile '" + preservationProfile
                    + "' does not match a known collection ID. Expected one of: " + possibleCollections;
            logger.error(errMsg);
            context.getRemotePreservationStateUpdater().sendPreservationResponseWithSpecificDetails(prs, 
                    State.PRESERVATION_REQUEST_FAILED, errMsg);
            return false;
        } 
        
        context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                State.PRESERVATION_REQUEST_RECEIVED);
        context.getStateDatabase().put(prs.getUUID(), prs);
        return true;
    }

    /**
     * 
     * @param prs
     * @throws YggdrasilException
     */
    private void preserveRequest(PreservationRequestState prs) throws PreservationException, YggdrasilException {
        if (prs.getRequest().Content_URI != null) {
            logger.info("Fetching content for preseravtion request with UUID '" + prs.getUUID() + "'");
            try {
                fetchContent(prs);
            } catch (IOException e) {
                String reason = "An issue occured when fetching the content for preservation request '" 
                        + prs.getUUID() + "'";
                throw new PreservationException(State.PRESERVATION_REQUEST_FAILED, reason, e);
            }
        }
        
        transformMetadata(prs);
        preservationManager.addToWarcFile(prs.getRequest().Preservation_profile, prs);
        logger.info("Finished handling the preservation request with UUID '" + prs.getUUID() + "' successfully");
    }

    /**
     * Transform the metadata included with the request to the proper METS preservation format.
     * @param prs The current request
     * @param currentUUID The UUID of the current request
     * @throws YggdrasilException Failure to transform the metadata.
     */
    private void transformMetadata(PreservationRequestState prs) throws PreservationException, YggdrasilException {
        String theMetadata = prs.getRequest().metadata;
        String modelToUse = prs.getRequest().Model.toLowerCase();

        if (!metadataModel.getMapper().containsKey(modelToUse)) {
            final String errMsg = "The given metadata-model'" + modelToUse
                    + "' is unknown. Expected one of: " + metadataModel.getMapper().keySet();
            throw new PreservationException(State.PRESERVATION_REQUEST_FAILED, errMsg);
        }
        File xsltDir = new File(context.getConfig().getConfigDir(), "xslt");
        if (!xsltDir.isDirectory()) {
            final String errMsg = "The xslt directory '" + xsltDir.getAbsolutePath()
                    + "' does not exist!";
            throw new PreservationException(State.PRESERVATION_REQUEST_FAILED, errMsg);
        }
        File xslFile = new File(xsltDir, metadataModel.getMapper().get(modelToUse));
        if (!xslFile.isFile()) {
            final String errMsg = "The needed xslt-script '" + xslFile.getAbsolutePath()
                    + "' does not exist!";
            throw new PreservationException(State.PRESERVATION_REQUEST_FAILED, errMsg);
        }

        try {
            InputStream metadataInputStream = null;
            FileInputStream xmlFileStream = null;
            File outputFile = null;
            try {
                XslTransformer xsltransform = XslTransformer.getTransformer(xslFile);
                XslUriResolver uriResolver = new XslUriResolver();
                XslErrorListener errorListener = new XslErrorListener();
                metadataInputStream = new ByteArrayInputStream(theMetadata.getBytes(Charset.defaultCharset()));
                Source xmlSource = new StreamSource(metadataInputStream);
                outputFile = new File(context.getConfig().getTemporaryDir(), UUID.randomUUID().toString());
                Result outputTarget = new StreamResult(outputFile);
                xsltransform.transform(xmlSource, uriResolver, errorListener, outputTarget);
                EntityResolver entityResolver = null;
                xmlFileStream = new FileInputStream(outputFile);
                XmlErrorHandler errorHandler = new XmlErrorHandler();
                XmlValidationResult result = new XmlValidationResult();
                boolean bValid = new XmlValidator().testDefinedValidity(xmlFileStream, entityResolver, 
                        errorHandler, result);
                if (!bValid) {
                    StringBuffer errMsg = new StringBuffer();
                    errMsg.append("The output metadata is invalid: ");
                    try {
                        errMsg.append(FileUtils.readFileToString(outputFile));
                    } catch (IOException e) {
                        logger.warn("Exception while reading output file:", e);
                    }
                    // Add errors/warnings to errmsg, so Valhal gets to see them.
                    if (errorHandler.hasErrors()) {
                        if (!errorHandler.errors.isEmpty()) {
                            errMsg.append("Errors: \n");
                            for (String error: errorHandler.errors) {
                                errMsg.append(error + "\n");
                            }
                            errMsg.append("\n");
                        }
                        if (!errorHandler.fatalErrors.isEmpty()) {
                            errMsg.append("Fatal errors: \n");
                            for (String fatalerror: errorHandler.fatalErrors) {
                                errMsg.append(fatalerror + "\n");
                            }
                        }
                        if (!errorHandler.warnings.isEmpty()) {
                            errMsg.append("Warnings: \n");
                            for (String warning: errorHandler.warnings) {
                                errMsg.append(warning + "\n");
                            }
                        }
                    }
                    throw new PreservationException(State.PRESERVATION_METADATA_PACKAGED_FAILURE, errMsg.toString());
                } else {
                    prs.setMetadataPayload(outputFile);
                    context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                            State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY);
                }
            } finally {
                if(xmlFileStream != null) {
                    xmlFileStream.close();
                }
            } 
        } catch (TransformerException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + prs.getUUID() + "'";
            throw new PreservationException(State.PRESERVATION_METADATA_PACKAGED_FAILURE, errMsg, e);
        } catch (IOException e) {
            final String errMsg = "Error occurred during transformation of metadata for uuid '"
                    + prs.getUUID() + "'";
            throw new PreservationException(State.PRESERVATION_METADATA_PACKAGED_FAILURE, errMsg, e);
        }
    }

    /**
     * Try and download the content using the Content_URI.
     * @param prs The current request
     * @throws YggdrasilException
     */
    private void fetchContent(PreservationRequestState prs) throws PreservationException, 
            YggdrasilException, IOException {
        // Try to download resource from Content_URI
        File tmpFile = null;
        PreservationRequest pr = prs.getRequest();
        logger.info("Attempting to download resource from '"
                + pr.Content_URI + "'");
        HttpPayload payload = HttpCommunication.get(pr.Content_URI);
        if (payload != null) {
            tmpFile = payload.writeToFile();
            prs.setContentPayload(tmpFile);
            context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                    State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
            context.getStateDatabase().put(prs.getUUID(), prs);
        } else {
            throw new PreservationException(State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE, 
                    "Failed to download resource.");
        }
    }
}
