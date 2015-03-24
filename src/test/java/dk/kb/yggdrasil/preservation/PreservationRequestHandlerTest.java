package dk.kb.yggdrasil.preservation;

import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.MetadataContentUtils;
import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.xslt.Models;

@RunWith(JUnit4.class)
public class PreservationRequestHandlerTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");

    protected static PreservationRequest request;
    protected static Config config;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        models = new Models(modelsFile);
        
        request = new PreservationRequest();
        request.Content_URI = null;
        request.File_UUID = null;
        request.metadata = MetadataContentUtils.getExampleMetadata();
        request.Model = "work";
        request.Preservation_profile = DEFAULT_COLLECTION;
        request.UUID = NON_RANDOM_UUID;
        request.Valhal_ID = "ID";
    }

    @Test
    public void testSuccessCase() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        PreservationContext context = new PreservationContext(bitrepository, config, states, updater);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        
        verify(states, times(2)).put(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
    }
    
    @Test
    public void testMissingCollection() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        // Do not return an array with the default collection in it.
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(""));
        
        PreservationContext context = new PreservationContext(bitrepository, config, states, updater);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).updateRemotePreservationStateWithSpecificDetails(any(PreservationRequestState.class), 
                eq(State.PRESERVATION_REQUEST_FAILED), anyString());
        
        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(states, bitrepository);
    }
    
    @Test
    public void testFailedUpload() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(false);
        
        PreservationContext context = new PreservationContext(bitrepository, config, states, updater);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_UPLOAD_FAILURE));
        
        verify(states, times(2)).put(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
    }
    
    @Test
    public void testTimeout() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        // Setup for huge warc-size, low wait-limit and condition checking interval.
        Config spyConfig = spy(config);
        stub(spyConfig.getWarcSizeLimit()).toReturn(100000000L);
        stub(spyConfig.getUploadWaitLimit()).toReturn(100L);
        stub(spyConfig.getCheckWarcConditionInterval()).toReturn(100L);
        
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.uploadFile(any(File.class), anyString())).thenReturn(true);
        
        PreservationContext context = new PreservationContext(bitrepository, spyConfig, states, updater);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(request);

        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(bitrepository).getKnownCollections();

        // Wait for timeout
        verify(updater, timeout(1500)).updateRemotePreservationState(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));

        verify(states, times(2)).put(eq(NON_RANDOM_UUID), any(PreservationRequestState.class));
        verify(states).delete(eq(NON_RANDOM_UUID));

        verify(bitrepository).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
    }
    
    @Test
    public void testMultipleWarcFiles() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

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
        
        PreservationContext context = new PreservationContext(bitrepository, config, states, updater);
        PreservationRequestHandler prh = new PreservationRequestHandler(context, models);

        prh.handleRequest(message);
        verify(states, times(2)).put(eq(uuid1), any(PreservationRequestState.class));
        verify(states).delete(eq(uuid1));
        
        message.UUID = uuid2;
        prh.handleRequest(message);

        verify(states, times(2)).put(eq(uuid2), any(PreservationRequestState.class));
        verify(states).delete(eq(uuid2));

        verify(bitrepository, times(2)).uploadFile(any(File.class), eq(DEFAULT_COLLECTION));
    }
}
