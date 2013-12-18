package dk.kb.yggdrasil.xslt;

import java.io.File;

/**
 * Small command line utility to validate an XML file.
 */
public class XmlValidate {

    /**
     * Run method.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: validate <file.xml>");
        } else {
            try {
                XmlValidator xmlValidator = new XmlValidator();
                File xmlFile = new File(args[0]);
                // Un-comment to enable DTD/XSL caching.
                /*
                File cacheDir = new File(new File(url.getFile()), "entity_cache");
                if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                    Assert.fail("Could not make entity_cache directory!");
                }
                XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
                */
                XmlEntityResolver entityResolver = null;
                XmlErrorHandler errorHandler = new XmlErrorHandler();
                XmlValidationResult result = xmlValidator.validate(xmlFile, entityResolver, errorHandler);
                if (result != null) {
                    System.out.println("Validated: " + result.bValidate);
                    System.out.println("    Valid: " + !errorHandler.hasErrors());
                } else {
                    System.out.println("Unable to validate file!");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

}
