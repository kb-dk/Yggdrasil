package dk.kb.yggdrasil.exceptions;

import dk.kb.yggdrasil.State;

@SuppressWarnings("serial")
public class PreservationException extends Exception {
    /** The failure state, which should be reported.*/
    private State state;
    
    /**
     * Constructor.
     * @param state The failure state.
     * @param msg The message of the exception.
     */
    public PreservationException(State state, String msg) {
        super(msg);
        this.state = state;
    }

    /**
     * Constructor.
     * @param state The failure state.
     * @param msg The message of the exception.
     * @param e The exception to embed.
     */
    public PreservationException(State state, String msg, Throwable e) {
        super(msg, e);
        this.state = state;
    }
    
    /**
     * @return The failure state for the exception.
     */
    public State getState() {
        return state;
    }
}
