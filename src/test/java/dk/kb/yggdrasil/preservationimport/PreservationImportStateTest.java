package dk.kb.yggdrasil.preservationimport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class PreservationImportStateTest {
    @Test
    public void testContentOfStates() throws Exception {
        for(PreservationImportState pis : PreservationImportState.values()) {
            assertNotNull(pis.getDescription());
            assertNotNull(pis.name());
            assertTrue(pis.ordinal() >= 0);
            assertTrue(pis.hasState(pis));
        }
    }

    @Test
    public void testSameState() {
        assertFalse(PreservationImportState.PRESERVATION_IMPORT_FINISHED.hasState(PreservationImportState.PRESERVATION_IMPORT_FAILURE));
        assertTrue(PreservationImportState.PRESERVATION_IMPORT_FINISHED.hasState(PreservationImportState.PRESERVATION_IMPORT_FINISHED));
    }

    @Test
    public void testVerifyStateChange() {
        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED, PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED, PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED);

        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED);

        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FINISHED));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FINISHED);

        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE);

        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE);

        assertTrue(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE));
        PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_FAILURE);
    }

    @Test(expected = ArgumentCheck.class)
    public void testVerifyStateChangeOldStateNull() throws YggdrasilException {
        assertFalse(PreservationImportState.isValidStateChange(null, PreservationImportState.PRESERVATION_IMPORT_FINISHED));
        PreservationImportState.verifyIfValidStateChange(null, PreservationImportState.PRESERVATION_IMPORT_FINISHED);
    }

    @Test(expected = ArgumentCheck.class)
    public void testVerifyStateChangeNewStateNull() throws YggdrasilException {
        assertFalse(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_FINISHED, null));
        PreservationImportState.verifyIfValidStateChange(PreservationImportState.PRESERVATION_IMPORT_FINISHED, null);
    }

    @Test(expected = YggdrasilException.class)
    public void testVerifyStateChangeOldStateFailure() throws YggdrasilException {
        assertFalse(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_FAILURE, PreservationImportState.PRESERVATION_IMPORT_FINISHED));
        PreservationImportState.verifyIfValidStateChange(PreservationImportState.PRESERVATION_IMPORT_FAILURE, PreservationImportState.PRESERVATION_IMPORT_FINISHED);
    }

    @Test(expected = YggdrasilException.class)
    public void testVerifyStateChangeNewStateLowerOrdinalThanOldState() throws YggdrasilException {
        assertFalse(PreservationImportState.isValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED));
        PreservationImportState.verifyIfValidStateChange(PreservationImportState.PRESERVATION_IMPORT_DELIVERY_INITIATED, PreservationImportState.PRESERVATION_IMPORT_REQUEST_RECEIVED_AND_VALIDATED);
    }

    @Test
    public void testIsOkState() {
        for(PreservationImportState pis : PreservationImportState.values()) {
            if(PreservationImportState.getFailStates().contains(pis)) {
                assertFalse(pis.isOkState());
            } else {
                assertTrue(pis.isOkState());
            }
        }
    }

    @Test
    public void testValueOf() {
        for(PreservationImportState pis : PreservationImportState.values()) {
            assertEquals(pis, PreservationImportState.valueOf(pis.name()));
        }
        
    }
}
