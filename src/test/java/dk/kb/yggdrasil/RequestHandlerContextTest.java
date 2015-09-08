package dk.kb.yggdrasil;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;

@RunWith(JUnit4.class)
public class RequestHandlerContextTest {

    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    protected static Bitrepository bitrepository;
    protected static Config config;
    protected static StateDatabase stateDatabase;
    protected static RemotePreservationStateUpdater updater;
    protected static HttpCommunication httpCommunication;

    @BeforeClass
    public static void beforeClass() throws Exception {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        bitrepository = Mockito.mock(Bitrepository.class);

        stateDatabase = new StateDatabase(config.getDatabaseDir());
        
        updater = Mockito.mock(RemotePreservationStateUpdater.class);
        
        httpCommunication = new HttpCommunication();
    }

    @Test
    public void fullContext() throws Exception {
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, stateDatabase, updater, httpCommunication);
        
        Assert.assertEquals(context.getBitrepository(), bitrepository);
        Assert.assertEquals(context.getConfig(), config);
        Assert.assertEquals(context.getRemotePreservationStateUpdater(), updater);
        Assert.assertEquals(context.getStateDatabase(), stateDatabase);
        Assert.assertEquals(context.getHttpCommunication(), httpCommunication);
    }
    
    @Test(expected = ArgumentCheck.class)
    public void nullbitrepositoryArgumentTest() throws Exception {
        new RequestHandlerContext(null, config, stateDatabase, updater, httpCommunication);
    }
    
    @Test(expected = ArgumentCheck.class)
    public void nullConfigArgumentTest() throws Exception {
        new RequestHandlerContext(bitrepository, null, stateDatabase, updater, httpCommunication);
    }

    @Test(expected = ArgumentCheck.class)
    public void nullStateDatabaseArgumentTest() throws Exception {
        new RequestHandlerContext(bitrepository, config, null, updater, httpCommunication);
    }

    @Test(expected = ArgumentCheck.class)
    public void nullUpdaterArgumentTest() throws Exception {
        new RequestHandlerContext(bitrepository, config, stateDatabase, null, httpCommunication);
    }

    @Test(expected = ArgumentCheck.class)
    public void nullHttpCommunicationArgumentTest() throws Exception {
        new RequestHandlerContext(bitrepository, config, stateDatabase, updater, null);
    }
}