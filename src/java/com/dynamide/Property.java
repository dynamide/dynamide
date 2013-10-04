/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.dynamide.datatypes.StringDatatype;
import org.jdom.Element;

import com.dynamide.datatypes.Datatype;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.datatypes.EnumeratedDatatype;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

/** This class handles reading of XML properties that are embedded in XML files for
 *   Widget and Page (and, in theory, Session).
 *
 *  The format for the properties element is
 *  <pre>
 *     &lt;properties&gt;
 *       &lt;property name="myEventProperty" isEvent="true"&gt;
 *          &lt;defaultValue&gt;&lt;/defaultValue&gt;
 *          &lt;type&gt;com.dynamide.property.EventProperty&lt;/type&gt;
 *       &lt;/property&gt;
 *       &lt;property name="myProperty"&gt;
 *          &lt;defaultValue&gt;Foo&lt;/defaultValue&gt;
 *          &lt;type&gt;java.lang.String&lt;/type&gt;
 *       &lt;/property&gt;
 *     &lt;/properties&gt;
 *  </pre>
 *
 *  The property element contains two attributes: name, and isEvent.
 *
 *  Valid property child nodes are:
 *  <pre>
 *    name
 *    defaultValue
 *    value
 *    designer
 *    datatype
 *    helpTip
 *    category
 *    data
 *  </pre>
 *  However, these are just stored in a StringList, so you can add them without changing the constructor.
 *  You would just need to modify constructor Property(Element) if one of the subpropbs had children.
 *  <br/><br/>
 * There is one property that is not a simple String, and that is "enumeration", which can have "value"
 *   child nodes.  The "value" nodes contain text nodes with the values that make up the enumeration.
 *  The enumeration is to support enumerated types, the datatype must be "com.dynamide.datatypes.EnumeratedDatatype".
 *  Here's an example:
 *  <pre>
 *     <properties>
 *       &lt;property name="myProperty"&gt;
 *          &lt;defaultValue&gt;Foo&lt;/defaultValue&gt;
 *          &lt;type&gt;java.lang.String&lt;/type&gt;
 *          &lt;datatype&gt;<b>enumeration</b>&lt;/datatype&gt;
 *          <b>&lt;enumeration&gt;</b>
 *              &lt;value&gt;left&lt;/value&gt;
 *              &lt;value&gt;right&lt;/value&gt;
 *              &lt;value&gt;top&lt;/value&gt;
 *              &lt;value&gt;bottom&lt;/value&gt;
 *          <b>&lt;/enumeration&gt;</b>
 *       &lt;/property&gt;
 *     &lt;/properties&gt;
 *  </pre>
 *  Property handles this subproperty specially in the Constructor.
 */
public class Property extends DynamideObject {
    public Property(DynamideObject owner) {
        super(owner);
        m_created = "by default constructor";
    }

    public Property(DynamideObject owner, String name, Object value) throws DatatypeException {
        super(owner);
        m_name = name;
        m_subprops.addObject("value", value);
        m_created = "by name-value constructor";
    }

    public Property(DynamideObject owner, Session session, Element propertyElement)  throws DatatypeException {
        this(owner, session, propertyElement, "", "");
        setSession(session);
        // %% setOwnerName(ownerName);
    }
    /** Handles creating a Property from an xml Element.  All child nodes are taken
     *  as "subproperties", just simple String values, except for "enumeration" which
     *  can have "value" child nodes.  Any single-node child of "property" that you add
     *  will automatically become a subproperty.
     */

    //OK: this should be revamped: when you encounter a <property> subprop, read it
    // in in at least xml element format, and preferably deserialize them into object types.
    // enumeration just becomes an enumerated type, but you would want to support hashtable-like collections
    // of objects, with subprops and so on.  Think of the links collection: display name, url, alignment, that three
    // strings per node.  Maybe you can use Composite...

