package com.dynamide.datatypes;

import org.jdom.Element;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;

/** A simple datatype to wrap a caption string, but adds a key value so you
 *  can use internationalized display text (text) and still have one set of keys (key).
 * <p>Defines the following elements:
 *
<ul>
<li>key</li>
<li>text</li>
</ul>
 </p>
 */
public class Caption extends ExpandableDatatype{
    public Caption (){
    }


    public Caption(DynamideObject owner, Session session){
        super(owner, session);
    }
    /*   Example:
     *
        <property name="items">
            <datatype>com.dynamide.datatypes.Enumeration</datatype>
            <value>
                <enumeration override="true">
                    <item datatype="com.dynamide.datatypes.Caption">
                        <key>contactUS</key>
                        <text>Contact Us</text>
                    </item>
             ...
     */

    public Caption (String key, String text){
        set("key", key);
        set("text", text);
    }

    public void init(Element element, String ownerID){   //we've been passed the <item> element.
        set("key", JDOMFile.safeGetElementChildText(element, "key"));
        set("text", JDOMFile.safeGetElementChildText(element, "text"));
    }

    public void addXMLContentTo(Element container){
        Element key = new Element("key");
        key.addContent(getAsStored("key"));
        container.addContent(key);
        Element text = new Element("text");
        text.addContent(getAsStored("text"));
        container.addContent(text);
    }

    public String toString(){
        return "com.dynamide.datatypes.Caption: key="+getAsStored("key")+" text="+getAsStored("text");
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
