package dk.kb.yggdrasil.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

/**
 * The StateDatabase persists incoming requests (PreservationRequestState) with a Berkeley DB JE Database
 */ 
public class StateDatabase {

    /** The logger used by this class. */
    private static Log log = LogFactory.getLog(StateDatabase.class);
    /** The basedir for the database itself. */
    private File databaseBaseDir;
    /** The subdirectory to the databaseBaseDir, where the database is located. */ 
    private static final String DATABASE_SUBDIR = "DB";
    /** The name of the database. */
    private static final String DATABASE_NAME = "YGGDRASIL";
    
    /** The name of the class database. */
    private static final String CLASS_DATABASE_NAME = "classDb";
    
    
    /** The Database environment. */
    private Environment env;
    /** The request Database. */
    private Database requestDB;
    /** The class Database. */
    private Database classDB;
    
    /** The Berkeley DB binder for the data object and keyObject in our database, 
     * i.e. PreservationRequestState. */
    private EntryBinding objectBinding;
    private EntryBinding keyBinding;
    
    /**
     * Constructor.
     * Initializes the Berkeley DB databases.
     * @param databasedir The directory where the database should be
     * @throws DatabaseException If unable to open the database
     */
    public StateDatabase(File databasedir) throws DatabaseException{
        ArgumentCheck.checkNotNull(databasedir, "File databasedir");
        this.databaseBaseDir = databasedir;
        initializeDatabase();
    }

    /**
     * Initialize the Berkeley DB databases.
     * @throws DatabaseException If unable to open the databases
     */
    private void initializeDatabase() throws DatabaseException {
        File homeDirectory = new File(databaseBaseDir, DATABASE_SUBDIR);
        if (!homeDirectory.isDirectory()) {
            log.warn("The database directory '" + homeDirectory.getAbsolutePath() 
                    + "' does not exist. Trying to create it");
            homeDirectory.mkdirs();
        }
        ArgumentCheck.checkExistsDirectory(homeDirectory, "File homeDirectory");
        
        log.info("Opening DB-environment in: " + homeDirectory.getAbsolutePath());

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);

        Transaction nullTransaction = null;
        
        env = new Environment(homeDirectory, envConfig);
        requestDB = env.openDatabase(nullTransaction, DATABASE_NAME, dbConfig);
        
        // Open the database that stores your class information.
        classDB = env.openDatabase(nullTransaction, CLASS_DATABASE_NAME, dbConfig);
        StoredClassCatalog classCatalog = new StoredClassCatalog(classDB);
        
        // Create the binding
        objectBinding = new SerialBinding(classCatalog, PreservationRequestState.class);
        keyBinding = new SerialBinding(classCatalog, String.class);
    }
    
    /**
     * Retrieve a PreservationRequestState with the given uuid.
     * @param uuid A given UUID representing an element in Valhal
     * @return a PreservationRequestState with the given uuid
     * @throws YggdrasilException
     */
    public PreservationRequestState getRecord(String uuid) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        Transaction nullTransaction = null;
        LockMode nullLockMode = null;
        DatabaseEntry key = new DatabaseEntry();
        keyBinding.objectToEntry(uuid, key);
        DatabaseEntry data = new DatabaseEntry();
        OperationStatus status = null;
        try {
            status = requestDB.get(nullTransaction, key, data, nullLockMode);
        } catch (DatabaseException e) {
            throw new YggdrasilException(
                    "Could not retrieve the PreservationRequestState for the record '" + uuid, e);
        }
        PreservationRequestState retrievedRequest = null;
        if (status == OperationStatus.SUCCESS) {
                retrievedRequest = (PreservationRequestState) objectBinding.entryToObject(data);
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
     * Create a new PreservationRequest in the database.
     * @param uuid A given UUID representing an element in Valhal
     * @param request 
     */
    public void put(String uuid, PreservationRequestState request) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        ArgumentCheck.checkNotNull(request, "PreservationRequestState request");
        Transaction txn = env.beginTransaction(null, null);
        DatabaseEntry theKey = new DatabaseEntry();
        DatabaseEntry theData = new DatabaseEntry(); 
        keyBinding.objectToEntry(uuid, theKey);
        objectBinding.objectToEntry(request, theData);

        try {
            requestDB.put(txn, theKey, theData);
            txn.commit();
        } catch (DatabaseException e) {
            if (txn != null) {
                txn.abort();
                txn = null;
            }
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
        CursorConfig nullCursorConfig = null;
        Transaction nullTransaction = null;
        List<String> resultList = new ArrayList<String>();
        try { 
            cursor = requestDB.openCursor(nullTransaction, nullCursorConfig);

            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();

            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
                    OperationStatus.SUCCESS) {
                String keyString = (String) keyBinding.entryToObject(foundKey);
                resultList.add(keyString);
            }
        } catch (DatabaseException de) {
            throw new YggdrasilException("Error when iterating the PreservationRequestStates ", de);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (DatabaseException e) {
                    log.warn("Database error occurred when closing the cursor: ", e);
                }
            }
        }
        return resultList;
    }
    
    /**
     * Delete the entry in the request database with the given uuid. 
     * @param uuid A given UUID representing an element in Valhal
     * @throws YggdrasilException
     */
    public void delete(String uuid) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(uuid, "String uuid");
        Transaction txn = env.beginTransaction(null, null);
        DatabaseEntry key = new DatabaseEntry();
        keyBinding.objectToEntry(uuid, key);
 
        try {
            requestDB.delete(txn, key);
            txn.commit();
        } catch (DatabaseException e) {
            if (txn != null) {
                txn.abort();
                txn = null;
            }
            throw new YggdrasilException(
                    "Database exception occuring during deletion of record", e);
        }
    }
    
    /**
     * Close the databases and set the instance to null. 
     */
    public void cleanup() {
        if (requestDB != null) {
            try {
                requestDB.close();
            } catch (DatabaseException e) {
                log.warn("Unable to close request database. The error was :", e);
            }
        }
        if (classDB != null) {
            try {
                classDB.close();
            } catch (DatabaseException e) {
                log.warn("Unable to close class database. The error was :", e);
            }
        }
    }
}
