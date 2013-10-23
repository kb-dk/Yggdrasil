package dk.kb.yggdrasil;

import java.io.File;

/**
 * Main class of the Yggdrasil Preservation service. 
 * The running mode is defined by the java property {@link RunningMode#RUNNINGMODE_PROPERTY}.
 * 
 * The configuration directory is defined by the property {@link #CONFIGURATION_DIRECTORY_PROPERTY}
 * The default value is ${user.home}/Yggdrasil/config
 * 
 */
public class Main {

    /** 
     * The list of configuration files that should be present in the configuration directory.
     */
    public static final String[] REQUIRED_SETTINGS_FILES = new String[] {"rabbitmq.yml"};
    /** Java Property to define Yggdrasil configuration directory. */
    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "YGGDRASIL_CONF_DIR";
    
    /** Java Property to define user.home. */
    private static final String USER_HOME_PROPERTY = "user.home";

    /**
     * Main program of the Yggdrasil preservation service.
     * @param args No args is read here. Properties are used to locate confdir and the running mode.
     * @throws Exception When unable to find a configuration directory or locate required settings files.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the Yggdrasil Main program");

        System.out.println("Initialising settings ");
        RunningMode m = RunningMode.getMode();
        System.out.println("Running in mode '" + m.toString().toLowerCase() + "'");
        File configdir = null;
        String configDirFromProperties = System.getProperty(CONFIGURATION_DIRECTORY_PROPERTY); 

        if (configDirFromProperties != null) {
            configdir = new File(configDirFromProperties);
        } else {
            File userHomeDir = new File(System.getProperty(USER_HOME_PROPERTY));
            configdir = new File(userHomeDir, "Yggdrasil/config");
        }
        if (!configdir.exists()) {
            throw new Exception("Fatal error: The chosen configuration directory '" 
                    + configdir.getAbsolutePath() + "' does not exist. "); 
        }
        System.out.println("Looking for configuration files in dir: " 
                + configdir.getAbsolutePath());
        for (String requiredSettingsFilename: REQUIRED_SETTINGS_FILES) {
            File reqFile = new File(configdir, requiredSettingsFilename);
            if (!reqFile.exists()) {
                throw new Exception("Fatal error. Required configuration file '" 
                        + reqFile.getAbsolutePath() + "' does not exist. "); 
            }
        }

        System.out.println("Hello world!");
        System.out.println("Shutting down the Yggdrasil Main program");
    }

}
