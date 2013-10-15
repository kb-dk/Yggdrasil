package dk.kb.yggdrasil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.junit.Ignore;
import org.junit.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

public class MqTester {
  
    @Ignore
    @Test
    public void finalTest() throws YggdrasilException {
        RabbitMqSettings settings = new RabbitMqSettings(
                "amqp://localhost:5600", "dev-queue", "dev-queue");
        Channel ch = MQ.createChannel(settings);

        String message = "Hello world";
        MQ.publishOnQueue("dev-queue", ch, message.getBytes());
        byte[] messageReceived = MQ.receiveOnQueue("dev-queue", ch);
        System.out.println("Message:" + new String(messageReceived));
    }
    
    @Ignore
    @Test
    public void test() throws YggdrasilException {
        RabbitMqSettings settings = new RabbitMqSettings(
                "amqp://localhost:5600", "dev-queue", "dev-queue");
        Channel ch = MQ.createChannel(settings);

        String message = "Hello world";
        try {
            ch.basicPublish("dev-queue", "dev-queue" , MQ.getMQProperties(), 
                    message.getBytes());
            Connection conn = ch.getConnection();
            ch.close();
            conn.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Ignore
    @Test
    public void testReceive() 
            throws KeyManagementException, NoSuchAlgorithmException, 
            URISyntaxException, IOException, ShutdownSignalException, 
            ConsumerCancelledException, InterruptedException {
    
    String queueName = "dev-queue";

    ConnectionFactory connFactory = new ConnectionFactory();
    connFactory.setUri("amqp://localhost:5600");
    Connection conn = connFactory.newConnection();

    final Channel ch = conn.createChannel();

    ch.queueDeclare(queueName, false, false, false, null);

    QueueingConsumer consumer = new QueueingConsumer(ch);
    ch.basicConsume(queueName, consumer);
    
    //while (true) {
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        System.out.println("Message: " + new String(delivery.getBody()));
        ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    //}
    }
    
    @Ignore
    @Test
    public void testReceived() throws KeyManagementException, 
        NoSuchAlgorithmException, URISyntaxException, IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqp://localhost:5600");
        Connection conn = factory.newConnection();

        final Channel channel = conn.createChannel();

        String exchangeName = "exchange";
        String queueName = "queue";
        String routingKey = "routing";

        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);
        byte[] messageBodyBytes = "hello world".getBytes();
     // .userId("bob")
        channel.basicPublish(exchangeName, routingKey,
    new AMQP.BasicProperties.Builder().contentType("text/plain").deliveryMode(2)
        .priority(1).build(), messageBodyBytes);

        try {
                Thread.sleep(5*1000);
        } catch (InterruptedException e) {
        }

        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, "myConsumerTag", 
                new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                throws IOException {
                String routingKey = envelope.getRoutingKey();
                String contentType = properties.getContentType();
                long deliveryTag = envelope.getDeliveryTag();
                // (process the message components here ...)
                System.out.println(new String(body));
                channel.basicAck(deliveryTag, false);
            }
           });

        
        // .userId("bob")
        channel.basicPublish(exchangeName, routingKey,
            new AMQP.BasicProperties.Builder().contentType("text/plain").deliveryMode(2).priority(1)
                .build(),messageBodyBytes);

        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
        }

        System.out.println(channel.getCloseReason());
        channel.close();
        conn.close();

    }
}
    
