package dk.kb.yggdrasil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/** The class reading the yggdrasil.yml file. */
public class Config {

    /** The configDir where the yggdrasilConfigFile was located. */
    private File configdir;

    /** The database directory property. */
    private static final String DATABASE_DIR_PROPERTY = "database_dir";
    /** The database directory.  (created if it doesn't exist. ) */
    private final File databaseDir;
    /** The temporary directory property.  */
    private static final String TEMPORARY_DIR_PROPERTY = "temporary_dir";
    /** The temporary directory. (created if it doesn't exist. )*/
    private final File tmpDir;
//    /** The prefix for object ids in the management repository (Valhal) **/
//    private final String MANAGEMENT_REPO_OBJECT_ID_PREFIX_PROPERTY = "management_repo_object_id_prefix";

    /** The monitor endpoint port property. */
    private static final String MONITOR_PORT_PROPERTY = "monitor_port";
    /** The monitor port for the RunState service endpoint */
    private final int monitorPort;
    
    /** The property for the warc size limit. */
    private static final String WARC_SIZE_LIMIT_PROPERTY = "warc_size_limit";
    /** The default value for the warc size limit: 1 GB. */
    private static final Long DEFAULT_WARC_SIZE_LIMIT = 1000000000L;
    /** The warc size limit. */
    private final long warcSizeLimit;
    
    /** The property for the upload wait limit. */
    private static final String UPLOAD_WAIT_LIMIT_PROPERTY = "upload_wait_limit";
    /** The default value for the upload wait limit: 10 min.*/
    private static final Long DEFAULT_UPLOAD_WAIT_LIMIT = 600000L;
    /** The upload wait limit.*/
    private final long uploadWaitLimit;
    
    /** The property for the interval for checking the conditions for the WARC files. */
    private static final String CHECK_WARC_CONDITION_INTERVAL_PROPERTY = "check_conditions";
    /** The default value for the interval.*/
    private static final Long DEFAULT_CHECK_WARC_CONDITION_INTERVAL = 60000L;
    /** The interval for checking WARC conditions. */
    private final Long checkWarcConditionInterval;
    
    /**
     * Constructor for class reading the general Yggdrasil config file.
     * @param yggrasilConfigFile the config file.
     * @throws YggdrasilException
     */
    public Config(File yggrasilConfigFile) throws YggdrasilException {
       ArgumentCheck.checkExistsNormalFile(yggrasilConfigFile, "File yggrasilConfigFile");
       configdir = yggrasilConfigFile.getParentFile();
       Map<String, LinkedHashMap> settings = YamlTools.loadYamlSettings(yggrasilConfigFile);
       Map<String, Object> valuesMap = settings.get(RunningMode.getMode().toString());

       databaseDir = extractConfigValueAsDirectory(valuesMap, DATABASE_DIR_PROPERTY, null);
       tmpDir = extractConfigValueAsDirectory(valuesMap, TEMPORARY_DIR_PROPERTY, "temporarydir");
       
       monitorPort = (Integer) extractConfigValue(valuesMap, MONITOR_PORT_PROPERTY, null);
       warcSizeLimit = extractConfigLongValue(valuesMap, WARC_SIZE_LIMIT_PROPERTY, DEFAULT_WARC_SIZE_LIMIT);
       uploadWaitLimit = extractConfigLongValue(valuesMap, UPLOAD_WAIT_LIMIT_PROPERTY, DEFAULT_UPLOAD_WAIT_LIMIT);
       checkWarcConditionInterval = extractConfigLongValue(valuesMap, CHECK_WARC_CONDITION_INTERVAL_PROPERTY, 
               DEFAULT_CHECK_WARC_CONDITION_INTERVAL);
    }
    
