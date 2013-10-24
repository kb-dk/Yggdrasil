package dk.kb.yggdrasil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.client.eventhandler.OperationEvent.OperationEventType;
import org.bitrepository.commandline.eventhandler.CompleteEventAwaiter;
import org.bitrepository.commandline.eventhandler.GetFileEventHandler;
import org.bitrepository.commandline.output.DefaultOutputHandler;
import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.common.exceptions.OperationFailedException;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.modify.ModifyComponentFactory;
import org.bitrepository.modify.putfile.BlockingPutFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.bitrepository.protocol.http.HttpFileExchange;
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

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * An class that uploads to and get files from a Bitrepository 1.0 type archive.
 */
public class Bitrepository {

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
    
    /** The authentication key used by the putfileClient. */
    private File privateKeyFile;
    
    /** The message bus used by the putfileClient. */
    private MessageBus bitMagMessageBus;

    /**
     * @param bitrepSettingsDir 
     */
    public Bitrepository(File settingsDir, File bitmagKeyFile) {
        this.settingsDir = settingsDir;
        this.privateKeyFile = bitmagKeyFile;
        initBitmagSettings();
        initBitmagSecurityManager();
        bitMagMessageBus = ProtocolComponentFactory.getInstance().getMessageBus(
                bitmagSettings, bitMagSecurityManager);
        bitMagPutClient = ModifyComponentFactory.getInstance().retrievePutClient(
                bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
        bitMagGetClient = AccessComponentFactory.getInstance().createGetFileClient(
                this.bitmagSettings, bitMagSecurityManager, COMPONENT_ID);
    }
    
    /**
     * Attempts to upload a given file.
     *
     * @param file The file to upload. Should exist
     * @param collectionId The Id of the collection to upload to 
     * @return true if the upload succeeded, false otherwise.
     */

    public boolean uploadFile(final File file, final String collectionId) {
        //Assert.NOT_VALID.assertNotNull("File to upload should never be null", 
        //        file);
        //Assert.NOT_VALID.assertIsFile(
        //        "The given file '" + file.getAbsolutePath() 
        //        + "' should exist but does not!", file);
         
        boolean success = false;
        try {
            OperationEventType finalEvent = putTheFile(bitMagPutClient, file, collectionId);         
            if(finalEvent == OperationEventType.COMPLETE) {
                success = true;
                //Alert.ALL_DEBUG.message("File '" + file.getAbsolutePath() 
                //        + "' uploaded successfully. ");
            } else {
                //Alert.ALL_DEBUG.message("Upload of file '" + file.getAbsolutePath() 
                //        + "' failed with event-type: " + finalEvent);
            }
        } catch (Exception e) {
            //Alert.ALL_ASAP.message("Error while storing file '"
            //        + file.getAbsolutePath() + "': " + e);
            success = false;
        } 
        return success;
    }
    
    private OperationEventType putTheFile(PutFileClient client, File f, String collectionID) 
            throws IOException, URISyntaxException {      
        FileExchange fileexchange 
            = ProtocolComponentFactory.getInstance().getFileExchange(this.bitmagSettings);
        BlockingPutFileClient bpfc = new BlockingPutFileClient(client);
        URL url = fileexchange.uploadToServer(f);
        String fileId = f.getName();
        ChecksumDataForFileTYPE validationChecksum = getValidationChecksum(f);
        // TODO The value of requestChecksum is currently always null
        ChecksumSpecTYPE requestChecksum = getRequestChecksumSpec();
        String putFileMessage = "Putting the file '" + f + "' with the file id '" 
                + fileId + "' from Yggdrasil - the SIFD preservation service.";
        try {
            bpfc.putFile(collectionID, url, fileId, f.length(), validationChecksum, requestChecksum, 
                    (EventHandler) null, putFileMessage);
            
        } catch (OperationFailedException e) {
            //Alert.ALL_ASAP.message("The putFile Operation failed (" + putFileMessage + ")", e);
            return OperationEventType.FAILED;
        } finally {
            // delete the uploaded file from server
            fileexchange.deleteFromServer(url);
            
        }
        //Alert.ALL_DEBUG.message("The putFile Operation succeeded (" + putFileMessage + ")");
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
        
        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
        URL fileUrl = extractUrl(fileId);
        
        CompleteEventAwaiter eventHandler = new GetFileEventHandler(this.bitmagSettings, output);
        output.debug("Initiating the GetFile conversation.");
        
        bitMagGetClient.getFileFromSpecificPillar(collectionId, 
                    fileId, null, fileUrl, "pillarId", eventHandler, null);
        
        bitMagGetClient.getFileFromFastestPillar(collectionId, fileId, null, fileUrl, eventHandler, null);
    
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
        FileExchange fileexchange = ProtocolComponentFactory.getInstance().getFileExchange(
                this.bitmagSettings);
        fileexchange.downloadFromServer(outputFile, fileUrl.toExternalForm());
        return outputFile;
    }

    /**
     * Extracts the URL for where the file should be delivered from the GetFile operation.
     * @param fileId The id of the file.
     * @return The URL where the file should be located.
     */
    private URL extractUrl(String fileId) {
        try {
            FileExchange fileexchange = new HttpFileExchange(this.bitmagSettings);
            return fileexchange.getURL(fileId);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not make an URL for the file '" 
                    + fileId + "'.", e);
        }
    }
    
    /**
     * Creates the data structure for encapsulating the validation checksums for validation of the PutFile operation.
     * @param file The file to have the checksum calculated.
     * @return The ChecksumDataForFileTYPE for the pillars to validate the PutFile operation.
     */

   private ChecksumDataForFileTYPE getValidationChecksum(File file) {
       ChecksumSpecTYPE csSpec = ChecksumUtils.getDefault(this.bitmagSettings);
       String checksum = ChecksumUtils.generateChecksum(file, csSpec);
       ChecksumDataForFileTYPE res = new ChecksumDataForFileTYPE();
       res.setCalculationTimestamp(CalendarUtils.getNow());
       res.setChecksumSpec(csSpec);
       res.setChecksumValue(Base16Utils.encodeBase16(checksum));
       return res;
   }


   /**
    *  The REQUEST_CHECKSUM functionality is not yet implemented.
    *  Currently always return null.
     * @return The requested checksum spec, or null if the arguments does not exist.
     */

   private ChecksumSpecTYPE getRequestChecksumSpec() {
       return null;
       
       // If request checksum required, uncomment the following code.
       //ChecksumSpecTYPE res = new ChecksumSpecTYPE();
       //res.setChecksumType(ChecksumType.SHA512); // or another one according to need. 
       
       // If checksum salting required
       // then use command: 
       //    res.setChecksumSalt(Base16Utils.encodeBase16(arg0)
       
       //return res;

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
     * @return the absolute path to the private keyfile.
     */
    public File getPrivateKeyfile() {
        /* Assert.NOT_VALID.assertNotNull(
                "The private Keyfile should not be null and should exist", 
                this.privateKeyFile); */
        return this.privateKeyFile;
    }

    
    public void shutdown() {
        if (bitMagMessageBus != null) {
            try {
                bitMagMessageBus.close();
            } catch (javax.jms.JMSException e) {
                e.printStackTrace();
            }
        }

    }
}
