package dk.kb.yggdrasil.exceptions;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** 
 * Tests of the YggdrasilException class. 
 */
@RunWith(JUnit4.class)
public class YggdrasilExceptionTest {

    @Test
    public void testConstructor1() {
        String message = "reason";
        YggdrasilException e = new YggdrasilException(message);
        assertEquals(message, e.getMessage());
    }
    
    @Test
    public void testConstructor2() {
        String message = "reason";
        String exceptionMessage = "Some error occurred";
        Exception e = new IOException(exceptionMessage);
        YggdrasilException e1 = new YggdrasilException(message, e);
        assertEquals(message, e1.getMessage());
        assertEquals(exceptionMessage, e1.getCause().getMessage());
    }

    @Test
    public void testConstructor1WithNullArgs() {
        String message = null;
        YggdrasilException e = new YggdrasilException(message);
        assertTrue(e.getMessage() == null);
        
        Exception anException = null;
        e = new YggdrasilException(message, anException);
        assertTrue(e.getMessage() == null);
        assertTrue(e.getCause() == null);
    } 
}
