package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;

import com.dynamide.util.StringTools;

import org.jdom.Element;

/** Use this datatype to store simple Strings.  If you want to store HTML, use com.dynamide.datatypes.HTMLDatatype.
 *  If you want to store a String that has an associated key, such as those required by com.dynamide.radiogroup, use
 *  com.dynamide.datatypes.Caption.
 * <p>Defines the following elements:
 *
 *   <ul>
 *   <li>value</li>
 *   </ul>
 *</p>
 */
public class StringDatatype extends Datatype {
    public StringDatatype(){
    }

    public StringDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public StringDatatype(String value){
        this();
        set("value", value);
    }

    public ValidationResult validate(){
        /*check for:
         *   maxLength
         *   minLength
         *   mask
         */
        return new ValidationResult(true);
    }

    public void init(Element element, String ownerID) throws DatatypeException { //we've been passed the <value> or <defaultValue> element, or in an enumeration, the <item> element.
        boolean bad = (element.getContentSize()>1); //hasChildren();
        if ( bad ) {
            String dn = "";
            try {
                dn = getDotName();
            }
            catch( Exception e ){
            }
            throw new DatatypeException("String datatype '"+dn+"' had child elements.  Is it another datatype?\r\n"+StringTools.escape(JDOMFile.output(element)));
        }
        String val = element.getText();
        set("value", val);
    }

    public void addXMLContentTo(Element container){
        container.addContent(getAsStored("value"));
    }

    // throws DatatypeException
    protected Datatype createClone(){
        return new StringDatatype(getOwner(), getSession());
    }

    public String toString(){
        return get("value").toString();
    }

    public boolean equals(Object other){
        if (other==null) return false;
        return get("value").toString().equals(other.toString());
    }

    public String getDotName(){
        String dotname;
        if (getOwner()!=null){
            String storedName = getAsStored("name");
            if (storedName.length()>0) {
                storedName = '.'+storedName;
                System.out.println("StringDatatype.name: "+storedName);
            }
            dotname = getOwner().getDotName()+storedName;
        } else {
            dotname = "NO-OWNER:"+'.'+getAsStored("key")+".text";
        }
        return dotname;
    }


}
