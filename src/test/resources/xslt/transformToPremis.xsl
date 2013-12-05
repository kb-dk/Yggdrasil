<?xml version="1.0" encoding="UTF-8"?> 
<xsl:stylesheet version="1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xlink="http://www.w3.org.1999/xlink"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:premis="info:lc/xmlns/premis-v2"
    
    extension-element-prefixes="java">

  <xsl:output encoding="UTF-8" method="xml" indent="yes" />

  <xsl:include href="transformToMix.xsl"/>
  
  <xsl:variable name="id" select="record/field[@name='objectIdentifierValue']/value"/>
  <xsl:variable name="PREMIS_LOCATION" select="'info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd'" />

  <xsl:template name="premis">
    <!-- Preservation level for bit safety. -->
    <premis:preservationLevel xsi:schemaLocation="{$PREMIS_LOCATION}">
      
      <!-- preservationLevelValue -->
      <xsl:element name="premis:preservationLevelValue">
        <xsl:value-of select="java:dk.kb.metadata.selector.PremisPreservationLevelEnumeratorSelector.getBitPreservationLevelValue(
                field[@name='preservationLevelValue_BitSafety']/value)" />
      </xsl:element>

      <!-- preservationLevelRationale -->
      <xsl:if test="field[@name='preservationLevelRationale_BitSafety']">
        <xsl:element name="premis:preservationLevelRationale">
          <xsl:value-of select="field[@name='preservationLevelRationale_BitSafety']/value" />
        </xsl:element>
      </xsl:if>
      
      <!-- preservationLevelDateAssigned -->
      <xsl:if test="field[@name='preservationLevelDateAssigned']">
        <xsl:element name="premis:preservationLevelDateAssigned">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:element>
      </xsl:if>
    </premis:preservationLevel>
    <!-- Preservation level for logical preservation. -->
    <premis:preservationLevel xsi:schemaLocation="{$PREMIS_LOCATION}">
      <!-- preservationLevelValue -->
      <xsl:element name="premis:preservationLevelValue">
        <xsl:value-of select="java:dk.kb.metadata.selector.PremisPreservationLevelEnumeratorSelector.getLogicalPreservationLevelValue(
                field[@name='preservationLevelValue_Logical']/value)" />
      </xsl:element>

      <!-- preservationLevelRationale -->
      <xsl:if test="field[@name='preservationLevelRationale_Logical']">
        <xsl:element name="premis:preservationLevelRationale">
          <xsl:value-of select="field[@name='preservationLevelRationale_Logical']/value" />
        </xsl:element>
      </xsl:if>

      <!-- preservationLevelDateAssigned -->
      <xsl:if test="field[@name='preservationLevelDateAssigned']">
        <xsl:element name="premis:preservationLevelDateAssigned">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:element>
      </xsl:if>
    </premis:preservationLevel>
    <!-- Preservation level for confidentiality. -->
    <premis:preservationLevel xsi:schemaLocation="{$PREMIS_LOCATION}">
      <!-- preservationLevelValue -->
      <xsl:element name="premis:preservationLevelValue">
        <xsl:value-of select="java:dk.kb.metadata.selector.PremisPreservationLevelEnumeratorSelector.getConfidentialityPreservationLevelValue(
                field[@name='preservationLevelValue_Confidentiality']/value)" />
      </xsl:element>
      
      <!-- preservationLevelRationale -->
      <xsl:if test="field[@name='preservationLevelRationale_Confidentiality']">
        <xsl:element name="premis:preservationLevelRationale">
          <xsl:value-of select="field[@name='preservationLevelRationale_Confidentiality']/value" />
        </xsl:element>
      </xsl:if>

      <!-- preservationLevelDateAssigned -->
      <xsl:if test="field[@name='preservationLevelDateAssigned']">
        <xsl:element name="premis:preservationLevelDateAssigned">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:element>
      </xsl:if>
    </premis:preservationLevel>
  </xsl:template>
  
  <xsl:template name="premis_event">
    <premis:event xsi:schemaLocation="{$PREMIS_LOCATION}">
      <!-- eventIdentifier -->
      <xsl:call-template name="premis_event_identifier" />
      
      <!-- eventType -->
      <xsl:call-template name="premis_event_eventtype" />
      
      <!-- date time -->
      <xsl:call-template name="premis_event_datetime" />
      
      <!-- linkingAgentIdentifier -->
      <xsl:call-template name="premis_event_linking_agent_identifier" />
      
      <!-- linkingObjectIdentifier -->
      <xsl:call-template name="premis_event_linking_object_identifier" />
    </premis:event>
  </xsl:template>

  <xsl:template name="premis_event_for_representation">
    <premis:event xsi:schemaLocation="{$PREMIS_LOCATION}">
      <!-- eventIdentifier -->
      <xsl:call-template name="premis_event_identifier" />
      <!-- eventType -->
      <xsl:call-template name="premis_event_eventtype" />
      <!-- date time -->
      <xsl:call-template name="premis_event_datetime" />
      <!-- linkingAgentIdentifier -->
      <xsl:call-template name="premis_event_linking_agent_identifier" />
    </premis:event>
  </xsl:template>
  
  <xsl:template name="premis_rights">
    <premis:rights xsi:schemaLocation="{$PREMIS_LOCATION}">
      <xsl:element name="premis:rightsStatement">
        <xsl:if test="field[@name='rightsStatementIdentifierValue']">
          <!-- rightsStatementIdentifier -->
          <xsl:element name="premis:rightsStatementIdentifier">
            <xsl:element name="premis:rightsStatementIdentifierType">
              <xsl:call-template name="premis_rights_identifier_type" />
            </xsl:element>
            <xsl:element name="premis:rightsStatementIdentifierValue">
              <xsl:call-template name="premis_rights_identifier_value" />
            </xsl:element>
          </xsl:element>
        
          <!-- Either private or public, depending of 'Published' being true. -->
          <xsl:element name="premis:rightsBasis">
            <xsl:choose>
              <xsl:when test="field[@name='Published']/value='true'">
                <xsl:value-of select="'public'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'private'" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:element>
        </xsl:if>
      </xsl:element>
    </premis:rights>
  </xsl:template>

  <xsl:template name="premis_object">
    <premis:object xsi:schemaLocation="{$PREMIS_LOCATION}">
      <xsl:attribute name="type" namespace="http://www.w3.org/2001/XMLSchema-instance">premis:file</xsl:attribute>
      <!-- START objectIdentifier -->
      <xsl:element name="premis:objectIdentifier">
        <xsl:element name="premis:objectIdentifierType">
          <xsl:call-template name="premis_object_identifier_type" />
        </xsl:element>
        <xsl:element name="premis:objectIdentifierValue">
          <xsl:call-template name="premis_object_identifier_value" />
        </xsl:element>
      </xsl:element>
      <!-- END objectIdentifier -->
      
      <!-- BEGIN SignificantProperties -->
      <xsl:if test="java:dk.kb.metadata.utils.FileFormatUtils.formatForMix(field[@name='formatName']/value)">
        <xsl:element name="premis:significantProperties">
          <xsl:element name="premis:significantPropertiesExtension">
            <xsl:call-template name="mix" />
          </xsl:element>
        </xsl:element>
      </xsl:if>
      <!-- END SignificantProperties -->
      
      <!-- BEGIN ObjectCharacteristics -->
      <xsl:element name="premis:objectCharacteristics">
        <xsl:element name="premis:compositionLevel">
          <xsl:if test="field[@name='compositionLevel']/value">
            <xsl:value-of select="field[@name='compositionLevel']/value" />
          </xsl:if>
        </xsl:element>
        
        <xsl:if test="field[@name='messageDigest']">
          <xsl:element name="premis:fixity">
            <xsl:element name="premis:messageDigestAlgorithm">
            <xsl:choose>
              <xsl:when test="field[@name='messageDigestAlgorithm']">
                <xsl:value-of select="field[@name='messageDigestAlgorithm']/value" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'MD5'" />
              </xsl:otherwise>
            </xsl:choose>
            </xsl:element>
            <xsl:element name="premis:messageDigest">
              <xsl:value-of select="field[@name='messageDigest']/value" />
            </xsl:element>
          </xsl:element>
        </xsl:if>
        
        <!-- size ||| File Data Size -->
        <xsl:choose>
          <xsl:when test="field[@name='size']">
            <xsl:element name="premis:size">
              <xsl:value-of select="field[@name='size']/value" />
            </xsl:element>
          </xsl:when>
          <xsl:when test="field[@name='File Data Size']">
            <xsl:element name="premis:size">
              <xsl:value-of select="field[@name='File Data Size']/value" />
            </xsl:element>
          </xsl:when>
        </xsl:choose>
        
        <xsl:choose>
          <xsl:when test="field[@name='formatName']/value">
            <xsl:element name="premis:format">
              <xsl:element name="premis:formatDesignation">
                <xsl:element name="premis:formatName">
                  <xsl:value-of select="field[@name='formatName']/value" />
                </xsl:element>
                <xsl:element name="premis:formatVersion">
                  <xsl:value-of select="field[@name='formatVersion']/value" />
                </xsl:element>
             </xsl:element>
            </xsl:element>
          </xsl:when>
          <xsl:otherwise>
            <xsl:if test="field[@name='objectCharacteristicsFormatName']/value">
              <xsl:element name="premis:format">
                <xsl:element name="premis:formatDesignation">
                  <xsl:element name="premis:formatName">
                    <xsl:value-of select="field[@name='objectCharacteristicsFormatName']/value" />
                  </xsl:element>
                  <xsl:for-each select="field[@name='objectCharacteristicsFormatVersion']/value">
                    <xsl:element name="premis:formatVersion">
                      <xsl:value-of select="." />
                    </xsl:element>
                  </xsl:for-each>
                </xsl:element>
              </xsl:element>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
      <!-- END ObjectCharacteristics -->
      
      <!-- START linkingIntellectualEntityIdentifier -->
      <xsl:if test="field[@name='linkingIntellectualEntityIdentifierValue']">
        <xsl:element name="premis:linkingIntellectualEntityIdentifier">
          <xsl:element name="premis:linkingIntellectualEntityIdentifierType">
            <xsl:value-of select="'UUID'" />
          </xsl:element>
          <xsl:element name="premis:linkingIntellectualEntityIdentifierValue">
            <xsl:value-of select="field[@name='linkingIntellectualEntityIdentifierValue']/value" />
          </xsl:element>
        </xsl:element>
      </xsl:if>
      <!-- END linkingIntellectualEntityIdentifier -->
      
      <!-- START linkingRightsStatementIdentifier -->
      <xsl:if test="field[@name='rightsStatementIdentifierValue']">
        <xsl:element name="premis:linkingRightsStatementIdentifier">
          <xsl:element name="premis:linkingRightsStatementIdentifierType">
            <xsl:call-template name="premis_rights_identifier_type" />
          </xsl:element>
          <xsl:element name="premis:linkingRightsStatementIdentifierValue">
            <xsl:call-template name="premis_rights_identifier_value" />
          </xsl:element>
        </xsl:element>
      </xsl:if>
      <!-- END linkingRightsStatementIdentifier -->
    </premis:object>
  </xsl:template>
  
  <xsl:template name="premis_event_identifier">
    <xsl:element name="premis:eventIdentifier">
      <xsl:element name="premis:eventIdentifierType">
        <xsl:call-template name="premis_event_identifier_type" />
      </xsl:element>
      <xsl:element name="premis:eventIdentifierValue">
         <xsl:call-template name="premis_event_identifier_value" />
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
  <xsl:template name="premis_event_eventtype">
    <xsl:element name="premis:eventType">
      <xsl:choose>
        <xsl:when test="field[@name='eventType']">
          <xsl:value-of select="field[@name='eventType']/value" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'ingestion'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>
  
  <xsl:template name="premis_event_datetime">
    <xsl:element name="premis:eventDateTime">
      <xsl:choose>
        <xsl:when test="field[@name='eventDateTime']">
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getDateTime(
              'EEE MMM dd HH:mm:ss z yyyy',field[@name='eventDateTime']/value)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>
  
  <xsl:template name="premis_event_linking_agent_identifier">
    <xsl:element name="premis:linkingAgentIdentifier">
      <xsl:element name="premis:linkingAgentIdentifierType">
        <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getIngestAgentType()" />
      </xsl:element>
      <xsl:element name="premis:linkingAgentIdentifierValue">
        <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getIngestAgentValue()" />
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
  <xsl:template name="premis_event_linking_object_identifier">
    <xsl:element name="premis:linkingObjectIdentifier">
      <xsl:element name="premis:linkingObjectIdentifierType">
        <xsl:call-template name="premis_object_identifier_type" />
      </xsl:element>
      <xsl:element name="premis:linkingObjectIdentifierValue">
        <xsl:call-template name="premis_object_identifier_value" />
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
  <xsl:template name="premis_object_identifier_type">
    <xsl:value-of select="'UUID'" />
  </xsl:template>
  
  <xsl:template name="premis_object_identifier_value">
    <xsl:value-of select="java:dk.kb.metadata.utils.GuidExtrationUtils.extractGuid(field[@name='GUID']/value)" />
  </xsl:template>

  <xsl:template name="premis_agent_identifier_type">
    <xsl:if test="field[@name='agentIdentifierType']">
      <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getAgentType(field[@name='agentIdentifierType']/value)" />
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="premis_agent_identifier_value">
    <xsl:if test="field[@name='agentIdentifierValue']">
      <xsl:value-of select="java:dk.kb.metadata.selector.AgentSelector.getAgentValue(field[@name='agentIdentifierValue']/value)" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="premis_event_identifier_type">
    <xsl:value-of select="'UUID'" />
  </xsl:template>
  
  <xsl:template name="premis_event_identifier_value">
    <xsl:value-of select="java:dk.kb.metadata.utils.IdentifierManager.getEventIdentifier($id)" />
  </xsl:template>  

  <xsl:template name="premis_rights_identifier_type">
    <xsl:value-of select="'UUID'" />
  </xsl:template>
  
  <xsl:template name="premis_rights_identifier_value">
    <xsl:value-of select="java:dk.kb.metadata.utils.GuidExtrationUtils.extractGuid(field[@name='rightsStatementIdentifierValue']/value)" />
  </xsl:template>  
</xsl:stylesheet> 
