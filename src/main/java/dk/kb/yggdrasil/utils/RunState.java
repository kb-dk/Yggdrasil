package dk.kb.yggdrasil.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Provide a service that at a given endpoint provide information about the state of Yggdrasil.
 */
public class RunState implements Runnable {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(RunState.class.getName());
    
    /** Server socket */
    private static ServerSocket sock;

    /** Yggdrasil version */
    private final String version;

    /**
     * Constructor - Setup the server socket at host at a specified port. 
     */
    public RunState() {
        version = this.getClass().getPackage().getImplementationVersion();
        final File generalConfigFile = new File("config/yggdrasil.yml");
        
        try {
            sock = new ServerSocket();
            sock.setReuseAddress(true);
            
            // Get host name where the service should run.
            final HostName hostname = new HostName();
            final String hname = hostname.getHostName();

            // Set port
            final Config config = new Config(generalConfigFile);
            final int MONITOR_PORT = config.getMonitorPort();
            
            logger.info("RunState.initialize: " + hname + ":" + MONITOR_PORT);
            
            // Bind service endpoint.
            final SocketAddress endpoint = new InetSocketAddress(hname, MONITOR_PORT);
            if (!sock.isBound()) {
                sock.bind(endpoint);
                }

        } catch (YggdrasilException e) {
            logger.error("Caught exception while getting monitor port form config file", e);
        } catch (IOException e) {
            logger.error("Caught Server Socket exception while starting RunState", e);
        }
    }

    /**
    * This method start the thread and performs all the operations.
    */
    public void run() {
        try {
            /** Wait for connection from client. */
            while (true) {
                final Socket socket = sock.accept();

                /** Open a reader to receive (and ignore the input) */
                final BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                /** Write the status message to the outputstream */
                try {
                    final BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    wr.write("Yggdrasil version: " + version + " is running");
                    wr.flush();
                } catch (IOException e) {
                    logger.error("Caught exception while writting to socket in RunState", e);
                }
                /** Close the inputstream since we really don't care about it */
                rd.close();
                //TimeUnit.SECONDS.sleep(1);
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("RunState Thread interrupted");
                    break;
                  }
            }
        } catch (Exception e) {
            logger.error("Caught exception while running RunState", e);
        }
    }
}
