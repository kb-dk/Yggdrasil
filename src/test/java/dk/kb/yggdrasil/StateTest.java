package dk.kb.yggdrasil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        State okState = State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        State failState = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        Assert.assertTrue(okState.isOkState());
        Assert.assertFalse(failState.isOkState());
    }
    @Test
    public void testGetFailStates() {
        State okState = State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        State failState = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        Set<State> failstates = State.getFailStates();
        Assert.assertFalse(failstates.contains(okState));
        Assert.assertTrue(failstates.contains(failState));
    }
    
    @Test
    public void testIsValidStateChange() {
        State state1 = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        State state2 = State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        State state3 = State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS;
        // invalid to go to from a failstate to another state
        assertFalse(State.isValidStateChange(state1, state2));
        
        // valid to go to from okstate to the same state
        assertTrue(State.isValidStateChange(state2, state2));
        
        // valid to go to from okstate to a later state
        assertTrue(State.isValidStateChange(state2, state3));
        
        // invalid to go to from a okstate to an earlier state
        assertFalse(State.isValidStateChange(state3, state1));        
    }
    
    @Test
    public void testHasState() {
        State state1 = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        State state2 = State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        assertTrue(state1.hasState(state1));
        assertFalse(state1.hasState(state2));
    }
    
}
