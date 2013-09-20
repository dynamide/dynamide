package com.dynamide.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.dynamide.DynamideObject;

public class SessionDatabase extends DynamideObject {

    public SessionDatabase(DynamideObject owner){
       super(owner);
        m_cleanThread = new CleanThread();
        m_cleanThread.owner = this;
        m_cleanThread.start();
        //addSession("SessionDatabase_started", ""+Tools.nowLocale());
        com.dynamide.resource.ResourceManager.getRootResourceManager().rebindAttribute("SessionDatabase_started", ""+Tools.nowLocale());
    }

    public SessionDatabase(){
        this(null);
    }

    public void shutdown(){
        if (m_cleanThread != null){
            m_cleanThread.interrupt();
            m_cleanThread = null;
            cleanSessionTable(CLEAN_WHICH_ALL);
        } else {
            Log.warn(SessionDatabase.class, "CleanThread was null already on shutdown()");
        }
        instance = null;
    }

    private Hashtable sessions = new Hashtable();
    private Vector lockedDetailItems = new Vector();
    /** If an item should not be viewable by a hacker, lock it against toString()
     *  with this function.  All items not locked will have toString called when
     *  showSessionObjects formats the showSessionObjectDetail link.
     */
    public void lockDetailItem(String key){
        lockedDetailItems.addElement(key);
    }
    public boolean isDetailItemLocked(String key){
         return ( lockedDetailItems.indexOf(key) > -1 ) ;
    }


    private long maxID = Tools.now().longValue();  //actually, this is the base ID.  It will be incremented for each store.
    private Hashtable cache = new Hashtable();
    private boolean wantCache = false;  //%% add a test for a global production mode.

    private class CleanThread extends Thread{
        public SessionDatabase owner;
        public void run(){
            while (true){
                try {
                    sleep(ISessionItem.TIMEOUT_CHECK_INTERVAL);
                    if (owner!=null){
                        synchronized(this){
                            try {
                                owner.cleanSessionTable(owner.CLEAN_WHICH_EXPIRED);
                            } catch (Exception e){
                                //This *does* happen, and kills the thread if not caught.
                                logError(Tools.errorToString(e, true));
                            }
                        }
                        //System.gc();
                    }
                } catch (InterruptedException e2) {
                    //So far, this never happens, except that it will happen when we call m_cleanThread.interrupt() from SessionDatabase during cleanup.
                    logError("************************************************************");
                    logError("************************************************************");
                    logError("***          WARNING: CleanThread returning.");
                    logError("************************************************************");
                    logError("************************************************************");
                    return;
                } catch (Exception e3){
                    //This *does* happen, and kills the thread if not caught.
                    logError("ERROR in SessionDatabase.CleanThread.run(): "+Tools.errorToString(e3, true));
                }
            }
        }
           }

    private CleanThread m_cleanThread;

    public void finalize() throws Throwable {
        logDebug("SessionDatabase.finalize");
        super.finalize();  // %% TODO: do this everywhere
    }

    public synchronized String getNextSessionID(){
        long d = Tools.now().longValue();
        if (d == maxID)
            d = maxID+1;
        maxID = d;
        return ""+(maxID);
    }

    public synchronized void clear(){
        sessions.clear();
    }

    public synchronized String addSession(Object sessionData){
        String sessionID = getNextSessionID();
        sessions.put(sessionID, sessionData);
        com.dynamide.resource.IContext ctx
           = (com.dynamide.resource.IContext)com.dynamide.resource.ResourceManager.getRootResourceManager().find("/homes/dynamide/sessions");
        ctx.rebindAttribute(sessionID, sessionData);
        return sessionID;
    }

    public synchronized String rebindSession(String id, Object sessionData){
        sessions.remove(id);
        sessions.put(id, sessionData);
        com.dynamide.resource.IContext ctx
           = (com.dynamide.resource.IContext)com.dynamide.resource.ResourceManager.getRootResourceManager().find("/homes/dynamide/sessions");
        ctx.rebindAttribute(id, sessionData);
        return id;
    }

