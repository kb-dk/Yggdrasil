package dk.kb.yggdrasil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/** The class reading the yggdrasil.yml file. */
public class Config {

    private String DATABASE_DIR_PROPERTY = "database_dir";
    private File databaseDir;
    
    /**
     * Constructor for class reading the general Yggdrasil config file.
     * @param yggrasilConfigFile the config file.
     * @throws YggdrasilException
     */
    public Config(File yggrasilConfigFile) throws YggdrasilException {
       ArgumentCheck.checkExistsNormalFile(yggrasilConfigFile, "File yggrasilConfigFile");
       Map<String, LinkedHashMap> settings = YamlTools.loadYamlSettings(yggrasilConfigFile);
       Map<String, Object> valuesMap = settings.get(RunningMode.getMode().toString());
       databaseDir = new File((String) valuesMap.get(DATABASE_DIR_PROPERTY));
    }
    
    /** 
     * @return the database dir
     */
    public File getDatabaseDir() {
        return databaseDir;
    }
    
}
