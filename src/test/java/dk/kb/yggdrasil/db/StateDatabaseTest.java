package dk.kb.yggdrasil.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.Metadata;
import dk.kb.yggdrasil.json.PreservationRequest;

public class StateDatabaseTest {

    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    @Test
    public void test() throws YggdrasilException {
        Config config = new Config(generalConfigFile);
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        PreservationRequest pr = new PreservationRequest();
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        pr.File_UUID = "dasdasdsdasd";
        PreservationRequestState prs = new PreservationRequestState(pr, 
                    State.PRESERVATION_PACKAGE_COMPLETE, UUID_sample);
        sd.put(UUID_sample, prs);
        assertTrue(sd.hasEntry(UUID_sample));
        sd.cleanup();
    }
    
    @Test
    public void testGet() throws YggdrasilException {
        Config config = new Config(generalConfigFile);
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        
        PreservationRequest pr = new PreservationRequest();
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        pr.File_UUID = "dasdasdsdasd";
        Metadata m = new Metadata();
        m.descMetadata = "Some descriptive metadata";
        m.preservationMetadata = "Some preservation metadata";
        m.provenanceMetadata = "Some provenance metadata";
        m.techMetadata = "Some technical metadata";
        pr.metadata = m;

        PreservationRequestState prs = new PreservationRequestState(pr, 
                State.PRESERVATION_REQUEST_RECEIVED, UUID_sample);
        sd.put(UUID_sample, prs);
        PreservationRequestState df = sd.getRecord(UUID_sample);
        assertEquals("dasdasdsdasd", df.getRequest().File_UUID);
        assertEquals("Some technical metadata", df.getRequest().metadata.techMetadata);
        sd.delete(UUID_sample);
        sd.cleanup();
    }

    @Test
    public void testGetOutstanding() throws YggdrasilException {
        Config config = new Config(generalConfigFile);
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        
        PreservationRequestState prs = new PreservationRequestState(pr, 
                State.PRESERVATION_PACKAGE_UPLOAD_SUCCESS, UUID_sample);
       
        sd.put(UUID_sample, prs);
        List<String> list = sd.getOutstandingUUIDS();
        assertTrue("Should have one entry, but has " + list.size(),
                list.size() == 1);
        String df = list.get(0);
        assertEquals(UUID_sample, df);
        sd.cleanup();
    }
}
