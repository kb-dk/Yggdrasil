package dk.kb.yggdrasil.xslt.creator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.xslt.XslTransform;

public class XsltCreator {

	public static void main(String ... args) throws IOException {
		if(args.length < 2) {
			System.out.println("Usage: [DocFile.csv] [metadata.xml]\n"
					+ "Or usage: [DocFile.csv] [metadata.xml] [output.xml]");
			System.exit(1);
		}
		File outputFile = null;
		if(args.length < 3) {
			String origFilename = new File(args[1]).getName();
			String name = origFilename.split("[.]")[0];
			outputFile = new File(name + ".mets.xml");
		} else {
			outputFile = new File(args[2]);
		}
		ArgumentCheck.checkTrue(!outputFile.exists(), "The output file at '" + outputFile.getAbsolutePath() 
				+ "' must not already exist.");
		
		File docFile = new File(args[0]);
		ArgumentCheck.checkExistsNormalFile(docFile, docFile.getAbsolutePath());
		File metadataFile = new File(args[1]);
		ArgumentCheck.checkExistsNormalFile(metadataFile, metadataFile.getAbsolutePath());
		
		File xsltFile = createXsltFile(docFile);
		
		XslTransform.main(metadataFile.getAbsolutePath(), xsltFile.getAbsolutePath(), "");
	}
	
	/**
	 * Creates the XSLT file from the documentation file.
	 * @param docFile The file with the transformation documentation.
	 * @return The XSLT file with the transformation.
	 * @throws IOException If something goes wrong with either reading the documentation file or
	 * writing the XSLT file.
	 */
	private static File createXsltFile(File docFile) throws IOException {
		XsltDocumentation doc = new XsltDocumentation(docFile);
		
		File xsltFile = new File(docFile.getAbsolutePath() + ".xslt");
		ArgumentCheck.checkTrue(!xsltFile.exists(), "The output xslt file must not already exist.");
		
		PrintStream ps = null;
		try {
			ps = new PrintStream(xsltFile);
			doc.printXslt(ps);
		} finally {
			if(ps != null) {
				ps.flush();
				ps.close();
				ps = null;
			}
		}
		return xsltFile;
	}
}
