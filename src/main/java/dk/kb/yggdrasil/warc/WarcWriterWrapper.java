package dk.kb.yggdrasil.warc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import org.jwat.common.ContentType;
import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Wrapper class to hide away WARC writing internals.
 */
public class WarcWriterWrapper {

    /** Logging mechanism. */
    private static final Logger logger = LoggerFactory.getLogger(WarcWriterWrapper.class.getName());

    /** UUID of package/WARC file. */
    protected String uuid;

    /** WARC file. */
    protected File writerFile;

    /** WARC <code>RandomAccessFile</code>.  */
    protected RandomAccessFile writer_raf;

    /** <code>RandomAccessFile</code> as an <code>OutputStream</code> */
    protected RandomAccessFileOutputStream writer_rafout;

	/** WARC writer implementation. */
    protected WarcWriter writer;

    /** Is the WARC file new or not. */
    protected boolean bIsNew;

    /**
     * Open new or existing WARC file.
     * @param path parent path where the file must be created/opened
     * @param uuid uuid of WARC file
     * @return WARC writer wrapper
     * @throws YggdrasilException is an exception occurs
     */
    public static WarcWriterWrapper getWriter(File path, String uuid) throws YggdrasilException {
    	ArgumentCheck.checkExistsDirectory(path, "path");
    	ArgumentCheck.checkNotNullOrEmpty(uuid, "uuid");
    	WarcWriterWrapper w3 = null;
    	File writerFile = new File(path, uuid);
    	try {
        	if (writerFile.exists() && !writerFile.isFile()) {
        		logger.error("'" + uuid +"' appears to be an existing folder, this is disappointing.");
        		throw new YggdrasilException("'" + uuid +"' appears to be an existing folder, this is disappointing.");
        	}
        	w3 = new WarcWriterWrapper();
        	w3.uuid = uuid;
        	w3.writerFile = writerFile;
            w3.writer_raf = new RandomAccessFile(w3.writerFile, "rw");
            w3.writer_raf.seek(w3.writer_raf.length());
            w3.writer_rafout = new RandomAccessFileOutputStream(w3.writer_raf);
            w3.writer = WarcWriterFactory.getWriter(w3.writer_rafout, 8192, false);
            w3.writer.setExceptionOnContentLengthMismatch(true);
            w3.bIsNew = (w3.writer_raf.length() == 0L);
    	} catch (FileNotFoundException e) {
    		//logger.error("WARC file not found: " + writerFile.getPath(), e);
    		throw new YggdrasilException("Exception while opening WARC file", e);
    	} catch (IOException e) {
    		//logger.error("WARC file not found: " + writerFile.getPath(), e);
    		throw new YggdrasilException("Exception while opening WARC file", e);
    	}
        return w3;
    }

    /** WARC file Warcinfo id. */
    private Uri warcinfoRecordId;

    /**
     * Returns the WARC file Warcinfo id.
     * @return the WARC file Warcinfo id
     */
    public Uri getWarcinfoRecordId() {
    	return warcinfoRecordId;
    }

    /**
     * Append a Warcinfo record to WARC file.
     * @param warcFieldsBytes warc fields as byte array
     * @param blockDigest optional block digest
     * @return WarcRecordId of newly created record
     * @throws YggdrasilException if an exception occurs while writing record
     */
    public Uri writeWarcinfoRecord(byte[] warcFieldsBytes, WarcDigest blockDigest) throws YggdrasilException {
    	ArgumentCheck.checkNotNull(warcFieldsBytes, "warcFieldsBytes");
    	try {
            ByteArrayInputStream bin = new ByteArrayInputStream(warcFieldsBytes);
            //warcinfoRecordId = new Uri("urn:uuid:" + UUID.randomUUID());
            warcinfoRecordId = new Uri("urn:uuid:" + uuid);
            WarcRecord record = WarcRecord.createRecord(writer);
            WarcHeader header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_WARCINFO;
            header.warcDate = new Date();
            header.warcFilename = uuid;
            header.warcRecordIdUri = warcinfoRecordId;
            header.contentTypeStr = WarcConstants.CT_APP_WARC_FIELDS;
            header.warcBlockDigest = blockDigest;
            header.contentLength = new Long(warcFieldsBytes.length);
            writer.writeHeader(record);
            writer.streamPayload(bin);
            writer.closeRecord();
    	} catch (UnsupportedEncodingException e) {
    		throw new YggdrasilException("Exception while writing WARChive warcinfo record!", e);
    	} catch (URISyntaxException e) {
    		throw new YggdrasilException("Exception while writing WARChive warcinfo record!", e);
		} catch (IOException e) {
    		throw new YggdrasilException("Exception while writing WARChive warcinfo record!", e);
		}
    	return warcinfoRecordId;
    }

