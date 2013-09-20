/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;

import com.dynamide.datatypes.DatatypeException;
import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;


/** <p>Runtime object representing a Widget.  (See also WidgetType, which is a representation of the component
 *  and its defining file, and contains all the valid/default properties.)  This class should give access
 *  to that information but also the runtime properties set by the app designer.  In the xml file
 *  that persists the design-time Page, all Widget instances are persisted, in widget elements.
 *  Properties for the Widget (that are legal from the WidgetType) are stored in the Widget instance's
 *  widget element in the page file.  Properties that are named in any attached Field object are
 *  <b>stored there instead</b>.</p>
 *
 *  <p>Widget and Widget type were made separate classes because the persistence is separate
 *  and represents the difference between type (WidgetType) and instance (Widget), not to be confused
 *  with types and instances of these java classes.   This class descends from AbstractWidget, as
 *  does com.dynamide.Page.  AbstractWidget abstracts the code for dealing with the WidgetType.</p>
 *
 *  <p>The lookup for properties is done in three steps then:</p>
 *  <ul>
 *   <li>Look in the Field referred to by the "field" string property.
 *   <li>Look in the Widget
 *   <li>Look in the WidgetType
 *  </ul>
 * </p>
 *
 *  <p>When setting properties, the lookup is:
 *  <ul>
 *    <li>Look in the Field referred to by the "field" string property to see if present.
 *       If so, set it in the Field.  %%TODO This will be problematic with DB fields!!
 *    <li>Look in the WidgetType, to see if legal.  If legal, set in Widget.
 *  </ul>
 *  Note that when setting, properties are NOT persisted into WidgetType, since this would change the component type.
 *  There is no GUI or any other way now to change the WidgetType properties.
 *  These must be edited in the component xml file.</p>
 *
 *  <p>Also, note that Widgets do not save themselves to a file like Page does.  They are persisted in the Page file's
 *  persistence of the Page they are on.</p>
 */
public class Widget extends AbstractWidget {
    public Widget(DynamideObject owner, Session session){
        super(owner, session);
    }

    //This is just here for Page subclass.  Widgets are not created from files.
    public Widget(DynamideObject owner, String filename, Session newSession)
    throws JDOMException, IOException {
        super(owner, filename, newSession);
    }

    public void finalize() throws Throwable {
        cleanup();
        //System.out.println("Widget.finalize");
        super.finalize();
    }

    public void cleanup(){
        setOwner(null);
        setSession(null);
        setPage(null);
        super.close();
    }

    public String toString(){
        return getClass().getName()+"[ID:"+getID()+";ObjectID:"+getObjectID()+"]";
    }

    private boolean m_designMode = false;
    public boolean getDesignMode(){return m_designMode;}
    public void setDesignMode(boolean new_value){m_designMode = new_value;}

    private String m_typeError = "";//hold the error string if there is any problem when setting the type, e.g. component not found.

    private Page page = null;
    public Page getPage(){return page;}
    public void setPage(Page new_value){
        page = new_value;
        if ( page != null ) {
            setOwner(page);
        }
    }

    /** Persist to Element, rather than just returning the m_element, since this saves any in-memory changes*/
    public Element toElement(){
        Element element = new Element("widget");
        element.setAttribute("id", getID());
        Element properties = getPropertiesElement();
        cleanProperties(properties);
        element.addContent(properties);
        return element;
    }


    public String  getName(){return getID();}
    public void setName(String  new_value) throws DatatypeException {
        System.out.println("WARNING: ==================== Widget.setName() was called");
        setID(new_value);
        setProperty("name", getName());
    }

    // %% and make sure fields are renamed.
    public void setID(String new_value){
        Page aPage = getPage();
        if ( aPage!=null && getOwner()==aPage ) {
            aPage.renameWidget(this, getID(), new_value);
        }
        super.setID(new_value);
    }

    public String getEventPrefixName(){
        String id = getName();
        if ( getPage()!=null ) {
            id = getPage().getEventPrefixName()+'_'+id;
        }
        return id;
    }

    /** There's also a version of this in Page, but not polymorphic.
     */
    public String eventShortNameToFullName(String eventShortName){
        return getEventPrefixName()+'_' + eventShortName;
    }

    private String getTypeFromWidgetElement(Element new_value){
        String aType = "";
        Element typeEl = JDOMFile.findFirstElementWithAttribute(new_value, "property", "name", "type", false);
        if ( typeEl != null ) {
            Element valueEl = typeEl.getChild("value");
            if ( valueEl != null ) {
                aType = valueEl.getText();
            }
        }
        return aType;
    }

    private void enterSetElement(Element new_value){
        try{
            if (DEBUG_PROPS) {
                System.out.println("Entering "+getID()+".initFromElement("+new_value.getName()+")");
                if ( new_value != null ) {
                    System.out.println(""+m_id+" properties from new_value [1] \r\n"+(new XMLOutputter()).outputString(new_value));
                } else {
                    System.out.println("new_value is null");
                }
            }
        } catch (Exception e){
            System.out.println("couldn't setElement "+Tools.errorToString(e,true));
        }
    }

