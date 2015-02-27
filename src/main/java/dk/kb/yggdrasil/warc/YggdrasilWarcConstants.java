package dk.kb.yggdrasil.warc;

import dk.kb.yggdrasil.xslt.extension.Agent;

/**
 * Constants for creating WARC files.
 */
public class YggdrasilWarcConstants {
    /**
     * Generate the WarcInfoPayload that Yggdrasil inserts into the warcfiles being
     * produced.
     *
        WARC/1.0
        WARC-Type: warcinfo
        WARC-Date: 2013-05-27T16:34:07Z
        WARC-Record-ID: <urn:uuid:7c9cb0b0-c6da-11e2-aa30-005056887b70>
        Content-Type: application/warc-fields
        Content-Length: 85

        description: http://id.kb.dk/authorities/agents/kbDkDomsBmIngest.html
        revision: 2079
        conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
     *
     * @return the WarcInfoPayload that Yggdrasil inserts into the warcfiles being
     * produced
     */
    public static String getWarcInfoPayload() {
        // make Warc-metadata record (WARC-INFO RECORD) containing
        // link to program description, archiverRevision, and conformsTo "ISO"
        //
        final String LF = "\n";
        final String COLON = ":";
        final String SPACE = " ";

        // 1. description: http://id.kb.dk/authorities/agents/kbDkYggdrasilIngest.html
        String descriptionKey = "description";
        String descriptionValue = Agent.getIngestAgentURL();
        // 2. archiverRevision:
        String revisionKey = "revision";
        String revisionValue = "1.0.0";

        // 3. conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
        String conformsToKey = "conformsTo";
        String conformsToValue = "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf";

        StringBuilder sb = new StringBuilder();
        sb.append(descriptionKey + COLON + SPACE + descriptionValue + LF);
        sb.append(revisionKey + COLON + SPACE + revisionValue + LF);
        sb.append(conformsToKey + COLON + SPACE + conformsToValue + LF);

        return sb.toString();
    }

}
