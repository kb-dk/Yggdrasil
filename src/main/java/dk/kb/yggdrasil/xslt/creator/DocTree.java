package dk.kb.yggdrasil.xslt.creator;

import java.util.HashMap;
import java.util.Map;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;

/**
 * The tree structure for the documentation elements.
 */
public class DocTree {
	/** The whole path for this node in the documentation tree. */
	private String path;
	/** The name of the node in the documentation tree. */
	private String nodeName;
	/** The documentation row for this node. */
	private DocRow node;
	/** The children for this node. */
	private Map<String, DocTree> children;
	
	/**
	 * Constructor.
	 * Extracts the path and element-name from the row.
	 * @param row The documentation row for this node.
	 */
	public DocTree(DocRow row) {
		this.node = row;
		this.path = row.toPath;
		this.children = new HashMap<String, DocTree>();
		
		String[] subPaths = row.toPath.split("[@/]");
		this.nodeName = row.toPath.substring(Math.max(0, row.toPath.length() - subPaths[subPaths.length-1].length()-1));
	}
	
	/**
	 * Adds a DocTree node as a child to this node.
	 * The path of the child-node must start with the path of this node.
	 * It is either added as a child directly to this node, or it is sent further to an existing child.
	 * Though it cannot be added as a new child, if it is not a direct child of this node.
	 * @param dt The node to be added as a child to this node.
	 */
	public void addChild(DocTree dt) {
		ArgumentCheck.checkTrue(dt.path.startsWith(path), "The path of the child must start with the same path. Expected path: " + path + ", got: " + dt.path);
		
		String subPath = dt.path.substring(path.length());
		String nextPath = subPath.split("[@/]")[1];
		
		if(children.containsKey(nextPath)) {
			children.get(nextPath).addChild(dt);
		} else {
			ArgumentCheck.checkTrue(subPath.endsWith(nextPath), "The subpath for a new child must be the last path of the path for the child. "
					+ " Excepted: " + nextPath + ", got: " + subPath);
			children.put(nextPath, dt);
		}
	}
	
	/**
	 * Determines whether the name of the node starts with an @, indicating that it is an attribute.
	 * @return True if the node is an XML attribute. False if the node is an XML element.
	 */
	public boolean isAttribute() {
		return nodeName.startsWith("@");
	}
	
	/**
	 * Prints the root element
	 * @param sb The stringbuilder to print the elements and attributes to.
	 */
	public void printXslt(StringBuilder sb) {
		printRoot(sb);
	}

	/**
	 * Prints the root element in the documentation tree.
	 * The level for all 
	 * @param sb The stringbuilder to print the elements and attributes to.
	 */
	private void printRoot(StringBuilder sb) {
		sb.append(DocFunctionUtils.xsltHeader());
		
		printElement(sb, 2);
		
		sb.append(DocFunctionUtils.xsltFooter());
	}
	
	/**
	 * Prints the XSLT for this element.
	 * Starts by handling the commentary, and then deals with the condition.
	 * If no condition, then the node is printed as it is.
	 * 
	 * @param sb The stringbuilder to print the elements and attributes to.
	 * @param level The current depth in the documentation tree.
	 */
	void printXslt(StringBuilder sb, int level) {
		if(node.commentary != null && !node.commentary.isEmpty()) {
			addIndentation(sb, level);
			sb.append("<!-- " + node.commentary + " -->\n");
		}
		if(node.condition != null && !node.condition.isEmpty()) {
			handleCondition(sb, level);
		} else {
			printNode(sb, level);
		}
	}
	
	/**
	 * Prints the current node either as an element, an attribute, or as the root of the documentation tree.
	 * @param sb The stringbuilder to print the elements and attributes to.
	 * @param level The current depth in the documentation tree.
	 */
	private void printNode(StringBuilder sb, int level) {
		switch(nodeName.charAt(0)) {
		case '/': 
			printElement(sb, level);
			break;
		case '@':
			printAttribute(sb, level);
			break;
		default:
			throw new ArgumentCheck("Cannot handle a non-root node, which does not start with either '/' or '@'. "
					+ "Got: " + nodeName);
		}
	}
	
	/**
	 * Prints the attributes before the sub-elements.
	 * @param sb The stringbuilder to print the sub-elements and attributes to.
	 * @param level The current depth in the documentation tree (will add one to print the child).
	 */
	private void printChildren(StringBuilder sb, int level) {
		for(DocTree dt : children.values()) {
			if(dt.isAttribute()) {
				dt.printXslt(sb, level + 1);
			}
		}
		for(DocTree dt : children.values()) {
			if(!dt.isAttribute()) {
				dt.printXslt(sb, level + 1);
			}
		}
	}
	
