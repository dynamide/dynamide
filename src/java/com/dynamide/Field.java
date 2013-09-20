/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import com.dynamide.datatypes.DatatypeException;

import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

import org.jdom.Element;
import org.jdom.JDOMException;

public class Field extends Persistent {

    public Field(DynamideObject owner, Session session){
        super(owner, session);
        this.m_session = session;
    }

    public Field(DynamideObject owner, Session session, String name, Object value)
    throws DatatypeException {
        this(owner, session);
        name = name.toUpperCase(); //CASE-INSENSITIVE
        addProperty("name", new Property(this, "name", name));
        addProperty("value", new Property(this, "value", value));
    }

    public Field(DynamideObject owner, Session session, Element fieldElement, String ownerID)
    throws JDOMException, DatatypeException {
        this(owner, session);
        String name = fieldElement.getAttributeValue("name");
        name = name.toUpperCase(); //CASE-INSENSITIVE
        addProperty("name", new Property(owner, "name", name));  // %% this means you don't look at the whole property element in <field>
        addProperties(this, fieldElement, "Field[oid:"+id+"]::"+ownerID);
    }

    public static String formatFullFieldName(String datasource, String fieldName, String fieldIndex){
        String result = fieldName;
        if ( datasource != null && datasource.length() > 0 ) {
            result = datasource + '$' + result;
        }
        if ( fieldIndex != null && fieldIndex.length() > 0 ) {
            result = result + '$'+fieldIndex;
        }
        result = result.toUpperCase();
        //System.out.println("formatFullFieldName: "+datasource+" fieldName: "+fieldName+" result: "+result);
        return result;  //CASE-INSENSITIVE
    }

    private Session m_session;

    public String getName(){
        return get("name").toString();
    }

    public String getID(){
        return ""+get("name")+'['+getObjectID()+']';
    }

    public Object get(String which){
        StringList proptable = getPropertiesTable();
        Property nameProp = (Property)proptable.get("name", false/*ignore case*/);
        String name = nameProp != null ? nameProp.getStringValue() : "Field" ;
        String look = (m_session == null ? null : m_session.lookupInternationalizedValue(name+'.'+which));
        if ( look != null && look.length() > 0 ) {
            return look;
        }
        Property res = (Property)proptable.get(which, true/*ignore case*/);
        if ( res == null ) {
            return "";
        }
        //return res.getStringValue();  4/26/2003 4:43PM
        return res.getValue();
    }

    public Object getValue(){
        return get("value");
    }

    public Object getValueObject(String name){
        return getPropertiesTable().get(name);
    }

    public void clear(){
        set("value", "");
        clearError();
    }

    public void setValue(Object value){
        set("value", value);
    }

    public void set(String name, Object value) {
        try {
            uncaughtSet(name, value);
        } catch( Exception e ){
            logError("setting value in Field.set "+Tools.errorToString(e, true));
        }
    }

    public void uncaughtSet(String name, Object value) throws DatatypeException {
        Object res = getPropertiesTable().get(name);
        if ( res != null ) {
            getPropertiesTable().remove(name);
        }
        if ( name.equals("name") ) {
            value = value.toString().toUpperCase();
        }
        if ( name.equals("value") ) {
            setError(false);
            set("errorMessage", "");
        }
        addProperty(name, new Property(getOwner(), name, value));
    }

    public boolean isValueTrue(String propertyName){
        return get(propertyName).toString().equalsIgnoreCase("true");
    }
    public String getStringValue(){
        return get("value").toString();
    }

    public boolean getEnabled(){
        return get("enabled").equals("true");
    }
    public void setEnabled(boolean new_value){
        set("enabled", ""+new_value);
    }

    public boolean getError(){
        return get("error").equals("true");
    }
    public void setError(boolean newError){
        set("error", ""+newError);
    }

    public String getErrorMessage(){
        return get("errorMessage").toString();
    }
    public void setErrorMessage(String message){
        if (message == null || message.length() ==0){
            setError(false);
            set("errorMessage", "");
        } else {
            setError(true);
            set("errorMessage", message);
        }
    }
    public void clearError(){
        setErrorMessage("");
    }

    public Object id = new Object();

    public String toString(){
        return getStringValue(); //6/29/2003 8:17AM
        //return dumpHTML();
    }

    /** This is implemented to simply compare the String value of the "value" attribute and
     *  nothing else, which makes it possible to compare this field to a string value  in WebMacro.
     *  Any other comparison will not be very good, as two Field objects are not checked for true identity.
     */
    public boolean equals(Object other){
        if (other != null){
            //System.out.println("################ Field.equals #######'"+toString()+"' == '"+other.toString()+"'?");
            if (other.toString().equals(toString())){
                return true;
            }
        }
        return false;
    }

    public String dump(){
        String nl  = "\r\n";
        return "Field: "+get("name")+"; properties: "+getPropertiesTable().toString();
    }

}
