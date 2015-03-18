package dk.kb.yggdrasil.xslt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class TestXslt {

    public static String[][] newXsltFiles = new String[][] {
        {"valhal/xml/basic_file.xml", "xslt/file.xsl"},
//        {"valhal/xml/ordered_instance.xml", "xslt/instance.xsl"},
//        {"valhal/xml/single_file_instance.xml", "xslt/instance.xsl"},
//        {"valhal/xml/work.xml", "xslt/work.xslt"},
//        {"valhal/xml/instance_with_unordered_multiple_files.xml", "xslt/instance.xsl"},
        {"valhal/xml/namespaceless_mods.xml", "xslt/instance.xsl"}
    };

    @Test
    public void test_new_xslt() {
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
        for (int i=0; i<newXsltFiles.length; ++i) {
            try {
                String xmlFilename = newXsltFiles[i][0];
                String xslFilename = newXsltFiles[i][1];

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
                
                System.out.println(new String(bytes));

                bool = xmlValidator.testDefinedValidity(new ByteArrayInputStream(bytes), entityResolver, errorHandler, result);
                Assert.assertTrue("Should create valid xml.", bool);
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
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
                Assert.fail("Unexpected exception!");
            } catch (TransformerException e) {
                e.printStackTrace();
                Assert.fail("Unexpected exception!");
            } catch (YggdrasilException e) {
                e.printStackTrace();
                Assert.fail("Unexpected exception!");
            }
        }
    }

}
