<?xml version="1.0" encoding="utf-8"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">












<xsl:preserve-space elements="*"/>

<xsl:output indent="no" omit-xml-declaration="yes" method="xml"/>

<xsl:template match="exampleHTML">
        <blockquote>
        <table border="1" cellpadding="25" cellspacing="0" bgcolor="#DDDDDD">
          <tr>
            <td>
<xsl:apply-templates/>
            </td>
          </tr>
        </table>
        </blockquote></xsl:template>


<xsl:template match="example">
        <style>
        .nospace {font-size: 0.8em; margin:0; padding:0; border:0; }
        </style>

        <blockquote>
        <table border="1" cellpadding="5" cellspacing="0" bgcolor="#DDDDDD">
          <tr>
            <td>
             <pre class="nospace"><font color="brown"><xsl:value-of select="."/></font></pre>
            </td>
          </tr>
        </table>
        </blockquote></xsl:template>


<xsl:template match="definition">
         <p>
<xsl:apply-templates/>
         </p>
         <table border="0" cellpadding="0" cellspacing="20">
           <tr>
             <td height="5" width="5" bgcolor="green">
             </td>
             <td height="5" width="5" bgcolor="green">
             </td>
             <td height="5" width="5" bgcolor="green">
             </td>
             <td height="5" width="5" bgcolor="green">
             </td>
           </tr>
         </table></xsl:template>


<xsl:template match="definition/caption">
        <a>
            <xsl:attribute name="name"><xsl:value-of select="./@id"/></xsl:attribute>
        </a>
        <span class="definitionCaption">
<xsl:apply-templates/>
        </span></xsl:template>


<xsl:template match="definition/caption" mode="toc">
        <a>
            <xsl:attribute name="href">#<xsl:value-of select="./@id"/></xsl:attribute>
            <xsl:value-of select="."/>
        </a><br/></xsl:template>


<xsl:template match="definition/text" mode="toc"/>


<xsl:template match="definition/text">
        <div class="contentsItem">
<xsl:apply-templates/>
        </div></xsl:template>



<xsl:template match="*|@*|text()|comment()|processing-instruction()">
        <xsl:copy>
          <xsl:copy-of select="@*"/>
<xsl:apply-templates/>
        </xsl:copy></xsl:template>
</xsl:stylesheet>