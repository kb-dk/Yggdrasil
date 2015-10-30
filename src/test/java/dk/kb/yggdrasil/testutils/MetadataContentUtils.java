package dk.kb.yggdrasil.testutils;

public class MetadataContentUtils {

    public static String getExampleInstanceMetadata() {
        return "<metadata>"
                + "    <provenanceMetadata>"
                + "                <fields>"
                + "                        <uuid>00a36f50-5847-0132-8e30-000c29cc78f6</uuid>"
                + "                </fields>"
                + "        </provenanceMetadata>"
                + "        <preservationMetadata>"
                + "                <fields>"
                + "                        <preservation_profile>A</preservation_profile>"
                + "                        <preservation_state>PRESERVATION_REQUEST_SEND</preservation_state>"
                + "                        <preservation_details>The preservation button has been pushed."
                + "                        </preservation_details>"
                + "                        <preservation_bitsafety>bitSafetyVeryHigh</preservation_bitsafety>"
                + "                        <preservation_confidentiality>confidentialityMedium"
                + "                        </preservation_confidentiality>"
                + "                        <preservation_modify_date>2014-12-16T15:07:36.201+01:00"
                + "                        </preservation_modify_date>"
                + "                        <warc_id>67603c27-e7df-4da4-9a34-7d60baf48eef</warc_id>"
                + "                </fields>"
                + "        </preservationMetadata>"
                + "        <mods xmlns=\"http://www.loc.gov/mods/v3\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                + "                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"3.5\""
                + "                xsi:schemaLocation=\"http://www.loc.gov/mods/v3        http://www.loc.gov/standards/mods/v3/mods-3-5.xsd\">  <!--We have found an instance! -->"
                + "                <name authorityURI=\"http://localhost:3000/resources/changeme:2\">"
                + "                             <role>"
                + "                        <roleTerm>aut</roleTerm>"
                + "                        </role>"
                + "                </name>"
                + "                <titleInfo>"
                + "                        <title>Andeby i Gamle Dage</title>"
                + "                </titleInfo>"
                + "                <relatedItem displayLabel=\"The intellectual content, bummer\">"
                + "                        <identifier type=\"uuid\">ee825870-5846-0132-8e2d-000c29cc78f6"
                + "                        </identifier>"
                + "                </relatedItem>"
                + "                <originInfo>"
                + "                        <dateCreated encoding=\"w3cdtf\">2009</dateCreated>"
                + "                </originInfo>"
                + "                <identifier type=\"isbn\">9780300144246</identifier>"
                + "                <identifier type=\"uuid\">00a36f50-5847-0132-8e30-000c29cc78f6"
                + "                </identifier>"
                + "                <identifier type=\"uri\">http://localhost:3000/resources/changeme:5"
                + "                </identifier>"
                + "        </mods>"
                + "        <file>"
                + "                <name>Lav_gode_sogninger.pdf</name>"
                + "                <uuid>ffedd290-5846-0132-8e2f-000c29cc78f6</uuid>"
                + "        </file>"
                + "        <file>"
                + "                <name>000773452_X02.xml</name>"
                + "                <uuid>1dace680-6759-0132-942e-000c29cc78f6</uuid>"
                + "        </file>"
                + "</metadata>";
    }

