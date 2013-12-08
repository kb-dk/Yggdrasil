package dk.kb.yggdrasil.xslt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implements an XML error handler which can be used while parsing/validating XML files.
 */
public class XmlErrorHandler implements ErrorHandler {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(XmlErrorHandler.class.getName());

    /** Errors accumulated. */
    public int errors;

    /** Fatal errors accumulated. */
    public int fatalErrors;

    /** Warnings accumulated. */
    public int warnings;

    /**
     * Reset accumulated errors counters.
     */
    public void reset() {
        errors = 0;
        fatalErrors = 0;
        warnings = 0;
    }

    /**
     * Returns a boolean indicating whether this handler has recorded any errors.
     * @return a boolean indicating whether this handler has recorded any errors
     */
    public boolean hasError() {
        return errors != 0 || fatalErrors != 0 || warnings != 0;
    }

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
