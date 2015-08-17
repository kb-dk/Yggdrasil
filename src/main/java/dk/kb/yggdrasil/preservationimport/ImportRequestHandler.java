package dk.kb.yggdrasil.preservationimport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import org.bitrepository.bitrepositoryelements.FilePart;
import org.bitrepository.common.utils.ChecksumUtils;
import org.jwat.common.Uri;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.RequestHandlerContext;
import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.messaging.MessageRequestHandler;

/**
 * The handler class for preservation import requests.
 */
public class ImportRequestHandler extends MessageRequestHandler<PreservationImportRequest>{
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Context for this preservation. */
    private final RequestHandlerContext context;
    
    private static final int BUFFER_SIZE = 16*1024;

    /**
     * Constructor.
     * @param context The context for the preservation import.
     */
    public ImportRequestHandler(RequestHandlerContext context) {
        ArgumentCheck.checkNotNull(context, "PreservationContext context");
        this.context = context;
    }

    /**
     * Handles the PreservationImportRequest.
     * @param request The preservation import request to handle.
     * @throws YggdrasilException if anything goes wrong.
     */
    public void handleRequest(PreservationImportRequest request) throws YggdrasilException {
        logger.info("Preservation request received.");
        if (!request.isMessageValid()) {
            logger.error("Skipping invalid message");
            return;
        }

        PreservationImportRequestState state = new PreservationImportRequestState(request, PreservationImportState.IMPORT_REQUEST_RECEIVED_AND_VALIDATED);

        if(!validateRequest(request)) {
            logger.warn("The request is invalid: " + request.toString());
            return;
        }

        performImport(state);
    }

    @Override
    public PreservationImportRequest extractRequest(byte[] b) throws YggdrasilException {
        return JSONMessaging.getRequest(new PushbackInputStream(new ByteArrayInputStream(b), PUSHBACKBUFFERSIZE), 
                PreservationImportRequest.class);
    }
    
    /**
     * 
     * @param state
     * @throws YggdrasilException
     */
    public void performImport(PreservationImportRequestState state) throws YggdrasilException {
        try {
            if(state.getImportData() == null || !state.getImportData().isFile()) {
                retrieveData(state);
                logger.info("Retrieved data from Bitrepository for '" + state.getRequest().uuid + "'.");
            } else {
                logger.warn("Already having retrieved the data. This must be recovery from "
                        + "failure or unexpected shutdown.");
            }
        } catch (YggdrasilException e) {
            // TODO SEND RETRIEVAL FAILURE RESPONSE.
            throw e;
        }

        validateExtractedData(state);

        // deliver data
        deliverData(state);

        // TODO send final success response.
        
        // cleanup

        logger.info("Finished processing the preservation import request");

    }

    /**
     * Validates the preservation import request.
     * Currently only validates the preservation profile against the possible bitrepository-collections.
     * @param request The preservation import request to validate.
     * @return Whether or not the request is valid.
     */
    protected boolean validateRequest(PreservationImportRequest request) {
        // Add check about whether the profile is a known collectionID or not known
        String preservationProfile = request.preservation_profile;
        List<String> possibleCollections = context.getBitrepository().getKnownCollections();
        if (!possibleCollections.contains(preservationProfile)) {
            String errMsg = "The given preservation profile '" + preservationProfile
                    + "' does not match a known collection ID. Expected one of: " + possibleCollections;
            logger.error(errMsg);

            // TODO Send the update about validation failure.
            //            context.getRemotePreservationStateUpdater().sendPreservationResponseWithSpecificDetails(prs, 
            //                    PreservationState.PRESERVATION_REQUEST_FAILED, errMsg);
            return false;
        } 

        // TODO send update about success retrieval and validation.
        //        context.getRemotePreservationStateUpdater().sendPreservationResponse(prs, 
        //                PreservationState.PRESERVATION_REQUEST_RECEIVED);
        //        context.getStateDatabase().put(prs.getUUID(), prs);
        return true;
    }

