package dk.kb.yggdrasil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/**
 * This class contains the known settings for the rabbitmq broker.
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
    /** The property for the rabbitmq dissemination setting in our rabbitmq.yml */
    public static final String RABBIT_MQ_DISSEMINATION_PROPERTY  = "dissemination";
    /** The property for the destination subsetting in our rabbitmq.yml */
    public static final String RABBIT_MQ_DESTINATION_PROPERTY  = "destination";
    /** Use these this property to override the rabbitmq hostname in the YAML file. */
    public static final String RABBIT_MQ_HOSTNAME = "RABBITMQ_HOSTNAME";
    /** Use these this property to override the rabbitmq port in the YAML file. */
    public static final String RABBIT_MQ_PORT = "RABBITMQ_PORT";

    /** The broker address as a URI. */
    private String brokerUri;
    /** The name of the preservation queue. */
    private String preservationDestination;

    /** The name of the dissemination queue. */
    private String disseminationDestination;

    /**
     * Constructor. Reads RabbitMQ settings from a YAML file.
     * @param ymlFile A YAML file containing RabbitMQ settings.
     * @throws YggdrasilException If some or all of the required RabbitMQ settings are missing.
     * @throws YggdrasilException If the YAML file is missing
     */
    public RabbitMqSettings(File ymlFile) throws YggdrasilException, FileNotFoundException {
        // Select CorrectLinkedHashMap based on the runningmode.
        String mode = RunningMode.getMode().toString();
        Map settings = YamlTools.loadYamlSettings(ymlFile);
        if (!settings.containsKey(mode)) {
            throw new YggdrasilException("Unable to find rabbitMQ settings for the mode '"
                    + mode + "' in the given YAML file ' " + ymlFile.getAbsolutePath() + "'");
        }
        settings = (Map) settings.get(mode);

        if (settings.containsKey(RABBIT_MQ_URI_PROPERTY)
                && settings.containsKey(RABBIT_MQ_PRESERVATION_PROPERTY)
                && settings.containsKey(RABBIT_MQ_DISSEMINATION_PROPERTY)) {
            brokerUri = (String) settings.get(RABBIT_MQ_URI_PROPERTY);
            Map preservationMap = (Map) settings.get(RABBIT_MQ_PRESERVATION_PROPERTY);
            preservationDestination = (String) preservationMap.get(RABBIT_MQ_DESTINATION_PROPERTY);
            Map disseminationMap = (Map) settings.get(RABBIT_MQ_DISSEMINATION_PROPERTY);
            disseminationDestination = (String) disseminationMap.get(RABBIT_MQ_DESTINATION_PROPERTY);
        } else {
            throw new YggdrasilException("Missing some or all of the required properties in the settings file");
        }
    }

    /**
     * Alternate constructor.
     * @param brokerUri The URI to connect to the broker.
     * @param preservationDestination The Queue for receiving messages from Valhal
     * @param disseminationDestination The Queue for receiving messages from Bifrost
     */
    public RabbitMqSettings(String brokerUri, String preservationDestination,
            String disseminationDestination)  {
        this.brokerUri = brokerUri;
        this.preservationDestination = preservationDestination;
        this.disseminationDestination = disseminationDestination;
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
     * @return the dissemination destination
     */
    public String getDisseminationDestination() {
        return disseminationDestination;
    }

    /**
     * Set the brokerUri.
     * @param newBrokerUri A new value for the brokerUri
     */
    public void setBrokerUri(String newBrokerUri) {
       this.brokerUri = newBrokerUri;
    }

}
