package dk.kb.yggdrasil.db;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import dk.kb.yggdrasil.json.PreservationRequest;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;


/**
 * The StateDatabase persists incoming requests (PreservationRequest) with a Berkeley DB JE Database
 */ 
public class StateDatabase {

    /**
     * The logger used by this class.
     */
    private static Log log = LogFactory.getLog(StateDatabase.class);
    /** The singleton instance of this class. */
    private static StateDatabase instance;
    /** The basedir for the database itself. */
    private File databaseBaseDir;
    /** The subdirectory to the databaseBaseDir, where the database is located. */ 
    private static final String DATABASE_SUBDIR = "DB";
    /** The name of the database. */
    private static final String DATABASE_NAME = "YGGDRASIL";
    /** The Database environment. */
    private Environment env;
    /** The request Database. */
    private Database requestDB;
    /** The class Database. */
    private Database classDB;
    
    
    /** The Berkeley DB binder for the data object in our database, i.e. PreservationRequest. */
    private EntryBinding objectBinding; 
    
    /**
     * Method for obtaining the current singleton instance of this class.
     * If the instance of this class has not yet been constructed, then
     * it will be initialised.
     *  
     * @return The current instance of this class.
     */
    public static synchronized StateDatabase getInstance() {
        if(instance == null) {
            instance = new StateDatabase();
        }
        return instance;
    }   

    /**
     * Constructor.
     * Initializes the Berkeley DB databases.
     * @throws DatabaseException If unable to open the database
     */
    public StateDatabase() throws DatabaseException{
        initializeDatabase();
    }

    /**
     * Initialize the Berkeley DB databases.
     * @throws DatabaseException If unable to open the databases
     */
    private void initializeDatabase() throws DatabaseException {
        //TODO read databasedir from general yml
        databaseBaseDir = new File(".");  
        File homeDirectory = new File(databaseBaseDir, DATABASE_SUBDIR);
        if (!homeDirectory.isDirectory()) {
            homeDirectory.mkdirs();
        }
        log.info("Opening DB-environment in: " + homeDirectory.getAbsolutePath());

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);

        env = new Environment(homeDirectory, envConfig);
        requestDB = env.openDatabase(null, DATABASE_NAME, dbConfig);
        
        // Open the database that stores your class information.
        classDB = env.openDatabase(null, "classDb", dbConfig);
        StoredClassCatalog classCatalog = new StoredClassCatalog(classDB);
        
        // Create the binding
        objectBinding = new SerialBinding(classCatalog, PreservationRequest.class);
        
    }

    
    /**
     * Retrieve a PreservationRequest with the given uuid.
     * @param uuid A given UUID representing an element in Valhal
     * @return a PreservationRequest with the given uuid
     * @throws YggdrasilException
     */
    public PreservationRequest getRecord(String uuid) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        byte[] keyBytes = null;
        try {
            keyBytes = uuid.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new YggdrasilException("Unexpected '" 
                    + UnsupportedEncodingException.class.getName()
                    + "' exception: " + e);
        }
        DatabaseEntry key = new DatabaseEntry(keyBytes);
        DatabaseEntry data = new DatabaseEntry();

        OperationStatus status = null;
        try {
            status = requestDB.get(null, key, data, null);
        } catch (DatabaseException e) {
            throw new YggdrasilException("Unexpected '" 
                    + DatabaseException.class.getName() 
                    + "' exception: " + e);
        }
        PreservationRequest retrievedRequest = null;
        if (status == OperationStatus.SUCCESS) {
                retrievedRequest = (PreservationRequest) objectBinding.entryToObject(data);
        }
        return retrievedRequest;
    }

    /**
     * Determine, if the database contains a PreservationRequest for a specific uuid.
     * @param uuid A given UUID representing an element in Valhal
     * @return true, if database contains a PreservationRequest for the given uuid; otherwise false
     * @throws YggdrasilException
     */
    public boolean hasEntry(String uuid) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        return (getRecord(uuid) != null);
    }
    
    /**
     * 
     * @param uuid A given UUID representing an element in Valhal
     * @param request 
     */
    public void put(String uuid, PreservationRequest request) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        ArgumentCheck.checkNotNull(request, "PreservationRequest request");

        DatabaseEntry theKey = null;
        DatabaseEntry theData = null;
        try {
            theKey = new DatabaseEntry(uuid.getBytes("UTF-8"));
            // Create the DatabaseEntry for the data. Use the EntryBinding object
            // that was just created to populate the DatabaseEntry
            theData = new DatabaseEntry();
            objectBinding.objectToEntry(request, theData);
        } catch (UnsupportedEncodingException e) {
            throw new YggdrasilException(e.toString());
        }

        try {
            requestDB.put(null, theKey, theData);
        } catch (DatabaseException e) {
            throw new YggdrasilException("Database exception occuring during ingest", e);
        }
    }  

    /** 
     * Retrieve list of outstanding requests. 
     * TODO maybe change to retrieve the requests themselves as a list?
     * @throws YggdrasilException 
     */
    public List<String> getOutstandingUUIDS() throws YggdrasilException {
        Cursor cursor = null;
        List<String> resultList = new ArrayList<String>();
        try { 
            cursor = requestDB.openCursor(null, null);

            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();

            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
                    OperationStatus.SUCCESS) {
                String keyString = new String(foundKey.getData());
                resultList.add(keyString);
            }
        } catch (DatabaseException de) {
            throw new YggdrasilException("Error accessing database." + de);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (DatabaseException e) {
                    log.warn("Database error occurred when closing the cursor: " + e);
                }
            }
        }
        return resultList;
    }
    
    /**
     * Close the databases and set the instance to null. 
     */
    public void cleanup() {
        if (requestDB != null) {
            try {
                requestDB.close();
            } catch (DatabaseException e) {
                log.warn("Unable to close request database. The error was :" + e);
            }
        }
        if (classDB != null) {
            try {
                classDB.close();
            } catch (DatabaseException e) {
                log.warn("Unable to close class database. The error was :" + e);
            }
        }
        instance = null;
        
        
    }
    
    /**
     * Delete the entry in the request database with the given uuid. 
     * @param uuid A given UUID representing an element in Valhal
     * @throws YggdrasilException
     */
    public void delete(String uuid) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
       
        DatabaseEntry theKey = null;
        try {
            theKey = new DatabaseEntry(uuid.getBytes("UTF-8")); 
        } catch (UnsupportedEncodingException e) {
            throw new YggdrasilException(e.toString());
        }

        try {
            requestDB.delete(null, theKey);
        } catch (DatabaseException e) {
            throw new YggdrasilException("Database exception occuring during deletion of record", e);
        }
    }
}
