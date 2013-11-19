package dk.kb.yggdrasil.db;

import java.io.Serializable;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;

public class PreservationRequestState implements Serializable {
    
    /** The preservationRequest received from Valhal. */ 
    private PreservationRequest request;
    /** The current preservationState */
    private State state;
    
    /** The uuid for the current  */
    private String uuid;
    
    
    public PreservationRequestState(PreservationRequest request,
            State preservationState, String uuid) {
        ArgumentCheck.checkNotNull(request, "PreservationRequest request");
        ArgumentCheck.checkNotNull(preservationState, "State preservationState");
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        this.request = request;
        this.state = preservationState;
        this.uuid = uuid;
    }
    
    /** @return existing preservation state. */  
    public State getState() {
        return state;
    }
    
    /** @return existing preservation state. */  
    public String getUUID() {
        return uuid;
    }    
    
    /**
     * Change state to the newState if this is a valid statechange 
     * @param newState The new state
     * @throws YggdrasilException
     */
    public void setState(State newState) throws YggdrasilException {
        State.verifyIfValidStateChange(this.state, newState);
        this.state = newState;
    }

    public PreservationRequest getRequest() {
        return request;
    }
   
}
