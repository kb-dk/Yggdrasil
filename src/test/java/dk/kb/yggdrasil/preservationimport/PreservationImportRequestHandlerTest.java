package dk.kb.yggdrasil.preservationimport;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.io.File;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.RequestHandlerContext;
import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.Warc;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.xslt.Models;

@RunWith(JUnit4.class)
public class PreservationImportRequestHandlerTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_WARC_ID = "random-warc-id";    
    protected static final String NON_RANDOM_RECORD_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static final String DEFAULT_URL = "http://localhost:3000/view_file/import";
    protected static final File WARC_FILE = new File("src/test/resources/warc/warcexample.warc");
    protected static final File BAD_WARC_FILE = new File("src/test/resources/warc/metadatawarcexample.warc");

    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");

    protected static PreservationImportRequest request;
    protected static Config config;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        models = new Models(modelsFile);

        request = makeRequest();
    }

    @Test
    public void testSuccessCase() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_FINISHED), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), eq(null));
        verifyNoMoreInteractions(bitrepository);

        verify(httpCommunication).post(eq(DEFAULT_URL), any());
        verifyNoMoreInteractions(httpCommunication);
    }

    @Test
    public void testInvalidRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        PreservationImportRequest badRequest = new PreservationImportRequest();
        prh.handleRequest(badRequest);

        verifyZeroInteractions(updater);
        verifyZeroInteractions(bitrepository);
        verifyZeroInteractions(states);
        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testBadCollectionInRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(""));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testInvalidURLInRequest() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        request.url = "NOT-A-PROPER-URL";
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testFailedRetrievalFromBitrepository() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(BAD_WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        // Needed for setting of the preservation import state by the updater, when failing.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PreservationImportRequestState prs = (PreservationImportRequestState) invocation.getArguments()[0];
                PreservationImportState newState = (PreservationImportState) invocation.getArguments()[1];
                prs.setState(newState);
                return null;
            }
        }).when(updater).sendPreservationImportResponse(any(), any(), any());

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    // TODO make this TEST!
    //    @Test
    //    public void testRetrievedValidationFailure() throws Exception {
    //        // TODO set bad-checksum in 'security'.
    //    }

    // TODO make this TEST!
    @Test
    public void testTokenTimeoutFailure() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(BAD_WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        // Needed for setting of the preservation import state by the updater, when failing.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PreservationImportRequestState prs = (PreservationImportRequestState) invocation.getArguments()[0];
                PreservationImportState newState = (PreservationImportState) invocation.getArguments()[1];
                prs.setState(newState);
                return null;
            }
        }).when(updater).sendPreservationImportResponse(any(), any(), any());

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testFailedDeliveryOfData() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(false);

        // Needed for setting of the preservation import state by the updater, when failing.
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PreservationImportRequestState prs = (PreservationImportRequestState) invocation.getArguments()[0];
                PreservationImportState newState = (PreservationImportState) invocation.getArguments()[1];
                prs.setState(newState);
                return null;
            }
        }).when(updater).sendPreservationImportResponse(any(), any(), any());

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_DELIVERY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verify(httpCommunication).post(eq(DEFAULT_URL), any());
        verifyZeroInteractions(httpCommunication);
    }

    public static PreservationImportRequest makeRequest() {
        PreservationImportRequest request = new PreservationImportRequest();
        request.preservation_profile = DEFAULT_COLLECTION;
        request.security = null;
        request.type = "FILE";
        request.url = DEFAULT_URL;
        request.uuid = NON_RANDOM_UUID;
        request.warc = new Warc();
        request.warc.warc_file_id = NON_RANDOM_WARC_ID;
        request.warc.warc_record_id = NON_RANDOM_RECORD_UUID;

        return request;
    }
}
