package dk.kb.yggdrasil;

import static org.junit.Assert.*;

import java.io.File;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Tests for {@link dk.kb.yggdrasil.Bitrepository }
 * Named BitrepositoryTester and not BitrepositoryTest to avoid inclusion in
 * the set of unittests run by Maven.
 * 
 */
@RunWith(JUnit4.class)
public class BitrepositoryTester {
    
    public static String MISSING_YAML_FILE = "src/test/resources/config/rabbitmq.yaml2";
    public static String INCORRECT_YAML_FILE = "src/test/resources/config/rabbitmq.yml";
    public static String OK_YAML_BITMAG_FILE = "src/test/resources/config/bitmag.yml";
    
    
    
    @Test
    public void testMissingYamlFile() {
        File missingConfigFile = new File(MISSING_YAML_FILE);
        assertFalse(missingConfigFile.exists());
        try {
            new Bitrepository(missingConfigFile);
            fail("Should throw YggdrasilException on missing config file");
        } catch (YggdrasilException e) {
            // Expected
        }
    }
    
    @Test
    public void testIncorrectYamlFile() {
        File badConfigFile = new File(INCORRECT_YAML_FILE);
        assertTrue(badConfigFile.exists());
        try {
            new Bitrepository(badConfigFile);
            fail("Should throw YggdrasilException on bad config file");
        } catch (YggdrasilException e) {
            // Expected
        }
    }
    
    @Test
    public void testOkYamlFile() {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        // Assumes that Yggdrasil/config contains a directory "bitmag-development-settings"
        // containing bitrepository 1.0 settings and with a keyfile named "client-16.pem"
        assertTrue(okConfigFile.exists());
        try {
            Bitrepository br = new Bitrepository(okConfigFile);
        } catch (YggdrasilException e) {
            fail("Should now throw YggdrasilException on bad config file. Reason: " + e);
        }
    }
    
    
    /**
    
    @Test
    public void testUpload() {
        fail("Not yet implemented");
    }
    
    @Test
    public void testGetFile() {
        fail("Not yet implemented");
    }
    
    @Test
    public void testGetChecksum() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetFileIds() {
        fail("Not yet implemented");
    }
    
    */
}
