package dk.kb.metadata.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExceptionUtils {

	private static List<RuntimeException> exceptions = new ArrayList<RuntimeException>();
	
	/**
	 * Cleans the list of exceptions.
	 */
	public static void clean() {
		exceptions.clear();
	}
	
	/**
	 * Adds the exception to the list.
	 * @param e The exception to add.
	 */
	public static void insertException(RuntimeException e) {
		exceptions.add(e);
	}
	
	/**
	 * Determines whether any exception has been thrown.
	 * @return Whether any exceptions has been thrown.
	 */
	public static boolean hasFailure() {
		return !exceptions.isEmpty();
	}
	
	/**
	 * If a single exception is caught, then it is returned.
	 * More than one exception will be put in the stacktrace of a generic exception.
	 * @return The caught exception(s).
	 */
	public static Exception retrieveFailure() {
		if(exceptions.size() == 1) {
			return exceptions.get(0);
		}
		
		RuntimeException res = new RuntimeException("Caught multiple exceptions.");
		List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();
		for(Exception e : exceptions) {
			stackTrace.addAll(Arrays.asList(e.getStackTrace()));
		}
		res.setStackTrace(stackTrace.toArray(new StackTraceElement[stackTrace.size()]));
		
		return res;
	}
}
