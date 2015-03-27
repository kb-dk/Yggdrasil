package dk.kb.yggdrasil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.BlockingGetChecksumsClient;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getchecksums.conversation.ChecksumsCompletePillarEvent;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FilePart;
import org.bitrepository.client.eventhandler.BlockingEventHandler;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.client.eventhandler.OperationEvent.OperationEventType;
import org.bitrepository.client.exceptions.NegativeResponseException;
import org.bitrepository.commandline.clients.PagingGetFileIDsClient;
import org.bitrepository.commandline.eventhandler.CompleteEventAwaiter;
import org.bitrepository.commandline.eventhandler.GetFileEventHandler;
import org.bitrepository.commandline.output.DefaultOutputHandler;
import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.commandline.outputformatter.GetFileIDsInfoFormatter;
import org.bitrepository.commandline.outputformatter.GetFileIDsOutputFormatter;
import org.bitrepository.common.exceptions.OperationFailedException;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.modify.ModifyComponentFactory;
import org.bitrepository.modify.putfile.BlockingPutFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.bitrepository.protocol.security.BasicMessageAuthenticator;
import org.bitrepository.protocol.security.BasicMessageSigner;
import org.bitrepository.protocol.security.BasicOperationAuthorizor;
import org.bitrepository.protocol.security.BasicSecurityManager;
import org.bitrepository.protocol.security.MessageAuthenticator;
import org.bitrepository.protocol.security.MessageSigner;
import org.bitrepository.protocol.security.OperationAuthorizor;
import org.bitrepository.protocol.security.PermissionStore;
import org.bitrepository.protocol.security.SecurityManager;
import org.bitrepository.settings.repositorysettings.ClientSettings;
import org.bitrepository.settings.repositorysettings.Collection;

import dk.kb.yggdrasil.bitmag.BitrepositoryUtils;
import dk.kb.yggdrasil.bitmag.YggdrasilBlockingEventHandler;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/**
 * The class for interacting with the BitRepository, e.g. put files, get files, etc.
 * Currently works with bitrepository 1.0 archives.
 */
public class Bitrepository {

    /** Logging mechanism. */
    private static final Logger logger = Logger.getLogger(Bitrepository.class.getName());

    /** The archive settings directory needed to upload to
     * a bitmag style repository */
    private File settingsDir = null;

    /** National bitrepository settings. */
    private Settings bitmagSettings = null;

    /** The bitrepository component id. */
    private final String componentId;

    /** The bitmag security manager.*/
    private SecurityManager bitMagSecurityManager;

    /** The client for performing the PutFile operation.*/
    private PutFileClient bitMagPutClient;

    /** The client for performing the GetFile operation.*/
    private GetFileClient bitMagGetClient;

    /** The client for performing the GetFileID operation.*/
    private GetFileIDsClient bitMagGetFileIDsClient;

    /** The client for performing the GetChecksums operation.*/
    private GetChecksumsClient bitMagGetChecksumsClient;

    /** The client for performing the ReplaceFile operation.
    private ReplaceFileClient bitMagReplaceFileClient;
     */

    /** The client for performing the DeleteFile operation.
    private DeleteFileClient bitMagDeleteFileClient;
     */

    /** The authentication key used by the putfileClient. */
    private File privateKeyFile;

    /** The message bus used by the putfileClient. */
    private MessageBus bitMagMessageBus;

    /** Name of YAML property used to find settings dir. */
    public static final String YAML_BITMAG_SETTINGS_DIR_PROPERTY = "settings_dir";
    /** Name of YAML property used to find keyfile. */
    public static final String YAML_BITMAG_KEYFILE_PROPERTY = "keyfile";

    /** Name of the YAML sub-map client*/
    public static final String YAML_BITMAG_CLIENTS = "client";
    /** Name of the YAML property under the client sub-map for maximum number of pillars accept to fail.*/
    public static final String YAML_BITMAG_CLIENT_PUTFILE_MAX_PILLAR_FAILURES = "putfile_max_pillars_failures";

    /** The maximum number of failing pillars. Default is 0, can be overridden by settings in the bitmag.yml. */
    private int maxNumberOfFailingPillars = 0; 

