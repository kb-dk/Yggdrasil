package dk.kb.yggdrasil.workflow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.MqFixtureTestAPI;
import dk.kb.yggdrasil.RabbitMqSettings;
import dk.kb.yggdrasil.Workflow;
import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessagingTestUtils;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.Warc;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.preservationimport.PreservationImportState;
import dk.kb.yggdrasil.xslt.Models;

@RunWith(JUnit4.class)
public class PreservationImportRequestWorkflowTest extends MqFixtureTestAPI {

    private static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    private static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    private static RabbitMqSettings settings;
    private static Models models;
    private static Config config;
    
    protected static final File WARC_FILE = new File("src/test/resources/warc/warcexample.warc");
    protected static final String SECURITY_CHECKSUM = "sha-1:5875f4d3fe7058ef89bcd28b6e11258e8ed2762b";
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_WARC_ID = "random-warc-id";    
    protected static final String NON_RANDOM_RECORD_UUID = "random-file-uuid";

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException, RabbitException {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        File rabbitMQConfig = new File(RABBITMQ_CONF_FILE);
        settings = new RabbitMqSettings(rabbitMQConfig);

        models = new Models(new File("config/models.yml"));
    }

    @Test
    public void preservationRequestHandlingSuccess() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "simple";
        String uuid = NON_RANDOM_RECORD_UUID;
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "FILE";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.getFile(anyString(), anyString(), any())).thenReturn(WARC_FILE);
        when(httpCommunication.post(anyString(), any())).thenReturn(true);

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_FINISHED), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(anyString(), anyString(), any());
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingProfileError() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "NotAValidProfile";
        String uuid = NON_RANDOM_RECORD_UUID;
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "FILE";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList());

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingTypeError() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "simple";
        String uuid = NON_RANDOM_RECORD_UUID;
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "ThisIsNotAValidType";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_VALIDATION_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingNoFile() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "simple";
        String uuid = NON_RANDOM_RECORD_UUID;
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "FILE";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.getFile(anyString(), anyString(), any())).thenThrow(new YggdrasilException("Not file"));

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
        
        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(anyString(), anyString(), any());
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingMissingRecord() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "simple";
        String uuid = UUID.randomUUID().toString();
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "FILE";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.getFile(anyString(), anyString(), any())).thenReturn(WARC_FILE);;

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
        
        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(anyString(), anyString(), any());
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingCannotUpload() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String profile = "simple";
        String uuid = NON_RANDOM_RECORD_UUID;
        String warcId = WARC_FILE.getName();
        String recordId = uuid;
        String type = "FILE";
        String url = "http://localhost:3000/view_file/import_from_preservation";
        PreservationImportRequest request = makeRequest(profile, type, url, uuid, warcId, recordId);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationImportRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.IMPORTREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(mq.getSettings()).thenReturn(settings);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.getFile(anyString(), anyString(), any())).thenReturn(WARC_FILE);
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
        
        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED), any());
        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), 
                eq(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_FAILURE), any());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(anyString(), anyString(), any());
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    public static PreservationImportRequest makeRequest(String preservationProfile, String type, String url, 
            String uuid, String warcId, String recordUuid) {
        PreservationImportRequest request = new PreservationImportRequest();
        request.preservation_profile = preservationProfile;
        request.security = null;
        request.type = type;
        request.url = url;
        request.uuid = uuid;
        request.warc = new Warc();
        request.warc.warc_file_id = warcId;
        request.warc.warc_record_id = recordUuid;
        return request;
    }
}