    public synchronized boolean addSession(String id, Object sessionData){
        if (sessions.containsKey(id)){
            return false;
        }
        sessions.put(id, sessionData);
        com.dynamide.resource.IContext ctx
           = (com.dynamide.resource.IContext)com.dynamide.resource.ResourceManager.getRootResourceManager().find("/homes/dynamide/sessions");
        ctx.rebindAttribute(id, sessionData);
        return true;
    }

    /** Safe to call if sessionID has already been removed.  If you have been called by ISessionItem.shutdown(),
     *  you don't need to call this method, though.
     */
    public synchronized void removeSession(String sessionID){
        sessions.remove(sessionID);
        int i = lockedDetailItems.indexOf(sessionID);
        if (i > -1 ) {
            lockedDetailItems.removeElementAt(i);
        }
        System.gc();
    }

    public synchronized void killSessionsStartingWith(String sessionPrefix){
        //System.out.println("killSessionsStartingWith: "+sessionPrefix);
        Enumeration ek = sessions.keys();
        Object key;
        while ( ek.hasMoreElements()){
            key = ek.nextElement();
            //System.out.println("key: "+key.toString());
            if ( (key != null) && ((String)key).startsWith(sessionPrefix) ){
                //System.out.println("key matches prefix: "+key.toString());
                ISessionItem item = (ISessionItem)sessions.get(key);
                try {
                    CloseSessionTimerTask.startCloseSessionTimerTask(item);
                    //item.shutdown();
                } catch (Exception e){
                    logError("ERROR: [87] exception raised while trying to shutdown a session: "+e);
                }
                sessions.remove(key);
            }
        }
        System.gc();
    }



    public synchronized void removeSessionObject(Object removeMe){
        Enumeration e = sessions.elements();
        Enumeration ek = sessions.keys();
        Object obj, key;
        while ( e.hasMoreElements()){
            obj = e.nextElement();
            key = ek.nextElement();
            if (obj == removeMe ){
                sessions.remove(key);
                break;
            }
        }
        System.gc();
    }

    public synchronized void shutdownSession(ISessionItem item){
        //item.shutdown();
        removeSessionObject(item);
        CloseSessionTimerTask.startCloseSessionTimerTask(item);
    }

    public synchronized Object getSession(String sessionID){
        return sessions.get(sessionID);
    }

    public synchronized boolean isSessionNull(String id) {
        if (sessions.containsKey(id)){
            if  (sessions.get(id) == null){
                return true;
            }
        }
        return false;
    }

    public Enumeration getSessions(){ return sessions.elements(); }

    public String printSessions(){
        StringBuffer buff = new StringBuffer();
        buff.append("Active Sessions:");
        Enumeration keys = sessions.keys();
        Enumeration objs = sessions.elements();
        while (keys.hasMoreElements()){
            buff.append("KEY: "+keys.nextElement());
            buff.append("OBJ: "+objs.nextElement());
        }
        return buff.toString();
    }

    public String printSessionKeys(){
        StringBuffer buff = new StringBuffer();
        buff.append("Active Sessions:\r\n");
        Enumeration keys = sessions.keys();
        while (keys.hasMoreElements()){
            buff.append("\r\nKEY: "+keys.nextElement());
        }
        return buff.toString();
    }

    public String showSessions(String baseUrl, String username){
        StringBuffer buf = new StringBuffer("Active Sessions:\r\n");
        Enumeration keys = sessions.keys();
        Enumeration objs = sessions.elements();
        String keyname;
        while (keys.hasMoreElements()){
            keyname = (String)keys.nextElement();
            if ( isDetailItemLocked(keyname) ) {
                buf.append("KEY: "+keyname+"\r\n");
            } else {
                buf.append("KEY: <a href='"+baseUrl+"?showSessionObjectDetail="+keyname
                            +"&USER="+username+"'>"
                            +keyname+"</a>\r\n");
            }
            Object obj = objs.nextElement();
            buf.append("OBJ: "+obj.getClass().getName()+"\r\n");
            buf.append("VALUE: "+obj.toString()+"\r\n\r\n");
        }
        return buf.toString();
    }

