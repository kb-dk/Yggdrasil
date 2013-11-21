package dk.kb.yggdrasil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Main class of the Yggdrasil Preservation service.
 * The running mode is defined by the java property {@link RunningMode#RUNNINGMODE_PROPERTY}.
 *
 * The configuration directory is defined by the property {@link #CONFIGURATION_DIRECTORY_PROPERTY}
 * The default value is ${user.home}/Yggdrasil/config
 *
 */
public class Main {
    public static String YGGDRASIL_CONF_FILENAME = "yggdrasil.yml";
    public static String RABBITMQ_CONF_FILENAME = "rabbitmq.yml";
    public static String BITMAG_CONF_FILENAME = "bitmag.yml";
    
    /**
     * The list of configuration files that should be present in the configuration directory.
     */
    public static final String[] REQUIRED_SETTINGS_FILES = new String[] {
        RABBITMQ_CONF_FILENAME, BITMAG_CONF_FILENAME};
    /** Java Property to define Yggdrasil configuration directory. */
    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "YGGDRASIL_CONF_DIR";

    /** Java Property to define user.home. */
    private static final String USER_HOME_PROPERTY = "user.home";

    private static boolean isUnitestmode = false;
    
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private StateDatabase sd;
    private MQ mq;
    private Bitrepository bitrepository;

    public Main(StateDatabase sd, MQ mq, Bitrepository bitrepository) {
        this.sd = sd;
        this.mq = mq;
        this.bitrepository = bitrepository;
    }

    /**
     * Main program of the Yggdrasil preservation service.
     * @param args No args is read here. Properties are used to locate confdir and the running mode.
     * @throws YggdrasilException When unable to find a configuration directory or locate required settings files.
     */
    public static void main(String[] args) throws YggdrasilException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        logger.info("Starting the Yggdrasil Main program");

        logger.info("Initialising settings using runningmode '" + RunningMode.getMode() + "'");
        if (args.length == 1 && args[0].equals("test")) {
            isUnitestmode = true;
        }
        
        File configdir = null;
        String configDirStr = System.getProperty(CONFIGURATION_DIRECTORY_PROPERTY);
        if (configDirStr != null) {
            configdir = new File(configDirStr);
        } else {
            configDirStr = System.getenv(CONFIGURATION_DIRECTORY_PROPERTY);
            if (configDirStr != null) {
                configdir = new File(configDirStr);
            } else {
                File userHomeDir = new File(System.getProperty(USER_HOME_PROPERTY));
                configdir = new File(userHomeDir, "Yggdrasil/config");
            }
        }
        if (!configdir.exists()) {
            throw new YggdrasilException("Fatal error: The chosen configuration directory '"
                    + configdir.getAbsolutePath() + "' does not exist. ");
        }
        logger.info("Looking for configuration files in dir: " + configdir.getAbsolutePath());

        for (String requiredSettingsFilename: REQUIRED_SETTINGS_FILES) {
            File reqFile = new File(configdir, requiredSettingsFilename);
            if (!reqFile.exists()) {
                throw new YggdrasilException("Fatal error. Required configuration file '"
                        + reqFile.getAbsolutePath() + "' does not exist. ");
            }
        }

        MQ mq = null;
        Bitrepository bitrepository = null;
        StateDatabase sd = null;
        Config generalConfig = null;
        try {
            File rabbitmqConfigFile = new File(configdir, RABBITMQ_CONF_FILENAME);
            RabbitMqSettings rabbitMqSettings = new RabbitMqSettings(rabbitmqConfigFile);
            mq = new MQ(rabbitMqSettings);
            mq.configureDefaultChannel();
            
            File bitmagConfigFile = new File(configdir, BITMAG_CONF_FILENAME);
            bitrepository = new Bitrepository(bitmagConfigFile);
            
            File yggrasilConfigFile = new File(configdir, YGGDRASIL_CONF_FILENAME);
            generalConfig = new Config(yggrasilConfigFile);
            
        } catch (FileNotFoundException e) {
            String errMsg = "Configuration file(s) missing!"; 
            logger.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        
        // Initiate call of StateDatabase
        sd = new StateDatabase(generalConfig.getDatabaseDir());
        Main main = new Main(sd, mq, bitrepository);
        if (!isUnitestmode) {
            Workflow wf = new Workflow(mq, sd, bitrepository);
            wf.run();
        }   
        logger.info("Shutting down the Yggdrasil Main program");
        main.cleanup();
  }

    private void cleanup() {
        bitrepository.shutdown();
        try {
            mq.close();
        } catch (IOException e) {
            logger.debug("Ignoring exception while closing down RabbitMQ", e);
        }
        sd.cleanup();
    }

}
