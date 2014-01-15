package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.xslt.creator.XsltDocumentation;

@RunWith(JUnit4.class)
public class TestXsltCreator {
    @Test
    public void test_xslt_creation() throws IOException, YggdrasilException {
    	URL url = this.getClass().getClassLoader().getResource("doc/ADL_book_transformering.csv");
        File xsltFile = new File(url.getFile());
        
        XsltDocumentation xsltDoc = new XsltDocumentation(xsltFile);
        xsltDoc.printXslt(System.out);
        
//        File outputFile = File.createTempFile("Test", "mets.xml");
        File outputFile = new File("/home/jolf/test.mets.xml");
        PrintStream ps = new PrintStream(outputFile);
        try {
        	xsltDoc.printXslt(ps);
        } finally {
        	ps.flush();
        	ps.close();
        }
        
        Assert.assertTrue(xsltFile.exists() && xsltFile.isFile() && xsltFile.canRead());
        XmlValidator xmlValidator = new XmlValidator();
        XmlErrorHandler errorHandler = new XmlErrorHandler();
        XmlValidationResult result = xmlValidator.validate(xsltFile, null, errorHandler);
        //Assert.assertTrue("The resulting XSLT file must be valid, but apparently is not.", result.bValid);
    }
}
