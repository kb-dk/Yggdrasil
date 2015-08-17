package dk.kb.yggdrasil.db;

import java.io.File;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.PreservationRequest;
import dk.kb.yggdrasil.json.preservation.Update;
import dk.kb.yggdrasil.preservation.PreservationState;

/**
 * This class is a container for the request from valhal, and its
 * temporary files built during the workflow.
 */
public class PreservationRequestState implements Serializable {

    /** Logging mechanism. */
    private static final Logger logger = LoggerFactory.getLogger(
            PreservationRequestState.class.getName());

    /** The preservationRequest received from Valhal. */ 
    private PreservationRequest request;
    /** The current preservationState */
    private PreservationState state;

    /** The uuid for the current  */
    private String uuid;
    /** The content payload. This is downloaded using REST from Valhal. */
    private File contentPayload;
    /** The metadata payload. This is the result of the transformation of the metadata
     * included in the request. */
    private File metadataPayload;
    /** The id of the warc file. */
    private String warcId;
    /** The id of the warc file containing the resource.*/
    private String fileWarcId;
    /** The preservation update data. Default null, since it is only used for preservation updates.*/
    private Update preservationUpdate = null;

    /**
     * The constructor of the PreservationRequestState.
     * @param request The request itself
     * @param preservationState Its current state in Yggdrasil
     * @param uuid The uuid of this request.
     */
    public PreservationRequestState(PreservationRequest request,
            PreservationState preservationState, String uuid) {
        ArgumentCheck.checkNotNull(request, "PreservationRequest request");
        ArgumentCheck.checkNotNull(preservationState, "State preservationState");
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        this.request = request;
        this.state = preservationState;
        this.uuid = uuid;
    }

    /** @return the preservation state of this request. */  
    public PreservationState getState() {
        return state;
    }

    /** @return uuid for this request */  
    public String getUUID() {
        return uuid;
    }    

    /**
     * Change state to the newState if this is a valid statechange 
     * @param newState The new state
     * @throws YggdrasilException If it fails to validate the state change.
     */
    public void setState(PreservationState newState) throws YggdrasilException {
        ArgumentCheck.checkNotNull(newState, "State newState");
        PreservationState.verifyIfValidStateChange(this.state, newState);
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
     * @return the warc file containing the metadata.
     */
    public String getWarcId() {
        return warcId;
    }

    /**
     * Set the warc file containing the metadata.
     * @param metadataWarcPackage The UploadPackage (This file must exist)
     */
    public void setMetadataWarcFile(File metadataWarcPackage) {
        ArgumentCheck.checkExistsNormalFile(metadataWarcPackage, "File metadataWarcPackage");
        this.warcId = metadataWarcPackage.getName();
    }
    
    /**
     * @return The id of the warc file containing the resource file.
     */
    public String getFileWarcId() {
        return fileWarcId;
    }
    
    /**
     * Set the warc file with the resource. 
     * @param resourceWarcPackage The Warc file containing the resource.
     */
    public void setResourceWarcFile(File resourceWarcPackage) {
        ArgumentCheck.checkExistsNormalFile(resourceWarcPackage, "File resourceWarcPackage");
        this.fileWarcId = resourceWarcPackage.getName();        
    }

    /**
     * Reset the uploadpackage file.
     */
    public void resetUploadPackage() {
        this.warcId = null;
        this.fileWarcId = null;
    }

    /**
     * @return The update element containing data about the preservation update. 
     */
    public Update getUpdatePreservation() {
        return preservationUpdate;        
    }
    
    /**
     * @param update The preservation update element to be set.
     */
    public void setUpdatePreservation(Update update) {
        this.preservationUpdate = update;
    }

    /**
     * Remove the temporary files for the records referred to in this object, if they still exist.
     */
    public void cleanup() {
        if (contentPayload != null && contentPayload.exists()) {
            if (!contentPayload.delete()) {
                logger.warn("Unable to delete temporary file for contentPayload '" 
                        + contentPayload.getAbsolutePath() + "'");
            }
            contentPayload = null;
        }

        if (metadataPayload != null && metadataPayload.exists()) {
            if (!metadataPayload.delete()) {
                logger.warn("Unable to delete temporary file for metadataPayload '" 
                        + metadataPayload.getAbsolutePath() + "'");
            }
            metadataPayload = null;
        }
    }
}
