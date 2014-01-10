package dk.kb.yggdrasil.xslt.creator;

import java.util.HashMap;
import java.util.Map;

public class DocFunctionUtils {
	
	/**
	 * Creates the predetermined header for the XSLT scripts.
	 * This involves the XML header, defining the transformation with schema references and extension prefixes,
	 * defining the output format, initializing the template for transforming the incoming metadata.
	 * @return The XSLT header for the transformation template.
	 */
	public static String xsltHeader() {
		return 	  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<xsl:transform version=\"1.0\"\n"
				+ "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
				+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "    xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
				+ "    xmlns:java=\"http://xml.apache.org/xalan/java\"\n"
				+ "    xmlns:mets=\"http://www.loc.gov/METS/\"\n"
				+ "    xmlns:mix=\"http://www.loc.gov/mix/v20\"\n"
				+ "    xmlns:mods=\"http://www.loc.gov/mods/v3\"\n"
				+ "    xmlns:premis=\"info:lc/xmlns/premis-v2\"\n"
				+ "\n"
				+ "    extension-element-prefixes=\"java\">\n"
				+ "\n"
				+ " <xsl:output encoding=\"UTF-8\" method=\"xml\" indent=\"yes\" />\n"
				+ "\n"
				+ " <xsl:template match=\"metadata\">\n";
//				+ "  <xsl:call-template name=\"mets_generator\" />\n"
//				+ " </xsl:template>\n"
//				+ "\n"
//				+ " <xsl:template name=\"mets_generator\">\n";
	}
	
	/**
	 * Creates the footer for the XSLT scripts.
	 * This is just the end-tags for the template and transformation elements.
	 * @return The XSLT footer for the transformation template.
	 */
	public static String xsltFooter() {
		return    " </xsl:template>\n"
				+ "</xsl:transform>\n";
	}
	
	private static Map<String, String> javaFunctionMap = new HashMap<String, String>();
	static {
		javaFunctionMap.put("[DATE-NOW]", "java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()");
		javaFunctionMap.put("[TIME-NOW]", "java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()");
		javaFunctionMap.put("[UUID]", "java:dk.kb.yggdrasil.xslt.extension.UUIDExtension.getRandomUUID()");
		javaFunctionMap.put("[MODS-ACCESS]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getModsAccessCondition()");
		javaFunctionMap.put("[profile_uri]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getProfileURL()");
		javaFunctionMap.put("[Yggdrasil-api-id]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getAPIID()");
		javaFunctionMap.put("[Yggdrasil-api-name]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getAPIName()");
		javaFunctionMap.put("[Yggdrasil-api-note]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getAPINote()");
	}
	
	/**
	 * Finds the given java function corresponding to the documentation value.
	 * @param function The name of the function, including potential parameters.
	 * @return The name of the function, including its potential parameters.
	 */
	public static String findFunction(String function) {
		
		if(javaFunctionMap.containsKey(function)) {
			return javaFunctionMap.get(function);
		}

		return "CODE: " + function;
		//throw new ArgumentCheck("Could not find a corresponding function to: '" + function + "'.");
	}
	
	
}
