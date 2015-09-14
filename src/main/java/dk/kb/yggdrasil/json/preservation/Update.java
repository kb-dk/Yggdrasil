package dk.kb.yggdrasil.json.preservation;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON update object representation.
 */
public class Update {

    /** 
     * UUID of the preservation update for the metadata element. 
     * If it is a preservation update for the file of a ContentFile, then this uuid should be null.
     */
    @JSONNullable
    public String uuid;

    /** 
     * UUID of the preservation update of the file of a ContentFile. 
     * Should be null, unless it is a preservation update for the file of a ContentFile.
     */
    @JSONNullable
    public String file_uuid;

    /** Date. */
    public String date;

    /** 
     * WARC ID for the metadata object.
     * Should only be null, if this is a preservation update for the file of a ContentFile. 
     */
    @JSONNullable
    public String warc_id;

    /** The offset of the warc record of the metadata record. 
     * Must be in the format: start#end. */
    @JSONNullable
    public String warc_offset;

    /** 
     * WARC ID for the file of the ContentFile. 
     * Should only be present, if it is a preservation update for the file of a ContentFile.
     */
    @JSONNullable
    public String file_warc_id; 

    /** The offset of the warc record for the file record. 
     * Must be in the format: start#end. */
    @JSONNullable
    public String file_warc_offset;
}
