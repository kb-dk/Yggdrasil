package dk.kb.yggdrasil.preservationimport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.bitrepository.bitrepositoryelements.ChecksumType;
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
import dk.kb.yggdrasil.json.preservationimport.Security;
import dk.kb.yggdrasil.messaging.MessageRequestHandler;

/**
 * The handler class for preservation import requests.
 */
public class PreservationImportRequestHandler extends MessageRequestHandler<PreservationImportRequest> {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Context for this preservation. */
    private final RequestHandlerContext context;
    
    /** The size of the buffer. */
    private static final int BUFFER_SIZE = 16*1024;
    
    /** The format for the timeout date. */
    private static final String DEFAULT_TIMEOUT_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    /**
     * Constructor.
     * @param context The context for the preservation import.
     */
    public PreservationImportRequestHandler(RequestHandlerContext context) {
        ArgumentCheck.checkNotNull(context, "PreservationContext context");
        this.context = context;
    }

    /**
     * Handles the PreservationImportRequest.
     * @param request The preservation import request to handle.
     * @throws YggdrasilException if anything goes wrong.
     */
    @Override
    public void handleRequest(PreservationImportRequest request) throws YggdrasilException {
        logger.info("Preservation request received.");
        if (!request.isMessageValid()) {
            logger.error("Skipping invalid message");
            return;
        }

        PreservationImportRequestState state = new PreservationImportRequestState(request, 
                PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED);

        if(!validateRequest(state)) {
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
     * Performing the import operation.
     * @param state The state for handling the preservation import request.
     * @throws YggdrasilException If it fails.
     */
    public void performImport(PreservationImportRequestState state) throws YggdrasilException {
        logger.info("Starting to import '" + state.getRequest().type + "' for uuid '" + state.getRequest().uuid + "'");
        try {
            retrieveData(state);
            logger.info("Retrieved data for import of '" + state.getRequest().type + "' for uuid '" 
                    + state.getRequest().uuid + "'");

            validateExtractedData(state);

            validateTokenDate(state);
            
            logger.info("Starting to deliver data for import '" + state.getRequest().type + "' for uuid '" 
                    + state.getRequest().uuid + "'");

            deliverData(state);

            // Send final success response.
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_FINISHED, null);

            // cleanup
            state.cleanup();
            logger.info("Finished processing the preservation import request of '" + state.getRequest().type 
                    + "' for uuid '" + state.getRequest().uuid + "'");
        } catch (YggdrasilException e) {
            // Send failure, if it is not a fail-state.
            if(state.getState().isOkState()) {
                context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                        PreservationImportState.PRESERVATION_IMPORT_FAILURE, e.getMessage());
            }
            logger.error("Failure", e);
        }
    }

