package dk.kb.yggdrasil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

public class Config {

    private String DATABASE_DIR_PROPERTY = "database_dir";
    private File databaseDir;
    
    public Config(File yggrasilConfigFile) throws YggdrasilException {
       ArgumentCheck.checkExistsNormalFile(yggrasilConfigFile, "File yggrasilConfigFile");
       Map<String, LinkedHashMap> settings = YamlTools.loadYamlSettings(yggrasilConfigFile);
       Map<String, Object> valuesMap = settings.get(RunningMode.getMode().toString());
       databaseDir = new File((String) valuesMap.get(DATABASE_DIR_PROPERTY));
    }

    public File getDatabaseDir() {
        return databaseDir;
    }
    
}
