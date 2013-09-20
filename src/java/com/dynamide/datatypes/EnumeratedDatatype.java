package com.dynamide.datatypes;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Element;

import com.dynamide.DynamideObject;
import com.dynamide.Session;
import com.dynamide.util.Tools;

public class EnumeratedDatatype extends Datatype {
    public EnumeratedDatatype(){
    }

    public EnumeratedDatatype(DynamideObject owner, Session session){
        super(owner, session);
        //printf("Creating EnumeratedDatatype: OID: %s ownerclass: %s ownerOID: %s", new Object[]{getObjectID(), owner.getDotName(), owner.getObjectID()});
    }

    private int m_defaultIndex = 0;
    public int getDefaultIndex(){return m_defaultIndex;}
    public void setDefaultIndex(int new_value){m_defaultIndex = new_value;}


    private boolean m_override = false;
    public boolean getOverride(){return m_override;}
    public void setOverride(boolean new_value){m_override = new_value;}

    private boolean m_final = false;
    public boolean getFinal(){return m_final;}
    public void setFinal(boolean new_value){m_final = new_value;}

    private String m_itemDatatype = "";
    public String getItemDatatype(){return m_itemDatatype;}
    public void setItemDatatype(String new_value){m_itemDatatype = new_value;}

    private Vector m_list = new Vector();
    public Enumeration getEnumeration()
    throws Exception {
        return m_list.elements();
    }


    /* Non javadoc example:
        <property name="links">
            <datatype>com.dynamide.datatypes.Enumeration</datatype>
            <data>
                <enumeration override="true" final="false" itemDatatype="com.dynamide.datatypes.StringDatatype">
                    <item datatype="com.dynamide.datatypes.Link">
                        <href>${SESSIONARG}&${PAGEARG}&action=com.dynamide.linkstrip&dmLinkstripItem=Contact%20Us</href>
                        <text>Contact Us</text>
                    </item>
     */

    /** Deals with XML structures like this:
     * <pre>
     *     &lt;property name="links"&gt;
     *       &lt;datatype&gt;com.dynamide.datatypes.Enumeration&lt;/datatype&gt;
     *       &lt;value&gt;
     *           &lt;enumeration override="true"  final="false" itemDatatype="com.dynamide.datatypes.StringDatatype" &gt;
     *               &lt;item datatype="com.dynamide.datatypes.Link"&gt;
     *                   &lt;href&gt;${SESSIONARG}&${PAGEARG}&action=com.dynamide.linkstrip&dmLinkstripItem=Contact%20Us&lt;/href&gt;
     *                   &lt;text&gt;Contact Us&lt;/text&gt;
     *               &lt;/item&gt;
     *         ...
     * </pre>
     */
    public void init(Element element, String ownerID) throws DatatypeException {
        //we've been passed the <value> or <defaultValue> element.
        Element enumeration = element.getChild("enumeration");
        if ( enumeration != null ) {
            //deal with "override" attribute, store as a member.
            m_override = Tools.isTrue(enumeration.getAttributeValue("override")); //isTrue deals with null by returning false.
            m_final = Tools.isTrue(enumeration.getAttributeValue("final")); //isTrue deals with null by returning false.
            //System.out.println("enumObj: "+element.getName()+'.'+enumeration.getAttributeValue("defaultIndex"));
            m_defaultIndex = Tools.stringToIntSafe(enumeration.getAttributeValue("defaultIndex"), 0); //stringToIntSafe deals with null by returning 0.
            m_itemDatatype = enumeration.getAttributeValue("itemDatatype");
            if ( m_itemDatatype == null ) {
                m_itemDatatype = "";
            }
            List items = enumeration.getChildren();  //the child nodes can be called anything, so long as they have a datatype attribute,
                //And so long as that datatype exists as a class on the classpath.
            Iterator i = items.iterator();
            while (i.hasNext()) {
                Element item = (Element) i.next();
                String datatype = item.getAttributeValue("datatype");
                if (datatype == null){
                    datatype = "java.lang.String";
                    //System.out.println("ERROR (continuing): datatype attribute is null in "+element.getName()+"."+item.getName());
                    //continue;
                }
                Datatype dt = Datatype.getDatatypeInstance(getOwner(), datatype, getSession());
                if (dt == null){
                    System.out.println("Datatype was null (not found or loaded): "+datatype);
                    continue;
                }
                dt.init(item, ownerID);
                m_list.addElement(dt);
                //System.out.println("dt: "+dt.toString());
            }
        }
        //System.out.println("init: "+getDotName()+" : "+getObjectID());
    }

    //============== methods, alphabetically ===============================


