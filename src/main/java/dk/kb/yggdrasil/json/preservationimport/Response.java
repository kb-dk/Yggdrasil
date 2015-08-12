package dk.kb.yggdrasil.json.preservationimport;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON warc object representation for the PreservationImportRequest.
 */
public class Response {
    /** 
     * The state telling about the progress for handling the response. E.g. failures or successes.
     */
    public String state;

    /** 
     * Details regarding the state, e.g. why it failed.
     * Optional, since the success states are self-explanatory.
     */
    @JSONNullable
    public String detail;

    /** 
     * The date for the response.   
     */
    public String date;
}