    private void leaveSetElement(Element new_value){
        try {
            if (DEBUG_PROPS){
                System.out.println("Leaving "+m_id+".initFromElement("+new_value.getName()+")");
                System.out.println(" with props: \r\n"+getPropertiesTable().toString());
                if (new_value != null){
                    System.out.println(m_id+" done setting properties from m_element \r\n"+(new XMLOutputter()).outputString(new_value));
                }
            }
        } catch (Exception e){
            System.out.println("couldn't setElement "+Tools.errorToString(e,true));
        }
    }

    //Totally reworked this thing in build-14.  See build-13 for the old way.
    public void initFromElement(Element new_value) throws DatatypeException {
        if (DEBUG_PROPS) enterSetElement(new_value);
        String elname = new_value.getName();
        if (elname.equals("widget")){
            m_id = JDOMFile.getAttributeValue(new_value, "id"); // %% note this makes local values hidden, id is looked up always.
            String aType = getTypeFromWidgetElement(new_value);
            try {
                if (aType.length() == 0){
                    logError("[20] No type found for widget: "+m_id);
                    return;
                }
                setType(aType);
            } catch (Exception e){
                m_typeError = Tools.errorToString(e, true);
            }
            if (DEBUG_PROPS) System.out.println("setting m_element to new_value");
        }
        if (new_value != null){
            Element propertiesElement = new_value.getChild("properties");
            //System.out.println("BEFORE props ["+getID()+"]: \r\n"+getPropertiesTable().toString());
            //5/16/2002 4:49PM back in:
            Persistent.addProperties(this, propertiesElement, getID());
            //4/14/2002 10:24AM %%  I removed this line, but then 5/16/2002 2:30PM I put it back.
            //basically, it does the right thing in memory, but when I go to persist, it writes out all the default props.
            // I want to have it walk the chain up to WidgetType to get default props instead. %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            //Persistent.mergeProperties(this, propertiesElement, getID());
            //System.out.println("AFTER props:  ["+getID()+"]\r\n"+getPropertiesTable().toString());
        }
        String fieldName = getPropertyStringValue("field");
        if ( fieldName != null && fieldName.length()>0 ) {
            //build-38-1 removed: setField(m_session.findField(fieldName));
        } else {
            // instead, come up with an audit system for who changes fields: System.out.println("No field for widget: "+m_id);
        }
        //It is tempting to call fire_onLoad() here, but don't.  It is called in Page.loadAllWidgets(),
        // which ensures that all widgets on the page are also loaded first, so that widgets that this
        // Widget may depend upon are available.
        if (DEBUG_PROPS) leaveSetElement(new_value);
    }

    /** this is called by XMLWidgetOutputter as it attempts to output the tree.
     *  We respond by using WebMacro to output the node.
     */
            // %%%%% At some point you should also update the file:
            //           com\dynamide\gui\XHTMLWriter.java
            // since it also processes the 'div' tag directly.
    public void printSerialized(Writer out, int indentLevel/*, NamespaceStack namespaces*/)
    throws Exception{
        printSerialized(out, indentLevel, true);
    }

    public void printSerialized(Writer out, int indentLevel, boolean wantWidgetSpan)
    throws Exception{
        if (m_widgetType == null){
            String errorMessage = "";
            if (m_type.length()==0){
                //out.write("<span class='designModeError'><b>[ERROR: [21] no WidgetType 'type' property found for ("+m_id+") :: "+m_typeError+"]</b></span>");
                errorMessage = "<span class='designModeError'><b>[ERROR: [21] no WidgetType 'type' property found for ("+m_id+") :: "+m_typeError+"]</b></span>";
            } else {
                errorMessage = "<span class='designModeError'><b>[ERROR: [22] WidgetType ("+m_type+") set but WidgetType component not found in("+m_id+") :: "+m_typeError+"]</b></span>";
            }
            throw new Exception(errorMessage);
            //return;
        }
            boolean invisible = getPropertyStringValue("visible").equalsIgnoreCase("false");
            if ( (invisible) && (!m_designMode) ) {
                out.write(" "); //in run mode, and invisible, just get the hell out of Dodge, and save some rendering time.
                return;
            }
            String templateExpanded = "";
            if ( (invisible) && (m_designMode) ) {
                templateExpanded = "<!-- "+m_id+".visible == false -->";
            } else {
                String templateSource = "";
                try {
                    templateSource = m_widgetType.getRawHTMLSource(getSession().getBrowserStringID());
                    int iEndHTML = templateSource.length() > 10 ? templateSource.length()-10 : templateSource.length()-1;
                    if (iEndHTML > -1){
                        String tt = templateSource.substring(iEndHTML).trim();
                        //if(tt.length()>0) System.out.println(m_id+" BEFORE WEBMACRO: '"+tt+'\'');
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR 1 in '"+m_id+"': "+Tools.errorToString(e, true)+"]");
                    out.write("<b>[ERROR 1: "+Tools.errorToString(e, true)+"]</b>");
                    return;
                }
                StringList variables = new StringList();
                variables.addObject("widget", this);
                if (page != null){
                    variables.addObject("page", page);
                    variables.addObject("pageID", page.getName());
                    variables.addObject("parent", null);
                }
                templateExpanded = getSession().expandTemplate(variables, templateSource, getDotName()); //logname was m_id 6/4/2003 10:00AM

                int iEndHTML = templateExpanded.length() > 10 ? templateExpanded.length() - 10 : templateExpanded.length()-1;
                if (iEndHTML > -1){
                    String tt = templateExpanded.substring(iEndHTML).trim();
                    //if(tt.length()>0) System.out.println(m_id+" AFTER WEBMACRO: '"+templateExpanded.substring(iEndHTML)+'\'');
                }
                //last char is always bad:
                templateExpanded = templateExpanded.substring(0, templateExpanded.length()-1);
            }


            // %% if (widget.isStretchable) { type = "div"; } else {type = "span";}
            String designModeJavascript = m_designMode
                 ?  " CONTENTEDITABLE='false'"
                   +" onresizestart='javascript:event.cancelBubble=true;return false;'"
                   +" onclick='widgetClicked(this)'"
                 : "";
            //the "onresizestart return false" business makes it so the component can't be resized,
            // though sadly, you can still see the arrow cursors.  The answer is probably behaviors.
            boolean bRaw = getPropertyStringValue("raw").equalsIgnoreCase("true");
            if (bRaw){
                String designModeJavascript2 = m_designMode
                 ?  " onresizestart='javascript:event.cancelBubble=true;return false;'"
                   +" onclick='widgetClicked(this)'"
                 : "";
                out.write("<span class='widgetContainer' id='"+getID()+'\''
                                  +" CONTENTEDITABLE='true'"
                                  +designModeJavascript
                                  +'>'+templateExpanded+"</span>");
            } else if (wantWidgetSpan){
                out.write("<span class='widget' id='"+getID()+'\''
                                  +designModeJavascript
                                  +'>'+templateExpanded+"</span>");
            } else {
                out.write(templateExpanded);
            }
            out.write("\r\n");
    }

