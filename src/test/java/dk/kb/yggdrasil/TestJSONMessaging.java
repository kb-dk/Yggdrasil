package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStructure;
import com.antiaction.common.json.JSONText;

import dk.kb.yggdrasil.JSONMessaging.Metadata;
import dk.kb.yggdrasil.JSONMessaging.Preservation;
import dk.kb.yggdrasil.JSONMessaging.PreservationRequest;
import dk.kb.yggdrasil.JSONMessaging.PreservationResponse;

@RunWith(JUnit4.class)
public class TestJSONMessaging {

    @Test
    public void test_jsonmessaging() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PushbackInputStream in;
        JSONStructure json_struct;

        JSONEncoding json_encoding = JSONEncoding.getJSONEncoding();
        JSONObjectMappings json_om = new JSONObjectMappings();
        JSONText json_text = new JSONText();
        try {
            json_om.register(PreservationRequest.class);
            json_om.register(PreservationResponse.class);
        } catch (JSONException e) {
			e.printStackTrace();
        	Assert.fail("Unexpected exception!");
        }

        JSONEncoder json_encoder = json_encoding.getJSONEncoder(JSONEncoding.E_UTF8);

        PreservationRequest request = new PreservationRequest();
        request.Preservation_profile = "simple";
        request.UUID = "uuid";
        request.Update_URI = "uuuri";
        request.File_UUID = "fuuid";
        request.Content_URI = "curi";
        request.metadata = new Metadata();
        request.metadata.descMetadata = "desc";
        request.metadata.preservationMetadata = "pres";
        request.metadata.provenanceMetadata = "provo";
        request.metadata.techMetadata = "techko";

        try {
            out.reset();
			json_struct = json_om.getStructureMarshaller().toJSON(request);
	        json_text.encodeJSONtext(json_struct, json_encoder, false, out);
	        out.close();
	        byte[] requestBytes = out.toByteArray();

	        in = new PushbackInputStream(new ByteArrayInputStream(requestBytes), 4);
	        request = JSONMessaging.getPreservationRequest(in);
	        in.close();
	        Assert.assertNotNull(request);
	        Assert.assertEquals("simple", request.Preservation_profile);
	        Assert.assertEquals("uuid", request.UUID);
	        Assert.assertEquals("uuuri", request.Update_URI);
	        Assert.assertEquals("fuuid", request.File_UUID);
	        Assert.assertEquals("curi", request.Content_URI);
	        Assert.assertNotNull(request.metadata);
	        Assert.assertEquals("desc", request.metadata.descMetadata);
	        Assert.assertEquals("pres", request.metadata.preservationMetadata);
	        Assert.assertEquals("provo", request.metadata.provenanceMetadata);
	        Assert.assertEquals("techko", request.metadata.techMetadata);

	        PreservationResponse response = new PreservationResponse();
	        Preservation preservation = new Preservation();
	        response.preservation = preservation;
	        preservation.preservation_state = "state";
	        preservation.preservation_details = "hello";
	        preservation.warc_id = "WAHRC!";
	        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
	        Assert.assertNotNull(responseBytes);

	        in = new PushbackInputStream(new ByteArrayInputStream(responseBytes), 4);
	        int encoding = JSONEncoding.encoding(in);
	        Assert.assertEquals(JSONEncoding.E_UTF8, encoding);

	        JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
	        json_struct = json_text.decodeJSONtext(in, json_decoder);
	        in.close();
	        response = json_om.getStructureUnmarshaller().toObject(json_struct, PreservationResponse.class);
	        Assert.assertNotNull(response);
	        Assert.assertNotNull(response.preservation);
	        Assert.assertEquals("state", response.preservation.preservation_state);
	        Assert.assertEquals("hello", response.preservation.preservation_details);
	        Assert.assertEquals("WAHRC!", response.preservation.warc_id);
        } catch (JSONException e) {
			e.printStackTrace();
        	Assert.fail("Unexpected exception!");
		} catch (IOException e) {
			e.printStackTrace();
        	Assert.fail("Unexpected exception!");
		}
    }

}