    /** Example of how to call this in a beanshell ServerSideEvent:
     * <pre>
     *
     *   items = event.currentPage.get("com_dynamide_select1").getProperty("items");
     *   //   getValueObject will return EnumerateDatatype instance, of this class.
     *   enumObj = items.getValue();
     *
     *   event.println("enumObj.dumpHTML(): "+enumObj.dumpHTML());
     *
     *   Datatype dtc = Datatype.getDatatypeInstance(null, "com.dynamide.datatypes.Caption", session);
     *   dtc.set("key", "greeting");
     *   dtc.set("text", "Hello, world!");
     *   enumObj.add(dtc);
     *
     *   event.println("enumObj.dumpHTML(): "+enumObj.dumpHTML());
     *
     * </pre>
     *
     * @return the index of the item added.
     */
    public int add(Datatype dt){
        m_list.addElement(dt);
        return m_list.size()-1;
    }

    public int add(int index, Datatype dt){
        if (index<0) index = 0;
        if (index>m_list.size()) index = m_list.size();
        m_list.add(index, dt);
        return index;
    }

    public void add(Collection datatypeCollection){  // %% new and untested
        String s;
        Datatype dt;
        Iterator it = datatypeCollection.iterator();
        while ( it.hasNext() ) {
            dt = (Datatype)it.next();
            add(dt);
        }
    }

    /** Example of how to call this in a beanshell ServerSideEvent:
     * <pre>
     *
     *   items = event.currentPage.get("com_dynamide_select1").getProperty("items");
     *   //   getValueObject will return EnumerateDatatype instance, of this class.
     *   enumObj = items.getValue();
     *
     *   event.print("enumObj.dumpHTML(): "+enumObj.dumpHTML());
     *
     *   Vector v = new Vector();
     *   v.addElement("aaa");
     *   v.addElement("bbb");
     *   v.addElement("ccc");
     *
     *   enumObj.clear();
     *   enumObj.addStrings(v);
     *
     *   event.println("enumObj.dumpHTML(): "+enumObj.dumpHTML());
     *
     * </pre>
     */
    public void addStrings(Collection newStrings){
        String s;
        Datatype dt;
        Iterator it = newStrings.iterator();
        while ( it.hasNext() ) {
            s = it.next().toString();
            dt = Datatype.getDatatypeInstance(this, "com.dynamide.datatypes.StringDatatype", getSession());
            dt.set("value", s);
            add(dt);
        }
    }

    public void addXMLContentTo(Element container){
        Element enumeration = new Element("enumeration");
        container.addContent(enumeration);
        enumeration.setAttribute("override", ""+m_override);
        enumeration.setAttribute("final", ""+m_final);
        if ( m_itemDatatype != null && m_itemDatatype.length()>0 ) {
            enumeration.setAttribute("itemDatatype", m_itemDatatype);
        }
        enumeration.setAttribute("defaultIndex", ""+m_defaultIndex);
        Enumeration enumObj = m_list.elements();
        while ( enumObj.hasMoreElements() ) {
            Datatype dt = (Datatype)enumObj.nextElement();
            Element item = new Element("item");
            super.insertItemDatatype(item, dt);
            dt.addXMLContentTo(item);
            enumeration.addContent(item);
        }
    }

    public void clear(){
        m_list.removeAllElements();
    }

    public int size(){
        return m_list.size();
    }

    public Datatype createClone(){
        EnumeratedDatatype en = new EnumeratedDatatype(getOwner(), getSession());
        en.setOverride(getOverride());
        en.setFinal(getFinal());
        en.setItemDatatype(getItemDatatype());
        en.setDefaultIndex(getDefaultIndex());
        return en;
    }

    public String dumpHTML(){
        return toHTML();
    }

    /** override to handle setting single string values, by just setting a single item with that value
     */
    public Object get(String what){
        //System.out.println("EnumeratedDatatype get ===========");
        if (what.equals("value")){
            Object value = super.get(what);
            if ( value.toString().length()==0 ) {
                value = getDefaultValue();
                logInfo("EnumeratedDatatype returning default value: "+value);
            } else {
                logInfo("EnumeratedDatatype returning value from super: "+value);
            }
            return value;
        } else {
            return super.get(what);
        }
    }

    /*
    @param defaultIndex can be null or empty.
    public Enumeration getEnumeration(Collection defaultColl){
        if (!m_override && defaultColl != null){
            m_list.addAll(0, defaultColl);
        }
        return m_list.elements();
    }
    */

    public Collection getCollection(){
        return (Collection)m_list.clone();
    }


    public Object getValue(int index){
        if ( 0 <= index && index < m_list.size()) {
            return m_list.elementAt(index);
        }
        return "";
    }

    public Object getDefaultValue(){
        if ( 0 <= m_defaultIndex && m_defaultIndex < m_list.size()) {
            return m_list.elementAt(m_defaultIndex);
        }
        return "";
    }

