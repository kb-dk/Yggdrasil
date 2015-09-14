package dk.kb.yggdrasil.xslt;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

public class TestModels {

    @Test
    public void test() throws YggdrasilException {
        Models m = new Models(new File("config/models.yml"));
        assertFalse(m.getMapper().keySet().isEmpty());
        assertEquals(3, m.getMapper().keySet().size());
    }

}
