package dk.kb.yggdrasil.preservation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.HttpPayload;
import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Models;
import dk.kb.yggdrasil.config.RequestHandlerContext;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.testutils.MetadataContentUtils;

// Only mocks external communication (HTTP, MQ and bitrepository)
@RunWith(JUnit4.class)
public class PreservationIntegrationTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_FILE_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");
    protected static File testStateDir = new File("temporarydir/statedir");
    protected static File testFileDir = new File("temporarydir");

    protected static YggdrasilConfig config;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new YggdrasilConfig(generalConfigFile);
        models = new Models(modelsFile);
        
        testStateDir.mkdirs();
        testFileDir.mkdirs();
        FileUtils.cleanDirectory(testStateDir);
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        FileUtils.deleteDirectory(testStateDir);
        FileUtils.deleteDirectory(testFileDir);
    }

    @Test
    public void testSuccessCaseWithoutFile() throws Exception {
        Bitrepository bitrepository = mock(Bitrepository.class);
        MQ mq = mock(MQ.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);

        StateDatabase states = new StateDatabase(testStateDir);
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);
        
        PreservationRequest request = makeRequest();
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(mq, times(5)).publishPreservationResponse(any(PreservationResponse.class));
        verifyNoMoreInteractions(mq);
        
        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyNoMoreInteractions(bitrepository);
        
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testSuccessCaseWithFile() throws Exception {
        Bitrepository bitrepository = mock(Bitrepository.class);
        MQ mq = mock(MQ.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        String payloadText = "Content file content";

        HttpPayload payload = new HttpPayload(new ByteArrayInputStream(payloadText.getBytes()), null, "application/octetstream", (long) payloadText.length(), testFileDir);
        when(httpCommunication.get(anyString())).thenReturn(payload);

        StateDatabase states = new StateDatabase(testStateDir);
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);
        
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        PreservationRequest request = makeRequest();
        request.Model = "contentfile";
        request.File_UUID = NON_RANDOM_FILE_UUID;
        request.Content_URI = "http://localhost/test.txt";
        request.metadata = MetadataContentUtils.getExampleContentFileMetadata();
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(mq, times(7)).publishPreservationResponse(any(PreservationResponse.class));
        verifyNoMoreInteractions(mq);
        
        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyNoMoreInteractions(bitrepository);
        
        verify(httpCommunication).get(anyString());
        verifyNoMoreInteractions(httpCommunication);
    }
    
    public static PreservationRequest makeRequest() {
        PreservationRequest request = new PreservationRequest();
        request.Content_URI = null;
        request.File_UUID = null;
        request.metadata = MetadataContentUtils.getExampleInstanceMetadata();
        request.Model = "instance";
        request.Preservation_profile = DEFAULT_COLLECTION;
        request.UUID = NON_RANDOM_UUID;
        request.Valhal_ID = "ID";
        return request;
    }    
}
