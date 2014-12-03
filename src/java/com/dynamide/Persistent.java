/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.dynamide.interpreters.InterpreterTools;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;

import com.dynamide.datatypes.DatatypeException;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.util.FileTools;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

public class Persistent extends JDOMFile implements IPropertyProvider {

    public Persistent(DynamideObject owner, Session session){
        super(owner);
        m_session = session;
    }

    public Persistent(DynamideObject owner, String filename, Session session)
    throws JDOMException, IOException {
        super(owner, filename);
        m_session = session;
        //System.out.println("Persistent loading file: "+filename);
    }

    private Session m_session = null;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    protected static final boolean DEBUG_PROPS = false;
    protected static final boolean DEBUG_PAGE_SAVE = false;

    private StringList m_propertiesTable = new StringList(8);
    public  StringList getPropertiesTable(){
        return m_propertiesTable;
    }

    /** Subclasses can get the props from somewhere and install them here.
     *  Widget does this since the defaults come first from WidgetType.
     */
    protected void resetPropertiesTable(StringList newTable, String reason, DynamideObject newOwner){
        if (DEBUG_PROPS) System.out.println(getObjectID()+".resetPropertiesTable ("+reason+") \r\nold:\r\n"+m_propertiesTable.toString() + "\r\nnew:\r\n"+newTable.toString());
        int newTable_size = newTable.size();
        for (int i=0; i < newTable_size; i++) {
            Object o = newTable.getObjectAt(i);
            if ( o instanceof Property ) {
                ((Property)o).setOwner(newOwner);
            }
        }
        m_propertiesTable = newTable;
    }

    protected void sortPropertiesTable(){
        StringList props = new StringList();
        StringList events = new StringList();
        synchronized (m_propertiesTable){
            int m_propertiesTable_size = m_propertiesTable.size();
            for (int i=0; i < m_propertiesTable_size; i++) {
                String name = m_propertiesTable.getString(i);
                Property prop = (Property)m_propertiesTable.getObjectAt(i);
                if ( prop.isEvent() ) {
                    events.addObject(name, prop);
                } else {
                    props.addObject(name, prop);
                }
            }
            events.sort();
            props.sort();
            m_propertiesTable.clear();
            m_propertiesTable.add(props);
            m_propertiesTable.add(events);
        }
    }

    /** Same as isPropertyTrue */
    public boolean isValueTrue(String propertyName)
    throws Exception {
        return get(propertyName).toString().equalsIgnoreCase("true");
    }

    /** Same as isValueTrue */
    public boolean isPropertyTrue(String propertyName)
    throws Exception {
        return get(propertyName).toString().equalsIgnoreCase("true");
    }

    public boolean hasProperty(String propertyName){
        return (null != m_propertiesTable.get(propertyName));
    }

    public Enumeration getProperties(){
        //updateProperties();
        sortPropertiesTable();
        return getPropertiesTable().objects();
        //return getPropertiesTable().toHashtable();
    }

    public Enumeration getPropertyNames(){
        m_propertiesTable.sort();
        return m_propertiesTable.keys();
    }

    public Element getPropertiesElement(){
        Element properties = new Element("properties");
        Property property;
        Element propertyElement;
        String propName;
        Enumeration en = getPropertyNames();
        while (en.hasMoreElements()){
            propName = ((String)en.nextElement());
            property = getProperty(propName);
            propertyElement = property.toElement();
            properties.addContent(propertyElement);
        }
        return properties;
    }

    public String getPropertyStringValue(String name){
        return getPropertyValue(name).toString();
    }

    public Object getPropertyValue(String name){
        Object o = m_propertiesTable.get(name, true/*ignore case*/);
        if (o==null){
            return "";
        }
        Object res = "ERROR: [57] Persistent.getPropertyValue";
        try {
            Property property = (Property)o;
            res = property.getValue();
        } catch (Exception e){
            //if (DEBUG_PROPS)
            logError("getting property string value: "+Tools.errorToString(e, true));
            res = ""+res + e;
        }
        return res;
    }

