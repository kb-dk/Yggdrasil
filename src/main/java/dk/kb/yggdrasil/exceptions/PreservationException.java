package dk.kb.yggdrasil.exceptions;

import dk.kb.yggdrasil.preservation.PreservationState;

/**
 * Exceptions for the preservation workflow.
 */
@SuppressWarnings("serial")
public class PreservationException extends Exception {
    /** The failure state, which should be reported.*/
    private PreservationState state;
    
    /**
     * Constructor.
     * @param state The failure state.
     * @param msg The message of the exception.
     */
    public PreservationException(PreservationState state, String msg) {
        super(msg);
        this.state = state;
    }

    /**
     * Constructor.
     * @param state The failure state.
     * @param msg The message of the exception.
     * @param e The exception to embed.
     */
    public PreservationException(PreservationState state, String msg, Throwable e) {
        super(msg, e);
        this.state = state;
    }
    
    /**
     * @return The failure state for the exception.
     */
    public PreservationState getState() {
        return state;
    }
}