	/**
	 * Prints this DocTree element as an XML element, including the children.
	 * @param sb The stringbuilder to print the XSLT code.
	 * @param level The current depth in the documentation tree.
	 */
	private void printElement(StringBuilder sb, int level) {
		// Add element declaration start
		addIndentation(sb, level);
		sb.append("<xsl:element name=\"" + getNameWithNamespace() + "\">\n");
		
		addValue(sb, level + 1);
		printChildren(sb, level);
		
		// add element declaration end
		addIndentation(sb, level);
		sb.append("</xsl:element>\n");
	}

	/**
	 * Prints the XSLT for this attribute.
	 * @param sb The stringbuilder to print the XSLT code.
	 * @param level The current depth in the documentation tree.
	 */
	private void printAttribute(StringBuilder sb, int level) {
		addIndentation(sb, level);
		sb.append("<xsl:attribute name=\"" + getNameWithNamespace() + "\">\n");
		
		addValue(sb, level + 1);
		
		// add element declaration end
		addIndentation(sb, level);
		sb.append("</xsl:attribute>\n");
	}
	
	/**
	 * Adds the value regarding the origin part of the documentation row.
	 * This handles functions, constants and XPaths.
	 * @param sb The stringbuilder where the indentation is added.
	 * @param level The number of spaces to print.
	 */
	private void addValue(StringBuilder sb, int level) {
		if(node.origin == null || node.origin.isEmpty()) {
			return;
		}
		
		addIndentation(sb, level);
		String value = node.origin;
		if(value.startsWith("[")) {
			value = DocFunctionUtils.findFunction(value);
		} else if(value.startsWith("\"")) {
			// Make documentation constant into XSLT constant. Changes from " to ' 
			// (Excel makes 3 x ", and we only need 1 of ', so regex for changing any number of concurrent " into 1 ').
			value = value.replaceAll("\"+", "'");
		}
		sb.append("<xsl:value-of select=\"" + value + "\" />\n");
	}
	
	/**
	 * Creates indentation for the XSLT. One space per level.
	 * @param sb The stringbuilder where the indentation is added.
	 * @param level The number of spaces to print.
	 */
	private void addIndentation(StringBuilder sb, int level) {
		for(int i = 0; i < level; i++) {
			sb.append(" ");
		}
	}
	
	/**
	 * Combines the name and namespace for the element/attribute.
	 * Also removes any initial XML-type characters (e.g. '/' or '@'), and any postfixes for 
	 * @return The name of the element/attribute with namespace.
	 */
	private String getNameWithNamespace() {
		String name = nodeName;
		if(name.startsWith("/") || name.startsWith("@")) {
			name = name.substring(1);
		}
		if(name.endsWith("]")) {
			name = name.substring(0, name.indexOf("["));
		}
			
		if(node.namespace == null || node.namespace.isEmpty()) {
			return name;
		} else {
			return node.namespace + ":" + name;
		}
	}

	/**
	 * 
	 * @param sb The stringbuilder to print the elements and attributes to.
	 * @param level The current depth in the documentation tree.
	 */
	private void handleCondition(StringBuilder sb, int level) {
		if(node.condition.startsWith("each")) {
			handleConditionForEach(sb, level);
		} else if (node.condition.startsWith("[copy-of]")) {
			handleConditionCopyOf(sb, level);			
		} else {
			throw new ArgumentCheck("Invalid condition for element: " + node.toString());
		}
	}
	
	/**
	 * Prints an 'for-each' XSLT around this node.
	 * @param sb The stringbuilder to print the elements and attributes to.
	 * @param level The current depth in the documentation tree.
	 */
	private void handleConditionForEach(StringBuilder sb, int level) {
		String fieldName = node.condition.split(" ")[1];
		
		addIndentation(sb, level);
		sb.append("<xsl:for-each select=\"" + fieldName + "\">\n");

		printNode(sb, level + 1);

		addIndentation(sb, level);
		sb.append("</xsl:for-each>\n");
	}
	
	/**
	 * Prints an 'copy-of' XSLT for the given origin node.
	 * @param sb The stringbuilder to print the elements and attributes to.
	 * @param level The current depth in the documentation tree.
	 */
	private void handleConditionCopyOf(StringBuilder sb, int level) {
		addIndentation(sb, level);
		sb.append("<xsl:copy-of select=\"" + node.origin + "\" />\n");
	}
}
