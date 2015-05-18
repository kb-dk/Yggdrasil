package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class TestXmlValidator {

    public static Object[][] xmlFiles = new Object[][] {
        {1, 1, 1, 1, "valhal/xml/basic_file.xml"},
        {1, 1, 1, 1, "valhal/xml/content_file_update.xml"},
        {1, 1, 1, 1, "valhal/xml/instance_with_unordered_multiple_files.xml"},
        {1, 1, 1, 1, "valhal/xml/instance_update.xml"},
        {1, 1, 1, 1, "valhal/xml/namespaceless_mods.xml"}
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

        boolean bool;
        try {
            XmlValidator xmlValidator = new XmlValidator();
            XmlValidationResult result;

            XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
            XmlErrorHandler errorHandler = new XmlErrorHandler();

            for (int i=0; i<xmlFiles.length; ++i) {
                int expectedNamespaces = (Integer)xmlFiles[i][0];
                int expectedSchemas = (Integer)xmlFiles[i][1];
                int expectedErrors1 = (Integer)xmlFiles[i][2];
                int expectedErrors2 = (Integer)xmlFiles[i][3];
                String xmlFilename = (String)xmlFiles[i][4];
                //System.out.println(xmlFilename);
                //System.out.println(result.xsiNamespaces.size());
                //System.out.println(result.schemas.size());
                //System.out.println(errorHandler.errors);
                /*
                 * Combined.
                 */
                url = this.getClass().getClassLoader().getResource(xmlFilename);
                Assert.assertNotNull(url);
                file = new File(url.getFile());
                errorHandler.reset();
                result = xmlValidator.validate(file, entityResolver, errorHandler);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                if (expectedSchemas == 0) {
                    Assert.assertFalse(xmlFilename, result.bXsdUsed);
                } else {
                    Assert.assertTrue(xmlFilename, result.bXsdUsed);
                }
                Assert.assertEquals(xmlFilename, expectedNamespaces, result.xsiNamespaces.size());
                Assert.assertEquals(xmlFilename, expectedSchemas, result.schemas.size());

                Assert.assertEquals(xmlFilename, expectedErrors1, errorHandler.numberOfErrors);
                Assert.assertEquals(xmlFilename, 0, errorHandler.numberOfFatalErrors);
                Assert.assertEquals(xmlFilename, 0, errorHandler.numberOfWarnings);

                Assert.assertTrue(xmlFilename, result.bWellformed);
                Assert.assertFalse(xmlFilename, result.bValid);
                /*
                 * Separated with ErrorHandler.
                 */
                errorHandler.reset();
                result = new XmlValidationResult();
                bool = xmlValidator.testStructuralValidity(url.openStream(), entityResolver, errorHandler, result);

                Assert.assertTrue(xmlFilename, bool);

                Assert.assertNull(xmlFilename, result.systemId);
                Assert.assertFalse(xmlFilename, result.bDtdUsed);
                if (expectedSchemas == 0) {
                    Assert.assertFalse(xmlFilename, result.bXsdUsed);
                } else {
                    Assert.assertTrue(xmlFilename, result.bXsdUsed);
                }
                Assert.assertEquals(xmlFilename, expectedNamespaces, result.xsiNamespaces.size());
                Assert.assertEquals(xmlFilename, expectedSchemas, result.schemas.size());

                Assert.assertEquals(0, errorHandler.numberOfErrors);
                Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
                Assert.assertEquals(0, errorHandler.numberOfWarnings);

                bool = xmlValidator.testDefinedValidity(url.openStream(), entityResolver, errorHandler, result);

                Assert.assertFalse(bool);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                if (expectedSchemas == 0) {
                    Assert.assertFalse(result.bXsdUsed);
                } else {
                    Assert.assertTrue(result.bXsdUsed);
                }
                Assert.assertEquals(expectedNamespaces, result.xsiNamespaces.size());
                Assert.assertEquals(expectedSchemas, result.schemas.size());

                Assert.assertEquals(expectedErrors2, errorHandler.numberOfErrors);
                Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
                Assert.assertEquals(0, errorHandler.numberOfWarnings);

                Assert.assertTrue(result.bWellformed);
                Assert.assertFalse(result.bValid);
                /*
                 * Separated with null ErrorHandler.
                 */
                result = new XmlValidationResult();
                bool = xmlValidator.testStructuralValidity(url.openStream(), entityResolver, null, result);

                Assert.assertTrue(bool);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                if (expectedSchemas == 0) {
                    Assert.assertFalse(result.bXsdUsed);
                } else {
                    Assert.assertTrue(result.bXsdUsed);
                }
                Assert.assertEquals(expectedNamespaces, result.xsiNamespaces.size());
                Assert.assertEquals(expectedSchemas, result.schemas.size());

                bool = xmlValidator.testDefinedValidity(url.openStream(), entityResolver, null, result);

                Assert.assertFalse(bool);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                if (expectedSchemas == 0) {
                    Assert.assertFalse(result.bXsdUsed);
                } else {
                    Assert.assertTrue(result.bXsdUsed);
                }
                Assert.assertEquals(expectedNamespaces, result.xsiNamespaces.size());
                Assert.assertEquals(expectedSchemas, result.schemas.size());

                Assert.assertTrue(result.bWellformed);
                Assert.assertFalse(result.bValid);
                /*
                 * Only validation with ErrorHandler.
                 */
                errorHandler.reset();
                result = new XmlValidationResult();
                bool = xmlValidator.testDefinedValidity(url.openStream(), entityResolver, errorHandler, result);

                Assert.assertFalse(bool);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                Assert.assertFalse(result.bXsdUsed);
                Assert.assertEquals(0, result.xsiNamespaces.size());
                Assert.assertEquals(0, result.schemas.size());

                Assert.assertEquals(expectedErrors2, errorHandler.numberOfErrors);
                Assert.assertEquals(0, errorHandler.numberOfFatalErrors);
                Assert.assertEquals(0, errorHandler.numberOfWarnings);

                Assert.assertFalse(result.bWellformed);
                Assert.assertFalse(result.bValid);
                /*
                 * Only validation with null ErrorHandler.
                 */
                result = new XmlValidationResult();
                bool = xmlValidator.testDefinedValidity(url.openStream(), entityResolver, null, result);

                Assert.assertFalse(bool);

                Assert.assertNull(result.systemId);
                Assert.assertFalse(result.bDtdUsed);
                Assert.assertFalse(result.bXsdUsed);
                Assert.assertEquals(0, result.xsiNamespaces.size());
                Assert.assertEquals(0, result.schemas.size());

                Assert.assertFalse(result.bWellformed);
                Assert.assertFalse(result.bValid);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (YggdrasilException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

}
