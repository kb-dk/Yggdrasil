package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStructure;
import com.antiaction.common.json.JSONText;

import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.preservation.Preservation;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportResponse;
import dk.kb.yggdrasil.json.preservationimport.Response;
import dk.kb.yggdrasil.json.preservationimport.Security;
import dk.kb.yggdrasil.json.preservationimport.Warc;

@RunWith(JUnit4.class)
public class TestJSONMessaging {
    protected static JSONEncoding json_encoding = JSONEncoding.getJSONEncoding();
    protected static JSONObjectMappings json_om = new JSONObjectMappings();
    protected static JSONText json_text = new JSONText();
    protected static JSONEncoder json_encoder = json_encoding.getJSONEncoder(JSONEncoding.E_UTF8);

    @BeforeClass
    public static void beforeClass() throws Exception {
        json_om.register(PreservationRequest.class);
        json_om.register(PreservationResponse.class);
        json_om.register(PreservationImportRequest.class);
        json_om.register(PreservationImportResponse.class);
    }

    @Test
    public void testJsonMessagingForPreservationRequest() throws Exception {
        PreservationRequest request = new PreservationRequest();
        request.Preservation_profile = "simple";
        request.UUID = "uuid";
        request.Valhal_ID = "Valhal:1";
        request.File_UUID = "fuuid";
        request.Content_URI = "curi";
        request.metadata = "Some metadata";
        request.Model = "work";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONStructure json_struct = json_om.getStructureMarshaller().toJSON(request);
        json_text.encodeJSONtext(json_struct, json_encoder, false, out);
        out.close();
        byte[] requestBytes = out.toByteArray();

        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4);
        request = JSONMessaging.getRequest(in, PreservationRequest.class);
        in.close();
        Assert.assertNotNull(request);
        Assert.assertEquals("simple", request.Preservation_profile);
        Assert.assertEquals("uuid", request.UUID);
        Assert.assertEquals("Valhal:1", request.Valhal_ID);
        Assert.assertEquals("fuuid", request.File_UUID);
        Assert.assertEquals("curi", request.Content_URI);
        Assert.assertEquals("work", request.Model);
        Assert.assertNotNull(request.metadata);
    }
    
    @Test
    public void testJsonMessagingForPreservationResponse() throws Exception {
        PreservationResponse response = new PreservationResponse();
        Preservation preservation = new Preservation();
        response.preservation = preservation;
        response.id = "Valhal:1";
        response.model = "work";
        preservation.preservation_state = "state";
        preservation.preservation_details = "hello";
        preservation.warc_id = "WAHRC!";
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        Assert.assertNotNull(responseBytes);

        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(responseBytes), 4);
        int encoding = JSONEncoding.encoding(in);
        Assert.assertEquals(JSONEncoding.E_UTF8, encoding);

        JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
        JSONStructure json_struct = json_text.decodeJSONtext(in, json_decoder);
        in.close();
        response = json_om.getStructureUnmarshaller().toObject(json_struct, PreservationResponse.class);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.preservation);
        Assert.assertEquals("state", response.preservation.preservation_state);
        Assert.assertEquals("hello", response.preservation.preservation_details);
        Assert.assertEquals("WAHRC!", response.preservation.warc_id);
        Assert.assertEquals("Valhal:1", response.id);
        Assert.assertEquals("work", response.model);
    }

    @Test
    public void testJsonMessagingForPreservationImportRequest() throws Exception {
        Date date = new Date();
        PreservationImportRequest request = new PreservationImportRequest();
        request.preservation_profile = "simple";
        request.security = new Security();
        request.security.checksum = "damm:1";
        request.security.token = "token";
        request.security.token_timeout = date.toString();
        request.type = "FILE";
        request.url = "http://localhost:3000/view_file";
        request.uuid = "uuid";
        request.warc = new Warc();
        request.warc.warc_file_id = "warc_file_id";
        request.warc.warc_offset = "warc_offset";
        request.warc.warc_record_id = "warc_record_id";
        request.warc.warc_record_size = "warc_record_size";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONStructure json_struct = json_om.getStructureMarshaller().toJSON(request);
        json_text.encodeJSONtext(json_struct, json_encoder, false, out);
        out.close();
        byte[] requestBytes = out.toByteArray();

        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4);
        request = JSONMessaging.getRequest(in, PreservationImportRequest.class);
        in.close();
        Assert.assertNotNull(request);
        Assert.assertEquals("simple", request.preservation_profile);
        Assert.assertNotNull(request.security);
        Assert.assertEquals("damm:1", request.security.checksum);
        Assert.assertEquals("token", request.security.token);
        Assert.assertEquals(date.toString(), request.security.token_timeout);
        Assert.assertEquals("FILE", request.type);
        Assert.assertEquals("http://localhost:3000/view_file", request.url);
        Assert.assertEquals("uuid", request.uuid);
        Assert.assertNotNull(request.warc);
        Assert.assertEquals("warc_file_id", request.warc.warc_file_id);
        Assert.assertEquals("warc_offset", request.warc.warc_offset);
        Assert.assertEquals("warc_record_id", request.warc.warc_record_id);
        Assert.assertEquals("warc_record_size", request.warc.warc_record_size);
    }
    
    @Test
    public void testJsonMessagingForPreservationImportResponse() throws Exception {
        Date date = new Date();
        PreservationImportResponse response = new PreservationImportResponse();
        response.response = new Response();
        response.response.date = date.toString();
        response.response.detail = "details";
        response.response.state = "STATE";
        response.type = "FILE";
        response.uuid = "uuid";

        byte[] responseBytes = JSONMessaging.getPreservationImportResponse(response);
        Assert.assertNotNull(responseBytes);

        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(responseBytes), 4);
        int encoding = JSONEncoding.encoding(in);
        Assert.assertEquals(JSONEncoding.E_UTF8, encoding);

        JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
        JSONStructure json_struct = json_text.decodeJSONtext(in, json_decoder);
        in.close();
        response = json_om.getStructureUnmarshaller().toObject(json_struct, PreservationImportResponse.class);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.response);
        Assert.assertEquals(date.toString(), response.response.date);
        Assert.assertEquals("details", response.response.detail);
        Assert.assertEquals("STATE", response.response.state);
        Assert.assertEquals("FILE", response.type);
        Assert.assertEquals("uuid", response.uuid);
    }

}
