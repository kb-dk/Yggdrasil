package dk.kb.yggdrasil.preservation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.jwat.common.ContentType;
import org.jwat.common.Uri;
import org.jwat.warc.WarcDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
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
    
    private final PreservationContext context;
    /** The collection id for this manager.*/
    private final String collectionId;
    /** The writer of the WARC file.*/
    private WarcWriterWrapper writer;
    /** The list of states for the preservationRequests to be stored in the WARC file.*/
    private List<PreservationRequestState> preservationRequests;
    /** */
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
     * @throws YggdrasilException  
     */
    public synchronized void verifyConditions() throws YggdrasilException {
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
     * Write the contentPaylod and transformed of the preservation record.
     * @param prs The record of the preservation request to write.
     */

    public synchronized void writePreservationRecord(PreservationRequestState prs) throws YggdrasilException {
        checkInitialize();
        preservationRequests.add(prs);
        try {
            Uri resourceId = null;
            Digest digestor = new Digest("SHA-1");
            File resource = prs.getContentPayload();
            File metadata = prs.getMetadataPayload();
            InputStream in = null;
            if (resource != null) {
                try {
                    in = new FileInputStream(resource);
                    WarcDigest blockDigest = digestor.getDigestOfFile(resource);
                    resourceId = writer.writeResourceRecord(in, resource.length(),
                            ContentType.parseContentType("application/binary"), blockDigest, prs.getRequest().File_UUID);
                } finally {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                }
            }
            if (metadata != null) {
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
            prs.setUploadPackage(writer.getWarcFile());
        } catch (FileNotFoundException e) {
            throw new YggdrasilException("Horrible exception while writing WARC record!", e);
        } catch (IOException e) {
            throw new YggdrasilException("Horrible exception while writing WARC record!", e);
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
            preservationRequests = new ArrayList<PreservationRequestState>();
            initializeNewWarcFile();
            logger.debug("Initialising new WARC file: " + writer.getWarcFileId() + ", with size limit: " 
                    + context.getConfig().getWarcSizeLimit() + ", date limit: " + new Date(currentTimeout));
        }
    }
    
    /**
     * Uploads the Warc file to the Bitrepository.
     * @throws YggdrasilException
     */
    private void uploadWarcFile() throws YggdrasilException {
        boolean success = context.getBitrepository().uploadFile(writer.getWarcFile(), collectionId);
        
        for(PreservationRequestState prs : preservationRequests) {
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
        context.getRemotePreservationStateUpdater().updateRemotePreservationState(prs, 
                preservationState);
        context.getStateDatabase().put(prs.getUUID(), prs);
    }
    
    /**
     * 
     * @throws YggdrasilException
     */
    private void cleanUp() throws YggdrasilException {
        preservationRequests.clear();
        if(writer != null) {
            boolean deleteSuccess = writer.getWarcFile().delete();
            logger.debug("Cleaned up file: succesfully removed from disc: " + deleteSuccess);
            writer.close();
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
}
