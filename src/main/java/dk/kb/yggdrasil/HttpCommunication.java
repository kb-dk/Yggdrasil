package dk.kb.yggdrasil;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpCommunication {

	private static final Logger logger = Logger.getLogger(HttpCommunication.class.getName());

	public static InputStream get(String url) {
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
	            if (responseCode != 200) {
	            	if (in != null) {
	            		in.close();
	            		in = null;
	            	}
	                logger.log(Level.SEVERE, "Http request resulted in status code '" 
	                		+ responseCode + "'. (" + url + ")");
	            }
			} else {
	            logger.log(Level.SEVERE, "Could not connect to " + url + ". No response received. ");
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
		return in;
	}

	public static boolean put(String url, byte[] contentBody, String contentType) {
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
	                logger.log(Level.WARNING, "Http request resulted in status code '" 
	                        + responseCode + "'. (" + url + ")");
	            }
	            if (in != null) {
	            	in.close();
	            	in = null;
	            }
			} else {
	            logger.log(Level.WARNING, "Could not connect to " + url + ". No response received. ");
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
		return bSuccess;
	}

}
