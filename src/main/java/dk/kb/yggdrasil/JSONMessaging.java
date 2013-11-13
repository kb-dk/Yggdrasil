package dk.kb.yggdrasil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStructure;
import com.antiaction.common.json.JSONText;
import com.antiaction.common.json.annotation.JSONNullable;

public class JSONMessaging {

    public static class PreservationRequest {

        public String UUID;

        public String Preservation_profile;

        public String Update_URI;

        @JSONNullable
        public String File_UUID;

        @JSONNullable
        public String Content_URI;

        public Metadata metadata;

    }

    public static class Metadata {

        @JSONNullable
        public String descMetadata;

        @JSONNullable
        public String provenanceMetadata;

        @JSONNullable
        public String preservationMetadata;

        @JSONNullable
        public String techMetadata;

    }

    public static class PreservationResponse {

         public Preservation preservation;

    }

    public static class Preservation {

        public String preservation_state;

        public String preservation_details;

        @JSONNullable
        public String warc_id;

    }

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(JSONMessaging.class.getName());

    /** JSON encoding encoder/decoder dispatcher. */
    private static JSONEncoding json_encoding = JSONEncoding.getJSONEncoding();

    /** JSON object mapping worker. */
    private  static JSONObjectMappings json_om = new JSONObjectMappings();

    private static JSONText json_text;

    static {
        json_text = new JSONText();
        try {
            json_om.register(PreservationRequest.class);
            json_om.register(PreservationResponse.class);
        } catch (JSONException e) {
            logger.error(e.toString(), e);
        }
    }

    public static PreservationRequest getPreservationRequest(PushbackInputStream in) throws IOException, JSONException {
        int encoding = JSONEncoding.encoding(in);
        JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
        JSONStructure json_object = json_text.decodeJSONtext(in, json_decoder);
        PreservationRequest request = json_om.getStructureUnmarshaller().toObject(json_object, PreservationRequest.class);
        return request;
    }

    public static byte[] getPreservationResponse(PreservationResponse response) throws IOException, JSONException {
        JSONEncoder json_encoder = json_encoding.getJSONEncoder(JSONEncoding.E_UTF8);
        JSONStructure json_object = json_om.getStructureMarshaller().toJSON(response);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        json_text.encodeJSONtext(json_object, json_encoder, false, bout);
        bout.close();
        byte[] content = bout.toByteArray();
        return content;
    }

}
