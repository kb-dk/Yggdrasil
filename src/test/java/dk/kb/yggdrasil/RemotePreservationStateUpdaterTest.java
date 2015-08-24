package dk.kb.yggdrasil;

import java.io.File;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.preservation.PreservationState;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;

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
        PreservationRequestState prs = new PreservationRequestState(request, PreservationState.PRESERVATION_REQUEST_RECEIVED, NON_RANDOM_UUID);        
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);

        updater.sendPreservationResponse(prs, PreservationState.PRESERVATION_REQUEST_FAILED);

        Mockito.verify(mq).publishPreservationResponse(Mockito.any(PreservationResponse.class));
    }

    @Test
    public void testUsingSpecificDetails() throws Exception {
        MQ mq = Mockito.mock(MQ.class);
        PreservationRequestState prs = new PreservationRequestState(request, PreservationState.PRESERVATION_REQUEST_RECEIVED, NON_RANDOM_UUID);        
        RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);

        updater.sendPreservationResponseWithSpecificDetails(prs, PreservationState.PRESERVATION_REQUEST_FAILED, "Test are test specific details.");

        Mockito.verify(mq).publishPreservationResponse(Mockito.any(PreservationResponse.class));
    }

    @Test
    public void testAllElements() throws Exception {
        MQ mq = Mockito.mock(MQ.class);
        final String warcId = UUID.randomUUID().toString();
        File warc = new File("temporarydir", warcId);
        try {
            warc.createNewFile();
            PreservationRequestState prs = new PreservationRequestState(request, PreservationState.PRESERVATION_REQUEST_RECEIVED, NON_RANDOM_UUID);
            prs.setMetadataWarcFile(warc);
            prs.setResourceWarcFile(warc);
            RemotePreservationStateUpdater updater = new RemotePreservationStateUpdater(mq);

            updater.sendPreservationResponseWithSpecificDetails(prs, PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA, "Test are test specific details.");

            ArgumentMatcher<PreservationResponse> matcher = new ArgumentMatcher<PreservationResponse>() {
                @Override
                public boolean matches(Object item) {
                    PreservationResponse response = (PreservationResponse) item;
                    if(!response.id.equals(request.Valhal_ID)) {
                        return false;
                    }
                    if(!response.model.equals(request.Model)) {
                        return false;
                    }

                    if(!response.preservation.preservation_state.equals(PreservationState.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA.name())) {
                        return false;
                    }
                    if(!response.preservation.warc_id.equals(warcId)) {
                        return false;
                    }
                    if(!response.preservation.file_warc_id.equals(warcId)) {
                        return false;
                    }
                    return true;
                }
            };

            Mockito.verify(mq).publishPreservationResponse(Mockito.argThat(matcher));     
        } finally {
            if(warc.exists()) {
                warc.delete();
            }
        }
    }
}
