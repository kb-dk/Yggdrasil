package dk.kb.yggdrasil.preservation;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;

/**
 * A wrapper class around the different components needed for sending files to preservation.
 */
public class PreservationContext {
    /** The bitrepository to upload the WARC files to, when they are ready. */
    private final Bitrepository bitrepository;
    /** The general settings used by Yggdrasil. */
    private final Config config;
    /** The remote preservation state updater.*/
    private final RemotePreservationStateUpdater remotePreservationStateUpdater;
    /** The StateDatase instance used by this workflow. */
    private final StateDatabase stateDatabase;

    /**
     * Constructor.
     * @param bitrepository The bitrepository to upload packaged files to.
     * @param config The configuration.
     * @param sd The state database.
     * @param remotePreservationStateUpdater The remote preservation state updater.
     */
    public PreservationContext(Bitrepository bitrepository, Config config, StateDatabase sd, 
            RemotePreservationStateUpdater remotePreservationStateUpdater) {
        ArgumentCheck.checkNotNull(bitrepository, "bitrepository");
        ArgumentCheck.checkNotNull(config, "config");
        ArgumentCheck.checkNotNull(sd, "state database");
        ArgumentCheck.checkNotNull(remotePreservationStateUpdater, "remotePreservationStateUpdater");
        this.bitrepository = bitrepository;
        this.config = config;
        this.remotePreservationStateUpdater = remotePreservationStateUpdater;
        this.stateDatabase = sd;
    }
    
    /**
     * @return The bitrepository.
     */
    public Bitrepository getBitrepository() {
        return bitrepository;
    }
    
    /**
     * @return The config.
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * @return The remote preservation state updater.
     */
    public RemotePreservationStateUpdater getRemotePreservationStateUpdater() {
        return remotePreservationStateUpdater;
    }
    
    /**
     * @return The state database.
     */
    public StateDatabase getStateDatabase() {
        return stateDatabase;
    }    
}
