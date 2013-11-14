package dk.kb.yggdrasil.json;

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

}



