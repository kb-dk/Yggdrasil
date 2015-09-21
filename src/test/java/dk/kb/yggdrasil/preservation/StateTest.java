package dk.kb.yggdrasil.preservation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.preservation.PreservationState;

/** Unit-tests for the State enum class. */
@RunWith(JUnit4.class)
public class StateTest {

    @Test
    public void testIsOkState() {
        PreservationState okState = PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        PreservationState failState = PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE;
        Assert.assertTrue(okState.isOkState());
        Assert.assertFalse(failState.isOkState());
    }
    @Test
    public void testGetFailStates() {
        PreservationState okState = PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        PreservationState failState = PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE;
        Set<PreservationState> failstates = PreservationState.getFailStates();
        Assert.assertFalse(failstates.contains(okState));
        Assert.assertTrue(failstates.contains(failState));
    }
    
    @Test
    public void testIsValidStateChange() {
        PreservationState state1 = PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE;
        PreservationState state2 = PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        PreservationState state3 = PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS;
        // invalid to go to from a failstate to another state
        assertFalse(PreservationState.isValidStateChange(state1, state2));
        
        // valid to go to from okstate to the same state
        assertTrue(PreservationState.isValidStateChange(state2, state2));
        
        // valid to go to from okstate to a later state
        assertTrue(PreservationState.isValidStateChange(state2, state3));
        
        // invalid to go to from a okstate to an earlier state
        assertFalse(PreservationState.isValidStateChange(state3, state1));        
    }
    
    @Test
    public void testHasState() {
        PreservationState state1 = PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE;
        PreservationState state2 = PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY;
        assertTrue(state1.hasState(state1));
        assertFalse(state1.hasState(state2));
    }
    
}
