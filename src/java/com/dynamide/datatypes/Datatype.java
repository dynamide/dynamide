package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Property;
import com.dynamide.Session;

import com.dynamide.util.Log;
import com.dynamide.util.StringList;

import org.jdom.Element;

public abstract class Datatype extends DynamideObject {
    public Datatype(){
        super(null);
    }

    public Datatype(DynamideObject owner, Session session){
        super(owner);
        m_session = session;
    }

    public void finalize() throws Throwable {
        m_session = null;
        super.finalize();
        //System.out.println("Datatype.finalize");
    }

    public static Datatype getDatatypeInstance(DynamideObject owner, String datatype, Session session){
        if (owner == null){
            Log.warn(Datatype.class, "owner is null in Datatype.getDatatypeInstance: "+datatype);
        }
        String datatypeClassName = getDatatypeClassName(datatype);
        try {
            Class cl  = Class.forName(datatypeClassName);
            Object instance = cl.newInstance();
            if ( instance instanceof Datatype ) {
                Datatype dt = (Datatype)instance;
                dt.setSession(session);
                dt.setOwner(owner);
                return dt;
            }
            if ( instance == null ) {
                Log.error(Datatype.class, "getDatatypeInstance could not instantiate datatype: "+datatype);
                return null;
            }
            //Log.info(Datatype.class, "getDatatypeInstance returning null for non-Datatype class type: "+instance.getClass().getName());
            return null;
        } catch (Throwable e){
            Log.error(Datatype.class, "Couldn't getDatatypeInstance: ", e);
            return null;
        }
    }

    /** @return The class name of a com.dynamide.datatypesDatatype subclass wrapper
     *  or implementor, defaulting to
     *  com.dynamide.datatypes.StringDatatype if not known.
     *  @param datatype You would get this by calling String datatype = property.get("datatype");
     */
    public static String getDatatypeClassName(String datatype){
        if ( datatype.equals("java.lang.String")){
            return "com.dynamide.datatypes.StringDatatype";
        } else if ( datatype.equals("java.lang.Boolean")
                 || datatype.equals("boolean") ){
            return "com.dynamide.datatypes.BooleanDatatype";
        } else if ( datatype.equals("java.lang.Integer")
                 || datatype.equals("int")){
            return "com.dynamide.datatypes.IntegerDatatype";
        } else if (datatype.equals("com.dynamide.datatypes.Enumeration")){
            return "com.dynamide.datatypes.EnumeratedDatatype";
        } else if (datatype.length() > 0){
            return datatype; //Don't need a wrapper, they specified a Datatype subclass directly.
        }
        return "com.dynamide.datatypes.StringDatatype";  //safely default unknown to String wrapper.
    }

    public static String getDatatypeClassName(Datatype dt){
        return getDatatypeClassName(dt.getClass().getName());
    }

    public String getDatatypeClassName(){
        return getDatatypeClassName(this.getClass().getName());
    }

    protected void insertItemDatatype(Element item, Datatype dt){
        String className = getDatatypeClassName(dt.getClass().getName());
        if ( className.equals("com.dynamide.datatypes.StringDatatype") ) {
            return;
        }
        item.setAttribute("datatype", className);
    }

    public void init(Element element, String ownerID) throws DatatypeException {  //subclasses should override... %%
        throw new DatatypeException("WARNING: Datatype ["+getClass().getName()+"] did not init. ownerID: "+ownerID);
    }

    public abstract void addXMLContentTo(Element container);

    /** subclasses can override and replace */
    protected Datatype createClone(){
        try {
            Datatype d = (Datatype)getClass().newInstance();
            d.setOwner(getOwner());
            d.setSession(getSession());
            return d;
        } catch (Exception e){
            logError("[47a] couldn't construct a clone in Datatype: "+e);
            return new StringDatatype(getOwner(), getSession()); //%%%%% arg!! what to do about m_property.  Not safe to copy, yet... maybe not needed.
        }
    }

    public Object clone(){
        try {
            Datatype d = createClone();
            Element element = new Element("temp");
            addXMLContentTo(element);
            String ownerID = getOwner() != null ? getOwner().getDotName() : "";
            d.init(element, ownerID);
            return d;
        } catch (Exception e){
            logError("[47] couldn't construct a clone in Datatype: "+e);
            return new StringDatatype(getOwner(), getSession()); //%%%%% arg!! what to do about m_property.  Not safe to copy, yet... maybe not needed.
        }
    }

    private Session m_session;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    private Property m_property = null;
    public Property getProperty(){return m_property;}
    public void setProperty(Property new_value){m_property = new_value;}

    private StringList m_nameValues = new StringList();

    public String dumpHTML(){
        return m_nameValues.dumpHTML();
    }
    
//  toString will call dump, so just get the value and get out of here.
    public String dump(){
    	return m_nameValues.dump();
    }


    public String getAsStored(String what){
        Object o = m_nameValues.getObject(what);
        return (o==null) ? "" : (String)o;
    }

    public Object get(String what){
        String dotname = getDotName()+'.'+what;
        String newValue = null;
        Session mySession = getSession();
        if (mySession != null){
            newValue = mySession.lookupInternationalizedValue(dotname);
        }
        if (newValue != null && newValue.length() > 0){
            return newValue;
        }
        return getAsStored(what);
    }

    public void set(String what, String value){
        m_nameValues.remove(what);
        m_nameValues.addObject(what, value);
    }


    public ValidationResult validate(){
        Property property = getProperty();
        String value = property.getStringValue();  // %% you really want to check the newValue versus the oldValue and veto it...
        if ( Property.REQUIRED.equalsIgnoreCase(property.get("category").toString()) ) {
            if ( value.length()==0 ) {
                return new ValidationResult(false, "Required value is empty");
            }
        }
        return new ValidationResult(true);
    }

    /* NOT USED 2/8/2004 10:44PM Subclasses, such as WebmacroDatatype can override.
    public String expand(String value){
        return value;
    }
     */

}
