package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestXslt {

	private static String[] xsltFiles = {
		"xslt/transformRepresentationMets.xsl",
		"xslt/retrieveMetsRelationship.xsl",
		"xslt/transformToMets.xsl",
		"xslt/transformToMix.xsl",
		"xslt/transformToMods.xsl",
		"xslt/transformToPremis.xsl"
	};

    @Test
    public void test_xslt() {
    	for (int i=0; i<xsltFiles.length; ++i) {
			try {
				URL url = this.getClass().getClassLoader().getResource(xsltFiles[i]);
				File file = new File(url.getFile());
	    		StreamSource source = new StreamSource(file);
	    		//source.setSystemId(file.getPath());
	    		XslTransformer.getTransformer(source);

	    		XslUriResolver uriResolver = new XslUriResolver();
	    		XslErrorListener errorListener = new XslErrorListener();
	    		// , uriResolver, errorListener
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
				Assert.fail("Unexpected exception!");
			}
    	}
    }

}
