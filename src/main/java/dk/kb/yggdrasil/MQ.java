package dk.kb.yggdrasil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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
 */
public class MQ {
    
    /** 
     * @return a set of AMQP properties for 
     */
    public static AMQP.BasicProperties getMQProperties() {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        AMQP.BasicProperties persistentTextXml = builder.deliveryMode(2).contentType("text/xml").build();
        return persistentTextXml;
    }
    
    /**
     * Create a Channel to a RabbitMQ broker based on the given setting.
     * @param setting A RabbitMQSettings instance.
     * @return a working channel to a specific RabbitMQ broker.
     * @throws YggdrasilException When problem with creating channel.
     */
    public static Channel createChannel(RabbitMqSettings setting) throws YggdrasilException {
        Channel ch = null;
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(setting.getBrokerUri());
            Connection conn = factory.newConnection();
            ch = conn.createChannel();
            String exchangeName = "exchange"; //TODO should this be a parameter in the settings?
            String queueName = setting.getPreservationDestination();
            String routingKey = setting.getPreservationDestination();
            ch.exchangeDeclare(exchangeName, "direct", true);
            ch.queueDeclare(queueName, true, false, false, null);
            ch.queueBind(queueName, exchangeName, routingKey);
           
        } catch (KeyManagementException e) {
            throw new YggdrasilException("Error connecting to Broker:", e);
        } catch (NoSuchAlgorithmException e) {
            throw new YggdrasilException("Error connecting to Broker:", e);
        } catch (URISyntaxException e) {
            throw new YggdrasilException("Error connecting to Broker:", e);
        } catch (IOException e) {
            throw new YggdrasilException("Error connecting to Broker:", e);
        }
        return ch;
    }
   
   /**
    * Close the given channel. This also closes the connection that created the channel.
    * @param ch A given channel to the broker.
    * @throws IOException If problems closing down the channel and associated connection.
    */
   public static void closeChannel(Channel ch) throws IOException {
       Connection conn = ch.getConnection();
       ch.close();
       conn.close();
   }
   
   /**
    * Publish a message on the given queue.
    * @param queueName A given MQ queue. 
    * @param ch An already created channel to the MQ broker.
    * @param message The message to be published on the queue.
    * @throws YggdrasilException If Unable to publish message to the queue.
    */
   public static void publishOnQueue(String queueName, Channel ch, byte[] message) 
           throws YggdrasilException {
       try {
           String exchangeName = "exchange";
           String routingKey = queueName;
           ch.basicPublish(exchangeName, routingKey, MQ.getMQProperties(), message);
       } catch (IOException e) {
           throw new YggdrasilException("Unable to publish message to queue '" 
                   + queueName + "'", e);
       }
   }
   
   /**
    * Receive message from a given queue. If no message is waiting on the queue, this message will
    * wait until a message arrives on the queue. 
    * @param queueName The name of the queue.
    * @param ch An already created channel to the MQ broker.
    * @return the bytes delivered in the message when a message is received.
    * @throws YggdrasilException
    */
   public static byte[] receiveMessageFromQueue(String queueName, Channel ch) throws YggdrasilException {
       QueueingConsumer consumer = new QueueingConsumer(ch);
       byte[] payload = null;
       String consumerTag = null;
       try {
        consumerTag = ch.basicConsume(queueName, consumer);
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        payload = delivery.getBody();
        ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        ch.basicCancel(consumerTag); // cancel the consumer
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
}
    
