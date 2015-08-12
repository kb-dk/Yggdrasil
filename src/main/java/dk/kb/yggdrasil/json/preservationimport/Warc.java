package dk.kb.yggdrasil.json.preservationimport;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON warc object representation for the PreservationImportRequest.
 */
public class Warc {
    /** 
     * The id of the warc file containing the data to retrieve.
     */
    public String warc_file_id;

    /** 
     * The id of the warc record within the warc file containing the data to retrieve.
     */
    public String warc_record_id;

    /** 
     * The warc record offset in the warc file (thus how many bytes into the warc file the warc record begins).   
     */
    @JSONNullable
    public String warc_offset;
        
    /** 
     * The size of the warc record. 
     * Can be used with the warc offset to determine start and end of the warc record within the warc file, 
     * which can be used to retrieve the exact warc record from the bitrepository, 
     * instead of the whole warc file and afterwards go through it to find the warc record.
     */
    @JSONNullable
    public String warc_record_size; 
}
