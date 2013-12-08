package dk.kb.metadata.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Utilities for validating whether a file format is relevant for a given metadata schema.
 */
public class FileFormatUtils {

	/** The file format name for Tiff images. */
	private static String FILE_FORMAT_TIFF = "TIFF Image";
	
	/** The list of file formats for the MIX metadata schema. */
	private static List<String> formatsForMix = Arrays.asList(
			FILE_FORMAT_TIFF);

	/**
	 * Determines whether the file format is amongst the file formats with technical metadata in MIX/NISO.
	 * @param format The file format.
	 * @return Whether it is a MIX format.
	 */
	public static boolean formatForMix(String format) {
		return formatsForMix.contains(format);
	}
}
