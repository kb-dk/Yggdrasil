package dk.kb.yggdrasil.json.preservationimport;

import java.io.Serializable;

/**
 * JSON preservation import response object representation.
 */
public class PreservationImportResponse implements Serializable {
    /** The type of data to import. */
    public String type;

    /** UUID of the element to import. */
    public String uuid;

    /** The response object containing information about the progress of handling the request. */
    public Response response;
}
