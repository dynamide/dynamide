package com.dynamide.datatypes;

import org.jdom.Element;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;

/** A datatype to indicate to the IDE that the property points to a Widget
 *  or a specific class of Widget or a specific interface of Widget.
 * <p>Defines the following elements:
 *
<ul>
<li>name</li>
<li>interface</li>
</ul>
 </p>
 */
public class WidgetDatatype extends ExpandableDatatype{
    public WidgetDatatype (){
    }


    public WidgetDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }
    /*   Example:
     *
        <property name="items">
            <datatype>com.dynamide.datatypes.Enumeration</datatype>
            <value>
                <enumeration override="true">
                    <item datatype="com.dynamide.datatypes.WidgetDatatype">
                        <name>datasource1</name>
                        <interface>datasource</interface>
                    </item>
             ...
     */

    public WidgetDatatype (String key, String interfaceText){
        set("key", key);
        set("interface", interfaceText);
    }

    public void init(Element element, String ownerID){   //we've been passed the <item> element.
        set("key", JDOMFile.safeGetElementChildText(element, "key"));
        set("interface", JDOMFile.safeGetElementChildText(element, "interface"));
    }

    public void addXMLContentTo(Element container){
        Element key = new Element("key");
        key.addContent(getAsStored("key"));
        container.addContent(key);
        String itf = getAsStored("interface");
        if ( itf != null && itf.length()>0 ) {
            Element text = new Element("interface");
            text.addContent(itf);
            container.addContent(text);
        }
    }

    /** override to handle setting single string values.
     */
    public void set(String what, String value){
        if (what.equals("value")){
            super.set("key", value);
        } else {
            super.set(what, value);
        }
    }


    public String toString(){
        return getAsStored("key");
    }

    public ValidationResult validate(){
        return new ValidationResult(true);//always OK
    }

    public String getDotName(){
        String dotname;
        if (getOwner()!=null){
            dotname = getOwner().getDotName()+'.'+getAsStored("key");//+".text";
        } else {
            dotname = "NO-OWNER"+'.'+getAsStored("key");//+".text";
        }
        return dotname;
    }

}
