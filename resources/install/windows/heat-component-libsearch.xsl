<?xml version="1.0" ?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://schemas.microsoft.com/wix/2006/wi"
>
    <xsl:output method="text" version="1.0" />
    <xsl:strip-space elements="*"/>
    <xsl:template match="*//*[local-name()='Component' and @Id='jitsi_defaults.properties']">
        <xsl:text>wix.heat.jitsi.lib.dir=</xsl:text><xsl:value-of select="../@Id"/>
    </xsl:template>
</xsl:stylesheet>
