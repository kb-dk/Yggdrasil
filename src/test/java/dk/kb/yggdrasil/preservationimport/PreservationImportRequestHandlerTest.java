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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;

import org.bitrepository.bitrepositoryelements.FilePart;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Models;
import dk.kb.yggdrasil.config.RequestHandlerContext;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.Security;
import dk.kb.yggdrasil.json.preservationimport.Warc;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;

@RunWith(JUnit4.class)
public class PreservationImportRequestHandlerTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_WARC_ID = "random-warc-id";    
    protected static final String NON_RANDOM_RECORD_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static final String DEFAULT_TYPE = "FILE";
    protected static final String DEFAULT_URL = "http://localhost:3000/view_file/import";
    protected static final File WARC_FILE = new File("src/test/resources/warc/warcexample.warc");
    protected static final File WARC_FILE_WITHOUT_THE_RECORD = new File("src/test/resources/warc/metadatawarcexample.warc");
    protected static final File WARC_FILE_WITH_BAD_CHECKSUM_IN_HEADER = new File("src/test/resources/warc/warcexample_badchecksum.warc");
    protected static final String SECURITY_CHECKSUM = "sha-1:5875f4d3fe7058ef89bcd28b6e11258e8ed2762b";

    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");

    protected static YggdrasilConfig config;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new YggdrasilConfig(generalConfigFile);
        models = new Models(modelsFile);
    }
    
    @Test
    public void testExtractRequest() throws Exception {
        StringBuilder request = new StringBuilder();
        request.append("{\n");
        request.append("  \"type\": \"" + DEFAULT_TYPE + "\",\n");
        request.append("  \"uuid\": \"" + NON_RANDOM_UUID + "\",\n");
        request.append("  \"preservation_profile\": \"" + DEFAULT_COLLECTION + "\",\n");
        request.append("  \"url\": \"" + DEFAULT_URL + "\",\n");
        request.append("  \"warc\": {\n");
        request.append("    \"warc_file_id\": \"" + NON_RANDOM_WARC_ID + "\",\n");
        request.append("    \"warc_record_id\": \"" + NON_RANDOM_RECORD_UUID + "\"\n");
        request.append("  }\n");
        request.append("}\n");

        RequestHandlerContext context = mock(RequestHandlerContext.class);
        PreservationImportRequestHandler prih = new PreservationImportRequestHandler(context);
        prih.extractRequest(request.toString().getBytes());
    }

    @Test(expected = YggdrasilException.class)
    public void testExtractRequestEmpty() throws Exception {
        String request = "";
        RequestHandlerContext context = mock(RequestHandlerContext.class);
        PreservationImportRequestHandler prih = new PreservationImportRequestHandler(context);
        prih.extractRequest(request.getBytes());
    }

    @Test
    public void testSuccessCase() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequest();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FINISHED), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), eq(null));
        verifyNoMoreInteractions(bitrepository);

        verify(httpCommunication).post(eq(DEFAULT_URL), any());
        verifyNoMoreInteractions(httpCommunication);
    }

    @Test
    public void testWarcOffset() throws Exception {
        Long offset = 87539319L;
        Long recordSize = 1729L;
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequest();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        request.warc.warc_offset = offset.toString();
        request.warc.warc_record_size = recordSize.toString();
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FINISHED), any());
        verifyNoMoreInteractions(updater);

        FilePart filePart = new FilePart();
        filePart.setPartOffSet(BigInteger.valueOf(offset));
        filePart.setPartLength(BigInteger.valueOf(recordSize));

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), eq(filePart));
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
        PreservationImportRequest request = makeRequest();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(""));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
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
        PreservationImportRequest request = makeRequest();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        request.url = "NOT-A-PROPER-URL";
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
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
        PreservationImportRequest request = makeRequest();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE_WITHOUT_THE_RECORD);
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

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testInvalidChecksumFormatFailure() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
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

        request.security.checksum = "NOT_AN_ALGORITHM";
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testInvalidChecksumAlgorithmFailure() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
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

        // The Verhoeff algorithm does exist, but it is not valid.
        request.security.checksum = "Verhoeff:4c24916aa6280f784c40c28b53df9343f2efcecc";
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }
    
    @Test
    public void testRetrievedValidationWithChecksum() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
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

        request.security.checksum = SECURITY_CHECKSUM;
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FINISHED), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verify(httpCommunication).post(eq(DEFAULT_URL), any());
        verifyNoMoreInteractions(httpCommunication);
    }
    
    @Test
    public void testRetrievedValidationWithIncorrectChecksum() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
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

        request.security.checksum = "sha-1:1111111111111111111111111111111111111111";
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }

    @Test
    public void testValidationWithIncorrectWarcHeaderChecksum() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE_WITH_BAD_CHECKSUM_IN_HEADER);
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

        request.security.checksum = SECURITY_CHECKSUM;
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any());
        verifyNoMoreInteractions(bitrepository);

        verifyZeroInteractions(httpCommunication);
    }
    
    
    @Test
    public void testTokenTimeoutFailure() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);
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

        request.security.token_timeout = new Date(0).toString();
        prh.handleRequest(request);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_FAILURE), any());
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
        PreservationImportRequest request = makeRequest();

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

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_FAILURE), any());
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
        request.type = DEFAULT_TYPE;
        request.url = DEFAULT_URL;
        request.uuid = NON_RANDOM_UUID;
        request.warc = new Warc();
        request.warc.warc_file_id = NON_RANDOM_WARC_ID;
        request.warc.warc_record_id = NON_RANDOM_RECORD_UUID;

        return request;
    }
    
    public static PreservationImportRequest makeRequestWithSecurity() {
        PreservationImportRequest request = makeRequest();
        request.security = new Security();
        request.security.token = "ASDF";
        request.security.token_timeout = new Date(9999999999999L).toString(); // unreasonable long time into the future
        return request;
    }
}