    /**
     * Append a resource record to WARC file.
     * @param in payload input stream
     * @param len payload length
     * @param contentType payload content-type
     * @param blockDigest optional block digest
     * @return WarcRecordId of newly created record
     * @throws YggdrasilException if an exception occurs while writing record
     */
    public Uri writeResourceRecord(InputStream in, long len, ContentType contentType, WarcDigest blockDigest) throws YggdrasilException {
    	ArgumentCheck.checkNotNull(in, "in");
    	ArgumentCheck.checkNotNull(len, "len");
    	ArgumentCheck.checkNotNull(contentType, "contentType");
    	Uri warcRecordIdUri = null;
    	try {
        	warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
        	WarcRecord record = WarcRecord.createRecord(writer);
        	WarcHeader header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_RESOURCE;
            header.warcDate = new Date();
            //header.warcIpAddress = "1.2.3.4";
            header.warcWarcinfoIdUri = warcinfoRecordId;
            header.warcRecordIdUri = warcRecordIdUri;
            header.warcTargetUriUri = warcRecordIdUri;
            //header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, responseOrRevisitRecordId, null);
            header.warcBlockDigest = blockDigest;
            header.contentType = contentType;
            header.contentLength = len;
            writer.writeHeader(record);
            writer.streamPayload(in);
            writer.closeRecord();
    	} catch (URISyntaxException e) {
    		throw new YggdrasilException("Exception while writing WARChive resource record!", e);
    	} catch (IOException e) {
    		throw new YggdrasilException("Exception while writing WARChive resource record!", e);
		}
    	return warcRecordIdUri;
    }

    /**
     * Append a metadata record to WARC file.
     * @param in payload input stream
     * @param len payload length
     * @param contentType payload content-type
     * @param blockDigest optional block digest
     * @return WarcRecordId of newly created record
     * @throws YggdrasilException if an exception occurs while writing record
     */
    public Uri writeMetadataRecord(InputStream in, long len, ContentType contentType, Uri refersTo, WarcDigest blockDigest) throws YggdrasilException {
    	ArgumentCheck.checkNotNull(in, "in");
    	ArgumentCheck.checkNotNull(len, "len");
    	ArgumentCheck.checkNotNull(contentType, "contentType");
    	Uri warcRecordIdUri = null;
    	try {
        	warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
        	WarcRecord record = WarcRecord.createRecord(writer);
        	WarcHeader header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_METADATA;
            header.warcDate = new Date();
            //header.warcIpAddress = "1.2.3.4";
            header.warcWarcinfoIdUri = warcinfoRecordId;
            header.warcRecordIdUri = warcRecordIdUri;
            //header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, responseOrRevisitRecordId, null);
            //header.warcTargetUriStr = uri;
            header.warcRefersToUri = refersTo;
            header.warcBlockDigest = blockDigest;
            header.contentType = contentType;
            header.contentLength = len;
            writer.writeHeader(record);
            writer.streamPayload(in);
            writer.closeRecord();
    	} catch (URISyntaxException e) {
    		throw new YggdrasilException("Exception while writing WARChive metadata record!", e);
    	} catch (IOException e) {
    		throw new YggdrasilException("Exception while writing WARChive metadata record!", e);
		}
    	return warcRecordIdUri;
    }

    /**
     * Close writer, output stream and random access file.
     * @throws YggdrasilException if an exception occurs while closing associated resources
     */
    public void close() throws YggdrasilException {
    	try {
        	if (writer != null) {
            	writer.close();
            	writer = null;
        	}
        	if (writer_rafout != null) {
        		writer_rafout.close();
            	writer_rafout = null;
        	}
        	if (writer_raf != null) {
            	writer_raf.close();
            	writer_raf = null;
        	}
    	} catch (IOException e) {
    		throw new YggdrasilException("Exception closing WARChive!", e);
    	}
    }

}