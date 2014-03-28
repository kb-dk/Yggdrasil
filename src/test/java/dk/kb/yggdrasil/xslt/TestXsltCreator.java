package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

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
    public void test_xslt_creation() throws IOException, YggdrasilException, TransformerException {
    	URL url = this.getClass().getClassLoader().getResource("doc/ADL_book_transformering.csv");
        File docFile = new File(url.getFile());
        
        XsltDocumentation xsltDoc = new XsltDocumentation(docFile);
        //xsltDoc.printXslt(System.out);
        
        
        File xsltFile = new File("/home/jolf/test/test.mets.xslt");
        if(xsltFile.exists()) {
        	Assert.assertTrue(xsltFile.delete());
        }
        PrintStream ps = new PrintStream(xsltFile);
        try {
        	xsltDoc.printXslt(ps);
        } finally {
        	ps.flush();
        	ps.close();
        }

        File outputFile = new File("/home/jolf/test/test.mets.xml");
        if(outputFile.exists()) {
        	Assert.assertTrue(outputFile.delete());
        }
        File inputFile = new File(this.getClass().getClassLoader().getResource("valhal/xml/book.xml").getFile());

        Assert.assertTrue(inputFile.exists() && inputFile.isFile() && inputFile.canRead());
        Assert.assertTrue(xsltFile.exists() && xsltFile.isFile() && xsltFile.canRead());
        Assert.assertFalse(outputFile.exists() || outputFile.isFile() || outputFile.canRead());

//        XslTransform.main(inputFile.getAbsolutePath(), xsltFile.getAbsolutePath(), outputFile.getAbsolutePath());
        XmlValidationResult res = transformTestFiles(inputFile, xsltFile, outputFile);
        
        Assert.assertTrue("The output should be wellformed.", res.bWellformed);
        Assert.assertTrue("Should have used XSD for validation.", res.bXsdUsed);
        Assert.assertFalse("Should not use DTDs for validation.", res.bDtdUsed);
        Assert.assertTrue("The resulting XML should be valid.", res.bValid);
    }
    
    private XmlValidationResult transformTestFiles(File inputFile, File xsltFile, File outputFile) throws IOException, TransformerException, YggdrasilException {
        XslTransformer transformer = XslTransformer.getTransformer(xsltFile);
        XslUriResolver uriResolver = new XslUriResolver();
        XslErrorListener errorListener = new XslErrorListener();

        Source source = new StreamSource(inputFile);
        byte[] bytes = transformer.transform(source, uriResolver, errorListener);
        
        XmlEntityResolver entityResolver = null;
        XmlErrorHandler errorHandler = new XmlErrorHandler();

        XmlValidator xmlValidator = new XmlValidator();

        RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
        raf.seek(0);
        raf.setLength(0);
        raf.write(bytes);
        raf.close();
        return xmlValidator.validate(outputFile, entityResolver, errorHandler);    	
    }
}