    public void setPropertyValue(String name, Object value) throws DatatypeException {
        Property property = getProperty(name);
        if ( property != null ) {
            property.setValue(value);
        } else {
            property = new Property(this, name, value);
        }
        setProperty(name, property);
    }

    public void setProperty(Property property){
        String propName = property.getName();
        setProperty(propName, property);
    }

    public void setProperty(String name, String value) throws DatatypeException {
        if (DEBUG_PROPS) logDebug("setProperty(S,S): "+getID()+':'+name+':'+value);
        System.out.println("setProperty(S,S)0: " + getID() + ':' + name + ':' + value + ';');
        Property property = getProperty(name);
        if ( property != null ) {
            System.out.println("setProperty(S,S)1: "+property.dump()+';'+property.getClass().getName());
            property.setValue(value);
            System.out.println("setProperty(S,S)2: "+property.dump()+';'+property.getClass().getName());
            System.out.println("setProperty(S,S)3: "+value+';'+value.getClass().getName());
        } else {
            property = new Property(this, name, value);
            System.out.println("setProperty(S,S)4: "+value+';'+value.getClass().getName());
        }
        setProperty(name, property);
    }

    public void setProperty(String name, Property value){
        if (DEBUG_PROPS) logDebug("setProperty: "+getID()+':'+name+':'+value.dump());
        Property old = (Property)m_propertiesTable.get(name);
        if (DEBUG_PROPS){
            if ( old != null ) {
                logDebug("setProperty old: "+getID()+':'+old.dump());
            }
        }
        m_propertiesTable.remove(name);
        m_propertiesTable.addObject(name, value);
        Property newp = (Property)m_propertiesTable.get(name);
        if (DEBUG_PROPS) {
            if ( newp != null ) {
                logDebug("setProperty new: "+getID()+':'+newp.dump());
            }
        }
    }

    public void removeProperty(String name){
        m_propertiesTable.remove(name);
    }

    /** %% this is funky, since right now all are stored as String name and value. isEvent is just lost, for example.*/
    public Property getProperty(String propertyName){
        return  (Property)m_propertiesTable.get(propertyName);
    }


    public void addProperty(String name, Property value){
        if (DEBUG_PROPS) logDebug("addProperty: "+getID()+':'+name+':'+value.dump());
        m_propertiesTable.remove(name);
        m_propertiesTable.addObject(name, value);
    }

    /** Static method can be recycled by any subclass.  Override addProperty for more control.*/
    public static void addProperties(Persistent persistent, Element propertyContainerElement, String ownerID)
     throws DatatypeException {
        if (DEBUG_PROPS)   System.out.println("Persistent.addProperties "+ownerID+"\r\n");
        if (propertyContainerElement == null){
            System.out.println("Can't add properties because container element is null (<properties> element is missing) in "
                 +persistent.getClass().getName()+" id: "+persistent.getObjectID()+ " persistent.filename: "+persistent.getFilename());
            Tools.printStackTrace();
            return;
        }
        List properties = propertyContainerElement.getChildren("property");
        Property property;
        Iterator i = properties.iterator();
        while (i.hasNext()) {
            Element propertyEl = (Element) i.next();
            property = new Property(persistent, persistent.getSession(), propertyEl, ownerID, persistent.getFilename());
            persistent.addProperty(property.getName(), property);
            if (DEBUG_PROPS) System.out.println("adding: "+property.getName()+" " +property.toString());
        }
    }

    protected void mergeProperty(String name, Property newProperty){
        Property existingProperty = (Property)m_propertiesTable.getObject(name);
        if ( existingProperty != null ) {
            //logDebug("mergeProperty:override:"+name);
            existingProperty.overrideValuesFrom(newProperty);
            //already in the table, and we just modified it with the overrides.
        } else {
            //that property didn't exist, so just add the new one.
            //logDebug("mergeProperty:add:"+name);
            m_propertiesTable.addObject(name, newProperty);
        }
    }

