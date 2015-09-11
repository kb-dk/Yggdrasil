package dk.kb.yggdrasil.json.preservation;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON preservation object representation.
 */
public class Preservation {

    /** Preservation state. */
    public String preservation_state;

    /** Preservation details. */
    public String preservation_details;

    /** WARC ID, optional. */
    @JSONNullable
    public String warc_id;

    /** The offset of the warc record of the metadata record. 
     * Must be in the format: start#end. */
    @JSONNullable
    public String warc_record_offset;

    /** WARC ID for the file, optional. */
    @JSONNullable
    public String file_warc_id;
    
    /** The offset of the warc record for the file record. 
     * Must be in the format: start#end. */
    @JSONNullable
    public String file_warc_record_offset;
}
