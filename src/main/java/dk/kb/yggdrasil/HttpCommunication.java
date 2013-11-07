package dk.kb.yggdrasil;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small class to send a HTTP GET or PUT request to a given URL.
 */
public class HttpCommunication {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(HttpCommunication.class.getName());

    /**
     * Send a HTTP GET request and return the result, if any, to the caller.
     * @param url the url to send a GET request to
     * @return HTTP response content body or null
     */
    public static HttpPayload get(String url) {
    	if (url == null || url.length() == 0) {
    		throw new IllegalArgumentException("'url' is null or empty.");
    	}
        HttpPayload httpResponse = null;
        InputStream in = null;
        try {
            /*
             * HTTP request.
             */
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(url);
            /*
             * HTTP response.
             */
            HttpResponse response = httpClient.execute(getRequest);
            if (response != null) {
                int responseCode = response.getStatusLine().getStatusCode();
                HttpEntity responseEntity = response.getEntity();
                in = responseEntity.getContent();
                if (responseCode == 200) {
                    Header contentEncodingHeader = responseEntity.getContentEncoding();
                    String contentEncoding = null;
                    if (contentEncodingHeader != null) {
                        contentEncoding = contentEncodingHeader.getValue();
                    }
                    Header contentTypeHeader = responseEntity.getContentType();
                    String contentType = null;
                    if (contentTypeHeader != null) {
                        contentType = contentTypeHeader.getValue();
                    }
                    httpResponse = new HttpPayload(in, contentEncoding, 
                            contentType, responseEntity.getContentLength());
                } else {
                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    logger.error("Http request resulted in status code '" 
                            + responseCode + "'. (" + url + ")");
                }
            } else {
                logger.error("Could not connect to '" + url + "'. No response received. ");
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        return httpResponse;
    }

    /**
     * Send a HTTP PUT request including content body and return the result
     * of the operation to the caller.
     * @param url the url to send a PUT request to
     * @param contentBody content body to send to server
     * @param contentType content type of content body
     * @return boolean indicating success or failure
     */
    public static boolean put(String url, byte[] contentBody, String contentType) {
    	if (url == null || url.length() == 0) {
    		throw new IllegalArgumentException("'url' is null or empty.");
    	}
    	if (contentBody == null) {
    		throw new IllegalArgumentException("'contentBody' is null.");
    	}
    	if (contentType == null || contentType.length() == 0) {
    		throw new IllegalArgumentException("'contentType' is null or empty.");
    	}
        boolean bSuccess = false;
        try {
            /*
             * HTTP request.
             */
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPut putRequest = new HttpPut(url);
            StringEntity putEntity = new StringEntity(new String(contentBody));
            putEntity.setContentType(contentType);
            putRequest.setEntity(putEntity);
            /*
             * HTTP response.
             */
            HttpResponse response = httpClient.execute(putRequest);
            if (response != null) {
                int responseCode = response.getStatusLine().getStatusCode();
                HttpEntity responseEntity = response.getEntity();
                InputStream in = responseEntity.getContent();
                if (responseCode == 200) {
                    bSuccess = true;
                } else {
                    logger.warn("Http request resulted in status code '" 
                            + responseCode + "'. (" + url + ")");
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
            } else {
                logger.warn("Could not connect to '" + url + "'. No response received. ");
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        return bSuccess;
    }

}