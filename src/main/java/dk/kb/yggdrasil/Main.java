package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.antiaction.common.json.JSONException;

import dk.kb.yggdrasil.JSONMessaging.Preservation;
import dk.kb.yggdrasil.JSONMessaging.PreservationRequest;
import dk.kb.yggdrasil.JSONMessaging.PreservationResponse;
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

    /** 
     * The list of configuration files that should be present in the configuration directory.
     */
    public static final String[] REQUIRED_SETTINGS_FILES = new String[] {"rabbitmq.yml"};
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

        try {
        	File rabbitmqConfigFile = new File(configdir, "rabbitmq.yml");
			RabbitMqSettings rabbitMqSettings = new RabbitMqSettings(rabbitmqConfigFile);
			MQ mq = MQ.getInstance(rabbitMqSettings);
			mq.configureDefaultChannel();

	        File bitmagConfigFile = new File(configdir, "bitmag.yml");
	        Bitrepository bitrepository = new Bitrepository(bitmagConfigFile);

			byte[] requestBytes = mq.receiveMessageFromQueue("preservation-dev-queue");

			PreservationRequest request = JSONMessaging.getPreservationRequest(new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4));

			logger.info("Preservation request received.");

			System.out.println(request.UUID);
			System.out.println(request.Preservation_profile);
			System.out.println(request.Update_URI);
			System.out.println(request.File_UUID);
			System.out.println(request.Content_URI);
			if (request.metadata != null) {
				System.out.println(request.metadata.descMetadata);
				System.out.println(request.metadata.preservationMetadata);
				System.out.println(request.metadata.provenanceMetadata);
				System.out.println(request.metadata.techMetadata);
			}

			if (request.Update_URI != null) {
				PreservationResponse response = new PreservationResponse();
				response.preservation = new Preservation();
				response.preservation.preservation_state = State.PRESERVATION_REQUEST_RECEIVED.name();
				response.preservation.preservation_details = "Preservation request recieved.";
				byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
				HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
				logger.info("Preservation status updated.");
			}

			byte[] tmpBuf = new byte[16384];
			int read;
			File tmpFile;
			RandomAccessFile raf;

			if (request.Content_URI != null) {
				logger.info("Attempting to download resource.");
				HttpPayload payload = HttpCommunication.get(request.Content_URI);
				if (payload != null) {
					UUID uuid = UUID.randomUUID();
					tmpFile = new File(uuid.toString());
					raf = new RandomAccessFile(tmpFile, "rw");
					InputStream in = payload.contentBody;
					while ((read = in.read(tmpBuf)) != -1 ) {
						raf.write(tmpBuf, 0, read);
					}
					raf.close();
					raf = null;
					in.close();

					if (request.Update_URI != null) {
						PreservationResponse response = new PreservationResponse();
						response.preservation = new Preservation();
						response.preservation.preservation_state = State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS.name();
						response.preservation.preservation_details = "Resource has been downloaded successfully.";
						byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
						HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
						logger.info("Preservation status updated.");
					}

			        boolean success = bitrepository.uploadFile(tmpFile, "books");

			        logger.info(success + "");
				} else {
					if (request.Update_URI != null) {
						PreservationResponse response = new PreservationResponse();
						response.preservation = new Preservation();
						response.preservation.preservation_state = State.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE.name();
						response.preservation.preservation_details = "Resource could not be downloaded.";
						byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
						HttpCommunication.post(request.Update_URI, responseBytes, "application/json");
						logger.info("Preservation status updated.");
					}
				}
			}

			bitrepository.shutdown();
			mq.close();
		} catch (FileNotFoundException e) {
			logger.error(e.toString(), e);
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (JSONException e) {
			logger.error(e.toString(), e);
		}

        logger.info("Shutting down the Yggdrasil Main program");
    }

}
