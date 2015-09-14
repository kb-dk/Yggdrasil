package dk.kb.yggdrasil.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

/** Class for reading YAML v 1.1 configuration files. */
public class YamlTools {
    /**
     * Load YAML settings in the given file.
     * @param ymlFile The settings file in YAML format to load
     * @throws YggdrasilException If settings file was not found
     * or experienced wrong datastructure.
     * @return the loaded settings as a {@link LinkedHashMap}
     */
    public static LinkedHashMap<String, LinkedHashMap> loadYamlSettings(File ymlFile)
            throws YggdrasilException {
        InputStream input = null;
        Object loadedSettings = null;
        try {
            input = new FileInputStream(ymlFile);
            loadedSettings = new Yaml().load(input);
            if (!(loadedSettings instanceof LinkedHashMap)) {
                throw new YggdrasilException("Internal error. Unable to read settings. Excepted load method to " 
                        + "return a LinkedHashMap, but it returned a " + loadedSettings.getClass().getName() 
                        + " instead");
            }
        } catch (IOException e) {
            throw new YggdrasilException("Internal error. Unable to read settings from file '" 
                    + ymlFile.getAbsolutePath() + "'. Reason:  ", e);
        } finally {
            IOUtils.closeQuietly(input);
        }
        return (LinkedHashMap<String, LinkedHashMap>) loadedSettings;
    }
}
