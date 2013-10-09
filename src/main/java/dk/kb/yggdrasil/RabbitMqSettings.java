package dk.kb.yggdrasil;

import java.util.LinkedHashMap;

/**
 * This class contains the known settings for the rabbitmq broker.
 */
public class RabbitMqSettings {

    public static String RABBIT_MQ_YAML_PROPERTY = "rabbitmq";
    public static String RABBIT_MQ_URI_PROPERTY  = "broker_uri";
    public static String RABBIT_MQ_DESTINATION_PROPERTY  = "preservation_destination";   
    
    private String brokerUri;
    private String preservationDestination;
    
    public String getBrokerUri() {
        return brokerUri;
    }
    
    public RabbitMqSettings(LinkedHashMap settings) throws Exception{
        if (settings.containsKey(RABBIT_MQ_URI_PROPERTY) 
                && settings.containsKey(RABBIT_MQ_DESTINATION_PROPERTY)) {
            brokerUri = (String) settings.get(RABBIT_MQ_URI_PROPERTY);
            preservationDestination = (String) settings.get(RABBIT_MQ_DESTINATION_PROPERTY);
        } else {
            throw new Exception("Missing properties in settings file");
        }
    }
    
    
    public String getPreservationDestination() {
        return preservationDestination;
    }
}
