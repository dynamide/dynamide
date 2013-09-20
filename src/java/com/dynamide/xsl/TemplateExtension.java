package com.dynamide.xsl;

import java.util.*;

import com.icl.saxon.Controller;
import com.icl.saxon.expr.Expression;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.output.Outputter;
import org.webmacro.Context;

import org.jdom.Element;

import com.dynamide.JDOMFile;

import com.dynamide.util.Log;
import com.dynamide.util.WebMacroTools;

import com.dynamide.xsl.DynamideSaxonController;

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
public class TemplateExtension
       extends com.icl.saxon.style.StyleElement {

    public void prepareAttributes(){
    }

    public void validate(){
    }

    public void process(com.icl.saxon.Context saxonContext)
    throws javax.xml.transform.TransformerException {
        boolean debug = false;
        String language = getAttributeValue("language");

        String match = saxonContext.getCurrentTemplate().getAttributeValue("match");


        com.icl.saxon.output.Outputter outputter = saxonContext.getOutputter();

        if (debug) outputter.write("START (match='"+match+"', template language='"+language+"')=>");

        //saxonContext.getCurrentNodeInfo().copy(outputter); //this dumps the *xml* document to the output stream.
        //saxonContext.getContextNodeInfo().copy(outputter);   //this dumps the *xml* document element
        //saxonContext.getCurrentTemplate().copy(outputter);   //this dumps the *xsl* document element for the template
        String template = getValue();

        try {
            String res;
            WebMacroTools wmt = new WebMacroTools("", "", false);
            Context c = wmt.getContext();
            try {
                TemplateState templateState = new TemplateState(saxonContext, c, match);
                c.put("TemplateState", templateState);
                c.put("template", templateState);
                c.put("match", match);
                c.put("context", c);
                DynamideSaxonController dmController = (DynamideSaxonController)saxonContext.getController();
                c.put("session", dmController.getSession());

                //test(saxonContext);
                res = wmt.expand(c, template);
                //System.out.println("webmacro-out{"+res+"}webmacro-out");
                outputter.write(res);
            } finally {
                wmt.releaseContext(c);
            }
        } catch (Exception e){
            Log.error(TemplateExtension.class, "Caught exception in process()", e);
            //throw new javax.xml.transform.TransformerException(e);
        }

        if (debug) outputter.write("<=DONE(match='"+match+"')");
    }

    private void test(com.icl.saxon.Context saxonContext)
    throws javax.xml.transform.TransformerException {
            Controller controller = saxonContext.getController();
            Outputter outputter = controller.getOutputter();
            java.io.StringWriter writer1 = new java.io.StringWriter();
            java.io.StringWriter writer2 = new java.io.StringWriter();
            com.icl.saxon.output.Emitter emitter = outputter.getEmitter();

            emitter.setWriter(writer1);
            outputter.write("hello1\r\n");

            emitter.setWriter(writer2);
            outputter.write("hello2\r\n");

            emitter.setWriter(writer1);
            outputter.write("hello1,again\r\n");
            System.out.println("writer1: "+writer1);
            System.out.println("writer2: "+writer2);
    }

    public boolean isInstruction(){
        return false;
    }

    public boolean mayContainTemplateBody(){
        return true;
    }

    public class TemplateState {
        public TemplateState(com.icl.saxon.Context context, org.webmacro.Context wmContext, String match){
            saxonContext = context;
            m_webMacroContext = wmContext;
            m_match = match;
        }

        private com.icl.saxon.Context saxonContext;
        public com.icl.saxon.Context getSaxonContext(){
            return saxonContext;
        }

        private org.webmacro.Context m_webMacroContext = null;
        public org.webmacro.Context getWebMacroContext(){return m_webMacroContext;}

        private com.icl.saxon.ParameterSet params = null;

        private String m_match = "";

        public com.icl.saxon.output.Outputter makeOutputter()
        throws javax.xml.transform.TransformerException {
            try {
            java.io.StringWriter writer = new java.io.StringWriter();
            com.icl.saxon.output.GeneralOutputter outputter = new com.icl.saxon.output.GeneralOutputter(new com.icl.saxon.om.NamePool());
   //         com.icl.saxon.output.Emitter emitter = outputter.getEmitter();
   //         emitter.setWriter(writer);

            java.util.Properties props = new java.util.Properties();//outputter.getOutputProperties();
            javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult(writer);
            System.out.println("props: "+props+" StreamResult: "+streamResult+" NamePool: "+saxonContext.getController().getNamePool());
            outputter.setOutputDestination(props, streamResult);
            return outputter;
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("e: "+e);
                return                             null;
            }
        }

        public String applyTemplates()
        throws com.icl.saxon.expr.XPathException, javax.xml.transform.TransformerException {
            return applyTemplates(null);
        }


        public String applyTemplates(String select)
        throws com.icl.saxon.expr.XPathException, javax.xml.transform.TransformerException {
            Expression selectExpr = null;
            if (select != null){
                selectExpr = Expression.make(select, saxonContext.getStaticContext());
            }
            //StringBuffer sbuff = new StringBuffer();
            //com.icl.saxon.output.StringOutputter outputter = new com.icl.saxon.output.StringOutputter(sbuff);
            Controller controller = saxonContext.getController();
            Outputter prevOutputter = controller.getOutputter();
            //controller.changeToTextOutputDestination(sbuff);
            java.io.StringWriter writer = new java.io.StringWriter();

            //Emitter.setWriter...
            //java.util.Properties props = prevOutputter.getOutputProperties();
            //javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult(writer);
            //prevOutputter.setOutputDestination(props, streamResult);
            com.icl.saxon.output.Emitter emitter = prevOutputter.getEmitter();
            java.io.Writer prevWriter = emitter.getWriter();
            if ( prevWriter == null ) {
                System.out.println("WARNING: prevWriter was null. outputStream: "+emitter.getOutputStream());
            }
            try {
                String id = ""+(++g_id);


                //This works, but it hoses any previous output to the emitter.
                // solve by setting it much earlier, in startup.
                emitter.setWriter(writer);
                controller.applyTemplates(saxonContext, selectExpr, saxonContext.getMode(), params);



                //controller.resetOutputDestination(prevOutputter);
                //System.out.println("WRITER"+id+"{"+writer.toString()+"}"+"WRITER"+id);
                return writer.toString();
            } finally{
                if ( prevWriter != null ) {
                    emitter.setWriter(prevWriter);
                }
            }
        }
        /*
        public String applyTemplates(String select)
        throws com.icl.saxon.expr.XPathException, javax.xml.transform.TransformerException {
            Expression selectExpr = null;
            if (select != null){
                selectExpr = Expression.make(select, saxonContext.getStaticContext());
            }
            StringBuffer sbuff = new StringBuffer();
            //com.icl.saxon.output.StringOutputter outputter = new com.icl.saxon.output.StringOutputter(sbuff);
            Controller controller = saxonContext.getController();
            Outputter prevOutputter = controller.getOutputter();
            controller.changeToTextOutputDestination(sbuff);
            controller.applyTemplates(saxonContext, selectExpr, saxonContext.getMode(), params);
            controller.resetOutputDestination(prevOutputter);
            return sbuff.toString();
        }
         *
         */



        public String copy(NodeInfo nodeInfo)
        throws com.icl.saxon.expr.XPathException, javax.xml.transform.TransformerException {
            if (nodeInfo == null){
                return "ERROR: [62] NodeInfo was null";
            }
            Controller controller = saxonContext.getController();
            Outputter prevOutputter = controller.getOutputter();
            com.icl.saxon.output.Emitter emitter = prevOutputter.getEmitter();
            java.io.Writer prevWriter = emitter.getWriter();
            if ( prevWriter == null ) {
                System.out.println("WARNING: prevWriter was null. outputStream: "+emitter.getOutputStream());
            }
            try {
                java.io.StringWriter writer = new java.io.StringWriter();
                emitter.setWriter(writer);
                nodeInfo.copy(prevOutputter);
                return writer.toString();
            } finally{
                if ( prevWriter != null ) {
                    emitter.setWriter(prevWriter);
                } else {
                    emitter.setWriter(((com.dynamide.xsl.DynamideSaxonController)controller).getRootWriter());
                }
            }
        }

        public int lineCount(String body){
            Vector lines = com.dynamide.util.StringTools.parseSeparatedValues(body, "\n", true);
            return lines.size();
        }

        public String outputChildren(){
            StringBuffer result = new StringBuffer();
            //don't call getCurrentNode or getCurrentNodeInfo if node is a jdom node wrapper,
            // instead, call getContextNodeInfo.
            Object oNodeInfo = saxonContext.getContextNodeInfo();

                //StringBuffer buf = new StringBuffer();
                //com.icl.saxon.output.StringOutputter outputter = new com.icl.saxon.output.StringOutputter(buf);
                //try {
                //    ((NodeInfo)oNodeInfo).copy(outputter);
                //    if (true){
                //        return buf.toString();
                //    }
                //} catch (Exception e){
                //    System.out.println("ERROR: [outputChildren] "+e);
                //}


            if ( oNodeInfo instanceof com.icl.saxon.jdom.NodeWrapper) {
                com.icl.saxon.jdom.NodeWrapper node = (com.icl.saxon.jdom.NodeWrapper)oNodeInfo;
                Element el = (Element)node.getNode();

                Iterator it = el.getChildren().iterator();
                while (it.hasNext()){
                    Element child = (Element)it.next();
                    result.append(JDOMFile.output(child));
                }
                return result.toString();
            } else {
                System.out.println("outputChildren. class: "+oNodeInfo.getClass().getName());
                return "ERROR: [63] wrong class:"+oNodeInfo.getClass().getName();
            }
        }

        /** Don't use this -- it was for the non-jdom version.
         */
        public String outputChildren(org.w3c.dom.Node node){ //e.g., from context.getCurrentNode()
            StringBuffer result = new StringBuffer();
            org.jdom.input.DOMBuilder builder = new org.jdom.input.DOMBuilder();
            org.jdom.Element el = builder.build((org.w3c.dom.Element)node);
            Iterator it = el.getChildren().iterator();
            while (it.hasNext()){
                Element child = (Element)it.next();
                result.append(JDOMFile.output(child));
            }
            return result.toString();
        }

        public String escape(String src){
            return com.dynamide.util.StringTools.escape(src);
        }

        public Object get(String what){
            if (what.equalsIgnoreCase("match")){
                return m_match;
            }
            return "";
        }

    }  //End inner class.

    /* --------------------------------------------------
     * Here's how to get params for applyTemplates:
        ParameterSet params = null;
        if (usesParams) {
            params = new ParameterSet();
            Node child = getFirstChild();
            while (child!=null) {
                if (child instanceof XSLWithParam) {
                    XSLWithParam param = (XSLWithParam)child;
                    params.put(param.getVariableFingerprint(), param.getParamValue(context));
                }
                child = child.getNextSibling();
            }
        }
     * --------------------------------------------------*/

    private static int g_id = 0;


}