    /**
     * Extracts the configuration value for the given property.
     * If the property has not been defined in the configuration, then the default value is used.
     * @param configs The configuration map to extract the given configuration from.
     * @param property The name of the configuration property.
     * @param defaultValue The default value to use, if no value was defined in the configuration.
     * @return The configuration value.
     * @throws YggdrasilException If the configuration was not set, and it has no default value.
     */
    private String extractConfigStringValue(Map<String, Object> configs, String property, String defaultValue) 
            throws YggdrasilException {
        // TODO handle sub-maps.
        String res = (String) configs.get(property);
        if(res == null || res.isEmpty()) {
            if(defaultValue == null || defaultValue.isEmpty()) {
                throw new YggdrasilException("Undefined configuraion '" + property + "', and no default value.");
            }
            return defaultValue;
        }
        return res;
    }
    
    /**
     * Extracts a configuration property as a Long, even though YAML claims it to be a Integer.
     * @param configs The configuration map to extract the given configuration from.
     * @param property The name of the configuration property.
     * @param defaultValue The default value to use, if no value was defined in the configuration.
     * @return The configuration value.
     * @throws YggdrasilException If the configuration was not set, and it has no default value.
     */
    private Long extractConfigLongValue(Map<String, Object> configs, String property, Object defaultValue) 
            throws YggdrasilException {
        Object res = extractConfigValue(configs, property, defaultValue);
        if(res.getClass() == Integer.class) {
            int i = (Integer) res;
            return new Long(i);
        }
        return (Long) res;
    }
    
    /**
     * Extracts the configuration value for the given property.
     * If the property has not been defined in the configuration, then the default value is used.
     * @param configs The configuration map to extract the given configuration from.
     * @param property The name of the configuration property.
     * @param defaultValue The default value to use, if no value was defined in the configuration.
     * @return The configuration value.
     * @throws YggdrasilException If the configuration was not set, and it has no default value.
     */
    private Object extractConfigValue(Map<String, Object> configs, String property, Object defaultValue) 
            throws YggdrasilException {
        // TODO handle sub-maps.
        Object res = configs.get(property);
        if(res == null) {
            if(defaultValue == null) {
                throw new YggdrasilException("Undefined configuraion '" + property + "', and no default value.");
            }
            return defaultValue;
        }
        return res;
    }
    
    /**
     * Instantiates a configuration property as a directory.
     * @param configs The configuration map to extract the given configuration from.
     * @param property The name of the configuration property.
     * @param defaultValue The default value to use, if no value was defined in the configuration.
     * @return The configuration value.
     * @throws YggdrasilException If the configuration was not set, and it has no default value.
     * Or if the directory path could not be instantiated as a directory.
     */
    private File extractConfigValueAsDirectory(Map<String, Object> configs, String property, String defaultValue) 
            throws YggdrasilException {
        File res = new File(extractConfigStringValue(configs, property, defaultValue));
        if (!res.exists()) {
            if (!res.mkdirs()) {
                throw new YggdrasilException("Unable to create directory '" + res.getAbsolutePath() 
                        + "' from configuration '" + property + "'.");
            }
        }
        if(!res.isDirectory()) {
            throw new YggdrasilException("The configuration '" + property 
                    + "' cannot be instantiated as a directory.");
        }
        
        return res;
    }
    
    /** 
     * @return the database dir
     */
    public File getDatabaseDir() {
        return databaseDir;
    }
    
    /** 
     * @return the temporary directory
     */
    public File getTemporaryDir() {
        return tmpDir;
    }  
    
    /** 
     * @return the config directory
     */
    public File getConfigDir() {
        return configdir;
    }

    /** 
     * @return the monitor port
     */
    public int getMonitorPort() {
        return monitorPort;
    }

    /**
     * @return The warc size limit.
     */
    public long getWarcSizeLimit() {
        return warcSizeLimit;
    }
    
    /**
     * @return The upload wait limit.
     */
    public long getUploadWaitLimit() {
        return uploadWaitLimit;
    }
    
    /**
     * @return The interval for checking the warc conditions.
     */
    public long getCheckWarcConditionInterval() {
        return checkWarcConditionInterval;
    }
}
