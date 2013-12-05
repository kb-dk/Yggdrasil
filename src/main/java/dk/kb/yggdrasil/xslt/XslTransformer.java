package dk.kb.yggdrasil.xslt;

import java.io.File;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;

public class XslTransformer {

	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private Transformer transformerImpl;

	private XslTransformer() {
	}

	public static XslTransformer getTransformer(Source source) throws TransformerConfigurationException {
		XslTransformer transformer = new XslTransformer();
		transformer.transformerImpl = transformerFactory.newTransformer(source);
		return transformer;
	}

	public static XslTransformer getTransformer(File xmlFile) throws TransformerConfigurationException {
		return getTransformer(new StreamSource(xmlFile));
	}

	public Transformer getTransformerImpl() {
		return transformerImpl;
	}

	public void transform(Source xmlSource, URIResolver uriResolver, ErrorListener errorListener, Result outputTarget) throws TransformerException {
		transformerImpl.reset();
		transformerImpl.setURIResolver(uriResolver);
		transformerImpl.setErrorListener(errorListener);
		transformerImpl.transform(xmlSource, outputTarget);
	}

	public Result transform(Source xmlSource, URIResolver uriResolver, ErrorListener errorListener) throws TransformerException {
		StreamResult result = new StreamResult();
		transform(xmlSource, uriResolver, errorListener, result);
		return result;
	}

}
