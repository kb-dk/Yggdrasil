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
     * The list of settings files that should be present in the configuration directory.
     */
    public static final String[] SETTINGS_FILES = new String[] {"rabbitmq.yml", "warc.yml"};
    
    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "dk.kb.yggdrasil.confdir";
    public static final String USER_HOME_PROPERTY = "user.home";
    
    public static void main(String[] args) throws Exception {
	    System.out.println("Starting the Yggdrasil Main program");
	    
	    System.out.println("Initialising settings ");
	    RunningMode m = RunningMode.getMode();
	    System.out.println("Running in mode '" + m.toString().toLowerCase() + "'");
	    File configdir = null;
	    String configDirFromProperties = System.getProperty(CONFIGURATION_DIRECTORY_PROPERTY); 
	    
	    if ( configDirFromProperties != null) {
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
	    
	    System.out.println("Hello world!");
	    System.out.println("Shutting down the Yggdrasil Main program");
	}
}
