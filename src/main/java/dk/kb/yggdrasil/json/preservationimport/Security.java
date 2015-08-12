package dk.kb.yggdrasil.json.preservationimport;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON security object representation for the PreservationImportRequest.
 */
public class Security {
    /** 
     * The expected checksum of the data to import.
     * Optional, since the checksum might not be known. 
     */
    @JSONNullable
    public String checksum;

    /** 
     * The token for authorization of sending the extracted data to Valhal.
     * Optional, since it is not required.
     */
    @JSONNullable
    public String token;

    /** 
     * Timeout for the token, thus the latest date for sending the extracted data to Valhal. 
     * Optional, since the token is optional.   
     */
    @JSONNullable
    public String token_timeout;
}
