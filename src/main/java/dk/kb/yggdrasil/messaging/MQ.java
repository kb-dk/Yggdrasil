package dk.kb.yggdrasil.messaging;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportResponse;

/**
 * Methods for publishing messages on a queue and receiving from a queue
 * using an RabbitMQ broker.
 * Tested with RabbitMQ broker 3.1.5, and amqp-client 3.1.4 (3.1.5 is
 * not available on maven repositories).
 * rabbitmq-javadoc: http://www.rabbitmq.com/releases/rabbitmq-java-client/v3.1.4/rabbitmq-java-client-javadoc-3.1.4/
 */
public class MQ implements AutoCloseable {

    /** List of existing consumers in use by this class.
     * The key is the queueName.
     */
    protected Map<String, QueueingConsumer> existingConsumers;

    /** List of existing consumers in use by this class identified by consumertags. */
    protected Set<String> existingConsumerTags;

    /** channel to the broker. Is one channel enough? */
    protected Channel theChannel;
    /** The settings used to create the broker configurations. */
    protected RabbitMqSettings settings;

    /** Default exchangename to be used by all queues. */
    protected String exchangeName = "exchange"; //TODO should this be a parameter in the settings?
    /** exchange type direct means a message sent to only one recipient. */
    protected String exchangeType = "direct";

    /** The message type for initiating the preservation. */ 
    public static final String PRESERVATIONREQUEST_MESSAGE_TYPE = "PreservationRequest";

    /** The message type for responding to preservation requests. */
    public static final String PRESERVATIONRESPONSE_MESSAGE_TYPE = "PreservationResponse";

    /** The message type for request for importing preserved data. */
    public static final String IMPORTREQUEST_MESSAGE_TYPE = "PreservationImportRequest";

    /** The message type for responding to import requests. */
    public static final String IMPORTRESPONSE_MESSAGE_TYPE = "PreservationImportResponse";

    /** The only valid message type, currently. */ 
    public static final String SHUTDOWN_MESSAGE_TYPE = "Shutdown";

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(MQ.class.getName());

    /**
     * Constructor for the MQ object.
     * @param settings The settings used to create the broker connection.
     * @throws YggdrasilException If it fails.
     * @throws RabbitException When message queue connection fails.
     */
    public MQ(RabbitMqSettings settings) throws YggdrasilException, RabbitException {
        this.existingConsumerTags = new HashSet<String>();
        this.existingConsumers = new HashMap<String, QueueingConsumer>();
        this.settings = settings;
        ConnectionFactory factory = new ConnectionFactory();
        Connection conn = null;
        try {
            factory.setUri(settings.getBrokerUri());
            conn = factory.newConnection();
            logger.info("Connecting to RabbitMQ on server: " + conn.getAddress().getCanonicalHostName());
            theChannel = conn.createChannel();
            configureChannel(settings.getPreservationDestination());
            configureChannel(settings.getPreservationResponseDestination());
            configureChannel(settings.getShutdownDestination());
        } catch (KeyManagementException e1) {
            throw new YggdrasilException("Error connecting to Broker at '"
                    + settings.getBrokerUri() + "' : ", e1);
        } catch (NoSuchAlgorithmException e2) {
            throw new YggdrasilException("Error connecting to Broker at '"
                    + settings.getBrokerUri() + "' : ", e2);
        } catch (URISyntaxException e3) {
            throw new YggdrasilException("Error connecting to Broker at '"
                    + settings.getBrokerUri() + "' : ", e3);
        } catch (IOException e4) {
            throw new RabbitException("Error connecting to Broker at '"
                    + settings.getBrokerUri() + "' : ", e4);
        }
    }

    /**
     * Close channel to broker, and cancel the associated consumers.
     * @throws IOException If it fails to close the connection.
     */
    public void close() throws IOException {
        if (theChannel != null && theChannel.isOpen()) {
            // close existing consumers before closing the channel its connection.
            for (String tag: existingConsumerTags) {
                theChannel.basicCancel(tag);
            }
            Connection conn = theChannel.getConnection();
            theChannel.close();
            conn.close();
        }
    }

    /**
     * @return a set of AMQP properties for
     */
    public static AMQP.BasicProperties getMQProperties() {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        AMQP.BasicProperties persistentTextXml = builder.deliveryMode(2).contentType("text/json").build();
        return persistentTextXml;
    }

    /**
     * Configure a channel with the default configuration.
     * @param destination The destination to configure,
     * @throws YggdrasilException When problem with configuring the channel.
     */
    protected void configureChannel(String destination) throws YggdrasilException {
        try {
            String queueName = destination;
            String routingKey = destination;
            // These next 3 lines are not necessarily all needed
            boolean durableExchange = true; // Exchanges will survive a rabbitmq server crash.
            theChannel.exchangeDeclare(exchangeName, exchangeType, durableExchange);
            boolean durableQueue = true;
            boolean exclusive = false; // meaning restricted to this connection
            boolean autodelete = false; //meaning delete when no longer used
            Map<String, Object> arguments = null;
            theChannel.queueDeclare(queueName, durableQueue, exclusive, autodelete, arguments);
            // Bind a queue to a given exchange
            theChannel.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException e) {
            throw new YggdrasilException("Problems configuring the broker", e);
        }
    }

