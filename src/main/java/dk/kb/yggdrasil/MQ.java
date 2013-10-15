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
 * Methods for publishing messages on a queue and receiving from a queue.
 *
 */
public class MQ {
    
    public static AMQP.BasicProperties getMQProperties() {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        AMQP.BasicProperties persistentTextXml = builder.deliveryMode(2).contentType("text/xml").build();
        return persistentTextXml;
    }
    
    public static Channel createChannel(RabbitMqSettings setting) throws YggdrasilException {
        Channel ch = null;
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(setting.getBrokerUri());
            Connection conn = factory.newConnection();
            ch = conn.createChannel();
            String exchangeName = "exchange";
            String queueName = setting.getPreservationDestination();
            String routingKey = setting.getPreservationDestination();
            //ch.exchangeDeclare(setting.getPreservationDestination(), "direct"); 
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
   
   public static void publishOnQueue(String queueName, Channel ch, byte[] message) 
           throws YggdrasilException {
       try {
           ch.basicPublish(queueName, queueName, MQ.getMQProperties(), message);
       } catch (IOException e) {
           throw new YggdrasilException("Unable to publish message to queue '" 
                   + queueName + "'", e);
       }
   }
   
   
   
   public static byte[] receiveOnQueue(String queueName, Channel ch) throws YggdrasilException {
       QueueingConsumer consumer = new QueueingConsumer(ch);
       byte[] payload = null;
       try {
        ch.basicConsume(queueName, consumer);
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        //system.out.println("Message: " + new String(delivery.getBody()));
        payload = delivery.getBody();
        ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
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
    
    

