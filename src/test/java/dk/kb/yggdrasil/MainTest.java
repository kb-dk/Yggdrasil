package dk.kb.yggdrasil;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;

/**
 * Tests for {@link dk.kb.yggdrasil.Main }
 */
@RunWith(JUnit4.class)
public class MainTest {
    
    File goodConfigDir = new File("src/test/resources/config");
    
    @Test
    public void testMainMethodWithGoodConfigDir() throws Exception {
        System.setProperty(Main.CONFIGURATION_DIRECTORY_PROPERTY, goodConfigDir.getAbsolutePath());    
        Main.main(new String[]{});
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
