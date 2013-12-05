package dk.kb.yggdrasil.xslt;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestXmlValidator {

    @Test
	public void test_xmlvalidator() {
		URL url;

		url = this.getClass().getClassLoader().getResource("");
		File cacheDir = new File(new File(url.getFile()), "entity_cache");
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			Assert.fail("Could not make entity_cache directory!");
		}

		url = this.getClass().getClassLoader().getResource("xml/Carrebye.xml");
		File file = new File(url.getFile());

		XmlValidator xmlValidator = new XmlValidator();

		XmlEntityResolver entityResolver = new XmlEntityResolver(cacheDir);
		XmlErrorHandler errorHandler = new XmlErrorHandler();

		xmlValidator.validate(file, entityResolver, errorHandler);

		Assert.assertEquals(0, errorHandler.errors);
		Assert.assertEquals(0, errorHandler.fatalErrors);
		Assert.assertEquals(0, errorHandler.warnings);
    }

}
