package dk.kb.metadata.utils;

/**
 * Utility class for String operations.
 */
public final class StringUtils {
	/** Private constructor for this utility class.*/
	private StringUtils() {}

	/**
	 * Validates whether it is possible to split on a comma, ','.
	 * @param line The line to validate.
	 * @return Whether the line contains a ','.
	 */
    public static boolean splitableOnComma(String line) {
        return line.contains(", ");
    }

    /**
     * Splits on the comma, ',', and retrieves the element at the given index.
     * @param line The line to split on a comma.
     * @param elementIndex The index for the element to select.
     * @return The requested element, or null if it is out-of-bounce.
     */
    public static String splitOnComma(String line, int elementIndex) {
        String[] split = line.split(", ");

        if(elementIndex > split.length) {
            return null;
        }

        return split[elementIndex];
    }

	/**
	 * Validates whether it is possible to split on a slash, '/'.
	 * @param line The line to validate.
	 * @return Whether the line contains a '/'.
	 */
    public static boolean splitableOnSlash(String line) {
        return line.contains("/");
    }

    /**
     * Splits on the slash, '/', and retrieves the element at the given index.
     * @param line The line to split on a slash.
     * @param elementIndex The index for the element to select.
     * @return The requested element, or null if it is out-of-bounce.
     */
    public static String splitOnSlash(String line, int element) {
        String[] split = line.split("/");

        if(element > split.length) {
            return null;
        }

        return split[element];
    }

    /**
     * Splits the given line on the slash, '/', and extracts the nominator and denominator (it is assumed that two 
     * such values exists). Then calculates the fraction: nominator / denominator.
     * If no '/' is given, then it is assumed, that the line is not a fraction
     * 
     * @param line The line to split into a nominator and denominator by the slash, '/'.
     * @return The calculated fraction. Or the line argument, if it is not a fraction.
     */
    public static String calculateFraction(String line) {
    	if(!splitableOnSlash(line)) {
    		return line;
    	}
        String[] split = line.split("/");
        
        float nominator = Float.parseFloat(split[0]);
        float denominator = Float.parseFloat(split[1]);

        return "" + (nominator / denominator);
    }

    /**
     * Extracts an integer from a double.
     * @param value The value which is in the double format.
     * @return The integer part of the double value. 
     */
    public static String extractIntegerFromDouble(String value) {
        Double d = Double.valueOf(value);

        return "" + d.intValue();
    }
    
    /**
     * Changes the string into 'UpperCamelCase'.
     * @param s The string to make into upper camel case.
     * @return The upper camel case of the string.
     */
    public static String encodeAsUpperCamelCase(String ... words) {
    	StringBuilder res = new StringBuilder();
    	for(String word : words) {
    		String lowerCase = word.toLowerCase();
    		Character startCharacter = Character.toUpperCase(lowerCase.charAt(0));
    		
    		res.append(startCharacter.toString() + lowerCase.substring(1));
    	}
    	return res.toString();
    }
    
    /**
     * Retrieves the integer nominator of a float in string-format.
     * @param value The fraction value to retrieve the integer nominator for.
     * @return The nominator for the fraction.
     */
    public static String retrieveNominatorAsInteger(String value) {
    	String[] split = value.split("[.]");
    	if(split.length == 1) {
    		return split[0];
    	} else {
    		return split[0] + split[1];
    	}
    }
    
    /**
     * Retrieves the integer denominator of a float in string-format.
     * @param value The fraction value to retrieve the integer denominator for.
     * @return The denominator for the fraction.
     */
    public static String retrieveDenominatorAsInteger(String value) {
    	String[] split = value.split("[.]");
    	if(split.length == 1) {
    		return "1";
    	}
    	String res = "1";
    	for(int i = 0; i < split[1].length(); i++) {
    		res += "0";
    	}
    	return res;
    }
}