    public static void mergeProperties(Persistent persistent, Element propertyContainerElement, String ownerID)
     throws DatatypeException {
        if (propertyContainerElement == null){
            persistent.logError("Can't merge properties because container element is null (<properties> element is missing) in "
                +persistent.getClass().getName()+" id: "+persistent.getObjectID()+ " persistent.filename: "+persistent.getFilename());
            return;
        }
        List properties = propertyContainerElement.getChildren("property");
        Property property;
        Iterator i = properties.iterator();
        while (i.hasNext()) {
            Element propertyEl = (Element) i.next();
            property = new Property(persistent, persistent.getSession(), propertyEl, ownerID, persistent.getFilename());
            persistent.mergeProperty(property.getName(), property);
        }
    }

    // deals with anything of the form:
    //    <elem>  <property name="foo"><value>Foo value<value></property> </elem>
    // if you call findProperty(element, "foo"), you'll get "Foo value".
    /**   Except that no-one uses it now...
     *  public static String findProperty(Element element, String propertyName){
     *      List properties = element.getChildren("property");
     *      Iterator i = properties.iterator();
     *      while (i.hasNext()) {
     *          Element propertyElement = (Element) i.next();
     *          if ( propertyElement.getAttributeValue("name").equals(propertyName) ) {
     *              Element valueElement = propertyElement.getChild("value");
     *              if ( valueElement != null ) {
     *                  return valueElement.getTextTrim();
     *              }
     *              valueElement = propertyElement.getChild("defaultValue");
     *              if ( valueElement != null ) {
     *                  return valueElement.getTextTrim();
     *              }
     *          }
     *      }
     *      return "";
     *  }
    */

    public String dumpProperties(){
        return "Properties: "+m_propertiesTable.toString();
    }

    public String listProperties(){
        String nl = "\r\n";
        String indent  = "    ";
        StringBuffer b = new StringBuffer();
        m_propertiesTable.sort();
        int m_propertiesTable_size = m_propertiesTable.size();
        for (int i=0; i < m_propertiesTable_size; i++) {
            String key = m_propertiesTable.getString(i);
            //not needed if you use prop.dump(): b.append(key);b.append(nl);
            Object obj = m_propertiesTable.getObject(key);
            Property prop = ((Property)obj);
            b.append(indent);
            b.append(prop.dump());
            b.append(nl);
        }
        return b.toString();
    }

    public StringList clonePropertiesTable(){
        int m_propertiesTable_size = m_propertiesTable.size();
        StringList other = new StringList(m_propertiesTable_size);
        for (int i=0; i < m_propertiesTable_size; i++) {
            String key = m_propertiesTable.getString(i);
            Object obj = m_propertiesTable.getObject(key);
            Property prop = (Property)((Property)obj).clone();
            other.addObject(key, prop);
        }
        return other;
    }

    public String dumpHTML(){
        StringBuffer buff = new StringBuffer(StringList.DUMPHTML_START);
        buff.append("objectID");
        buff.append(StringList.DUMPHTML_ELEMSEP);
        buff.append(""+getObjectID());
        buff.append(StringList.DUMPHTML_LINESEP);

        StringList list = getPropertiesTable();
        for (Enumeration keys = list.keys(); keys.hasMoreElements(); ){
            String key =  (String)keys.nextElement();
            buff.append(key);
            Object obj = list.getObject(key);
            if (obj!=null){
                buff.append(StringList.DUMPHTML_ELEMSEP);
                //Property property = (Property)obj;
                if ( obj instanceof DynamideObject ) {
                    buff.append( ((DynamideObject)obj).dumpHTML() );
                } else {
                    buff.append(obj.toString());
                }
            } else {
                buff.append(StringList.DUMPHTML_ELEMSEP);
                buff.append(StringList.DUMPHTML_OBJPLACEHOLDER+"empty");
            }
            if (keys.hasMoreElements()) {
                buff.append(StringList.DUMPHTML_LINESEP);
            }
        }
        buff.append(StringList.DUMPHTML_END);
        return buff.toString();
    }

    public String toString(){
        return dumpHTML();
    }

    /** A place to store objects in memory that won't be persisted.  See setNamedObject. */
    public Object getNamedObject(String name){
        Object o = m_propertiesTable.getObject(name);
        if ( o == null ) {
            o = new StringList();
        }
        return o;
    }

