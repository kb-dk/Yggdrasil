package dk.kb.yggdrasil.preservationimport;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * This class describes all the system states of the Yggdrasil preservation import workflow.
 * It is the intention, that these states are reported back to the Valhal system after
 * each state change, thus telling about the progress, successes or failures.
 */
public enum PreservationImportState implements Serializable {
    /** Preservation request received and understood (i.e. the message is complete). */
    IMPORT_REQUEST_RECEIVED_AND_VALIDATED("Preservation import request received and validated"),
    
    /** The preservation import request is incomplete. Something is missing. Failstate. */
    IMPORT_REQUEST_VALIDATION_FAILURE("Preservation import request incomplete. Something is missing"),
    
    /** Start retrieving data from the Bitrepository. */
    IMPORT_RETRIEVAL_FROM_BITREPOSITORY_INITIATED("Initiating the retrieval of the data from the Bitrepository."),
    
    /** Failed to retrieve data from the Bitrepository. Failstate. */
    IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE("Failed to retrieve data from the Bitrepository"),
    
    /** Starting to deliver the data to Valhal. */
    IMPORT_DELIVERY_INITIATED("Initiating the delivery of data to Valhal."),

    /** Failed to deliver the data to Valhal. Failstate. */
    IMPORT_DELIVERY_FAILURE("Failed to deliver the data to Valhal"),
    
    /** Generic failure, which does not fall into any of the other failures. Failstate. */
    IMPORT_FAILURE("Unspecified failure."),
    
    /** Finished the import operation. */
    IMPORT_FINISHED("Finshed the import operation.");

    private PreservationImportState(String description) {
        this.defaultDescription = description;
    }
    
    /** 
     * @return The default description of this state.
     */
    public String getDescription() {
        return this.defaultDescription;
    }
    
    /** Default description. */
    private String defaultDescription; 
    
    
    /** Set with the failstates in this enum class. */
    private static final Set<PreservationImportState> FAIL_STATES = new HashSet<PreservationImportState>(Arrays.asList(
            IMPORT_REQUEST_VALIDATION_FAILURE,
            IMPORT_RETRIEVAL_FROM_BITREPOSITORY_FAILURE, 
            IMPORT_DELIVERY_FAILURE,
            IMPORT_FAILURE));

    /**
     * @return set of failure states.
     */
    public static Set<PreservationImportState> getFailStates() {
        return FAIL_STATES;
    }

    /**
     * Method for finding out whether the state is a failstate or not.
     * @return true, if the state is a failstate; otherwise it returns false.
     */
    public boolean isOkState() {
        return !FAIL_STATES.contains(this);
    }

    /**
     * Verify if state change is valid. Throws an Exception if not valid change
     * @param oldState the old state
     * @param newState the new state
     * @throws YggdrasilException If not a valid state change.
     */
    public static void verifyIfValidStateChange(PreservationImportState oldState, PreservationImportState newState) 
            throws YggdrasilException {
        ArgumentCheck.checkNotNull(oldState, "State oldState");
        ArgumentCheck.checkNotNull(newState, "State newState");
        
        if (!oldState.isOkState()) {
            throw new YggdrasilException("Cannot change from state '" 
                    + oldState + "' to '" + newState + "'");
        }
        
        if (oldState.ordinal() > newState.ordinal()) {
            throw new YggdrasilException("Cannot change from state '" 
                    + oldState + "' to '" + newState + "'");  
        }
    }
    
    /**
     * Check, if the state change from oldState to newState is valid.
     * @param oldState The existing state
     * @param newState The new state
     * @return true, if the transition from oldState to newState is valid; otherwise false is returned.
     */
    public static boolean isValidStateChange(PreservationImportState oldState, PreservationImportState newState){
        ArgumentCheck.checkNotNull(oldState, "State oldState");
        ArgumentCheck.checkNotNull(newState, "State newState");
        try {
            verifyIfValidStateChange(oldState, newState);
            return true;
        } catch (YggdrasilException e) {
            return false;
        }
    }
    
    /** 
     * 
     * @param aState A given state
     * @return true, if the current state equals the given state, otherwise false.
     */
    public boolean hasState(PreservationImportState aState) {
        return this.equals(aState);
    }
}
