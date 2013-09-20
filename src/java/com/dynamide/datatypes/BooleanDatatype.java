package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Property;
import com.dynamide.Session;

import org.jdom.Element;

public class BooleanDatatype extends Datatype {
    public BooleanDatatype(){
    }

    public BooleanDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public ValidationResult validate(){
        ValidationResult res = super.validate();
        if (res.getValid() ) {
            return res;
        }

        Property property = getProperty();
        String value = property.getStringValue();  // %% you really want to check the newValue versus the oldValue and veto it...
        if ( value.equalsIgnoreCase("TRUE") ) {
            property.setValue("true");
            return new ValidationResult(true);
        }
        if ( value.equalsIgnoreCase("FALSE") ) {
            property.setValue("false");
            return new ValidationResult(true);
        }
        return new ValidationResult(false, "Value should be 'true' or 'false'");
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