    /**
     * Publish a message on the given queue.
     * @param queueName A given MQ queue.
     * @param message The message to be published on the queue.
     * @param messageType The Type of the message
     * @throws YggdrasilException If Unable to publish message to the queue.
     */
    public void publishOnQueue(String queueName, byte[] message, String messageType) throws YggdrasilException {
        try {
            String routingKey = queueName;
            AMQP.BasicProperties messageProps = MQ.getMQProperties();
            messageProps.setType(messageType);
            messageProps.setTimestamp(new Date());
            logger.debug("Publishing message on a queue: {} at {}\n {}", queueName, settings.getBrokerUri(), 
                    new String(message, Charset.defaultCharset()));
            theChannel.basicPublish(exchangeName, routingKey, messageProps, message);
        } catch (IOException e) {
            throw new YggdrasilException("Unable to publish message to queue '"
                    + queueName + "'", e);
        }
    }

    /**
     * Receive message from a given queue. If no message is waiting on the queue, this message will
     * wait until a message arrives on the queue.
     * @param queueName The name of the queue.
     * @return the messageType and bytes delivered in the message when a message is received.
     * @throws YggdrasilException If it fails.
     * @throws RabbitException When message queue connection fails.
     */
    public MqResponse receiveMessageFromQueue(String queueName) throws YggdrasilException, RabbitException {
        ArgumentCheck.checkNotNullOrEmpty(queueName, "String queueName");
        QueueingConsumer consumer = null;
        String consumerTag = null;
        if (existingConsumers.containsKey(queueName)) {
            consumer = existingConsumers.get(queueName);
        } else {
            consumer = new QueueingConsumer(theChannel);
            try {
                consumerTag = theChannel.basicConsume(queueName, consumer);
                existingConsumers.put(queueName, consumer);
                existingConsumerTags.add(consumerTag);
            } catch (IOException e) {
                throw new YggdrasilException("Unable to attach to queue '"
                        + queueName + "'", e);
            }
        }
        byte[] payload = null;
        String messageType = null;
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            messageType = delivery.getProperties().getType();
            Date sentDate = delivery.getProperties().getTimestamp();
            logger.info("received message of type '" + messageType 
                    + "' with timestamp '" + sentDate + "'");
            payload = delivery.getBody();
            boolean acknowledgeMultipleMessages = false;
            theChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), 
                    acknowledgeMultipleMessages);
        } catch (IOException e) {
            throw new YggdrasilException("Unable to receive message from queue '"
                    + queueName + "'", e);
        } catch (ShutdownSignalException e) {
            throw new RabbitException("Unable to receive message from queue '"
                    + queueName + "'", e);
        } catch (ConsumerCancelledException e) {
            throw new YggdrasilException("Unable to receive message from queue '"
                    + queueName + "'", e);
        } catch (InterruptedException e) {
            throw new YggdrasilException("Unable to receive message from queue '"
                    + queueName + "'", e);
        }

        return new MqResponse(messageType, payload);
    }
    
    /**
     * Purges the queue.
     * @param queue The queue to purge.
     * @throws RabbitException 
     */
    public void purgeQueue(String queue) throws RabbitException {
        try {
            theChannel.queuePurge(queue);
        } catch (IOException e) {
            throw new RabbitException("Could not purge the queue.", e);
        }
    }
    
    /**
     * Publishes a preservation response message.
     * @param response The preservation response message.
     * @throws YggdrasilException If unable to publish the preservation response on the message queue.
     */
    public void publishPreservationResponse(PreservationResponse response) throws YggdrasilException {
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        publishOnQueue(settings.getPreservationResponseDestination(), responseBytes, 
                MQ.PRESERVATIONRESPONSE_MESSAGE_TYPE);
    }

    /**
     * Publishes a preservation response message.
     * @param response The preservation response message.
     * @throws YggdrasilException If unable to publish the preservation response on the message queue.
     */
    public void publishPreservationImportResponse(PreservationImportResponse response) throws YggdrasilException {
        byte[] responseBytes = JSONMessaging.getPreservationImportResponse(response);
        publishOnQueue(settings.getPreservationResponseDestination(), responseBytes, 
                MQ.IMPORTRESPONSE_MESSAGE_TYPE);
    }
    
    /**
     * @return The settings.
     */
    public RabbitMqSettings getSettings() {
        return this.settings;
    }
}
