package dk.kb.yggdrasil;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import java.util.LinkedHashMap;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.yaml.snakeyaml.scanner.ScannerException;

import dk.kb.yggdrasil.utils.YamlTools;

/** 
 * Tests for the methods in the YamlTools class. 
 *
 */
@RunWith(JUnit4.class)
public class YamlToolsTester {

    public static String YAML_TEST_FILE = "src/test/resources/config/rabbitmq.yml";
    public static String NOT_YAML_TEST_FILE = "src/test/resources/config/rabbitmq.yaml";
    public static String NOT_YAML_TEST_FILE2 = "src/test/resources/config/file_with_no_yaml_content.xml";
    
    @Test
    public void testReadYamlFailed() throws Exception {
        File f = new File(NOT_YAML_TEST_FILE);
        try {
            YamlTools.loadYamlSettings(f);
            fail("Should throw IOException on non existing file");
        } catch (IOException e) {
            // expected
        }
    }
    
    @Test
    public void testReadNonYamlFile() throws Exception {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "test");
        File f = new File(NOT_YAML_TEST_FILE2);
        try {
            YamlTools.loadYamlSettings(f);
            fail("Should throw YAML ScannerException on reading non YAML file");
        } catch (ScannerException e) {
            // expected
        }
    }
    
    @Test
    public void testReadYamlFile() throws Exception {
        System.setProperty(RunningMode.RUNNINGMODE_PROPERTY, "test");
        File f = new File(YAML_TEST_FILE);
        LinkedHashMap m = YamlTools.loadYamlSettings(f);
        Assert.assertNotNull(m);
        String mode = RunningMode.getMode().toString().toLowerCase();
        Assert.assertEquals(mode, "test");
        Assert.assertTrue(m.containsKey(mode));
        LinkedHashMap modeMap = (LinkedHashMap) m.get(mode);
        // Extract RabbitMqSettings from YAML file
        //Assert.assertTrue(modeMap.containsKey(
        //        RabbitMqSettings.RABBIT_MQ_YAML_PROPERTY));
        //LinkedHashMap m1 = (LinkedHashMap)modeMap.get(RabbitMqSettings.RABBIT_MQ_YAML_PROPERTY);
        RabbitMqSettings rmSettings = new RabbitMqSettings(modeMap);
        
        Assert.assertNotNull(rmSettings);
        Assert.assertNotNull(rmSettings.getBrokerUri());
        Assert.assertNotNull(rmSettings.getPreservationDestination());
        /*
        System.out.println("brokerURI:" + rmSettings.getBrokerUri());
        System.out.println("PreservationDestination:" + rmSettings.getPreservationDestination());
        */
    }
}



