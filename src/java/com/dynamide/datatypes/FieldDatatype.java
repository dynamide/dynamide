package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

import org.jdom.Element;

public class FieldDatatype extends Datatype {
    public FieldDatatype(){
    }

    public FieldDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element.
        String val = element.getText();
        set("value", val);
    }

    public void addXMLContentTo(Element container){
        container.addContent(get("value").toString());
    }

    public String toString(){
        return get("value").toString();
    }


}
