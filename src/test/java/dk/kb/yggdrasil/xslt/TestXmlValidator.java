package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestXmlValidator {

    public static String[] xmlFiles = new String[] {
        "valhal/xml/basic_file.xml",
        "valhal/xml/book.xml",
        "valhal/xml/ordered_representation.xml",
        "valhal/xml/person.xml",
        "valhal/xml/single_file_representation.xml",
        "valhal/xml/work.xml"
    };

    @Test
    public void test_xmlvalidator() {
        URL url;
        File file;

        url = this.getClass().getClassLoader().getResource("");
        File cacheDir = new File(new File(url.getFile()), "entity_cache");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Assert.fail("Could not make entity_cache directory!");
        }

        url = this.getClass().getClassLoader().getResource("xml/Carrebye.xml");
        file = new File(url.getFile());

        XmlValidator xmlValidator = new XmlValidator();
        XmlValidationResult result;

        XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
        XmlErrorHandler errorHandler = new XmlErrorHandler();

        result = xmlValidator.validate(file, entityResolver, errorHandler);

        Assert.assertTrue(result.bValidate);
        Assert.assertNull(result.systemId);
        Assert.assertEquals(1, result.xsiNamespaces.size());
        Assert.assertEquals(9, result.schemas.size());

        Assert.assertEquals(0, errorHandler.errors);
        Assert.assertEquals(0, errorHandler.fatalErrors);
        Assert.assertEquals(0, errorHandler.warnings);

        url = this.getClass().getClassLoader().getResource("xml/Car_S-9092.tif.raw.xml");
        file = new File(url.getFile());

        result = xmlValidator.validate(file, entityResolver, errorHandler);

        Assert.assertFalse(result.bValidate);
        Assert.assertNull(result.systemId);
        Assert.assertEquals(0, result.xsiNamespaces.size());
        Assert.assertEquals(0, result.schemas.size());

        Assert.assertEquals(0, errorHandler.errors);
        Assert.assertEquals(0, errorHandler.fatalErrors);
        Assert.assertEquals(0, errorHandler.warnings);

        // FIXME
        /*
        for (int i=0; i<xmlFiles.length; ++i) {
            url = this.getClass().getClassLoader().getResource(xmlFiles[i]);
            file = new File(url.getFile());
            errorHandler.reset();
            result = xmlValidator.validate(file, entityResolver, errorHandler);

            Assert.assertFalse(result.bValidate);
            Assert.assertNull(result.systemId);
            Assert.assertEquals(0, result.xsiNamespaces.size());
            Assert.assertEquals(0, result.schemas.size());

            Assert.assertEquals(0, errorHandler.errors);
            Assert.assertEquals(0, errorHandler.fatalErrors);
            Assert.assertEquals(0, errorHandler.warnings);
        }
        */
    }

}
