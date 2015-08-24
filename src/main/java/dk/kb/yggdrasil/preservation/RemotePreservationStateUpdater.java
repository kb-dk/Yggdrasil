package dk.kb.yggdrasil.preservation;

import java.util.Date;

import org.bitrepository.common.ArgumentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.db.PreservationImportRequestState;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.preservation.Preservation;
import dk.kb.yggdrasil.json.preservation.PreservationResponse;
import dk.kb.yggdrasil.json.preservationimport.PreservationImportResponse;
import dk.kb.yggdrasil.json.preservationimport.Response;
import dk.kb.yggdrasil.messaging.MQ;
import dk.kb.yggdrasil.preservationimport.PreservationImportState;

/**
 * Simple class for dealing with updating the remote preservation states.
 * Thus creating and sending PreservationResponse messages.
 */
public class RemotePreservationStateUpdater {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /** The RabbitMQ connection used by this workflow. */
    private final MQ mq;

    /**
     * Constructor.
     * @param mq The MQ for sending updates.
     */
    public RemotePreservationStateUpdater(MQ mq) {
        this.mq = mq;
    }

    /**
     * Update remote preservation state with a non-default details about the state.
     * Especially used for failures, where the details about the failure can be delivered.
     * @param prs a given PreservationRequestState
     * @param newState The new state.
     * @param details The new details for the new state.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    public void sendPreservationResponseWithSpecificDetails(PreservationRequestState prs, PreservationState newState,
            String details) throws YggdrasilException {
        ArgumentValidator.checkNotNull(prs, "PreservationRequestState prs");
        ArgumentValidator.checkNotNull(newState, "State newPreservationState");

        Preservation preseravtionInfo = new Preservation();
        preseravtionInfo.preservation_state = newState.name();
        preseravtionInfo.preservation_details = details;
        sendPreservationResponse(prs, preseravtionInfo);

        logger.info("Preservation status updated to '" + newState.name()
                +  "' using the updateURI. Reason: " + details );
    }

    /**
     * Update the remote preservationState corresponding with this current request using the Update_URI field.
     * @param prs The current request.
     * @param newState The new Preservation State.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    public void sendPreservationResponse(PreservationRequestState prs, PreservationState newState) 
            throws YggdrasilException {
        ArgumentValidator.checkNotNull(prs, "PreservationRequestState prs");
        ArgumentValidator.checkNotNull(newState, "State newState");
        
        Preservation preservationInfo = new Preservation();
        preservationInfo.preservation_state = newState.name();
        preservationInfo.preservation_details = newState.getDescription();
        sendPreservationResponse(prs, preservationInfo);
        
        prs.setState(newState);
        logger.info("Preservation status updated to '" + newState.name() +  "' using the updateURI.");
    }
    
    /**
     * Creates and send the PreservationResponse about the new state in the handling of for a given 
     * PreservationRequest.
     * @param prs The request to be updated.
     * @param newState The new state.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    private void sendPreservationResponse(PreservationRequestState prs, Preservation newState) 
            throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.id = prs.getRequest().Valhal_ID;
        response.model = prs.getRequest().Model;
        response.preservation = newState;
        if (prs.getWarcId() != null) {
            response.preservation.warc_id = prs.getWarcId();
        }
        if(prs.getFileWarcId() != null) {
            response.preservation.file_warc_id = prs.getFileWarcId();
        }
        if(prs.getUpdatePreservation() != null) {
            response.update = prs.getUpdatePreservation();
        }
        
        mq.publishPreservationResponse(response);
    }
    
    /**
     * Update remote preservation state with a non-default details about the state.
     * Especially used for failures, where the details about the failure can be delivered.
     * @param prs a given PreservationRequestState
     * @param newState The new state.
     * @param details The new details for the new state.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    public void sendPreservationImportResponse(PreservationImportRequestState prs, PreservationImportState newState, 
            String details) throws YggdrasilException {
        ArgumentValidator.checkNotNull(prs, "PreservationImportRequestState prs");
        ArgumentValidator.checkNotNull(newState, "PreservationImportState newState");

        prs.setState(newState);
        
        Response preservationImportResponse = new Response();
        preservationImportResponse.date = new Date().toString();
        preservationImportResponse.state = prs.getState().name();

        if(details != null && !details.isEmpty()) {
            preservationImportResponse.detail = details;
        } else {
            preservationImportResponse.detail = newState.getDescription();
        }

        PreservationImportResponse response = new PreservationImportResponse();
        response.uuid = prs.getRequest().uuid;
        response.type = prs.getRequest().type;
        response.response = preservationImportResponse;
        
        mq.publishPreservationImportResponse(response);
    }
}
