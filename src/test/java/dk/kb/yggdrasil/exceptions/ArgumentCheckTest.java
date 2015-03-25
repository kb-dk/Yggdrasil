package dk.kb.yggdrasil.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of the ArgumentCheck class.
 */
@RunWith(JUnit4.class)
public class ArgumentCheckTest {

    @Test
    public void testWithoutEmbeddedException() {
        String message = "reason";
        ArgumentCheck e = new ArgumentCheck(message);
        assertEquals(message, e.getMessage());
        assertNull(e.getCause());
    }
    
    @Test
    public void testWithEmbeddedException() {
        String message = "reason";
        Exception embeddedException = new YggdrasilException("This is apparently an YggdrasilException");
        ArgumentCheck e = new ArgumentCheck(message, embeddedException);
        assertEquals(message, e.getMessage());
        assertEquals(embeddedException, e.getCause());
    }
    
    @Test
    public void testString() {
        try {
            ArgumentCheck.checkNotNullOrEmpty((String) null, "Null String");
            fail("Should trow an exception");
        } catch (ArgumentCheck e) {
            // Expected
        }
        
        try {
            ArgumentCheck.checkNotNullOrEmpty("", "Empty String");
            fail("Should trow an exception");
        } catch (ArgumentCheck e) {
            // Expected
        }

        ArgumentCheck.checkNotNullOrEmpty("String", "Null byte array");        
    }
    
    @Test
    public void testByteArray() {
        try {
            ArgumentCheck.checkNotNullOrEmpty((byte[]) null, "Null byte array");
            fail("Should trow an exception");
        } catch (ArgumentCheck e) {
            // Expected
        }
        
        try {
            ArgumentCheck.checkNotNullOrEmpty(new byte[0], "Empty byte array");
            fail("Should trow an exception");
        } catch (ArgumentCheck e) {
            // Expected
        }

        ArgumentCheck.checkNotNullOrEmpty(new byte[] {0,1}, "Null byte array");
    }
}
