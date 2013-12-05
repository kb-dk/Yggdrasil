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
  
  <xsl:variable name="FILE_GUID" select="java:dk.kb.metadata.utils.GuidExtrationUtils.extractGuid(record/field[@name='GUID']/value)" />
  
  <xsl:template match="record">
    <xsl:call-template name="mets_representation_relationship" />
  </xsl:template>
  
  <xsl:template name="mets_representation_relationship">
    <!-- Inserting the file id and file guid into the MetaGuidResolver for potential use of the representation metadata. -->
    <xsl:value-of select="java:dk.kb.metadata.representation.MetaGuidResolver.insertEntry(
        field[@name='Record Name']/value, $FILE_GUID)" />
  
<!--     <xsl:if test="field[@name='Related Sub Assets']/value"> -->
<!--       <xsl:value-of select="java:dk.kb.metadata.representation.RelationshipHandler.setCurrentMaster( -->
<!--         field[@name='Related Sub Assets']/value/pack/@id)" /> -->
<!--       <xsl:for-each select="//value[@type='string']"> -->
<!--         <xsl:value-of select="java:dk.kb.metadata.representation.RelationshipHandler.addAssetToCurrentMaster(.)" /> -->
<!--       </xsl:for-each> -->
<!--     </xsl:if> -->
  </xsl:template>
</xsl:transform> 
