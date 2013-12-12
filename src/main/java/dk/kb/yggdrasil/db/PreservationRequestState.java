package dk.kb.yggdrasil.db;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Logger;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.PreservationRequest;

/**
 * This class is a container for the request from valhal, and its
 * temporary files built during the workflow.
 */
public class PreservationRequestState implements Serializable {
    
    /** Logging mechanism. */
    private static final Logger logger = Logger.getLogger(PreservationRequestState.class.getName());
    
    /** The preservationRequest received from Valhal. */ 
    private PreservationRequest request;
    /** The current preservationState */
    private State state;
    
    /** The uuid for the current  */
    private String uuid;
    /** The content payload. This is downloaded using REST from Valhal. */
    private File contentPayload;
    /** The metadata payload. This is the result of the transformation of the metadata
     * included in the request. */
    private File metadataPayload;
    /** The uploadpackage. This is the warcfile to be uploaded to the bitrepository. */
    private File uploadPackage;
    
    /**
     * The constructor of the PreservationRequestState.
     * @param request The request itself
     * @param preservationState Its current state in Yggdrasil
     * @param uuid The uuid of this request.
     */
    public PreservationRequestState(PreservationRequest request,
            State preservationState, String uuid) {
        ArgumentCheck.checkNotNull(request, "PreservationRequest request");
        ArgumentCheck.checkNotNull(preservationState, "State preservationState");
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        this.request = request;
        this.state = preservationState;
        this.uuid = uuid;
    }
    
    /** @return the preservation state of this request. */  
    public State getState() {
        return state;
    }
    
    /** @return uuid for this request */  
    public String getUUID() {
        return uuid;
    }    
    
    /**
     * Change state to the newState if this is a valid statechange 
     * @param newState The new state
     * @throws YggdrasilException
     */
    public void setState(State newState) throws YggdrasilException {
        ArgumentCheck.checkNotNull(newState, "State newState");
        State.verifyIfValidStateChange(this.state, newState);
        this.state = newState;
    }
    
    /** 
     * @return the request itself
     */
    public PreservationRequest getRequest() {
        return request;
    }

    /** 
     * @return the content payload.
     */
    public File getContentPayload() {
        return contentPayload;
    }

    /** 
     * Set the content payload.
     * @param contentPayload the content payload as a File
     */
    public void setContentPayload(File contentPayload) {
        ArgumentCheck.checkExistsNormalFile(contentPayload, "File contentPayload");
        this.contentPayload = contentPayload;
    }
    
    /**
     * 
     * @return the metadata payload
     */
    public File getMetadataPayload() {
        return metadataPayload;
    }

    /**
     * Set the metadata payload file.
     * @param metadataPayload The metadata payload (This file must exist)
     */
    public void setMetadataPayload(File metadataPayload) {
        ArgumentCheck.checkExistsNormalFile(metadataPayload, "File metadataPayload");
        this.metadataPayload = metadataPayload;
    }

    /**
     * @return the Uploadpackage file.
     */
    public File getUploadPackage() {
        return uploadPackage;
    }
    
    /**
     * Set the uploadpackage file.
     * @param uploadPackage The UploadPackage (This file must exist)
     */
    public void setUploadPackage(File uploadPackage) {
        ArgumentCheck.checkExistsNormalFile(uploadPackage, "File uploadPackage");
        this.uploadPackage = uploadPackage;
    }
   
    /**
    * Reset the uploadpackage file.
    */
   public void resetUploadPackage() {
       this.uploadPackage = null;
   }
   
   /**
    * Remove the temporary files referred to in this object, if they
    * still exist.
    */
   public void cleanup() {
       if (uploadPackage != null && uploadPackage.exists()) {
           boolean deleted = uploadPackage.delete();
           if (!deleted) {
               logger.warning("Unable to delete uploadpackagefile '" 
                       + uploadPackage.getAbsolutePath() + "'");
           }
       }
       if (contentPayload != null && contentPayload.exists()) {
           boolean deleted = contentPayload.delete();
           if (!deleted) {
               logger.warning("Unable to delete temporary file for contentPayload '" 
                       + contentPayload.getAbsolutePath() + "'");
           }
       }
       
       if (metadataPayload != null && metadataPayload.exists()) {
           boolean deleted = metadataPayload.delete();
           if (!deleted) {
               logger.warning("Unable to delete temporary file for metadataPayload '" 
                       + metadataPayload.getAbsolutePath() + "'");
           }
       }
   }
}
