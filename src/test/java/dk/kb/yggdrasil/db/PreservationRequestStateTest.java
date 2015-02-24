package dk.kb.yggdrasil.db;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;

@RunWith(JUnit4.class)
public class PreservationRequestStateTest {

    @Test
    public void testConstructor() {
        PreservationRequest pr = new PreservationRequest();
        String uuid = UUID.randomUUID().toString();
        State preservationState = State.PRESERVATION_REQUEST_RECEIVED;
        PreservationRequestState prs 
            = new PreservationRequestState(pr, preservationState, uuid);
        assertTrue(pr.equals(prs.getRequest()));
        assertTrue(preservationState.equals(prs.getState()));
        assertTrue(uuid.equals(prs.getUUID()));
    }

    @Test
    public void testSetState() throws YggdrasilException {
        PreservationRequest pr = new PreservationRequest();
        String uuid = UUID.randomUUID().toString();
        State preservationState = State.PRESERVATION_REQUEST_RECEIVED;
        PreservationRequestState prs 
            = new PreservationRequestState(pr, preservationState, uuid);
        assertTrue(preservationState.equals(prs.getState()));
        prs.setState(State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY);
        assertTrue(State.PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY.equals(
                prs.getState()));
    }
    
    @Test
    public void testSetAndGetFileMethods() throws YggdrasilException, IOException {
        PreservationRequest pr = new PreservationRequest();
        String uuid = UUID.randomUUID().toString();
        State preservationState = State.PRESERVATION_REQUEST_RECEIVED;
        PreservationRequestState prs 
            = new PreservationRequestState(pr, preservationState, uuid);
        assertTrue(preservationState.equals(prs.getState()));
        assertFalse(prs.getContentPayload()!= null);
        assertFalse(prs.getMetadataPayload()!= null);
        assertFalse(prs.getWarcId() != null);
        
        File nonexistingFile = new File(UUID.randomUUID().toString());
        assertFalse(nonexistingFile.exists());
        
        try {
            prs.setContentPayload(nonexistingFile);
            fail("Should have thrown ArgumentCheck exception on nonexisting file");
        } catch (ArgumentCheck e) {
            // Expected
        }
        
        try {
            prs.setMetadataPayload(nonexistingFile);
            fail("Should have thrown ArgumentCheck exception on nonexisting file");
        } catch (ArgumentCheck e) {
            // Expected
        }
        
        try {
            prs.setUploadPackage(nonexistingFile);
            fail("Should have thrown ArgumentCheck exception on nonexisting file");
        } catch (ArgumentCheck e) {
            // Expected
        }
        File existingFileOne = new File(UUID.randomUUID().toString());
        File existingFileTwo = new File(UUID.randomUUID().toString());
        File existingFileThree = new File(UUID.randomUUID().toString());
        try {
            existingFileOne.createNewFile();
            existingFileTwo.createNewFile();
            existingFileThree.createNewFile();
            assertTrue(existingFileOne.exists());
            assertTrue(existingFileTwo.exists());
            assertTrue(existingFileThree.exists());

            prs.setContentPayload(existingFileOne);
            prs.setMetadataPayload(existingFileTwo);
            prs.setUploadPackage(existingFileThree);
            assertTrue(existingFileOne.equals(prs.getContentPayload()));
            assertTrue(existingFileTwo.equals(prs.getMetadataPayload()));
            assertTrue(existingFileThree.getName().equals(prs.getWarcId()));
        } finally {
            existingFileOne.delete();
            existingFileTwo.delete();
            existingFileThree.delete();
        }
        
    }

    
    
    
    
}
