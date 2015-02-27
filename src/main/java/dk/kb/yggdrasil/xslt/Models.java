package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.YamlTools;

/**
 * Container for the mapping between Model and XSLT script.
 * The mappings are read from the models.yml file.
 */
public class Models {
    /** Map between models and XSLT scripts.*/
    private final Map<String, String> modelmapper;

    /**
     * Constructor.
     * @param mappingFile the YAML file with the mappings
     * @throws YggdrasilException If the model mapping file cannot be loaded.
     */
    public Models(File mappingFile) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(mappingFile, "File mappingFile");
        modelmapper = new HashMap<String, String>();
        Map<String, LinkedHashMap> settings = YamlTools.loadYamlSettings(mappingFile);
        Map<String, String> valuesMap = settings.get("models");
        modelmapper.putAll(valuesMap);
    }

    /**
     * @return the constructed mapping between model and xslt script
     */
    public Map<String,String> getMapper() {
        return modelmapper;
    }

}
