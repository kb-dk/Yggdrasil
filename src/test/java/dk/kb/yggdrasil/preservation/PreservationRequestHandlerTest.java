package dk.kb.yggdrasil.preservation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.HttpPayload;
import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Models;
import dk.kb.yggdrasil.config.RequestHandlerContext;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.testutils.MetadataContentUtils;

@RunWith(JUnit4.class)
public class PreservationRequestHandlerTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_FILE_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");
    protected static File testFileDir = new File("temporarydir");

    protected static PreservationRequest request;
    protected static YggdrasilConfig config;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new YggdrasilConfig(generalConfigFile);
        models = new Models(modelsFile);
        
        request = makeRequest();
    }

    @Test
    public void testSuccessCase() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        verifyNoMoreInteractions(updater);
        
        verify(states, times(3)).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testInvalidRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        PreservationRequest badRequest = new PreservationRequest();
        prh.handleRequest(badRequest);
        
        verifyZeroInteractions(updater);
        verifyZeroInteractions(bitrepository);
        verifyZeroInteractions(states);
        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testInvalidModelInRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        PreservationRequest badRequest = makeRequest();
        badRequest.Model = "ThisIsDefinitelyNotAValidModel";
        
        try {
            prh.handleRequest(badRequest);
            Assert.fail("Should throw an exception");
        } catch (YggdrasilException e) {
            // expected
        }
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_FAILED), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);
        verify(states).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);
        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testInvalidMetadataInRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        PreservationRequest badRequest = makeRequest();
        badRequest.metadata = "<metadata />";
        
        try {
            prh.handleRequest(badRequest);
            Assert.fail("Should throw an exception");
        } catch (YggdrasilException e) {
            // expected
        }
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);
        verify(states).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test(expected = YggdrasilException.class)
    public void testBadFileURL() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        PreservationRequest requestWithBadURI = makeRequest();
        requestWithBadURI.File_UUID = "Random File UUID";
        requestWithBadURI.Content_URI = "http://127.0.0.1/" + (new Date()).getTime();
        prh.handleRequest(requestWithBadURI);
    }
    
    @Test
    public void testMissingCollection() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        // Do not return an array with the default collection in it.
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(""));
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), 
                eq(PreservationState.PRESERVATION_REQUEST_FAILED), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(states, bitrepository);
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testFailedUpload() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(false);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_FAILURE));
        verifyNoMoreInteractions(updater);
        
        verify(states, times(3)).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testTimeout() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        // Setup for huge warc-size, low wait-limit and condition checking interval.
        YggdrasilConfig spyConfig = spy(config);
        stub(spyConfig.getWarcSizeLimit()).toReturn(100000000L);
        stub(spyConfig.getUploadWaitLimit()).toReturn(100L);
        stub(spyConfig.getCheckWarcConditionInterval()).toReturn(100L);
        
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, spyConfig, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater, timeout(1500)).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        verifyNoMoreInteractions(updater);

        verify(states, timeout(500)).delete(eq(NON_RANDOM_UUID));
        verify(states, times(3)).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyNoMoreInteractions(bitrepository);
        
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testMultipleWarcFiles() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        String uuid1 = "UUID-1";
        String uuid2 = "UUID-2";
        
        PreservationRequest message = new PreservationRequest();
        message.metadata = request.metadata;
        message.Model = request.Model;
        message.Preservation_profile = request.Preservation_profile;
        message.Valhal_ID = request.Valhal_ID;
        
        message.UUID = uuid1;

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(message);
        verify(states, times(3)).putPreservationRecord(eq(uuid1), any(PreservationRequestState.class));
        verify(states).delete(eq(uuid1));
        
        message.UUID = uuid2;
        prh.handleRequest(message);

        verify(states, times(3)).putPreservationRecord(eq(uuid2), any(PreservationRequestState.class));
        verify(states).delete(eq(uuid2));

        verify(bitrepository, times(2)).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testMultipleRecords() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);

        // Setup for huge warc-size, low wait-limit and condition checking interval.
        YggdrasilConfig spyConfig = spy(config);
        stub(spyConfig.getWarcSizeLimit()).toReturn(100000000L);
        stub(spyConfig.getUploadWaitLimit()).toReturn(100L);
        stub(spyConfig.getCheckWarcConditionInterval()).toReturn(100L);
        
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, spyConfig, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater, timeout(1500)).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        verifyNoMoreInteractions(updater);

        verify(states, timeout(1500)).delete(eq(NON_RANDOM_UUID));
        verify(states, times(3)).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testContentFileRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        
        PreservationRequest request = makeRequest();
        request.Model = "contentfile";
        request.File_UUID = NON_RANDOM_FILE_UUID;
        request.Content_URI = "http://localhost/test.txt";
        request.metadata = MetadataContentUtils.getExampleContentFileMetadata();
        String payloadText = "Content file content";
        
        HttpPayload payload = new HttpPayload(new ByteArrayInputStream(payloadText.getBytes()), null, "application/octetstream", (long) payloadText.length(), testFileDir);
        when(httpCommunication.get(anyString())).thenReturn(payload);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater, timeout(1500)).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        verifyNoMoreInteractions(updater);

        verify(states, times(4)).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states, timeout(1500)).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
        verifyNoMoreInteractions(bitrepository);
        
        verify(httpCommunication).get(anyString());
        verifyNoMoreInteractions(httpCommunication);
    }
    
    @Test
    public void testContentFileDownloadFailure() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = getMockUpdater();
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        
        PreservationRequest request = makeRequest();
        request.Model = "contentfile";
        request.File_UUID = NON_RANDOM_FILE_UUID;
        request.Content_URI = "http://localhost/test.txt";
        request.metadata = MetadataContentUtils.getExampleContentFileMetadata();

        when(httpCommunication.get(anyString())).thenReturn(null);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        try {
            prh.handleRequest(request);
            Assert.fail();
        } catch (YggdrasilException e) {
            // Expected
        }

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_RESOURCES_DOWNLOAD_FAILURE), anyString());
        verifyNoMoreInteractions(updater);

        verify(states).putPreservationRecord(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states, timeout(1500)).delete(eq(NON_RANDOM_UUID));
        verifyNoMoreInteractions(states);

        verify(bitrepository).getKnownCollections();
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
    
    public static RemotePreservationStateUpdater getMockUpdater() throws YggdrasilException {
        RemotePreservationStateUpdater res = mock(RemotePreservationStateUpdater.class);

        // Needed for setting of the preservation import state by the updater, when failing.
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                PreservationRequestState prs = (PreservationRequestState) invocation.getArguments()[0];
                PreservationState newState = (PreservationState) invocation.getArguments()[1];
                prs.setState(newState);
                return null;
            }
        }).when(res).sendPreservationResponse(any(PreservationRequestState.class), any(PreservationState.class));
        
        return res;
    }
}
