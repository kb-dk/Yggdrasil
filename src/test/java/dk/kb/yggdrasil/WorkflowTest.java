package dk.kb.yggdrasil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.xslt.Models;

@RunWith(JUnit4.class)
public class WorkflowTest extends MqFixtureTestAPI {

    private static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    private static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    private static RabbitMqSettings settings;
    private static Models models;
    private static Config config;

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
    public void verifyShutdownMessageHandling() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(finalReponse);

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verifyNoMoreInteractions(updater);

        verifyNoMoreInteractions(bitrepository);

        verify(mq).getSettings();
        verify(mq).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }
    
    @Test
    public void verifyOddTypeMessageHandling() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        MqResponse firstReponse = new MqResponse("ThisIsNotAProperMessageType", "418: I'm a teapot".getBytes());
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(firstReponse, finalReponse);

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verifyNoMoreInteractions(updater);

        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }
    
    @Test
    public void verifyNoTypeMessageHandling() throws Exception {
        StateDatabase stateDatabase = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        HttpCommunication httpCommunication = mock(HttpCommunication.class);
        MQ mq = mock(MQ.class);
        RemotePreservationStateUpdater updater = mock(RemotePreservationStateUpdater.class);
        when(mq.getSettings()).thenReturn(settings);

        Workflow workflow = new Workflow(mq, stateDatabase, bitrepository, config, models, httpCommunication, updater);

        MqResponse firstReponse = new MqResponse(null, "null".getBytes());
        MqResponse finalReponse = new MqResponse(MQ.SHUTDOWN_MESSAGE_TYPE, "Please terminate Yggdrasil".getBytes());

        when(mq.receiveMessageFromQueue(anyString())).thenReturn(firstReponse, finalReponse);

        workflow.run();

        verifyNoMoreInteractions(stateDatabase);

        verifyNoMoreInteractions(updater);

        verifyNoMoreInteractions(bitrepository);

        verify(mq, times(2)).getSettings();
        verify(mq, times(2)).receiveMessageFromQueue(anyString());
        verifyNoMoreInteractions(mq);
    }
}
