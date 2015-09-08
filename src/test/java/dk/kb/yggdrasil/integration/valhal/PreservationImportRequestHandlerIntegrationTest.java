package dk.kb.yggdrasil.integration.valhal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.HttpCommunication;
import dk.kb.yggdrasil.RabbitMqSettings;
import dk.kb.yggdrasil.RequestHandlerContext;
import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.Security;
import dk.kb.yggdrasil.json.preservationimport.Warc;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.preservationimport.PreservationImportRequestHandler;
import dk.kb.yggdrasil.preservationimport.PreservationImportState;
import dk.kb.yggdrasil.xslt.Models;

@RunWith(JUnit4.class)
public class PreservationImportRequestHandlerIntegrationTest {
    protected static final String NON_RANDOM_UUID = "f0/86/a8/7b/f086a87b-973a-4403-9326-1acc727864b3";
    protected static final String NON_RANDOM_WARC_ID = "random-warc-id";    
    protected static final String NON_RANDOM_RECORD_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static final String DEFAULT_TYPE = "FILE";
    protected static final String DEFAULT_URL = "http://localhost:3000/view_file/import_from_preservation";
    protected static final File WARC_FILE = new File("src/test/resources/warc/warcexample.warc");
    protected static final File WARC_FILE_WITHOUT_THE_RECORD = new File("src/test/resources/warc/metadatawarcexample.warc");
    protected static final File WARC_FILE_WITH_BAD_CHECKSUM_IN_HEADER = new File("src/test/resources/warc/warcexample_badchecksum.warc");
    protected static final String SECURITY_CHECKSUM = "sha-1:5875f4d3fe7058ef89bcd28b6e11258e8ed2762b";

    protected static File generalConfigFile = new File("src/test/resources/config/yggdrasil.yml");
    protected static File modelsFile = new File("src/test/resources/config/models.yml");

    protected static Config config;
    protected static RabbitMqSettings mqSettings;
    protected static Models models;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        models = new Models(modelsFile);
        mqSettings = new RabbitMqSettings("amqp://localhost:5672", "preservation-dev-queue", 
                "preservation-dev-response-queue");
    }
    
    @Test
//    @Ignore // Should only be run manually for integration with Valhal.
    public void testSuccessCase() throws Exception {
        StateDatabase states = mock(StateDatabase.class);
        Bitrepository bitrepository = mock(Bitrepository.class);
        MQ mq = new MQ(mqSettings);
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);
//        HttpCommunication httpCommunication = Mockito.mock(HttpCommunication.class);
        HttpCommunication httpCommunication = new HttpCommunication();
        PreservationImportRequest request = makeRequestWithSecurity();

        when(bitrepository.getKnownCollections()).thenReturn(Arrays.asList(DEFAULT_COLLECTION));
        when(bitrepository.getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), any())).thenReturn(WARC_FILE);

        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, updater, httpCommunication);
        PreservationImportRequestHandler prh = new PreservationImportRequestHandler(context);

        prh.handleRequest(request);

//        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED), any());
//        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED), any());
//        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_DELIVERY_INITIATED), any());
//        verify(updater).sendPreservationImportResponse(any(PreservationImportRequestState.class), eq(PreservationImportState.IMPORT_FINISHED), any());
//        verifyNoMoreInteractions(updater);

        verify(bitrepository).getKnownCollections();
        verify(bitrepository).getFile(eq(NON_RANDOM_WARC_ID), eq(DEFAULT_COLLECTION), eq(null));
        verifyNoMoreInteractions(bitrepository);
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
        request.security.token = "pyjMGSscvmOKiHsrNNYxqz8FfS66Jc0rcZ8ydXbS9hM=";
        request.security.token_timeout = new Date(9999999999999L).toString(); // unreasonable long time into the future
        return request;
    }
}
