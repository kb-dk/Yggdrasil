package dk.kb.yggdrasil.xslt.extension;

/**
 * Matrjosjka agent XSLT functions.
 */
public class Agent {

    /** Current METS profile URL. */
    public static final String PROFILE_URL = "http://id.kb.dk/standards/mets/profiles/version_1/kbMetsProfile.xml";

    /** The current version of the agent. */
    public static final String VERSION = "1.0.0-SNAPSHOT";

    /** Agent ID.*/
    public static final String AGENT_ID = "kbDkYggdrasil1.0";

    /** Agent API name. */
    public static final String API_NAME = "KB Yggdrasil v. " + VERSION;

    /** Agent API note. */
    public static final String API_NOTE = "dk.kb.yggdrasil.MetsGenerator (??)";

    /** Name of the organization. */
    public static final String ORGANIZATION = "Det Kongelige Bibliotek, Nationalbibliotek og Københavns Universitetsbibliotek";

    /** Name of the department. */
    public static final String DEPARTMENT = "NSA: National Samlings Afdelingen";

    /** MODS access condition. */
    public static final String MODSACCESSCONDITION = "Det Kongelige Bibliotek";

    /**
     * @return The current version METS profile.
     */
    public static String getProfileURL() {
        return PROFILE_URL;
    }

    /**
     * @return The name of the agent for the API.
     */
    public static String getID() {
        return AGENT_ID;
    }

    /**
     * @return The name of the entire API.
     */
    public static String getAPIName() {
        return API_NAME;
    }

    /**
     * @return The note of the entire API.
     */
    public static String getAPINote() {
        return API_NOTE;
    }

    /**
     * @return name of the organization
     */
    public static String getOrganization() {
        return ORGANIZATION;
    }

    /**
     * @return name of the department
     */
    public static String getDepartment() {
        return DEPARTMENT;
    }

    /**
     * @return mods access condition
     */
    public static String getModsAccessCondition() {
        return MODSACCESSCONDITION;
    }

}
