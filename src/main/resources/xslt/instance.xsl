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
    <xsl:call-template name="instance_mets_generator" />
  </xsl:template>

  <xsl:template name="instance_mets_generator">
    <mets:mets xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version191/mets.xsd">
      <xsl:attribute name="TYPE">
        <xsl:value-of select="'Instance'" />
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

        <xsl:for-each select="field[@name='Department']">
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
        </xsl:for-each>
      </xsl:element>
      <!-- END metsHdr -->

      <!-- START dmdSec -->
      <xsl:element name="mets:dmdSec">
        <xsl:attribute name="CREATED">
          <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
        </xsl:attribute>    
        <xsl:attribute name="ID">
          <xsl:value-of select="'Mods1'" />
        </xsl:attribute>
        <xsl:element name="mets:mdWrap">
          <xsl:attribute name="MDTYPE">
            <xsl:value-of select="'MODS'" />
          </xsl:attribute>
          <xsl:element name="mets:xmlData">
            <xsl:copy-of select="mods:mods"></xsl:copy-of>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END dmdSec -->

      <!-- START amdSec -->
      <xsl:element name="mets:amdSec">
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
              <mods:mods xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-4.xsd" version="3.4">
                <xsl:element name="mods:accessCondition">
                  <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getModsAccessCondition()" />
                </xsl:element>
              </mods:mods>
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
                <xsl:element name="premis:eventType">
                  <xsl:value-of select="'ingestion'" />
                </xsl:element>
                <xsl:element name="premis:eventDateTime">
                  <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Dates.getCurrentDate()" />
                </xsl:element>
                <xsl:element name="premis:linkingAgentIdentifier">
                  <xsl:element name="premis:linkingAgentIdentifierType">
                    <xsl:value-of select="'URL'" />
                  </xsl:element>
                  <xsl:element name="premis:linkingAgentIdentifierValue">
                    <xsl:value-of select="java:dk.kb.yggdrasil.xslt.extension.Agent.getIngestAgentURL()" />
                  </xsl:element>
                  <xsl:element name="premis:linkingAgentRole">
                    <xsl:value-of select="'Ingest'" />
                  </xsl:element>
                </xsl:element>
                <xsl:element name="premis:linkingObjectIdentifier">
                  <xsl:element name="premis:linkingObjectIdentifierType">
                    <xsl:value-of select="'UUID'" />
                  </xsl:element>
                  <xsl:element name="premis:linkingObjectIdentifierValue">
                    <xsl:value-of select="provenanceMetadata/fields/uuid" />
                  </xsl:element>
                </xsl:element>
              </premis:event>
            </xsl:element>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END amdSec -->

      <!-- Produces invalid XML if no file is present. -->
      <xsl:if test="file">
      <!-- START structMap -->
        <xsl:element name="mets:structMap">
          <xsl:attribute name="TYPE">
            <xsl:value-of select="'logical'" />
          </xsl:attribute>
          <xsl:element name="mets:div">
            <xsl:attribute name="DMDID">
              <xsl:value-of select="'Mods1'" />
            </xsl:attribute>
            <xsl:attribute name="ADMID">
              <xsl:value-of select="'ModsRights1 Premis1 PremisEvent1'" />
            </xsl:attribute>

            <xsl:for-each  select="file">
              <xsl:element name="mets:div">
                <xsl:attribute name="LABEL">
                  <xsl:value-of select="name" />
                </xsl:attribute>
                <xsl:element name="mets:mptr">
                  <xsl:attribute name="LOCTYPE">
                    <xsl:value-of select="'URN'" />
                  </xsl:attribute>
                  <xsl:attribute name="xlink:href">
                    <xsl:value-of select="concat('urn:uuid:', uuid)" />
                  </xsl:attribute>
                </xsl:element>
              </xsl:element>
            </xsl:for-each>

          <!-- The ORDER part of the transformation has temporarily been commented out
          Remember to add the techMetadata to the test before creating the structMap.
            <xsl:if test="techMetadata">
              <xsl:for-each select="techMetadata/mets:mets/mets:structMap/mets:div">
                <xsl:element name="mets:div">
                  <xsl:attribute name="LABEL">
                    <xsl:value-of select="@ID" />
                  </xsl:attribute>
                  <xsl:attribute name="ORDER">
                    <xsl:value-of select="@ORDER" />
                  </xsl:attribute>
                  <xsl:element name="mets:mptr">
                    <xsl:attribute name="LOCTYPE">
                      <xsl:value-of select="'URN'" />
                    </xsl:attribute>
                    <xsl:attribute name="xlink:href">
                      <xsl:value-of select="concat('urn:uuid:', mets:fptr/@FILEID)" />
                    </xsl:attribute>
                  </xsl:element>
                </xsl:element>
              </xsl:for-each>
            </xsl:if>
            -->

          </xsl:element>
        </xsl:element>
        <!-- END structMap -->
      </xsl:if>

    </mets:mets>
  </xsl:template>
</xsl:transform>
