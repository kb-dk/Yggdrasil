package dk.kb.yggdrasil.preservation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.mockito.Mockito;

import dk.kb.yggdrasil.Bitrepository;
import dk.kb.yggdrasil.Config;
import dk.kb.yggdrasil.MetadataContentUtils;
import dk.kb.yggdrasil.State;
import dk.kb.yggdrasil.db.PreservationRequestState;
import dk.kb.yggdrasil.db.StateDatabase;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.json.PreservationRequest;

@RunWith(JUnit4.class)
public class PreservationPackerTest {
    protected static final String NON_RANDOM_UUID = "random-uuid";
    protected static final String NON_RANDOM_FILE_UUID = "random-file-uuid";
    protected static final String DEFAULT_COLLECTION = "collection";
    protected static final String WARC_FILE_ID = "warc-file.warc";

    private static File generalConfigFile = new File("config/yggdrasil.yml");
    
    protected static Bitrepository bitrepository;
    protected static Config config;
    protected static StateDatabase stateDatabase;
    
    protected static File metadataPayloadFile;
    protected static File contentFilePayloadFile;

    @BeforeClass
    public static void beforeClass() throws Exception {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");

        config = new Config(generalConfigFile);
        FileUtils.deleteDirectory(config.getDatabaseDir());

        bitrepository = Mockito.mock(Bitrepository.class);

        stateDatabase = new StateDatabase(config.getDatabaseDir());
        
        metadataPayloadFile = new File(config.getTemporaryDir(), "metadataPayloadFile.txt");
        if(!metadataPayloadFile.isFile()) {
            FileOutputStream fos = new FileOutputStream(metadataPayloadFile);
            fos.write(new String("This is the metadata payload file").getBytes());
            fos.close();
        }
        Assert.assertTrue(metadataPayloadFile.isFile());
        
        contentFilePayloadFile = new File(config.getTemporaryDir(), "contentFilePayloadFile.txt");
        if(!contentFilePayloadFile.isFile()) {
            FileOutputStream fos = new FileOutputStream(contentFilePayloadFile);
            fos.write(new String("This is the contentFile payload file").getBytes());
            fos.close();
        }
        Assert.assertTrue(metadataPayloadFile.isFile());

        Assert.assertTrue(metadataPayloadFile.length() != contentFilePayloadFile.length());
    }

