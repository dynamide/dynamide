<?xml version="1.0" encoding="utf-8"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">













<xsl:include href="doco.xsl"/>

<xsl:preserve-space elements="*"/>

<xsl:output indent="no" omit-xml-declaration="yes" method="xml"/>

<xsl:template match="/dynamideDoco">
        <!--link rel="stylesheet"  SRC="/com/dynamide/doc/dynamidedoco.css" href="/com/dynamide/doc/dynamidedoco.css"></link-->
<xsl:apply-templates/></xsl:template>
</xsl:stylesheet>