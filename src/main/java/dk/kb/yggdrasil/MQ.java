package dk.kb.yggdrasil;

import java.io.IOException;
import java.net.URISyntaxException;
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

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Methods for publishing messages on a queue and receiving from a queue
 * using an RabbitMQ broker.
 * Tested with RabbitMQ broker 3.1.5, and amqp-client 3.1.4 (3.1.5 is
 * not available on maven repositories).
 * rabbitmq-javadoc: http://www.rabbitmq.com/releases/rabbitmq-java-client/v3.1.4/rabbitmq-java-client-javadoc-3.1.4/
 */
public class MQ {

    /** List of existing consumers in use by this class.
     * The key is the queueName.
     */
    private Map<String, QueueingConsumer> existingConsumers;

    /** List of existing consumers in use by this class identified by consumertags. */
    private Set<String> existingConsumerTags;

    /** channel to the broker. Is one channel enough? */
    private Channel theChannel;
    /** The settings used to create the broker configurations. */
    private RabbitMqSettings settings;
    /** the singleton instance. */
    private static MQ instance;

    /** Default exchangename to be used by all queues. */
    private String exchangeName = "exchange"; //TODO should this be a parameter in the settings?
    /** exchange type direct means a message sent to only one recipient. */
    private String exchangeType = "direct";

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(MQ.class.getName());
    
    /**
     * private constructor for the MQ singleton.
     * @param settings
     * @throws YggdrasilException
     */
    private MQ(RabbitMqSettings settings) throws YggdrasilException {
        this.existingConsumerTags = new HashSet<String>();
        this.existingConsumers = new HashMap<String, QueueingConsumer>();
        this.settings = settings;
        ConnectionFactory factory = new ConnectionFactory();
        Connection conn = null;
        try {
            factory.setUri(settings.getBrokerUri());
            conn = factory.newConnection();
            theChannel = conn.createChannel();
            configureDefaultChannel();
        } catch (KeyManagementException e1) {
            throw new YggdrasilException("Error connecting to Broker:", e1);
        } catch (NoSuchAlgorithmException e2) {
            throw new YggdrasilException("Error connecting to Broker:", e2);
        } catch (URISyntaxException e3) {
            throw new YggdrasilException("Error connecting to Broker:", e3);
        } catch (IOException e4) {
            throw new YggdrasilException("Error connecting to Broker:", e4);
        }
    }

    /**
     * Close channel to broker, and cancel the associated consumers.
     * @throws IOException
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
        AMQP.BasicProperties persistentTextXml = builder.deliveryMode(2).contentType("text/xml").build();
        return persistentTextXml;
    }

    /**
     * Configure default channel configuration.
     * @throws YggdrasilException When problem with configuring the channel.
     */
    public void configureDefaultChannel() throws YggdrasilException {
        Channel ch = this.theChannel;
        try {
            String queueName = settings.getPreservationDestination();
            String routingKey = settings.getPreservationDestination();
            // These next 3 lines are not necessarily all needed
            boolean durableExchange = true; // Exchanges will survive a rabbitmq server crash.
            ch.exchangeDeclare(exchangeName, exchangeType, durableExchange);
            boolean durable = true;
            boolean exclusive = false; // meaning restricted to this connection
            boolean autodelete = false; //meaning delete when no longer used
            Map<String, Object> arguments = null;
            ch.queueDeclare(queueName, durable, exclusive, autodelete, arguments);
            // Bind a queue to a given exchange
            ch.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException e) {
            throw new YggdrasilException("Problems configuring the broker", e);
        }
    }

   /**
    * Publish a message on the given queue.
    * @param queueName A given MQ queue.
    * @param ch An already created channel to the MQ broker.
    * @param message The message to be published on the queue.
    * @throws YggdrasilException If Unable to publish message to the queue.
    */
   public void publishOnQueue(String queueName, byte[] message)
           throws YggdrasilException {
       try {
           String routingKey = queueName;
           theChannel.basicPublish(exchangeName, routingKey, MQ.getMQProperties(), message);
       } catch (IOException e) {
           throw new YggdrasilException("Unable to publish message to queue '"
                   + queueName + "'", e);
       }
   }

   /**
    * Receive message from a given queue. If no message is waiting on the queue, this message will
    * wait until a message arrives on the queue.
    * @param queueName The name of the queue.
    * @return the bytes delivered in the message when a message is received.
    * @throws YggdrasilException
    */
   public byte[] receiveMessageFromQueue(String queueName) throws YggdrasilException {
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
       try {
           QueueingConsumer.Delivery delivery = consumer.nextDelivery();
           String messageType = delivery.getProperties().getType();
           Date sentDate = delivery.getProperties().getTimestamp();
           logger.info("received message of type '" + messageType 
                   + "' with timestamp " + sentDate);
           payload = delivery.getBody();
           boolean acknowledgeMultipleMessages = false;
           theChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), acknowledgeMultipleMessages);
       } catch (IOException e) {
           throw new YggdrasilException("Unable to receive message from queue '"
                   + queueName + "'", e);
       } catch (ShutdownSignalException e) {
           throw new YggdrasilException("Unable to receive message from queue '"
                   + queueName + "'", e);
       } catch (ConsumerCancelledException e) {
           throw new YggdrasilException("Unable to receive message from queue '"
                   + queueName + "'", e);
       } catch (InterruptedException e) {
           throw new YggdrasilException("Unable to receive message from queue '"
                   + queueName + "'", e);
       }

       return payload;
   }

   /**
    * Create a singleton instance if not already created.
    * @param settings The settings used to create the broker connection.
    * @return the singleton object of this class.
    * @throws YggdrasilException If Unable to create an instance of this class.
    */
   public static synchronized MQ getInstance(RabbitMqSettings settings) throws YggdrasilException {
       if (instance == null){
           instance = new MQ(settings);
       }
       return instance;
   }
}
