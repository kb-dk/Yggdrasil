package dk.kb.yggdrasil.preservation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.jwat.common.ContentType;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConcurrentTo;
import org.jwat.warc.WarcDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.PreservationException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.Update;
import dk.kb.yggdrasil.warc.Digest;
import dk.kb.yggdrasil.warc.WarcWriterWrapper;
import dk.kb.yggdrasil.warc.YggdrasilWarcConstants;

/**
 * Class to manage the creation and upload of WARC files.
 * It will create one WARC record at the time, and new records will be added until one of following two 
 * conditions are met:
 * The size of the WARC file is too large, or too long time has passed since the first record was added.
 * When one of these conditions are met, then the WARC file is uploaded to the Bitrepository, replied 
 * to the 
 * 
 * This manager only handles a specific preservation collection.
 */
public class PreservationPacker {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** The context, containing settings, etc. */
    private final PreservationContext context;
    /** The collection id for this manager.*/
    private final String collectionId;
    /** The writer of the WARC file.*/
    private WarcWriterWrapper writer;
    /** The preservationRequests where the metadata are stored in the warc file.*/
    private List<PreservationRequestState> metadataRequests;
    /** The date for the current timeout. 0 until the writer is initialized. */
    private Long currentTimeout = 0L;

    /**
     * Constructor.
     * @param context The context for the preservation
     * @param collectionId The id of the collection.
     */
    public PreservationPacker(PreservationContext context, String collectionId) {
        this.context = context;
        this.collectionId = collectionId;
    }

    /**
     * Check the conditions, and upload if any of them has been met.
     */
    public synchronized void verifyConditions() {
        if(writer != null) {
            boolean conditionsMet = false;
            if (writer.getWarcFileSize() > context.getConfig().getWarcSizeLimit()) {
                conditionsMet = true;
                logger.debug("WARC file size limit reached.");
            } 
            if(new Date().getTime() > currentTimeout) {
                conditionsMet = true;
                logger.debug("Time limit reached.");
            }

            if(conditionsMet) {
                logger.info("Finished packaging WARC file. Uploading and cleaning up.");
                uploadWarcFile();
                cleanUp();
            }
        }
    }