    /** A place to store objects in memory that won't be persisted.  Useful when building applications
     *  since you don't have to create a Field just to hold an object like a list between a ServerSide event
     *  and the Widget rendering code, for example.
     */
    public void setNamedObject(String name, Object value){
        m_propertiesTable.remove(name);
        m_propertiesTable.addObject(name, value);
    }

    public ScriptEventSource getEventSourceBody(String findName){
        Element event = findEventElement(findName);
        if ( event != null ) {
            String theText = event.getText();
            if ( theText == null ) {
                return new ScriptEventSource();
            }
            String language = event.getAttributeValue("language");
            language = language == null ? "" : language;
            //System.out.println("eventSourceBody before: "+Tools.showControlChars(theText, true));
            //doesn't seem to help: theText = StringTools.searchAndReplaceAll(theText, "\n", "\r\n");
            //System.out.println("eventSourceBody after: "+Tools.showControlChars(theText, true));
            return new ScriptEventSource(findName, theText, theText, language, getID(), getFilename());
        }
        return new ScriptEventSource();
    }



    private Map m_eventSourceCache = new TreeMap();

    public ScriptEventSource getEventSource(String findName){
        return getEventSource(findName, true);
    }

    public ScriptEventSource getEventSource(String findName, boolean addSignature){
        ScriptEventSource result = (ScriptEventSource)m_eventSourceCache.get(findName);
        if ( result != null ) {
            return result;
        }
        Element event = findEventElement(findName);
        if ( event != null ) {
            String theText = event.getText();
            if ( theText == null ) {
                result = new ScriptEventSource();
            } else {
                String language = event.getAttributeValue("language");
                language = language == null ? "" : language;
                //System.out.println("eventSource before: "+Tools.showControlChars(theText, true));
                // doesn't seem to help: theText = StringTools.searchAndReplaceAll(theText, "\n", "\r\n");
                //System.out.println("eventSource after: "+Tools.showControlChars(theText, true));
                String fullsrc;
                if ( addSignature ) {
                    fullsrc = InterpreterTools.getEventSignature(findName, language)+"{\r\n"
                                +theText
                                +"\r\n}\r\n";
                } else {
                    fullsrc = theText;
                }
                result = new ScriptEventSource(findName,
                                               theText,
                                               fullsrc,
                                               language,
                                               getID(),
                                               getFilename());
            }
        } else {
            result = new ScriptEventSource();
        }
        m_eventSourceCache.put(findName, result);
        return result;
    }

    public Element findEventElement(String findName){
        Element theRoot;
        try {
            theRoot = getRootElement();
        } catch (Exception e){
            System.out.println("getRootElement() threw Exception in "+getID()+" : "+getFilename()+" : "+e);
            return null;
        }
        if (theRoot==null){
            System.out.println("getRootElement() is null in "+getID()+" ["+getClass().getName()+"] : "+getFilename() +
               " at " + Tools.getStackTrace());
            return null;
        }
        List events = theRoot.getChildren("event");
        //System.out.println(getFilename()+" has "+ events.size() +" registered events.");
        Iterator i = events.iterator();
        while (i.hasNext()) {
            Element event = (Element) i.next();
            String eventNameAttr = event.getAttributeValue("name");
            if ( eventNameAttr == null ) {
                logError("Malformed event (has no 'name' attribute: ");
                try{
                    logError((new XMLOutputter()).outputString(event));
                } catch (Exception e){
                    logError("[can't print element]");
                }
            }
            if (eventNameAttr.equals(findName)) {
                return event;
            }
        }
        return null;
    }

    public String listEvents(){
        return Tools.setToString(getEvents());
    }

    public String listEventsHTML(){
        StringBuffer buff = new StringBuffer(StringList.DUMPHTML_START);
        StringList list = getPropertiesTable();
        for (Iterator keys=getEvents().iterator(); keys.hasNext(); ){
            String key =  (String)keys.next();
            buff.append(key);
            if (keys.hasNext()) {
                buff.append(StringList.DUMPHTML_LINESEP);
            }
        }
        buff.append(StringList.DUMPHTML_END);
        return buff.toString();
    }

