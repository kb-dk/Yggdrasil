package dk.kb.yggdrasil.json;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.common.json.annotation.JSONNullable;

import dk.kb.yggdrasil.json.Metadata;

/**
 * JSON preservation request object representation.
 */
public class PreservationRequest implements Serializable {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(PreservationRequest.class.getName());
    
    /** Valhal element UUID. */
    public String UUID;

    /** Preservation profile. */
    public String Preservation_profile;

    /** Preservation state update URI. */
    public String Update_URI;

    /** Optional content UUID. */
    @JSONNullable
    public String File_UUID;

    /** Optional content URI. */
    @JSONNullable
    public String Content_URI;

    /** Metadata data. */
    public String metadata;

    public boolean isMessageValid() {
        StringBuffer missingContent = new StringBuffer();
        if (UUID == null || UUID.isEmpty()) {
            missingContent.append("Mandatory field 'UUID' is undefined");
        }
        if (Preservation_profile == null || Preservation_profile.isEmpty()) {
            missingContent.append("Mandatory field 'Preservation_profile' is undefined");
        }
        if (Update_URI == null || Update_URI.isEmpty()) {
            missingContent.append("Mandatory field 'Update_URI' is undefined");
        }
        if (missingContent.length() > 0) {
            logger.warn(missingContent.toString());
            return false;
        }
        return true;
    }
    
 }
