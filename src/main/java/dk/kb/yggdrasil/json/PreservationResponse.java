package dk.kb.yggdrasil.json;

import dk.kb.yggdrasil.json.Preservation;

/**
 * JSON preservation response object representation.
 * The single containing object reflects the JSON structure.
 */
public class PreservationResponse {

    /** id corresponds to the Fedora Object id of the preserved object in the management repository (i.e.: Valhal)**/
    public String id;
    /** model corresponds to the name of the model defined in the management repo (Valhal) for the preserved object **/
    public String model;
    /** Preservation data. */
    public Preservation preservation;
}