    /**
     * Extracts the WARC data from the Bitrepository.
     * @param request The request containing about which warc file to retrieve and which 
     * Bitrepository collection to retrieve the warc file from.  
     * @return The warc file.
     * @throws YggdrasilException If retrieving the warc file from the Bitrepository fails.
     */
    protected void retrieveData(PreservationImportRequestState state) throws YggdrasilException {
        FilePart filePart = null;
        PreservationImportRequest request = state.getRequest();
        if(state.getRequest().warc.warc_offset != null && request.warc.warc_record_size != null) {
            filePart = new FilePart();
            filePart.setPartOffSet(BigInteger.valueOf(Long.parseLong(request.warc.warc_offset)));
            filePart.setPartLength(BigInteger.valueOf(Long.parseLong(request.warc.warc_record_size)));
        }
        File warcFile = context.getBitrepository().getFile(request.warc.warc_file_id, 
                request.preservation_profile, filePart);
        
        File record = extractData(warcFile, state);
        state.setImportData(record);
        // TODO send update to tell, that retrieval is complete.
    }
    
    /**
     * Extracts the warc-record payload from the warc-file.
     * @param warcFile The warc file.
     * @param request The request containing information about which warc record to extract.
     * @return A file containing the warc-record payload.
     * @throws YggdrasilException If the extraction of the warc-record fails.
     */
    protected File extractData(File warcFile, PreservationImportRequestState state) throws YggdrasilException {
        try (InputStream in = new FileInputStream(warcFile);) {
            WarcRecord retrievedRecord = null;
            Uri uuid = new Uri("urn:uuid:" + state.getRequest().warc.warc_record_id);        
            WarcReader reader = WarcReaderFactory.getReader( in );
            WarcRecord record;
            while ((record = reader.getNextRecord()) != null && retrievedRecord == null) {
                if(record.header.warcRecordIdUri == uuid) {
                    retrievedRecord = record;
                    state.setWarcHeaderChecksum(record.header.warcBlockDigestStr);
                }
            }
            if(retrievedRecord == null) {
                String errMsg = "Did not find the record";
                logger.warn(errMsg);
                throw new YggdrasilException(errMsg);
            }
            
            return extractRecordPayloadAsFile(retrievedRecord);
        } catch (IOException e) {
            throw new YggdrasilException("Could not extract the data from the warc file.", e);
        } catch (URISyntaxException e) {
            throw new YggdrasilException("URI for the warc-record is invalid.", e);
        }
    }
    
    /**
     * Extracts the payload from the warc-record.
     * @param record The warc record.
     * @return A file containing the payload of the warc-record.
     * @throws YggdrasilException If the extraction fails.
     */
    private File extractRecordPayloadAsFile(WarcRecord record) throws YggdrasilException {
        File res = new File(context.getConfig().getTemporaryDir(), "warc-record-" + new Date().getTime());
        InputStream in = record.getPayloadContent();
        try (FileOutputStream out = new FileOutputStream(res);){
            byte[] read = new byte[BUFFER_SIZE];
            while(in.read(read) > -1) {
                out.write(read);            
            }
            out.flush();
        } catch (IOException e) {
            throw new YggdrasilException("Could not extract warc record content into seperate file.", e);
        }
        
        return res;
    }
    
    /**
     * Validates the extracted data against the optional security-checksum in the request (if it is there).
     * @param state The preservation import request state.
     * @throws YggdrasilException If the extracted data is not valid.
     */
    private void validateExtractedData(PreservationImportRequestState state) throws YggdrasilException {
        if(state.getRequest().security.checksum == null || state.getRequest().security.checksum.isEmpty()) {
            logger.debug("No checksum to validate ");
            return;
        }
        String checksum = ChecksumUtils.generateChecksum(state.getImportData(), context.getBitrepository().getDefaultChecksum());
        
        if(!checksum.equalsIgnoreCase(state.getRequest().security.checksum)) {
            String errMsg = "Inconsistent checksum between retrieved file ('" + checksum 
                    + "') and the expected checksum ('" + state.getRequest().security.checksum + "')";
            // TODO send failure update.
            throw new YggdrasilException(errMsg);
        }
        
        // TODO validate against header checksum!
    }
    
    /**
     * Sends the file to the given URL, though security demands a token, then also deliver the token.
     * @param state The state of the preservation import request message handling.
     * @throws YggdrasilException If the data fails to be delivered.
     */
    private void deliverData(PreservationImportRequestState state) throws YggdrasilException {
        // TODO fix reality.
    }
}
