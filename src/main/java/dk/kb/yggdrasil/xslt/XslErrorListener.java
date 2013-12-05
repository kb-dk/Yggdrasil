package dk.kb.yggdrasil.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XslErrorListener implements ErrorListener {

    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(XslErrorListener.class.getName());
    
	public int errors;

	public int fatalErrors;

	public int warnings;

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