    public String render(){
        return render(true);
    }

    public String render(boolean wantWidgetSpan){
        try {
            fire_beforeOutput();
            StringWriter writer = new StringWriter();
            printSerialized(writer, 0, wantWidgetSpan);
            writer.close();
            String res = writer.toString();
            return res;
        } catch (Exception e){
            return "ERROR: [109] Exception writing widget: "+e.toString();
        }
    }

    public ScriptEvent fire_onLoad(){
        WidgetType wt = getWidgetType();
        wt.fire_onLoad(this);

        String eventName = getName()+"_onLoad";
        //logDebug(eventName);
        //System.out.println("Widget.onLoad "+getDotName()+" ::\r\n"+Tools.getStackTrace());
        Page page = getPage();
        if ( page != null ) {
            return getSession().fireEvent(this, eventName, "", "", "", page.getEventSource(eventName), page.getFilename(), false);
        }
        return null;
    }

    public ScriptEvent fire_beforeOutput(){
        String eventName = getName()+"_beforeOutput";
        Page page = getPage();
        if ( page != null ) {
            return getSession().fireEvent(this, eventName, "", "", "", page.getEventSource(eventName), page.getFilename(), false);
        }
        return null;
    }

    public Object call(String eventFullName) {
        Object res = call(eventFullName, new Object());
        return res;
    }

    public Object call(String eventFullName, Object inputObject) {
        ScriptEventSource eventSource = m_widgetType.getEventSource(eventFullName);
        Object res = getSession().fireEvent(this, inputObject, eventFullName, ""/*pageID*/, ""/*nextPageID*/, ""/*action*/, eventSource, getFilename(), false, null, "").getOutputObject();
        return res;
    }

    /** Fires a "class event", that is, fires an event defined in the widget type source
     *  (e.g int com.dynamide.edit.xml which looks like com_dynamide_edit_onLoad),
     *  but passes in an instance of the actual Widget; This is different from calling a wired Widget event, which is
     *  in the Page source (e.g. page1.xml, which looks like page1_datasource1_beforePost); If you wish to
     *  do the latter, use the overload that has no pageID.
     */
    public ScriptEvent fireEvent(Object inputObject, String eventFullName, String pageID, String action) {
        ScriptEventSource eventSource = m_widgetType.getEventSource(eventFullName);
        return getSession().fireEvent(this, inputObject, eventFullName, pageID, ""/*nextPageID*/, action, eventSource, getFilename(), false, null, "");
    }

    public ScriptEvent fireEvent(Object inputObject, String eventShortName) {
        Page page = getPage();
        if ( page != null ) {
            String eventFullName = eventShortNameToFullName(eventShortName);
            ScriptEventSource eventSource = page.getEventSource(eventFullName);
            return getSession().fireEvent(this, inputObject, eventFullName, page.getID(), "", "", eventSource, getFilename(), false, null, "");
        }
        ScriptEvent event = new ScriptEvent();
        event.evalErrorMsg = "Widget.page is not set: can't fire event";
        event.resultCode = ScriptEvent.RC_ERROR;
        event.eventName = eventShortName;
        return event;
    }



}
