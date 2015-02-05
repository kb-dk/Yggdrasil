package dk.kb.yggdrasil.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.utils.HostName;

/**
 * Tests for the methods in the HostName class.
 *
 */
@RunWith(JUnit4.class)
public class HostNameTest {

    @Test
    public void testHostNamePresent() throws Exception {
        HostName hostname = new HostName();
        String hn =hostname.getHostName();
        Assert.assertNotNull(hn);
    }
}
