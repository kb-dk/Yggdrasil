package dk.kb.yggdrasil.preservation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * This class describes all the system states of the Yggdrasil preservation service.
 * It is the intention, that these states are reported back to the Valhal system after
 * each state change.
 *  Step 1: Message is received from valhal results in either state PRESERVATION_REQUEST_RECEIVED
 *      or the failstate PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE
 *  Step 2: If metadata download succesfully, package the metadata . This can result in either failstate
 *      PRESERVATION_METADATA_PACKAGED_FAILURE or successtate PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY
 *  Step 3: If still no failstate, we continue with fetching the resources associated with this metadata.
 *       This can result in the failstate PRESERVATION_RESOURCES_DOWNLOAD_FAILURE or the OK-state 
 *       PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS
 *  Step 4: If still good to go, we package the data in the warc-format. No failstate here (need one),
 *      but if more data required before upload to bitrepository we go to wait-state 
 *      PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA
 *      otherwise we change state to PRESERVATION_PACKAGE_COMPLETE (is this state necessary?). If in 
 *      PRESERVATION_PACKAGE_COMPLETE remember to check
 *      if status for other requests can be changed from PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA to 
 *      PRESERVATION_PACKAGE_COMPLETE.
 *  Step 5: Initiate upload to Bitrepository. If initiation fails, go to the failstate 
 *      PRESERVATION_PACKAGE_UPLOAD_FAILURE.
 *  Step 6: Wait for Bitrepository upload to complete. This can result in the failstate 
 *      PRESERVATION_PACKAGE_UPLOAD_FAILURE or final OK-state PRESERVATION_PACKAGE_UPLOAD_SUCCESS.
 */
public enum PreservationState implements Serializable {
    /** Preservation request received and understood (i.e. the message is complete). */
    PRESERVATION_REQUEST_RECEIVED("Preservation request received and validated"),
    
    /** Preservation request incomplete. Something is missing. Failstate. */
    PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE("Preservation request incomplete. Something is missing"),
    
    /** Resources downloaded successfully. */
    PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS("Resources downloaded successfully"),

    /** Resources downloaded unsuccessfully. Failstate. */
    PRESERVATION_RESOURCES_DOWNLOAD_FAILURE("Resources downloaded unsuccessfully"),
    
    /** Metadata packaged successfully. */
    PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY("Metadata packaged successfully."),
    
    /** Metadata packaged unsuccessfully (eg. METS error or similar). Failstate. */
    PRESERVATION_METADATA_PACKAGED_FAILURE("Metadata packaged unsuccessfully (eg. METS error or similarly)"),
    
    /** Resources packaged successfully. */
    PRESERVATION_RESOURCES_PACKAGE_SUCCESS("Resources packaged successfully."),

    /** Resources not package successfully. Failstate. */
    PRESERVATION_RESOURCES_PACKAGE_FAILURE("Resources not packaged successfully."),

    /** Package complete (metadata and ressources written to the WARC format).
     * and ready to initiate upload. */
    PRESERVATION_PACKAGE_COMPLETE("metadata and ressources has been written to the WARC format"),
    
    /** Waiting for more requests before upload is initiated.
     * If the request does not have the requirement, that it should be packaged in its own package.
     * Then it arrives into this state. However, we can only package data together with
     * the same bitrepository profile. So each profile must have its own waiting queue.
     */
    PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA(
            "Waiting for more requests before upload to bitrepository is initiated"),
    
    /** Initiated upload to Bitrepository. */
    PRESERVATION_PACKAGE_UPLOAD_INITIATED("Upload to bitrepository initiated"),
    
    /** Upload to Bitrepository failed. Failstate. */
    PRESERVATION_PACKAGE_UPLOAD_FAILURE("Upload to bitrepository failed."),
    
    /** Upload to Bitrepository was successful. */
    PRESERVATION_PACKAGE_UPLOAD_SUCCESS("Upload to bitrepository was successful"),
    
    /** Preservation request failed */
    PRESERVATION_REQUEST_FAILED("The preservation request failed");
    
    private PreservationState(String description) {
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
    private static final Set<PreservationState> FAIL_STATES = new HashSet<PreservationState>(Arrays.asList(
            PRESERVATION_PACKAGE_UPLOAD_FAILURE,
            PRESERVATION_METADATA_PACKAGED_FAILURE, 
            PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE,
            PRESERVATION_RESOURCES_DOWNLOAD_FAILURE,
            PRESERVATION_REQUEST_FAILED));

    /**
     * @return set of failure states.
     */
    public static Set<PreservationState> getFailStates() {
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
    public static void verifyIfValidStateChange(PreservationState oldState, PreservationState newState) 
            throws YggdrasilException {
        ArgumentCheck.checkNotNull(oldState, "State oldState");
        ArgumentCheck.checkNotNull(newState, "State newState");
        
        if (!oldState.isOkState()) {
            throw new YggdrasilException("Cannot change from state '" 
                    + oldState + "' to '" + newState + "', since the current state is a fail-state.");
        }
        
        if (oldState.ordinal() > newState.ordinal()) {
            throw new YggdrasilException("Cannot change from state '" 
                    + oldState + "' to '" + newState + "', since we cannot go back to previous states.");  
        }
    }
    
    /**
     * Check, if the state change from oldState to newState is valid.
     * @param oldState The existing state
     * @param newState The new state
     * @return true, if the transition from oldState to newState is valid; otherwise false is returned.
     */
    public static boolean isValidStateChange(PreservationState oldState, PreservationState newState){
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
     * @param aState A given state
     * @return true, if the current state equals the given state, otherwise false.
     */
    public boolean hasState(PreservationState aState) {
        return this.equals(aState);
    }
}
