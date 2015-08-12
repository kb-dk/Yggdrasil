package dk.kb.yggdrasil.json;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.json.preservation.PreservationRequest;

@RunWith(JUnit4.class)
public class PreservationRequestTest {

    private String defaultPreservationProfile = "simple";
    private String defaultValhalId = "Valhal:1";
    private String defaultModel = "Work";
    
    @Test
    public void testPreservationRequestUUID() {
        PreservationRequest pr = getDefaultPreservationRequest();
        pr.UUID = null;
        assertFalse(pr.isMessageValid());
        pr.UUID = "";
        assertFalse(pr.isMessageValid());
        pr.UUID = UUID.randomUUID().toString();
        assertTrue(pr.isMessageValid());
    }
    
    @Test
    public void testPreservationRequestPreservationProfile() {
        PreservationRequest pr = getDefaultPreservationRequest();
        pr.Preservation_profile = null;
        assertFalse(pr.isMessageValid());
        pr.Preservation_profile = "";
        assertFalse(pr.isMessageValid());
        pr.Preservation_profile = defaultPreservationProfile;
        assertTrue(pr.isMessageValid());
    }
    
    @Test
    public void testPreservationRequestValhalId() {
        PreservationRequest pr = getDefaultPreservationRequest();
        pr.Valhal_ID = null;
        assertFalse(pr.isMessageValid());
        pr.Valhal_ID = "";
        assertFalse(pr.isMessageValid());
        pr.Valhal_ID = defaultValhalId;
        assertTrue(pr.isMessageValid());
    }
    
    @Test
    public void testPreservationRequestModel() {
        PreservationRequest pr = getDefaultPreservationRequest();
        pr.Model = null;
        assertFalse(pr.isMessageValid());
        pr.Model = "";
        assertFalse(pr.isMessageValid());
        pr.Model = defaultModel;
        assertTrue(pr.isMessageValid());
    }

    public PreservationRequest getDefaultPreservationRequest() {
        PreservationRequest pr = new PreservationRequest();
        pr.UUID = UUID.randomUUID().toString();
        pr.Preservation_profile = defaultPreservationProfile;
        pr.Valhal_ID = defaultValhalId;
        pr.Model = defaultModel;
        return pr;
    }
}
