package dk.kb.metadata.utils;

/**
 * Handles the extraction of GUIDs from the potentially old and invalid format.
 */
public final class GuidExtrationUtils {
	/** Private constructor for this Utility class.*/
	private GuidExtrationUtils() {}

    /**
     * Method for extracting the part of the KB-GUID which is valid as a 'xs:ID' standardized guid.
     * 
     * @param guid The GUID for the system.
     * @return The extracted GUID.
     */
    public static String extractGuid(String guid) {
    	if(guid == null || guid.isEmpty()) {
    		RuntimeException e = new IllegalArgumentException("A GUID must be defined.");
    		ExceptionUtils.insertException(e);
    		throw e;
    	}
    	
        String[] guidParts = guid.split("[/]");
        if(guidParts.length > 1) {
            return guidParts[1];
        }

        return guid;
    }
}