    /**
     * Validates the preservation import request.
     * Currently only validates the preservation profile against the possible bitrepository-collections.
     * @param request The preservation import request to validate.
     * @return Whether or not the request is valid.
     */
    protected boolean validateRequest(PreservationImportRequestState state) throws YggdrasilException {
        List<String> errors = new ArrayList<String>();

        // Add check about whether the profile is a known collectionID or not known
        String preservationProfile = state.getRequest().preservation_profile;
        List<String> possibleCollections = context.getBitrepository().getKnownCollections();
        if (!possibleCollections.contains(preservationProfile)) {
            String errMsg = "The given preservation profile '" + preservationProfile
                    + "' does not match a known collection ID. Expected one of: " + possibleCollections;
            logger.error(errMsg);
            errors.add(errMsg);
        }
        
        // Check the type. Must be 'FILE' 
        // TODO Handle other types than FILE.
        if(!state.getRequest().type.equalsIgnoreCase("FILE")) {
            String errMsg = "The given preservation profile '" + preservationProfile
                    + "' does not match a known collection ID. Expected one of: " + possibleCollections;
            logger.error(errMsg);
            errors.add(errMsg);            
        }
        
        // validate the delivery URL
        try {
            new URL(state.getRequest().url);
        } catch (MalformedURLException e) {
            String errMsg = "Malformed URL: " + state.getRequest().url;
            logger.error(errMsg, e);
            errors.add(errMsg);
        }
        
        // validate checksum format: 'algorithm':'checksum'
        if(state.getRequest().security != null && state.getRequest().security.checksum != null
                && !state.getRequest().security.checksum.isEmpty()) {
            String checksum = state.getRequest().security.checksum;
            if(!checksum.contains(":")) {
                String errMsg = "The checksum in the request does not comply with definition. No algorithm";
                logger.error(errMsg);
                errors.add(errMsg);
            } else {
                try {
                    extractChecksumType(checksum);
                } catch (YggdrasilException e) {
                    logger.error(e.getMessage());
                    errors.add(e.getMessage());
                }
            }
        }
        
        if(errors.isEmpty()) {
            // Send update about success retrieval and validation.
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED, null);
            return true;
        } else {
            // Send the update about validation failure.
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE, errors.toString());
            return false;
        }
    }

    /**
     * Extracts the WARC data from the Bitrepository.
     * @param request The request containing about which warc file to retrieve and which 
     * Bitrepository collection to retrieve the warc file from.  
     * @return The warc file.
     * @throws YggdrasilException If retrieving the warc file from the Bitrepository fails.
     */
    protected void retrieveData(PreservationImportRequestState state) throws YggdrasilException {
        context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED, null);
        try {
            if(state.getImportData() == null || !state.getImportData().isFile()) {
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
                logger.info("Retrieved data from Bitrepository for '" + state.getRequest().uuid + "'.");
            } else {
                logger.warn("Already having retrieved the data. This must be recovery from "
                        + "failure or unexpected shutdown.");
            }
        } catch (YggdrasilException e) {
            // Sending retrieval failure response.
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Extracts the warc-record payload from the warc-file.
     * @param warcFile The warc file.
     * @param request The request containing information about which warc record to extract.
     * @return A file containing the warc-record payload.
     * @throws YggdrasilException If the extraction of the warc-record fails.
     */
    protected File extractData(File warcFile, PreservationImportRequestState state) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(warcFile, "File warcFile");
        try (InputStream in = new FileInputStream(warcFile);) {
            WarcRecord retrievedRecord = null;
            Uri uuid = new Uri("urn:uuid:" + state.getRequest().warc.warc_record_id);        
            WarcReader reader = WarcReaderFactory.getReader( in );
            WarcRecord record;
            while (retrievedRecord == null && (record = reader.getNextRecord()) != null) {
                if(record.header.warcRecordIdUri.equals(uuid)) {
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
            int i;
            while((i = in.read(read)) > -1) {
                out.write(read, 0, i);
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
        if(state.getRequest().security == null || state.getRequest().security.checksum == null 
                || state.getRequest().security.checksum.isEmpty()) {
            logger.debug("No checksum to validate ");
            return;
        }
        ChecksumType csType = extractChecksumType(state.getRequest().security.checksum);
        String deliveredChecksum = state.getRequest().security.checksum.split(":")[1];
        String calculatedChecksum = ChecksumUtils.generateChecksum(state.getImportData(), csType);
        
        // Validate against delivered checksum.
        if(!calculatedChecksum.equalsIgnoreCase(deliveredChecksum)) {
            String errMsg = "Inconsistent checksum between retrieved file ('" + calculatedChecksum 
                    + "') and the delivered checksum ('" + deliveredChecksum + "') in the algorithm '" 
                    + csType.name() + "'.";
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_FAILURE, errMsg);

            throw new YggdrasilException(errMsg);
        }
        
        // Validate against warc-header checksum
        if(state.getWarcHeaderChecksum() == null || state.getWarcHeaderChecksum().isEmpty() 
                || !state.getWarcHeaderChecksum().contains(":")) {
            logger.warn("Cannot validate against header fields. Continuing anyway.");
            return;
        }
        ChecksumType headerCsType = extractChecksumType(state.getWarcHeaderChecksum());
        String headerChecksum = state.getWarcHeaderChecksum().split(":")[1];
        String checksumForHeader;
        if(headerCsType == csType) {
            checksumForHeader = calculatedChecksum;             
        } else {
            checksumForHeader = ChecksumUtils.generateChecksum(state.getImportData(), headerCsType);
        }
        
        if(!headerChecksum.equalsIgnoreCase(checksumForHeader)){
            String errMsg = "Inconsistent checksum between retrieved file ('" + checksumForHeader 
                    + "') and the header checksum ('" + headerChecksum + "') in the algorithm '" 
                    + headerCsType.name() + "'.";
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_FAILURE, errMsg);

            throw new YggdrasilException(errMsg);
        }
    }
    
    /**
     * Validate the token timeout date. 
     * @param state The state containing the request with the token-timeout to validate.
     * @throws YggdrasilException If the timeout has already been reached.
     */
    private void validateTokenDate(PreservationImportRequestState state) throws YggdrasilException {
        Security s = state.getRequest().security;
        if(s != null && s.token != null && s.token_timeout != null) {
            try {
                DateFormat formatter = new SimpleDateFormat(DEFAULT_TIMEOUT_DATE_FORMAT);
                Date d = formatter.parse(s.token_timeout);
                if(d.getTime() < new Date().getTime()) {
                    throw new YggdrasilException("Token timeout (" + d.toString() + ") exceeded.");
                }
            } catch (ParseException e) {
                logger.warn("Could not parse the timeout date. Trying to continue anyway.", e);
            }
        } else {
            logger.debug("No timeout of the token to validate.");
        }
    }
    
    /**
     * Sends the file to the given URL, though security demands a token, then also deliver the token.
     * @param state The state of the preservation import request message handling.
     * @throws YggdrasilException If the data fails to be delivered.
     */
    private void deliverData(PreservationImportRequestState state) throws YggdrasilException {
        context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, null);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if(state.getRequest().security != null) {
            String token = state.getRequest().security.token;
            if(token != null && !token.isEmpty()) {
                builder.addTextBody("token", token, ContentType.TEXT_PLAIN);                    
            }
        }
        builder.addTextBody("uuid", state.getRequest().uuid);
        builder.addTextBody("type", state.getRequest().type);

        builder.addBinaryBody("file", state.getImportData());
        HttpEntity multipart = builder.build();
        boolean success = context.getHttpCommunication().post(state.getRequest().url, multipart);

        if(success) {
            logger.info("Successfully delivered data for '" + state.getRequest().uuid + "'");
        } else {
            // Failure. Send response telling about the error.
            String errMsg = "Could not deliver the data to '" + state.getRequest().url;
            context.getRemotePreservationStateUpdater().sendPreservationImportResponse(state, 
                    PreservationImportState.PRESERVATION_IMPORT_DELIVERY_FAILURE, errMsg);
            throw new YggdrasilException(errMsg);
        }
    }
    
    /**
     * Extracts the checksum type from a digestBlock.
     * @param digestBlock The digestBlock in format 'algorithm':'checksum'.
     * @return The checksum type.
     */
    private ChecksumType extractChecksumType(String digestBlock) throws YggdrasilException {
        if(!digestBlock.contains(":")) {
            throw new YggdrasilException("The checksum in the request does not comply with definition. "
                    + "No algorithm");
        } else {
            String checksumType = digestBlock.split(":")[0];
            // Remove any '-' from the SHA algorithms, and makes it upper-case.
            checksumType = checksumType.replaceFirst("-", "").toUpperCase();
            try {
                return ChecksumType.fromValue(checksumType);
            } catch (IllegalArgumentException e) {
                throw new YggdrasilException(checksumType + " is not supported.", e);
            }
        }
    }
}
