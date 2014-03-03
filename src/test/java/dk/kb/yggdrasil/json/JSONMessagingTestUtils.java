package dk.kb.yggdrasil.json;

import java.io.IOException;
import java.io.PushbackInputStream;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONStructure;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

public class JSONMessagingTestUtils extends JSONMessaging {
    public static PreservationResponse getPreservationResponse(PushbackInputStream in) throws YggdrasilException {
        try {
            int encoding = JSONEncoding.encoding(in);
            JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
            JSONStructure json_object = json_text.decodeJSONtext(in, json_decoder);
            PreservationResponse response = json_om.getStructureUnmarshaller().toObject(json_object, PreservationResponse.class);
            return response;
        } catch (IOException e) {
            throw new YggdrasilException("IOException unmarshalling preservation response.", e);
        } catch (JSONException e) {
            throw new YggdrasilException("JSONException unmarshalling preservation response.", e);
        }
    }
}
