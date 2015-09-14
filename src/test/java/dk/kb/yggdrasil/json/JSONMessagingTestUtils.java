package dk.kb.yggdrasil.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONStructure;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;

public class JSONMessagingTestUtils extends JSONMessaging {
    public static PreservationResponse getPreservationResponse(PushbackInputStream in) throws YggdrasilException {
        try {
            int encoding = JSONEncoding.encoding(in);
            JSONDecoder json_decoder = JSON_ENCODING.getJSONDecoder(encoding);
            JSONStructure json_object = JSON_TEXT.decodeJSONtext(in, json_decoder);
            PreservationResponse response = JSON_OM.getStructureUnmarshaller().toObject(json_object, PreservationResponse.class);
            return response;
        } catch (IOException e) {
            throw new YggdrasilException("IOException unmarshalling preservation response.", e);
        } catch (JSONException e) {
            throw new YggdrasilException("JSONException unmarshalling preservation response.", e);
        }
    }
    
    public static byte[] getPreservationRequest(PreservationRequest request) throws YggdrasilException {
        try {
            JSONEncoder json_encoder = JSON_ENCODING.getJSONEncoder(JSONEncoding.E_UTF8);
            JSONStructure json_object = JSON_OM.getStructureMarshaller().toJSON(request);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            JSON_TEXT.encodeJSONtext(json_object, json_encoder, false, bout);
            bout.close();
            byte[] content = bout.toByteArray();
            return content;
        } catch (Exception e) {
            throw new YggdrasilException("Error while marshalling preservation response.", e);
        }
    }
    
    public static byte[] getPreservationImportRequest(PreservationImportRequest request) throws YggdrasilException {
        try {
            JSONEncoder json_encoder = JSON_ENCODING.getJSONEncoder(JSONEncoding.E_UTF8);
            JSONStructure json_object = JSON_OM.getStructureMarshaller().toJSON(request);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            JSON_TEXT.encodeJSONtext(json_object, json_encoder, false, bout);
            bout.close();
            byte[] content = bout.toByteArray();
            return content;
        } catch (Exception e) {
            throw new YggdrasilException("Error while marshalling preservation response.", e);
        }
    }
}
