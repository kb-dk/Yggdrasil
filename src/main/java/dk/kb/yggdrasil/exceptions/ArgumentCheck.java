package dk.kb.yggdrasil.exceptions;

import java.io.File;
import java.util.Collection;

/**
 * Checks for argument validity.
 */
public class ArgumentCheck extends RuntimeException {
    
    /**
     * Constructs new ArgumentCheck with the specified detail message.
     *
     * @param message The detail message
     */
    public ArgumentCheck(String message) {
        super(message);
    }

    /**
     * Constructs new ArgumentCheck with the specified detail
     * message and cause.
     *
     * @param message The detail message
     * @param cause   The cause
     */
    public ArgumentCheck(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Check if a String argument is null or the empty string.
     *
     * @param val  the value to check
     * @param name the name and type of the value being checked
     * @throws ArgumentCheck if test fails
     */
    public static void checkNotNullOrEmpty(String val, String name) {
        checkNotNull(val, name);

        if (val.isEmpty()) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must not be an empty string.");
        }
    }

    /**
     * Check if an Object argument is null.
     *
     * @param val  the value to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkNotNull(Object val, String name) {
        if (val == null) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must not be null.");
        }
    }

    /**
     * Check if an int argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkNotNegativeInt(int num, String name) {
        if (num < 0) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must be non-negative, but is " + num + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkNotNegativeLong(long num, String name) {
        if (num < 0) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must be non-negative, but is " + num + ".");
        }
    }

    /**
     * Check if an int argument is less than or equal to 0.
     *
     * @param num  argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkPositiveInt(int num, String name) {
        if (num <= 0) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a long argument is less than 0.
     *
     * @param num argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkPositiveLong(long num, String name) {
        if (num <= 0) {
            throw new ArgumentCheck("The value of the variable '" + name
                    + "' must be positive, but is " + num + ".");
        }
    }

    /**
     * Check if a List argument is not null and the list is not empty.
     *
     * @param c argument to check
     * @param name the name and type of the value being checked.
     * @throws ArgumentCheck if test fails
     */
    public static void checkNotNullOrEmptyCollection(Collection<?> c, String name) {
        checkNotNull(c, name);
        if (c.isEmpty()) {
            throw new ArgumentCheck("The contents of the variable '" + name
                        + "' must not be empty.");
        }
    }

    /**
     * Check that some condition on input parameters is true and throw an
     * ArgumentNotValid if it is false.
     * @param b the condition to check
     * @param s the error message to be reported
     * @throws ArgumentCheck if b is false
     */
    public static void checkTrue(boolean b, String s) {
        if (!b) {
            throw new ArgumentCheck(s);
        }
    }
    
    /**
     * Check, if the given argument is an existing directory.
     * @param aDir a given File object.
     * @param name Name of object
     * @throws ArgumentCheck If aDir is not an existing directory 
     */
    public static void checkExistsDirectory(File aDir, String name) {
        checkNotNull(aDir, name);
        if (!aDir.isDirectory()) {
            String message = "The file '" + aDir.getAbsolutePath()
                             + "' does not exist or is not a directory.";
            throw new ArgumentCheck(message);
        }
    }
    
    /**
     * Check, if the given argument is an existing normal file.
     * @param aFile a given File object.
     * @param name Name of object
     * @throws ArgumentCheck If aDir is not an existing file 
     */
    public static void checkExistsNormalFile(File aFile, String name) {
        checkNotNull(aFile, name);
        if (!aFile.isFile()) {
            String message = "The file '" + aFile.getAbsolutePath()
                             + "' does not exist or is not a normal file.";
            throw new ArgumentCheck(message);
        }
    }
}
