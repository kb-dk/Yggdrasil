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
                //XmlEntityResolver entityResolver = new XmlEntityResolver(cache_dir);
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
