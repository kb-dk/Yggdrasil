package dk.kb.yggdrasil.json;

import java.io.ByteArrayOutputStream;
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

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportResponse;

/**
 * Small class for marshalling JSON messages to/from Valhal.
 * Also includes java class representations of the data.
 */
public class JSONMessaging {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(JSONMessaging.class.getName());

    /** JSON encoding encoder/decoder dispatcher. */
    protected static final JSONEncoding JSON_ENCODING = JSONEncoding.getJSONEncoding();

    /** JSON object mapping worker. */
    protected static final JSONObjectMappings JSON_OM = new JSONObjectMappings();

    /** JSON decoder/encoder. */
    protected static final JSONText JSON_TEXT;

    /**
     * Initialize JSON marshaller.
     */
    static {
        JSON_TEXT = new JSONText();
        try {
            JSON_OM.register(PreservationRequest.class);
            JSON_OM.register(PreservationResponse.class);
            JSON_OM.register(PreservationImportRequest.class);
            JSON_OM.register(PreservationImportResponse.class);
        } catch (JSONException e) {
            logger.error(e.toString(), e);
        }
    }

    /**
     * Convert JSON data into a preservation request object.
     * @param in <code>InputStream</code> containing JSON data
     * @return preservation request object representation
     * @throws YggdrasilException if an I/O error occurs while unmashalling
     */
    public static PreservationRequest getPreservationRequest(PushbackInputStream in) throws YggdrasilException {
        try {
            int encoding = JSONEncoding.encoding(in);
            JSONDecoder json_decoder = JSON_ENCODING.getJSONDecoder(encoding);
            JSONStructure json_object = JSON_TEXT.decodeJSONtext(in, json_decoder);
            PreservationRequest request = JSON_OM.getStructureUnmarshaller().toObject(json_object, 
                    PreservationRequest.class);
            return request;
        } catch (Exception e) {
            throw new YggdrasilException("Error while unmarshalling preservation request.", e);
        }
    }

    /**
     * Convert JSON data into a preservation import request object.
     * @param in <code>InputStream</code> containing JSON data
     * @return preservation import request object representation
     * @throws YggdrasilException if an I/O error occurs while unmashalling
     */
    public static PreservationImportRequest getPreservationImportRequest(PushbackInputStream in) throws YggdrasilException {
        try {
            int encoding = JSONEncoding.encoding(in);
            JSONDecoder json_decoder = JSON_ENCODING.getJSONDecoder(encoding);
            JSONStructure json_object = JSON_TEXT.decodeJSONtext(in, json_decoder);
            PreservationImportRequest request = JSON_OM.getStructureUnmarshaller().toObject(json_object, 
                    PreservationImportRequest.class);
            return request;
        } catch (Exception e) {
            throw new YggdrasilException("Error while unmarshalling preservation request.", e);
        }
    }

    /**
     * Convert preservation response object into JSON data.
     * @param response preservation response object
     * @return preservation response as JSON data
     * @throws YggdrasilException if an I/O error occurs while marshalling
     */
    public static byte[] getPreservationResponse(PreservationResponse response) throws YggdrasilException {
        try {
            JSONEncoder json_encoder = JSON_ENCODING.getJSONEncoder(JSONEncoding.E_UTF8);
            JSONStructure json_object = JSON_OM.getStructureMarshaller().toJSON(response);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            JSON_TEXT.encodeJSONtext(json_object, json_encoder, false, bout);
            bout.close();
            byte[] content = bout.toByteArray();
            return content;
        } catch (Exception e) {
            throw new YggdrasilException("Error while marshalling preservation response.", e);
        }
    }


    /**
     * Convert preservation import response object into JSON data.
     * @param response preservation import response object
     * @return preservation response as JSON data
     * @throws YggdrasilException if an I/O error occurs while marshalling
     */
    public static byte[] getPreservationImportResponse(PreservationImportResponse response) throws YggdrasilException {
        try {
            JSONEncoder json_encoder = JSON_ENCODING.getJSONEncoder(JSONEncoding.E_UTF8);
            JSONStructure json_object = JSON_OM.getStructureMarshaller().toJSON(response);
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
