package com.dynamide.util;

import java.util.Map;

import com.dynamide.DynamideObject;
import com.dynamide.Page;
import com.dynamide.Session;
import com.dynamide.Widget;

import com.dynamide.event.ScriptEvent;

public class RegisteredActions extends DynamideObject {

    public RegisteredActions(Session session){
        super(session);
    }

    public Session getSession(){
        return (Session)getOwner();
    }

    private StringList m_registeredActions = new StringList();

    public RegisteredAction findRegisteredAction(String action){
        return (RegisteredAction)m_registeredActions.getObject(action);
    }

    public Map getMap(){
        return m_registeredActions.getMap();
    }

    public String listRegisteredActions(){
        return m_registeredActions.toString();
    }

    public void registerAction(String action,
                               DynamideObject sender,
                               String eventName,
                               Object inputObject){
        RegisteredAction ra = new RegisteredAction();
        ra.action = action;
        ra.sender = sender;
        ra.eventName = eventName;
        ra.inputObject = inputObject;
        m_registeredActions.addObject(action, ra);
    }

    public void unregisterAction(String action){
        m_registeredActions.remove(action);
    }

    public ScriptEvent fireRegisteredAction(String action){
        RegisteredAction ra = findRegisteredAction(action);
        if (ra!=null){
            //look this up and call widget.call(eventName, inputObject)
            if ( ra.sender!=null && ra.sender instanceof Widget ) {
                getSession().logHandlerProc("INFO", "Page.handleAction using registered action: "+ ra.dumpHTML());
                if (ra.sender instanceof Page){
                    Page page = (Page)ra.sender;
                    ScriptEvent event = page.fireEvent(ra.inputObject, ra.eventName);
                    return event;
                } else {
                    Widget widget = ((Widget)ra.sender);
                    ScriptEvent event = (ScriptEvent)widget.fireEvent(ra.inputObject, ra.eventName, widget.getPage().getID(), ra.action);
                    //works, but noisy: getSession().logHandlerProc("DEBUG", "Result of page.handleAction: "+ (event!=null?event.dumpHTML():"null"));
                    return event;
                }
            } else {
                logError("There was a RegisteredAction, but the object is invalid: "+ra.toString());
                return null;
            }
        }
        return null;
    }
}