    //Probably the type to deal with the subproperties is the EditorType, each type streaming itself
    // in and out.  Even though the designer would present the choices and build the Type, the
    //  Type knows its persistence.  It stays in memeory as a subtype.  StringType just returns the
    //  String from getValue.
    // Each Type.xml should customize the javascript events so that on change fires correctly...
    /*
     *
        <property name="links">
            <datatype>com.dynamide.datatypes.Enumeration</datatype>
            <data>
                <enumeration override="true">
                    <item type="com.dynamide.datatypes.Link">
                        <href>${SESSIONARG}&${PAGEARG}&action=com.dynamide.linkstrip&dmLinkstripItem=Contact%20Us</href>
                        <text>Contact Us</text>
                    </item>
     */
    public Property(DynamideObject owner, Session session, Element propertyElement, String ownerID, String location)
    throws DatatypeException {
        super(owner);
        setSession(session);

        if ( session != null ) {
            try {
                String href = propertyElement.getAttributeValue("href");
                if ( href != null && href.length()>0) {
                    String source = session.getAppFileContent(href);
                    if ( source != null && source.length()>0) {
                        //Element el = new Element("property");
                        //el.setAttribute("name", propertyElement.getAttributeValue("name"));
                        //el.addContent(source);

                        JDOMFile jdf = new JDOMFile(null);
                        jdf.readFromString(source);
                        propertyElement = jdf.getRootElement();
                        //logDebug("Using href for property: "+JDOMFile.output(propertyElement));
                    } else {
                        logError("Couldn't use href for property, source was not available: "+href);
                    }
                }
            } catch (Exception e)  {
                logError("Couldn't get property href resource", e);
            }
        } else {
            logError("Session was null in "+getDotName()+" when constructing new Property");
        }


        m_isEvent = Tools.isTrue(propertyElement.getAttributeValue("isEvent"));
        m_name = propertyElement.getAttributeValue("name");
        m_ownerID = ownerID;
        String datatype = JDOMFile.safeGetElementChildText(propertyElement,"datatype"); //returns at least an "".
        //it will also get added as a subprop, below, which is good in the Detail page, but here we just want the value.
        if ( datatype.length() == 0 ) {
            datatype = "java.lang.String";
        }

        //Deal with value and defaultValue:
        String valueSrc = "value";
        Element data = propertyElement.getChild("value");
        if (data == null){
            valueSrc = "defaultValue";
            data = propertyElement.getChild("defaultValue");
        }
        if (data == null){
            logWarn("[23] no value or defaultValue element for "+m_name+" ownerID: "+ownerID);
            //+" owner: "+owner
            //+" trace: "+Tools.getStackTrace());
        } else {
            String fullname = /*owner.getName()+*/ m_name;
            //System.out.println("Property creating dt: "+getOwner().getDotName()+'.'+m_name);
            Datatype dt = Datatype.getDatatypeInstance(this, datatype, getSession());
            if (dt != null){
                String ownerDotname = owner != null ? owner.getDotName() : "";
                dt.init(data, ownerID+" ["+ownerDotname+"] ["+location+"]");
                m_subprops.addObject(valueSrc, dt);
                //noisy!:
                //System.out.println("m_subprops.addObject: "+m_name+'.'+valueSrc+" "+dt.getClass().getName());
            } else {
                try {
                    Class.forName(datatype);
                } catch (Throwable e){
                    throw new DatatypeException("Couldn't load datatype class: "+datatype);
                }
            }
        }

        //Deal with all other arbitrary subprops, notably, "readOnly", and including "datatype", but also any others.
        //All will be dealt with as String (name-value) subprops.
        List properties = propertyElement.getChildren();
        Iterator i = properties.iterator();
        while (i.hasNext()) {
            Element subpropEl = (Element) i.next();
            String subpropName = subpropEl.getName();
            if ( subpropName.equals("value") || subpropName.equals("defaultValue") ) {
                continue;
            } else {
                //All others are simple string subprops.
                String subpropValue = subpropEl.getText();
                m_subprops.addObject(subpropName, subpropValue);
            }
        }
        m_created = "by element constructor";
    }

    public void finalize() throws Throwable {
        //System.out.println("Property.finalize");
        setSession(null);
        setOwner(null);
        super.finalize();
    }


    public static Property createFromXML(Element propertyElement)
    throws Exception {
        return new Property(null, (Session)null, propertyElement);
    }

    public static Property createFromXML(String xmlFilename)
    throws Exception {
        JDOMFile jdf = new JDOMFile(null, xmlFilename);
        Element propertyElement = jdf.getRootElement();
        return createFromXML(propertyElement);
    }

    private Session m_session = null;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    public String getDotName(){
        return getOwner() != null
            ? getOwner().getDotName()+'.'+get("name")
            : "null-owner."+get("name"); //+getAsStored ??
    }

    private String m_created = "";
    private String m_ownerID = "";
    private StringList m_subprops = new StringList();

    private String m_name = "";
    public String getName(){ return m_name;}
    public void setName(String new_name){
        m_name = new_name;
    }


