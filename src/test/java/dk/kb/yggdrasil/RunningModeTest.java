package dk.kb.yggdrasil;

import static org.junit.Assert.fail;
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
        System.out.println("mode: " + savedRunningModeProperty);
    }

    @Override
    public void tearDown() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, savedRunningModeProperty);
        System.out.println("mode: " + RunningMode.getMode());
    }

    @Test
    public void testGetModeWithBadProperty() {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "");
        assertTrue(RunningMode.getMode() == RunningMode.DEVELOPMENT);
    }

}
