package dk.kb.yggdrasil.json.preservationimport;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON preservation import request object representation.
 */
public class PreservationImportRequest implements Serializable {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(PreservationImportRequest.class.getName());

    /** 
     * The type of data to import.
     * Refers to the type of object, either the 'METADATA' or the 'FILE'.
     * TODO Must currently be 'FILE', but fix when we can import metadata. 
     * */
    public String type;

    /** UUID of the element to import. */
    public String uuid;

    /** The preservation profile, where the data has been preserved. */
    public String preservation_profile;

    /** The address where the data should be delivered. */
    public String url;
    
    /** The WARC object containing information about the warc file, where the data should be extracted.*/
    public Warc warc;
    
    /** 
     * The security object containing information how to validate and authorize the data and delivery of data.
     * Optional, since the security might not be used.
     */
    @JSONNullable
    public Security security;
    
    /**
     * Validates the message.
     * @return Whether the mandatory fields are set.
     */
    public boolean isMessageValid() {
        StringBuffer missingContent = new StringBuffer();
        if (type == null || type.isEmpty()) {
            missingContent.append("Mandatory field 'type' is undefined");
        }
        if (uuid == null || uuid.isEmpty()) {
            missingContent.append("Mandatory field 'uuid' is undefined");
        }
        if (preservation_profile == null || preservation_profile.isEmpty()) {
            missingContent.append("Mandatory field 'preservation_profile' is undefined");
        }
        if (url == null || url.isEmpty()) {
            missingContent.append("Mandatory field 'url' is undefined");
        }
        if (warc == null) {
            missingContent.append("Mandatory element 'warc' is undefined");
        }
        if (warc != null && (warc.warc_file_id == null || warc.warc_file_id.isEmpty())) {
            missingContent.append("Mandatory field 'warc_file_id' in the 'warc' element is undefined");
        }
        if (warc != null && (warc.warc_record_id == null || warc.warc_record_id.isEmpty())) {
            missingContent.append("Mandatory field 'warc_record_id' in the 'warc' element is undefined");
        }

        if (missingContent.length() > 0) {
            logger.warn(missingContent.toString());
            return false;
        }
        return true;
    }    
}