    public int indexOf(Object o){
        int m_list_size = m_list.size();
        for (int i = 0; i < m_list_size; i++){
            Datatype dt = (Datatype)m_list.elementAt(i);
            if ( dt.equals(o) ) {
                return i;
            }
        }
        return -1;
    }

    public void remove(int index){     // %% new and untested
        int m_list_size = m_list.size();
        if (0 <= index && index < m_list_size){
            m_list.removeElementAt(index);
        }
    }

    public void remove(String key){     // %% new and untested
        int m_list_size = m_list.size();
        for (int i = 0; i < m_list_size; i++){
            Datatype dt = (Datatype)m_list.elementAt(i);
            if ( dt.getDotName().equals(key) ) {
                m_list.removeElementAt(i);
                return;
            }
        }
    }

    /*public int searchList(String value){
        int m_list_size = m_list.size();
        for (int i = 0; i < m_list_size; i++){
            Datatype dt = (Datatype)m_list.elementAt(i);
            if ( dt.getValue().equals(value) ) {
                return i;
            }
        }
        return -1;
    } */

    /** override to handle setting single string values, by just setting a single item with that value
     */
    public void set(String what, String value){
        if (what.equals("value")){
            System.out.println(getID()+":"+what+" -- searching EnumeratedDatatype: "+dump()+" for value: "+value);
            int idx = indexOf(value);
            if ( idx > -1 ) {
                setDefaultIndex(idx);
                System.out.println("using index in EnumeratedDatatype: "+idx+" for value: "+value);
                return;
            }
            StringDatatype sdt = (StringDatatype)Datatype.getDatatypeInstance(getOwner(), "java.lang.String", getSession());
            sdt.set("value", value);
            clear(); //remove the default values, since we are doing a single-value set here.
            add(sdt);
        } else {
            super.set(what, value);
        }
    }

    public String toHTML(){
        int idx = 0;
        if (m_list != null){
            StringBuffer sb = new StringBuffer();
            sb.append("EnumeratedDatatype: [<br />\r\n");
            Enumeration keys = m_list.elements();
            while ( keys.hasMoreElements() ) {
                Datatype dt = (Datatype)keys.nextElement();
                if (idx == m_defaultIndex) sb.append("<b>");
                sb.append(dt.toString()) ;
                sb.append(":");
                sb.append(dt.getClass().getName()) ;
                if (idx == m_defaultIndex) sb.append("</b>");
                sb.append("\r\n<br />");
                idx ++;
            }
            sb.append("]");
            sb.append(" defaultIndex:"+m_defaultIndex);
            sb.append(" itemDatatype:'"+m_itemDatatype+'\'');
            sb.append(" final:"+m_final);
            return sb.toString();
        } else {
            return "EnumeratedDatatype (with null list)";
        }
    }

    public String dump(){
        return dump(false);
    }

    public String dump(boolean linebreaks){
        int idx = 0;
        if (m_list != null){
            StringBuffer sb = new StringBuffer();
            sb.append("{EnumeratedDatatype:{");
            Enumeration elems = m_list.elements();
            boolean hasCount = elems.hasMoreElements();
            if ( hasCount && linebreaks ) {
                sb.append("\r\n    ");
            }
            while ( elems.hasMoreElements() ) {
                Datatype dt = (Datatype)elems.nextElement();
                if (idx == m_defaultIndex) sb.append("[");
                sb.append(dt.toString()) ;
                sb.append(":");
                sb.append(dt.getClass().getName()) ;
                if (idx == m_defaultIndex) sb.append("]");
                if ( elems.hasMoreElements() ) {
                    sb.append(",");
                    if ( linebreaks ) {
                        sb.append("\r\n    ");
                    }
                }
                idx ++;
            }
            if ( hasCount && linebreaks ) {
                sb.append("\r\n");
            }
            sb.append("},defaultIndex:"+m_defaultIndex);
            sb.append(",itemDatatype:'"+m_itemDatatype+'\'');
            sb.append(",final:"+m_final+"}");
            return sb.toString();
        } else {
            return "EnumeratedDatatype (with null list)";
        }
    }

    /* %% TODO: public void get(String name){return get element from enumeration whose get("key") matches name;} */

    public String toString(){
        //System.out.println("EnumeratedDatatype toString called: "+dump());
        if (m_list != null){
            int index = 0;
            if ( 0 <= m_defaultIndex && m_defaultIndex < m_list.size() ) {
                index = m_defaultIndex;
            }
            if ( 0 <= index && index < m_list.size() ) {
                Datatype dt = (Datatype)m_list.get(index);
                return dt.toString();
            }
        }
        return "";//EnumeratedDatatype: "+m_list;
    }

    public ValidationResult validate(){
        /*check for:
         *   maxLength
         *   minLength
         *   mask
         */
        return new ValidationResult(true);
    }


}
