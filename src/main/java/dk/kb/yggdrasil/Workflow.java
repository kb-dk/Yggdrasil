package dk.kb.yggdrasil;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.bitmag.Bitrepository;
import dk.kb.yggdrasil.config.Models;
import dk.kb.yggdrasil.config.RequestHandlerContext;
import dk.kb.yggdrasil.config.YggdrasilConfig;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MessageRequestHandler;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.messaging.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.preservation.PreservationRequestHandler;
import dk.kb.yggdrasil.preservationimport.PreservationImportRequestHandler;

/**
 * The class receiving and initiating the workflows for the different kinds of requests.
 */
public class Workflow {
    /** The RabbitMQ connection used by this workflow. */
    private MQ mq;

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Workflow.class.getName());
    /** The mapping between message type and message request handlers.*/
    private final Map<String, MessageRequestHandler> requestHandlers;
    
    /** Whether or not to shutdown. */
    private boolean shutdown = false;
    
    /**
     * Constructor for the Workflow class.
     * @param rabbitconnector The rabbitmq connector object
     * @param states the StateDatabase
     * @param bitrepository the interface with bitrepository
     * @param config general configuration
     * @param models metadatamodelMapper
     * @param httpCommunication The httpCommunication.
     * @param updater The remote preservation state updater.
     */
    public Workflow(MQ rabbitconnector, StateDatabase states, Bitrepository bitrepository, YggdrasilConfig config,
            Models models, HttpCommunication httpCommunication, RemotePreservationStateUpdater updater) {
        ArgumentCheck.checkNotNull(rabbitconnector, "MQ rabbitconnector");
        ArgumentCheck.checkNotNull(states, "StateDatabase states");
        ArgumentCheck.checkNotNull(bitrepository, "Bitrepository bitrepository");
        ArgumentCheck.checkNotNull(config, "Config config");
        ArgumentCheck.checkNotNull(models, "Models models");
        this.mq = rabbitconnector;
        
        RequestHandlerContext context = new RequestHandlerContext(bitrepository, config, states, 
                updater, httpCommunication);
        requestHandlers = new HashMap<String, MessageRequestHandler>();
        requestHandlers.put(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE.toUpperCase(), 
                new PreservationRequestHandler(context, models));
        requestHandlers.put(MQ.IMPORTREQUEST_MESSAGE_TYPE.toUpperCase(), 
                new PreservationImportRequestHandler(context));
    }

    /**
     * Run this method infinitely.
     * @throws YggdrasilException If a preservation request cannot be handled.
     * @throws RabbitException When message queue connection fails.
     */
    public void run() throws YggdrasilException, RabbitException {
        RequestReceiver requestReceiver = new RequestReceiver(mq.getSettings().getPreservationDestination());
        RequestReceiver shutdownReceiver = new RequestReceiver(mq.getSettings().getShutdownDestination());

        requestReceiver.run();
        shutdownReceiver.run();

        while (!shutdown) {
            try {
                synchronized(this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                logger.debug("Ignore interruption.", e);
            }
        }

        synchronized(requestReceiver) {
            requestReceiver.notify();
        }
        synchronized(shutdownReceiver) {
            shutdownReceiver.notify();
        }
    }

    class RequestReceiver extends Thread {
        protected String queue;
        RequestReceiver(String queue) {
            this.queue = queue;
        }
        
        public void run() {
            while (!shutdown) {
                try {
                    this.handleNextRequest();
                } catch (YggdrasilException e) {
                    logger.error("Caught exception while retrieving message from rabbitmq. Skipping message", e);
                    continue;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        
        /**
         * Wait until the next request arrives on the queue and handle it responsively.
         * @return whether a shutdown message was received.
         * @throws YggdrasilException If bad messagetype
         * @throws RabbitException When message queue connection fails.
         */
        private void handleNextRequest() throws YggdrasilException, RabbitException {
            MqResponse requestContent = mq.receiveMessageFromQueue(queue);
            String messageType = (requestContent == null ? null : requestContent.getMessageType());
            if (requestContent == null || messageType == null) {
                throw new YggdrasilException("'null' messagetype is not handled. message ignored ");
            } else if (messageType.equalsIgnoreCase(MQ.SHUTDOWN_MESSAGE_TYPE)) {
                terminate();
            } else if (requestHandlers.containsKey(messageType.toUpperCase())) {
                MessageRequestHandler mrh = requestHandlers.get(messageType.toUpperCase());
                mrh.handleRequest(mrh.extractRequest(requestContent.getPayload()));
            } else {
                throw new YggdrasilException("The message type '"
                        + messageType + "' is not handled by Yggdrasil.");
            }
        }
        
        /**
         * Shutting down.
         */
        private synchronized void terminate() {
            logger.warn("Shutdown message received");
            shutdown = true;
            this.notifyAll();
        }
    }
}
