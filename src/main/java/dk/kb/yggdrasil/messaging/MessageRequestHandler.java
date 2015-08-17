package dk.kb.yggdrasil.messaging;

import dk.kb.yggdrasil.exceptions.YggdrasilException;


/**
 * Abstract message request handler.
 * @param <T> The class for the message request to be handled.
 */
public abstract class MessageRequestHandler<T> {
    /** Size of pushback buffer for determining the encoding of the json message. */
    protected static final int PUSHBACKBUFFERSIZE = 4;

    /**
     * Handles the request for this message request handler.
     * @param request The request of the type for this message request handler.
     * @throws YggdrasilException If something goes wrong.
     */
    public abstract void handleRequest(T request) throws YggdrasilException;
    
    /**
     * Extract the request from a byte array.
     * @param b The byte array to extract the 
     * @return The request.
     * @throws YggdrasilException If the request cannot be extracted.
     */
    public abstract T extractRequest(byte[] b) throws YggdrasilException; 
}
