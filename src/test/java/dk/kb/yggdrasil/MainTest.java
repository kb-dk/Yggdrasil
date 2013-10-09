package dk.kb.yggdrasil;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link dk.kb.yggdrasil.Main }
 */
@RunWith(JUnit4.class)
public class MainTest {

    @Test
    public void thisAlwaysPasses() throws Exception {
        Main.main(new String[]{});
    }

    @Test
    @Ignore
    public void thisIsIgnored() {
    }
}
