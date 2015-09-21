package dk.kb.yggdrasil.workflow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
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

import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.MqFixtureTestAPI;
import dk.kb.yggdrasil.Workflow;
import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Models;
import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessagingTestUtils;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.preservation.PreservationState;
import dk.kb.yggdrasil.testutils.MetadataContentUtils;

@RunWith(JUnit4.class)
public class PreservationRequestWorkflowTest extends MqFixtureTestAPI {

    private static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    private static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    private static RabbitMqSettings settings;
    private static Models models;
    private static YggdrasilConfig config;

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException, RabbitException {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new YggdrasilConfig(generalConfigFile);
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
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String uuid = UUID.randomUUID().toString();
        String profile = "simple";
        String valhalId = "valhal:1";
        String model = "Instance";
        String metadata = MetadataContentUtils.getExampleInstanceMetadata();
        PreservationRequest request = makeRequest(model, profile, uuid, valhalId, metadata);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.uploadFile(any(), anyString())).thenReturn(true);

        workflow.run();

        verify(stateDatabase, times(2)).putPreservationRecord(eq(uuid), any(PreservationRequestState.class));
        verify(stateDatabase, timeout(1500)).delete(eq(uuid));
        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater, timeout(1500)).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS));
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(profile));
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingFailWrongProfile() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);


        String uuid = UUID.randomUUID().toString();
        String profile = "simple";
        String valhalId = "valhal:1";
        String model = "Instance";
        String metadata = MetadataContentUtils.getExampleInstanceMetadata();
        PreservationRequest request = makeRequest(model, profile, uuid, valhalId, metadata);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList());
        when(bitrepository.uploadFile(any(), anyString())).thenReturn(true);

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_FAILED), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingFailWrongModel() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String uuid = UUID.randomUUID().toString();
        String profile = "simple";
        String valhalId = "valhal:1";
        String model = "NotAnValhalModel";
        String metadata = MetadataContentUtils.getExampleInstanceMetadata();
        PreservationRequest request = makeRequest(model, profile, uuid, valhalId, metadata);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.uploadFile(any(), anyString())).thenReturn(true);

        workflow.run();

        verify(stateDatabase).putPreservationRecord(eq(uuid), any(PreservationRequestState.class));
        verify(stateDatabase, timeout(1500)).delete(eq(uuid));
        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_FAILED), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingFailMetadataError() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String uuid = UUID.randomUUID().toString();
        String profile = "simple";
        String valhalId = "valhal:1";
        String model = "Instance";
        String metadata = "<metadata></metadata>";
        PreservationRequest request = makeRequest(model, profile, uuid, valhalId, metadata);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.uploadFile(any(), anyString())).thenReturn(true);

        workflow.run();

        verify(stateDatabase).putPreservationRecord(eq(uuid), any(PreservationRequestState.class));
        verify(stateDatabase, timeout(1500)).delete(eq(uuid));
        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponseWithSpecificDetails(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_FAILURE), anyString());
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    @Test
    public void preservationRequestHandlingFailedUpload() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        String uuid = UUID.randomUUID().toString();
        String profile = "simple";
        String valhalId = "valhal:1";
        String model = "Instance";
        String metadata = MetadataContentUtils.getExampleInstanceMetadata();
        PreservationRequest request = makeRequest(model, profile, uuid, valhalId, metadata);
        byte[] requestBytes = JSONMessagingTestUtils.getPreservationRequest(request);

        MqResponse handledResponse = new MqResponse(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE, requestBytes);
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(handledResponse, finalReponse);
        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(profile));
        when(bitrepository.uploadFile(any(), anyString())).thenReturn(false);

        workflow.run();

        verify(stateDatabase, times(2)).putPreservationRecord(eq(uuid), any(PreservationRequestState.class));
        verify(stateDatabase, timeout(1500)).delete(eq(uuid));
        verifyNoMoreInteractions(stateDatabase);

        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_REQUEST_RECEIVED));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verify(updater, timeout(1500)).sendPreservationResponse(any(PreservationRequestState.class), eq(PreservationState.PRESERVATION_PACKAGE_UPLOAD_FAILURE));
        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).uploadFile(any(File.class), eq(profile));
        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }

    public static PreservationRequest makeRequest(String model, String preservationProfile, String uuid, 
            String valhalId, String metadata) {
        PreservationRequest request = new PreservationRequest();
        request.Content_URI = null;
        request.File_UUID = null;
        request.Model = model;
        request.Preservation_profile = preservationProfile;
        request.UUID = uuid;
        request.Valhal_ID = valhalId;
        request.metadata = metadata;
        return request;
    }
}
