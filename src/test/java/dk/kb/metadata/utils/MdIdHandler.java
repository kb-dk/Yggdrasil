package dk.kb.metadata.utils;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Manages the different MD objects in the METS, both the DMD and AMD wrappers for encapsulating 
 * the other metadata formats.
 */
public final class MdIdHandler {
	/** Private constructor for this utility class.*/
	private MdIdHandler() {}

    /** Container for the ids for the MD objects for different documents with different ids.*/
    private static Map<String, List<String>> mdIds = new HashMap<String, List<String>>();

    /** 
     * Creates the id for a new MD object of a specific metadata document.
     * 
     * @param type The type of MD object.
     * @return The id for a new MD object.
     */
    public static String createNewMdId(String type) {
        List<String> idList;
        if(mdIds.containsKey(type)) {
            idList = mdIds.get(type);
        } else {
            idList = new ArrayList<String>();
        }
        String res = type + (idList.size() + 1);
        idList.add(res);

        mdIds.put(type, idList);

        return res;
    }

    /** 
     * Retrieves the list of ids for the MD objects for a given METS file.
     * 
     * @param type The type of MD object.
     * @return The list of ids for the MD objects of the given type within the METS file, 
     * or an empty list if none is found.
     */
    public static List<String> getMdIDs(String type) {
        if(mdIds.containsKey(type)) {
            return mdIds.get(type);
        }
        return new ArrayList<String>();
    }

    /**
     * Retrieves the list of ids for the Div attributes for the given types.
     * 
     * @param types The types of MDs to retrieve the ids for.
     * @return The requested list of ids in the format for the div attribute.
     */
    public static String getDivAttributeFor(String types) {
        StringBuffer res = new StringBuffer();

        for(String type : types.split(",")) {
            for(String id : getMdIDs(type)) {
                if(res.length() == 0) {
                    res.append(id);
                } else {
                    res.append(" " + id);
                }
            }
        }

        return res.toString();
    }
    
    /**
     * Cleanup data after use (should be called after each transformation).
     */
    public static void clean() {
        mdIds.clear();
    }
}
