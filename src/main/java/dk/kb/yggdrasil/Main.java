package dk.kb.yggdrasil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.utils.RunState;
import dk.kb.yggdrasil.xslt.Models;

/**
 * Main class of the Yggdrasil Preservation service.
 * The running mode is defined by the java property {@link RunningMode#RUNNINGMODE_PROPERTY}.
 *
 * The configuration directory is defined by the property {@link #CONFIGURATION_DIRECTORY_PROPERTY}
 * The default value is ${user.home}/Yggdrasil/config
 *
 */
public class Main {
    /** The name of the Yggdrasil configuration file.*/
    static final String YGGDRASIL_CONF_FILENAME = "yggdrasil.yml";
    /** The name of the configuration for RabbitMQ.*/
    static final String RABBITMQ_CONF_FILENAME = "rabbitmq.yml";
    /** The name of the configuration for the BitRepository.*/
    static final String BITMAG_CONF_FILENAME = "bitmag.yml";
    /** The name of the configuration for the models.*/
    static final String MODELS_CONF_FILENAME = "models.yml";

    /**
     * The list of configuration files that should be present in the configuration directory.
     */
    static final String[] REQUIRED_SETTINGS_FILES = new String[] {
        RABBITMQ_CONF_FILENAME, BITMAG_CONF_FILENAME, MODELS_CONF_FILENAME};
    /** Java Property to define Yggdrasil configuration directory. */
    static final String CONFIGURATION_DIRECTORY_PROPERTY = "YGGDRASIL_CONF_DIR";

    /** Java Property to define user.home. */
    private static final String USER_HOME_PROPERTY = "user.home";
    /** Boolean for deciding whether to start main workflow (Normal mode) 
     * or just shutdown program after initialization (Unittest mode). */
    private static boolean isUnittestmode = false;

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Main.class.getName());
    /** The state database.*/
    private StateDatabase sd;
    /** The messagequeue.*/
    private MQ mq;
    /** The bitrepository interface.*/
    private Bitrepository bitrepository;

    /**
     * Constructor.
     * TODO: why is this public?
     * @param sd The StateDatabase.
     * @param mq The messagequeue
     * @param bitrepository The bitrepository interface.
     */
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
            isUnittestmode = true;
        }

        File configdir = getConfigDir();
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
        Models modelsConfig = null;

        try {
            File rabbitmqConfigFile = new File(configdir, RABBITMQ_CONF_FILENAME);
            RabbitMqSettings rabbitMqSettings = new RabbitMqSettings(rabbitmqConfigFile);

            File bitmagConfigFile = new File(configdir, BITMAG_CONF_FILENAME);
            bitrepository = new Bitrepository(bitmagConfigFile);

            File yggrasilConfigFile = new File(configdir, YGGDRASIL_CONF_FILENAME);
            generalConfig = new Config(yggrasilConfigFile);

            File modelsConfigFile = new File(configdir, MODELS_CONF_FILENAME);
            modelsConfig = new Models(modelsConfigFile);

            // Initiate call of StateDatabase
            sd = new StateDatabase(generalConfig.getDatabaseDir());

            RunState runnableRunState = new RunState();
            Thread runstate = new Thread(runnableRunState);
            runstate.start();

            Main main = new Main(sd, mq, bitrepository);
            if (!isUnittestmode) {
                main.runWorkflow(sd, bitrepository, generalConfig, modelsConfig, rabbitMqSettings);
            } else {
                logger.info("isUnittestmode = " + isUnittestmode);
            }
            logger.info("Shutting down the Yggdrasil Main program");
            runstate.interrupt();
            main.cleanup();

        } catch (FileNotFoundException e) {
            String errMsg = "Configuration file(s) missing!"; 
            logger.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    private void cleanup() {
        bitrepository.shutdown();
        try {
            if (!isUnittestmode) {this.mq.close();}
        } catch (IOException e) {
            logger.debug("Ignoring exception while closing down RabbitMQ", e);
        }
        sd.cleanup();
    }

    /** 
     * @return the configuration directory.
     */
    public static File getConfigDir() {
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

    /**
     * Initialize message queue (RabbitMQ).
     * @throws YggdrasilException When unable to connect to message queue.
     */
    private void initializeRabbitMQ(final RabbitMqSettings rabbitMqSettings) throws YggdrasilException {
        logger.info("Initialising RabbitMQ");
        this.mq = null;
        try {
            this.mq = new MQ(rabbitMqSettings);
        } catch (RabbitException e) {
            String errMsg = "initializeRabbitMQ exception "; 
            logger.error(errMsg, e);
            try {
                TimeUnit.MINUTES.sleep(rabbitMqSettings.getPollingIntervalInMinutes());
            } catch (InterruptedException e1) {
                errMsg = "Slowing down workfow exception "; 
                throw new YggdrasilException(errMsg, e1);
            }
            initializeRabbitMQ(rabbitMqSettings); 
        }
    }


    /**
     * Run Yggdrasil workflow with slow down if needed.
     * @throws YggdrasilException When unable to connect to message queue.
     * @throws FileNotFoundException Pass through from workflow run method.
     */
    private void runWorkflow(final StateDatabase sd, final Bitrepository bitrepository, final Config generalConfig, 
            final Models modelsConfig, final RabbitMqSettings rabbitMqSettings) throws FileNotFoundException, 
            YggdrasilException {
        logger.info("Starting main workflow of Yggdrasil program");
        this.initializeRabbitMQ(rabbitMqSettings);
        final Workflow wf = new Workflow(this.mq, sd, bitrepository, generalConfig, modelsConfig);
        logger.info("Ready to run workflow");
        // Consider refactoring this at a time where the used rabbitmq.client.ConnectionFactory supports 
        // the setAutomaticRecoveryEnabled and setNetworkRecoveryInterval methods.
        try {
            wf.run();
        } catch (RabbitException e) {
            String errMsg = "runWorkflow exception "; 
            logger.error(errMsg, e);
            try {
                TimeUnit.MINUTES.sleep(rabbitMqSettings.getPollingIntervalInMinutes());
            } catch (InterruptedException e1) {
                errMsg = "Slowing down workfow exception "; 
                throw new YggdrasilException(errMsg, e1);
            }
            runWorkflow(sd, bitrepository, generalConfig, modelsConfig, rabbitMqSettings);   
        }
    }
}
