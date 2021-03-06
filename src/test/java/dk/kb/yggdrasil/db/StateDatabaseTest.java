package dk.kb.yggdrasil.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.preservation.PreservationState;

@RunWith(JUnit4.class)
public class StateDatabaseTest {

    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    @Test
    public void test() throws YggdrasilException, IOException {
        YggdrasilConfig config = new YggdrasilConfig(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        PreservationRequest pr = new PreservationRequest();
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        pr.File_UUID = "dasdasdsdasd";
        PreservationRequestState prs = new PreservationRequestState(pr, 
                    PreservationState.PRESERVATION_PACKAGE_COMPLETE, UUID_sample);
        sd.putPreservationRecord(UUID_sample, prs);
        assertTrue(sd.hasPreservationEntry(UUID_sample));
        sd.cleanup();
    }
    
    @Test
    public void testGet() throws YggdrasilException, IOException {
        YggdrasilConfig config = new YggdrasilConfig(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        
        PreservationRequest pr = new PreservationRequest();
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        pr.File_UUID = "dasdasdsdasd";
        pr.metadata = "Some technical metadata";

        PreservationRequestState prs = new PreservationRequestState(pr, 
                PreservationState.PRESERVATION_REQUEST_RECEIVED, UUID_sample);
        sd.putPreservationRecord(UUID_sample, prs);
        PreservationRequestState df = sd.getPreservationRecord(UUID_sample);
        assertEquals("dasdasdsdasd", df.getRequest().File_UUID);
        assertEquals("Some technical metadata", df.getRequest().metadata);
        sd.delete(UUID_sample);
        sd.cleanup();
    }

    @Test
    public void testGetOutstanding() throws YggdrasilException, IOException {
        YggdrasilConfig config = new YggdrasilConfig(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());
        StateDatabase sd = new StateDatabase(config.getDatabaseDir());
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        String UUID_sample = "sample_uuid";
        pr.UUID = UUID_sample;
        
        PreservationRequestState prs = new PreservationRequestState(pr, 
                PreservationState.PRESERVATION_PACKAGE_UPLOAD_SUCCESS, UUID_sample);
       
        sd.putPreservationRecord(UUID_sample, prs);
        List<String> list = sd.getOutstandingUUIDS();
        assertTrue("Should have one entry, but has " + list.size(),
                list.size() == 1);
        String df = list.get(0);
        assertEquals(UUID_sample, df);
        sd.cleanup();
    }
}
