package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;

import org.jdom.Element;

/**
 * <p>Defines the following elements:
 *
<ul>
<li>value</li>
</ul></p>
 */
public class HTMLDatatype extends StringDatatype {
    public HTMLDatatype(){
    }

    public HTMLDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public HTMLDatatype(String value){
        this();
        set("value", value);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element, or in an enumeration, the <item> element.
        String val = JDOMFile.output(element);//.getText();
        set("value", val);
    }


    public void addXMLContentTo(Element container){
        container.addContent(getAsStored("value"));
    }

}
