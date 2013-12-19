package dk.kb.metadata.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of the file ids for the respective GUIDs.
 */
public final class FileIdHandler {
	/** Private constructor for this Utility class.*/
	private FileIdHandler() {}
    
    /** Maps between a GUID and the respective file id. */
    private static Map<String, String> fileIds = new HashMap<String, String>();
    
    /**
     * Returns the file id for the respective GUID. If no file id exists for such GUID, then it is created.
     * @param GUID The guid of the file.
     * @return The file id corresponding to the GUID.
     */
    public static String getFileID(String GUID) {
    	String fileId = fileIds.get(GUID);
        if (fileId == null) {
            fileId = "fileId" + (fileIds.size() + 1);
            fileIds.put(GUID, fileId);
        }
        return fileIds.get(GUID);
    }
    
    /**
     * Cleanup data after use (should be called after each transformation).
     */
    public static void clean() {
        fileIds.clear();
    }
}
