// call setjavadynamide.bat
// set lib=%classpath%
// javac -classpath C:/dynamide/src/java;%lib% C:/dynamide/src/java/com/dynamide/util/WMTransform.java


package com.dynamide.util;

import java.util.*;

import org.jdom.*;

import org.jaxen.*;

import com.dynamide.DynamideObject;

import com.dynamide.util.XDB;


/*
start
    get root of xml doc
    get root of templates doc
    look for first template that matches, given the root element of the xml doc.
        That will be the list returned for the first template whose "select" attribute returns a non-empty node list
        when selected against the current element.

            e.g. given that we are in the context of the root node, and have a template:
                 <template match="/">webmacro here</template>
            and an xml document:
                 <first>
                    <second id="a"/>
                    <second id="b"/>
                 </first>
            the list will contain one Element:
                 <first>...</first>

        With that node list, iterate, calling the template body using the root node as $node.

    in the templates, there will be a call to apply() or apply(xpath)
    for apply(), find templates that match given the current node.

    You know the current node because you put it on the stack

            e.g. given that we are in the context of the first node already, and have a template:
                 <template match="second">webmacro here</template>
            and an xml document:
                 <first>
                    <second id="a"/>
                    <second id="b"/>
                 </first>
            the list will contain two Elements:
                    <second id="a"/>
                    <second id="b"/>

   For apply(), you can select the List that matches the select= attribute and pass that in.
      The engine should iterate over the whole list, seeing which ones are matched by the templates.
      so this is NxM loops, where N = number of nodes the user selected with an xpath, M is the number of templates
      that match, based on the *template's* xpath.   You'll have to save these lists on a stack, though maybe local variables
      means you use the java stack...

   You can't have a template that matches "", and you can't have a template that matches ".".
   When you specify that the template matches "/", you get the Document node, otherwise you get the Element node.

*/

public class WMTransform extends DynamideObject {
    public WMTransform(){
        super(null);
    }

    private class TemplateStackState {
        public TemplateStackState(List currentList, Element currentEl, Context context){
            this.currentList = currentList;
            this.currentEl = currentEl;
            this.context = context;
        }
        List currentList;   // keep track of current node for when we come back from a dive, and want to do the next
        Element currentEl;  // keep track of current node for when we come back from a dive, and want to use $node
        Context context;    // don't let recursive calls mess up the current webmacro context.
    }

    private Stack templateStack = new Stack();
    private XDB xdb = new XDB();
    private List g_templateList = null;

    public void start(){
        System.out.println("in start");
        Element xmlroot = xdb.openXML("xmlroot", "C:\\temp\\junk\\wmxsl\\wmtemplate-data.xml"); //returns the root element
        Element templates = xdb.openXML("templates", "C:\\temp\\junk\\wmxsl\\wmtemplates.xml"); //returns the root element
        List templateList = xdb.select(templates, "template");
        g_templateList = templateList;
        String result = applyRootTemplate(xmlroot.getDocument(), templateList);
        //String result = apply(xmlroot, templateList);
        System.out.println("result: ==["+result+"]==");
    }

    public String applyRootTemplate(Document doc, List templateList){
        System.out.println("in applyRootTemplate");
        Element templateEl = xdb.selectFirst("templates", "template[@match='/']");
        if (templateEl != null){
            String template = templateEl.getText();
            String templateID = templateEl.getAttributeValue("match");
            StringBuffer result = new StringBuffer();
            Vector matches = new Vector();
            matches.addElement(doc);
            result = applyTemplateToDocElements(matches, null /*stackState*/, template, templateID, result);
            return result.toString();
        } else {
            return applyDefault(doc.getRootElement(), g_templateList);
            //return applyDefault(doc, templateList);
        }
    }

    /*public String applyDefault(Document doc, List templateList){
        System.out.println("in applyDefault(Document,List)");
        return "NOT IMPLEMENTED";
        //return apply(doc.getRootElement(), templateList);
    } */

    public String applyDefault(Element docEl, List templateList){
        System.out.println("in applyDefault(Element,List)");
        return apply(docEl, docEl.getChildren(), templateList);
    }



    public String apply(Element currentNode, String xpath){
        System.out.println("in applyFoo");
        return apply(currentNode, xpath, null);
    }

    public String apply(Element currentNode, String xpath, Map variables){
        System.out.println("in apply(Element["+currentNode+"], String["+xpath+"], Map)");
        try {
            List list = xdb.select(currentNode, xpath);
            System.out.println("xdb.select(currentNode, xpath): "+list);
            StringBuffer result = new StringBuffer();
            Iterator it = list.iterator();
            while ( it.hasNext() ) {
                Element docEl = (Element)it.next();
                result.append(apply(currentNode, docEl));
            }
            return result.toString();
        } catch (Exception e){
            return "ERROR: [78] "+e;
        }
    }

    public String apply(Element contextEl, List list, List templateList){
        System.out.println("in apply(Element, List, List)");
        StringBuffer result = new StringBuffer();
        Iterator it = list.iterator();
        while ( it.hasNext() ) {
            Element docEl = (Element)it.next();
            result.append(applyTemplatesForChild(contextEl, docEl, templateList));
        }
        return result.toString();
    }

    public String apply(Element docEl, Element child){
        System.out.println("in apply(Element, Element) == ("+docEl+", "+child+")");
        if ( docEl == null ) {
            return "DONE";
        }
        return applyTemplatesForChild(docEl, child, g_templateList);
    }

