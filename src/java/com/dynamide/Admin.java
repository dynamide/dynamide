package com.dynamide;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.dynamide.resource.IContext;

import com.dynamide.util.Tools;

public class Admin extends DynamideObject {
    public Admin(DynamideObject owner){
        super(owner);
        session = findParentSession();
    }

    private Session session;

    private Session m_targetSession = null;
    public Session getTargetSession(){return m_targetSession;}
    public void setTargetSession(Session new_value){m_targetSession = new_value;}

    public void close(){
        session = null;
        setOwner(null);
        logDebug("Admin.close");
    }

    public String doAction()
    throws Exception {
        String id;
        HttpServletRequest request = session.getRequest();
        if ( request.getParameter("closeAllSessions") != null ) {
            //not implemented.  This is done in the Admin application instead: killSessionsStartingWith(Constants.SESSION_PREFIX);
            return showSessionTable(request);
        } else if ( request.getParameter("showSessions") != null ) {
            return "not implemented(Admin.java)";
        } else if ( request.getParameter("showSessionTable") != null) {
            return showSessionTable(request);
        }
        return "";
    }


    protected String closeSession(String sessionID) {
        throw new SecurityException("Obviated");
    }


    private String showSessionTable(HttpServletRequest request)
    throws Exception {
        String body = "<h2>Sessions</h2>"
                        +"NOT IMPLEMENTED"//....formatSessionTable("*")
                        +"<br/><small><a href='?closeAllSessions=1'>Close All Sessions</a></small>"
                        +"<br/><small>"+Tools.nowLocale()+"</small>";
        return body;
    }

    public Map sortSessionTable(IContext sessions, String columnKey, String sortAsc){
        boolean sortAscending = sortAsc.equals("1");
        Map result;
        if ( sortAscending ) {
            result = new TreeMap();
        } else {
            result = new TreeMap(Collections.reverseOrder());
        }
        Session targetSession;
        Iterator it = sessions.getAttributes().entrySet().iterator();
        while (it.hasNext()){
            Map.Entry en = (Map.Entry)it.next();
            String key = (String)en.getKey();
            targetSession = (Session)en.getValue();
            String value = targetSession.getString(columnKey)+targetSession.getSessionID();//need to make keys unique
            result.put(value, targetSession);
        }
        return result;
    }


}
