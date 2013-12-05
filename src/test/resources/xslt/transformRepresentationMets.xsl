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

  <xsl:include href="transformToMods.xsl"/>
  <xsl:include href="transformToPremis.xsl"/>
 
  <xsl:variable name="MODS-ID" select="'Mods'" />
  <xsl:variable name="MODS-RIGHTS-ID" select="'ModsRights'" />
  <xsl:variable name="PREMIS-ID" select="'Premis'" />
  <xsl:variable name="PREMIS-AGENT-ID" select="'PremisAgent'" />
  <xsl:variable name="PREMIS-EVENT-ID" select="'PremisEvent'" />
  <xsl:variable name="PREMIS-OBJECT-ID" select="'PremisObject'" />
  <xsl:variable name="PREMIS-RIGHTS-ID" select="'PremisRights'" />

  <xsl:template match="record">
    <xsl:call-template name="mets_representation_generator" />
  </xsl:template>
  
  <xsl:template name="mets_representation_generator">
    <mets:mets xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version191/mets.xsd">
      <xsl:attribute name="TYPE">
        <xsl:value-of select="'Representation'" />
      </xsl:attribute>
      <xsl:attribute name="OBJID">
        <xsl:value-of select="java:dk.kb.metadata.utils.GuidExtrationUtils.extractGuid(field[@name='REPRESENTATION METADATA GUID']/value)" />
      </xsl:attribute>
      <xsl:attribute name="PROFILE">
        <xsl:value-of select="java:dk.kb.metadata.Constants.getProfileURL()" />
      </xsl:attribute>
            
      <!-- START metsHdr -->
      <xsl:element name="mets:metsHdr">
        <xsl:attribute name="CREATEDATE">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:attribute>

        <xsl:element name="mets:agent">
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getKbAgent()" />
          </xsl:attribute>
          <xsl:attribute name="ROLE"> 
            <xsl:value-of select="'CREATOR'" />
          </xsl:attribute>
          <xsl:attribute name="TYPE">
            <xsl:value-of select="'ORGANIZATION'" />
          </xsl:attribute>
          <xsl:element name="mets:name">
            <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getKbAgentValue()" />
          </xsl:element>
          <xsl:element name="mets:note">
            <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getKbAgentType()" />
          </xsl:element>
        </xsl:element>
        
        <xsl:element name="mets:agent">
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.metadata.Constants.getAPIAgent()" />
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
            <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getMdGenAgentValue()" />
          </xsl:element>
          <xsl:element name="mets:note">
            <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getMdGenAgentType()" />
          </xsl:element>
        </xsl:element>     
      </xsl:element>
      <!-- END metsHdr -->
            
      <!-- START dmdSec  -->
      <xsl:element name="mets:dmdSec">
        <xsl:attribute name="CREATED">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:attribute>    
        <xsl:attribute name="ID">
          <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.createNewMdId($MODS-ID)" />
        </xsl:attribute>
        <xsl:element name="mets:mdWrap">
          <xsl:attribute name="MDTYPE">
            <xsl:value-of select="'MODS'" />
          </xsl:attribute>
          <xsl:element name="mets:xmlData">
            <xsl:call-template name="mods_for_representation_mets" />
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END dmdSec -->
      
      <!-- START amdSec -->
      <xsl:element name="mets:amdSec">
        ADD MODS (rights)
        <xsl:element name="mets:rightsMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.createNewMdId($MODS-RIGHTS-ID)" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'MODS'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <xsl:call-template name="mods_rights" />
            </xsl:element>
          </xsl:element>
        </xsl:element>
        <!-- ADD PREMIS:RIGHTS -->
        <xsl:if test="field[@name='rightsStatementIdentifierValue']">
          <xsl:element name="mets:rightsMD">
            <xsl:attribute name="CREATED">
              <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
            </xsl:attribute>
            <xsl:attribute name="ID">
              <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.createNewMdId($PREMIS-RIGHTS-ID)" />
            </xsl:attribute>
            <xsl:element name="mets:mdWrap">
              <xsl:attribute name="MDTYPE">
                <xsl:value-of select="'PREMIS:RIGHTS'" />
              </xsl:attribute>
              <xsl:element name="mets:xmlData">
                <xsl:call-template name="premis_rights" />
              </xsl:element>
            </xsl:element>
          </xsl:element>
        </xsl:if>
        <!-- ADD PREMIS -->
        <xsl:element name="mets:digiprovMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.createNewMdId($PREMIS-ID)" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'PREMIS'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <xsl:call-template name="premis" />
            </xsl:element>
          </xsl:element>
        </xsl:element>
        <!-- ADD PREMIS:EVENT -->
        <xsl:element name="mets:digiprovMD">
          <xsl:attribute name="CREATED">
            <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
          </xsl:attribute>
          <xsl:attribute name="ID">
            <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.createNewMdId($PREMIS-EVENT-ID)" />
          </xsl:attribute>
          <xsl:element name="mets:mdWrap">
            <xsl:attribute name="MDTYPE">
              <xsl:value-of select="'PREMIS:EVENT'" />
            </xsl:attribute>
            <xsl:element name="mets:xmlData">
              <xsl:call-template name="premis_event_for_representation" />
            </xsl:element>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- END amdSec -->
      
      <!-- START structMap -->
      <xsl:element name="mets:structMap">
        <xsl:attribute name="TYPE">
          <xsl:value-of select="'logical'" />
        </xsl:attribute>
        
        <xsl:element name="mets:div">
          <xsl:attribute name="DMDID">
            <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.getDivAttributeFor($MODS-ID)" />
          </xsl:attribute>
          <xsl:attribute name="ADMID">
            <xsl:value-of select="java:dk.kb.metadata.utils.MdIdHandler.getDivAttributeFor(concat($MODS-RIGHTS-ID, ',', $PREMIS-ID, ',', $PREMIS-AGENT-ID, ',', $PREMIS-EVENT-ID, ',', $PREMIS-OBJECT-ID, ',', $PREMIS-RIGHTS-ID))" />
          </xsl:attribute>
          
          <xsl:call-template name="generate-mets-relationship" />
        </xsl:element>
      </xsl:element>
      <!-- END structMap -->
      
    </mets:mets>
  </xsl:template>
  
  <xsl:template name="generate-mets-relationship">
    <xsl:for-each select="field[@name='Related Sub Assets']/value/pack">
      <xsl:for-each select="@id">
        <xsl:call-template name="create-div" />
      </xsl:for-each>
      <xsl:for-each select="//value[@type='string']">
        <xsl:call-template name="create-div" />
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template name="create-div">
    <xsl:element name="mets:div">
      <xsl:attribute name="ORDER">
        <xsl:value-of select="java:dk.kb.metadata.representation.MetaGuidResolver.getOrder(.)" />
      </xsl:attribute>
      
      <xsl:element name="mets:mptr">
        <xsl:attribute name="LOCTYPE">
          <xsl:value-of select="'URN'" />
        </xsl:attribute>
        
        <xsl:attribute name="xlink:href">
          <xsl:value-of select="concat('urn:uuid:', java:dk.kb.metadata.representation.MetaGuidResolver.getGuid(.))" />
        </xsl:attribute>
      </xsl:element>
    </xsl:element>
  </xsl:template>
</xsl:transform> 
