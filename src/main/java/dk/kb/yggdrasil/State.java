package dk.kb.yggdrasil;

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
 *       This can result in the failstate PRESERVATION_RESOURCES_DOWNLOAD_FAILURE or the OK-state PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS
 *  Step 4: If still good to go, we package the data in the warc-format. No failstate here (need one),
 *      but if more data required before upload to bitrepository we go to wait-state PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA
 *      otherwise we change state to PRESERVATION_PACKAGE_COMPLETE (is this state necessary?). If in PRESERVATION_PACKAGE_COMPLETE remember to check
 *      if status for other requests can be changed from PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA to PRESERVATION_PACKAGE_COMPLETE.
 *  Step 5: Initiate upload to Bitrepository. If initiation fails, go to the failstate PRESERVATION_PACKAGE_UPLOAD_FAILURE.
 *  Step 6: Wait for Bitrepository upload to complete. This can result in the failstate PRESERVATION_PACKAGE_UPLOAD_FAILURE or
 *      final OK-state PRESERVATION_PACKAGE_UPLOAD_SUCCESS.
 *
 */
public enum State implements Serializable {
    /** Preservation request received and understood (i.e. the message is complete). */
    PRESERVATION_REQUEST_RECEIVED,
    /** Preservation request incomplete. Something is missing. Failstate. */
    PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE,
    /** Metadata packaged successfully. */
    PRESERVATION_METADATA_PACKAGED_SUCCESSFULLY,
    /** Metadata packaged unsuccessfully (eg. METS error or similar). Failstate. */
    PRESERVATION_METADATA_PACKAGED_FAILURE,
    /** Resources downloaded successfully. */
    PRESERVATION_RESOURCES_DOWNLOAD_SUCCESS,
    /** Resources downloaded unsuccessfully. Failstate. */
    PRESERVATION_RESOURCES_DOWNLOAD_FAILURE,
    /** Package complete (metadata and ressources written to the WARC format).
     * and ready to initiate upload. */
    PRESERVATION_PACKAGE_COMPLETE,
    /** Waiting for more requests before upload is initiated.
     * If the request does not have the requirement, that it should be packaged in its own package.
     * Then it arrives into this state. However, we can only package data together with
     * the same bitrepository profile. So each profile must have its own waiting queue.
     */
    PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA,
    /** Initiated upload to Bitrepository. */
    PRESERVATION_PACKAGE_UPLOAD_INITIATED,
    /** Upload to Bitrepository failed. Failstate. */
    PRESERVATION_PACKAGE_UPLOAD_FAILURE,
    /** Upload to Bitrepository was successful. */
    PRESERVATION_PACKAGE_UPLOAD_SUCCESS;

    /** Set with the failstates in this enum class. */
    private static final Set<State> FAIL_STATES = new HashSet<State>(Arrays.asList(
            PRESERVATION_PACKAGE_UPLOAD_FAILURE,
            PRESERVATION_METADATA_PACKAGED_FAILURE, PRESERVATION_REQUEST_RECEIVED_BUT_INCOMPLETE,
            PRESERVATION_RESOURCES_DOWNLOAD_FAILURE));

    /**
     * @return set of failure states.
     */
    public static Set<State> getFailStates() {
        return FAIL_STATES;
    }

    /**
     * Method for finding out whether the given state is a failstate or not.
     * @param aState The given state
     * @return true, if the given state is a failstate; otherwise it returns false.
     */
    public boolean isOkState() {
        return !FAIL_STATES.contains(this);
    }

    /**
     * Verify if state change is valid. Throws an Exception if not valid change
     * @param oldState the old state
     * @param newState the new state
     * @throws YggdrasilException
     */
    public static void verifyIfValidStateChange(State oldState, State newState) throws YggdrasilException {
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
    
    public static boolean isValidStateChange(State oldState, State newState){
        ArgumentCheck.checkNotNull(oldState, "State oldState");
        ArgumentCheck.checkNotNull(newState, "State newState");
        try {
            verifyIfValidStateChange(oldState, newState);
            return true;
        } catch (YggdrasilException e) {
            return false;
        }
    }
    
    public boolean hasState(State aState) {
        return this.equals(aState);
    }
}
