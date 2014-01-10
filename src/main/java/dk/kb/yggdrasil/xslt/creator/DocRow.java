package dk.kb.yggdrasil.xslt.creator;


/**
 * A given documentation element.
 * Represented in a row in a documentation file.
 */
public class DocRow {
	/** Where the data comes from. This can be the XPath, a constant, a method or empty/null.*/ 
	String origin;
	/** Where the position of this */
	String toPath;
	/** The condition for performing this.*/
	String condition;
	/** The namespace of the resulting XML field (should not occur at attributes).*/
	String namespace;
	/** The optional commentary about the transformation row.*/
	String commentary;
	
	/**
	 * Constructor.
	 * @param row A row in the documentation. Should be comma-separated in the order: origin, toPath, condition, namespace, commentary.
	 */
	DocRow(String row) {
		String[] split = row.split(";", 4);
		
		origin = split[0];
		toPath = split[1];
		condition = split[2];
		namespace = split[3];
		if(split.length > 4) {
			commentary = split[5];
		} else {
			commentary = null;
		}
	}
	
	/** 
	 * Checks the value of the fields in the row.
	 * @return Whether all required parts of the row are empty, thus indicating an empty row.
	 */
	boolean isEmpty() {
		return origin.isEmpty() && toPath.isEmpty() && condition.isEmpty() && namespace.isEmpty();
	}
	
	String validate() {
		return toPath.isEmpty() ? "Invalid" : "Valid";
	}
	
	@Override
	public String toString() {
		if(commentary != null) {
			return origin + ";" + toPath + ";" + condition + ";" + namespace + ";" + commentary;
		} else {
			return origin + ";" + toPath + ";" + condition + ";" + namespace;
		}
	}
}