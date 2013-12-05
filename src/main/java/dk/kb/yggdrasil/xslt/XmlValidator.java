package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

public class XmlValidator {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(XmlValidator.class.getName());
    
	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 

	private DocumentBuilderFactory factoryParsing;
	private DocumentBuilder builderParsing;

	private DocumentBuilderFactory factoryValidating;
	private DocumentBuilder builderValidating;

	/**
	 * Construct an <code>XmlValidator</code>.
	 */
	public XmlValidator() {
    	factoryParsing = DocumentBuilderFactory.newInstance();
    	factoryParsing.setNamespaceAware(true);
    	factoryParsing.setValidating(false);
		factoryValidating = DocumentBuilderFactory.newInstance();
    	factoryValidating.setNamespaceAware(true);
    	factoryValidating.setValidating(true);
		factoryValidating.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
		try {
    		builderParsing = factoryParsing.newDocumentBuilder();
    		builderValidating = factoryValidating.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("Could not create a new 'DocumentBuilder'!");
		}
    }

	public Document document = null;

	public String systemId = null; 

	public void validate(File xmlFile, EntityResolver entityResolver, ErrorHandler errorHandler) {
    	InputStream in = null;
    	document = null;
    	try {
    		in = new FileInputStream(xmlFile);
    		builderParsing.reset();
    		builderParsing.setErrorHandler(errorHandler);
    		document = builderParsing.parse(in);
    		in.close();
    		in = null;

    		systemId = null;
    		DocumentType documentType = document.getDoctype();
    		boolean bValidate = false;
    		if (documentType != null) {
        		systemId = documentType.getSystemId();
    		}
    		if (systemId != null) {
    			// debug
    			//System.out.println("systemId: " + systemId);
        		bValidate = true;
    		} else {
        		XPathFactory xpf = XPathFactory.newInstance();
        		XPath xp = xpf.newXPath();
        		NodeList nodes;
    			Node node;
    			// JDK6 XPath engine supposedly only implements v1.0 of the specs.
        		nodes = (NodeList)xp.evaluate("//*", document.getDocumentElement(), XPathConstants.NODESET);
        		for (int i = 0; i < nodes.getLength(); i++) {
        			node = nodes.item(i).getAttributes().getNamedItem("xmlns:xsi");
        			if (node != null) {
        				// debug
        				//System.out.println(node.getNodeValue());
                		bValidate = true;
        			}
        			node = nodes.item(i).getAttributes().getNamedItem("xsi:schemaLocation");
        			if (node != null) {
        				// debug
        				//System.out.println(node.getNodeValue());
                		bValidate = true;
        			}
        		}
    		}
    		if (bValidate) {
        		in = new FileInputStream(xmlFile);
        		builderValidating.reset();
        		builderValidating.setEntityResolver(entityResolver);
        		builderValidating.setErrorHandler(errorHandler);
        		document = builderValidating.parse(in);
        		in.close();
        		in = null;
    		}
    	} catch (Throwable t) {
			logger.error("Exception validating XML stream!", t.toString(), t);
    	} finally {
    		if (in != null) {
    			try {
					in.close();
				} catch (IOException e) {
					logger.error("Exception closing stream!", e.toString(), e);
				}
    			in = null;
    		}
    	}
	}

}
