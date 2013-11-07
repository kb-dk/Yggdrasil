package dk.kb.yggdrasil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Tests for {@link dk.kb.yggdrasil.Bitrepository }
 * Named BitrepositoryTester and not BitrepositoryTest to avoid inclusion in
 * the set of unittests run by Maven.
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
            fail("Should throw ArgumentCheck on missing config file");
        } catch (ArgumentCheck e) {
            // Expected
        } catch (YggdrasilException e) {
            fail("Should not throw YggdrasilException on missing config file");
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
            new Bitrepository(okConfigFile);
        } catch (YggdrasilException e) {
            fail("Should now throw YggdrasilException on bad config file. Reason: " + e);
        }
    }
    
    @Test
    public void testUpload() throws YggdrasilException, IOException {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new Bitrepository(okConfigFile);
        File payloadFile = getFileWithContents("helloworld.txt2", "Hello World".getBytes());
        boolean success = br.uploadFile(payloadFile, "books");
        assertTrue("Should have returned true for success, but failed", success);
        br.existsInCollection(payloadFile.getName(), "books");
        //File fr = br.getFile("helloworld.txt2", "books");
        //byte[] payloadReturned = getPayload(fr);
        //br.shutdown();
    }
    
    private byte[] getPayload(File fr) throws IOException {
        InputStream is = new FileInputStream(fr);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        is.close();
        baos.close();
        return baos.toByteArray();
    }
    
    @Test
    public void testGetFile() throws Exception {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new Bitrepository(okConfigFile);
        File fr = br.getFile("helloworld.txt", "books");
        byte[] payloadReturned = getPayload(fr);
        String helloWorldReturned = new String(payloadReturned, "UTF-8");
        assertEquals("Hello World", helloWorldReturned);
        //br.shutdown();
    }
    
    @Test
    public void testGetChecksums() throws YggdrasilException, IOException {
        
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new Bitrepository(okConfigFile);
        String packageId = "helloworld.txt2";
        String collection = "books";
        assertTrue("package '" +  packageId + "' should already exist but didn't", 
                br.existsInCollection(packageId, collection));
        br.getChecksums(null, "books");
    }
    
    @Ignore
    @Test
    public void testGetFileIds() throws YggdrasilException {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new Bitrepository(okConfigFile);
        //br.shutdown();
    }
    
    private File getFileWithContents(String packageId, byte[] payload) throws IOException {
        File tempDir = new File("tempDir");
        if (tempDir.isFile()) {
            fail("please remove file '" + tempDir.getAbsolutePath() + "'.");
        }
        File fr = new File(packageId);
        // Remove file if it exists
        if (fr.exists()) {
            fr.delete();
        }
        if (fr.exists()) {
            fail("please remove file '" + fr.getAbsolutePath() + "'.");
        }
        OutputStream ous = new FileOutputStream(fr);
        ous.write(payload);
        ous.close();
        
        return fr;
    }
    
    /*
    public List<String> getFileIdsInCollection(String collectionID) {
        ArgumentCheck.checkNotNullOrEmpty(collectionID, "String collectionId");
        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
        output.debug("Instantiation GetFileID outputFormatter.");
       bitMagGetFileIDsClient.
        
        
        GetFileIDsOutputFormatter outputFormatter = new GetFileIDsInfoFormatter(output);
        
        long timeout = getClientTimeout(bitmagSettings);

        output.debug("Instantiation GetFileID paging client.");
        PagingGetFileIDsClient pagingClient = new PagingGetFileIDsClient(
                bitMagGetFileIDsClient, timeout, outputFormatter, output);
        Boolean success = pagingClient.getFileIDs(collectionID, null, 
                getCollectionPillars(collectionID));
        return success; 
    }
    */
    
    
    
}