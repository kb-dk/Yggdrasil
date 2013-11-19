package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PushbackInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.Preservation;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.json.PreservationResponse;
import dk.kb.yggdrasil.db.PreservationRequestState;
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

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Main.class.getName());

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
            mq = MQ.getInstance(rabbitMqSettings);
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
        String currentUUID = null;
        
        try {
            // FIXME read queue from rabbitMqSettings
            byte[] requestBytes = mq.receiveMessageFromQueue("preservation-dev-queue");
            PreservationRequest request = JSONMessaging.getPreservationRequest(
                    new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4));
            logger.info("Preservation request received.");
            PreservationRequestState prs = null;
            File tmpFile = null;
            
            /* Validate message content. */
            if (!request.isMessageValid()) {
                logger.error("Skipping invalid message");
                //prs = new PreservationRequestState(request, 
                //        State.PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE);
            } else {
                prs = new PreservationRequestState(request, 
                        State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
                currentUUID = request.UUID;
                
                PreservationResponse response = new PreservationResponse();
                response.preservation = new Preservation();
                response.preservation.preservation_state = State.PRESERVATION_REQUEST_RECEIVED.name();
                response.preservation.preservation_details = "Preservation request received.";
                byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
                HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
                logger.info("Preservation status updated to '" +  State.PRESERVATION_REQUEST_RECEIVED.name() 
                        +  "' using the updateURI.");
                sd.put(currentUUID, prs);   
            }
            
            if (request.Content_URI == null) {
                // TODO Need to know what to do in this case
                logger.warn("No Content_URI contained in message. No further processing is done for the moment");
            } else {
                if (prs.getState().hasState(State.PRESERVATION_REQUEST_RECEIVED)) {
                    
                    // Try to download ressource from Content_URI
                    tmpFile = null;
                    logger.info("Attempting to download resource from '" 
                            + request.Content_URI + "'");
                    
                    HttpPayload payload = HttpCommunication.get(request.Content_URI);
                    if (payload != null) {
                        tmpFile = payload.writeToFile();
                        PreservationResponse response = new PreservationResponse();
                        response.preservation = new Preservation();
                        response.preservation.preservation_state = State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS.name();
                        response.preservation.preservation_details = "Resource has been downloaded successfully.";
                        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
                        HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
                        logger.info("Preservation status updated.");
                        prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS);
                        sd.put(currentUUID, prs);
                    } else {
                        PreservationResponse response = new PreservationResponse();
                        response.preservation = new Preservation();
                        response.preservation.preservation_state = State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE.name();
                        response.preservation.preservation_details = "Resource could not be downloaded.";
                        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
                        HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
                        logger.info("Preservation status updated.");
                        prs.setState(State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE);
                        sd.put(currentUUID, prs);
                    }
                }
            }
            
            if (prs.getState().hasState(State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS)) {
                // Try to upload to bitrepository
                
                //FIXME change "books" to request.Preservation_profile
                boolean success = bitrepository.uploadFile(tmpFile, "books");
                logger.info(success + "");
            
            }
        } catch (FileNotFoundException e) {
            logger.error(e.toString(), e);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        } catch (YggdrasilException e) {
            logger.error(e.toString(), e);
        } finally {
            bitrepository.shutdown();
            try {
                mq.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        logger.info("Shutting down the Yggdrasil Main program");
        }
    }

}
