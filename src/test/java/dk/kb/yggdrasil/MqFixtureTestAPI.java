package dk.kb.yggdrasil;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;

public class MqFixtureTestAPI {

    public static MQ instantiatePurgesMQ(RabbitMqSettings settings) throws IOException, YggdrasilException, RabbitException {
        TestRabbitMQ mq = new TestRabbitMQ(settings);
        mq.purgeQueue(settings.getPreservationDestination());
        mq.purgeQueue(settings.getPreservationResponseDestination());
        mq.purgeQueue(settings.getShutdownDestination());
        return mq;
    }
    
    /**
     * A test-only instance of the MQ class. 
     * It allows purge of queues, which should not be possible in the main branch!
     */
    public static class TestRabbitMQ extends MQ {
        
        public TestRabbitMQ(RabbitMqSettings settings) throws YggdrasilException,
                RabbitException {
            super(settings);
        }
        
        /**
         * @return The channel.
         */
        public Channel getChannel() {
            return theChannel;
        }
    }
}
