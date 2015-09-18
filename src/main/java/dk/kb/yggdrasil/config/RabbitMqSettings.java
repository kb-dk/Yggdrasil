package dk.kb.yggdrasil.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/**
 * This class contains the known settings for the rabbitmq broker.
 * Reads a rabbitmq.yml with the following syntax for each runningmode:
 * 
 * development:
 *   mq_uri: "amqp://localhost:5672"
 *   preservation:
 *       destination: "dev-queue"
 *   dissemination:
 *       destination: "dev-queue"
 */
public final class RabbitMqSettings {

    /** The property for the rabbitmq broker_uri setting in our rabbitmq.yml. */
    public static final String RABBIT_MQ_URI_PROPERTY  = "mq_uri";
    /** The property for the rabbitmq preservation setting in our rabbitmq.yml */
    public static final String RABBIT_MQ_PRESERVATION_PROPERTY  = "preservation";
    /** The property for the rabbitmq preservation response queue name */
    public static final String RABBIT_MQ_PRESERVATION_RESPONSE_PROPERTY = "response";
    /** The property for the destination subsetting in our rabbitmq.yml */
    public static final String RABBIT_MQ_DESTINATION_PROPERTY  = "destination";
    /** The property for the rabbitmq  interval for polling the message queue in minutes */
    public static final String RABBIT_MQ_POLLING_INTERVAL_IN_MINUTES_PROPERTY = "polling_interval_in_minutes";
    /** Use these this property to override the rabbitmq hostname in the YAML file. */
    public static final String RABBIT_MQ_HOSTNAME = "RABBITMQ_HOSTNAME";
    /** Use these this property to override the rabbitmq port in the YAML file. */
    public static final String RABBIT_MQ_PORT = "RABBITMQ_PORT";
    /** Default value for the rabbitmq port number. */
    public static final String RABBIT_MQ_DEFAULT_PORT = "5672";
    /** Default value for the rabbitmq hostname. */
    public static final String RABBIT_MQ_DEFAULT_HOSTNAME = "localhost";

    /** The broker address as a URI. */
    private String brokerUri;
    
    /** The name of the preservation queue. */
    private String preservationDestination;

    /** The name of the preservation response queue. **/
    private String preservationResponseDestination;

    /** The interval for polling the MQ in minutes. **/
    private int pollingIntervalInMinutes;

    /**
     * Constructor. Reads RabbitMQ settings from a YAML file.
     * @param ymlFile A YAML file containing RabbitMQ settings.
     * @throws YggdrasilException If some or all of the required RabbitMQ settings are missing.
     * @throws FileNotFoundException If the YAML file is missing
     */
    public RabbitMqSettings(File ymlFile) throws YggdrasilException, FileNotFoundException {
        // Select correct LinkedHashMap based on the runningmode.
        String mode = RunningMode.getMode().toString();
        Map settings = YamlTools.loadYamlSettings(ymlFile);
        if (!settings.containsKey(mode)) {
            throw new YggdrasilException("Unable to find rabbitMQ settings for the mode '"
                    + mode + "' in the given YAML file ' " + ymlFile.getAbsolutePath() + "'");
        }
        settings = (Map) settings.get(mode);

        if (settings.containsKey(RABBIT_MQ_URI_PROPERTY)
                && settings.containsKey(RABBIT_MQ_PRESERVATION_PROPERTY)) {
            brokerUri = (String) settings.get(RABBIT_MQ_URI_PROPERTY);
            Map preservationMap = (Map) settings.get(RABBIT_MQ_PRESERVATION_PROPERTY);
            preservationDestination = (String) preservationMap.get(RABBIT_MQ_DESTINATION_PROPERTY);
            preservationResponseDestination = (String) preservationMap.get(RABBIT_MQ_PRESERVATION_RESPONSE_PROPERTY);

            try {
                pollingIntervalInMinutes = (Integer) preservationMap.get(
                        RABBIT_MQ_POLLING_INTERVAL_IN_MINUTES_PROPERTY);

                if (pollingIntervalInMinutes < 0) {
                    throw new YggdrasilException("The polling interval wasn't set correctly, it has to be positive");
                }
            } catch (Exception e) {
                throw new YggdrasilException("The polling interval wasn't set correctly");
            }
        } else {
            throw new YggdrasilException("Missing some or all of the required properties in the settings file");
        }

        // Check if rabbitmq-port or rabbitmq-hostname is overridden by defined properties
        // If either is overriden, set the BrokerURI to the overridden values and use the default value if only
        // partially overriden.
        if (null != System.getProperty(RabbitMqSettings.RABBIT_MQ_HOSTNAME)
                ||
                null != System.getProperty(RabbitMqSettings.RABBIT_MQ_PORT)) {
            setBrokerUri("amqp://"
                    + System.getProperty(RabbitMqSettings.RABBIT_MQ_HOSTNAME, RABBIT_MQ_DEFAULT_HOSTNAME)
                    + ":" + System.getProperty(RabbitMqSettings.RABBIT_MQ_PORT, RABBIT_MQ_DEFAULT_PORT));
        }
    }

    /**
     * Alternate constructor.
     * @param brokerUri The URI to connect to the broker.
     * @param preservationDestination The Queue for receiving messages from Valhal
     * @param preservationResponseDestination The Queue for sending responses to Valhal.
     */
    public RabbitMqSettings(String brokerUri, String preservationDestination, 
            String preservationResponseDestination) {
        this.brokerUri = brokerUri;
        this.preservationDestination = preservationDestination;
        this.preservationResponseDestination = preservationResponseDestination;
    }

    /**
     * @return the brokerUri
     */
    public String getBrokerUri() {
        return brokerUri;
    }

    /**
     * @return the preservation destination
     */
    public String getPreservationDestination() {
        return preservationDestination;
    }

    /**
     * Set the preservation destination.
     * @param preservationDestination The new destination for the preservation.
     */
    public void setPreservationDestination(String preservationDestination) {
        this.preservationDestination = preservationDestination;
    }
    
    /**
     *
     * @return the preservation response destination
     */
    public String getPreservationResponseDestination() {
        return preservationResponseDestination;
    }

    /**
     * Set the preservation response destination
     * @param preservationResponseDestination The new destination for the preservation responses.
     */
    public void setPreservationResponseDestination(String preservationResponseDestination) {
        this.preservationResponseDestination = preservationResponseDestination;
    }

    /**
     *
     * @return the interval for polling the MQ in minutes
     */
    public int getPollingIntervalInMinutes() {
        return pollingIntervalInMinutes;
    }

    /**
     * Set the brokerUri.
     * @param newBrokerUri A new value for the brokerUri
     */
    public void setBrokerUri(String newBrokerUri) {
        this.brokerUri = newBrokerUri;
    }

}