    public String showObjectDetail(String id){
     if ( isDetailItemLocked(id) ) {
            //this should not be possible.  Means a hacker is faking the link
            // that would have been formatted by showSessionObjects if the item
            // were not locked.
            return "Detail not allowed for this object";
        }
        Object obj = getSession(id);
        if ( obj != null ) {
            return "<pre>"+obj.toString()+"</pre>";
        }
        return "Null entry";
    }

    public int getCriticalItemCount() {
        int criticalCount = 0;
        Enumeration e = getSessions();
        ISessionItem sd;
        Object obj;
        while ( e.hasMoreElements()){
            obj = e.nextElement();
            if ( obj == null ){
                continue;
            }
            if (! (obj instanceof ISessionItem) ){
                continue;
            }
            sd = (ISessionItem)obj;
            if ( sd.isCritical() ) {
                criticalCount++;
            }
        }
        return criticalCount;
    }

    public void clearTemplateCache(){
        cache.clear();
    }

    public int getCacheCount(){
        return cache.size();
    }

    public boolean getWantCache(){
        return wantCache;
    }

    public void setWantCache(boolean new_wantCache){
        if (!new_wantCache && wantCache)
            cache.clear();
        wantCache = new_wantCache;
    }

    public synchronized String getCachedString(String key){
        if (!wantCache)
            return null;
        return (String)cache.get(key);
    }

    public synchronized void putCachedString(String key, String value){
        if (wantCache)
            cache.put(key, value);
    }

    public int CLEAN_WHICH_ALL = 1;
    public int CLEAN_WHICH_EXPIRED = 2;

    public synchronized void cleanSessionTable(int which){
        Enumeration e = getSessions();
        Enumeration keys = sessions.keys();
        ISessionItem sd;
        long now, startTime, lastAccessTime;
        Object obj, key;
        now = Tools.now().longValue();
        while ( e.hasMoreElements()){
            obj = e.nextElement();
            key = keys.nextElement(); //should go in lockstep with sessions.objects.
            if ( obj == null ) {
                if (key == null){
                    logWarn("NULL session found in session table with null key also ... ignoring.");
                } else {
                    logWarn("NULL session found in session table: "+key+" ... ignoring.");
                }
                continue;
            }
            if (! (obj instanceof ISessionItem) ) {
                continue;
            }
            sd = (ISessionItem)obj;

            if ( which == CLEAN_WHICH_ALL ) {
                try{
                    logDebug("shutting down session: {"+sd+"} at "+Tools.nowLocale());
                    CloseSessionTimerTask.startCloseSessionTimerTask(sd);
                    //sd.shutdown();
                } catch (Exception e2){
                    logError("Error shutting down session: "+e2.toString());
                    e2.printStackTrace();
                    if (key != null){
                        logError("key.toString: "+key.toString());
                    }
                }
                try {
                    sessions.remove(key);
                } catch (Exception e3){
                    logError("Error removing session: "+e3.toString());
                    e3.printStackTrace();
                    if (key != null){
                        logError("key.toString: "+key.toString());
                    }
                }
            } else {   //CLEAN_WHICH_EXPIRED
                //Now rely on timeout() managing its own decision as to timing out.
                if (sd.timeout(now)) {
                    sessions.remove(key);
                    logDebug("Session expired: "+key);
                }
            }

        }
    }

