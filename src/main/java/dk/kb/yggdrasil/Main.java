package dk.kb.yggdrasil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Config;
import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.config.RunningMode;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.utils.RunState;

/**
 * Main class of the Yggdrasil Preservation service.
 * The running mode is defined by the java property {@link RunningMode#RUNNINGMODE_PROPERTY}.
 *
 * The configuration directory is defined by the property {@link #CONFIGURATION_DIRECTORY_PROPERTY}
 * The default value is ${user.home}/Yggdrasil/config
 *
 */
public class Main {

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
     * @param sd The StateDatabase.
     * @param bitrepository The bitrepository interface.
     */
    protected Main(StateDatabase sd, Bitrepository bitrepository) {
        this.sd = sd;
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

        Config config = new Config();

        Bitrepository bitrepository = new Bitrepository(config.getBitmagConfigFile());

        // Initiate call of StateDatabase
        StateDatabase sd = new StateDatabase(config.getYggdrasilConfig().getDatabaseDir());
        HttpCommunication httpCommunication = new HttpCommunication(config.getYggdrasilConfig().getTemporaryDir());

        RunState runnableRunState = new RunState();
        Thread runstate = new Thread(runnableRunState);
        runstate.start();

        Main main = new Main(sd, bitrepository);
        if (!isUnittestmode) {
            main.runWorkflow(config, httpCommunication);
        } else {
            logger.info("isUnittestmode = " + isUnittestmode);
        }
        logger.info("Shutting down the Yggdrasil Main program");
        runstate.interrupt();
        main.cleanup();
    }

    /**
     * Cleanup. Closes Bitrepository and MQ. 
     */
    protected void cleanup() {
        bitrepository.shutdown();
        try {
            if (!isUnittestmode) {this.mq.close();}
        } catch (IOException e) {
            logger.debug("Ignoring exception while closing down RabbitMQ", e);
        }
        sd.cleanup();
    }

    /**
     * Initialize message queue (RabbitMQ).
     * @throws YggdrasilException When unable to connect to message queue.
     */
    protected void initializeRabbitMQ(final RabbitMqSettings rabbitMqSettings) throws YggdrasilException {
        logger.info("Initialising RabbitMQ");
        this.mq = null;
        try {
            this.mq = new MQ(rabbitMqSettings);
            this.mq.purgeQueue(rabbitMqSettings.getShutdownDestination());
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
    protected void runWorkflow(final Config config, final HttpCommunication httpCommunication) 
            throws YggdrasilException {
        logger.info("Starting main workflow of Yggdrasil program");
        this.initializeRabbitMQ(config.getMqSettings());
        final Workflow wf = new Workflow(this.mq, sd, bitrepository, config.getYggdrasilConfig(), config.getModels(), 
                httpCommunication, new RemotePreservationStateUpdater(mq));
        logger.info("Ready to run workflow");
        // Consider refactoring this at a time where the used rabbitmq.client.ConnectionFactory supports 
        // the setAutomaticRecoveryEnabled and setNetworkRecoveryInterval methods.
        try {
            wf.run();
        } catch (RabbitException e) {
            String errMsg = "runWorkflow exception "; 
            logger.error(errMsg, e);
            try {
                TimeUnit.MINUTES.sleep(config.getMqSettings().getPollingIntervalInMinutes());
            } catch (InterruptedException e1) {
                errMsg = "Slowing down workfow exception "; 
                throw new YggdrasilException(errMsg, e1);
            }
            runWorkflow(config, httpCommunication);   
        }
    }
}
