package dk.kb.yggdrasil.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.Yaml;

/** Class for reading YAML v 1.1 configuration files. */ 
public class YamlTools {
    /**
     * Load YAML settings in the given file.
     * @param ymlFile The settings file in YAML format to load
     * @throws FileNotFoundException If settings file was not found
     * @return the loaded settings as a {@link LinkedHashMap}
     */
    @SuppressWarnings("rawtypes")
    public static LinkedHashMap loadYamlSettings(File ymlFile) throws FileNotFoundException {
        
        InputStream is = new FileInputStream(ymlFile);
        Object loadedSettings = new Yaml().load(is);
        if (!(loadedSettings instanceof LinkedHashMap)) {
            System.err.println("Unable to read settings. Excepted load method to return a LinkedHashMap, but it returned a " 
                    + loadedSettings.getClass().getName() + " instead");
            System.exit(1);
        }
        return (LinkedHashMap) loadedSettings;
    } 
}
