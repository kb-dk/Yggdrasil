package dk.kb.yggdrasil.xslt.creator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Format for documentation files:
 * 
 * Comment*
 * Empty line
 * Initials
 * origin;toPath;condition;namespace;commentary
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
		
		StringBuilder sb = new StringBuilder();
		root.printXslt(sb);
		System.out.println(sb.toString());
	}
	
	public void print() {
		for(DocRow e : rows) {
			System.out.println("Element: " + e.origin + " ; " + e.toPath + " ; " + e.condition + " ; " + e.namespace + " ; " + e.commentary + " -> " + e.validate());
		}
	}
	
	private void initializeRows(File f) {
		List<DocRow> elements = retrieveElements(f);
		int index = findFirstRow(elements);
		rows = elements.subList(index, elements.size());
	}
	
	private int findFirstRow(List<DocRow> elements) {
		for(int i = 0; i < elements.size(); i++) {
			if(elements.get(i).isEmpty()) {
				return i + 2;
			}
		}
		return 1;
	}
	
	private List<DocRow> retrieveElements(File f) {
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
