package dk.kb.yggdrasil.xslt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlErrorHandler implements ErrorHandler {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(XmlErrorHandler.class.getName());
    
	public int errors;

	public int fatalErrors;

	public int warnings;

	@Override
	public void error(SAXParseException exception) throws SAXException {
		++errors;
		logger.error("SAX parsing error!", "Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage(), exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		++fatalErrors;
		logger.error("SAX parsing error!", "Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage(), exception);
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		++warnings;
		logger.warn("SAX parsing warning!", "Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage(), exception);
	}

}
