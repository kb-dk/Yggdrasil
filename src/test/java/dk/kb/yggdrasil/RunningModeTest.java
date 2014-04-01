package dk.kb.yggdrasil;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    public void testGetModeWithBadProperty() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "");
        assertTrue(RunningMode.getMode() == RunningMode.DEVELOPMENT);
    }
}
