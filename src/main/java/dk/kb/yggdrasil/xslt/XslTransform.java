package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

/**
 * Small command line utility to transform an XML file.
 */
public class XslTransform {

    /**
     * Run method.
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: transform <input.xml> <transformer.xsl> <output.xml>");
        } else {
            try {
                File file = new File(args[1]);
                XslTransformer transformer = XslTransformer.getTransformer(file);

                XslUriResolver uriResolver = new XslUriResolver();
                XslErrorListener errorListener = new XslErrorListener();

                file = new File(args[0]);
                Source source = new StreamSource(file);
                byte[] bytes = transformer.transform(source, uriResolver, errorListener);

                /*
                File cacheDir = new File(new File(url.getFile()), "entity_cache");
                if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                    Assert.fail("Could not make entity_cache directory!");
                }
                XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
                */

                XmlEntityResolver entityResolver = null;
                XmlErrorHandler errorHandler = new XmlErrorHandler();

                XmlValidator xmlValidator = new XmlValidator();
                XmlValidationResult result;

                file = new File(args[2]);
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(0);
                raf.setLength(0);
                raf.write(bytes);
                raf.close();
                result = xmlValidator.validate(file, entityResolver, errorHandler);
                if (result != null) {
                    System.out.println("Validated: " + result.bValidate);
                    System.out.println("    Valid: " + !errorHandler.hasErrors());
                } else {
                    System.out.println("Unable to validate file!");
                }
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
