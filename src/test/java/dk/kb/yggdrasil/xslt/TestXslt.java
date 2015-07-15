package dk.kb.yggdrasil.xslt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class TestXslt {
    
    @Test
    public void testContentFile() throws Exception {
        testXslt("valhal/xml/content_file.xml", "xslt/file.xsl");
    }

    @Test
    public void testContentFileUpdate() throws Exception {
        testXslt("valhal/xml/content_file_update.xml", "xslt/file.xsl");
    }

    @Test
    public void testInstance() throws Exception {
        testXslt("valhal/xml/instance.xml", "xslt/instance.xsl");
    }

    @Test
    public void testInstanceWithUnorderedMultipleFiles() throws Exception {
        testXslt("valhal/xml/instance_with_unordered_multiple_files.xml", "xslt/instance.xsl");
    }

    @Test
    public void testInstanceUpdate() throws Exception {
        testXslt("valhal/xml/instance_update.xml", "xslt/instance.xsl");
    }

    @Test
    public void testInstanceNamespacelessMods() throws Exception {
        testXslt("valhal/xml/namespaceless_mods.xml", "xslt/instance.xsl");
    }

    public void testXslt(String xmlFilename, String xslFilename) throws Exception {
        URL url;
        File file;
        StreamSource source;
        XslTransformer transformer;
        boolean bool;

        url = this.getClass().getClassLoader().getResource("");
        File cacheDir = new File(new File(url.getFile()), "entity_cache");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Assert.fail("Could not make entity_cache directory!");
        }

        XslUriResolver uriResolver = new XslUriResolver();
        XslErrorListener errorListener = new XslErrorListener();

        XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
        XmlErrorHandler errorHandler = new XmlErrorHandler();

        XmlValidator xmlValidator = new XmlValidator();
        XmlValidationResult result = new XmlValidationResult();
        /*
         * Try to initialize and validate some XSL files.
         */
        try {
            url = this.getClass().getClassLoader().getResource(xslFilename);
            file = new File(url.getFile());
            source = new StreamSource(file);
            //source.setSystemId(file.getPath());
            transformer = XslTransformer.getTransformer(source);
            Assert.assertNotNull(transformer);

            url = this.getClass().getClassLoader().getResource(xmlFilename);
            file = new File(url.getFile());
            source = new StreamSource(file);
            byte[] bytes = transformer.transform(source, uriResolver, errorListener);
            /*
             * With ErrorHandler.
             */
            bool = xmlValidator.testStructuralValidity(new ByteArrayInputStream(bytes), entityResolver, errorHandler, result);
            Assert.assertTrue(xmlFilename, bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertEquals(0, errorHandler.numberOfErrors);
            Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
            Assert.assertEquals(0, errorHandler.numberOfWarnings);
            Assert.assertTrue(result.bWellformed);
            Assert.assertFalse(result.bValid);

//            System.out.println(new String(bytes, "UTF8"));

            bool = xmlValidator.testDefinedValidity(new ByteArrayInputStream(bytes), entityResolver, errorHandler, result);
            Assert.assertTrue("Should create valid xml from file '" + xmlFilename + "' with xslt '" + xslFilename + "'", bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertEquals(0, errorHandler.numberOfErrors);
            Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
            Assert.assertEquals(0, errorHandler.numberOfWarnings);
            Assert.assertTrue(result.bWellformed);
            Assert.assertTrue(result.bValid);
            /*
             * Without ErrorHandler.
             */
            bool = xmlValidator.testStructuralValidity(new ByteArrayInputStream(bytes), entityResolver, null, result);
            Assert.assertTrue(bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertTrue(result.bWellformed);
            Assert.assertFalse(result.bValid);

            bool = xmlValidator.testDefinedValidity(new ByteArrayInputStream(bytes), entityResolver, null, result);
            Assert.assertTrue(bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertTrue(result.bWellformed);
            Assert.assertTrue(result.bValid);
            /*
             * Validity only with ErrorHandler.
             */
            bool = xmlValidator.testDefinedValidity(new ByteArrayInputStream(bytes), entityResolver, errorHandler, result);
            Assert.assertTrue(bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertEquals(0, errorHandler.numberOfErrors);
            Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
            Assert.assertEquals(0, errorHandler.numberOfWarnings);
            Assert.assertTrue(result.bWellformed);
            Assert.assertTrue(result.bValid);
            /*
             * Validity only without ErrorHandler.
             */
            bool = xmlValidator.testDefinedValidity(new ByteArrayInputStream(bytes), entityResolver, null, result);
            Assert.assertTrue(bool);
            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            Assert.assertTrue(result.bWellformed);
            Assert.assertTrue(result.bValid);
        } catch (YggdrasilException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception: " + e.getMessage());            
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception: " + e.getMessage());            
        }
    }
}