    public Set getEvents(){
        Element theRoot;
        Map v = Tools.createSortedCaseInsensitiveMap();
        //Vector v = new Vector();
        try {
            theRoot = getRootElement();
        } catch (Exception e){
            System.out.println("getRootElement() threw Exception in "+getID()+" : "+getFilename()+" : "+e);
            return v.keySet();
        }
        if (theRoot==null){
            System.out.println("getRootElement() is null in "+getID()+" ["+getClass().getName()+"] : "+getFilename() +
               " at " + Tools.getStackTrace());
            return v.keySet();
        }
        List events = theRoot.getChildren("event");
        //System.out.println(getFilename()+" has "+ events.size() +" registered events.");
        Iterator i = events.iterator();
        while (i.hasNext()) {
            Element event = (Element) i.next();
            String eventNameAttr = event.getAttributeValue("name");
            if ( eventNameAttr == null ) {
                logError("Malformed event (has no 'name' attribute: ");
                try{
                    logError((new XMLOutputter()).outputString(event));
                } catch (Exception e){
                    logError("[can't print element]");
                }
            } else {
                v.put(eventNameAttr,null);
            }
        }
        return v.keySet();
    }


    public void setEventSource(String eventName, String source){
        try {
            boolean created = false;

            Persistent eventOwner = this;
            DynamideObject theOwner = getOwner();
            if ( theOwner != null && theOwner instanceof Page ) {
                //  super-booty kludge. %% Widgets do not persist, right now.  They stuff themselves into pages.
                //  If this changes, then you'd need to change this test, remembering that Page descends from Widget.
                    eventOwner = (Page)theOwner;
            }
            Element eventEl = eventOwner.findEventElement(eventName);
            if ( eventEl == null ) {
                eventEl = new Element("event");
                eventEl.setAttribute("name", eventName);
                eventEl.setAttribute("language", "beanshell");
                created = true;
            }
            eventEl.removeContent(); //IMPORTANT: removes all child nodes.  Right now I assume there is only one -- the CDATA section.
            eventEl.setText("");
            CDATA cdata = new CDATA(source);
            eventEl.addContent(cdata);
            if (created) {
                Element theRoot = eventOwner.getRootElement();
                theRoot.addContent(eventEl);
            }
        } catch (Exception e){
            logError("[setEventSource]: "+Tools.errorToString(e, true));
        }
    }

    public class Source{
        public String browserID;
        public boolean isWebMacro;
        public boolean isXHTML;
        public Element element;
        public String source;
        public String toString(){return "{Persistent.Source::browserID:"+browserID+";isWebMacro:"+isWebMacro+";isXHTML:"+isXHTML+";element:"+element+";source.length:"+source.length()+"}";}
    }

    public Source getSourceElement()
    throws XMLFormatException {
        //logDebug("getSourceElement() returning source for browser '*'");
        return getSourceElement("*");
    }

    private Map m_sourceCache = new TreeMap();

