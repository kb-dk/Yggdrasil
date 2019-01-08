package dk.kb.yggdrasil.bitmag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getchecksums.conversation.ChecksumsCompletePillarEvent;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResultingChecksums;
import org.bitrepository.client.eventhandler.CompleteEvent;
import org.bitrepository.client.eventhandler.ContributorCompleteEvent;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.ContributorFailedEvent;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.OperationFailedEvent;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.FileUtils;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Tests for {@link dk.kb.yggdrasil.bitmag.Bitrepository }
 * Named BitrepositoryTester and not BitrepositoryTest to avoid inclusion in
 * the set of unittests run by Maven.
 */
@RunWith(JUnit4.class)
public class BitrepositoryTest {

    public static String MISSING_YAML_FILE = "src/test/resources/config/rabbitmq.yaml2";
    public static String INCORRECT_YAML_FILE = "src/test/resources/config/rabbitmq.yml";
    public static String OK_YAML_BITMAG_FILE = "src/test/resources/config/bitmag.yml";

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");
    }
    
    @Ignore
    @Test(expected = ArgumentCheck.class)
    public void testMissingYamlFile() throws Exception {
        File missingConfigFile = new File(MISSING_YAML_FILE);
        assertFalse(missingConfigFile.exists());
        new Bitrepository(new BitrepositoryConfig(missingConfigFile));
    }

    @Ignore
    @Test(expected = YggdrasilException.class)
    public void testIncorrectYamlFile() throws Exception {
        File badConfigFile = new File(INCORRECT_YAML_FILE);
        assertTrue(badConfigFile.exists());
        new Bitrepository(new BitrepositoryConfig(badConfigFile));
    }

    @Ignore
    @Test
    public void testOkYamlFile() throws Exception {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        // Assumes that Yggdrasil/config contains a directory "bitmag-development-settings"
        // containing bitrepository 1.0 settings and with a keyfile named "client-16.pem"
        assertTrue(okConfigFile.exists());
        new BitrepositoryTestingAPI(okConfigFile);
    }
    
    // Apparently we cannot mock the PutFileClient.
    @Ignore
    @Test
    public void testUpload() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        PutFileClient mockClient = mock(PutFileClient.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[6];

                List<ContributorEvent> events = new ArrayList<ContributorEvent>();
                for(String pillarID : SettingsUtils.getPillarIDsForCollection(collectionID)) {
                    ContributorEvent ce = new ContributorCompleteEvent(pillarID, collectionID);
                    events.add(ce);
                    eh.handleEvent(ce);
                }
                eh.handleEvent(new CompleteEvent(collectionID, events));
                return null;
            }
        }).when(mockClient).putFile(anyString(), any(URL.class), anyString(), any(), any(), any(), any(EventHandler.class), anyString());
        FileExchange fe = mock(FileExchange.class);
        
        br.setPutFileClient(mockClient);
        
        when(fe.putFile(any(File.class))).thenReturn(new URL("http://localhost:80/dav/test.txt"));
        br.setFileExchange(fe);
        
        String generatedName = "helloworld" + System.currentTimeMillis() + ".txt";
        File payloadFile = getFileWithContents(generatedName, "Hello World".getBytes());
        boolean success = br.uploadFile(payloadFile, "books");
        assertTrue("Should have returned true for success, but failed", success);
        payloadFile.delete();
    }

    @Ignore
    @Test
    public void testUploadOnUnknownCollection() throws YggdrasilException, IOException {
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new BitrepositoryTestingAPI(okConfigFile);
        String generatedName = "helloworld" + System.currentTimeMillis() + ".txt";
        File payloadFile = getFileWithContents(generatedName, "Hello World".getBytes());
        boolean success = br.uploadFile(payloadFile, "cars");
        assertFalse("Shouldn't have returned true for success, but succeeded", success);
        payloadFile.delete();
    }

    @Ignore
    @Test
    public void testGetFileSuccess() throws Exception {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        final String FILE_CONTENT = "Hello World";
        GetFileClient mockClient = mock(GetFileClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];
                eh.handleEvent(new CompleteEvent(collectionID, Arrays.asList()));
                return null;
            }
        }).when(mockClient).getFileFromFastestPillar(anyString(), anyString(), any(), any(), any(), any());
        br.setGetFileClient(mockClient);
        
        // mock file-exchange
        FileExchange fe = mock(FileExchange.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                File out = (File) invocation.getArguments()[0];
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(FILE_CONTENT.getBytes());
                fos.flush();
                fos.close();
                return null;
            }
        }).when(fe).getFile(any(File.class), anyString());
        when(fe.getURL(anyString())).thenReturn(new URL("http://localhost:80/dav/test.txt"));
        br.setFileExchange(fe);
        
        File fr = br.getFile("helloworld.txt", "books", null);
        byte[] payloadReturned = getPayload(fr);
        String helloWorldReturned = new String(payloadReturned, "UTF-8");
        assertEquals(FILE_CONTENT, helloWorldReturned);
        
        verify(fe).getURL(anyString());
        verify(fe).getFile(any(File.class), anyString());
        verifyNoMoreInteractions(fe);

        verify(mockClient).getFileFromFastestPillar(anyString(), anyString(), any(), any(), any(), anyString());
        verifyNoMoreInteractions(mockClient);
        FileUtils.delete(fr);
    }

    @Ignore
    @Test(expected = YggdrasilException.class)
    public void testGetFileFailureOperation() throws Exception {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetFileClient mockClient = mock(GetFileClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];
                eh.handleEvent(new OperationFailedEvent(collectionID, "Is intended to fail", Arrays.asList()));
                return null;
            }
        }).when(mockClient).getFileFromFastestPillar(anyString(), anyString(), any(), any(), any(), any());
        br.setGetFileClient(mockClient);
        
        // mock file-exchange
        FileExchange fe = mock(FileExchange.class);
        when(fe.getURL(anyString())).thenReturn(new URL("http://localhost:80/dav/test.txt"));
        br.setFileExchange(fe);
        
        br.getFile("helloworld.txt", "books", null);
    }
    
    @Ignore
    @Test(expected = YggdrasilException.class)
    public void testGetFileFailuredDownload() throws Exception {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetFileClient mockClient = mock(GetFileClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];
                eh.handleEvent(new CompleteEvent(collectionID, Arrays.asList()));
                return null;
            }
        }).when(mockClient).getFileFromFastestPillar(anyString(), anyString(), any(), any(), any(), any());
        br.setGetFileClient(mockClient);
        
        // mock file-exchange
        FileExchange fe = mock(FileExchange.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IOException("Fail downloading file.");
            }
        }).when(fe).getFile(any(File.class), anyString());
        when(fe.getURL(anyString())).thenReturn(new URL("http://localhost:80/dav/test.txt"));
        br.setFileExchange(fe);
        
        br.getFile("helloworld.txt", "books", null);
    }
    
    @Ignore
    @Test(expected = YggdrasilException.class)
    public void getFileFailureBadURL() throws Exception {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetFileClient mockClient = mock(GetFileClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];
                eh.handleEvent(new CompleteEvent(collectionID, Arrays.asList()));
                return null;
            }
        }).when(mockClient).getFileFromFastestPillar(anyString(), anyString(), any(), any(), any(), any());
        br.setGetFileClient(mockClient);
        
        // mock file-exchange
        FileExchange fe = mock(FileExchange.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IOException("Fail downloading file.");
            }
        }).when(fe).getFile(any(File.class), anyString());
        when(fe.getURL(anyString())).thenReturn(new URL("http://localhost:80/dav/test.txt"));
        br.setFileExchange(fe);
        
        br.getFile("helloworld.txt", "books", null);
    }
    
    @Ignore
    @Test
    public void testGetChecksumsSuccessEmptyResults() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetChecksumsClient mockClient = mock(GetChecksumsClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[5];

                eh.handleEvent(new CompleteEvent(collectionID, Arrays.asList()));
                return null;
            }
        }).when(mockClient).getChecksums(anyString(), any(), anyString(), any(ChecksumSpecTYPE.class), any(), any(EventHandler.class), anyString());
        br.setGetChecksumsClient(mockClient);

        Map<String, ChecksumsCompletePillarEvent> res = br.getChecksums(fileid, "books");
        
        verify(mockClient).getChecksums(anyString(), any(), anyString(), any(), any(), any(EventHandler.class), anyString());
        verifyNoMoreInteractions(mockClient);

        assertTrue("Should be empty.", res.isEmpty());
    }

    @Ignore
    @Test
    public void testGetChecksumsSuccessFullResults() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        String collectionID = "books";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetChecksumsClient mockClient = mock(GetChecksumsClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                ChecksumSpecTYPE checksumType = (ChecksumSpecTYPE) invocation.getArguments()[4];
                EventHandler eh = (EventHandler) invocation.getArguments()[5];
                ResultingChecksums resCs = new ResultingChecksums();
                ChecksumDataForChecksumSpecTYPE csData = new ChecksumDataForChecksumSpecTYPE();
                csData.setCalculationTimestamp(CalendarUtils.getNow());
                csData.setChecksumValue("checksum".getBytes());
                csData.setFileID(collectionID);
                resCs.getChecksumDataItems().add(csData);

                List<ContributorEvent> events = new ArrayList<ContributorEvent>();
                for(String pillarID : SettingsUtils.getPillarIDsForCollection(collectionID)) {
                    ContributorEvent ce = new ChecksumsCompletePillarEvent(pillarID, collectionID, resCs, checksumType, false);
                    events.add(ce);
                    eh.handleEvent(ce);
                }
                eh.handleEvent(new CompleteEvent(collectionID, events));

                return null;
            }
        }).when(mockClient).getChecksums(anyString(), any(), anyString(), any(ChecksumSpecTYPE.class), any(), any(EventHandler.class), anyString());
        br.setGetChecksumsClient(mockClient);

        Map<String, ChecksumsCompletePillarEvent> res = br.getChecksums(fileid, collectionID);
        
        verify(mockClient).getChecksums(anyString(), any(), anyString(), any(), any(), any(EventHandler.class), anyString());
        verifyNoMoreInteractions(mockClient);

        assertFalse("Should not be empty.", res.isEmpty());
        for(String pillarID : SettingsUtils.getPillarIDsForCollection(collectionID)) {
            assertTrue(res.containsKey(pillarID));
            assertNotNull(res.get(pillarID));
        }
    }

    @Ignore
    @Test
    public void testGetChecksumsFailure() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        String collectionID = "books";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetChecksumsClient mockClient = mock(GetChecksumsClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[5];

                List<ContributorEvent> events = new ArrayList<ContributorEvent>();
                for(String pillarID : SettingsUtils.getPillarIDsForCollection(collectionID)) {
                    ContributorEvent ce = new ContributorFailedEvent(pillarID, collectionID, ResponseCode.FAILURE);
                    events.add(ce);
                    eh.handleEvent(ce);
                }

                eh.handleEvent(new OperationFailedEvent(collectionID, "Failure intended", events));

                return null;
            }
        }).when(mockClient).getChecksums(anyString(), any(), anyString(), any(ChecksumSpecTYPE.class), any(), any(EventHandler.class), anyString());
        br.setGetChecksumsClient(mockClient);

        Map<String, ChecksumsCompletePillarEvent> res = br.getChecksums(fileid, collectionID);
        
        verify(mockClient).getChecksums(anyString(), any(), anyString(), any(), any(), any(EventHandler.class), anyString());
        verifyNoMoreInteractions(mockClient);

        assertTrue("Should be empty.", res.isEmpty());
    }

    @Ignore
    @Test
    public void testGetFileIDsSuccess() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        String collectionID = "books";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetFileIDsClient mockClient = mock(GetFileIDsClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];

                eh.handleEvent(new CompleteEvent(collectionID, Arrays.asList()));
                return null;
            }
        }).when(mockClient).getFileIDs(anyString(), any(), anyString(), any(URL.class), any(EventHandler.class));
        br.setGetFileIDsClient(mockClient);

        boolean success = br.existsInCollection(fileid, collectionID);
        assertTrue(success);
    }

    @Ignore
    @Test
    public void testGetFileIDsResponseFailure() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        String collectionID = "books";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        GetFileIDsClient mockClient = mock(GetFileIDsClient.class);
        
        // Set the Complete action, when the event is called.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String collectionID = (String) invocation.getArguments()[0];
                EventHandler eh = (EventHandler) invocation.getArguments()[4];

                List<ContributorEvent> events = new ArrayList<ContributorEvent>();
                for(String pillarID : SettingsUtils.getPillarIDsForCollection(collectionID)) {
                    ContributorEvent ce = new ContributorFailedEvent(pillarID, collectionID, ResponseCode.FAILURE);
                    events.add(ce);
                    eh.handleEvent(ce);
                }

                eh.handleEvent(new OperationFailedEvent(collectionID, "Failure intended", events));
                return null;
            }
        }).when(mockClient).getFileIDs(anyString(), any(), anyString(), any(URL.class), any(EventHandler.class));
        br.setGetFileIDsClient(mockClient);

        boolean success = br.existsInCollection(fileid, collectionID);
        assertFalse(success);
    }

    @Ignore
    @Test
    public void testGetFileIDsCollectionFailure() throws YggdrasilException, IOException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        String fileid = "The ID of the file";
        String collectionID = "NonExistingCollection";
        BitrepositoryTestingAPI br = new BitrepositoryTestingAPI(okConfigFile);

        boolean success = br.existsInCollection(fileid, collectionID);
        assertFalse(success);
    }

    @Ignore
    @Test
    public void testGetCollections() throws YggdrasilException {
//        if (TravisUtils.runningOnTravis()) {
//            return;
//        }
        File okConfigFile = new File(OK_YAML_BITMAG_FILE);
        Bitrepository br = new BitrepositoryTestingAPI(okConfigFile);
        List<String> knownCols = br.getKnownCollections();
        assertEquals(knownCols.size(), 5);
    }
    
    private File getFileWithContents(String packageId, byte[] payload) throws IOException {
        File tempDir = new File("temporarydir");
        if (tempDir.isFile()) {
            fail("please remove file '" + tempDir.getAbsolutePath() + "'.");
        }
        tempDir.mkdirs();
        File fr = new File(tempDir, packageId);
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
}
