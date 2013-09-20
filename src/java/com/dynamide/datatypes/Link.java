package com.dynamide.datatypes;

import org.jdom.Element;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;
import com.dynamide.util.Tools;

/** <p>Defines the following elements:
 *
<ul>
<li>key</li>
<li>href</li>
<li>target</li>
<li>text</li>
</ul>
 *</p>
 */
public class Link extends ExpandableDatatype {
    public Link(){
    }

    public Link(DynamideObject owner, Session session){
        super(owner, session);
    }

    /*
     *
        <property name="links">
            <datatype>com.dynamide.datatypes.Enumeration</datatype>
            <value>
                <enumeration override="true">
                    <item datatype="com.dynamide.datatypes.Link">
                        <href>${SESSIONARG}&${PAGEARG}&action=com.dynamide.linkstrip&dmLinkstripItem=Contact%20Us</href>
                        <text>Contact Us</text>
                    </item>
     */

    public void init(Element element, String ownerID){   //we've been passed the <item> element.
        set("key", JDOMFile.safeGetElementChildText(element, "key"));
        set("href", JDOMFile.safeGetElementChildText(element, "href"));
        set("target", JDOMFile.safeGetElementChildText(element, "target"));
        set("text", JDOMFile.safeGetElementChildText(element, "text"));
        set("level", JDOMFile.safeGetElementChildText(element, "level"));
        set("class", JDOMFile.safeGetElementChildText(element, "class"));
    }

    public void addXMLContentTo(Element container){
        Element href = new Element("href");
        href.addContent(getAsStored("href"));
        container.addContent(href);
        Element text = new Element("text");
        text.addContent(getAsStored("text"));
        container.addContent(text);
        Element key = new Element("key");
        key.addContent(getAsStored("key"));
        container.addContent(key);
        Element target = new Element("target");
        target.addContent(getAsStored("target"));
        container.addContent(target);
        Element level = new Element("level");
        level.addContent(getAsStored("level"));
        container.addContent(level);
        Element classEl = new Element("class");
        classEl.addContent(getAsStored("class"));
        container.addContent(classEl);
        Element classCurrentEl = new Element("classCurrent");
        classCurrentEl.addContent(getAsStored("classCurrent"));
        container.addContent(classCurrentEl);
    }

    public String dump(){
        return "Link: key="+getAsStored("key")
                   +" href="+getAsStored("href")
                   +" text="+getAsStored("text")
                   +" target="+getAsStored("target")
                   +" level="+getAsStored("level")
                   +" class="+getAsStored("class")
                   +" classCurrent="+getAsStored("classCurrent");
    }

    public String toString(){
        String key = getAsStored("key");
        if ( Tools.isBlank(key) ) {
            key = com.dynamide.util.StringTools.identifier(getAsStored("text"));
        }
        return key;
    }

    public boolean equals(Object other){
        if (other==null) return false;
        //return get("key").toString().equals(other.toString());
        return toString().equals(other.toString());
    }

    public ValidationResult validate(){
        /*check for:
         *   maxLength
         *   minLength
         *   mask
         */
        return new ValidationResult(true);
    }

    public String getDotName(){
        String dotname;
        if (getOwner()!=null){
            dotname = getOwner().getDotName()+'.'+getAsStored("key");
        } else {
            dotname = "NULL-OWNER."+getAsStored("key");
        }
        return dotname;
    }

}
