package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.xslt.creator.XsltDocumentation;

@RunWith(JUnit4.class)
public class TestXsltCreator {
    @Test
    public void test_xslt_creation() {
    	URL url = this.getClass().getClassLoader().getResource("doc/ADL_book_transformering.csv");
        File file = new File(url.getFile());
        
        XsltDocumentation xsltDoc = new XsltDocumentation(file);
        xsltDoc.printXslt(System.out);
    }
}
