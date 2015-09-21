package dk.kb.yggdrasil.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import dk.kb.yggdrasil.MqFixtureTestAPI;
import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.config.RunningMode;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;

/**
 * These tests assume the rabbitmq on localhost, if not override RABBITMQ_HOSTNAME
 * (e.g. export RABBITMQ_HOSTNAME=dia-prod-udv-01.kb.dk or DRABBITMQ_HOSTNAME=dia-prod-udv-01.kb.dk,)
 * and another port than 5672 by setting the RABBITMQ_PORT (
 * (e.g. export RABBITMQ_PORT=5673 or DRABBITMQ_PORT=5673);
 * 
 * You can have several rabbitmq brokers running on the same server.
 * Each though must have their own port, and nodename which can be set
 * using export RABBITMQ_NODENAME=bunny
 * See manpage for rabbitmq-server for more details.
 *
 */
@RunWith(JUnit4.class)
public class MqTest extends MqFixtureTestAPI {
    public static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    
    @BeforeClass
    public static void initialize() {
    	System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, RunningMode.TEST.name());
    }

    /**
     * Testing of queue publish and consume for MQ class 
     * @throws YggdrasilException
     * @throws IOException
     * @throws RabbitException
     */
    @Test
    public void finalTest() throws YggdrasilException, IOException, RabbitException {
        String methodName = this.getClass().getName()
                + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
        
        RabbitMqSettings settings = fetchMqSettings();
        settings.setPreservationDestination(settings.getPreservationDestination() + "-" + methodName);
        MQ mq = instantiatePurgesMQ(settings);
        assertTrue(settings.equals(mq.getSettings()));
        
        String message = "Hello world from " + methodName;
        String queueName = settings.getPreservationDestination();
        mq.publishOnQueue(queueName, message.getBytes(), MQ.PRESERVATIONREQUEST_MESSAGE_TYPE);
        MqResponse messageReceived = mq.receiveMessageFromQueue(queueName); 
        Assert.assertArrayEquals(message.getBytes(), messageReceived.getPayload());
        
        message = "Hello X from " + methodName;
        mq.publishOnQueue(queueName, message.getBytes(), MQ.PRESERVATIONREQUEST_MESSAGE_TYPE);
        messageReceived = mq.receiveMessageFromQueue(queueName);
        Assert.assertArrayEquals(message.getBytes(), messageReceived.getPayload());

        mq.close();
    }

    /**
     * Unit test for sending a Yggdrasil shutdown message
     * @throws YggdrasilException
     * @throws IOException
     * @throws RabbitException
     */
    @Test
    public void sendShutdown() throws YggdrasilException, IOException, RabbitException {
        String methodName = this.getClass().getName()
                + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
        
        RabbitMqSettings settings = fetchMqSettings();
        settings.setPreservationDestination(settings.getPreservationDestination() + "-" + methodName);
        MQ mq = instantiatePurgesMQ(settings);
        
        String queueName = settings.getPreservationDestination();
        String message = "Shutdown Message";
        mq.publishOnQueue(queueName, message.getBytes(), MQ.SHUTDOWN_MESSAGE_TYPE);
        MqResponse messageReceived = mq.receiveMessageFromQueue(queueName);
        assertTrue(messageReceived.getMessageType().equals(MQ.SHUTDOWN_MESSAGE_TYPE));
        Assert.assertArrayEquals(message.getBytes(), messageReceived.getPayload());
        
        mq.close();
    }

    /**
     * Unit test for general testing of queue publish and consume
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException
     * @throws IOException
     * @throws YggdrasilException
     * @throws ShutdownSignalException
     * @throws ConsumerCancelledException
     * @throws InterruptedException
     */
    @Test
    public void testReceived() throws KeyManagementException,NoSuchAlgorithmException, URISyntaxException, IOException, 
    YggdrasilException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        String methodName = this.getClass().getName()
                + "." + Thread.currentThread().getStackTrace()[1].getMethodName();

        RabbitMqSettings settings = fetchMqSettings();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(settings.getBrokerUri());
        Connection conn = factory.newConnection();
        final Channel channel = conn.createChannel();
        
        String exchangeName = "exchange" + "-" + this.getClass().getName();
        String queueName = settings.getPreservationDestination() + "-" + methodName;
        String routingKey = "routing";
        boolean queueDurable = true;

        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueDeclare(queueName, queueDurable, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);

        String message = "Hello world from " + methodName;
        byte[] messageBodyBytes = message.getBytes();
        
        channel.basicPublish(exchangeName, routingKey,
                new AMQP.BasicProperties.Builder().contentType("text/plain").deliveryMode(2)
                .priority(1).build(), messageBodyBytes);
        
        final boolean AUTO_ACK = false;
        
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, AUTO_ACK, consumer);
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        Assert.assertArrayEquals(messageBodyBytes, delivery.getBody());
        
        channel.exchangeDelete(exchangeName, false);
        channel.queueDelete(queueName, false, true);
        channel.close();
        conn.close();
    }

    /**
     * Unit test for alternate constructor for RabbitMq setting
     * @throws YggdrasilException
     * @throws FileNotFoundException
     */
    @Test
    public void testRabbitMqSettingsAlternateConstructor() throws YggdrasilException, FileNotFoundException {
        File f = new File(RABBITMQ_CONF_FILE);
        RabbitMqSettings settings = new RabbitMqSettings(f);
        String brokerUri = settings.getBrokerUri();
        String presDest = settings.getPreservationDestination();
        String presRecDest = settings.getPreservationResponseDestination();
        RabbitMqSettings settingsCopy = new RabbitMqSettings(brokerUri, presDest, presRecDest);
        assertEquals(brokerUri, settingsCopy.getBrokerUri());
        assertEquals(presDest, settingsCopy.getPreservationDestination());
    }

    /**
     * Helper method that fetch RabbitMq settings
     * @throws YggdrasilException
     * @throws FileNotFoundException
     */
    private RabbitMqSettings fetchMqSettings() throws YggdrasilException, FileNotFoundException {
        File f = new File(RABBITMQ_CONF_FILE);
        RabbitMqSettings settings = new RabbitMqSettings(f);
        return settings;
    }
}
