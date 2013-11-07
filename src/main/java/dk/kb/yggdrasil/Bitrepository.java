package dk.kb.yggdrasil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jms.JMSException;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.BlockingGetChecksumsClient;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FilePart;
import org.bitrepository.client.eventhandler.BlockingEventHandler;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.client.eventhandler.OperationEvent.OperationEventType;
import org.bitrepository.client.exceptions.NegativeResponseException;
import org.bitrepository.commandline.clients.PagingGetChecksumsClient;
import org.bitrepository.commandline.clients.PagingGetFileIDsClient;
import org.bitrepository.commandline.eventhandler.CompleteEventAwaiter;
import org.bitrepository.commandline.eventhandler.GetFileEventHandler;
import org.bitrepository.commandline.output.DefaultOutputHandler;
import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.commandline.outputformatter.GetChecksumDistributionFormatter;
import org.bitrepository.commandline.outputformatter.GetChecksumsInfoFormatter;
import org.bitrepository.commandline.outputformatter.GetChecksumsOutputFormatter;
import org.bitrepository.commandline.outputformatter.GetFileIDsInfoFormatter;
import org.bitrepository.commandline.outputformatter.GetFileIDsOutputFormatter;
import org.bitrepository.common.exceptions.OperationFailedException;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.common.utils.FileUtils;
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
    
    /** The component id. */
    private final static String COMPONENT_ID = "YggdrasilClient";

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
 
    /**
     * Constructor for the BitRepository class.
     * @param configFile A YAML config file with links to the bitrepository settingsdir and keyfile
     * @throws YggdrasilException If the config file points to missing keyfile or settings directory
     * @throws ArgumentCheck if configFile is null or  
     */
    public Bitrepository(File configFile) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(configFile, "File configFile");
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
                bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
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
        bitMagGetClient = acf.createGetFileClient(
                bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
        bitMagGetFileIDsClient = acf.createGetFileIDsClient(
                bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
        
        bitMagGetChecksumsClient = acf.createGetChecksumsClient
                (bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
    }

    /**
     * Read the configfile, and initialize the settingsDir and privateKeyFile variables.  
     * @param configFile The YAML configuration file.
     * @throws YggdrasilException If unable to find the relevant information in the given configFile 
     *  or the configFile is null or does not exist.
     */
    private void readConfigFile(File configFile) throws YggdrasilException {
        if (configFile == null || !configFile.isFile()) {
            throw new YggdrasilException("ConfigFile '" 
                    + (configFile == null? "null" : configFile.getAbsolutePath()) 
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
            throw new YggdrasilException("Unable to find one or both properties (" 
                    + YAML_BITMAG_KEYFILE_PROPERTY + "," 
                    + YAML_BITMAG_SETTINGS_DIR_PROPERTY + ") using the current running mode '"
                    + mode + "' in the given YAML file ' " + configFile.getAbsolutePath() + "'");
        }
        
        this.settingsDir = new File((String) modeMap.get(YAML_BITMAG_SETTINGS_DIR_PROPERTY));
        this.privateKeyFile = new File((String) modeMap.get(YAML_BITMAG_KEYFILE_PROPERTY));
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
        FileExchange fileexchange 
            = ProtocolComponentFactory.getInstance().getFileExchange(this.bitmagSettings);
        BlockingPutFileClient bpfc = new BlockingPutFileClient(client);
        URL url = fileexchange.uploadToServer(packageFile);
        String fileId = packageFile.getName();
        ChecksumSpecTYPE csSpec = ChecksumUtils.getDefault(this.bitmagSettings);
        ChecksumDataForFileTYPE validationChecksum = BitrepositoryUtils.getValidationChecksum(
                packageFile,csSpec); 
       
        ChecksumSpecTYPE requestChecksum = null;
        String putFileMessage = "Putting the file '" + packageFile + "' with the file id '" 
                + fileId + "' from Yggdrasil - the SIFD preservation service.";
        
        EventHandler putFileEventHandler = new BlockingEventHandler();
       /*
        // TODO For the moment the eventhandler only logs the progress.
        // Later, some or all of the events could result in updates being sent back to Valhal
        // TODO Make also the eventHandler a separate class.
        EventHandler putFileEventHandler = new EventHandler() {
            @Override
            public void handleEvent(OperationEvent event) {
                logger.info("Event " + event.getEventType() 
                        + " received related to putFile of package");     
            }
        };
        */
        try {
            bpfc.putFile(collectionID, url, fileId, packageFile.length(), validationChecksum, requestChecksum, 
                    putFileEventHandler, putFileMessage);
            
        } catch (OperationFailedException e) {
            logger.warning("The putFile Operation failed (" + putFileMessage + ")" + e);
            return OperationEventType.FAILED;
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
    * FIXME Complete this method. Still unclear how the result should be treated.
    * Check the checksums for a whole collection, or only a single packageId in a collection.
    * @param packageId A given package ID (if null, checksums for the whole collection is requested)
    * @param collectionID A given collection ID
    * @throws IOException 
    * @throws YggdrasilException 
    */
   public void getChecksums(String packageID, String collectionID) throws IOException, YggdrasilException {
       ArgumentCheck.checkNotNullOrEmpty(collectionID, "String collectionId");
       //If packageID = null, checksum is requested for all files in the collection.
       OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
       GetChecksumsOutputFormatter outputFormatter = null;
       if (packageID != null) {
           outputFormatter = new GetChecksumDistributionFormatter(output);
       } else {
           outputFormatter = new GetChecksumsInfoFormatter(output);
       }
       List<String> pillarIDs =  getCollectionPillars(collectionID);
       BlockingGetChecksumsClient client = new BlockingGetChecksumsClient(bitMagGetChecksumsClient);
       
       ChecksumSpecTYPE checksumSpec = ChecksumUtils.getDefault(bitmagSettings);
       //PagingGetChecksumsClient pagingClient = new PagingGetChecksumsClient(bitMagGetChecksumsClient, 
       //        getClientTimeout(bitmagSettings), outputFormatter, output);
       UUID f = UUID.randomUUID();
       URL deliveryUrl = getDeliveryUrl(f.toString());
       BlockingEventHandler eventhandler = new BlockingEventHandler();
       List<ContributorEvent> result = null;
       try {
           result = client.getChecksums(collectionID, null, packageID, checksumSpec, deliveryUrl, 
                   eventhandler, null);
       } catch (NegativeResponseException e) {
           throw new YggdrasilException("Got bad feedback from the bitrepository " + e);
       }
       
       if (result != null) {
           for (ContributorEvent e : result) {
               System.out.println(e.getFileID());
               System.out.println(e.getInfo());
               System.out.println(e.additionalInfo());
               
           }
          //File outputFile = downloadFile(deliveryUrl);
          //System.out.println(org.apache.commons.io.FileUtils.readFileToString(outputFile));
       }
       
       
       
       //Boolean result = pagingClient.getChecksums(collectionID, packageID, pillarIDs, 
       //        checksumSpec);
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
                        COMPONENT_ID);
            bitmagSettings = settingsLoader.getSettings();
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
                this.bitmagSettings);
    }
    
}