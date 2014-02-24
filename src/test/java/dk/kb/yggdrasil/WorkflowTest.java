package dk.kb.yggdrasil;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.xslt.Models;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by jatr on 2/24/14.
 */
@RunWith(JUnit4.class)
public class WorkflowTest {

    private static Workflow workflow;
    private static String RABBITMQ_CONF_FILE = "src/test/resources/config/rabbitmq.yml";
    private static File generalConfigFile = new File("config/yggdrasil.yml");
    private static File okConfigFile = new File("src/test/resources/config/bitmag.yml");

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException {

        Config config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        File rabbitMQConfig = new File(RABBITMQ_CONF_FILE);
        RabbitMqSettings settings = new RabbitMqSettings(rabbitMQConfig);
        MQ mq = new MQ(settings);
        StateDatabase stateDatabase = new StateDatabase(config.getDatabaseDir());
        Bitrepository bitrepository = new Bitrepository(okConfigFile);
        Models models = new Models(new File("config/models.yml"));

        workflow = new Workflow(mq, stateDatabase, bitrepository, config, models);
    }

    @Test
    public void correctlyFormattedPreservationResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        PreservationRequest preservationRequest = new PreservationRequest();
        State newPreservationState = State.PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS;
        PreservationRequestState prs = new PreservationRequestState(preservationRequest, newPreservationState, UUID.randomUUID().toString());

        Method updateRemotePreservationState = Workflow.class.getDeclaredMethod("updateRemotePreservationState");
        updateRemotePreservationState.setAccessible(true);
        updateRemotePreservationState.invoke(workflow, prs, newPreservationState);

        //now read message from queue and check content is correct for this use case
    }
}
