package dk.kb.yggdrasil.xslt.extension;

import dk.kb.yggdrasil.xslt.XslTransformer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yggdrasil Matrjosjka agent XSLT functions.
 */
public class Agent {
    /** Current METS profile URL. */
    public static final String PROFILE_URL = "http://id.kb.dk/standards/mets/profiles/version_1/kbMetsProfile.xml";

    /** Ingest agent URL. */
    public static final String INGEST_AGENT_URL = "http://id.kb.dk/authorities/agents/kbDkYggdrasilIngest.xml";

    /** The current version of the agent. */
    public static final String VERSION = Agent.class.getPackage().getImplementationVersion();

    /** Organization ID. */
    public static final String ORGANIZATION_ID = "kbDk";

    /** Organization name. */
    public static final String ORGANIZATION_NAME = "Det Kongelige Bibliotek, Nationalbibliotek og Københavns Universitetsbibliotek";

    /** Size of the major + minor substring. */
    static final int SIZE_OF_MAJOR_MINOR_SUBSTRING = 3;
    
    /** API ID. */
    public static final String API_ID = "kbDkYggdrasil" + StringUtils.left(VERSION, SIZE_OF_MAJOR_MINOR_SUBSTRING);

    /** API name. */
    public static final String API_NAME = "KB Yggdrasil v. " + VERSION;

    /** API note. */
    public static final String API_NOTE = XslTransformer.class.getName() + " (" + VERSION + ")";

    /** Department ID. */
    public static final String DEPARTMENT_ID = "kbDkNsa";

    /** Department name. */
    public static final String DEPARTMENT_NAME = "NSA: Nationalsamlingsafdelingen";

    /** PREMIS message digest originator. */
    public static final String MESSAGE_DIGEST_ORIGINATOR = "Det Kongelige Bibliotek, Nationalbibliotek og Københavns Universitetsbibliotek";

    /** MODS access condition. */
    public static final String MODS_ACCESS_CONDITION = "Det Kongelige Bibliotek, Nationalbibliotek og Københavns Universitetsbibliotek";

    /**
     * Private constructor to prevent instantiation of extension class.
     */
    private Agent() {
    }

    /**
     * @return The current version METS profile.
     */
    public static String getProfileURL() {
        return PROFILE_URL;
    }

    /**
     * @return id of the organization
     */
    public static String getOrganizationID() {
        return ORGANIZATION_ID;
    }

    /**
     * @return name of the organization
     */
    public static String getOrganizationName() {
        return ORGANIZATION_NAME;
    }

    /**
     * @return The name of the API.
     */
    public static String getAPIID() {
        return API_ID;
    }

    /**
     * @return The name of the API.
     */
    public static String getAPIName() {
        return API_NAME;
    }

    /**
     * @return The note of the API.
     */
    public static String getAPINote() {
        return API_NOTE;
    }

    /**
     * @return id of the department
     */
    public static String getDepartmentID() {
        return DEPARTMENT_ID;
    }

    /**
     * @return name of the department
     */
    public static String getDepartmentName() {
        return DEPARTMENT_NAME;
    }

    /**
     * @return PREMIS message digest originator
     */
    public static String getMessageDigestOriginator() {
        return MESSAGE_DIGEST_ORIGINATOR;
    }

    /**
     * @return MODS access condition
     */
    public static String getModsAccessCondition() {
        return MODS_ACCESS_CONDITION;
    }

    /**
     * @return URL for the ingest agent.
     */
    public static String getIngestAgentURL() {
        return INGEST_AGENT_URL;
    }
}
