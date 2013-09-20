package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

import org.jdom.Element;

public class ServerSideEvent extends ExpandableDatatype {
    public ServerSideEvent(){
    }

    public ServerSideEvent(DynamideObject owner, Session session){
        super(owner, session);
    }

    public ValidationResult validate(){
        ValidationResult res = super.validate(); //check for empty required values, etc.
        if (res.getValid() ) {
            return res;
        }
        return new ValidationResult(true);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element.
        String val = element.getText();
        set("value", val);
    }

    public void addXMLContentTo(Element container){
        container.addContent(get("value").toString());
    }
    
    
    public String toString(){
    	Object o = get("value");
    	if (o instanceof String) {
			return (String)o;
		}
    	System.out.println("ServerSideEvent toString "+o.getClass().getName());
    	return o.toString();
    }


}
