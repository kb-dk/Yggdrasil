/**
 * Some rights...
 */
package dk.kb.metadata.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The manager for the identifiers.
 */
public final class IdentifierManager {
	/** Private constructor for this utility class.*/
	private IdentifierManager() {}

	/** The mapping between the different file ids and their event identifiers.*/
    private static Map<String, String> eventIdentifierMap = new HashMap<String, String>();

    /**
     * Retrieves the event identifier for the given fileId.
     * If no event identifier exists for the given fileId, then a new random UUID is created for it.
     * @param fileId The fileId for the event identifier.
     * @return The event identifier corresponding to the fileId.
     */
    public static String getEventIdentifier(String fileId) {
    	String uuid = eventIdentifierMap.get(fileId);
        if(uuid == null) {
        	uuid = UUID.randomUUID().toString();
            eventIdentifierMap.put(fileId, uuid);
        }
        return uuid;
    }
    
    /**
     * Cleanup data after use (should be called after each transformation).
     */
    public static void clean() {
        eventIdentifierMap.clear();
    }
}
