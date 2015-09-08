package dk.kb.yggdrasil.json;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportResponse;
import dk.kb.yggdrasil.json.preservationimport.Response;
import dk.kb.yggdrasil.json.preservationimport.Warc;
import dk.kb.yggdrasil.preservationimport.PreservationImportState;

@RunWith(JUnit4.class)
public class PreservationImportRequestTest {

    private String defaultPreservationProfile = "simple";
    private String defaultUrl = "http://url.url/";
    private String defaultType = "METADATA";
    private String defaultWarcFileId = "WARC_FILE_ID";
    private String defaultWarcRecordId = "WARC_RECORD_ID";

    @Test
    public void testDefaultPreservationImport() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestUUID() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.uuid = null;
        assertFalse(pr.isMessageValid());
        pr.uuid = "";
        assertFalse(pr.isMessageValid());
        pr.uuid = UUID.randomUUID().toString();
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestType() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.type = null;
        assertFalse(pr.isMessageValid());
        pr.type = "";
        assertFalse(pr.isMessageValid());
        pr.type = defaultType;
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestPreservationProfile() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.preservation_profile = null;
        assertFalse(pr.isMessageValid());
        pr.preservation_profile = "";
        assertFalse(pr.isMessageValid());
        pr.preservation_profile = defaultPreservationProfile;
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestUrl() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.url = null;
        assertFalse(pr.isMessageValid());
        pr.url = "";
        assertFalse(pr.isMessageValid());
        pr.url = defaultUrl;
        assertTrue(pr.isMessageValid());
    }
    
    @Test
    public void testPreservationImportRequestWarc() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.warc = null;
        assertFalse(pr.isMessageValid());
        pr.warc = new Warc();
        pr.warc.warc_file_id = defaultWarcFileId;
        pr.warc.warc_record_id = defaultWarcRecordId;
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestWarcFileId() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.warc.warc_file_id = null;
        assertFalse(pr.isMessageValid());
        pr.warc.warc_file_id = "";
        assertFalse(pr.isMessageValid());
        pr.warc.warc_file_id = defaultWarcFileId;
        assertTrue(pr.isMessageValid());
    }

    @Test
    public void testPreservationImportRequestWarcRecordId() {
        PreservationImportRequest pr = getDefaultPreservationImportRequest();
        pr.warc.warc_record_id = null;
        assertFalse(pr.isMessageValid());
        pr.warc.warc_record_id = "";
        assertFalse(pr.isMessageValid());
        pr.warc.warc_record_id = defaultWarcRecordId;
        assertTrue(pr.isMessageValid());
    }
    
    public PreservationImportRequest getDefaultPreservationImportRequest() {
        PreservationImportRequest pr = new PreservationImportRequest();
        pr.uuid = UUID.randomUUID().toString();
        pr.preservation_profile = defaultPreservationProfile;
        pr.url = defaultUrl;
        pr.type = defaultType;
        pr.warc = new Warc();
        pr.warc.warc_file_id = defaultWarcFileId;
        pr.warc.warc_record_id = defaultWarcRecordId;
        
        return pr;
    }
    
    @Test
    public void testPreservationImportResponse() {
        PreservationImportResponse response = new PreservationImportResponse();
        response.uuid = UUID.randomUUID().toString(); 
        response.type = defaultType;
        response.response = new Response();
        response.response.date = new Date().toString();
        response.response.detail = PreservationImportState.PRESERVATION_IMPORT_FINISHED.getDescription();
        response.response.state = PreservationImportState.PRESERVATION_IMPORT_FINISHED.name();
    }
}
