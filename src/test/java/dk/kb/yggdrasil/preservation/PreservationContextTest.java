package dk.kb.yggdrasil.preservation;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;

@RunWith(JUnit4.class)
public class PreservationContextTest {

    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    protected static Bitrepository bitrepository;
    protected static Config config;
    protected static StateDatabase stateDatabase;
    protected static RemotePreservationStateUpdater updater;

    @BeforeClass
    public static void beforeClass() throws Exception {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        bitrepository = Mockito.mock(Bitrepository.class);

        stateDatabase = new StateDatabase(config.getDatabaseDir());
        
        updater = Mockito.mock(RemotePreservationStateUpdater.class);
    }

    @Test
    public void fullContext() throws Exception {
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        
        Assert.assertEquals(context.getBitrepository(), bitrepository);
        Assert.assertEquals(context.getConfig(), config);
        Assert.assertEquals(context.getRemotePreservationStateUpdater(), updater);
        Assert.assertEquals(context.getStateDatabase(), stateDatabase);
    }
    
    @Test
    public void nullbitrepositoryArgumentTest() throws Exception {
        try {
            new PreservationContext(null, config, stateDatabase, updater);
            Assert.fail("Must throw an ArgumentCheck exception here.");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }
    
    @Test
    public void nullConfigArgumentTest() throws Exception {
        try {
            new PreservationContext(bitrepository, null, stateDatabase, updater);
            Assert.fail("Must throw an ArgumentCheck exception here.");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void nullStateDatabaseArgumentTest() throws Exception {
        try {
            new PreservationContext(bitrepository, config, null, updater);
            Assert.fail("Must throw an ArgumentCheck exception here.");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void nullUpdaterArgumentTest() throws Exception {
        try {
            new PreservationContext(bitrepository, config, stateDatabase, null);
            Assert.fail("Must throw an ArgumentCheck exception here.");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

}
