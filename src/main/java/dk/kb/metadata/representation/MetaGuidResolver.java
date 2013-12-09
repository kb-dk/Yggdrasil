package dk.kb.metadata.representation;

import dk.kb.metadata.utils.OrderedMap;

/**
 * The resolver of metadata guids.
 */
public final class MetaGuidResolver {
	/** Private constructor for this Utility class.*/
	private MetaGuidResolver() {}

	/** The map between a file id and its guid.*/
    private static OrderedMap fileidGuid = null;
    
    /** Any exception, if the extraction fails.*/
    private static RuntimeException exception = null;
    
    /**
     * Sets the guids for the given 
     * @param guids
     */
    public static void setGuidMap(OrderedMap guids) {
    	fileidGuid = guids;
    }
    
    /**
     * Retrieves the GUID corresponding to a given file id.
     * @param fileId The id of the file.
     * @return The corresponding GUID for the file.
     */
    public static String getGuid(String fileId) {
    	validateFileid(fileId);
        return fileidGuid.getValue(fileId);
    }

    /**
     * Retrieves the order for given file id. Adds one to the index.
     * @param fileId The id of the file.
     * @return The corresponding GUID for the file.
     */
    public static String getOrder(String fileId) {
    	validateFileid(fileId);
    	Integer index = fileidGuid.getIndex(fileId) + 1;
    	return index.toString();
    }

    /**
     * Clears the map 
     */
    public static void clear() {
    	fileidGuid = null;
    	exception = null;
    }

    private static void validateFileid(String fileId) {
    	if(fileidGuid == null || !fileidGuid.hasKey(fileId)) {
        	exception = new IllegalStateException("No entry for the file '" + fileId + "'.");
        	throw exception;
    	}
    }
    
    public static boolean hasFailure() {
    	return (exception != null);
    }
    public static RuntimeException getException() {
    	return exception;
    }
}
