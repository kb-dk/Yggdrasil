package dk.kb.metadata;

/**
 * The constants for the Metadata Matrjosjka.
 */
public class Constants {
    /** The current version of the Metadata Matrjosjka.*/
    public static final String VERSION = "1.3.1";
    /** The name of the Agent.*/
    public static final String AGENT_ID = "kbDkMdGen";
    
    /**
     * @return The name of the entire API.
     */
    public static String getAPI() {
        return "KB METADATA MATRJOSJKA v. " + VERSION;
    }
    
    /**
     * @return The name of the agent for the API.
     */
    public static String getAPIAgent() {
        return AGENT_ID;
    }
    
    /**
     * @return The current version METS profile.
     */
    public static String getProfileURL() {
        return "http://id.kb.dk/standards/mets/profiles/version_1/kbMetsProfile.xml";
    }
}
