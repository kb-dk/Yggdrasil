package dk.kb.yggdrasil.xslt;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;

/**
 * A class containing various information produced by the XML validator.
 */
public class XmlValidationResult {

    /** Parsed XML document. */
    public Document document = null;

    /** Was the document validated against DTD/XSD. */
    public boolean bValidate = false;

    /** XML document systemId, sometimes referred to as the DTD. */
    public String systemId = null; 

    /** xmlns:xsi namespace(s). */
    public List<String> xsiNamespaces = new LinkedList<String>();

    /** Schemas referred to in the XML document. */
    public List<String> schemas = new LinkedList<String>();

}
