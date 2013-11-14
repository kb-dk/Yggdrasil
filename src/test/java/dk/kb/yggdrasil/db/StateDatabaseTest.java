package dk.kb.yggdrasil.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
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
        sd.put("sample_uuid", pr);
        String df = sd.getRecord("sample_uuid");
        assertEquals("dasdasdsdasd", df);
        sd.cleanup();
    }

    @Ignore
    @Test
    public void testGetOutstanding() throws YggdrasilException {
        StateDatabase sd = StateDatabase.getInstance();
        PreservationRequest pr = new PreservationRequest();
        pr.File_UUID = "dasdasdsdasd";
        sd.put("sample_uuid", pr);
        List<String> list = sd.getOutstandingUUIDS();
        assertTrue(list != null && list.size() == 1);
        String df = list.get(0);
        assertEquals("sample_uuid", df);
        sd.cleanup();
    }
    
    
}