    public Object clone() {
        Element propertyEl = new Element("property");
        propertyEl.setAttribute("name", m_name);
        propertyEl.setAttribute("isEvent", ""+m_isEvent);
        int m_subprops_size = m_subprops.size();
        for (int i=0; i < m_subprops_size; i++) {
            String subname = m_subprops.getString(i);
            String val;
            Element subpropEl = new Element(subname);
            propertyEl.addContent(subpropEl);
            Object o = m_subprops.getObjectAt(i);
            if ( o instanceof String ) {
                val = (String)o;
                subpropEl.addContent(val);
            } else {
                Datatype dt = (Datatype)o;
                dt.addXMLContentTo(subpropEl);
            }
        }
        Property copy = null;
        try {
            copy = new Property(getOwner(), getSession(), propertyEl, "clone."+m_ownerID, "");
        }
        catch( DatatypeException e){
            logError("couldn't throw exception from Property.clone(): "+Tools.errorToString(e, true));
        }
        return copy;
    }

    public Datatype getDatatype(){
        Object obj = getValue();
        if (obj != null && obj instanceof Datatype){
            return (Datatype)obj;
        }
        return new StringDatatype();
    }

    /** Returns the Object stored in "value", or, if that is null, reutrns the default value.
     *  WebMacro requires this to be available if setValue() is available, since it thinks it is a bean;
     *  this method simply calls getValue(), you should call getStringValue() if you specifically need a String.
     */

    public Object getValue(){
        Object obj = m_subprops.get("value");
        if (obj != null){
            return obj;
        } else {
            obj = getDefaultValue();
            //%%System.out.println("****  Property returning defaultValueObject: "+obj.getClass().getName());
            return obj;
        }
    }

    public Object getValueObjectNoDefault(){
        return m_subprops.get("value");
    }

    public Object getDefaultValue(){
        Object objdef = m_subprops.get("defaultValue");
        if (objdef != null){
            return objdef;
        } else {
            return null;
        }
    }


    public void setValue(Object value){
        Object obj = m_subprops.get("value", true);
        if ( obj != null ) {
            m_subprops.remove("value");
        }
        m_subprops.addObject("value", value);
        //System.out.println("WARNING: Property.setValue("+value+") was called, but you should implement setting Datatype values, especially enumerated ones, with the index");
    }

    public void set(String key, Object value){
        Object obj = m_subprops.get(key, true);
        if ( obj != null ) {
            m_subprops.remove(key);
        }
        m_subprops.addObject(key, value);
    }

    public Object get(String what){
        String res = "";
        if ( what.equalsIgnoreCase("name") ) {
            res = getName();
        } else {
            Object obj = m_subprops.get(what, true);
            if ( obj != null ) {
                return obj; //11/29/2003 10:01AM //res = obj.toString();
            }
        }
        //System.out.println("Property["+m_name+"].get("+what+") == "+res +" ("+m_created+")");
        return res;
    }

    public boolean isValueTrue(String subPropertyName){
        Object o = get(subPropertyName);
        if ( o != null ) {
            return Tools.isTrue(o.toString());
        }
        return false;
    }

    public String getStringValue(){
        Object res = getValue();
        String result =  res != null
        ? res.toString()
        : "";
        /* NOT USED 2/8/2004 10:44PM (I put it in but backed it out right away.)
         * Datatype dt = getDatatype();
        if (dt!=null){
            result = dt.expand(result);  //may use webmacro
        }
        */
        return result;
    }

    public String getCurrentStringValue(){
        return getCurrentValue().toString();
    }

    public Object getCurrentValue(){
        Object obj = m_subprops.get("value");
        if (obj != null){
            return obj;
        } else {
            Object objdef = m_subprops.get("defaultValue");
            if (objdef != null){
                return objdef;
            } else {
                return "";  //better than null
            }
        }
    }

    public int getEnumerationDefaultIndex(){
        Object obj = getDefaultValue();
        if (obj != null && obj instanceof Datatype){
            Datatype dt = (Datatype)obj;
            if (dt instanceof EnumeratedDatatype){
                return ((EnumeratedDatatype)dt).getDefaultIndex();
            }
        }
        return -1;
    }

    public Enumeration getEnumeration()
    throws Exception {
        Object obj = getValue();
        if (obj != null && obj instanceof Datatype){
            Datatype dt = (Datatype)obj;
            if (dt instanceof EnumeratedDatatype){
                return ((EnumeratedDatatype)dt).getEnumeration();
            }
        }
        return (new Vector()).elements();
    }