    /**
     * Write the contentPaylod and transformed of the preservation records.
     * @param prs The record of the preservation request to write.
     * @throws YggdrasilException If it fails to write the preservation request state.
     * @throws PreservationException If it fails to perform the preservation.
     */
    public synchronized void writePreservationRecord(PreservationRequestState prs) throws YggdrasilException,
            PreservationException {
        checkInitialize();
        metadataRequests.add(prs);
        try {
            Uri resourceId = null;
            Digest digestor = new Digest("SHA-1");
            InputStream in = null;
            if (prs.getContentPayload() != null) {
                File resource = prs.getContentPayload();
                try {
                    in = new FileInputStream(resource);
                    WarcDigest blockDigest = digestor.getDigestOfFile(resource);
                    resourceId = writer.writeResourceRecord(in, resource.length(),
                            ContentType.parseContentType("application/binary"), blockDigest, 
                            prs.getRequest().File_UUID);
                } finally {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                }
                prs.setResourceWarcFile(writer.getWarcFile());
                context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                        State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS);
            }
            if (prs.getMetadataPayload() != null) {
                File metadata = prs.getMetadataPayload();
                try {
                    in = new FileInputStream(metadata);
                    WarcDigest blockDigest = digestor.getDigestOfFile(metadata);
                    writer.writeMetadataRecord(in, metadata.length(),
                            ContentType.parseContentType("text/xml"), resourceId, blockDigest,
                            prs.getRequest().UUID);
                    in.close();
                } finally {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                }
            }
            context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                    State.PRESERVATION_PACKAGE_COMPLETE);
            prs.setMetadataWarcFile(writer.getWarcFile());
            context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                    State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA);
        } catch (IOException e) {
            throw new PreservationException(State.PRESERVATION_METADATA_PACKAGED_FAILURE, 
                    "Error while writing WARC record!", e);
        }
    }
    
    /**
     * Write the contentPaylod and transformed of the preservation update records.
     * @param prs The record of the preservation update request to write.
     * @throws YggdrasilException If it fails to write the preservation request state.
     * @throws PreservationException If it fails to perform the preservation.
     */
    public synchronized void writeUpdateRecord(PreservationRequestState prs) throws YggdrasilException,
            PreservationException {
        checkInitialize();
        metadataRequests.add(prs);
        Update update = createUpdateElement(prs, writer.getWarcFile().getName());
        prs.setUpdatePreservation(update);
        try {
            Uri resourceId = null;
            Digest digestor = new Digest("SHA-1");
            InputStream in = null;
            if (prs.getContentPayload() != null) {
                File resource = prs.getContentPayload();
                try {
                    WarcConcurrentTo concurrentTo = new WarcConcurrentTo();
                    concurrentTo.warcConcurrentToStr = prs.getRequest().File_UUID;
                    in = new FileInputStream(resource);
                    WarcDigest blockDigest = digestor.getDigestOfFile(resource);
                    resourceId = writer.writeUpdateRecord(in, resource.length(), 
                            ContentType.parseContentType("application/binary"), null, 
                            Arrays.asList(concurrentTo), blockDigest, update.file_uuid);
                } finally {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                }
                context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                        State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS);
            }
            if (prs.getMetadataPayload() != null) {
                File metadata = prs.getMetadataPayload();
                try {
                    WarcConcurrentTo concurrentTo = new WarcConcurrentTo();
                    concurrentTo.warcConcurrentToStr = prs.getRequest().UUID;
                    in = new FileInputStream(metadata);
                    WarcDigest blockDigest = digestor.getDigestOfFile(metadata);
                    resourceId = writer.writeUpdateRecord(in, metadata.length(), 
                            ContentType.parseContentType("text/xml"), resourceId, 
                            Arrays.asList(concurrentTo), blockDigest, update.uuid);
                    in = new FileInputStream(metadata);
                    in.close();
                } finally {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                }
            }
            context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                    State.PRESERVATION_PACKAGE_COMPLETE);
            context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
                    State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA);
        } catch (IOException e) {
            throw new PreservationException(State.PRESERVATION_METADATA_PACKAGED_FAILURE, 
                    "Error while writing WARC record!", e);
        }
    }

    /**
     * Initializes the WARC file in necessary.
     * Also performs the condition check.
     * @throws YggdrasilException 
     */
    private void checkInitialize() throws YggdrasilException {
        verifyConditions();
        if(writer == null) {
            currentTimeout = new Date().getTime() + context.getConfig().getUploadWaitLimit();
            metadataRequests = new ArrayList<PreservationRequestState>();
            initializeNewWarcFile();
            logger.debug("Initialising new WARC file: " + writer.getWarcFileId() + ", with size limit: " 
                    + context.getConfig().getWarcSizeLimit() + ", date limit: " + new Date(currentTimeout));
        }
    }

    /**
     * Uploads the Warc file to the Bitrepository.
     * @throws YggdrasilException
     */
    private void uploadWarcFile() {
        boolean success = context.getBitrepository().uploadFile(writer.getWarcFile(), collectionId);

        try {
            for(PreservationRequestState prs : metadataRequests) {
                if(success) {
                    updateRequestState(State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS, prs);
                    logger.info("Upload to bitrepository for UUID '" + prs.getUUID()
                            + "' of package '" + writer.getWarcFileId() + "' was successful.");
                } else {
                    prs.resetUploadPackage(); // reset warcId to null
                    updateRequestState(State.PRESERVATION_PACKAGE_UPLOAD_FAILURE, prs);
                    logger.warn("Upload to bitrepository for UUID '" + prs.getUUID() + "' of package '" 
                            + writer.getWarcFileId() + "' failed.");
                }
                prs.cleanup();
                context.getStateDatabase().delete(prs.getUUID());
            } 
        } catch (YggdrasilException e) {
            logger.error("A error occured when reporting about bitrepository upload of the file '"
                    + writer.getWarcFileId() + "' to the collection '" + collectionId + "'. Trying to continue.", e);
        }
    }

    /**
     * Update the preservation state of the request, both locally and remote.
     * @param preservationState The new state.
     * @param prs The request to update.
     * @throws YggdrasilException If something goes wrong.
     */
    private void updateRequestState(State preservationState, PreservationRequestState prs) 
            throws YggdrasilException {
        prs.setState(preservationState);
        context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, preservationState);
        context.getStateDatabase().put(prs.getUUID(), prs);
    }

    /**
     * Cleans up the current warc writer.
     */
    protected void cleanUp() {
        metadataRequests.clear();
        if(writer != null) {
            //boolean deleteSuccess = writer.getWarcFile().delete();
            //logger.debug("Cleaned up file: succesfully removed from disc: " + deleteSuccess);
            try {
                writer.close();
            } catch (YggdrasilException e) {
                logger.warn("An issue occured when closing the current writer.", e);
            }
            writer = null;
        }
    }

    /**
     * Initializes the new WarcFile, with the WarcInfo.
     */
    private void initializeNewWarcFile()  throws YggdrasilException {
        UUID packageId = UUID.randomUUID();
        File writeDirectory = context.getConfig().getTemporaryDir();
        writer = WarcWriterWrapper.getWriter(writeDirectory, packageId.toString());

        try {
            Digest digestor = new Digest("SHA-1");
            String warcInfoPayload = YggdrasilWarcConstants.getWarcInfoPayload();
            byte[] warcInfoPayloadBytes = warcInfoPayload.getBytes("UTF-8");
            writer.writeWarcinfoRecord(warcInfoPayloadBytes,
                    digestor.getDigestOfBytes(warcInfoPayloadBytes));
        } catch (IOException e) {
            throw new YggdrasilException("Could not create the WARC info record.", e);
        }
    }
    
    /**
     * @param prs The preservation request state to 
     * @return The Update element for the preservation request state.
     */
    private Update createUpdateElement(PreservationRequestState prs, String warcId) {
        ArgumentCheck.checkTrue(prs.getContentPayload() != null || prs.getMetadataPayload() != null, 
                "Cannot create an update element with neither content nor metadata.");
        Update res = new Update();
        res.date = new Date().toString();

        if(prs.getContentPayload() != null) {
            res.file_uuid = UUID.randomUUID().toString();
            res.file_warc_id = warcId;
        }
        if(prs.getMetadataPayload() != null) {
            res.uuid = UUID.randomUUID().toString();
            res.warc_id = warcId;
        }
        return res;
    }
}
