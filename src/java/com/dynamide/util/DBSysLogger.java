package com.dynamide.util;

import com.dynamide.db.RDBDatabase;
import com.dynamide.db.RDBDatasource;
import com.dynamide.resource.ResourceManager;
import com.dynamide.resource.ILogger;

public class DBSysLogger implements ILogger {

    public static boolean DISABLED = true;

    private RDBDatasource m_datasource = null;
    public RDBDatasource getDatasource(){return m_datasource;}

    private String m_dbname = "";
    public String getDBName(){return m_dbname;}

    private String m_tableName = "errorlog";
    public String getTableName(){return m_tableName;}
    public void setTableName(String new_value){m_tableName = new_value;}

    RDBDatabase m_rdb = null;

    RDBDatasource m_ds = null;

    boolean m_connected = false;

    String m_hostname = "";

    public void connect(ResourceManager rm, String dbname)
    throws Exception {
        System.out.println("========== DBSysLogger.connect. DISABLED: "+DISABLED);
        if (DISABLED){
            return;
        }
        m_dbname = dbname;
        RDBDatabase db = rm.openDatabase(dbname, null);
        db.setDebugSQL(false);
        db.setUseLog4j(false);
        /*m_ds = new RDBDatasource();
        m_ds.setDatabase(m_rdb);
        m_ds.load("SELECT * from errorlog;");
        m_ds.setProperty("tablename", "errorlog");
         */
        m_hostname = rm.getHostName();
        m_rdb = db;
        m_connected = true;
    }

    public void log(org.apache.log4j.Priority priority,
                    String sessionid,
                    String threadid,
                    String stacktrace,
                    String message,
                    String errorURI){
        log(priority, sessionid, "", threadid, "", "", "", stacktrace, message, "", message.substring(0, 20)+"...", errorURI);
    }

    public void log(org.apache.log4j.Priority priority,
                    String sessionid,
                    String applicationid,
                    String threadid,
                    String account,
                    String login,
                    String resourcename,
                    String stacktrace,
                    String message,
                    String exceptionName,
                    String shortMessage,
                    String errorURI){
        try {
            System.out.println("========== DBSysLogger.log. DISABLED. Message: "+message);
            if (DISABLED){
                return;
            }
            String level = priority.toString();

            applicationid = RDBDatabase.escapeForPostgres(applicationid);
            resourcename = RDBDatabase.escapeForPostgres(resourcename);
            stacktrace = RDBDatabase.escapeForPostgres(stacktrace);
            message = RDBDatabase.escapeForPostgres(message);

            String sql =
            "INSERT into "+m_tableName+" "
             + "(HOST, LEVEL, SESSIONID, APPLICATIONID, THREADID, ACCOUNT, LOGIN, RESOURCENAME, STACKTRACE, MESSAGE)"
             + " VALUES ('"+m_hostname+"', '"+level+"', '"+sessionid+"', '"+applicationid+"', '"+threadid+"', '"+account+"', '"+login+"', '"+resourcename+"', '"+stacktrace+"', '"+message+"');";
            if ( m_rdb != null ) {
                m_rdb.execute(sql, false, "DBSysLogger");
            } else {
                System.out.println("Can't log, because database is still null. SQL: "+sql);
            }
        } catch (Exception e)  {
            System.err.println("ERROR: in "+toString()+" "+Tools.errorToString(e, true)+"\r\nCould not log message for "+sessionid+" in "+threadid+"\r\nmessage: "+message);
        }
    }
    
    public synchronized void flush(){
    	//db flushes automatically.
    }
	

            /*
            //System.out.println("DBSysLogger.log m_connected: "+m_connected);
            m_ds.insert();
            m_ds.setFieldValue("level", level);
            m_ds.setFieldValue("sessionid", sessionid);
            m_ds.setFieldValue("applicationid", applicationid);
            m_ds.setFieldValue("threadid", threadid);
            m_ds.setFieldValue("account", account);
            m_ds.setFieldValue("login", login);
            m_ds.setFieldValue("resourcename", resourcename);
            m_ds.setFieldValue("stacktrace", stacktrace);
            m_ds.setFieldValue("message", message);
            m_ds.setFieldValue("host", m_hostname);
            m_ds.post();
            */

}
