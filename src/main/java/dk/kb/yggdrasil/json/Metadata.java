package dk.kb.yggdrasil.json;

import java.io.Serializable;

import com.antiaction.common.json.annotation.JSONNullable;

/**
 * JSON metadata object representation.
 */
public class Metadata implements Serializable {

    /** Descriptive metadata. */
    @JSONNullable
    public String descMetadata;

    /** Provenance metadata. */
    @JSONNullable
    public String provenanceMetadata;

    /** Preservation metadata. */
    @JSONNullable
    public String preservationMetadata;

    /** technical metadata. */
    @JSONNullable
    public String techMetadata;

}