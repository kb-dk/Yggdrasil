package dk.kb.yggdrasil.xslt;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;

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

	        /*
	        try {
				System.out.println(new String(bytes, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
	            Assert.fail("Unexpected exception!");
			}
			*/

	        Assert.assertNotNull(bytes);
            Assert.assertThat(bytes.length, is(greaterThan(0)));

            Assert.assertEquals(0, errorListener.errors);
            Assert.assertEquals(0, errorListener.fatalErrors);
            Assert.assertEquals(0, errorListener.warnings);

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

            Assert.assertTrue(result.bValidate);
            Assert.assertNull(result.systemId);
            // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" os missing
            Assert.assertEquals(1, result.xsiNamespaces.size());
            Assert.assertEquals(9, result.schemas.size());

            Assert.assertEquals(0, errorHandler.errors);
            Assert.assertEquals(0, errorHandler.fatalErrors);
            Assert.assertEquals(0, errorHandler.warnings);
        } catch (TransformerConfigurationException e) {
			e.printStackTrace();
            Assert.fail("Unexpected exception!");
		} catch (TransformerException e) {
			e.printStackTrace();
            Assert.fail("Unexpected exception!");
		} catch (IOException e) {
			e.printStackTrace();
            Assert.fail("Unexpected exception!");
		}
    }

    public static String[] newXsltFiles = new String[] {
    	"xslt/book.xslt",
    	"xslt/file.xslt",
    	"xslt/ordered_representation.xsl",
    	"xslt/person.xslt",
    	"xslt/work.xslt"
    };

    @Test
    public void test_new_xslt() {
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
    }

}
