package dk.kb.yggdrasil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for the HTTP response payload.
 */
public class HttpPayload {

    /** Response content body steam. (Remember to close) */
    public InputStream contentBody;

    /** Optional response content body content encoding. */
    public String contentEncoding;

    /** Optional response content type. */
    public String contentType;

    /** Optional response content length. */
    public long contentLength;

    /**
     * Construct a payload object with the supplied parameters. 
     * @param contentBody <code>InputStream</code> with the content body
     * @param contentEncoding optional content encoding
     * @param contentType optional content type
     * @param contentLength optional content length
     */
    public HttpPayload(InputStream contentBody, String contentEncoding, String contentType, long contentLength) {
        this.contentBody = contentBody;
        this.contentEncoding = contentEncoding;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    /**
     * Close the payload object and release any resources associated with it.
     * @throws IOException an i/o exception if an error occurs while closing stream
     */
    public void close() throws IOException {
        if (contentBody != null) {
            contentBody.close();
            contentBody = null;
        }
    }

}
