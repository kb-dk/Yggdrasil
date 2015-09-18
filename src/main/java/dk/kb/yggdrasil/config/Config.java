package dk.kb.yggdrasil.config;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Configuration class to keep track of the different configuration files.
 */
public class Config {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Config.class.getName());

    /** Java Property to define Yggdrasil configuration directory. */
    public static final String CONFIGURATION_DIRECTORY_PROPERTY = "YGGDRASIL_CONF_DIR";
    /** The name of the Yggdrasil configuration file.*/
    public static final String YGGDRASIL_CONF_FILENAME = "yggdrasil.yml";
    /** The name of the configuration for RabbitMQ.*/
    public static final String RABBITMQ_CONF_FILENAME = "rabbitmq.yml";
    /** The name of the configuration for the BitRepository.*/
    public static final String BITMAG_CONF_FILENAME = "bitmag.yml";
    /** The name of the configuration for the models.*/
    public static final String MODELS_CONF_FILENAME = "models.yml";
    /** Java Property to define user.home. */
    public static final String USER_HOME_PROPERTY = "user.home";

    /**
     * The list of configuration files that should be present in the configuration directory.
     */
    static final String[] REQUIRED_SETTINGS_FILES = new String[] {
        RABBITMQ_CONF_FILENAME, BITMAG_CONF_FILENAME, MODELS_CONF_FILENAME};

    /** Models. */
    private final Models models;
    /** Settings for the RabbitMQ.*/
    private final RabbitMqSettings mqSettings;
    /** General Yggdrasil configuration.*/
    private final YggdrasilConfig yggdrasilConfig;
    /** The configuration file for the Bitrepository.*/
    private final File bitmagConfigFile;

    /**
     * Constructor. 
     * Initiates the configurations.
     * @throws YggdrasilException If a configuration is missing or not possible to parse.
     */
    public Config() throws YggdrasilException {
        try {
            File configDir = getConfigDir();
            logger.info("Looking for configuration files in dir: " + configDir.getAbsolutePath());
            validateConfigDir(configDir);

            File rabbitmqConfigFile = new File(configDir, RABBITMQ_CONF_FILENAME);
            mqSettings = new RabbitMqSettings(rabbitmqConfigFile);

            bitmagConfigFile = new File(configDir, BITMAG_CONF_FILENAME);

            File yggrasilConfigFile = new File(configDir, YGGDRASIL_CONF_FILENAME);
            yggdrasilConfig = new YggdrasilConfig(yggrasilConfigFile);

            File modelsConfigFile = new File(configDir, MODELS_CONF_FILENAME);
            models = new Models(modelsConfigFile);
        } catch (IOException e) {
            throw new YggdrasilException("Failed loading configurations.", e);
        }
    }
        
    /**
     * Validates that the configuration directory exists and contains the expected files.
     * @param configdir The directory containing the configurations.
     * @throws YggdrasilException If the directory or any of the configurations files does not exist.
     */
    protected static void validateConfigDir(File configdir) throws YggdrasilException {
        if (!configdir.exists()) {
            throw new YggdrasilException("Fatal error: The chosen configuration directory '"
                    + configdir.getAbsolutePath() + "' does not exist. ");
        }
        for (String requiredSettingsFilename : REQUIRED_SETTINGS_FILES) {
            File reqFile = new File(configdir, requiredSettingsFilename);
            if (!reqFile.exists()) {
                throw new YggdrasilException("Fatal error. Required configuration file '"
                        + reqFile.getAbsolutePath() + "' does not exist. ");
            }
        }
    }
    
    public Models getModels() {
        return models;
    }
    
    public RabbitMqSettings getMqSettings() {
        return mqSettings;
    }
    
    public YggdrasilConfig getYggdrasilConfig() {
        return yggdrasilConfig;
    }
    
    public File getBitmagConfigFile() {
        return bitmagConfigFile;
    }
    

    /** 
     * @return the configuration directory.
     * @throws If the configuration directory does not exist.
     */
    public static File getConfigDir() throws YggdrasilException {
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
        return configdir;
    }
}
