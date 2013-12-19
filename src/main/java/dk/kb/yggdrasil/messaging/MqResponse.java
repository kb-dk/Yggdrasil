package dk.kb.yggdrasil.messaging;

/** Container class for the response from our message broker,. */
public class MqResponse {
    /** The type of the message. */
    private String messageType;
    /** The payload of the message. */
    private byte[] payload;

    /**
     * Constructor.
     * @param messageType The type of the message (can be null)
     * @param payload The payload of the message (can be null)
     */
    public MqResponse(String messageType, byte[] payload) {
        this.messageType = messageType;
        this.payload = payload;
    }
    
    /** 
     * @return the payload
     */
    public byte[] getPayload() {
        return this.payload;
    }
    
    /**
     * @return the messageType.
     */
    public String getMessageType(){
        return this.messageType;
    }
    
}
