package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Property;
import com.dynamide.Session;

import com.dynamide.util.Tools;

import org.jdom.Element;

public class IntegerDatatype extends Datatype {
    public IntegerDatatype(){
    }

    public IntegerDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }
    public ValidationResult validate(){
        ValidationResult res = super.validate(); //check for empty required values, etc.
        if (res.getValid() ) {
            return res;
        }
        Property property = getProperty();
        String value = property.getStringValue();  // %% you really want to check the newValue versus the oldValue and veto it...
        int iValue = 0;
        try {
            iValue = Tools.stringToInt(value);
        } catch (NumberFormatException ce){
            return new ValidationResult(false, "New value is not an integer");
        }
        String max = property.get("max").toString();
        if ( max.length()>0 ) {
            try {
                if ( Tools.stringToInt(max) < iValue ) {
                    return new ValidationResult(false, "New value is greater than maximum value");
                }
            } catch (NumberFormatException ce2){
                return new ValidationResult(false, "Max value is not an integer");
            }
        }
        String min = property.get("min").toString();
        if ( min.length()>0 ) {
            try {
                if ( Tools.stringToInt(min) > iValue ) {
                    return new ValidationResult(false, "New value is greater than minimum value");
                }
            } catch (NumberFormatException ce3){
                return new ValidationResult(false, "Min value is not an integer");
            }
        }
        return new ValidationResult(true);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element.
        String val = element.getText();
        // %% TODO...
    }

    public void addXMLContentTo(Element container){
        System.out.println("ERROR IntegerDatatype.addXMLContentTo not implemented");
    }


}
