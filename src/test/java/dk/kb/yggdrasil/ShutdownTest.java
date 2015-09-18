package dk.kb.yggdrasil;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.config.Config;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;

/**
 * Tests for {@link dk.kb.yggdrasil.Main }
 */
@RunWith(JUnit4.class)
public class ShutdownTest {

    File goodConfigDir = new File("src/test/resources/config");

    @Test
    public void testRunningWorkflow() throws Exception {
        System.setProperty(Config.CONFIGURATION_DIRECTORY_PROPERTY, goodConfigDir.getAbsolutePath());
        Shutdown s = new Shutdown();
        s.main(new String[]{"test"}); 
        
        Config config = new Config();
        try (MQ mq = new MQ(config.getMqSettings())) {
            MqResponse response = mq.receiveMessageFromQueue(config.getMqSettings().getPreservationDestination());
            Assert.assertNotNull(response.getMessageType());
            Assert.assertEquals(response.getMessageType(), MQ.SHUTDOWN_MESSAGE_TYPE);
        }
    }
}
