package dk.kb.yggdrasil.bitmag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.utils.Base16Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link dk.kb.yggdrasil.bitmag.Bitrepository }
 * Named BitrepositoryTester and not BitrepositoryTest to avoid inclusion in
 * the set of unittests run by Maven.
 */
@RunWith(JUnit4.class)
public class BitrepositoryUtilsTest {
    @Test
    public void testInstantiation() {
        assertNotNull(new BitrepositoryUtils());
    }
    
    @Test
    public void testGetRequestChecksumSpecWithoutSalt() {
        ChecksumSpecTYPE cst = BitrepositoryUtils.getRequestChecksumSpec(ChecksumType.HMAC_MD5, null);
        assertNotNull(cst);
        assertFalse(cst.isSetChecksumSalt());
        assertTrue(cst.isSetChecksumType());
        assertFalse(cst.isSetOtherChecksumType());
        assertEquals(cst.getChecksumType(), ChecksumType.HMAC_MD5);
    }
    
    @Test
    public void testGetRequestChecksumSpecWithSalt() {
        ChecksumSpecTYPE cst = BitrepositoryUtils.getRequestChecksumSpec(ChecksumType.HMAC_MD5, "salt");
        assertNotNull(cst);
        assertTrue(cst.isSetChecksumSalt());
        assertTrue(cst.isSetChecksumType());
        assertFalse(cst.isSetOtherChecksumType());
        assertEquals(cst.getChecksumType(), ChecksumType.HMAC_MD5);
        assertEquals(cst.getChecksumSalt().length, Base16Utils.encodeBase16("salt").length);
    }

    @Test
    public void testComponentId() {
        String componentId = BitrepositoryUtils.generateComponentID();
        assertNotNull(componentId);
        assertFalse(componentId.isEmpty());
    }
    
    @Test
    public void testGetValidationChecksum() throws Exception {
        ChecksumSpecTYPE cst = BitrepositoryUtils.getRequestChecksumSpec(ChecksumType.MD5, null);
        File file = File.createTempFile(this.getClass().getName(), null);
        ChecksumDataForFileTYPE cdfft = BitrepositoryUtils.getValidationChecksum(file, cst);
        assertNotNull(cdfft);
        assertTrue(cdfft.isSetCalculationTimestamp());
        assertTrue(cdfft.isSetChecksumSpec());
        assertTrue(cdfft.isSetChecksumValue());
        assertEquals(cdfft.getChecksumSpec(), cst);
    }
}
