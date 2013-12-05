<?xml version="1.0" encoding="UTF-8"?> 
<xsl:stylesheet version="1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xlink="http://www.w3.org.1999/xlink"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:mix="http://www.loc.gov/mix/v20"
    
    extension-element-prefixes="java">

  <xsl:output encoding="UTF-8" method="xml" indent="yes" />

  <xsl:template name="mix">
    <mix:mix xsi:schemaLocation="http://www.loc.gov/mix/v20 http://www.loc.gov/standards/mix/mix20/mix20.xsd">
        
      <!-- Start mix/BasicDigitalObjectInformation NDD id="6" -->
      <xsl:element name="mix:BasicDigitalObjectInformation">
      
        <!-- mix/BasicDigitalObjectInformation/objectIdentifier NDD id="6.1" -->
        <xsl:if test="field[@name='objectIdentifierValue']">
          <xsl:element name="mix:ObjectIdentifier">
            <!-- objectIdentifierType NDD id="6.1.1" -->
            <xsl:element name="mix:objectIdentifierType">
              <xsl:value-of select="'UUID'" />
            </xsl:element>
            <!-- objectIdentifierValue NDD id="6.1.2" -->
            <xsl:element name="mix:objectIdentifierValue">
              <xsl:value-of select="java:dk.kb.metadata.utils.GuidExtrationUtils.extractGuid(field[@name='GUID']/value)" />
            </xsl:element>
          </xsl:element>
        </xsl:if>
        
        <!-- mix/BasicDigitalObjectInformation/fileSize NDD id="6.2" -->
        <xsl:choose>
          <xsl:when test="field[@name='size']">
            <xsl:element name="mix:fileSize">
              <xsl:value-of select="field[@name='size']/value" />
            </xsl:element>
          </xsl:when>
          <xsl:when test="field[@name='File Data Size']">
            <xsl:element name="mix:fileSize">
              <xsl:value-of select="field[@name='File Data Size']/value" />
            </xsl:element>
          </xsl:when>
        </xsl:choose>
        
        <!-- mix/BasicDigitalObjectInformation/FormatDesignation NDD id="6.3" -->
        <xsl:choose>
          <xsl:when test="field[@name='formatName']/value">
            <xsl:element name="mix:FormatDesignation">
              <!-- formatName NDD id="6.3.1" -->
              <xsl:element name="mix:formatName">
                <xsl:value-of select="field[@name='formatName']/value" />
              </xsl:element>
              <!-- formatVersion NDD id="6.3.2" -->
              <xsl:element name="mix:formatVersion">
                <xsl:value-of select="field[@name='formatVersion']/value" />
              </xsl:element>
            </xsl:element>
          </xsl:when>
          <xsl:when test="field[@name='objectCharacteristicsFormatName']">
            <xsl:element name="mix:FormatDesignation">
              <!-- formatName NDD id="6.3.1" -->
              <xsl:element name="mix:formatName">
                <xsl:value-of select="field[@name='objectCharacteristicsFormatName']/value" />
              </xsl:element>
              <!-- formatVersion NDD id="6.3.2" -->
              <xsl:element name="mix:formatVersion">
                <xsl:value-of select="field[@name='objectCharacteristicsFormatVersion']/value" />
              </xsl:element>
            </xsl:element>
          </xsl:when>
        </xsl:choose>
        
        <!-- mix/BasicDigitalObjectInformation/byteOrder NDD id="6.5" -->
        <xsl:element name="mix:byteOrder">
          <xsl:value-of select="'big endian'" />
        </xsl:element>
        
        <!-- mix/BasicDigitalObjectInformation/Compression NDD id="6.6" -->
        <xsl:for-each select="field[@name='Compression']/value">
          <xsl:element name="mix:Compression">
            <!-- compressionScheme NDD id="6.6.1" -->
            <xsl:element name="mix:compressionScheme">
              <xsl:value-of select="." />
            </xsl:element>
          </xsl:element>
        </xsl:for-each>

        <!-- mix/BasicDigitalObjectInformation/Fixity NDD id="6.7" -->
        <xsl:choose>
          <xsl:when test="field[@name='messageDigest']">
            <xsl:element name="mix:Fixity">
              <!-- messageDigestAlgorithm NDD id="6.7.1" -->
              <xsl:element name="mix:messageDigestAlgorithm">
                <xsl:value-of select="field[@name='messageDigestAlgorithm']/value" />
              </xsl:element>
              <!-- messageDigest NDD id="6.7.2" -->
              <xsl:element name="mix:messageDigest">
                <xsl:value-of select="field[@name='messageDigest']/value" />
              </xsl:element>            
            </xsl:element>            
          </xsl:when>
          <xsl:when test="field[@name='CHECKSUM_ORIGINAL_MASTER']">
            <xsl:element name="mix:Fixity">
              <!-- messageDigestAlgorithm NDD id="6.7.1" -->
              <xsl:element name="mix:messageDigestAlgorithm">
                <xsl:value-of select="'MD5'" />
              </xsl:element>
              <!-- messageDigest NDD id="6.7.2" -->
              <xsl:element name="mix:messageDigest">
                <xsl:value-of select="field[@name='CHECKSUM_ORIGINAL_MASTER']/value" />
              </xsl:element>            
            </xsl:element>            
          </xsl:when>
        </xsl:choose>
      </xsl:element>
      <!-- End mix/BasicDigitalObjectInformation NDD id="6" -->
      
      <!-- Begin mix/BasicImageInformation NDD id="7" -->
      <xsl:element name="mix:BasicImageInformation">
      
        <!-- mix/BasicImageInformation/BasicImageCharacteristics NDD id="7.1" -->
        <xsl:element name="mix:BasicImageCharacteristics">
        
          <!-- imageWidth NDD id="7.1.1" -->
          <xsl:for-each select="field[@name='Horizontal Pixels']/value">
            <xsl:element name="mix:imageWidth">
              <xsl:value-of select="." />
            </xsl:element>
          </xsl:for-each>
          <!-- imageHeight NDD id="7.1.2" -->
          <xsl:for-each select="field[@name='Vertical Pixels']/value">
            <xsl:element name="mix:imageHeight">
              <xsl:value-of select="." />
            </xsl:element>
          </xsl:for-each>
          
          <!-- PhotometricInterpretation NDD id="7.1.3" -->
          <xsl:element name="mix:PhotometricInterpretation">
            <!-- colorSpace NDD id="7.1.3.1" -->
            <xsl:for-each select="field[@name='Color Model']/value">
              <xsl:element name="mix:colorSpace">
                <xsl:value-of select="java:dk.kb.metadata.selector.MixEnumeratorSelector.colorSpace(.)" />
              </xsl:element>
            </xsl:for-each>
            <!-- ColorProfile NDD id="7.1.3.2" -->
            <xsl:for-each select="field[@name='ICC Profile Name']/value">
              <xsl:element name="mix:ColorProfile">
                <!-- IccProfile NDD id="7.1.3.2.1" -->
                <xsl:element name="mix:IccProfile">
                  <!-- iccProfileName NDD id="7.1.3.2.1.1" -->
                  <xsl:element name="mix:iccProfileName">
                    <xsl:value-of select="." />
                  </xsl:element>
                </xsl:element>
              </xsl:element>
            </xsl:for-each>
          </xsl:element>
        </xsl:element>
      </xsl:element>
      <!-- End mix/BasicImageInformation NDD id="7" -->
      
      <!-- Start mix/ImageCaptureMetadata NDD id="8" -->
      <xsl:element name="mix:ImageCaptureMetadata">
      
        <!-- Start mix/ImageCaptureMetadata/SourceInformation NDD id="8.1" -->
        <xsl:if test="field[@name='@Image Width' or @name='@Image height']">
          <xsl:element name="mix:SourceInformation">
            <!-- SourceSize NDD id="8.1.3" -->
            <xsl:element name="mix:SourceSize">
              <!-- SourceXDimension NDD id="8.1.3.1" -->
              <xsl:for-each select="field[@name='@Image Width']">
                <xsl:element name="mix:SourceXDimension">
                  <!-- sourceXDimensionValue NDD id="8.1.3.1.1" -->
                  <xsl:element name="mix:sourceXDimensionValue">
                    <xsl:value-of select="value" />
                  </xsl:element>
                  <!-- sourceXDimensionUnit NDD id="8.1.3.1.2" -->
                  <xsl:element name="mix:sourceXDimensionUnit">
                    <xsl:value-of select="'in.'" />
                  </xsl:element>
                </xsl:element>
              </xsl:for-each>
            
              <!-- SourceYDimension NDD id="8.1.3.2" -->
              <xsl:for-each select="field[@name='@Image Height']">
                <xsl:element name="mix:SourceYDimension">
                  <!-- sourceYDimensionValue NDD id="8.1.3.1.1" -->
                  <xsl:element name="mix:sourceYDimensionValue">
                    <xsl:value-of select="value" />
                  </xsl:element>
                  <!-- sourceYDimensionUnit NDD id="8.1.3.1.2" -->
                  <xsl:element name="mix:sourceYDimensionUnit">
                    <xsl:value-of select="'in.'" />
                  </xsl:element>
                </xsl:element>
              </xsl:for-each>
            </xsl:element>
        
          </xsl:element>
        </xsl:if>
        <!-- End mix/ImageCaptureMetadata/SourceInformation NDD id="8.1" -->
        
        <!-- Start mix/ImageCaptureMetadata/GeneralCaptureInformation NDD id="8.2" -->
        <xsl:element name="mix:GeneralCaptureInformation">
          <!-- dateTimeCreated NDD id="8.2.1" -->
          <xsl:element name="mix:dateTimeCreated">
            <xsl:choose>
              <xsl:when test="field[@name='Captured Date']">
                <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getDateTime(
                        'EEE MMM dd HH:mm:ss z yyyy', field[@name='Captured Date']/value)" />
              </xsl:when>
              <xsl:when test="field[@name='Date Time Digitized']">
                <xsl:value-of select="field[@name='Date Time Digitized']/value" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getCurrentDate()" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:element>
          <!-- imageProducer NDD id="8.2.2" -->
          <xsl:if test="field[@name='Creator'] or field[@name='Byline'] or 
          field[@name='Owner'] or field[@name='Source']">
            <xsl:element name="mix:imageProducer">
              <xsl:choose>
                <xsl:when test="field[@name='Creator']">
                  <xsl:value-of select="field[@name='Creator']/value" />
                </xsl:when>
                <xsl:when test="field[@name='Byline']">
                  <xsl:value-of select="field[@name='Byline']/value" />
                </xsl:when>
                <xsl:when test="field[@name='Owner']">
                  <xsl:value-of select="field[@name='Owner']/value" />
                </xsl:when>
                <xsl:when test="field[@name='Source']">
                  <xsl:value-of select="field[@name='Source']/value" />
                </xsl:when>
              </xsl:choose>
            </xsl:element>
          </xsl:if>
        </xsl:element>
        <!-- End mix/ImageCaptureMetadata/GeneralCaptureInformation NDD id="8.2" -->

        <!-- Start mix/ImageCaptureMetadata/ScannerCapture NDD id="8.3" -->
        <xsl:element name="mix:ScannerCapture">
          <!-- ScannerModel NDD id="8.2" -->
          <xsl:if test="field[@name='Scanner Model']">
            <xsl:element name="mix:ScannerModel">
              <!-- scannerModelName NDD id="8.2.1" -->
                <xsl:element name="mix:scannerModelName">
                  <xsl:value-of select="field[@name='Scanner Model']/value" />
                </xsl:element>
            </xsl:element>
          </xsl:if>
          
          <!-- MaximumOpticalResolution NDD id="8.3.3" -->
          <xsl:element name="mix:MaximumOpticalResolution">
            <xsl:for-each select="field[@name='Horizontal Resolution']">
              <xsl:element name="mix:xOpticalResolution">
                <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.extractIntegerFromDouble(value)" />
              </xsl:element>
            </xsl:for-each>
            <xsl:for-each select="field[@name='Vertical Resolution']">
              <xsl:element name="mix:yOpticalResolution">
                <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.extractIntegerFromDouble(value)" />
              </xsl:element>
            </xsl:for-each>
          </xsl:element>
          
          <!-- ScanningSystemSoftware NDD id="8.3.5" -->
          <xsl:element name="mix:ScanningSystemSoftware">
            <xsl:for-each select="field[@name='creatingApplication']/value">
              <!-- scanningeSoftwareName NDD id="8.3.5.1" -->
              <xsl:element name="mix:scanningSoftwareName">
                <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnComma(., 0)" />
              </xsl:element>
              <!-- scanningSoftwareVersion NDD id="8.3.5.2" -->
              <xsl:if test="java:dk.kb.metadata.utils.StringUtils.splitableOnComma(.)">
                <xsl:element name="mix:scanningSoftwareVersionNo">
                  <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnComma(., 1)" />
                </xsl:element>
              </xsl:if>
            </xsl:for-each>
          </xsl:element>
        </xsl:element>
        <!-- End mix/ImageCaptureMetadata/ScannerCapture -->

        <!-- Start mix/ImageCaptureMetadata/DigitalCameraCapture NDD id="8.4" -->
        <xsl:if test="field[@name='Camera Manufacturer'] or field[@name='Camera Model']">
          <xsl:element name="mix:DigitalCameraCapture">
          
            <!-- digitalCameraManufacturer NDD id="8.4.1" -->
            <xsl:for-each select="field[@name='Camera Manufacturer']/value">
              <xsl:element name="mix:digitalCameraManufacturer">
                <xsl:value-of select="." />
              </xsl:element>
            </xsl:for-each>
            
            <!-- DigitalCameraModel NDD id="8.4.2" -->
            <xsl:for-each select="field[@name='Camera Model']/value">
              <xsl:element name="mix:DigitalCameraModel">
                <!-- digitalCameraModelName NDD id="8.4.2.1" -->
                <xsl:element name="mix:digitalCameraModelName">
                  <xsl:value-of select="." />
                </xsl:element>
              </xsl:element>
            </xsl:for-each>
          
            <!-- NDD id="8.4.4" -->
            <xsl:element name="mix:CameraCaptureSettings">
              <!-- NDD id="8.4.4.1" -->
              <xsl:element name="mix:ImageData">
              
                <!-- F Number time NDD id="8.4.4.1.1" -->
                <xsl:choose>
                  <xsl:when test="field[@name='F Number (String)']">
                    <xsl:element name="mix:fNumber">
                      <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.calculateFraction(field[@name='F Number (String)']/value)" />
                    </xsl:element>
                  </xsl:when>
                  <xsl:when test="field[@name='F Number']">
                    <xsl:element name="mix:fNumber">
                      <xsl:value-of select="field[@name='F Number']/value" />
                    </xsl:element>
                  </xsl:when>
                </xsl:choose>
  
                <!-- Exposure time NDD id="8.4.4.1.2" -->
                <xsl:choose>
                  <xsl:when test="field[@name='Exposure Time (String)']">
                    <xsl:element name="mix:exposureTime">
                      <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.calculateFraction(field[@name='Exposure Time (String)']/value)" />
                    </xsl:element>
                  </xsl:when>
                  <xsl:when test="field[@name='Exposure Time']">
                      <xsl:element name="mix:exposureTime">
                      <xsl:value-of select="field[@name='Exposure Time']/value" />
                    </xsl:element>
                  </xsl:when>
                </xsl:choose>
                
                <!-- iso speed ratings NDD id="8.4.4.1.5" -->
                <xsl:for-each select="field[@name='@ISO Speed']/value">
                  <xsl:element name="mix:isoSpeedRatings">
                    <xsl:value-of select="." />
                  </xsl:element>
                </xsl:for-each>
                
                <!-- Exif version NDD id="8.4.4.1.7" -->
                <xsl:for-each select="field[@name='EXIF Version']/value">
                  <xsl:element name="mix:exifVersion">
                    <xsl:value-of select="java:dk.kb.metadata.selector.MixEnumeratorSelector.exifVersion(.)" />
                  </xsl:element>
                </xsl:for-each>
  
                <!-- shutterSpeedValue NDD id="8.4.4.1.5" -->
                <xsl:if test="field[@name='Shutter Time [s]']">
                  <xsl:element name="mix:shutterSpeedValue">
                    <xsl:element name="mix:numerator">
                      <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnSlash(field[@name='Shutter Time [s]']/value, 0)" />
                    </xsl:element>
                    <xsl:element name="mix:denominator">
                      <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnSlash(field[@name='Shutter Time [s]']/value, 1)" />
                    </xsl:element>
                  </xsl:element>
                </xsl:if>
                
                <!-- Aperture NDD id="8.4.4.1.9" -->
                <xsl:choose>
                  <xsl:when test="field[@name='Max Aperture (String)']/value">
                    <xsl:element name="mix:apertureValue">
                      <xsl:element name="mix:numerator">
                        <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnSlash(field[@name='Max Aperture (String)']/value, 0)" />
                      </xsl:element>
                      <xsl:element name="mix:denominator">
                        <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnSlash(field[@name='Max Aperture (String)']/value, 1)" />
                      </xsl:element>
                    </xsl:element>
                  </xsl:when>
                  <xsl:when test="field[@name='Aperture']/value">
                    <xsl:element name="mix:apertureValue">
                      <xsl:element name="mix:numerator">
                        <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.retrieveNominatorAsInteger(field[@name='Aperture']/value)" />
                      </xsl:element>
                      <xsl:element name="mix:denominator">
                        <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.retrieveDenominatorAsInteger(field[@name='Aperture']/value)" />
                      </xsl:element>
                    </xsl:element>
                  </xsl:when>
                </xsl:choose>
                
                <!-- meteringMode NDD id="8.4.4.1.14" -->
                <xsl:if test="java:dk.kb.metadata.selector.MixEnumeratorSelector.validMeteringMode(field[@name='Meter Mode']/value)">
                  <xsl:element name="mix:meteringMode">
                    <xsl:value-of select="java:dk.kb.metadata.selector.MixEnumeratorSelector.meteringMode(field[@name='Meter Mode']/value)" />
                  </xsl:element>
                </xsl:if>
                
                <!-- focalLenght NDD id="8.4.4.1.17" -->
                <xsl:if test="field[@name='Focal Length [mm]']">
                  <xsl:element name="mix:focalLength">
                    <xsl:value-of select="field[@name='Focal Length [mm]']/value" />
                  </xsl:element>
                </xsl:if>
                              
              </xsl:element>
            </xsl:element>
          </xsl:element>
        </xsl:if>
        <!-- End mix/ImageCaptureMetadata/DigitalCameraCapture NDD id="8.4" -->

        <!-- mix/ImageCaptureMetadata/orientation NDD id="8.5" -->
        <xsl:if test="field[@name='Image Orientation']">
          <xsl:element name="mix:orientation">
            <xsl:value-of select="java:dk.kb.metadata.selector.MixEnumeratorSelector.orientation(field[@name='Image Orientation']/value)" />
          </xsl:element>
        </xsl:if>        
      </xsl:element>
      <!-- End mix/ImageCaptureMetadata NDD id="8" -->
        
      <!-- Begin mix/ImageAssessmentMetadata NDD id="9" -->
      <xsl:element name="mix:ImageAssessmentMetadata">
        <!-- NDD id="9.2" -->
        <xsl:element name="mix:ImageColorEncoding">
          <!-- NDD id="9.2.1" -->
          <xsl:element name="mix:BitsPerSample">
            <xsl:for-each select="field[@name='Bits Per Channel']/value">
              <!-- NDD id="9.2.1.1" -->
              <xsl:element name="mix:bitsPerSampleValue">
                <xsl:value-of select="." />
              </xsl:element>
              <!-- NDD id="9.2.1.2" -->
              <xsl:element name="mix:bitsPerSampleUnit">
                <xsl:value-of select="'integer'" />
              </xsl:element>
            </xsl:for-each>
          </xsl:element>
          <!-- NDD id="9.2.2" -->
          <xsl:if test="field[@name='Color Channels']">
            <xsl:element name="mix:samplesPerPixel">
              <xsl:value-of select="field[@name='Color Channels']/value" />
            </xsl:element>
          </xsl:if>
        </xsl:element>
      </xsl:element>
      <!-- End mix/ImageAssessmentMetadata NDD id="9" -->
      
      <!-- Start mix/ChangeHistory NDD id="10" -->
      <xsl:element name="mix:ChangeHistory">
        <!-- NDD id="10.1" -->
        <xsl:element name="mix:ImageProcessing">
          <!-- NDD id="10.1.1" -->
          <xsl:if test="field[@name='Date Time Original']">
            <xsl:element name="mix:dateTimeProcessed">
              <xsl:value-of select="java:dk.kb.metadata.utils.CalendarUtils.getDateTime(
                'EEE MMM dd HH:mm:ss z yyyy',field[@name='Date Time Original']/value)" />
            </xsl:element>
          </xsl:if>
          <!-- NDD id="10.1.2" -->
          <xsl:element name="mix:sourceData">
            <xsl:value-of select="'initial capture'" />
          </xsl:element>
          <!-- NDD id="10.1.5" -->
          <xsl:for-each select="field[@name='Software']/value">
            <xsl:element name="mix:ProcessingSoftware">
              <xsl:element name="mix:processingSoftwareName">
                <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnComma(., 0)" />
              </xsl:element>
              <xsl:if test="java:dk.kb.metadata.utils.StringUtils.splitableOnComma(.)">
                <xsl:element name="mix:processingSoftwareVersion">
                  <xsl:value-of select="java:dk.kb.metadata.utils.StringUtils.splitOnComma(., 1)" />
                </xsl:element>
              </xsl:if>
            </xsl:element>
          </xsl:for-each>
        </xsl:element>
      </xsl:element>
      <!-- End mix/ChangeHistory NDD id="10" -->
    </mix:mix>
  </xsl:template>
</xsl:stylesheet> 
