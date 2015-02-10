package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessagingTestUtils;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.json.PreservationResponse;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.xslt.Models;

/**
 * Created by jatr on 2/24/14.
 */
@RunWith(JUnit4.class)
public class WorkflowTest {

    private static Workflow workflow;
    private static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    private static File generalConfigFile = new File("config/yggdrasil.yml");
    private static MQ mq;
    private static RabbitMqSettings settings; 

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException, RabbitException {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        Config config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        File rabbitMQConfig = new File(RABBITMQ_CONF_FILE);
        settings = new RabbitMqSettings(rabbitMQConfig);
        mq = new MQ(settings);
        StateDatabase stateDatabase = new StateDatabase(config.getDatabaseDir());
        Bitrepository bitrepository = Mockito.mock(Bitrepository.class);
        Models models = new Models(new File("config/models.yml"));

        workflow = new Workflow(mq, stateDatabase, bitrepository, config, models);
    }

    @Test
    public void correctlyFormattedPreservationResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, YggdrasilException, RabbitException {
        PreservationRequest preservationRequest = new PreservationRequest();
        preservationRequest.Model = "Book";
        preservationRequest.Valhal_ID = "Valhal:1";
        preservationRequest.File_UUID = UUID.randomUUID().toString();
        State newPreservationState = State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS;
        PreservationRequestState prs = new PreservationRequestState(preservationRequest, newPreservationState, UUID.randomUUID().toString());

        Method updateRemotePreservationState = Workflow.class.getDeclaredMethod("updateRemotePreservationState", 
        		PreservationRequestState.class, State.class);
        updateRemotePreservationState.setAccessible(true);
        updateRemotePreservationState.invoke(workflow, prs, newPreservationState);

        //now read message from queue and check content is correct for this use case
        MqResponse requestContent = mq.receiveMessageFromQueue(
                mq.getSettings().getPreservationResponseDestination());
        Assert.assertNotNull(requestContent);

        PreservationResponse response = JSONMessagingTestUtils.getPreservationResponse(new PushbackInputStream(new ByteArrayInputStream(requestContent.getPayload()), 4));
        Assert.assertNotNull(response);
        Assert.assertEquals(response.id, preservationRequest.Valhal_ID);
        Assert.assertEquals(response.model, preservationRequest.Model);
        Assert.assertEquals(response.preservation.preservation_state, newPreservationState.name());
    }
    
    @Test
    public void handlingRequestMessage() throws NoSuchMethodException, SecurityException, YggdrasilException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	Method getNextRequest = Workflow.class.getDeclaredMethod("getNextRequest");
    	getNextRequest.setAccessible(true);
    	
    	String uuid = UUID.randomUUID().toString();
    	String profile = "simple";
    	String ValhalId = "valhal:1";
    	String model = "Book";
    	String message = "{\"UUID\":\"" + uuid + "\","
    			+ "\"Preservation_profile\":\"" + profile + "\","
    			+ "\"Valhal_ID\":\"" + ValhalId + "\","
    			+ "\"Model\":\"" + model + "\","
    			+ "\"metadata\": \"<metadata></metadata>\""
    			+ "}";
    	
    	mq.publishOnQueue(settings.getPreservationDestination(), 
    			message.getBytes(), 
    			MQ.PRESERVATIONREQUEST_MESSAGE_TYPE);
    	
    	PreservationRequest req = (PreservationRequest) getNextRequest.invoke(workflow);
    	Assert.assertNotNull(req);
    	Assert.assertEquals(req.UUID, uuid);
    	Assert.assertEquals(req.Preservation_profile, profile);
    	Assert.assertEquals(req.Valhal_ID, ValhalId);
    	Assert.assertEquals(req.Model, model);
    	Assert.assertNull(req.File_UUID);
    	Assert.assertNull(req.Content_URI);
    }
}
