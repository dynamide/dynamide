package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Property;
import com.dynamide.Session;

import com.dynamide.util.Tools;

import org.jdom.Element;

public class SloppyBooleanDatatype extends Datatype {
    public SloppyBooleanDatatype(){
    }

    public SloppyBooleanDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public ValidationResult validate(){
        ValidationResult res = super.validate();
        if (res.getValid() ) {
            return res;
        }
        Property property = getProperty();
        String value = property.getStringValue();
        boolean b = Tools.isTrue(value);
        if ( b ) {
            property.setValue("true");
        } else {
            property.setValue("false"); //sets even if empty string, hence 'sloppy'.
        }
        return new ValidationResult(true);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element.
        String val = element.getText();
        // %% TODO...
    }

    public void addXMLContentTo(Element container){
        System.out.println("ERROR SloppyBooleanDatatype.addXMLContentTo not implemented");
    }

}
