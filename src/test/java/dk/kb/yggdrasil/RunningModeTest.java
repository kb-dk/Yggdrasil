package dk.kb.yggdrasil;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.config.RunningMode;

@RunWith(JUnit4.class)
public class RunningModeTest extends TestCase {

    public String savedRunningModeProperty;

    @Override
    public void setUp() {
        savedRunningModeProperty = System.getProperty(RunningMode.RUNNINGMODE_PROPERTY);
    }

    @Override
    public void tearDown() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, savedRunningModeProperty);
    }

    @Test
    public void testGetModeWithEmptyProperty() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "");
        assertTrue(RunningMode.getMode() == RunningMode.DEVELOPMENT);
    }
    
    @Test
    public void testGetModeWithWhiteSpaceProperty() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "   ");
        assertTrue(RunningMode.getMode() == RunningMode.DEVELOPMENT);
    }

    @Test
    public void testGetModeWithNullProperty() {
        System.clearProperty(RunningMode.RUNNINGMODE_PROPERTY);
        assertTrue(RunningMode.getMode() == RunningMode.DEVELOPMENT);
    }
    
    @Test
    public void testGetModeFromText() {
        for(RunningMode r : RunningMode.values()) {
            RunningMode extracted = RunningMode.valueOf(r.name());
            assertEquals(extracted, r);
        }
    }

}