    /**
     * Constructor for the BitRepository class.
     * @param configFile A YAML config file with links to the bitrepository settingsdir and keyfile
     * @throws YggdrasilException If the config file points to missing keyfile or settings directory
     * @throws ArgumentCheck if configFile is null or
     */
    public Bitrepository(File configFile) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(configFile, "File configFile");
        componentId = BitrepositoryUtils.generateComponentID();
        readConfigFile(configFile);
        initBitmagSettings();
        initBitmagSecurityManager();
        bitMagMessageBus = ProtocolComponentFactory.getInstance().getMessageBus(
                bitmagSettings, bitMagSecurityManager);
        initBitMagClients();
    }

    /**
     * Initialization of the various bitmag client.
     */
    private void initBitMagClients() {
        bitMagPutClient = ModifyComponentFactory.getInstance().retrievePutClient(
                bitmagSettings, bitMagSecurityManager, componentId);
        // Maybe needed later
        // bitMagDeleteFileClient = ModifyComponentFactory.getInstance().retrieveDeleteFileClient(
        //        bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
        // API:
        // bitMagDeleteFileClient.String collectionID, String fileId, String pillarId,
        // ChecksumDataForFileTYPE checksumForPillar, ChecksumSpecTYPE checksumRequested,
        // EventHandler eventHandler, String auditTrailInformation);

        // Maybe needed later
        // bitMagReplaceFileClient = ModifyComponentFactory.getInstance().retrieveReplaceFileClient(
        //        bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
        // API:
        // bitMagReplaceFileClient.replaceFile(String collectionID, String fileId, String pillarId,
        // ChecksumDataForFileTYPE checksumForDeleteAtPillar, ChecksumSpecTYPE checksumRequestedForDeletedFile,
        // URL url, long sizeOfNewFile, ChecksumDataForFileTYPE checksumForNewFileValidationAtPillar,
        // ChecksumSpecTYPE checksumRequestsForNewFile, EventHandler eventHandler, String auditTrailInformation);
        //
        AccessComponentFactory acf = AccessComponentFactory.getInstance();
        bitMagGetClient = acf.createGetFileClient(bitmagSettings, bitMagSecurityManager, componentId);
        bitMagGetFileIDsClient = acf.createGetFileIDsClient(bitmagSettings, bitMagSecurityManager, componentId);

        bitMagGetChecksumsClient = acf.createGetChecksumsClient(bitmagSettings, bitMagSecurityManager, componentId);
    }

    /**
     * Read the configfile, and initialize the settingsDir and privateKeyFile variables.
     * @param configFile The YAML configuration file.
     * @throws YggdrasilException If unable to find the relevant information in the given configFile
     *  or the configFile is null or does not exist.
     */
    private void readConfigFile(File configFile) throws YggdrasilException {
        if (configFile == null || !configFile.isFile()) {
            throw new YggdrasilException("ConfigFile '" + (configFile == null? "null" : configFile.getAbsolutePath())
                    + "' is undefined or missing. ");
        }
        Map yamlMap = YamlTools.loadYamlSettings(configFile);
        RunningMode mode = RunningMode.getMode();
        if (!yamlMap.containsKey(mode.toString())) {
            throw new YggdrasilException("Unable to find bitmag settings for the mode '"
                    + mode + "' in the given YAML file ' " + configFile.getAbsolutePath() + "'");
        }
        Map modeMap = (Map) yamlMap.get(mode.toString());
        if (!modeMap.containsKey(YAML_BITMAG_KEYFILE_PROPERTY)
                || !modeMap.containsKey(YAML_BITMAG_SETTINGS_DIR_PROPERTY)) {
            throw new YggdrasilException("Unable to find one or both properties (" + YAML_BITMAG_KEYFILE_PROPERTY 
                    + "," + YAML_BITMAG_SETTINGS_DIR_PROPERTY + ") using the current running mode '"
                    + mode + "' in the given YAML file ' " + configFile.getAbsolutePath() + "'");
        }

        this.settingsDir = new File((String) modeMap.get(YAML_BITMAG_SETTINGS_DIR_PROPERTY));
        this.privateKeyFile = new File((String) modeMap.get(YAML_BITMAG_KEYFILE_PROPERTY));
        if(modeMap.containsKey(YAML_BITMAG_CLIENTS)) {
            Map clientMap = (Map) modeMap.get(YAML_BITMAG_CLIENTS);
            if(clientMap.containsKey(YAML_BITMAG_CLIENT_PUTFILE_MAX_PILLAR_FAILURES)) {
                this.maxNumberOfFailingPillars = (Integer) clientMap.get(
                        YAML_BITMAG_CLIENT_PUTFILE_MAX_PILLAR_FAILURES);
            }            
        }
    }

    /**
     * Attempts to upload a given file.
     *
     * @param file The file to upload. Should exist. The packageId is the name of the file
     * @param collectionId The Id of the collection to upload to
     * @return true if the upload succeeded, false otherwise.
     */
    public boolean uploadFile(final File file, final String collectionId) {
        ArgumentCheck.checkExistsNormalFile(file, "File file");
        // Does collection exists? If not return false
        if (getCollectionPillars(collectionId).isEmpty()) {
            logger.warning("The given collection Id does not exist");
            return false;
        }
        boolean success = false;
        try {
            OperationEventType finalEvent = putTheFile(bitMagPutClient, file, collectionId);
            if(finalEvent == OperationEventType.COMPLETE) {
                success = true;
                logger.info("File '" + file.getAbsolutePath() + "' uploaded successfully. ");
            } else {
                logger.warning("Upload of file '" + file.getAbsolutePath()
                        + "' failed with event-type '" + finalEvent + "'.");
            }
        } catch (Exception e) {
            logger.warning("Unexpected error while storing file '"
                    + file.getAbsolutePath() + "': " + e);
            success = false;
        }
        return success;
    }

    /**
     * Upload the file to the uploadserver, initiate the PutFile request, and wait for the
     * request to finish.
     * @param client the PutFileClient responsible for the put operation.
     * @param packageFile The package to upload
     * @param collectionID The ID of the collection to upload to.
     * @return OperationEventType.FAILED if operation failed; otherwise returns OperationEventType.COMPLETE
     * @throws IOException If unable to upload the packageFile to the uploadserver
     */
    private OperationEventType putTheFile(PutFileClient client, File packageFile, String collectionID)
            throws IOException, URISyntaxException {
        FileExchange fileexchange = ProtocolComponentFactory.getInstance().getFileExchange(this.bitmagSettings);
        BlockingPutFileClient bpfc = new BlockingPutFileClient(client);
        URL url = fileexchange.uploadToServer(packageFile);
        String fileId = packageFile.getName();
        ChecksumSpecTYPE csSpec = ChecksumUtils.getDefault(this.bitmagSettings);
        ChecksumDataForFileTYPE validationChecksum = BitrepositoryUtils.getValidationChecksum(
                packageFile,csSpec);

        ChecksumSpecTYPE requestChecksum = null;
        String putFileMessage = "Putting the file '" + packageFile + "' with the file id '"
                + fileId + "' from Yggdrasil - the preservation service of Chronos.";

        YggdrasilBlockingEventHandler putFileEventHandler = new YggdrasilBlockingEventHandler(collectionID, 
                maxNumberOfFailingPillars);
        try {
            bpfc.putFile(collectionID, url, fileId, packageFile.length(), validationChecksum, requestChecksum,
                    putFileEventHandler, putFileMessage);
        } catch (OperationFailedException e) {
            logger.log(Level.WARNING, "The putFile Operation was not a complete success (" + putFileMessage + ")."
                    + " Checksum whether we accept anyway.", e);
            if(putFileEventHandler.hasFailed()) {
                return OperationEventType.FAILED;
            } else {
                return OperationEventType.COMPLETE;
            }
        } finally {
            // delete the uploaded file from server
            fileexchange.deleteFromServer(url);
        }
        logger.info("The putFile Operation succeeded (" + putFileMessage + ")");
        return OperationEventType.COMPLETE;
    }

    /**
     * Get a file with a given fileId from a given collection.
     * @param fileId A fileId of a package known to exist in the repository
     * @param collectionId A given collection in the repository
     * @return the file if found. Otherwise an exception is thrown
     * @throws YggdrasilException If not found or an error occurred during the fetch process.
     */
    public File getFile(final String fileId, final String collectionId) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(fileId, "String fileId");
        ArgumentCheck.checkNotNullOrEmpty(collectionId, "String collectionId");
        // Does collection exists? If not throw exception
        if (getCollectionPillars(collectionId).isEmpty()) {
            throw new YggdrasilException("The given collection Id does not exist");
        }
        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
        URL fileUrl = getDeliveryUrl(fileId);
        // Note that this eventHandler is blocking
        CompleteEventAwaiter eventHandler = new GetFileEventHandler(this.bitmagSettings, output);
        output.debug("Initiating the GetFile conversation.");
        FilePart filePart = null; // Means whole file (otherwise the filepart is specified by offset and length
        String auditTrailInformation = "Retrieving package '" + fileId + "' from collection '" + collectionId + "'";
        bitMagGetClient.getFileFromFastestPillar(collectionId, fileId, filePart, fileUrl, eventHandler,
                auditTrailInformation);

        OperationEvent finalEvent = eventHandler.getFinish();
        if(finalEvent.getEventType() == OperationEventType.COMPLETE) {
            File result = null;
            try {
                result = downloadFile(fileUrl);
            } catch (IOException e) {
                throw new YggdrasilException(
                        "Download was successful, but we failed to create result File: ", e);
            }
            return result;
        } else {
            throw new YggdrasilException("Download of package w/ id '" + fileId + "' failed. Reason: "
                    + finalEvent.getInfo());
        }
    }

    /**
     * Downloads the file from the URL defined in the conversation.
     * @throws IOException
     */
    private File downloadFile(URL fileUrl) throws IOException {
        File outputFile = File.createTempFile("Extracted", null);
        FileExchange fileexchange = getFileExchange(bitmagSettings);
        fileexchange.downloadFromServer(outputFile, fileUrl.toExternalForm());
        return outputFile;
    }

    /**
     * Generates the URL for where the file should be delivered from the GetFile operation.
     * @param fileId The id of the file.
     * @return The URL where the file should be located.
     */
    private URL getDeliveryUrl(String fileId) {
        try {
            return getFileExchange(bitmagSettings).getURL(fileId);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not make an URL for the file '"
                    + fileId + "'.", e);
        }
    }

    /**
     * Check if a package with the following id exists within a specific collection.
     * @param packageId A given packageId
     * @param collectionID A given collection ID
     * @return true, if a package with the given ID exists within the given collection. Otherwise returns false
     */
    public boolean existsInCollection(String packageId, String collectionID) {
        ArgumentCheck.checkNotNullOrEmpty(packageId, "String packageId");
        ArgumentCheck.checkNotNullOrEmpty(collectionID, "String collectionId");
        // Does collection exists? If not return false
        if (getCollectionPillars(collectionID).isEmpty()) {
            logger.warning("The given collection Id does not exist");
            return false;
        }

        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);

        output.debug("Instantiation GetFileID outputFormatter.");
        // TODO: change to non pagingClient
        GetFileIDsOutputFormatter outputFormatter = new GetFileIDsInfoFormatter(output);

        long timeout = getClientTimeout(bitmagSettings);

        output.debug("Instantiation GetFileID paging client.");
        PagingGetFileIDsClient pagingClient = new PagingGetFileIDsClient(
                bitMagGetFileIDsClient, timeout, outputFormatter, output);

        Boolean success = pagingClient.getFileIDs(collectionID, packageId,
                getCollectionPillars(collectionID));
        return success;
    }

    /**
     * Check the checksums for a whole collection, or only a single packageId in a collection.
     * @param packageID A given package ID (if null, checksums for the whole collection is requested)
     * @param collectionID A given collection ID
     * @return a map with the results from the pillars
     * @throws YggdrasilException If it fails to retrieve the checksums.
     */
    public Map<String, ChecksumsCompletePillarEvent> getChecksums(String packageID, String collectionID) 
            throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(collectionID, "String collectionId");
        //If packageID = null, checksum is requested for all files in the collection.
        if (packageID != null) {
            logger.info("Collecting checksums for package '" + packageID + "' in collection '" + collectionID + "'");
        } else {
            logger.info("Collecting checksums for all packages in collection '" + collectionID + "'");
        }
        BlockingGetChecksumsClient client = new BlockingGetChecksumsClient(bitMagGetChecksumsClient);
        ChecksumSpecTYPE checksumSpec = ChecksumUtils.getDefault(bitmagSettings);
        BlockingEventHandler eventhandler = new BlockingEventHandler();

        try {
            client.getChecksums(collectionID, null, packageID, checksumSpec, null, eventhandler, null);
        } catch (NegativeResponseException e) {
            throw new YggdrasilException("Got bad feedback from the bitrepository " + e);
        }

        int failures = eventhandler.getFailures().size();
        int results = eventhandler.getResults().size();

        if (failures > 0) {
            logger.warning("Got back " + eventhandler.getFailures().size() + " failures");
        }
        if (results > 0) {
            logger.info("Got back " + eventhandler.getResults().size() + " successful responses");
        }

        Map<String, ChecksumsCompletePillarEvent> resultsMap = new HashMap<String, ChecksumsCompletePillarEvent>();

        for (ContributorEvent e : eventhandler.getResults()) {
            ChecksumsCompletePillarEvent event = (ChecksumsCompletePillarEvent) e;
            resultsMap.put(event.getContributorID(), event);
        }
        return resultsMap;
    }

    /**
     * Initialize the BITMAG security manager.
     */
    private void initBitmagSecurityManager() {
        PermissionStore permissionStore = new PermissionStore();
        MessageAuthenticator authenticator = new BasicMessageAuthenticator(permissionStore);
        MessageSigner signer = new BasicMessageSigner();
        OperationAuthorizor authorizer = new BasicOperationAuthorizor(permissionStore);

        bitMagSecurityManager = new BasicSecurityManager(bitmagSettings.getRepositorySettings(),
                getPrivateKeyFile().getAbsolutePath(),
                authenticator, signer, authorizer, permissionStore,
                bitmagSettings.getComponentID());
    }

    private File getPrivateKeyFile() {
        return this.privateKeyFile;
    }

    /**
     * Load BitMag settings, if not already done.
     */
    private void initBitmagSettings() {
        if (bitmagSettings == null) {
            SettingsProvider settingsLoader =
                    new SettingsProvider(
                            new XMLFileSettingsLoader(
                                    settingsDir.getAbsolutePath()),
                                    componentId);
            bitmagSettings = settingsLoader.getSettings();
            SettingsUtils.initialize(bitmagSettings);
        }
    }

    /**
     * Shutdown the messagebus.
     */
    public void shutdown() {
        if (bitMagMessageBus != null) {
            try {
                bitMagMessageBus.close();
            } catch (JMSException e) {
                logger.warning("JMSException caught during shutdown of messagebus " + e);
            }
        }
    }

    /**
     * Helper method for reading the list of pillars preserving the given collection.
     * @param collectionID The ID of a specific collection.
     * @return the list of pillars preserving the collection with the given ID.
     */
    private List<String> getCollectionPillars(String collectionID) {
        return SettingsUtils.getPillarIDsForCollection(collectionID);
    }

    /**
     * Helper method for computing the clientTimeout. The clientTimeout is the identificationTimeout
     * plus the OperationTimeout.
     * @param bitmagSettings The bitmagsettingg
     * @return the clientTimeout
     */
    private long getClientTimeout(Settings bitmagSettings) {
        ClientSettings clSettings = bitmagSettings.getRepositorySettings().getClientSettings();
        return clSettings.getIdentificationTimeout().longValue()
                + clSettings.getOperationTimeout().longValue();
    }

    private FileExchange getFileExchange(Settings bitmagSettings) {
        return ProtocolComponentFactory.getInstance().getFileExchange(
                bitmagSettings);
    }

    /**
     * @return a set of known CollectionIDs
     */
    public List<String> getKnownCollections() {
        List<Collection> knownCollections = bitmagSettings.getRepositorySettings()
                .getCollections().getCollection();
        List<String> collectionIDs = new ArrayList<String>();
        for (Collection c: knownCollections) {
            collectionIDs.add(c.getID());
        }
        return collectionIDs;
    }
}