    /** get whatever the first element inside the CDATA inside the htmlsrc element is.  For
     *  containers, this may be HTML, or it may be SPAN or DIV or something else for Widgets.
     */
    public Source getSourceElement(String browserID)
    throws XMLFormatException {
        String cacheName = browserID.length()>0 ? browserID : "default";
        Source result = (Source)m_sourceCache.get(cacheName);
        if ( result != null ) {
            return result;
        }
        //logDebug("Persistent.getSourceElement "+getFilename()+"::"+browserID);
        Element htmlsrc = getHtmlsrcElement(browserID);
        if (htmlsrc == null){
            return null;
        }
        result = new Source();
        result.browserID = browserID;
        String text = htmlsrc.getText();
        String isWebMacro = htmlsrc.getAttributeValue("isWebMacro");
        if ( isWebMacro == null ) {
            result.isWebMacro = true;
        } else  {
            result.isWebMacro = isWebMacro.equals("true");
        }
        String isXHTML = htmlsrc.getAttributeValue("isXHTML");
        if ( isXHTML == null ) {
            result.isXHTML= true;
        } else  {
            result.isXHTML = isXHTML.equals("true");
        }
        if ( result.isXHTML && (!result.isWebMacro) ){
            JDOMFile jdf = new JDOMFile(this);
            Document newDoc;
            try {
                //not actually necessary: text = "<?xml version=\"1.0\"?>\r\n<!DOCTYPE HTML>\r\n" + text ;
                newDoc = jdf.readFromString(text);
                logDebug("Persistent.getSourceElement rendering non-webmacro source: "+getFilename());
            } catch (Exception e){
                logError("[108] [getHtmlsrcElement] Persistent couldn't read string. ---------\r\n"+text+"\r\n--------- Error: "+e);
                //return new Element("ERROR").addContent("ERROR: [107] "+Session.formatTemplateSyntaxException(text, e.getMessage(), -1));
                throw Session.createTemplateSyntaxException("ERROR: [108] "+getFilename()+"\r\nthrew: "+e.getMessage(), text);
            }
            Element html = newDoc.getRootElement();
            result.element = html;
        } else {
            result.source = text;
        }
        m_sourceCache.put(cacheName, result);
        return result;

        /*Element html = htmlsrc.getChild("html");
        if ( html == null ) {
            html = htmlsrc.getChild("HTML");
        }
        return html;
         * */
    }

    /** @todo make it use the request browser ID. %%
     */
    public Element getHtmlsrcElement(){
        return getHtmlsrcElement("*");
    }

    private Map m_htmlsrcCache = new TreeMap();

    public Element getHtmlsrcElement(String browserID){
        Element htmlsrc;
        String cacheName = browserID.length()>0 ? browserID : "default";
        htmlsrc = (Element)m_htmlsrcCache.get(cacheName);
        if ( htmlsrc != null ) {
            return htmlsrc;
        }
        //logDebug("getHtmlsrcElement called for: "+cacheName+" cache: "+m_htmlsrcCache);
        htmlsrc = findFirstElementWithAttribute(getRootElement(), "htmlsrc", "browser", browserID, false);
        //if (htmlsrc != null) logDebug("getHtmlsrcElement(\""+browserID+"\") returning source for browser '"+browserID+"'");
        if (htmlsrc == null){
            htmlsrc = findFirstElementWithAttribute(getRootElement(), "htmlsrc", "browser", "*", false);
            //if (htmlsrc != null) logDebug("getHtmlsrcElement(\""+browserID+"\") returning source for browser '*'");
        }
        if (htmlsrc == null){
            htmlsrc = getRootElement().getChild("htmlsrc");
            //if (htmlsrc != null) logDebug("getHtmlsrcElement(\""+browserID+"\") returning source from htmlsrc element");
        }
        if ( htmlsrc != null ) {
            m_htmlsrcCache.put(cacheName, htmlsrc);
            String href = htmlsrc.getAttributeValue("href");
            if ( href != null && href.length()>0) {
                Session session = getSession();
                if ( session != null ) {
                    try {
                        String source = session.getAppFileContent(href);
                        if ( source != null && source.length()>0) {
                            htmlsrc.removeContent(); //IMPORTANT: removes all child nodes.  Right now I assume there is only one -- the CDATA section.
                            htmlsrc.setText("");
                            CDATA cdata = new CDATA(source);
                            htmlsrc.addContent(cdata);
                        }
                    } catch (Exception e)  {
                        logError("Couldn't get htmlsrc href resource", e);
                    }
                } else {
                    logError("Session was null in "+getDotName()+" when calling getHtmlsrcElement()");
                }
            }
        }
        return htmlsrc; //could be null if not found.
    }

    public String getRawHTMLSource(){
        return getRawHTMLSource("*");
    }

    /**  Return just the un-rendered xhtml source, un-processed by Webmacro.
      *  This is used by the IDE.
      *  Doesn't work for Widget since Widget isn't file based, so getRootElement fails.
      *
      * WidgetType, Widget call this method rather than getHTMLSource,
      *  since there is no HTML element in a widget type source.
      */
    public String getRawHTMLSource(String browserID){
        Element htmlsrc = getHtmlsrcElement(browserID);
        if (htmlsrc == null){
            logError("[112] getRawHTMLSource couldn't find source for browserID '"+browserID+"' in "+getFilename());
            return "";
        }
        String result = htmlsrc.getText();
        return result;
    }


