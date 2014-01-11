package dk.kb.yggdrasil.xslt.creator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Format for documentation files :
 * 
 * Introduction (one or many)
 * Empty line (only if introduction)
 * Explanation (only one row)
 * origin;toPath;condition;namespace;commentary*
 */
public class XsltDocumentation {

	private List<DocRow> rows;
	private final DocTree root;
	
	public XsltDocumentation(File f) { 
		initializeRows(f);
		
		root = new DocTree(rows.get(0));
		for(int i = 1; i < rows.size(); i++) {
			root.addChild(new DocTree(rows.get(i)));
		}
	}
	
	/**
	 * Prints the resulting XSLT to the print stream. 
	 * @param ps The print stream to put the resulting XSLT.
	 */
	public void printXslt(PrintStream ps) {
		StringBuilder sb = new StringBuilder();
		root.printXslt(sb);
		ps.append(sb.toString());
	}
	
	/**
	 * Initializes the documentation rows in the file.
	 * Has to ignore the first description rows.
	 * @param docFile The documentation file.
	 */
	private void initializeRows(File docFile) {
		List<DocRow> elements = retrieveFullDocumentationList(docFile);
		int index = findFirstTransformationRow(elements);
		rows = elements.subList(index, elements.size());
	}
	
	/**
	 * Finds the first row with XSLT transformation.
	 * There might be some introduction lines to the actual transformation, which are separated by an empty row. 
	 * The actual transformation starts with a row explaining the different columns. This row is ignored.
	 * 
	 * @param rows The list of all the rows, including introduction and explanation rows.
	 * @return The index for the first actual transformation row.
	 */
	private int findFirstTransformationRow(List<DocRow> rows) {
		for(int i = 0; i < rows.size(); i++) {
			if(rows.get(i).isEmpty()) {
				return i + 2;
			}
		}
		return 1;
	}
	
	/**
	 * Retrieves all the rows in the file, even the description ones prior to the actual transformation rows.
	 * 
	 * @param f The file with the transformation documentation.
	 * @return The complete list of rows in the transformation.
	 */
	private List<DocRow> retrieveFullDocumentationList(File f) {
		List<DocRow> res = new ArrayList<DocRow>();
		
		InputStream in = null;
		try {
			in = new FileInputStream(f);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			String line;
			while((line = reader.readLine()) != null) {
				res.add(new DocRow(line));
			}
			
			reader.close();
			in.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed something...", e);
		}
		return res;
	}
}
