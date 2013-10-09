package dk.kb.yggdrasil;

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

    /** The java property for the runningmode. */
    public static final String RUNNINGMODE_PROPERTY = "dk.kb.yggdrasil.runningmode"; 
     
    private static RunningMode fromString(String text) {
        if (text != null && !text.isEmpty()) {
          for (RunningMode b : RunningMode.values()) {
            if (text.equalsIgnoreCase(b.toString())) {
              return b;
            }
          }
        }
        return null;
      }
    
    public static RunningMode getMode() {
        String propertyValue = System.getProperty(RUNNINGMODE_PROPERTY);
        if (propertyValue != null) {
            RunningMode mode = fromString(propertyValue);
            if (mode != null) {
                return mode;
            } else {
                System.out.println("System property '" + RUNNINGMODE_PROPERTY 
                        + "' not set properly. Unable to recognize '" 
                        + propertyValue + "' as a runningmode. " 
                        + "Choosing default mode (DEVELOPMENT)");
                return DEVELOPMENT;
            }
        } else {
            // return default value
            System.out.println("System property '" + RUNNINGMODE_PROPERTY 
                    + "' not set. Choosing default mode (DEVELOPMENT)");
            return DEVELOPMENT;
        }
    }
    
}