    /** Use this to set the source using IE's invalid innerHTML and outterHTML.
     *  This calls com.dynamide.util.IETidy to tidy the source and turn it into XHTML,
     *  and then calls the normal setHTMLSource(String).
     *
     *  @see #setHTMLSource(java.lang.String)
     */
    public String setHTMLSourceIE(String nonXhtmlSource) throws XMLFormatException {
        //System.out.println("\r\nFROM BROWSER: -----------------------------\r\n"+nonXhtmlSource+"\r\n----------------\r\n");
        nonXhtmlSource = com.dynamide.util.StringTools.searchAndReplaceAll(nonXhtmlSource, "&nbsp;", "&#160;");
        return setHTMLSource(com.dynamide.util.IETidy.parse(nonXhtmlSource));
    }

    public String setHTMLSource(String xhtmlSource) throws XMLFormatException {
        try{
            //System.out.println("\r\nFROM BROWSER, TIDIED: -----------------------------\r\n"+xhtmlSource+"\r\n----------------\r\n");
            //Format the new xhtmlSource into prettyHTML:
            JDOMFile htmlFile = new JDOMFile(this);
            htmlFile.readFromString(xhtmlSource);
            removeWidgetContents((Element)(htmlFile.getRootElement()));  //operate on the newly created tree, htmlFile.
            Element clone = (Element)(htmlFile.getRootElement().clone());
            String prettyHTML = "";
            try {
                prettyHTML = prettyPrintHTML(output(clone));  //methods in superclass JDOMFile.
                if (DEBUG_PAGE_SAVE){
                    if (getSession()!=null){
                        FileTools.saveFile("", "pagepostPretty.html", prettyHTML);
                        System.out.println("SEE: ./pagepostPretty.html");
                    }
                }
            } catch (XMLFormatException xe){
                String err1 = "ERROR: [109] [Persistent.setHTMLSource] "+xe.getMessage();
                System.out.println(err1);
                return err1;
            }

            //Remove the old HTML by replacing the htmlsrc element (removeChildren didn't work):
            Element root = getRootElement();
            Element htmlsrc = root.getChild("htmlsrc");    //%% arg! just gets the first one!!!
            root.removeContent(htmlsrc);
            htmlsrc = new Element("htmlsrc");
            htmlsrc.setAttribute("browser", "*");  // %% set this to accomodate other browsers.
            root.addContent(htmlsrc);

            //Add the new prettyHTML to the newly cleaned out htmlsrc:
            CDATA cdata = new CDATA(prettyHTML);
            htmlsrc.addContent(cdata);
            System.out.println("Successfully added new content: "+getFilename());
            //System.out.println("************** \r\n"+getHTMLSource("*")+"\r\n**************");
            return "";
        } catch (Exception e){
            String err2 =
                Session.formatTemplateSyntaxException("ERROR: [9] :::::setHTMLSource "+e, xhtmlSource, -1);
            //System.out.println(err2);
            throw new XMLFormatException(err2);
        }
    }

    public void removeWidgetContents(Element root){
        //IE always uppercases elements, but don't rely on that.
        //Also, SPAN is used now, but Dynamide will support both div and span
        //Use the "or" operator to avoid doing 4 selectes:
        removeWidgetContents(select(root, "(//span|//SPAN|//div|//DIV)[@class='widget']"));
    }

    public void removeWidgetContents(List list){
        //System.out.println("found some nodes ^^^^^^^^^^^^^^^^^^^^^^^"+list.size()+getErrors());
        //clearErrors();
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if (obj instanceof Element){
                Element el = (Element)obj;
                if (el.getAttributeValue("class").equals("widget")){
                    el.removeContent();
                    el.setText("");
                    el.removeAttribute("onresizestart");
                    el.removeAttribute("onclick");
                }
            } else {
                System.out.println("WARNING: [removeWidgetContents] obj NOT an Element: "+obj.toString());  //doesn't happen, but want a warning in case something changes.
            }
        }
    }

}
