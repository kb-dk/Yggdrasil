package dk.kb.yggdrasil.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

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
        ArgumentCheck.checkNotNullOrEmpty("String", "Null byte array");        
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testStringNull() {
        ArgumentCheck.checkNotNullOrEmpty((String) null, "Null String");
    }

    @Test(expected = ArgumentCheck.class)
    public void testStringEmpty() {
            ArgumentCheck.checkNotNullOrEmpty("", "Empty String");
    }

    @Test
    public void testByteArray() {
        ArgumentCheck.checkNotNullOrEmpty(new byte[] {0,1}, "Null byte array");

    }
    
    @Test(expected = ArgumentCheck.class)
    public void testByteArrayNull() {
        ArgumentCheck.checkNotNullOrEmpty((byte[]) null, "Null byte array");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testByteArrayEmpty() {
        ArgumentCheck.checkNotNullOrEmpty(new byte[0], "Empty byte array");
    }

    @Test
    public void testObject() {
        ArgumentCheck.checkNotNull((Object) new Object(), "Null object");
    }

    @Test(expected = ArgumentCheck.class)
    public void testObjectNull() {
        ArgumentCheck.checkNotNull((Object) null, "Null object");
    }
    
    @Test
    public void testNotNegativeIntOne() {
        ArgumentCheck.checkNotNegativeInt(1, "1 not negative int");
    }
    
    @Test
    public void testNotNegativeIntZero() {
        ArgumentCheck.checkNotNegativeInt(0, "0 not negative int");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testNotNegativeIntMinusOne() {
        ArgumentCheck.checkNotNegativeInt(-1, "-1 not negative int");
    }
    
    @Test
    public void testNotNegativeLongOne() {
        ArgumentCheck.checkNotNegativeLong(1L, "1 not negative long");
    }
    
    @Test
    public void testNotNegativeLongZero() {
        ArgumentCheck.checkNotNegativeLong(0L, "0 not negative long");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testNotNegativeLongMinusOne() {
        ArgumentCheck.checkNotNegativeLong(-1L, "-1 not negative long");
    }
    
    @Test
    public void testPositiveIntOne() {
        ArgumentCheck.checkPositiveInt(1, "1 positive int");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testPositiveIntZero() {
        ArgumentCheck.checkPositiveInt(0, "0 positive int");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testPositiveIntMinusOne() {
        ArgumentCheck.checkPositiveInt(-1, "-1 positive int");
    }

    @Test
    public void testPositiveLongOne() {
        ArgumentCheck.checkPositiveLong(1L, "1 positive long");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testPositiveLongZero() {
        ArgumentCheck.checkPositiveLong(0L, "0 positive long");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testPositiveLongMinusOne() {
        ArgumentCheck.checkPositiveLong(-1L, "-1 positive long");
    }
    
    @Test
    public void testCollection() {
        ArgumentCheck.checkNotNullOrEmptyCollection(Arrays.asList("TEST"), "non-empty array");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testCollectionNull() {
        ArgumentCheck.checkNotNullOrEmptyCollection((Collection<Object>) null, "Null collection");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testCollectionEmpty() {
        ArgumentCheck.checkNotNullOrEmptyCollection(Arrays.asList(), "Empty array");
    }
    
    @Test
    public void testTrue() {
        ArgumentCheck.checkTrue(true, "true");
    }
    
    @Test(expected = ArgumentCheck.class)
    public void testTrueWhenFalse() {
        ArgumentCheck.checkTrue(false, "false");
    }

    // TODO files and directories.
}
