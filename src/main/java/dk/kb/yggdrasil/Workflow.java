package dk.kb.yggdrasil;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.PushbackInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.messaging.MqResponse;
import dk.kb.yggdrasil.preservation.PreservationContext;
import dk.kb.yggdrasil.preservation.PreservationRequestHandler;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;
import dk.kb.yggdrasil.xslt.Models;

/**
 * The class handling the workflow, and the updates being sent back to Valhal.
 * We have currently two kind of workflows envisioned.
 *  - A content and metadata workflow, where we package metadata and content into one warcfile.
 *  - A metadata workflow, where the metadata is the only content.
 */
public class Workflow {
    /** The RabbitMQ connection used by this workflow. */
    private MQ mq;
    /** Size of pushback buffer for determining the encoding of the json message. */
    private final static int PUSHBACKBUFFERSIZE = 4;

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(Workflow.class.getName());
    
    private final PreservationRequestHandler preservationRequestHandler;
    
    /**
     * Constructor for the Workflow class.
     * @param rabbitconnector The rabbitmq connector object
     * @param states the StateDatabase
     * @param bitrepository the interface with bitrepository
     * @param config general configuration
     * @param models metadatamodelMapper
     */
    public Workflow(MQ rabbitconnector, StateDatabase states, Bitrepository bitrepository, Config config,
            Models models) {
        ArgumentCheck.checkNotNull(rabbitconnector, "MQ rabbitconnector");
        ArgumentCheck.checkNotNull(states, "StateDatabase states");
        ArgumentCheck.checkNotNull(bitrepository, "Bitrepository bitrepository");
        ArgumentCheck.checkNotNull(config, "Config config");
        ArgumentCheck.checkNotNull(models, "Models models");
        this.mq = rabbitconnector;
        
        PreservationContext context = new PreservationContext(bitrepository, config, states, 
                new RemotePreservationStateUpdater(mq));
        preservationRequestHandler = new PreservationRequestHandler(context, models);
    }

    /**
     * Run this method infinitely.
     * @throws YggdrasilException
     * @throws RabbitException When message queue connection fails.
     */
    public void run() throws YggdrasilException, FileNotFoundException, RabbitException {
        boolean shutdown = false;
        while (!shutdown) {
            PreservationRequest request = null;
            try {
                request = getNextRequest();
            } catch (YggdrasilException e) {
                logger.error("Caught exception while retrieving message from rabbitmq. Skipping message", e);
                continue;
            }

            if (request == null) {
                logger.info("Received shutdown message. Shutting down. ");
                shutdown = true;
                continue;
            }

            preservationRequestHandler.handleRequest(request);
        } 
    }

    /**
     * Wait until the next request arrives on the queue, and then return the request.
     * @return the next request from the queue (returns null, if shutdown message)
     * @throws YggdrasilException If bad messagetype
     * @throws RabbitException When message queue connection fails.
     */
    private PreservationRequest getNextRequest() throws YggdrasilException, RabbitException {
        // TODO Should there be a timeout here?
        MqResponse requestContent = mq.receiveMessageFromQueue(
                mq.getSettings().getPreservationDestination());
        final String messageType = requestContent.getMessageType();
        if (messageType == null) {
            throw new YggdrasilException("'null' messagetype is not handled. message ignored ");
        } else if (messageType.equalsIgnoreCase(MQ.SHUTDOWN_MESSAGE_TYPE)) {
            logger.warn("Shutdown message received");
            // Shutdown message received
            return null;
        } else if (messageType.equalsIgnoreCase(MQ.PRESERVATIONREQUEST_MESSAGE_TYPE)) {
            PreservationRequest request = JSONMessaging.getPreservationRequest(
                    new PushbackInputStream(new ByteArrayInputStream(requestContent.getPayload())
                    , PUSHBACKBUFFERSIZE));
            return request;
        } else {
            throw new YggdrasilException("The message type '"
                    + messageType + "' is not handled by Yggdrasil.");
        }
    }
}
