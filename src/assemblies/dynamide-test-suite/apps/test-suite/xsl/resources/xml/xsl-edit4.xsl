<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:dynamideStringTools="http://www.dynamide.com/java/com.dynamide.util.StringTools"
     xmlns:dynamide="http://www.dynamide.com/java/com.dynamide.xsl.DynamideExtensionElementFactory"
     extension-element-prefixes="dynamide"
     >
  <xsl:output indent="no" omit-xml-declaration="yes" method="html"/>

  <xsl:template match="xsl:output">
  </xsl:template>

  <xsl:template match="/">
    <html>
      <body>
        <table border='1' cellpadding='0' cellspacing='0'>
            <xsl:apply-templates/>
        </table>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="xsl:stylesheet">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="xsl:template">
      <tr>
        <td>
        <nobr><b>match: "<xsl:value-of select="@match"/>"</b></nobr>
        </td>
        <td>
          <dynamide:template disable-output-escaping="yes">
          <![CDATA[
          #set $sctx = $TemplateState.getSaxonContext()
          ##set $body = $template.copy($sctx.getCurrentNodeInfo())
          #set $tm = $sctx.getContextNodeInfo().getAttributeValue("","match")
          ## this only works if it is a org.w3c.dom.Node - sctx: $sctx.getCurrentNode()
          ##sctx cn: $sctx.getContextNodeInfo().getDisplayName() $sctx.getContextNodeInfo().getAttributeValue("", "match")
          ##sctx ct: $sctx.getCurrentTemplate()
          ##SESSION: $session
          ## $session.queryParam("editTemplate")
          ## 3/6/2003 This works better than just outputChildren():
          #set $body = $template.escape($template.outputChildren($sctx.getCurrentNodeInfo()))
          #if ($tm == "/") {
            <a href='?SESSIONID=$session.SESSIONID&amp;editTemplate=$tm'>Edit</a>
              ## #set $body = $template.escape($template.outputChildren())
              <pre>$body</pre>
          } #else {
              ## #set $body = $template.outputChildren()
              #set $rows = $template.lineCount($body) + 2
              <textarea name='template_$' cols='80' rows='$rows'>$body</textarea>
          }
          ]]>
          </dynamide:template>
        </td>
      </tr>
  </xsl:template>

  <xsl:template match="*|@*|text()|comment()|processing-instruction()">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
