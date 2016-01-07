<?xml version="1.0" encoding="UTF-8"?> 
<xsl:transform version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:mix="http://www.loc.gov/mix/v20"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:premis="info:lc/xmlns/premis-v2"
    
    extension-element-prefixes="java">
  
  <xsl:output encoding="UTF-8" method="xml" indent="yes" />

  <xsl:template match="metadata">
    <xsl:call-template name="file_mets_generator" />
  </xsl:template>
  
  <xsl:template name="file_mets_generator">
    <mets:mets xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version110/mets.xsd">
      <xsl:attribute name="TYPE">
        <xsl:value-of select="'File'" />
      </xsl:attribute>
      <xsl:attribute name="OBJID">
        <xsl:value-of select="provenanceMetadata/fields/uuid" />
      </xsl:attribute>
      <xsl:attribute name="PROFILE">
        <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getProfileURL()" />
      </xsl:attribute>
            
      <!-- START metsHdr -->
      <xsl:element name="mets:metsHdr">
        <xsl:attribute name="CREATEDATE">
          <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
        </xsl:attribute>

        <xsl:element name="mets:agent">
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getOrganizationID()" />
          </xsl:attribute>
          <xsl:attribute name="ROLE"> 
            <xsl:value-of select="'CREATOR'" />
          </xsl:attribute>
          <xsl:attribute name="TYPE">
            <xsl:value-of select="'ORGANIZATION'" />
          </xsl:attribute>
          <xsl:element name="mets:name">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getOrganizationName()" />
          </xsl:element>
        </xsl:element>
        
        <xsl:element name="mets:agent">
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getAPIID()" />
          </xsl:attribute>
          <xsl:attribute name="ROLE"> 
            <xsl:value-of select="'CREATOR'" />
          </xsl:attribute>
          <xsl:attribute name="TYPE">
            <xsl:value-of select="'OTHER'" />
          </xsl:attribute>
          <xsl:attribute name="OTHERTYPE">
            <xsl:value-of select="'API'" />
          </xsl:attribute>
          <xsl:element name="mets:name">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getAPIName()" />
          </xsl:element>
          <xsl:element name="mets:note">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getAPINote()" />
          </xsl:element>
        </xsl:element>
        
        <xsl:element name="mets:agent">
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getDepartmentID()" />
          </xsl:attribute>
          <xsl:attribute name="ROLE">
            <xsl:value-of select="'EDITOR'" />
          </xsl:attribute>
          <xsl:attribute name="TYPE">
            <xsl:value-of select="'OTHER'" />
          </xsl:attribute>
          <xsl:attribute name="OTHERTYPE">
            <xsl:value-of select="'DEPARTMENT'" />
          </xsl:attribute>
          <xsl:element name="mets:name">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getDepartmentName()" />
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END metsHdr -->
      
      <!-- START amdSec -->
      <xsl:element name="mets:amdSec">
        <!-- ADD PREMIS:OBJECT -->
        <xsl:element name="mets:techMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="'PremisObject1'" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'PREMIS:OBJECT'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <premis:object xsi:schemaLocation="info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd" xsi:type="premis:file">
                <xsl:element name="premis:objectIdentifier">
                  <xsl:element name="premis:objectIdentifierType">
                    <xsl:value-of select="'UUID'" />
                  </xsl:element>
                  <xsl:element name="premis:objectIdentifierValue">
                    <xsl:value-of select="techMetadata/fields/file_uuid" />
                  </xsl:element>
                </xsl:element>
                <!-- BEGIN objectCharacteristics -->
                <xsl:element name="premis:objectCharacteristics">
                  <!--Mandatory for a PREMIS File object, set to 0 (zero - the default value) here as no information is received from the repository.-->
                  <xsl:element name="premis:compositionLevel">
                    <xsl:value-of select="0" />
                  </xsl:element>
                  <xsl:element name="premis:fixity">
                    <xsl:element name="premis:messageDigestAlgorithm">
                      <xsl:value-of select="'MD5'" />
                    </xsl:element>
                    <xsl:element name="premis:messageDigest">
                      <xsl:value-of select="techMetadata/fields/file_checksum" />
                    </xsl:element>
                    <xsl:element name="premis:messageDigestOriginator">
                      <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getMessageDigestOriginator()" />
                    </xsl:element>
                  </xsl:element>
                  <!--Size of the file in bytes.-->  
                  <xsl:element name="premis:size">
                    <xsl:value-of select="techMetadata/fields/file_size" />
                  </xsl:element>
                  <xsl:element name="premis:format">
                    <xsl:element name="premis:formatDesignation">
                      <xsl:element name="premis:formatName">
                        <xsl:value-of select="techMetadata/fields/mime_type" />
                      </xsl:element>
                    </xsl:element>
                  </xsl:element>
                  <!-- BEGIN embedding FITS -->
                  <xsl:element name="premis:objectCharacteristicsExtension">
                    <xsl:copy-of select="fitsMetadata1"/> 
                  </xsl:element>
                  <!-- END embedding FITS -->
                </xsl:element>
                <!-- END objectCharacteristics -->
                <!-- BEGIN originalName -->
                <xsl:element name="premis:originalName">
                  <xsl:value-of select="techMetadata/fields/original_filename"/>
                </xsl:element>
                <!-- END originalName -->
              </premis:object>
            </xsl:element>
          </xsl:element>
        </xsl:element>
        <!-- ADD MODS (rights) -->
        <xsl:element name="mets:rightsMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="'ModsRights1'" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'MODS'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <!-- START MODS -->
              <mods:mods xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-5.xsd" version="3.5">
                <xsl:element name="mods:accessCondition">
                  <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getModsAccessCondition()" />
                </xsl:element>
              </mods:mods>
              <!-- END MODS -->  
            </xsl:element>
          </xsl:element>
        </xsl:element>
        <!-- ADD PREMIS -->
        <xsl:element name="mets:digiprovMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="'Premis1'" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'PREMIS'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <!-- Preservation level for bit safety. -->
              <premis:preservationLevel xsi:schemaLocation="info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd">
                <xsl:element name="premis:preservationLevelValue">
                  <xsl:value-of select="preservationMetadata/fields/preservation_bitsafety" />
                </xsl:element>
                <xsl:element name="premis:preservationLevelDateAssigned">
                  <xsl:value-of select="preservationMetadata/fields/preservation_modify_date" />
                </xsl:element>
              </premis:preservationLevel>
              <!-- Preservation level for confidentiality. -->
              <premis:preservationLevel xsi:schemaLocation="info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd">
                <xsl:element name="premis:preservationLevelValue">
                  <xsl:value-of select="preservationMetadata/fields/preservation_confidentiality" />
                </xsl:element>
                <xsl:element name="premis:preservationLevelDateAssigned">
                  <xsl:value-of select="preservationMetadata/fields/preservation_modify_date" />
                </xsl:element>
              </premis:preservationLevel>
            </xsl:element>
          </xsl:element>
        </xsl:element>
        <!-- ADD PREMIS:EVENT -->
        <xsl:element name="mets:digiprovMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="'PremisEvent1'" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'PREMIS:EVENT'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <premis:event xsi:schemaLocation="info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd">
                <xsl:element name="premis:eventIdentifier">
                  <xsl:element name="premis:eventIdentifierType">
                    <xsl:value-of select="'UUID'" />
                  </xsl:element>
                  <xsl:element name="premis:eventIdentifierValue">
                    <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.UUIDExtension.getRandomUUID()" />
                  </xsl:element>
                </xsl:element>
                <xsl:choose>
                  <xsl:when test="preservationMetadata/fields/warc_id">
                    <xsl:element name="premis:eventType">
                      <xsl:value-of select="'update'" />
                    </xsl:element>                   
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:element name="premis:eventType">
                      <xsl:value-of select="'ingestion'" />
                    </xsl:element>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:element name="premis:eventDateTime">
                  <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
                </xsl:element>
                <xsl:element name="premis:linkingAgentIdentifier">
                  <xsl:element name="premis:linkingAgentIdentifierType">
                    <xsl:value-of select="'URL'" />
                  </xsl:element>
                  <xsl:choose>
                    <xsl:when test="preservationMetadata/fields/warc_id">
                      <xsl:element name="premis:linkingAgentIdentifierValue">
                        <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getUpdateAgentURL()" />
                      </xsl:element>
                      <xsl:element name="premis:linkingAgentRole">
                        <xsl:value-of select="'Update'" />
                      </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:element name="premis:linkingAgentIdentifierValue">
                        <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getIngestAgentURL()" />
                      </xsl:element>
                      <xsl:element name="premis:linkingAgentRole">
                        <xsl:value-of select="'Ingest'" />
                      </xsl:element>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:element>
                <xsl:element name="premis:linkingObjectIdentifier">
                  <xsl:element name="premis:linkingObjectIdentifierType">
                    <xsl:value-of select="'UUID'" />
                  </xsl:element>
                  <xsl:element name="premis:linkingObjectIdentifierValue">
                    <xsl:value-of select="techMetadata/fields/file_uuid" />
                  </xsl:element>
                </xsl:element>
              </premis:event>
            </xsl:element>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END amdSec -->

      <!-- START fileSec -->
      <xsl:element name="mets:fileSec">
        <xsl:element name="mets:fileGrp">
          <xsl:element name="mets:file">
            <xsl:attribute name="ID">
              <xsl:value-of select="concat('Fil-', techMetadata/fields/file_uuid)" />
            </xsl:attribute>
            <xsl:element name="mets:FLocat">
              <xsl:attribute name="LOCTYPE">
                <xsl:value-of select="'URN'" />
              </xsl:attribute>
              <xsl:attribute name="xlink:href">
                <xsl:value-of select="concat('urn:uuid:', techMetadata/fields/file_uuid)" />
              </xsl:attribute>
            </xsl:element>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END fileSec -->

      <!-- START structMap -->
      <xsl:element name="mets:structMap">
        <xsl:attribute name="TYPE">
          <xsl:value-of select="'logical'" />
        </xsl:attribute>
        <xsl:element name="mets:div">
          <xsl:attribute name="ADMID">
            <xsl:value-of select="'ModsRights1 Premis1 PremisEvent1 PremisObject1'" />
          </xsl:attribute>
          <xsl:element name="mets:fptr">
            <xsl:attribute name="FILEID">
              <xsl:value-of select="concat('Fil-', techMetadata/fields/file_uuid)" />
            </xsl:attribute>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END structMap -->
      
    </mets:mets>
  </xsl:template>
</xsl:transform> 
