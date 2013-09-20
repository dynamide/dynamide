package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

import org.jdom.Element;

/** This is a marker Datatype -- it just marks the property as being
 *  valid Webmacro script, so that the Session knows to eval it.
 *  Note that toString() will NOT cause the macro to be expanded -- use DynamideObject.expand(String).
 * <p>Defines the following elements:
 *
<ul>
<li>value</li>
</ul></p>
 */
public class WebmacroDatatype extends StringDatatype {
    public WebmacroDatatype(){
    }

    public WebmacroDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public WebmacroDatatype(String value){
        this();
        set("value", value);
    }

    public void init(Element element, String ownerID){   //we've been passed the <value> or <defaultValue> element, or in an enumeration, the <item> element.
        String val = element.getText();
        set("value", val);
    }


    public void addXMLContentTo(Element container){
        container.addContent(getAsStored("value"));
    }

    /* This doesn't really work, since the chain of owners may be broken.  2/8/2004 10:44PM
    public String expand(String raw)
    throws DynamideException{
        System.out.println("WebmacroDatatype expand() +++++++++++++++ "+raw);
        return super.expand(raw);
    }
     */

}