    @Test
    public void writePreservationRecordWithoutFiles() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);

        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());

        packer.writePreservationRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNotNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());
    }
    
    @Test
    public void writePreservationRecordWithoutFilesValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);

        packer.writePreservationRecord(prs);
        
        // Verify WARC file
        // Must only contain the warcinfo record.
        File warcFile = new File(config.getTemporaryDir(), prs.getWarcId());
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertNotNull(records);
        Assert.assertEquals(records.size(), 1);
        Assert.assertEquals(((WarcRecord) records.values().toArray()[0]).header.warcTypeStr, "warcinfo");
    }

    @Test
    public void writePreservationRecordWithMetadataFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setMetadataPayload(metadataPayloadFile);

        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());

        packer.writePreservationRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNotNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());
    }

    @Test
    public void writePreservationRecordWithMetadataFileValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setMetadataPayload(metadataPayloadFile);

        packer.writePreservationRecord(prs);
        
        // Verify WARC file
        // Should contain 2 warc-records. 
        // Including the packaged metadata in a warc-record with the UUID 
        File warcFile = new File(config.getTemporaryDir(), prs.getWarcId());
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 2);
        
        // look at packaged metadata
        String metadataRecordUUID = "urn:uuid:" + prs.getUUID();
        Assert.assertTrue(records.containsKey(metadataRecordUUID));
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcTypeStr, "metadata");
        Assert.assertNull(records.get(metadataRecordUUID).header.warcRefersToStr); 
        Assert.assertEquals(records.get(metadataRecordUUID).header.contentLengthStr, "" + metadataPayloadFile.length());
    }
    
    @Test
    public void writePreservationRecordWithResourceFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);

        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());

        packer.writePreservationRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNotNull(prs.getWarcId());
        Assert.assertNotNull(prs.getFileWarcId());
        Assert.assertEquals(prs.getWarcId(), prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());
    }
    
    @Test
    public void writePreservationRecordWithResourceFileValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);

        packer.writePreservationRecord(prs);
        
        // Verify WARC file
        // Should contain 2 warc-records. 
        // Including the packaged resource in a warc-record with the file_UUID 
        File warcFile = new File(config.getTemporaryDir(), prs.getWarcId());
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 2);
        
        // look at packaged resource
        String fileRecordUUID = "urn:uuid:" + prs.getRequest().File_UUID;
        Assert.assertTrue(records.containsKey(fileRecordUUID));
        Assert.assertEquals(records.get(fileRecordUUID).header.warcTypeStr, "resource");
        Assert.assertNull(records.get(fileRecordUUID).header.warcRefersToStr);
        Assert.assertEquals(records.get(fileRecordUUID).header.contentLengthStr, "" + contentFilePayloadFile.length());
    }

    @Test
    public void writePreservationRecordWithBothFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(metadataPayloadFile);
        prs.setMetadataPayload(contentFilePayloadFile);

        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());

        packer.writePreservationRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNotNull(prs.getWarcId());
        Assert.assertNotNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());
    }

    @Test
    public void writePreservationRecordWithBothFileValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);
        prs.setMetadataPayload(metadataPayloadFile);

        packer.writePreservationRecord(prs);
        
        // Verify WARC file
        // Should contain 3 warc-records. 
        // Including the packaged metadata and packaged resource in a warc-records 
        File warcFile = new File(config.getTemporaryDir(), prs.getWarcId());
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 3);

        // look at packaged resource
        String fileRecordUUID = "urn:uuid:" + prs.getRequest().File_UUID;
        Assert.assertTrue(records.containsKey(fileRecordUUID));
        Assert.assertEquals(records.get(fileRecordUUID).header.warcTypeStr, "resource");
        Assert.assertNull(records.get(fileRecordUUID).header.warcRefersToStr); 
        Assert.assertEquals(records.get(fileRecordUUID).header.contentLengthStr, "" + contentFilePayloadFile.length());

        // look at packaged metadata
        String metadataRecordUUID = "urn:uuid:" + prs.getUUID();
        Assert.assertTrue(records.containsKey(metadataRecordUUID));
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcTypeStr, "metadata");
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcRefersToStr, "<" + fileRecordUUID + ">"); 
        Assert.assertEquals(records.get(metadataRecordUUID).header.contentLengthStr, "" + metadataPayloadFile.length());
    }

    @Test(expected = ArgumentCheck.class)
    public void writeUpdateRecordFailure() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);

        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());

        packer.writeUpdateRecord(prs);
    }
    
    @Test
    public void writeUpdateRecordWithMetadataFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setMetadataPayload(metadataPayloadFile);
        
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());

        packer.writeUpdateRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNotNull(prs.getUpdatePreservation());
        Assert.assertNotNull(prs.getUpdatePreservation().date);
        Assert.assertNotNull(prs.getUpdatePreservation().uuid);
        Assert.assertNotNull(prs.getUpdatePreservation().warc_id);
        Assert.assertNull(prs.getUpdatePreservation().file_uuid);
        Assert.assertNull(prs.getUpdatePreservation().file_warc_id);
    }
    
    @Test
    public void writeUpdateRecordWithMetadataFileValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setMetadataPayload(metadataPayloadFile);

        packer.writeUpdateRecord(prs);
        
        // Verify WARC file
        // Should contain 2 warc-records. 
        // Including the update record for the metadata 
        File warcFile = new File(config.getTemporaryDir(), prs.getUpdatePreservation().warc_id);
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 2);

        // look at packaged metadata
        String metadataRecordUUID = "urn:uuid:" + prs.getUpdatePreservation().uuid;
        Assert.assertTrue(records.containsKey(metadataRecordUUID));
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcTypeStr, "update");
        Assert.assertNull(records.get(metadataRecordUUID).header.warcRefersToStr); 
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcConcurrentToList.size(), 1);
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcConcurrentToList.get(0).warcConcurrentToStr, "<" + prs.getUUID() + ">");
        Assert.assertEquals(records.get(metadataRecordUUID).header.contentLengthStr, "" + metadataPayloadFile.length());
    }
    
    @Test
    public void writeUpdateRecordWithContentFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(metadataPayloadFile);
        
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());

        packer.writeUpdateRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNotNull(prs.getUpdatePreservation());
        Assert.assertNotNull(prs.getUpdatePreservation().date);
        Assert.assertNull(prs.getUpdatePreservation().uuid);
        Assert.assertNull(prs.getUpdatePreservation().warc_id);
        Assert.assertNotNull(prs.getUpdatePreservation().file_uuid);
        Assert.assertNotNull(prs.getUpdatePreservation().file_warc_id);
    }

    @Test
    public void writeUpdateRecordWithContentFileValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);
        
        packer.writeUpdateRecord(prs);
        
        // Verify WARC file
        // Should contain 2 warc-records. 
        // Including the update record for the resource 
        File warcFile = new File(config.getTemporaryDir(), prs.getUpdatePreservation().file_warc_id);
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 2);

        // look at packaged resource
        String fileRecordUUID = "urn:uuid:" + prs.getUpdatePreservation().file_uuid;
        Assert.assertTrue(records.containsKey(fileRecordUUID));
        Assert.assertEquals(records.get(fileRecordUUID).header.warcTypeStr, "update");
        Assert.assertNull(records.get(fileRecordUUID).header.warcRefersToStr); 
        Assert.assertEquals(records.get(fileRecordUUID).header.warcConcurrentToList.size(), 1);
        Assert.assertEquals(records.get(fileRecordUUID).header.warcConcurrentToList.get(0).warcConcurrentToStr, "<" + prs.getRequest().File_UUID + ">");
        Assert.assertEquals(records.get(fileRecordUUID).header.contentLengthStr, "" + contentFilePayloadFile.length());
    }
    
    @Test
    public void writeUpdateRecordWithBothFiles() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);
        prs.setMetadataPayload(metadataPayloadFile);
        
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNull(prs.getUpdatePreservation());

        packer.writeUpdateRecord(prs);
        
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_RESOURCES_PACKAGE_SUCCESS));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_COMPLETE));
        verify(updater).sendPreservationResponse(any(PreservationRequestState.class), eq(State.PRESERVATION_PACKAGE_WAITING_FOR_MORE_DATA));
        verifyNoMoreInteractions(updater);
        Assert.assertNull(prs.getWarcId());
        Assert.assertNull(prs.getFileWarcId());
        Assert.assertNotNull(prs.getUpdatePreservation());
        Assert.assertNotNull(prs.getUpdatePreservation().date);
        Assert.assertNotNull(prs.getUpdatePreservation().uuid);
        Assert.assertNotNull(prs.getUpdatePreservation().warc_id);
        Assert.assertNotNull(prs.getUpdatePreservation().file_uuid);
        Assert.assertNotNull(prs.getUpdatePreservation().file_warc_id);
    }
    
    @Test
    public void writeUpdateRecordWithBothFilesValidateWarcFile() throws Exception {
        RemotePreservationStateUpdater updater = Mockito.mock(RemotePreservationStateUpdater.class);
        PreservationContext context = new PreservationContext(bitrepository, config, stateDatabase, updater);
        PreservationPacker packer = new PreservationPacker(context, "test-collection");
        PreservationRequest request = makeRequest();
        request.warc_id = WARC_FILE_ID;
        
        PreservationRequestState prs = new PreservationRequestState(request, State.PRESERVATION_REQUEST_RECEIVED, request.UUID);
        prs.setContentPayload(contentFilePayloadFile);
        prs.setMetadataPayload(metadataPayloadFile);

        packer.writeUpdateRecord(prs);
        
        // Verify WARC file
        // Should contain 3 warc-records. 
        // Including the update record for the resource 
        File warcFile = new File(config.getTemporaryDir(), prs.getUpdatePreservation().file_warc_id);
        Assert.assertTrue(warcFile.isFile());
        Map<String, WarcRecord> records = retrieveWarcRecords(warcFile);
        Assert.assertEquals(records.size(), 3);

        // look at packaged resource
        String fileRecordUUID = "urn:uuid:" + prs.getUpdatePreservation().file_uuid;
        Assert.assertTrue(records.containsKey(fileRecordUUID));
        Assert.assertEquals(records.get(fileRecordUUID).header.warcTypeStr, "update");
        Assert.assertNull(records.get(fileRecordUUID).header.warcRefersToStr); 
        Assert.assertEquals(records.get(fileRecordUUID).header.warcConcurrentToList.size(), 1);
        Assert.assertEquals(records.get(fileRecordUUID).header.warcConcurrentToList.get(0).warcConcurrentToStr, "<" + prs.getRequest().File_UUID + ">");
        Assert.assertEquals(records.get(fileRecordUUID).header.contentLengthStr, "" + contentFilePayloadFile.length());

        // look at packaged metadata
        String metadataRecordUUID = "urn:uuid:" + prs.getUpdatePreservation().uuid;
        Assert.assertTrue(records.containsKey(metadataRecordUUID));
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcTypeStr, "update");
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcRefersToStr, "<" + fileRecordUUID + ">"); 
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcConcurrentToList.size(), 1);
        Assert.assertEquals(records.get(metadataRecordUUID).header.warcConcurrentToList.get(0).warcConcurrentToStr, "<" + prs.getUUID() + ">");
        Assert.assertEquals(records.get(metadataRecordUUID).header.contentLengthStr, "" + metadataPayloadFile.length());
    }
    
    public static PreservationRequest makeRequest() {
        PreservationRequest request = new PreservationRequest();
        request.Content_URI = null;
        request.File_UUID = NON_RANDOM_FILE_UUID;
        request.metadata = MetadataContentUtils.getExampleInstanceMetadata();
        request.Model = "instance";
        request.Preservation_profile = DEFAULT_COLLECTION;
        request.UUID = NON_RANDOM_UUID;
        request.Valhal_ID = "ID";
        return request;
    }
    
    public static Map<String, WarcRecord> retrieveWarcRecords(File warcFile) {
        Map<String, WarcRecord> res = new HashMap<String, WarcRecord>();
        try {
            WarcReader reader = WarcReaderFactory.getReader(new FileInputStream(warcFile));
            WarcRecord record;
            while ( (record = reader.getNextRecord()) != null ) {
                res.put(record.header.warcRecordIdUri.toString(), record);
                System.out.println(record.header.warcRecordIdUri.toString() + " -> " + record.header.warcTypeStr);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        return res;
    }
}