    private String applyTemplatesForChild(Element docEl, Element child, List templateList){
        System.out.println("in applyTemplatesForChild(Element, Element, List)== ("+docEl+", "+child+", templateList)");
        //System.out.println("\r\n\r\n\r\napply2 {"+xdb.outputElement(docEl)+"}");
        StringBuffer result = new StringBuffer();
        List matches = null;
        String template = "";
        Iterator it = templateList.iterator();
        while ( it.hasNext() ) {
            matches = null;
            template = "";
            Element templateEl = (Element)it.next();
            //System.out.println("template: '"+xdb.outputElement(templateEl)+"\r\ntext: "+templateEl.getText());
            String match = templateEl.getAttributeValue("match");
            if (match != null && match.length()>0){
                //System.out.println("matching: {"+xdb.outputElement(docEl)+"}");
                matches = xdb.select(docEl, match);  // e.g. select(first, "second"), where you have /first/second in the tree
                                                     // should give you all the "second" elements.
                System.out.println("    xdb.select("+docEl+", "+match+") == "+matches);
                if (matches != null && matches.size()>0){
                    if ( selfInList(matches, child)){
                        //System.out.println("\r\n\r\napply2 used docEl "+docEl.getName()+" {"+xdb.outputElement(docEl)+"}");
                        String templateID = templateEl.getAttributeValue("match");
                        template = templateEl.getText();
                        System.out.println("applyTemplatesForChild(Element,List) template matches docEl: '"+docEl+"' match: '"+match+"' matches: "+matches);
                        TemplateStackState stackState = null;
                        result = applyTemplateToDocElement(child, stackState, template, templateID, result);
                        return result.toString();
                    }
                }
            }
        }
        return "";
    }

    private boolean selfInList(List matches, Element docEl){
        System.out.println("    in selfInList");
        Iterator it = matches.iterator();
        while ( it.hasNext() ) {
            Object obj = it.next();
            if (obj instanceof Element){
                Element el = (Element)obj;
                if ( el == docEl ) {
                    return true;
                }
            } else {
                System.out.println("        WARNING: [selfInList] element was not an Element: "+obj.getClass().getName());
            }
        }
        return false;
    }

    private StringBuffer applyTemplateToDocElement(Element child, TemplateStackState stackState, String template, String templateID, StringBuffer result){
        templateID = templateID == null ? "null" : templateID;
        System.out.println("in applyTemplateToDocElement ["+templateID+"]");
        result.append(expandTemplate(template, templateID, child, stackState)); //expand the WebMacro template for this document element.
        return result;
    }

    private StringBuffer applyTemplateToDocElements(List docElements, TemplateStackState stackState, String template, String templateID, StringBuffer result){
        templateID = templateID == null ? "null" : templateID;
        System.out.println("in applyTemplateToDocElements ["+templateID+"]");
        Iterator it = docElements.iterator();
        while ( it.hasNext() ) {
            Object obj = it.next();
            if (obj instanceof Element){
                Element docEl = (Element)obj;
                result.append(expandTemplate(template, templateID, docEl, stackState)); //expand the WebMacro template for this document element.
            } else if (obj instanceof Document){
                result.append(expandTemplate(template, templateID, obj, stackState)); //expand the WebMacro template for this document element.
            } else {
                System.out.println("    WARNING: [applyTemplateToDocElements] element was not an Element: "+obj.getClass().getName());
            }
        }
        return result;
    }

    private String expandTemplate(String template, String templateID, Object node, TemplateStackState stackState){
        System.out.println("in expandTemplate ["+templateID+"]");
        try {
            WebMacroTools wmt = new WebMacroTools("C:\\temp\\junk\\wmxml",             //templateroot
                                                  "C:/dynamide/src/java/com/dynamide/WebMacroDynamide.properties",  //properties-file
                                                  false);                              //cache
            org.webmacro.Context c = wmt.getContext();
            try {
                if (node instanceof Element){
                    c.put("node", node);
                    c.put("document", ((Element)node).getDocument());
                } else if (node instanceof Document){
                    c.put("document", node);
                }
                c.put("templateList", g_templateList);
                c.put("driver", this);
                c.put("context", c);
                //System.out.println("template-----------------\r\n"+template+"\r\n-----------------");
                String output = wmt.expand(c, template);
                //System.out.println("output: "+output);
                return output;
            } finally {
                wmt.releaseContext(c);
                wmt.destroy();
            }
        } catch (Exception e){
            System.out.println("ERROR: [79] "+e);
            return "";
        }
    }

    private void test(){
        Element docEl = xdb.openXML("templates", "C:\\temp\\junk\\wmxsl\\wmtemplate-data.xml");
        String match  = "/first";
        Element first = xdb.selectFirst(docEl, "/first/second");
        //System.out.println("matches: "+matches.toString());
        List matches = xdb.select(first, match);
        System.out.println(" xdb.select("+first+", "+match+") == "+matches);

        /*
        System.out.println("\r\nfirst element: "+xdb.outputElement(first));
        matches = xdb.select(first, "/first");
        if (matches != null){
            System.out.println("\r\nmatches of /first in first element: "+matches.toString());
        }
        */
    }


    public static void main(String[]args){
        WMTransform transform = new WMTransform();
        transform.start();
        //transform.test();
        System.out.flush();
        System.exit(0);
    }

}