package dk.kb.yggdrasil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TeatHttpCommunication {

	@Test
	public void test_httpcommunication() {
		HttpPayload httpPayload = HttpCommunication.get("http://localhost:8080/get");
		Assert.assertNull(httpPayload);

		byte[] contentBody = "the body".getBytes();
		boolean bSuccess = HttpCommunication.put("http://localhost:8080/put", contentBody, "text/plain");
		Assert.assertFalse(bSuccess);
	}

}
