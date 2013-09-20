<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dynamide="http://www.dynamide.com/java/com.dynamide.xsl.DynamideExtensionElementFactory"
  extension-element-prefixes="dynamide"
  >

  <xsl:preserve-whitespace elements="*"/>

  <xsl:output method="xml" omit-xml-declaration="yes" indent="no" />

  <xsl:template match="/"  >
    <html>
      <dynamide:template language="WebMacro"><![CDATA[
      WebMacro here...<dm_nbsp/>
      #set $foo = bar
      mojo-$foo-mojo
      ]]></dynamide:template>
    </html>
  </xsl:template>

  <xsl:template match="name" >
     name:<xsl:value-of select="."/>:name
  </xsl:template>

</xsl:stylesheet>