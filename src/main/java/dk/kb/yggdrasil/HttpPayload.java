package dk.kb.yggdrasil;

import java.io.IOException;
import java.io.InputStream;

public class HttpPayload {

	public InputStream contentBody;

	public String contentEncoding;

	public String contentType;

	public long contentLength;

	public HttpPayload(InputStream contentBody, String contentEncoding, String contentType, long contentLength) {
		this.contentBody = contentBody;
		this.contentEncoding = contentEncoding;
		this.contentType = contentType;
		this.contentLength = contentLength;
	}

	public void close() throws IOException {
		if (contentBody != null) {
			contentBody.close();
			contentBody = null;
		}
	}

}
