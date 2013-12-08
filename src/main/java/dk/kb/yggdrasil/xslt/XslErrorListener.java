package dk.kb.yggdrasil.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements an XSL error listener which can be used while transforming XML files.
 */
public class XslErrorListener implements ErrorListener {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(XslErrorListener.class.getName());
    
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
     * Returns a boolean indicating whether this listener has recorded any errors.
     * @return a boolean indicating whether this listener has recorded any errors
     */
    public boolean hasError() {
        return errors != 0 || fatalErrors != 0 || warnings != 0;
    }

    @Override
    public void error(TransformerException exception) throws TransformerException {
        ++errors;
        logger.error("XLST processing error!", exception.getMessageAndLocation(), exception);
    }

    @Override
    public void fatalError(TransformerException exception) throws TransformerException {
        ++fatalErrors;
        logger.error("XLST processing error!", exception.getMessageAndLocation(), exception);
    }

    @Override
    public void warning(TransformerException exception) throws TransformerException {
        ++warnings;
        logger.warn("XLST processing warning!", exception.getMessageAndLocation(), exception);
    }

}
