package dk.kb.yggdrasil.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;

import org.junit.Assert;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.Config;

/**
 * Tests for the methods in the RunState class.
 *
 */
@RunWith(JUnit4.class)
public class RunStateTest {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(RunState.class.getName());
    
    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    @Test
    public void testReadRunState() throws Exception {

        RunState runnableRunState = new RunState();
        Thread runstate = new Thread(runnableRunState);
        runstate.start();
        
        // Get host name where the service should run.
        HostName hostname = new HostName();
        String hn = hostname.getHostName();

        // Set port
        Config config = new Config(generalConfigFile);
        int MONITOR_PORT = config.getMonitorPort();
        
        logger.info("RunStateTest: " + hn + ":" + MONITOR_PORT);
        
        Socket socket = new Socket(hn, MONITOR_PORT);
        
        // Read run state form endpoint
        BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Assert.assertNotNull(rd);
        String runStateText = rd.readLine();
        Assert.assertThat(runStateText, CoreMatchers.containsString("Yggdrasil"));

        rd.close();
    }
}
