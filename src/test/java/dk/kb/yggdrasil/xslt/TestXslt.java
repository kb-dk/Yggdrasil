package dk.kb.yggdrasil.xslt;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.UUID;

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

    private static String[] oldXsltFiles = {
        "xslt/transformRepresentationMets.xsl",
        "xslt/retrieveMetsRelationship.xsl",
        "xslt/transformToMets.xsl",
        "xslt/transformToMix.xsl",
        "xslt/transformToMods.xsl",
        "xslt/transformToPremis.xsl",
    };

    @Test
    public void test_old_xslt() {
        URL url;
        File file;
        StreamSource source;
        XslTransformer transformer;
        /*
         * Try to initialize and validate some XSL files.
         */
        for (int i=0; i<oldXsltFiles.length; ++i) {
            try {
                url = this.getClass().getClassLoader().getResource(oldXsltFiles[i]);
                file = new File(url.getFile());
                source = new StreamSource(file);
                //source.setSystemId(file.getPath());
                transformer = XslTransformer.getTransformer(source);
                Assert.assertNotNull(transformer);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
                Assert.fail("Unexpected exception!");
            }
        }
        /*
         * Try an XSL transformation.
         */
        try {
            url = this.getClass().getClassLoader().getResource("xslt/transformToMets.xsl");
            file = new File(url.getFile());
            transformer = XslTransformer.getTransformer(file);
            Assert.assertNotNull(transformer);

            XslUriResolver uriResolver = new XslUriResolver();
            XslErrorListener errorListener = new XslErrorListener();

            url = this.getClass().getClassLoader().getResource("xml/Car_S-9092.tif.raw.xml");
            file = new File(url.getFile());
            source = new StreamSource(file);
            byte[] bytes = transformer.transform(source, uriResolver, errorListener);

            Assert.assertNotNull(bytes);
            Assert.assertThat(bytes.length, is(greaterThan(0)));

            Assert.assertEquals(0, errorListener.numberOfErrors);
            Assert.assertEquals(0, errorListener.numberOfFatalErrors);
            Assert.assertEquals(0, errorListener.numberOfWarnings);

            url = this.getClass().getClassLoader().getResource("");
            File cacheDir = new File(new File(url.getFile()), "entity_cache");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Assert.fail("Could not make entity_cache directory!");
            }

            XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
            XmlErrorHandler errorHandler = new XmlErrorHandler();

            XmlValidator xmlValidator = new XmlValidator();
            XmlValidationResult result;

            file = File.createTempFile(UUID.randomUUID().toString(), ".xml");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.write(bytes);
            raf.close();
            result = xmlValidator.validate(file, entityResolver, errorHandler);
            if (!file.delete()) {
                file.deleteOnExit();
            }

            Assert.assertNull(result.systemId);
            Assert.assertFalse(result.bDtdUsed);
            Assert.assertTrue(result.bXsdUsed);
            // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" os missing
            Assert.assertEquals(1, result.xsiNamespaces.size());
            Assert.assertEquals(9, result.schemas.size());

            Assert.assertEquals(0, errorHandler.numberOfErrors);
            Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
            Assert.assertEquals(0, errorHandler.numberOfWarnings);

            Assert.assertTrue(result.bWellformed);
            Assert.assertTrue(result.bValid);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (TransformerException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (YggdrasilException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    public static String[][] newXsltFiles = new String[][] {
        {"valhal/xml/basic_file.xml", "xslt/file.xslt"},
        {"valhal/xml/book.xml", "xslt/book.xslt"},
        {"valhal/xml/ordered_representation.xml", "xslt/ordered_representation.xsl"},
        {"valhal/xml/person.xml", "xslt/person.xslt"},
        {"valhal/xml/single_file_representation.xml", "xslt/ordered_representation.xsl"},
        {"valhal/xml/work.xml", "xslt/work.xslt"}
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
