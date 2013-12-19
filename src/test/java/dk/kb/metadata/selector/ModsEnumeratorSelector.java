package dk.kb.metadata.selector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains the different selectors for the MODS metadata, some will translate the value into the 
 * corresponding in the enumerator.
 * 
 * The different selectors will through 'IllegalStateException' if the given value 
 * cannot be found within their enumerator.
 */
public final class ModsEnumeratorSelector {
	/** Private constructor for this Utility class.*/
	private ModsEnumeratorSelector() {}

	/** The restrictions for the 'mods:typeOfResource'*/
	private static final Set<String> TYPE_OF_RESOURCE_RESTRICTIONS = new HashSet<String>(Arrays.asList("text", 
			"cartographic", "notated music", "sound recording-musical", "sound recording-nonmusical", 
			"sound recording", "still image", "moving image", "three dimensional object", "software, multimedia", 
	        "mixed material"));

	/**
	 * Selects a given valid entry for the field 'mods:typeOfResource'.
	 * @param typeOfResource The value to validate. 
	 * @param defaultValue The default value, if the given typeOfResource does not validate.
	 * Note, that this value it not validated.
	 * @return The evaluated value.
	 */
	public static String typeOfResource(String typeOfResource, String defaultValue) {
		if(typeOfResource == null || typeOfResource.isEmpty()) {
			return defaultValue;
		}

		if(TYPE_OF_RESOURCE_RESTRICTIONS.contains(typeOfResource)) {
			return typeOfResource;
		}

		return defaultValue;
	}

	/** The collection of the restrictions values for the 'mods:relatedItem/@type'.*/
	private static final Set<String> RELATED_ITEM_TYPE_RESTRICTION = new HashSet<String>(Arrays.asList("preceding", 
			"succeeding", "original", "host", "constituent", "series", "otherVersion", "otherFormat", "isReferencedBy", 
			"references", "reviewOf"));

	/**
	 * Retrieves the restricted value for the 'mode:relatedItem/@type'.
	 * @param type The type to validate.
	 * @param defaultValue The default value.
	 * @return The evaluated value.
	 */
	public static String relatedItemAttributeType(String type, String defaultValue) {
		if(type == null || type.isEmpty()) {
			return defaultValue;
		}

		if(RELATED_ITEM_TYPE_RESTRICTION.contains(type)) {
			return type;
		}

		return defaultValue;
	}
}