    //======== Session Table ==========================
    private static final String ST_HDR = "<TH>Program</TH><TH>Current Page</TH><TH>Modes</TH><TH>Close</TH><TH>Running</TH><TH>Last Access</TH><TH>Info</TH>";
    private static final String ST_HDRE = "<TH>User</TH><TH>Machine</TH>";
    private static final String ST_NS = "<center><h3>No Active Sessions</h3></center>";
    private static final String ST_TS = "<TABLE BORDER='1'>\r\n";
    private static final String ST_TE = "</TABLE>";
    private static final String ST_RS = "\t<TR><TD>";
    private static final String ST_RE = "</TD></TR>\r\n";
    private static final String ST_DS = "<TD>";
    private static final String ST_DE = "</TD>";
    private static final String ST_TDTD = "</TD><TD>";

    private static final String HDR = "Program\tCurrent Page\tReport\tClose\tRunning\tLast Access\tInfo\t\r\n";
    private static final String HDRE = "User\tMachine\t";

    public String formatSessionTable(String userName){
        return formatSessionTable(userName, "", "");
    }

    public String formatSessionTable(String userName, String detailTemplate, String closelinkTemplate){
        return formatSessionTable(userName,
                                  ST_HDR, ST_HDRE, ST_NS, ST_TS, ST_TE, ST_RS, ST_RE, ST_TDTD, ST_DS, ST_DE,
                                  detailTemplate,
                                  closelinkTemplate);
    }

    public String formatSessionTable(String userName, boolean htmlOutput){
        if (htmlOutput){
            return formatSessionTable(userName);
        } else {
            String lf = "\r\n";
            //                                  hdr, hdre, ns,                   ts, te, rs, re, tdtd  ds  de
            return formatSessionTable(userName, HDR, HDRE, "No Active Sessions", lf, lf, lf, lf, "\t", "", "", "", "");
        }

    }

    //ds = detail start, de=detail end, rs = row start, re = row end
    @SuppressWarnings("deprecation")
	public String formatSessionTable(String userName, String hdr, String hdre, String ns,
                                     String ts, String te, String rs, String re, String tdtd, String ds, String de,
                                     String detailTemplate, String closelinkTemplate){
        StringBuffer result = new StringBuffer();
        StringBuffer resultRow = new StringBuffer();
        String key = "";
        StringList rows = new StringList();
        boolean allUsers = false;
        if (userName.equals("*")){
            allUsers = true;
        }
        Enumeration objects = getSessions();
        if ( ! objects.hasMoreElements()){
            result.append(ns);
        } else {
            result.append(ts);

            result.append(rs);
            if (allUsers){
                result.append(hdre);
            }
            result.append(hdr);
            result.append(re);
            ISessionTableItem sd;
            String lastAccessTime, elapsed;
            long runningTime, hours, minutes, seconds, remainder;
            double dRunningTime;
            java.util.Date dStartTime, dLastAccessTime;
            String T = tdtd;

            long now = Tools.now().longValue();
            Object obj;
            while ( objects.hasMoreElements()){
                obj = objects.nextElement();
                if (obj == null){
                    continue;
                }
                if (! (obj instanceof ISessionTableItem) )
                    continue;
                sd = (ISessionTableItem)obj;
                if (! allUsers && ! sd.getUserName().equals(userName)){
                    continue;
                }
                dStartTime = (new java.util.Date(sd.getStartTime()));
                //startTime = dStartTime.toLocaleString();
                dLastAccessTime = (new java.util.Date(sd.getLastAccessTime()));
                lastAccessTime = dLastAccessTime.toLocaleString();
                //runningTime = dLastAccessTime.getTime() - dStartTime.getTime();
                runningTime = now - dStartTime.getTime();
                runningTime = runningTime / 1000; //convert to seconds from millis.
                hours = runningTime / 3600;
                remainder = runningTime % 3600;
                minutes = remainder / 60;
                seconds = remainder % 60;
                elapsed = ""+ hours
                      + ( (""+minutes).length()==1 ? ":0" : ":" ) + minutes
                      + ( (""+seconds).length()==1 ? ":0" : ":" ) + seconds;

                key = elapsed;

                resultRow.setLength(0);

                    resultRow.append(rs);
                    resultRow.append(ds);
                    if (allUsers){
                        resultRow.append(sd.getUserName() +T);
                        resultRow.append(sd.getHostName() +T);
                    }
                    String closelink;
                    if (closelinkTemplate.length()>0){
                        closelink = StringTools.searchAndReplaceAll(closelinkTemplate, "$SESSIONID", sd.getSessionID());
                    } else {
                        closelink = sd.getCloseLink();
                    }
                    String detail;
                    if (detailTemplate.length()>0){
                        detail = StringTools.searchAndReplaceAll(detailTemplate, "$SESSIONID", sd.getSessionID());
                    } else {
                        detail = sd.getExtraInfo();
                    }
                    resultRow.append(  sd.getAttachToLink() +T+ //formatLinkForSessionID(sd.sessionID) +T+
                                    sd.getCurrentPageID() +T+
                                    "<code>"+sd.getModeListing()+"</code>" +T+
                                    closelink+T+ //formatCloseLinkForSessionID(sd.sessionID) +T+
                                    elapsed  +T+
                                    lastAccessTime +T+
                                    detail
                                    );
                    resultRow.append(de);
                    resultRow.append(re);

                rows.addObject(key, resultRow.toString());

            }
            rows.sort();
            Enumeration rowsEnum = rows.objects();
            while ( rowsEnum.hasMoreElements() ) {
                result.append(rowsEnum.nextElement().toString());
            }

            result.append(te);
        }
        return result.toString();
    }

