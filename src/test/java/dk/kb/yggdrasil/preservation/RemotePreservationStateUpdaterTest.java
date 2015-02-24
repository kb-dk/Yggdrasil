package dk.kb.yggdrasil.preservation;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.messaging.MQ;

@RunWith(JUnit4.class)
public class RemotePreservationStateUpdaterTest {
    protected static PreservationRequest request;
    protected static final String NON_RANDOM_UUID = "random-uuid";

    @BeforeClass
    public static void beforeClass() throws Exception {
        request = new PreservationRequest();
        request.Content_URI = null;
        request.File_UUID = null;
        request.metadata = "";
        request.Model = "MODEL";
        request.Preservation_profile = "collectionId";
        request.UUID = NON_RANDOM_UUID;
        request.Valhal_ID = "ID";
    }

    @Test
    public void testUsingStateDefaultsDetails() throws Exception {
        MQ mq = Mockito.mock(MQ.class);
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, NON_RANDOM_UUID);        
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);
        
        updater.updateRemotePreservationState(prs, State.PRESERVATION_REQUEST_FAILED);
        
        Mockito.verify(mq).publishPreservationResponse(Mockito.any(byte[].class));
    }
    
    @Test
    public void testUsingSpecificDetails() throws Exception {
        MQ mq = Mockito.mock(MQ.class);
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, NON_RANDOM_UUID);        
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);
        
        updater.updateRemotePreservationStateWithSpecificDetails(prs, State.PRESERVATION_REQUEST_FAILED, "Test are test specific details.");
        
        Mockito.verify(mq).publishPreservationResponse(Mockito.any(byte[].class));
    }
}
