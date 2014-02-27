package dk.kb.yggdrasil;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.utils.TravisUtils;

/**
 * Tests for {@link dk.kb.yggdrasil.Main }
 */
@RunWith(JUnit4.class)
public class MainTest {

    File goodConfigDir = new File("src/test/resources/config");

    @BeforeClass
    public static void beforeClass() throws YggdrasilException, IOException {
    	System.setProperty("dk.kb.yggdrasil.runningmode", "test");
    }
    
    @Test
    public void testMainMethodWithGoodConfigDir() throws Exception {
        if (TravisUtils.runningOnTravis()) {
            return;
        }
        System.setProperty(Main.CONFIGURATION_DIRECTORY_PROPERTY, goodConfigDir.getAbsolutePath());
        Config c = new Config(new File(goodConfigDir, Main.YGGDRASIL_CONF_FILENAME));
        FileUtils.deleteDirectory(c.getDatabaseDir());
        Main.main(new String[]{"test"});
    }
    
    @Test
    public void testMainMethodWithBadConfigDir() {
        String userHome = System.getProperty("user.home");
        File badConfigDir = new File(userHome + "/configconfig");
        Assert.assertFalse(badConfigDir.exists());
        System.setProperty(Main.CONFIGURATION_DIRECTORY_PROPERTY, badConfigDir.getAbsolutePath());
        try {
            Main.main(new String[]{});
            fail("Should throw Exception when given configuration directory was not found");
        } catch (Exception e) {
            // Expected
        }
    }
}
