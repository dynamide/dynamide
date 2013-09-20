package com.dynamide.xsl;


/** From the SAXON doco:
 *
 *
 <table border='1'>
        <tr><td valign=top width="30%">prepareAttributes()</td>
           <td>This is called while the stylesheet tree is still being built, so it should not attempt
           to navigate the tree. Its task is to validate the attributes of the stylesheet element and
           perform any preprocessing necessary. For example, if the attribute is an attribute value template,
           this includes creating an Expression that can subsequently be evaluated to get the AVT's
           value.</td></tr>
        <tr><td valign=top>validate()</td>
           <td>This is called once the tree has been built, and its task is to check that the stylesheet
           element appears in the right context within the tree, e.g. that it is within a template</td></tr>
        <tr><td valign=top>process()</td>
           <td>This is called to process a particular node in the source document, which can be accessed
           by reference to the Context supplied as a parameter.</td></tr>
        <tr><td valign=top>isInstruction()</td>
           <td>This should return true, to ensure that the element is allowed to appear
           within a template body.</td></tr>
        <tr><td valign=top>mayContainTemplateBody(()</td>
           <td>This should return true, to ensure that the element can contain instructions.
           Even if it can't contain anything else, extension elements should allow an xsl:fallback
           instruction to provide portability between processors</td></tr>
    </table>
  */
public class WMTemplateExtension
       extends com.icl.saxon.style.StyleElement {

    public void prepareAttributes(){
    }

    public void validate(){
    }

    public void process(com.icl.saxon.Context saxonContext)
    throws javax.xml.transform.TransformerException {
        com.icl.saxon.output.Outputter outputter = saxonContext.getOutputter();
        outputter.write("START=>");
        //saxonContext.getCurrentNodeInfo().copy(outputter); //this dumps the *xml* document to the output stream.
        //saxonContext.getContextNodeInfo().copy(outputter);   //this dumps the *xml* document element
        //saxonContext.getCurrentTemplate().copy(outputter);   //this dumps the *xsl* document element for the template
        outputter.write(getValue());
        outputter.write("<=DONE");
    }

    public boolean isInstruction(){
        return false;
    }

    public boolean mayContainTemplateBody(){
        return true;
    }




}
