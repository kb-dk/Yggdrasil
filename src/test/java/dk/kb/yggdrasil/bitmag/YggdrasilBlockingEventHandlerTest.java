package dk.kb.yggdrasil.bitmag;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.client.eventhandler.ContributorCompleteEvent;
import org.bitrepository.client.eventhandler.ContributorFailedEvent;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.SettingsUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Tests for {@link dk.kb.yggdrasil.Bitrepository }
 * Named BitrepositoryTester and not BitrepositoryTest to avoid inclusion in
 * the set of unittests run by Maven.
 */
@RunWith(JUnit4.class)
public class YggdrasilBlockingEventHandlerTest {
    public static String TEST_SETTINGS_DIR = "config/bitmag-test-settings";

    private static Settings settings;
    private static String COLLECTION_ID_1;
    private static String PILLAR_1;
    private static String PILLAR_2;

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");
        SettingsProvider settingsLoader = new SettingsProvider(
                new XMLFileSettingsLoader(TEST_SETTINGS_DIR),
                YggdrasilBlockingEventHandlerTest.class.getName() + "-TEST");
        settings = settingsLoader.getSettings();
        SettingsUtils.initialize(settings);

        COLLECTION_ID_1 = settings.getCollections().get(0).getID();
        PILLAR_1 = SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).get(0);
        PILLAR_2 = SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).get(1);
    }

    @Test
    public void testBeforeEvents() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 0);
        assertEquals(0, ybeh.getResults().size());
        assertEquals(0, ybeh.getFailures().size());
        assertFalse("The operation should be considered a failure, before any events", ybeh.hasFailed());
    }

    @Test
    public void testOnlySuccess() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 0);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            ybeh.handleEvent(new ContributorCompleteEvent(pillarId, COLLECTION_ID_1));
        }
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size(), ybeh.getResults().size());
        assertEquals(0, ybeh.getFailures().size());
        assertFalse("The operation should not be considered a failure, when every pillar has succeeded", ybeh.hasFailed());
    }

    @Test
    public void testOnlyFailures() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 0);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            ybeh.handleEvent(new ContributorFailedEvent(pillarId, COLLECTION_ID_1, ResponseCode.FAILURE));
        }
        assertEquals(0, ybeh.getResults().size());
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size(), ybeh.getFailures().size());
        assertTrue("The operation should be considered a failure, when every pillar has failed", ybeh.hasFailed());
    }

    @Test
    public void testOneFailureWhenNoneAllowed() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 0);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            if(pillarId.equals(PILLAR_1)) {
                ybeh.handleEvent(new ContributorFailedEvent(pillarId, COLLECTION_ID_1, ResponseCode.FAILURE));
            } else {
                ybeh.handleEvent(new ContributorCompleteEvent(pillarId, COLLECTION_ID_1));
            }
        }
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size()-1, ybeh.getResults().size());
        assertEquals(1, ybeh.getFailures().size());
        assertTrue("The operation should be considered a failure", ybeh.hasFailed());
    }

    @Test
    public void testOneFailureWhenOneAllowed() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 1);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            if(pillarId.equals(PILLAR_1)) {
                ybeh.handleEvent(new ContributorFailedEvent(pillarId, COLLECTION_ID_1, ResponseCode.FAILURE));
            } else {
                ybeh.handleEvent(new ContributorCompleteEvent(pillarId, COLLECTION_ID_1));
            }
        }
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size()-1, ybeh.getResults().size());
        assertEquals(1, ybeh.getFailures().size());
        assertFalse("The operation should not be considered a failure", ybeh.hasFailed());
    }
    
    @Test
    public void testOneFailureWhenTwoAllowed() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 2);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            if(pillarId.equals(PILLAR_1)) {
                ybeh.handleEvent(new ContributorFailedEvent(pillarId, COLLECTION_ID_1, ResponseCode.FAILURE));
            } else {
                ybeh.handleEvent(new ContributorCompleteEvent(pillarId, COLLECTION_ID_1));
            }
        }
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size()-1, ybeh.getResults().size());
        assertEquals(1, ybeh.getFailures().size());
        assertFalse("The operation should not be considered a failure", ybeh.hasFailed());
    }
    
    @Test
    public void testTwoFailuresWhenOneAllowed() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 1);
        for(String pillarId : SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1)) {
            if(pillarId.equals(PILLAR_1) || pillarId.equals(PILLAR_2)) {
                ybeh.handleEvent(new ContributorFailedEvent(pillarId, COLLECTION_ID_1, ResponseCode.FAILURE));
            } else {
                ybeh.handleEvent(new ContributorCompleteEvent(pillarId, COLLECTION_ID_1));
            }
        }
        assertEquals(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).size()-2, ybeh.getResults().size());
        assertEquals(2, ybeh.getFailures().size());
        assertTrue("The operation should be considered a failure", ybeh.hasFailed());
    }
    
    @Test
    public void testOnlyOneFailure() {
        YggdrasilBlockingEventHandler ybeh = new YggdrasilBlockingEventHandler(COLLECTION_ID_1, 0);
        ybeh.handleEvent(new ContributorFailedEvent(SettingsUtils.getPillarIDsForCollection(COLLECTION_ID_1).get(0), COLLECTION_ID_1, ResponseCode.FAILURE));
        assertEquals(0, ybeh.getResults().size());
        assertTrue("The operation should be considered a failure, when every pillar has failed", ybeh.hasFailed());
    }
}