    public String formatSessionItemUptime(ISessionTableItem sd){
        String elapsed;
        long runningTime, hours, minutes, seconds, remainder;
        java.util.Date dStartTime;
        long now = Tools.now().longValue();
        dStartTime = (new java.util.Date(sd.getStartTime()));
        runningTime = now - dStartTime.getTime();
        runningTime = runningTime / 1000; //convert to seconds from millis.
        hours = runningTime / 3600;
        remainder = runningTime % 3600;
        minutes = remainder / 60;
        seconds = remainder % 60;
        elapsed = ""+ hours
                + ( (""+minutes).length()==1 ? ":0" : ":" ) + minutes
                + ( (""+seconds).length()==1 ? ":0" : ":" ) + seconds;
        return elapsed;
    }

    public String formatSessionList(String userName, ISessionTableItem current){
        StringBuffer result = new StringBuffer();
        Enumeration objects = getSessions();
        result.append("<TABLE  CELLPADDING=0 CELLSPACING=0 BORDER=0>\r\n");
        if ( objects.hasMoreElements()){
            ISessionTableItem sd;
            long runningTime, hours, minutes, seconds, remainder;
            double dRunningTime;
            java.util.Date dStartTime, dLastAccessTime;

            Object obj;
            while ( objects.hasMoreElements()){
                obj = objects.nextElement();
                if (obj == null){
                    continue;
                }
                if (! (obj instanceof ISessionTableItem) )
                    continue;
                sd = (ISessionTableItem)obj;
                if (! sd.getUserName().equals(userName)){
                    continue;
                }
                result.append("<TR><TD>");
                if (current != null && sd == current){
                    result.append("<b>"+sd.getActiveItemTag()+"</b>");
                } else {
                    result.append(sd.getAttachToLink());
                }
                result.append("</TD></TR>\r\n");
            }
            result.append("</TABLE>\r\n");
        }
        return result.toString();
    }

    //========Static accessors=========================

    private static SessionDatabase instance = null;

    /** You use this to get the singleton object database for cached web extensions.
     *  Be careful when you retrieve objects from the database.
     *  The database can be used to store objects of different types.
     *  The objects do not persist after run-time, however, so each invocation of the
     *  Java VM will mean a fresh database.  The database's store method:  addSession()
     *  will return you a unique ID for each object stored.
     */
    public static SessionDatabase getSharedDB(){
        if (instance == null)
            instance = new  SessionDatabase();
        //System.out.println("%%%%% getSharedDB: "+instance);
        return instance;
    }




}
