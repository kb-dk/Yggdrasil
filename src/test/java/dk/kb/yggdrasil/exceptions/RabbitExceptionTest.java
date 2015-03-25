package dk.kb.yggdrasil.exceptions;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of the RabbitException class.
 */
@RunWith(JUnit4.class)
public class RabbitExceptionTest {

    @Test
    public void testWithoutEmbeddedException() {
        String message = "reason";
        RabbitException e = new RabbitException(message);
        assertEquals(message, e.getMessage());
    }

    @Test
    public void testWithEmbeddedException() {
        String message = "reason";
        String exceptionMessage = "Some error occurred";
        Exception e = new IOException(exceptionMessage);
        RabbitException e1 = new RabbitException(message, e);
        assertEquals(message, e1.getMessage());
        assertEquals(exceptionMessage, e1.getCause().getMessage());
    }
}
