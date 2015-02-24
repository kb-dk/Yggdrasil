package dk.kb.yggdrasil.preservation;

import org.bitrepository.common.ArgumentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.json.JSONMessaging;
import dk.kb.yggdrasil.json.Preservation;
import dk.kb.yggdrasil.json.PreservationResponse;
import dk.kb.yggdrasil.messaging.MQ;

/**
 * Simple class for dealing with updating the remote preservation states.
 */
public class RemotePreservationStateUpdater {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
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
    public void updateRemotePreservationStateWithSpecificDetails(PreservationRequestState prs, State newState,
            String details) throws YggdrasilException {
        ArgumentValidator.checkNotNull(prs, "PreservationRequestState prs");
        ArgumentValidator.checkNotNull(newState, "State newPreservationState");

        Preservation preseravtionInfo = new Preservation();
        preseravtionInfo.preservation_state = newState.name();
        preseravtionInfo.preservation_details = details;
        updateRemotePreservationState(prs, preseravtionInfo);

        logger.info("Preservation status updated to '" + newState.name()
                +  "' using the updateURI. Reason: " + details );
    }

    /**
     * Update the remote preservationState corresponding with this current request using the Update_URI field.
     * @param prs The current request.
     * @param newState The new Preservation State.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    public void updateRemotePreservationState(PreservationRequestState prs,
            State newState) throws YggdrasilException {
        ArgumentValidator.checkNotNull(prs, "PreservationRequestState prs");
        ArgumentValidator.checkNotNull(newState, "State newState");
        
        Preservation preservationInfo = new Preservation();
        preservationInfo.preservation_state = newState.name();
        preservationInfo.preservation_details = newState.getDescription();
        updateRemotePreservationState(prs, preservationInfo);

        logger.info("Preservation status updated to '" + newState.name()
                +  "' using the updateURI.");
    }
    
    /**
     * Creates and send the PreservationResponse about the new state in the handling of for a given 
     * PreservationRequest.
     * @param prs The request to be updated.
     * @param newState The new state.
     * @throws YggdrasilException If an issue with sending the message occurs.
     */
    private void updateRemotePreservationState(PreservationRequestState prs, Preservation newState) 
            throws YggdrasilException {
        PreservationResponse response = new PreservationResponse();
        response.id = prs.getRequest().Valhal_ID;
        response.model = prs.getRequest().Model;
        response.preservation = newState;
        if (prs.getWarcId() != null) {
            response.preservation.warc_id = prs.getWarcId();
        }

        /* send to RabbitMQ */
        byte[] responseBytes = JSONMessaging.getPreservationResponse(response);
        mq.publishPreservationResponse(responseBytes);
    }
}
