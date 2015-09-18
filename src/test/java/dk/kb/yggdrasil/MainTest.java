package dk.kb.yggdrasil;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Config;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.utils.TravisUtils;

/**
 * Tests for {@link dk.kb.yggdrasil.Main }
 */
@RunWith(JUnit4.class)
public class MainTest {

    File goodConfigDir = new File("src/test/resources/config");

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");
    }
    
    @Test
    public void testMainMethodWithGoodConfigDir() throws Exception {
        if (TravisUtils.runningOnTravis()) {
            return;
        }
        System.setProperty(Config.CONFIGURATION_DIRECTORY_PROPERTY, goodConfigDir.getAbsolutePath());
        YggdrasilConfig c = new YggdrasilConfig(new File(goodConfigDir, Config.YGGDRASIL_CONF_FILENAME));
        FileUtils.deleteDirectory(c.getDatabaseDir());
        Main.main(new String[]{"test"});
    }
    
    @Test(expected = YggdrasilException.class)
    public void testMainMethodWithBadConfigDir() throws Exception {
        String userHome = System.getProperty("user.home");
        File badConfigDir = new File(userHome + "/configconfig");
        Assert.assertFalse(badConfigDir.exists());
        System.setProperty(Config.CONFIGURATION_DIRECTORY_PROPERTY, badConfigDir.getAbsolutePath());
        Main.main(new String[]{});
    }
    
    @Test
    public void testRunningWorkflow() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        Main m = new Main(stateDatabase, bitrepository);
        
        System.setProperty(Config.CONFIGURATION_DIRECTORY_PROPERTY, goodConfigDir.getAbsolutePath());
        Config config = new Config();
        
        MQ mq = new MQ(config.getMqSettings());
        mq.publishOnQueue(config.getMqSettings().getPreservationDestination(), "Please terminate Yggdrasil".getBytes(), MQ.SHUTDOWN_MESSAGE_TYPE);
        mq.close();
                
        m.runWorkflow(config, httpCommunication);
    }
}
