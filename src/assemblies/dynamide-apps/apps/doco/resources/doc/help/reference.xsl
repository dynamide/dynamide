<?xml version="1.0" encoding="utf-8"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">





<xsl:import href="doco.xsl"/>

<xsl:output indent="no" omit-xml-declaration="yes" method="xml"/>

<xsl:preserve-space elements="*"/>

<xsl:template match="detail">
<xsl:apply-templates/></xsl:template>


<xsl:template match="discussion">
        <b>Discussion</b>
        <blockquote>
<xsl:apply-templates/>
        </blockquote></xsl:template>
</xsl:stylesheet>