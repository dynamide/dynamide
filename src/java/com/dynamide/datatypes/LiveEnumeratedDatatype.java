package com.dynamide.datatypes;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

import org.jdom.Element;
import org.jdom.Parent;

/** Behaves just like EnumeratedDatatype, but allows the LiveEnumeratedDatatype.editor.xml to be
 *  created with a real Java object behind it, which allows the editor to get the EnumeratedDatatype
 *  associated with the widget defaults.
 */
public class LiveEnumeratedDatatype extends EnumeratedDatatype {
    public LiveEnumeratedDatatype(){
    }

    public LiveEnumeratedDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    private String liveEvalString = "";

    public void init(Element element, String ownerID) throws DatatypeException {
        //we've been passed the <value> or <defaultValue> element.
        Parent parent = element.getParent();
        if ( parent instanceof Element ) {
            Element parentEl = (Element)parent;
            liveEvalString = parentEl.getChild("liveEvalString").getText();
            //System.out.println("LiveEnumeratedDatatype init: "+getDotName()+" : "+getObjectID()+" liveEvalString: "+liveEvalString);
        } else {
            throw new DatatypeException("parent was not an element. owner: "+ownerID+" parent: "+parent);
        }
    }


    public Enumeration getEnumeration()
    throws Exception {
            Vector list = new Vector();
            Object o = null;
            if (liveEvalString.length()>0){
                o = getSession().eval(liveEvalString);
                if (o==null){
                    //Allowed.  Means the eval was present but returned an empty result.  //%% maybe allow session log for app developers to see when they code with empty results. %%
                } else if (o instanceof Enumeration){
                    Enumeration en = (Enumeration)o;
                    while(en.hasMoreElements()){
                        String s = en.nextElement().toString();
                        Datatype dt = Datatype.getDatatypeInstance(getOwner(), "java.lang.String", getSession());
                        dt.set("value",s);
                        list.add(dt);
                    }
                } else if (o instanceof Collection){
                    Iterator it = ((Collection)o).iterator();
                    while(it.hasNext()){
                        String s = it.next().toString();
                        Datatype dt = Datatype.getDatatypeInstance(getOwner(), "java.lang.String", getSession());
                        dt.set("value",s);
                        list.add(dt);
                    }
                } else {
                    logError("Wrong class returned from LiveEnumeratedDatatype.getEnumeration: "+o);
                }
            }
            //System.out.println("done getEnumeration in LiveEnumeratedDatatype");
            return list.elements() ;
    }
}
