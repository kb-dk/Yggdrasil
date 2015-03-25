package dk.kb.yggdrasil.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.State;

/**
 * Tests of the PreservationException class.
 */
@RunWith(JUnit4.class)
public class PreservationExceptionTest {

    @Test
    public void testWithoutEmbeddedException() {
        String message = "reason";
        State state = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        PreservationException e = new PreservationException(state, message);
        assertEquals(message, e.getMessage());
        assertEquals(state, e.getState());
        assertNull(e.getCause());
    }
    
    @Test
    public void testWithEmbeddedException() {
        String message = "reason";
        State state = State.PRESERVATION_METADATA_PACKAGED_FAILURE;
        Exception embeddedException = new YggdrasilException("This is apparently an YggdrasilException");
        PreservationException e = new PreservationException(state, message, embeddedException);
        assertEquals(message, e.getMessage());
        assertEquals(state, e.getState());
        assertEquals(embeddedException, e.getCause());
    }
}
