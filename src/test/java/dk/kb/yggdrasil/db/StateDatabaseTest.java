package dk.kb.yggdrasil.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.Metadata;
import dk.kb.yggdrasil.json.PreservationRequest;

public class StateDatabaseTest {

    @Test
    public void test() throws YggdrasilException {
        StateDatabase sd = StateDatabase.getInstance();
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        sd.put("sample_uuid", pr);
        assertTrue(sd.hasEntry("sample_uuid"));
        sd.cleanup();
    }
    
    @Test
    public void testGet() throws YggdrasilException {
        StateDatabase sd = StateDatabase.getInstance();
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        Metadata m = new Metadata();
        m.descMetadata = "Some descriptive metadata";
        m.preservationMetadata = "Some preservation metadata";
        m.provenanceMetadata = "Some provenance metadata";
        m.techMetadata = "Some technical metadata";
        pr.metadata = m;
        sd.put("sample_uuid", pr);
        PreservationRequest df = sd.getRecord("sample_uuid");
        assertEquals("dasdasdsdasd", df.File_UUID);
        assertEquals("Some technical metadata", df.metadata.techMetadata);
        sd.delete("sample_uuid");
        sd.cleanup();
    }

    @Test
    public void testGetOutstanding() throws YggdrasilException {
        StateDatabase sd = StateDatabase.getInstance();
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        sd.put("sample_uuid", pr);
        List<String> list = sd.getOutstandingUUIDS();
        assertTrue("Should have one entry, but has " + list.size(),
                list.size() == 1);
        String df = list.get(0);
        assertEquals("sample_uuid", df);
        sd.cleanup();
    }
    
    
}
