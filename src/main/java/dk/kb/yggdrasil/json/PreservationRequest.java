package dk.kb.yggdrasil.json;

import com.antiaction.common.json.annotation.JSONNullable;

import dk.kb.yggdrasil.json.Metadata;

/**
 * JSON preservation request object representation.
 */
public class PreservationRequest {

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
    public Metadata metadata;

    }
