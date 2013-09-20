package com.dynamide.xsl;

import com.icl.saxon.style.ExtensionElementFactory;

public class DynamideExtensionElementFactory
       implements ExtensionElementFactory  {

    /** This dog gets called by Saxon when it encounters an extension element, and it passes
     *  us the local name, e.g. for dynamide:foo, it passes "foo".
     */
    public Class getExtensionClass(java.lang.String localname){
        //System.err.println("getExtensionClass("+localname+")");
        if (localname.equalsIgnoreCase("template")){
            return com.dynamide.xsl.TemplateExtension.class;
        }
        return null;
    }

}
