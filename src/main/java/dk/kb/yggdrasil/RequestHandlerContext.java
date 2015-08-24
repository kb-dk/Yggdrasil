package dk.kb.yggdrasil;

import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.preservation.RemotePreservationStateUpdater;

/**
 * A wrapper class around the different components needed for sending files to preservation.
 */
public class RequestHandlerContext {
    /** The bitrepository to upload the WARC files to, when they are ready. */
    private final Bitrepository bitrepository;
    /** The general settings used by Yggdrasil. */
    private final Config config;
    /** The remote preservation state updater.*/
    private final RemotePreservationStateUpdater remotePreservationStateUpdater;
    /** The StateDatase instance used by this workflow. */
    private final StateDatabase stateDatabase;
    /** Deals with HTTP communications. */
    private final HttpCommunication httpCommunication;

    /**
     * Constructor.
     * @param bitrepository The bitrepository to upload packaged files to.
     * @param config The configuration.
     * @param sd The state database.
     * @param remotePreservationStateUpdater The remote preservation state updater.
     * @param httpCommunication The http communication.
     */
    public RequestHandlerContext(Bitrepository bitrepository, Config config, StateDatabase sd, 
            RemotePreservationStateUpdater remotePreservationStateUpdater, HttpCommunication httpCommunication) {
        ArgumentCheck.checkNotNull(bitrepository, "Bitrepository bitrepository");
        ArgumentCheck.checkNotNull(config, "Config config");
        ArgumentCheck.checkNotNull(sd, "StateDatabase sd");
        ArgumentCheck.checkNotNull(remotePreservationStateUpdater, 
                "RemotePreservationStateUpdater remotePreservationStateUpdater");
        ArgumentCheck.checkNotNull(httpCommunication, "HttpCommunication httpCommunication");
        this.bitrepository = bitrepository;
        this.config = config;
        this.remotePreservationStateUpdater = remotePreservationStateUpdater;
        this.stateDatabase = sd;
        this.httpCommunication = httpCommunication;
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
    
    /**
     * @return The HTTP communication.
     */
    public HttpCommunication getHttpCommunication() {
        return httpCommunication;
    }
}
