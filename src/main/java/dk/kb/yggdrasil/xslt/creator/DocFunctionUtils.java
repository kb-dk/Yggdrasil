package dk.kb.yggdrasil.xslt.creator;

import java.util.HashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;

/**
 * Utility functions for dealing with the transformation function values.
 * This includes the values which should be replaced by java functions or XSLT functions.
 */
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
	
	/**
	 * Creating all the relations between the function names and their java functions.
	 */
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
		javaFunctionMap.put("[Organization-ID]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getOrganizationID()");
		javaFunctionMap.put("[Organization-Name]", "java:dk.kb.yggdrasil.xslt.extension.Agent.getOrganizationName()");
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

		if(function.startsWith("[CONCAT]")) {
			String[] split = function.split(" ");
			ArgumentCheck.checkTrue(split.length >= 3, "A concatination function requires at least two arguments, "
					+ "and must be in format: [CONCAT] first second ...\nBut this function was: '" + function + "'");
			
			String res = "concat(" + formatConstant(split[1]);
			for(int i = 2; i < split.length; i++) {
				res += ", " + formatConstant(split[i]);
			}
			res += ")";
			return res;
		}
		
		throw new ArgumentCheck("Could not find a corresponding function to: '" + function + "'.");
	}
	
	/**
	 * Extracts the constant in the right format.
	 * Make documentation constant into XSLT constant. Changes from " to '.
	 * (Excel makes 3 x ", and we only need 1 of ', so regex for changing any number of concurrent " into 1 ').
	 *  
	 * @param value The value for the constant in documentation format.
	 * @return The constant formatted for XSLT.
	 */
	public static String formatConstant(String value) {
		return value.replaceAll("\"+", "'");
	}
}
