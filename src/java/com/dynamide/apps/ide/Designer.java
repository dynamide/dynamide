package com.dynamide.apps.ide;

import java.util.Collection;

import com.dynamide.Page;
import com.dynamide.Persistent;
import com.dynamide.Property;
import com.dynamide.Session;
import com.dynamide.Widget;
import com.dynamide.datatypes.Datatype;
import com.dynamide.datatypes.EnumeratedDatatype;

import com.dynamide.event.ScriptEvent;

import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class Designer {

    public Designer(){
    }

    public void initFrom(Designer other){
        doneScript = other.getDoneScript();
        target = other.getTarget();
        targetDatatype = other.getTargetDatatype();
        propertyName = other.getPropertyName();
        targetID = other.getTargetID();
        targetOwnerID = other.getTargetOwnerID();
        targetPath = other.getTargetPath();
        targetClass = other.getTargetClass();
        index = other.getIndex();
    }

    private String id = "";
    public String getID(){return id;}
    public void setID(String new_value){id = new_value;}

    private String doneScript = "";
    public String getDoneScript(){return doneScript;}
    public void setDoneScript(String new_value){doneScript = new_value;}

    private String forceRefreshScript = "";
    public String getForceRefreshScript(){return forceRefreshScript;}
    public void setForceRefreshScript(String new_value){forceRefreshScript = new_value;}

    private Persistent target = null;
    public Persistent getTarget(){ return target;}
    public void setTarget(Persistent new_target){
        target = new_target;
    }

    private Datatype targetDatatype = null;
    public Datatype getTargetDatatype(){ return targetDatatype;}
    public void setTargetDatatype(Datatype new_targetDatatype){
        targetDatatype = new_targetDatatype;
    }

    private Property targetProperty = null;
    public Property getTargetProperty(){ return targetProperty;}
    public void setTargetProperty(Property new_targetProperty){
        targetProperty = new_targetProperty;
    }

    private String propertyName = "";
    public String getPropertyName(){ return propertyName;}
    public void setPropertyName(String new_propertyName){
        propertyName = new_propertyName;
    }

    private String targetID = "";
    public String getTargetID(){ return targetID;}
    public void setTargetID(String new_targetID){
        targetID = new_targetID;
    }

    private String targetOwnerID = "";
    public String getTargetOwnerID(){ return targetOwnerID;}
    public void setTargetOwnerID(String new_targetOwnerID){
        targetOwnerID = new_targetOwnerID;
    }

    private String targetPath = "";
    public String getTargetPath(){ return targetPath;}
    public void setTargetPath(String new_targetPath){
        targetPath = new_targetPath;
    }

    private String targetClass = "";
    public String getTargetClass(){return targetClass;}
    public void setTargetClass(String new_value){
        targetClass = new_value;
    }

    private String index = "";
    public String getIndex(){return index;}
    public void setIndex(String new_value){
        index = new_value;
    }

    private String state = "";
    public String getState(){return state;}
    /** Use this to record the designer page's state when another datatype must be delegated to.*/
    public void setState(String new_value){
        state = new_value;
    }

    private String callingPageID = "";
    public String getCallingPageID(){return callingPageID;}
    public void setCallingPageID(String new_value){
        callingPageID = new_value;
    }

    private boolean m_isDone = false;
    public boolean isDone(){return m_isDone;}
    public void setDone(boolean new_value){m_isDone = new_value;}

    private Designer parentDesigner = null;
    public Designer getParentDesigner(){return parentDesigner;}
    public void setParentDesigner(Designer new_value){parentDesigner = new_value;}


    public String toString(){
        return "{Designer:id:"+id+",target:"+target+",targetPath:"+targetPath+",targetOwnerID:"+targetOwnerID+",targetID:"+targetID+",propertyName:"+propertyName+",index:"+index+",targetDatatype:"+targetDatatype+"}";
    }

    public String formatDoneScript(String newPropertyValue){
        String scriptChunk ="alert('Incorrect targetClass ["+targetClass+"] in Designer.formatDoneScript()');";
        if (targetClass.equals("Session")){
            scriptChunk = "var inspectorwin = opener.getInspector();"
                        +" if (inspectorwin!=null)inspectorwin.applicationPropertyChanged('"+propertyName+"', '"
                                                                      +newPropertyValue+"', '"
                                                                      +targetID+"', '"
                                                                      +targetOwnerID+"', 'designer');";
        } else if (targetClass.equals("Page")) {
            scriptChunk = "var inspectorwin = opener.getInspector();"
                         +" if (inspectorwin!=null) inspectorwin.pagePropertyChanged('"+propertyName+"', '"
                                                                      +newPropertyValue+"', '"
                                                                      +targetID+"', '"
                                                                      +targetOwnerID+"');";

        } else if (targetClass.equals("Widget")) {
            //js prototype: function pickit(propertyName, propertyValue, widgetID, targetPageID, callerID, forceReload, noPropUpdate){
            scriptChunk = "var inspectorwin = opener.getInspector();"
                        +" if (inspectorwin!=null)inspectorwin.pickit('"+propertyName+"', '"
                                                                        +newPropertyValue+"', '"
                                                                        +targetID+"', '"
                                                                        +targetOwnerID
                                                                        +"', 'designer', true, true);";
        }
        return scriptChunk;
    }

    public ScriptEvent onAction(ScriptEvent event){
        event.println("in designertop_onAction: "+event.action);
        //event.println(event.session.dumpRequestInfo());
        //event.println(event.toString());
        if (event.action.equals("validateDesigner")){

             String propertyName = event.getQueryParam("propertyName");
             String targetOwnerID = event.getQueryParam("targetOwnerID");
             String targetClass = event.getQueryParam("targetClass");
             String targetID = event.getQueryParam("targetID");
             String source = event.getQueryParam("source");
             System.out.println("=============source:\r\n"+source+"\r\n==============");
             String datatype= event.getQueryParam("datatype");
             boolean doPage = false;
             boolean doApplication = false;
             String newPropertyValue = "";
             String scriptChunk = "";
             String validationDesc = "";

             // %% CHANGE THIS.  It needs to be loaded into designer events in the specific designer files.
             Session subsession = null;
             try {
                subsession = event.session.getSubsession();
             } catch (Exception e)  {
                event.returnError("Designer could not create subsession", e);
                return event;
             }

             if (datatype.equals("com.dynamide.datatypes.ServerSideEvent")){
                if (subsession!=null){
                    if (targetClass.equals("Session")){
                        newPropertyValue = targetOwnerID+'_'+propertyName;
                        subsession.setEventSource(newPropertyValue, source); //first param is the new event name
                        validationDesc = "doApplication";
                        scriptChunk = "  var inspectorwin = opener.getInspector();\r\n"
                                     +"  if (inspectorwin!=null)inspectorwin.applicationPropertyChanged('"+propertyName+"', '"+newPropertyValue+"', '"+targetID+"', '"+targetOwnerID+"', 'designer');\r\n";
                    } else if (targetClass.equals("Page")) {
                        newPropertyValue = targetOwnerID+'_'+propertyName;
                        Page page = (Page)subsession.find(targetOwnerID);
                        if (page != null){
                            validationDesc = "doPage";
                            page.setEventSource(newPropertyValue, source); //first param is the new event name
                            //targetID = "foo";
                            scriptChunk = "  var inspectorwin = opener.getInspector();\r\n"
                                         +"  if (inspectorwin!=null) inspectorwin.pagePropertyChanged('"+propertyName+"', '"+newPropertyValue+"', '"+targetID+"', '"+targetOwnerID+"');\r\n";
                        }
                     } else {
                            newPropertyValue = targetID+'_'+propertyName; //%%This maybe should be set by the caller.
                            Page page = (Page)subsession.find(targetOwnerID);
                            if (page != null){
                                Widget widget = page.getWidgetByID(targetID);
                                if (widget == null){
                                    event.println("widget ["+targetID+"] not found in page ["+targetOwnerID+"]");
                                } else {
                                    event.println("widget ["+targetID+"] in page ["+targetOwnerID+"] gets event source.");
                                    widget.setEventSource(newPropertyValue, source); //first param is the new event name
                                    validationDesc = "doWidget";
                                    scriptChunk = "  var inspectorwin = opener.getInspector();\r\n"
                                                +"  if (inspectorwin!=null)inspectorwin.pickit('"+propertyName+"', '"+newPropertyValue+"', '"+targetID+"', '"+targetOwnerID+"', 'designer', false, false);\r\n";
                                }
                            } else {
                                event.println("page was null when looking for targetOwnerID: "+targetOwnerID);
                            }
                    }
                } else {
                    validationDesc = "subsession was null"+event.session.dumpFields();
                }
             } else if (datatype.equals("com.dynamide.datatypes.EnumeratedDatatype")){
                validationDesc = "Validating EnumeratedDatatype";
                Collection values = StringTools.parseSeparatedValues(source, "\r\n");

                Persistent target = null;

                Property pd = null;

                if (targetClass.equals("Session")){
                    target = subsession;
                    pd = subsession.getProperty(propertyName);
                } else if (targetClass.equals("Page")) {
                    Page page = (Page)subsession.find(targetID);
                    target = page;
                    pd = page.getPropertyDefault(propertyName);
                } else if (targetClass.equals("Widget")) {
                    Page page = (Page)subsession.find(targetOwnerID);
                    Widget widget = page.getWidgetByID(targetID);
                    target = widget;
                    pd = ((Widget)target).getWidgetType().getPropertyDefault(propertyName);
                    event.println("pd: "+pd+" target: "+target+" widget: "+widget+" page: ");
                } else {
                    event.println("ERROR: targetClass has an illegal value: "+targetClass);
                }

                if (pd != null){
                    Property propertyClone = (Property)pd.clone();

                    EnumeratedDatatype edt;
                    Object vo = pd.getValue();
                    if (vo instanceof EnumeratedDatatype){
                        edt = ((EnumeratedDatatype)vo);
                        edt.clear();
                        edt.addStrings(values);
                        event.println("new EnumeratedDatatype: "+StringTools.escape(edt.dumpHTML()));
                        propertyClone.set("value", edt);
                        propertyClone.setOwner(target);
                        target.setProperty(propertyClone);
                        Property propertyCheck = target.getProperty(propertyName);
                        event.println("new property check: "+StringTools.escape(propertyCheck.dumpXML()));
                        event.println("new property dump: "+StringTools.escape(propertyCheck.dump()));
                        validationDesc = "EnumeratedDatatype";
                        newPropertyValue = ""; //try empty string for now.  result should be SELECT with first item selected, I guess.

                        String forceRefresh =
                            "var inspectorwin = opener.getInspector();"
                                        +" if (inspectorwin!=null)inspectorwin.location=inspectorwin.location;";
                        setForceRefreshScript(forceRefresh);

                        if (targetClass.equals("Session")){
                            validationDesc = "doApplication";
                            scriptChunk = "var inspectorwin = opener.getInspector();"
                                        +" if (inspectorwin!=null)inspectorwin.applicationPropertyChanged('"+propertyName+"', '"
                                                                                      +newPropertyValue+"', '"
                                                                                      +targetID+"', '"
                                                                                      +targetOwnerID+"', 'designer');";
                        } else if (targetClass.equals("Page")) {
                            scriptChunk = "var inspectorwin = opener.getInspector();"
                                         +" if (inspectorwin!=null) inspectorwin.pagePropertyChanged('"+propertyName+"', '"
                                                                                      +newPropertyValue+"', '"
                                                                                      +targetID+"', '"
                                                                                      +targetOwnerID+"');";

                        } else if (targetClass.equals("Widget")) {
                            //js prototype: function pickit(propertyName, propertyValue, widgetID, targetPageID, callerID, forceReload, noPropUpdate){
                            scriptChunk = "var inspectorwin = opener.getInspector();"
                                        +" if (inspectorwin!=null)inspectorwin.pickit('"+propertyName+"', '"
                                                                                        +newPropertyValue+"', '"
                                                                                        +targetID+"', '"
                                                                                        +targetOwnerID
                                                                                        +"', 'designer', true, true);";
                        }
                    } else {
                        validationDesc = "doWidget EnumeratedDatatype called with wrong datatype: "+vo.getClass().getName();
                        scriptChunk = "alert('wrong datatype: "+vo.getClass().getName()+"');";
                        newPropertyValue = "";
                    }
                } else {
                    String errmsg = "Could not load property '"+propertyName+"' for targetOwnerID: "+targetOwnerID+" and targetID: "+targetID;
                    event.println(errmsg);
                    scriptChunk = "alert('"+errmsg+"');";
                }

                System.out.println("Validating EnumeratedDatatype: \r\n"+Tools.showControlChars(source));
             }
             //May have to delegate to editor event here...3/24/2002 0:43AM

             setDoneScript(scriptChunk);

             event.println("======================================event.action was validateDesigner");
             //opener will be idetop.
             event.resultSrc = "<html><body><pre>"
                                +validationDesc
                                +"\r\n targetClass: "+targetClass
                                +"\r\n targetOwnerID: "+targetOwnerID
                                +"\r\n targetID: "+targetID
                                +"\r\n propertyName: "+propertyName
                                +"\r\n newPropertyValue: "  + newPropertyValue
                                +"\r\n DEBUG: script: "+scriptChunk
                                +"\r\n Validated <script>\r\n"
                                + scriptChunk
                                +"\r\n  parent.close();"
                                +"</script>"
                                +"</pre></body></html>";
             event.resultAction = ScriptEvent.RA_RETURN_SOURCE;
             event.println("Validation result: \r\n--------------\r\n"+StringTools.escape(event.resultSrc)+"\r\n--------------");
             //if error, then set a label to have an error, and do this: event.resultAction = ScriptEvent.RA_SHOW_ERRORS;
        }
        return event;
    }


}
