package dk.kb.yggdrasil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * Trivial tests to check if the put/get methods work as intended.
 */
@RunWith(JUnit4.class)
public class TestHttpCommunication {

    private HttpCommunication httpCommunication = new HttpCommunication();
    
    @Test
    public void test_httpcommunication() {
        HttpPayload httpPayload = null;
        byte[] contentBody;
        boolean bSuccess;
        WebServer server = null;

        try {
            /*
             * Test invalid url. Assuming that port 65535 is not running a webserver.
             */
            httpPayload = httpCommunication.get("http://localhost:65535/get");
            Assert.assertNull(httpPayload);

            contentBody = "the body".getBytes();
            bSuccess = httpCommunication.post("http://localhost:65535/post", contentBody, "text/plain");
            Assert.assertFalse(bSuccess);
            /*
             * Start webserver.
             */
            server = new WebServer();
            server.start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            /*
             * Invalid requests.
             */
            httpPayload = httpCommunication.get("http://localhost:" + server.port + "/gett");
            Assert.assertNull(httpPayload);

            contentBody = "the body".getBytes();
            bSuccess = httpCommunication.post("http://localhost:" + server.port + "/postt", contentBody, "text/plain");
            Assert.assertFalse(bSuccess);
            /*
             * Valid requests.
             */
            httpPayload = httpCommunication.get("http://localhost:" + server.port + "/get");
            Assert.assertNotNull(httpPayload);

            int read;
            byte[] tmpArr = new byte[1024];
            InputStream in = httpPayload.getContentBody();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while ((read = in.read(tmpArr)) != -1) {
                bout.write(tmpArr, 0, read);
            }
            in.close();

            Assert.assertNull(httpPayload.getContentEncoding());
            Assert.assertEquals("application/x-monkey", httpPayload.getContentType());
            Assert.assertEquals(new Long("I am Jettyman.".getBytes().length), httpPayload.getContentLength());
            Assert.assertArrayEquals("I am Jettyman.".getBytes(), bout.toByteArray());

            contentBody = "the body".getBytes();
            bSuccess = httpCommunication.post("http://localhost:" + server.port + "/post", contentBody, "text/plain");
            Assert.assertTrue(bSuccess);
            

            // Test the post with Http Entity element
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("uuid", "UUID");
            builder.addTextBody("type", "RandomType");

            builder.addBinaryBody("file", contentBody);
            httpCommunication.post("http://localhost:" + server.port + "/post", builder.build());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } finally {
            if (httpPayload != null) {
                try {
                    httpPayload.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail("Unexpected exception!");
                }
                httpPayload = null;
            }
            if (server != null) {
                server.stop();
                server = null;
            }
        }
    }

    /**
     * Defines a simple <code>HttpServlet</code> to validate our get/put methods.
     */
    public static class Servlet extends HttpServlet {
        /**
         * UID.
         */
        private static final long serialVersionUID = -7223254916357089420L;

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String pathInfo = req.getPathInfo();
            String method = req.getMethod();
            if ("/get".equals(pathInfo) && "GET".equals(method)) {
                resp.setStatus(200);
                resp.setContentType("application/x-monkey");
                byte[] contentBody = "I am Jettyman.".getBytes();
                resp.setContentLength(contentBody.length);
                OutputStream out = resp.getOutputStream();
                out.write(contentBody);
                out.flush();
                out.close();
            } else if ("/post".equals(pathInfo) && "POST".equals(method)) {
                int read;
                byte[] tmpArr = new byte[1024];
                InputStream in = req.getInputStream();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                while ((read = in.read(tmpArr)) != -1) {
                    bout.write(tmpArr, 0, read);
                }
                in.close();
                String payload = new String(bout.toByteArray());
                if ("the body".equals(payload)) {
                    resp.setStatus(200);
                } else {
                    resp.sendError(501);
                }
            } else {
                resp.sendError(404);
            }
        }
    }

    /**
     * Set up a small webserver to test HTTP get/put.
     */
    public static class WebServer implements Runnable {

        /** Jetty webserver instance. */
        public Server webServer;
        /** Connector used to get random bind port. */
        public Connector connector;
        /** Random connector port. */
        public int port = -1;

        /**
         * Stop webserver.
         */
        public void stop() {
            try {
                webServer.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Start webserver.
         */
        public void start() {
            new Thread(this, "web-server").start();
        }

        @Override
        public void run() {
            webServer = new Server();
            connector = new SocketConnector();
            webServer.setConnectors(new Connector[] {connector});
            ServletHandler h = new ServletHandler();
            h.addServletWithMapping(Servlet.class, "/*");
            webServer.addHandler(h);
            webServer.addLifeCycleListener(new LifeCycle.Listener() {
                @Override
                public void lifeCycleStarting(LifeCycle lifeCycle) {
                }
                @Override
                public void lifeCycleStarted(LifeCycle lifeCycle) {
                }
                @Override
                public void lifeCycleFailure(LifeCycle lifeCycle, Throwable throwable) {
                }
                @Override
                public void lifeCycleStopping(LifeCycle lifeCycle) {
                }
                @Override
                public void lifeCycleStopped(LifeCycle lifeCycle) {
                }
            });
            try {
                webServer.start();
                port = connector.getLocalPort();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