    public Collection getCollection(){
        Object obj = getValue();
        if (obj != null && obj instanceof Datatype){
            Datatype dt = (Datatype)obj;
            if (dt instanceof EnumeratedDatatype){
                return ((EnumeratedDatatype)dt).getCollection();
            }
        }
        return new Vector();
    }

    public EnumeratedDatatype getEnumeratedDatatype(){
        Object obj = getValue();
        if (obj != null && obj instanceof EnumeratedDatatype){
            return (EnumeratedDatatype)obj;
        }
        return null;
    }

    public String toString(){
        return getCurrentStringValue();//11/28/2003 1:56PM
        //11/28/2003 1:56PM return dump();
        //return getCurrentValue();//tried  in 3/18/2002 9:12AM build-38-1 as a test along with modifying
                                   //AbstractWidget.get to return the Property object, but it doesn't work.
    }

    public String dumpHTML(){
        m_subprops.sort();
        return m_subprops.dumpHTML();//+m_created; turn this on if you are unclear about which constructor created this property.
    }

    public String dump(){
        StringBuffer buff = new StringBuffer();
        buff.append('{'+m_name+' ');
        if ( isEvent() ) {
            buff.append(" isEvent:true ");
        }
        m_subprops.sort();
        Object o;
        String s;
        int m_subprops_size = m_subprops.size();
        for (int i=0; i < m_subprops_size; i++) {
            buff.append("{");
            buff.append(m_subprops.getString(i));
            buff.append(":");
            o = m_subprops.getObjectAt(i);
            if (o instanceof DynamideObject){
                s = ((DynamideObject)o).dump();
            } else {
                s = o.toString();
            }
            buff.append(s);
            buff.append("}");
        }
        buff.append("}");
        return buff.toString();
    }

    public String dumpXML(){
        String result = "";
        try {
            result = JDOMFile.output(toElement());
            result = com.dynamide.JDOMFile.prettyPrintHTML(result);
        } catch (Exception e){
            result = "<error>Couldn't dump property: "+e+"</error>";

        }
        return result;
    }

    /** Persist to Element to save any in-memory changes*/
    public Element toElement(){
        Element element = new Element("property");
        element.setAttribute("name", getName());
        if (isEvent()){
            element.setAttribute("isEvent", "true");
        }
        //element.addContent(getValue().toString());//%% change this to IPersitentElement.toElement() or something...
        String name, value;
        Object subprop;
        Element subelement;
        m_subprops.sort();
        int m_subprops_size = m_subprops.size();
        for (int i=0; i < m_subprops_size; i++) {
            name = m_subprops.getString(i);
            subelement = new Element(name);
            element.addContent(subelement);
            subprop = m_subprops.getObjectAt(i);
            if (subprop instanceof Datatype){
                ((Datatype)subprop).addXMLContentTo(subelement); //subelement will probably be "<value>"
                //String dt = subprop.getClass().getName();
                //if ( ! dt.equals("com.dynamide.datatypes.StringDatatype")) {
                //    element.addContent(
                //        (new Element("datatype")).addContent(dt)
                //    );
                //}
            } else {
                //logDebug("Property persisting non-Datatype: "+subprop.getClass().getName());
                subelement.addContent(subprop.toString());
            }
        }
        return element;
    }

    public void overrideValuesFrom(Property newProperty){
        StringList newSubproperties = newProperty.m_subprops;
        int newSubproperties_size = newSubproperties.size();
        for (int i=0; i < newSubproperties_size; i++) {
            String subpropName = newSubproperties.getString(i);
            Object subpropValue;
            Object o = newSubproperties.getObjectAt(i);
            if ( o instanceof Datatype ) {
                subpropValue = ((Datatype)o).clone();
            } else {
                subpropValue = o.toString();
            }
            /*Object old = m_subprops.get(subpropName);
            if ( old != null ) {
                logDebug("old property: "+subpropName+" = "+old.toString());
            }
            */
            m_subprops.remove(subpropName);
            m_subprops.addObject(subpropName, subpropValue);
            //logDebug("Property.overrideValuesFrom:"+subpropName+" = "+subpropValue);
        }
    }

    private boolean m_isEvent = false;
    public boolean isEvent(){ return m_isEvent;}
    public void setIsEvent(boolean new_value){
        m_isEvent = new_value;
    }

    private Object m_editor = null;
    public Object getEditor(){return m_editor;}
    public void setEditor(Object new_value){m_editor = new_value;}
    public boolean hasEditor(){return (m_editor != null);}

    //categories:
    public final static String OPTIONAL = "optional";
    public final static String RECOMMENDED = "recommended";
    public final static String REQUIRED = "required";

}
