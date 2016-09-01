package dk.kb.yggdrasil.bitmag;

import java.io.File;
import java.util.Map;

import dk.kb.yggdrasil.config.RunningMode;
import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/**
 * Configuratio for the Bitrepository.
 */
public class BitrepositoryConfig {

    /** The archive settings directory needed to upload to
     * a bitmag style repository. May not be null. */
    private final File settingsDir;
    /** The authentication key used by the putfileClient. */
    private File privateKeyFile;
    /** The maximum number of failing pillars. Default is 0, can be overridden by settings in the bitmag.yml. */
    private int maxNumberOfFailingPillars = 0; 
    /** The bitrepository component id. */
    private final String componentId;
    
    /** Name of YAML property used to find settings dir. */
    public static final String YAML_BITMAG_SETTINGS_DIR_PROPERTY = "settings_dir";
    /** Name of YAML property used to find keyfile. */
    public static final String YAML_BITMAG_KEYFILE_PROPERTY = "keyfile";

    /** Name of the YAML sub-map client*/
    public static final String YAML_BITMAG_CLIENTS = "client";
    /** Name of the YAML property under the client sub-map for maximum number of pillars accept to fail.*/
    public static final String YAML_BITMAG_CLIENT_PUTFILE_MAX_PILLAR_FAILURES = "putfile_max_pillars_failures";

    /**
     * Constructor, when using the config file.
     * @param configFile The YAML configuration file.
     * @throws YggdrasilException If unable to find the relevant information in the given configFile
     *  or the configFile is null or does not exist.
     */
    public BitrepositoryConfig(File configFile) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(configFile, "File configFile");
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
        
        componentId = BitrepositoryUtils.generateComponentID();
    }

    /**
     * @return The settings directory for the bitrepository settings files.
     */
    public File getSettingsDir() {
        return settingsDir;
    }
    
    /** 
     * @return The authentication key used by the putfileClient. 
     */
    public File getPrivateKeyFile() {
        return privateKeyFile;
    }
    
    /** 
     * @return The maximum number of failing pillars. 
     */
    public int getMaxNumberOfFailingPillars() {
        return maxNumberOfFailingPillars;
    }
    
    /** 
     * @return The bitrepository component id. 
     */
    public String getComponentId() {
        return componentId;
    }
}
