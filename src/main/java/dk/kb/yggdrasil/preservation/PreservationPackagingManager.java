package dk.kb.yggdrasil.preservation;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.exceptions.PreservationException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * Manager for the warc file creators.
 * 
 */
public class PreservationPackagingManager {
    /** Logging mechanism. */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    /** The context for preserving warc files. */
    private final PreservationContext context;
    /** Map of warc creators for each collection. */
    private Map<String, PreservationPacker> creators;
    /** The timer to schedule the preservation task. */
    private Timer timer;
    
    /**
     * Constructor.
     * @param wpc The context for the preservation of warc files.
     */
    public PreservationPackagingManager(PreservationContext wpc) {
        this.context = wpc;
        this.creators = new HashMap<String, PreservationPacker>();
        
        // Add the timer task.
        timer = new Timer("WarcPreservationManager");
        timer.scheduleAtFixedRate(new WarcPreservationTimerTask(), 
                context.getConfig().getCheckWarcConditionInterval(), 
                context.getConfig().getCheckWarcConditionInterval());
    }
    
    /**
     * Adds the preservation request state to the warc file for the given collection.
     * @param collectionId The id of the collection.
     * @param prs The preservation request to handle.
     * @throws YggdrasilException 
     */
    public void addToWarcFile(String collectionId, PreservationRequestState prs) throws YggdrasilException, 
            PreservationException {
        getCreator(collectionId).writePreservationRecord(prs);
        getCreator(collectionId).verifyConditions();
    }

    /**
     * Gets a WarcCreator for the given collection.
     * If no WarcCreator exists for the collection, then a new one is instantiated.
     * @param collectionId The id of the collection.
     * @return The WarcCreator for the collectionId.
     */
    private PreservationPacker getCreator(String collectionId) {
        if(!creators.containsKey(collectionId)) {
            creators.put(collectionId, new PreservationPacker(context, collectionId));
        }
        return creators.get(collectionId);
    }
    
    /**
     * The timer task for checking the conditions for each WarcCreator.
     * It is especially meant for testing the time condition.
     */
    private class WarcPreservationTimerTask extends TimerTask {
        @Override
        public void run() {
            for(PreservationPacker creator : creators.values()) {
                logger.trace("Checking conditions");
                creator.verifyConditions();
            }
        }
    }
}