    public static String getExampleContentFileMetadata() {
        StringBuilder res = new StringBuilder();

        res.append("<?xml version=\"1.0\"?>");
        res.append("<metadata>");
        res.append("  <provenanceMetadata>");
        res.append("    <fields>");
        res.append("      <uuid>19/2e/da/d8/192edad8-02df-4037-815c-99745e3873d4</uuid>");
        res.append("    </fields>");
        res.append("  </provenanceMetadata>");
        res.append("  <preservationMetadata>");
        res.append("    <fields>");
        res.append("  <preservation_collection>storage</preservation_collection>");
        res.append("  <preservation_state>PRESERVATION_STATE_NOT_LONGTERM</preservation_state>");
        res.append("  <preservation_details>Not longterm preservation.</preservation_details>");
        res.append("  <preservation_modify_date>2015-10-30T10:31:18.089+01:00</preservation_modify_date>");
        res.append("<preservation_bitsafety>bitSafetyVeryLow</preservation_bitsafety><preservation_confidentiality>confidentialityVeryLow</preservation_confidentiality></fields>");
        res.append("  </preservationMetadata>");
        res.append("  <techMetadata>");
        res.append("    <fields>");
        res.append("  <file_uuid>f0845080-6115-0133-afae-0050562881f4</file_uuid>");
        res.append("  <created>2015-10-30T09:24:25Z</created>");
        res.append("  <last_accessed>2015-10-30 10:24:25 +0100</last_accessed>");
        res.append("  <last_modified>2015-10-30 10:24:25 +0100</last_modified>");
        res.append("  <file_checksum>a92386d5c7397844fadef5d837050507</file_checksum>");
        res.append("  <original_filename>xvf.zip</original_filename>");
        res.append("  <mime_type>application/x-zip-compressed</mime_type>");
        res.append("  <file_size>2599</file_size>");
        res.append("</fields>");
        res.append("  </techMetadata>");
        res.append("  <fitsMetadata>");
        res.append("    <fits xmlns=\"http://hul.harvard.edu/ois/xml/ns/fits/fits_output\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://hul.harvard.edu/ois/xml/ns/fits/fits_output http://hul.harvard.edu/ois/xml/xsd/fits/fits_output.xsd\" version=\"0.8.9\" timestamp=\"10/30/15 10:54 AM\">");
        res.append("  <identification>");
        res.append("    <identity format=\"ZIP Format\" mimetype=\"application/zip\" toolname=\"FITS\" toolversion=\"0.8.9\">");
        res.append("      <tool toolname=\"file utility\" toolversion=\"5.04\"/>");
        res.append("      <tool toolname=\"Exiftool\" toolversion=\"9.13\"/>");
        res.append("      <tool toolname=\"Droid\" toolversion=\"6.2.0-SNAPSHOT\"/>");
        res.append("      <tool toolname=\"ffident\" toolversion=\"0.2\"/>");
        res.append("      <tool toolname=\"Tika\" toolversion=\"1.8\"/>");
        res.append("      <version toolname=\"file utility\" toolversion=\"5.04\">2.0</version>");
        res.append("      <externalIdentifier toolname=\"Droid\" toolversion=\"6.2.0-SNAPSHOT\" type=\"puid\">x-fmt/263</externalIdentifier>");
        res.append("    </identity>");
        res.append("  </identification>");
        res.append("  <fileinfo>");
        res.append("    <lastmodified toolname=\"Exiftool\" toolversion=\"9.13\" status=\"SINGLE_RESULT\">2015:10:30 10:54:37+01:00</lastmodified>");
        res.append("    <filepath toolname=\"OIS File Information\" toolversion=\"0.2\" status=\"SINGLE_RESULT\">/tmp/xvf.zip20151030-14748-to7zk1.zip</filepath>");
        res.append("    <filename toolname=\"OIS File Information\" toolversion=\"0.2\" status=\"SINGLE_RESULT\">xvf.zip20151030-14748-to7zk1.zip</filename>");
        res.append("    <size toolname=\"OIS File Information\" toolversion=\"0.2\" status=\"SINGLE_RESULT\">2599</size>");
        res.append("    <md5checksum toolname=\"OIS File Information\" toolversion=\"0.2\" status=\"SINGLE_RESULT\">a92386d5c7397844fadef5d837050507</md5checksum>");
        res.append("    <fslastmodified toolname=\"OIS File Information\" toolversion=\"0.2\" status=\"SINGLE_RESULT\">1446198877000</fslastmodified>");
        res.append("  </fileinfo>");
        res.append("  <filestatus/>");
        res.append("  <metadata/>");
        res.append("  <statistics fitsExecutionTime=\"1051\">");
        res.append("    <tool toolname=\"OIS Audio Information\" toolversion=\"0.1\" status=\"did not run\"/>");
        res.append("    <tool toolname=\"ADL Tool\" toolversion=\"0.1\" status=\"did not run\"/>");
        res.append("    <tool toolname=\"Jhove\" toolversion=\"1.12-SNAPSHOT\" executionTime=\"1028\"/>");
        res.append("    <tool toolname=\"file utility\" toolversion=\"5.04\" executionTime=\"989\"/>");
        res.append("    <tool toolname=\"Exiftool\" toolversion=\"9.13\" executionTime=\"975\"/>");
        res.append("    <tool toolname=\"Droid\" toolversion=\"6.2.0-SNAPSHOT\" executionTime=\"336\"/>");
        res.append("    <tool toolname=\"NLNZ Metadata Extractor\" toolversion=\"3.4GA\" status=\"did not run\"/>");
        res.append("    <tool toolname=\"OIS File Information\" toolversion=\"0.2\" executionTime=\"328\"/>");
        res.append("    <tool toolname=\"OIS XML Metadata\" toolversion=\"0.2\" status=\"did not run\"/>");
        res.append("    <tool toolname=\"ffident\" toolversion=\"0.2\" executionTime=\"861\"/>");
        res.append("    <tool toolname=\"Tika\" toolversion=\"1.8\" executionTime=\"664\"/>");
        res.append("  </statistics>");
        res.append("</fits>");
        res.append("  </fitsMetadata>");
        res.append("</metadata>");
        return res.toString();
    }
}
