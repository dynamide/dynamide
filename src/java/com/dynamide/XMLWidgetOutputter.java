/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.IOException;
import java.io.Writer;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.dynamide.util.Log;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

//import org.jdom.Attribute;
///import org.jdom.CDATA;
//import org.jdom.Comment;
//import org.jdom.DocType;
///import org.jdom.Document;
//import org.jdom.Namespace;
//import org.jdom.ProcessingInstruction;

//import com.dynamide.NamespaceStack;


//import com.dynamide.XMLOutputter;

//public class XMLWidgetOutputter extends org.jdom.output.XMLOutputter
public class XMLWidgetOutputter extends XMLOutputter
 {
    /** This constructor uses a jdom outputter with pretty print.
    */
    public XMLWidgetOutputter() {
        super();
        setFormat(org.jdom.output.Format.getPrettyFormat());
    }
    /** This constructor uses a jdom outputter with pretty print.
    */
    public XMLWidgetOutputter(String indent) {
        super();
        setFormat(org.jdom.output.Format.getPrettyFormat().setIndent(indent));
    }
    public XMLWidgetOutputter(String indent, boolean newlines) {
        super();
        org.jdom.output.Format f = getFormat();
        f.setIndent(indent);
        if (!newlines){
            f.setLineSeparator("");   
        }
        setFormat(f);
    }
    public XMLWidgetOutputter(String indent, boolean newlines, String encoding) {
        this(indent, newlines);
        setFormat(getFormat().setEncoding(encoding));
    }
    /*public XMLWidgetOutputter(XMLOutputter that) {
        super(that);
    } */

    public void finalize() throws Throwable {
        setSession(null);
        //System.out.println("XMLOutputter.finalize");
        super.finalize();
    }
    
    public void setExpandEmptyElements(boolean value){
        setFormat(getFormat().setExpandEmptyElements(value));   
    }
            

    private Session session = null;
    public Session getSession(){return session;}
    public void setSession(Session new_value){session = new_value;}

    private Page m_page = null;
    public Page getPage(){return m_page;}
    public void setPage(Page new_value){m_page = new_value;}

    private int m_rawOutputLevel = 0; //signals a no-expansion block.

    private StringList m_foundWidgets = new StringList();
    public StringList getFoundWidgets(){
        return m_foundWidgets;
    }

    public static final int PRINT_WIDGETS = 0;
    public static final int FIND_WIDGETS = 1;

    private int m_mode = PRINT_WIDGETS;
    public int getMode(){return m_mode;}
    public void setMode(int new_value){m_mode = new_value;}

    protected void printElement(Writer out, Element element, int indentLevel, NamespaceStack namespaces)
    throws IOException {
        if ( element == null ) {
            Log.error(XMLWidgetOutputter.class, "XMLWidgetOutputter: element is null!"
                     +Tools.getStackTrace(new Exception()));
            return;
        }
        if ( m_mode == FIND_WIDGETS ) {
            findWidgetsInElement(element, out, indentLevel, namespaces);
            return;
        }
        String name = element.getName();
        //if ( (name == null) || (! element.getName().equals("widget")) ){
        if ( name == null){
            super.printElement(out, element, indentLevel, namespaces);
            return;
        }
        if (m_rawOutputLevel > 0){
            super.printElement(out, element, indentLevel, namespaces);
            return;
        }
        if ( JDOMFile.getAttributeValue(element, "class").equals("dynamideRawOutput")) {
            try {
                m_rawOutputLevel++;
                super.printElement(out, element, indentLevel, namespaces);
            } finally {
                m_rawOutputLevel--;
            }
            return;
        }
        if ( JDOMFile.getAttributeValue(element, "class").equals("designModeError")) {
            if (session != null && session.getDesignMode()){
                super.printElement(out, element, indentLevel, namespaces);
            } else {
                // nothing.  %% Ideally, we'd like to run through here when saving the page -- errors don't need to be saved in the source.
            }
            return;
        }
        String theClass = JDOMFile.getAttributeValue(element, "class");
        boolean isWidget = theClass.equals("widget");
        boolean isContainer = theClass.equals("container");
        if ( (!isWidget) && (!isContainer)) {
            super.printElement(out, element, indentLevel, namespaces);
            return;
        }
        if ( m_page == null ) {
            Log.error(XMLWidgetOutputter.class, "XMLWidgetOutputter.m_page is null!");
            return;
        }
        try {
            String href = JDOMFile.getAttributeValue(element, "href");
            if ( href != null && href.length()>0 ) {
                String id = JDOMFile.getAttributeValue(element, "id");
                if ( isContainer ) {
                    Log.debug(XMLWidgetOutputter.class, "XMLWidgetOutputter.calling expandContainer...");
                    String containerOutput = m_page.getSession().expandContainer(id, href, null, m_page);
                    out.write(containerOutput);
                } else if (isWidget) {
                    Log.error(XMLWidgetOutputter.class, "Widget href unsupported now in XMLWidgetOutputter.printElement");
                    /*
                     * %% todo: make it so that you can have just the <widget> element in another file, and load that here.
                     *
                    Log.debug(XMLWidgetOutputter.class, "XMLWidgetOutputter.calling expand Widget...");
                    Element element = new Widget(...openFile(href), m_page);
                    String containerOutput = m_page.getSession().expandContainer(id, href, null, m_page);
                    out.write(containerOutput);
                    */
                }
            } else {
                m_page.printSerializedWidget(element, out, indentLevel);
            }
        } catch (Exception e){
            IOException iox = new IOException(e.toString());
            iox.initCause(e);
            throw iox;
        }
        return;
    }

    private void findWidgetsInElement(Element element, Writer out, int indentLevel, NamespaceStack namespaces)
    throws IOException {
        if ( ! JDOMFile.getAttributeValue(element, "class").equals("widget")) {
            //System.out.println("not: "+element.getName());
            super.printElement(out, element, indentLevel, namespaces);
            return;
        }
        //System.out.println("found: "+element.getName());
        m_foundWidgets.addObject(element.getAttributeValue("id"), element);
        return;
    }

}
