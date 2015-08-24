package dk.kb.yggdrasil.db;

import java.io.File;
import java.io.Serializable;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportRequest;
import dk.kb.yggdrasil.preservationimport.PreservationImportState;

/**
 * This class is a container for the preservation import request from valhal,
 * and to keep track of the progress.
 */
public class PreservationImportRequestState implements Serializable {
//
//    /** Logging mechanism. */
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The preservationRequest received from Valhal. */ 
    private PreservationImportRequest request;
    /** The current preservationState */
    private PreservationImportState state;
    /** The Checksum of the warc record payload from the warc-record-header.*/
    private String warcHeaderChecksum = null;
    
    /** The data to be imported into Valhal. */
    private File importData = null;

    /**
     * The constructor of the PreservationImportRequestState.
     * @param request The request itself
     * @param preservationState Its current state in Yggdrasil
     */
    public PreservationImportRequestState(PreservationImportRequest request,
            PreservationImportState preservationState) {
        ArgumentCheck.checkNotNull(request, "PreservationImportRequest request");
        ArgumentCheck.checkNotNull(preservationState, "PreservationImportState preservationState");
        this.request = request;
        this.state = preservationState;
    }

    /** @return the preservation import state of this request. */  
    public PreservationImportState getState() {
        return state;
    }

    /**
     * Change state to the newState if this is a valid statechange 
     * @param newState The new state
     * @throws YggdrasilException If it fails to validate the state change.
     */
    public void setState(PreservationImportState newState) throws YggdrasilException {
        ArgumentCheck.checkNotNull(newState, "PreservationImportState newState");
        
        System.err.println("STATE - Changing from '" + this.state.name() + "' to '" + newState.name() + "'");
        PreservationImportState.verifyIfValidStateChange(this.state, newState);
        this.state = newState;
    }

    /** 
     * @return the request itself
     */
    public PreservationImportRequest getRequest() {
        return request;
    }
    
    /**
     * Sets the import data file.
     * @param importFile The import data file.
     */
    public void setImportData(File importFile) {
        ArgumentCheck.checkExistsNormalFile(importFile, "File importFile");
        importData = importFile;
    }
    
    /**
     * @return The file with the import data (or null, if no such file has been retrieved).
     */
    
    public File getImportData() {
        return importData;
    }
    
    /**
     * @param checksum The value for the warc header checksum variable.
     */
    public void setWarcHeaderChecksum(String checksum) {
        warcHeaderChecksum = checksum;
    }
    
    /**
     * @return The checksum from the warc header.
     */
    public String getWarcHeaderChecksum() {
        return warcHeaderChecksum;
    }

    /**
     * Removes the file with the import data.
     */
    public void cleanup() { 
        if(importData != null && importData.isFile()) {
            importData.delete();
        }
    }
}
