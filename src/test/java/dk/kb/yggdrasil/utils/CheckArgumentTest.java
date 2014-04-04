package dk.kb.yggdrasil.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;

/**
 * Tests for the static methods in the CheckArgument class.
 */
@RunWith(JUnit4.class)
public class CheckArgumentTest {

    static File NORMAL_FILE_THAT_EXISTS = new File("src/test/resources/config/rabbitmq.yml");
    static File NORMAL_FILE_THAT_DOES_NOT_EXIST = new File("src/test/resources/config/rabbitmq.yml2");
    static File FILE_IS_A_DIRECTORY = new File("src/test/resources/config/");
    static File FILE_IS_A_NON_EXISTING_DIRECTORY = new File("src/test/resources/config2/");

    @Test
    public void testConstructor() {
        String message = "Bad argument";
        ArgumentCheck e = new ArgumentCheck(message);
        assertEquals(message, e.getMessage());
        String errorMessage = "IO error";
        Exception embeddedException = new IOException(errorMessage);
        e = new ArgumentCheck(message, embeddedException);
        assertEquals(message, e.getMessage());
        assertEquals(errorMessage, e.getCause().getMessage());
    }

    @Test
    public void testNotNull() {
        String notNullString = "NotNull";
        String nullString = null;
        try {
            ArgumentCheck.checkNotNull(notNullString, "String notNullString");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
        try {
            ArgumentCheck.checkNotNull(nullString, "String nullString");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void testNotNullOrEmpty() {
        String emptyString = "";
        String notNullString = "NotNull";
        String nullString = null;
        try {
            ArgumentCheck.checkNotNullOrEmpty(notNullString, "String notNullString");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
        try {
            ArgumentCheck.checkNotNullOrEmpty(nullString, "String nullString");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkNotNullOrEmpty(emptyString, "String emptyString");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void testExistsNormalFile() {
        assertTrue(NORMAL_FILE_THAT_EXISTS.exists());
        assertFalse(NORMAL_FILE_THAT_DOES_NOT_EXIST.exists());
        assertTrue(FILE_IS_A_DIRECTORY.isDirectory());
        File nullFile = null;

        try {
            ArgumentCheck.checkExistsNormalFile(NORMAL_FILE_THAT_EXISTS, "File NORMAL_FILE_THAT_EXISTS");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
        try {
            ArgumentCheck.checkExistsNormalFile(nullFile, "String nullFile");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkExistsNormalFile(NORMAL_FILE_THAT_DOES_NOT_EXIST,
                    "String NORMAL_FILE_THAT_DOES_NOT_EXIST");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkExistsNormalFile(FILE_IS_A_DIRECTORY, "File FILE_IS_A_DIRECTORY");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void testExistsDirectory() {
        assertTrue(NORMAL_FILE_THAT_EXISTS.exists());
        assertFalse(FILE_IS_A_NON_EXISTING_DIRECTORY.exists());
        assertTrue(FILE_IS_A_DIRECTORY.isDirectory());
        File nullDirectory = null;

        try {
            ArgumentCheck.checkExistsDirectory(FILE_IS_A_DIRECTORY, "File FILE_IS_A_DIRECTORY");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
        try {
            ArgumentCheck.checkExistsDirectory(nullDirectory, "String nullDirectory");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkExistsDirectory(FILE_IS_A_NON_EXISTING_DIRECTORY,
                    "String FILE_IS_A_NON_EXISTING_DIRECTORY");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkExistsDirectory(NORMAL_FILE_THAT_EXISTS, "File NORMAL_FILE_THAT_EXISTS");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }
    }

    @Test
    public void testCheckPositiveIntAndLongs() {
        int zeroInt = 0;
        int positiveInt = 3;
        int negativeInt = -3;
        long zeroLong = 0L;
        long positiveLong = 3L;
        long negativeLong = -3L;
        // check that positive numbers (> 0) are accepted
        try {
            ArgumentCheck.checkPositiveInt(positiveInt, "int positiveInt");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        try {
            ArgumentCheck.checkPositiveLong(positiveLong, "long positiveLong");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        // check the value zero is not accepted
        try {
            ArgumentCheck.checkPositiveInt(zeroInt, "int zeroInt");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkPositiveLong(zeroLong, "long zeroLong");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // expected
        }

        // check the values < 0 are not accepted
        try {
            ArgumentCheck.checkPositiveInt(negativeInt, "int negativeInt");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkPositiveLong(negativeLong, "long negativeLong");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // expected
        }
    }

    @Test
    public void testCheckNotNegativeIntAndLongs() {
        int zeroInt = 0;
        int positiveInt = 3;
        int negativeInt = -3;
        long zeroLong = 0L;
        long positiveLong = 3L;
        long negativeLong = -3L;

        // check that positive numbers (> 0) are accepted
        try {
            ArgumentCheck.checkNotNegativeInt(positiveInt, "int positiveInt");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        try {
            ArgumentCheck.checkNotNegativeLong(positiveLong, "long positiveLong");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        // check the value zero is accepted
        try {
            ArgumentCheck.checkNotNegativeInt(zeroInt, "int zeroInt");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        try {
            ArgumentCheck.checkNotNegativeLong(zeroLong, "long zeroLong");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }

        // check the values < 0 are not accepted
        try {
            ArgumentCheck.checkNotNegativeInt(negativeInt, "int negativeInt");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        try {
            ArgumentCheck.checkNotNegativeLong(negativeLong, "long negativeLong");
            fail("Should now throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // expected
        }
    }

    @Test
    public void testCheckTrue() {
        boolean falsevalue = false;
        boolean truevalue = true;

        try {
            ArgumentCheck.checkTrue(falsevalue, "boolean falsevalue");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // expected
        }

        try {
            ArgumentCheck.checkTrue(truevalue, "boolean truevalue");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
    }

    @Test
    public void testCheckNotNullOrEmptyCollection() {
        Collection<Integer> nullCollection = null;
        Collection<Integer> emptyCollection = new HashSet<Integer>();
        Collection<Integer> nonEmptyCollection = new HashSet<Integer>();
        nonEmptyCollection.add(new Integer(2));

        // Test null collection
        try {
            ArgumentCheck.checkNotNullOrEmptyCollection(nullCollection, "Collection<Integer> nullCollection");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        // Test empty collection
        try {
            ArgumentCheck.checkNotNullOrEmptyCollection(emptyCollection, "Collection<Integer> emptyCollection");
            fail("Should throw ArgumentCheck Exception, but didn't");
        } catch (ArgumentCheck e) {
            // Expected
        }

        // Test non-empty collection
        try {
            ArgumentCheck.checkNotNullOrEmptyCollection(nonEmptyCollection, "Collection<Integer> nonEmptyCollection");
        } catch (ArgumentCheck e) {
            fail("Shouldn't throw ArgumentCheck Exception, but did");
        }
    }

}
