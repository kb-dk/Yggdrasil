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
    
    public PreservationRequestState(PreservationRequest request,
            State preservationState) {
        ArgumentCheck.checkNotNull(request, "PreservationRequest request");
        ArgumentCheck.checkNotNull(preservationState, "State preservationState");
        this.request = request;
        this.state = preservationState;
    }
    
    /** @return existing preservation state. */  
    public State getState() {
        return state;
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
