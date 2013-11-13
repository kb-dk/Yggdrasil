package dk.kb.yggdrasil;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit-tests for the State enum class. */
@RunWith(JUnit4.class)
public class StateTest {

    @Test
    public void testIsOkState() {
        State okState = State.PRESERVATION_METADATA_DOWNLOAD_SUCCESS;
        State failState = State.PRESERVATION_METADATA_DOWNLOAD_FAILURE;
        Assert.assertTrue(State.isOkState(okState));
        Assert.assertFalse(State.isOkState(failState));
    }

    public void testGetFailStates() {
        State okState = State.PRESERVATION_METADATA_DOWNLOAD_SUCCESS;
        State failState = State.PRESERVATION_METADATA_DOWNLOAD_FAILURE;
        Set<State> failstates = State.getFailStates();
        Assert.assertFalse(failstates.contains(okState));
        Assert.assertTrue(failstates.contains(failState));
    }

}
