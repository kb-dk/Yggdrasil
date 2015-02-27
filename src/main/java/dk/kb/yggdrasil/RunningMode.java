package dk.kb.yggdrasil;

import java.util.logging.Logger;

/**
 * The three running modes available for Yggdrasil: development, test, production.
 * Defined by Java property dk.kb.yggdrasil.runningmode
 * The default is development mode.
 */
public enum RunningMode {

    /**
     * The development mode (for the development environment and continuous integration).
     */
    DEVELOPMENT,
    /**
     * The test mode (for the releasetest and staging environment).
     */
    TEST,
    /**
     * The production mode (used by the production environment).
     */
    PRODUCTION;

    /**
     * Override the existing toString method to avoid toLowercase calls
     * @return The lowercase name of the running mode.
     */
    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    /** The java property for the runningmode. */
    public static final String RUNNINGMODE_PROPERTY = "dk.kb.yggdrasil.runningmode";

    /** Logging mechanism. */
    private static final Logger logger = Logger.getLogger(RunningMode.class.getName());

    /**
     * Method to parse string as a known runningmode.
     * @param text The text to parse
     * @return a known RunningMode or null if not recognized as a known RunningMode
     */
    private static RunningMode fromString(String text) {
        if (text != null && !text.trim().isEmpty()) {
            for (RunningMode b : RunningMode.values()) {
                if (text.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * Return the RunningMode to use by Yggdrasil.
     * @return the RunningMode defined by the RUNNINGMODE_PROPERTY property or
     * the Development if property is undefined or the value unrecognizable as running mode.
     */
    public static RunningMode getMode() {
        String propertyValue = System.getProperty(RUNNINGMODE_PROPERTY);
        if (propertyValue != null) {
            RunningMode mode = fromString(propertyValue);
            if (mode != null) {
                return mode;
            } else {
                logger.warning("System property '" + RUNNINGMODE_PROPERTY
                        + "' not set properly. Unable to recognize '"
                        + propertyValue + "' as a runningmode. "
                        + "Choosing default mode (DEVELOPMENT)");
                return DEVELOPMENT;
            }
        } else {
            logger.info("System property '" + RUNNINGMODE_PROPERTY
                    + "' not set. Choosing default mode (DEVELOPMENT)");
            return DEVELOPMENT;
        }
    }
}
