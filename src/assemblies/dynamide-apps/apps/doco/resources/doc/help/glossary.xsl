<?xml version="1.0" encoding="utf-8"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">





<xsl:import href="doco.xsl"/>

<xsl:output indent="no" omit-xml-declaration="yes" method="xml"/>

<xsl:template match="/dynamideDoco">
            <link rel="stylesheet" SRC="/static/dynamide/doco/dynamidedoco.css" href="/static/dynamide/doco/dynamidedoco.css"/>
            <span class="FAQContentsHead">Contents</span>
            <div class="FAQList">
<xsl:apply-templates select="//definition[not(parent::section)]" mode="toc"><xsl:sort select="caption/@id"/></xsl:apply-templates>
<xsl:apply-templates select="//section" mode="toc"><xsl:sort select="@id"/></xsl:apply-templates>
            </div>

            <br/>


            <hr/>
<xsl:apply-templates select="//definition[not(parent::section)]"><xsl:sort select="caption/@id"/></xsl:apply-templates>
<xsl:apply-templates select="//section"><xsl:sort select="@id"/></xsl:apply-templates>
            <br/></xsl:template>


<xsl:template match="section" mode="toc">

            <span class="sectionHeadTOC">
            <a>
                <xsl:attribute name="class">sectionHeadTOC</xsl:attribute>
                <xsl:attribute name="href">#<xsl:value-of select="./@id"/></xsl:attribute>
                <xsl:value-of select="./@title"/>
            </a>
            </span><br/>
            <table border="0" cellpadding="0" cellspacing="0">
              <tr>
                <td width="20"><dm_nbsp/></td><td>
<xsl:apply-templates select=".//definition" mode="toc"><xsl:sort select="caption/@id"/></xsl:apply-templates>
                </td>
              </tr>
            </table></xsl:template>


<xsl:template match="section">
            <a><xsl:attribute name="name"><xsl:value-of select="./@id"/></xsl:attribute></a>
            <span class="sectionHead">
            <xsl:value-of select="./@title"/>
            </span><br/>
            <blockquote>
<xsl:apply-templates select=".//definition"><xsl:sort select="caption/@id"/></xsl:apply-templates>
            </blockquote>
            <a href="#top">FAQ Contents</a>
            <hr/></xsl:template>
</xsl:stylesheet>