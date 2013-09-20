package com.dynamide;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.jdom.JDOMException;

import com.dynamide.datatypes.Datatype;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.datatypes.WebmacroDatatype;
import com.dynamide.db.IDatasource;
import com.dynamide.event.ChangeEvent;
import com.dynamide.event.IChangeListener;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public abstract class AbstractWidget extends Persistent implements IChangeListener {
    public AbstractWidget(DynamideObject owner, Session session){
        super(owner, session);
        
        //System.out.println("created AbstractWidget: "+getObjectID());
    }

    public AbstractWidget(DynamideObject owner, String filename, Session session)
     throws JDOMException, IOException {
        super(owner, filename, session);
        //System.out.println("created AbstractWidget: "+getObjectID());
    }
    
    public void setTableModel(Object tableModel){
    	this.m_tableModel = tableModel;
    }

    public void finalize() throws Throwable {
        //logDebug("finalize "+getDotName());
        close();
        m_object = null;
        m_tableModel = null;
        super.finalize();
    }

    public void close(){
        //System.out.println("AbstractWidget.close "+getDotName()+" ::\r\n"+Tools.getStackTrace());
        if (changeListeners != null) changeListeners.clear();
        changeListeners = null;
    }


    private Object m_tableModel;
    public Object getTableModel(){return m_tableModel;}

    private boolean m_error = false;
    public boolean getError(){
        Field aField = getField();
        return aField != null ? aField.getError() : m_error;
    }
    public void setError(boolean new_value){
        Field aField = getField();
        if (aField != null) {
            aField.setError(new_value);
        } else {
            m_error = new_value;
        }
    }

    private String m_errorMessage = "";
    public String getErrorMessage()
    throws Exception {
        Field aField = getField();
        if (aField != null){
            return aField.get("errorMessage").toString();
        } else {
            return m_errorMessage;
        }
    }

    public String getErrorMessageHTML(){
        return "<span class='errorCaption'>"+m_errorMessage+"</span>";
    }

    public void setErrorMessage(String message){
        Field aField = getField();
        if (message == null || message.length() ==0){
            setError(false);
            m_errorMessage = "";
            if (aField != null){
                aField.setError(false);
                aField.set("errorMessage", "");
            }
        } else {
            setError(true);
            m_errorMessage = message;
            if (aField != null){
                aField.setError(true);
                aField.set("errorMessage", message);
            }
        }
    }

    public void clearError(){
        setErrorMessage("");
    }
    public void clearErrorMessage(){
        setErrorMessage("");
    }

    private Object m_object;
    /** Widgets may have an underlying Java Object that performs the real work of the Widget.
     * In this case, the Widget author will set the Widget's Object to the Java Object.
     * For example, the com.dynamide.rdbdatasource.xml file defines a Widget, but the work
     * is all done in com.dynamide.db.RDBDatasource.java.  So the xml widget source sets the
     * Object of the Widget on load.  Then, application programmers can get the backing object
     * with code like this: RDBDatasource ds = event.sender.getObject();
     */
    public Object getObject(){
       return m_object;
    }
    public void setObject(Object object){
        m_object = object;
    }

    protected WidgetType m_widgetType;
    public WidgetType getWidgetType(){return m_widgetType;}

    protected String  m_type = "";
    public String  getType(){return m_type;}

    public void setType(String  new_value) throws DatatypeException {
        if (DEBUG_PROPS) System.out.println(m_id+".setType("+new_value+")");
        m_type = new_value;
        m_widgetType = getSession().findWidgetType(m_type);
        if ( m_widgetType == null ) {
            System.out.println("BAD WIDGET TYPE: '"+m_type+'\'');
            DatatypeException de = new DatatypeException("[120] Couldn't find page type '"+m_type+"' in session.  This widget.dotName: "+getDotName()+"  base file: "+getFilename());
            System.out.println("[120] Couldn't find page type '"+m_type+"' in session.  This widget.dotName: "+getDotName()+"  base file: "+getFilename()+" trace: "+Tools.errorToString(de,true));
            throw de;
        }
        StringList newPropertyTable = m_widgetType.clonePropertiesTable();
        resetPropertiesTable(newPropertyTable, m_id+".setType("+new_value+")", this);

        //now set the value, as opposed to the defaultValue.
        Property typeProp = m_widgetType.getPropertyDefault("type");
        typeProp.setValue(m_type);
        setProperty("type", typeProp);
        //System.out.println("typeProp: "+typeProp.dump());

        sortPropertiesTable(); //in Persistent.
        if (changeListeners.size()>0){
            int sz = getPropertiesTable().size();
            for ( int i = 0; i<sz; i++ ) {
                Property prop = (Property)getPropertiesTable().getObjectAt(i);
                notifyChangeListeners(prop.getName(), new Property(null), prop);
                //Any property page holding onto our type must be notified...
            }
        }
    }

    public static String widgetTypeToScriptName(String type){
        String widgetName = type.replace('.', '_');
        widgetName = widgetName.replace('-', '_');
        widgetName = widgetName.replace('/', '_');
        return widgetName;
    }

    private String m_cacheDotName = null;
    public void clearCachedDotName(){
        m_cacheDotName = null;
    }
    public String getDotName(){
        if (m_cacheDotName == null){
            if (getOwner() instanceof Session){
                if (m_id.length()>0){
                    m_cacheDotName = m_id;
                }
            } else {
                m_cacheDotName = getOwner().getDotName()+'.'+m_id;  //+getAsStored ??
            }
        }
        return m_cacheDotName;
    }

    public void setID(String new_value){
        super.setID(new_value);
        clearCachedDotName();
    }

    public Object getValue(){
        try {
            return get("value", true);
        } catch (Exception e)  {
            logError("AbstractWidget.getValue()", e);
            return "ERROR: can't get value in '"+getID()+"'";
        }
    }

    public void setValue(Object newValue){
        try {
            if ( getSession().getDesignMode() == false && getOwner()!=null) {
               autoField();
            }
            Field field = getField();
            if (field!=null){
                field.setValue(newValue);
            } else {
                logError("Could not setValue because "+getID()+" can't find field.");
            }
        } catch (Exception e)  {
            logError("Could not setValue because "+getID()+" threw exception", e);
        }
    }

    /** <p>Handles get("name") and get("field") specially, otherwise performs a lookup in the
     *  persisted and runtime properties, then from the WidgetType type definition as specified
     *  by the widget xml file.
     *  </p>
     *  <p>
     *  get("field") autocreates fields based on parent name and current object name when
     *  NOT in designMode.  So, at runtime, if you didn't set field, it will be created for you
     *  so that widgets persist their values between screens even if you don't feel like adding
     *  a field to the fielddefs.  This only happens at non designTime, so the autocreated fields
     *  do not get persisted to the pagefile or get seen in the property inspector.  In the
     *  property inspector, you will just see "{autocreate}".
     *  </p>
     *  <p>IMPORTANT: No checking is done to see if the field is missing.
     *  If you set the field to a value, then delete the field, the widet will not
     *  have a field.
     *  </p>
     *
     */
     public Object get(String what)
     throws Exception {
        return get(what, true);
     }

     public static final boolean debugFrom = false; //if this is set to false, then code is compiled out.

     public Object get(String what, boolean lookForField)
     throws Exception {
        boolean expandedAlready = false;
        if ( what.equalsIgnoreCase("name") ) {
            return m_id;
        } else if (what.equalsIgnoreCase("errorMessage")){
            return m_errorMessage;
        } else if (what.equalsIgnoreCase("errorMessageHTML")){
            return getErrorMessageHTML();
        }


        String from = "";

        String fullname = getDotName()+'.'+what;
        //logDebug("AbstractWidget.get fullname: "+fullname+" classname: "+getClass().getName());
        //if (this instanceof Widget){
            //logDebug("Widget.page: "+((Widget)this).getPage());
        //}
        String value = null;
        value = getSession().lookupInternationalizedValue(fullname);
        if ( value != null ) {
            return value;
        }

        if ( what.equalsIgnoreCase("field") && getSession().getDesignMode() == false && getOwner()!=null) {
            return autoField();
        }
        if ( what.equalsIgnoreCase("fieldID") ) {
            return getFieldID();
        }

        if (debugFrom) from = getClass().getName();//could be com.dynamide.Widget or com.dynamide.Page.
        //Object propobj = getProperty(what);//begin build-38-1  but it didn't work
        //if (propobj != null) {
        //    if (debugFrom) logError( StringTools.pad(fullname, 50, StringTools.PAD_LEFT)
        //                                  +StringTools.pad("["+from+"::getProperty()] == ", 25, StringTools.PAD_LEFT)
        //                                  +propobj);
        //    return propobj;
        //}                                  //end build-38-1

        Datatype dt = null;
        Property prop = getProperty(what);
        if (prop!=null) {
            value = prop.getStringValue();   //%%%%%%% this should be moved to Property, and add getRawStringValue.....
            expandedAlready = true;
            dt = prop.getDatatype();
            if (dt!=null && dt instanceof WebmacroDatatype){
                value = expand(value);  //use webmacro
            }
        }
        if ( lookForField && (value == null || value.length()==0)  ) {
            Field aField = getField();
            if (aField != null){
                prop = (Property)aField.getValueObject(what);
                if (prop!=null){
                    dt = prop.getDatatype();
                    value = prop.getStringValue();
                    expandedAlready = true;
                    if (dt!=null && dt instanceof WebmacroDatatype){
                        value = expand(prop.getStringValue());  //use webmacro
                    }
                } else {
                    value = aField.get(what).toString();
                }
                if (debugFrom) from = "field";
            }
        }

        if ( value == null /*|| value.length()==0*/  ) {
            if (m_widgetType != null){
                value = m_widgetType.findPropertyDefaultValue(what);
                if (debugFrom) from = "type-default";
            }
        }

        if ( value == null ) {
            Object valueObject = super.get(what);
            if ( valueObject != null ) {
                value = valueObject.toString();
                if (debugFrom) logFromInfo(Widget.class, "super.get", fullname, value);
                return value;
            } else {
                value = "";
                if (debugFrom) logFromInfo(Widget.class, "still-null", fullname, value);
                return value;
            }
        }

        if (debugFrom) logFromInfo(Widget.class, from, fullname, value);

        //The Webmacro context is fairly expensive.
        //So check to see if webmacro. If so, expand using Webmacro:
        if ((!expandedAlready) && dt!=null && dt instanceof WebmacroDatatype){
            return expand(value);  //use webmacro
        } else {
            return value;          //not a webmacro expression, just send it back.
        }
     }

     public static void logFromInfo(Class theClass, String from, String fullname, String value){
        if (debugFrom) com.dynamide.util.Log.info(theClass, StringTools.pad(fullname, 50, StringTools.PAD_LEFT)
                                          +StringTools.pad("["+from+"] == ", 25, StringTools.PAD_LEFT)
                                          +value);
     }

     public Property getPropertyDefault(String what){
        return getWidgetType().getPropertyDefault(what);
     }

     public String getPropertyDefaultValue(String what){
        return getWidgetType().getPropertyDefaultValue(what);
     }

    public IDatasource getDatasource(){
        IDatasource datasource = null;
        Session session = getSession();
        String datasourceName = getPropertyStringValue("datasource");
        if ( ! Tools.isBlank(datasourceName) ) {
            datasource = session.getDatasource(datasourceName);
        } else {
            datasource = session;
        }
        return datasource;
    }


    /** Only Widgets use fields right now, Pages don't. */
    //build-38-1 I don't store the Field any more, always get from Session.
    public Field getField(){
        return getField(getPropertyStringValue("field"));
    }

    public Field getField(String fieldName){
        if ( fieldName == null || fieldName.length()==0 ) {
            return null;
        }
        Session session = getSession();
        IDatasource datasource = getDatasource();
        if (datasource!=null) {
            String datasourceName = datasource.getID();
            String fieldIndex = getPropertyStringValue("fieldIndex");
            if ( fieldIndex.length()>0 ) {
                Field result = datasource.getField(fieldName, fieldIndex);
                //logDebug("AbstractWidget.getField("+fieldName+","+fieldIndex+") returning "+result);
                return result;
            } else {
                //logDebug("AbstractWidget.getField("+fieldName+") "+getID()+" using datasource: "+datasourceName);
                return datasource.getField(fieldName);
            }
        }
        return null;
    }

    public String getFieldStringValue(String fieldName){
        Field f = getField(fieldName);
        if ( f != null  ) {
            return f.getStringValue();
        }
        return "";
    }


    /** Call this overload to get the fieldname for a field other than "field", e.g. "captionField".
     */
    public String getFieldID(String fieldName)
    throws Exception {
        String datasource = getPropertyStringValue("datasource");
        String fieldIndex = ""; // doesn't make sense for other fields: getPropertyStringValue("fieldIndex");
        if ( datasource != null && datasource.length() > 0 ) {
            return Field.formatFullFieldName(datasource, fieldName, fieldIndex);
        }
        return fieldName; //12/30/2003 9:25AM "";
    }

    /** This method returns the full, formatted fieldname, qualified with the datasource and index, if any.
     *  Calling this method is equivalent to $widget.fieldID or $widget.FieldID in webmacro, because
     *  get("fieldID") is defined, and simply calls this method, and if you say $widget.FieldID (capital F)
     *  then Webmacro will call getFieldID() directly.
     */
    public String getFieldID()
    throws Exception {
        return formatFullFieldName(this, true);
    }


    public static String formatFullFieldName(AbstractWidget widget, boolean doAutoWidget)
    throws Exception {
        String fieldName = widget.getPropertyStringValue("field");
        return formatFullFieldName(widget, doAutoWidget, fieldName);
    }

    public static String formatFullFieldName(AbstractWidget widget, boolean doAutoWidget, String fieldName)
    throws Exception {
        boolean debug = false;
        String datasource = widget.getPropertyStringValue("datasource");
        String fieldIndex = widget.getPropertyStringValue("fieldIndex");
        if ( datasource != null && datasource.length() > 0 ) {
            if (debug) System.out.println("formatFullFieldName using datasource: "+fieldName);
            return Field.formatFullFieldName(datasource, fieldName, fieldIndex);
        }
        if ( doAutoWidget ) {
            return autoField(widget);
        } else {
            return fieldName;
        }
    }

    private String autoField()
    throws Exception {
        return autoField(this);
    }

    private static  String autoField(AbstractWidget widget)
    throws Exception {
            boolean debug = false;
            //the "Miranda" field: if you can't afford one, one will be created for you...
            String fieldName = widget.getPropertyStringValue("field");
            if (debug) System.out.println("autoField fieldName: "+fieldName);
            if ( fieldName.length()==0){ //not present in Widget (didn't check WidgetType), so autocreate
                String autoFieldID;
                if (widget.getOwner() instanceof Session){
                    autoFieldID = widget.getID();
                } else {
                    autoFieldID = widget.getOwner().getID()+'_'+widget.getID();
                }
                if (debug) System.out.println("autoFieldID: "+autoFieldID);
                widget.getSession().logAutoFieldID(autoFieldID);
                String value = widget.get("value", false).toString();  //recursive, one level deep.
                try {
                    Session session = widget.getSession();
                    session.setFieldValue(autoFieldID, value); //performs override 5/17/2003 1:48PM used to be createField
                    Property property = widget.m_widgetType.getPropertyDefault("field");
                    property.setValue(autoFieldID);
                    widget.setProperty(property); //"field", autoFieldID);  //set the "field" property.
                    return autoFieldID;
                } catch (DynamideException e){
                    com.dynamide.util.Log.error(AbstractWidget.class, "ERROR: [88] [AbstractWidget.get] ", e);
                    return "ERROR_88";
                }
            } else {
                return fieldName;
            }
    }

    public void changeProperty(String propertyName, String propertyValue){
        if (DEBUG_PROPS) System.out.println("Entering "+m_id+".changeProperty("+propertyName+")");
        Property newProperty = (Property)(getProperty(propertyName).clone());
        Object valueObject = getProperty(propertyName).getValue();
        System.out.println("\r\nchangeProperty valueObject: "+valueObject+"\r\n");
        Object defaultValueObject = m_widgetType.getPropertyDefault(propertyName).getDefaultValue();
        System.out.println("\r\nchangeProperty defaultValueObject: "+defaultValueObject+"\r\n");
        if ( valueObject == null ) {
            logDebug("AbstractWidget.changeProperty using defaultValueObject: "+defaultValueObject);
            valueObject = defaultValueObject;
        }
        if ( valueObject != null && valueObject instanceof Datatype ) {
            Datatype datatype = ((Datatype)valueObject);
            Datatype datatypeClone = ((Datatype)datatype.clone());
            datatypeClone.set("value", propertyValue); //EnumeratedDatatype overrides to add it as a sub-item.
            newProperty.set("value", datatypeClone);
        } else {
            newProperty.setValue(propertyValue);  // %% Note: String value only.
        }
        setProperty(newProperty);
        /*

        if ( ! hasProperty(propertyName)){
            property = getProperty(propertyName);
        } else {
            property = m_widgetType.getPropertyDefault(propertyName);
        }
        Object valueobj = getProperty(propertyName).getValue();
        if ( valueobj == null ) {
            valueobj = property.getDefaultValue();
        }
        if ( valueobj instanceof Datatype ) {
            Datatype datatype = ((Datatype)valueobj);
            datatype.set("value", propertyValue); //EnumeratedDatatype overrides to add it as a sub-item.
            property.set("value", datatype);
        } else {
            property.setValue(propertyValue);  // %% Note: String value only.
        }
        setProperty(property);
       */
    }

    public void changePropertyOLDDDDDDD(String propertyName, String propertyValue){
        if (DEBUG_PROPS) System.out.println("Entering "+m_id+".changeProperty("+propertyName+")");
        Property property;
        if ( ! hasProperty(propertyName)){
            property = getProperty(propertyName);
        } else {
            property = m_widgetType.getPropertyDefault(propertyName);
        }
        Object originalValue = getProperty(propertyName).getValue();
        System.out.println("originalValue: "+((DynamideObject)originalValue).dump());
        Property propertyClone = (Property)property.clone();
        Object valueobj = propertyClone.getDefaultValue();

        if ( valueobj instanceof Datatype ) {
            Datatype datatype = (Datatype) ((Datatype)valueobj).clone();  //Object.clone is protected, so you must only clone Datatype, then cast it again to put it into edt.
            logDebug("AbstractWidget.changeProperty: "+propertyName+":"+propertyValue+":dt:"+datatype.getClass().getName()
            +"  datatype: "+datatype.dump()+" valueobj: "+((Datatype)valueobj).dump());
            datatype.set("value", propertyValue); //EnumeratedDatatype overrides to add it as a sub-item.
            propertyClone.set("value", datatype);

            String body = JDOMFile.output(propertyClone.toElement());
            try {
                body = com.dynamide.JDOMFile.prettyPrintHTML(body);
                if (DEBUG_PROPS) System.out.println("-------------propertyClone: "+body);
            } catch (Exception e){
                logError("Couldn't dump new property:::::::", e);
            }

        } else {
            propertyClone.setValue(propertyValue);  // %% Note: String value only.
        }
        setProperty(propertyClone);  //%%warning: this may set the property to duplicate all the defaults.
    }

    public void setProperty(Property property){
        if (DEBUG_PROPS) System.out.println("Entering "+m_id+".setProperty("+property.get("name")+")");
        String propName = property.getName();
        Property oldProperty = (Property)getPropertiesTable().getObject(propName);
        //oldProperty may be null.

        if ( propName.equalsIgnoreCase("name") ) {
            System.out.println("ARG! CHANGING NAME CAN HAVE RAMIFICATIONS FOR PAGE OBJECTS. Allowing the change anyway.");  //%%
            super.setProperty(propName, property);
            if (DEBUG_PROPS) System.out.println("Setting property in Widget["+getID()+" OID: "+getObjectID()+"].  name: "+propName+" value: "+property.getStringValue());
            notifyChangeListeners(propName, oldProperty, property);
            setID(property.getStringValue()); //arg.  %% I still support name property and m_id being called name.
            return;
        }

        if ( propName.equalsIgnoreCase("field") ) {
            if ( property.getStringValue().length()==0 ) {
                System.out.println("nullifying field in "+getDotName());
                super.removeProperty("field");  //%%%%%%%%%%%%%%%%%% 1) notifyChangeListeners is not called
                                                // %%%%%%%%%%%%%%%%%% and 2) this seems like it would leave the field property gone.
                return;
            }
        }

        if ( propName.equalsIgnoreCase("type") ) {
        //    setType(property.getStringValue());
            System.out.println("WARNING: Can't set 'type' in AbstractWidget.setProperty -- operation not allowed.");
            return;
        }

        Field aField = getField();//getSession().findField(getPropertyStringValue("field"));//build-38-1
        if (aField != null){
            if (aField.hasProperty(propName)){
                aField.set(propName, property.getStringValue());
                System.out.println("Setting property in Field, not in widget.  name: "+propName+" value: "+property.getStringValue());
                //ARG! %% treating fieldvalue as string only.
                //Properties may or may not be strings, but I am coercing to string here,
                //since the xml storage and Persistent.properties is string-based.
                return;
            }
        }

        //Not found in Field, set the widget property.
        //first check to see that WidgetType allows a property of this name...
        //then see if it is the same as the default widgettype property. (or maybe not - but think about it at any rate. %%)
        if ( m_widgetType!=null && m_widgetType.hasProperty(propName) ) {
            super.setProperty(propName, property);
            if (DEBUG_PROPS) System.out.println("Setting property in Widget["+getID()+" OID: "+getObjectID()+"].  name: "+propName+" value: "+property.getStringValue());
            notifyChangeListeners(propName, oldProperty, property);
         } else {
            System.out.println("WARNING: Can't set property in widget, since type doesn't allow it -- name: "+propName+" value: "+property.getStringValue());
         }
    }


    /*package-access*/ static void cleanProperties(Element properties){
        //Iterator it = properties.getChildren().iterator(); //getChildren always returns a list, even if empty.
        List list = properties.getChildren();
        int list_size = list.size();
        for (int i=list_size-1; i >= 0 ; i--) {
            Element property = (Element)list.get(i);
            Element value = property.getChild("value");
            if ( value == null ) {
                properties.removeContent(property);
                System.out.println("cleanProperties removing property: "+property.getAttribute("name"));
                continue;
            } else {
                property.removeChild("defaultValue");
                //property.removeChild("datatype");
                //property.removeChild("readOnly");
                //property.removeChild("intl");
                //System.out.println("cleanProperties: "+property.getName());
            }

        }
        //while ( it.hasNext() ) {
        //    Element property = (Element)it.next();
        //}
    }

    public void setPropertiesFromURL(){
        Session session = getSession();
        if (session == null) return;
        HttpServletRequest request = session.getRequest();
        if (request==null) return;
        Enumeration paramNames = request.getParameterNames();
        String paramName;
        String[] paramValues;
        String paramValue;
        while(paramNames.hasMoreElements()) {
            paramName = (String)paramNames.nextElement();
            paramValues = request.getParameterValues(paramName);
            if ( paramValues.length>0 ) {
                paramValue = paramValues[0];
            } else {
                paramValue = "";
            }
            if (paramValue.length()>0){
                try {
                    setProperty(paramName, paramValue);
                } catch (DatatypeException e){
                    logError("Couldn't set property from URL", e);
                }
            }
        }

    }

    public void propertyChanged(ChangeEvent changeEvent){
        getWidgetType().fire_onPropertyChanged(this, changeEvent);
    }


    //========== IChangeListener broadcaster ================
    private Vector changeListeners = new Vector();

    public void addChangeListener(IChangeListener new_listener){
        changeListeners.addElement(new_listener);
    }
    public void removeChangeListener(IChangeListener listener){
        changeListeners.removeElement(listener);
    }
    protected void notifyChangeListeners(String propertyName, Object oldValue, Object newValue){
        ChangeEvent event = new ChangeEvent();
        event.oldValue = oldValue;
        event.newValue = newValue;
        event.fieldName = propertyName;
        event.sender = this;

        //Someone can respond by removing themselves.  Therefore clone the vector and loop over the clone.
        Vector changeListenersClone = (Vector)changeListeners.clone();
        for (int i = 0; i < changeListeners.size(); i++){
            ((IChangeListener)changeListenersClone.elementAt(i)).propertyChanged(event);
        }
    }

    //============= Handle the PropertyTableModel ======================================

    public int getPropertyCount(){
        return getPropertiesTable().size();
    }

    public String getPropertyNameByOrdinal(int row){
        Property property =  (Property)getPropertiesTable().getObjectAt(row);
        if (property == null){
            return "";
        }
        return property.getName();
    }

    public Property getPropertyByOrdinal(int row){
        Property property = (Property)getPropertiesTable().getObjectAt(row);
        if (property == null){
            try {
                return new Property(this);//better than returning null.
            } catch( Exception e ){
                return null;
            }
        }
        return property;
    }

    public String getEventNameByOrdinal(int row){
        //not dealing with this at all.
        return "";
    }

    public Property getEventByOrdinal(int row){
        //not dealing with this at all.
        try {
            return new Property(this);//better than returning null.
        } catch( Exception e ){
            return null;
        }
    }

    public int getEventCount(){
        //not dealing with this at all.
        return 0;
    }



}
