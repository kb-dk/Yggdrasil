package dk.kb.yggdrasil.json;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON update object representation.
 */
public class Update {

    /** UUID of the preservation update element. */
    @JSONNullable
    public String uuid;

    /** UUID of the preservation update file. */
    @JSONNullable
    public String file_uuid;

    /** Date. */
    public String date;

    /** WARC ID, optional. */
    @JSONNullable
    public String warc_id;
        
    /** WARC ID for the file, optional. */
    @JSONNullable
    public String file_warc_id; 
}
