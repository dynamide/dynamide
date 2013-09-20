package com.dynamide.resource;

import com.dynamide.Session;

import com.dynamide.event.ScriptEvent;

import com.dynamide.util.Log;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class Job implements Runnable {
    public Job(ThreadGroup group,
               String name,
               Session session,
               String installedName,
               String eventName,
               Object inputObject,
               long startDelay){
        this.m_name = name;
        this.m_group = group;
        this.session = session;
        m_eventName = eventName;
        m_inputObject = inputObject;
        this.m_startDelay = startDelay;
        this.m_busy = false;
        this.installedName = installedName;
        String useDetached = session.getPropertyStringValue("jobUseDetachedInterpreter");
        this.m_useDetachedInterpreter = useDetached.length() == 0
                                      ? true
                                      : Tools.isTrue(useDetached);
    }

    public String toString(){
        return "com.dynamide.resource.Job{name:"+m_name+",installedName:"+installedName+",startDelay:"+m_startDelay
                +",busy:"+m_busy+",jobInterval:"+m_jobInterval+",jobCount:"+m_jobCount+",session:"+session+"}";
    }

    public void start(){
        if (m_busy){
            Log.error(Job.class, "Job can't start() because it is busy with previous start "+m_name+" :: "+installedName);
            return;
        }
        this.m_busy = true;
        new Thread(m_group, this, m_name+"."+(m_count)).start();
    }

    private ThreadGroup m_group = null;
    private String m_name = "";
    public String getName(){return m_name;}

    public String getFullName(){
        return getName()+
            ((installedName.length()>0) ? "."+installedName : "");
    }

    private Session session;
    public Session getSession(){return session;}

    private String installedName = "";

    private String m_eventName = "";
    private Object m_inputObject;

    private int m_jobCount = 1;
    public int getJobCount(){return m_jobCount;}
    public void setJobCount(int new_value){m_jobCount = new_value;}

    private int  m_jobInterval = -1;
    public int  getJobInterval(){return m_jobInterval;}
    public void setJobInterval(int  new_value){
        m_jobInterval = new_value;
        updateNextStartTime();
    }

    private long updateNextStartTime(){
        m_nextStartTime = Tools.now().longValue() + m_jobInterval;
        return m_nextStartTime;
    }
    private long m_nextStartTime = 0;
    public long getNextStartTime(){
        return m_nextStartTime;
    }

    private Object m_outputObject = null;
    public Object getOutputObject(){return m_outputObject;}
    public void setOutputObject(Object new_value){m_outputObject = new_value;}

    private long m_startDelay = 0;
    public long getstartDelay(){return m_startDelay;}

    private int m_count = 0;
    public int getCount(){return m_count;}
    public void setCount(int new_value){m_count = new_value;}

    private boolean m_busy = false;
    public boolean getBusy(){return m_busy;}

    private boolean m_useDetachedInterpreter = true;

    public void close(){
        close(true);
    }

    private boolean m_closing = false;

    public void close(boolean removeInstalledJob){
        if ( m_closing ) {
            return;
        }
        m_closing = true;
        if (installedName.length()>0) {
            Log.info(Job.class, "Installed Job closing: '"+toString());//+" at "+Tools.getStackTrace());
        } else {
            Log.info(Job.class, "Job closing: "+toString());
        }
        if ( session != null ) {
            boolean closeSession = Tools.isTrue(session.getPropertyStringValue("closeOnJobClose"));
            if ( closeSession ) {
                session.close();
            }
        }
        session = null;
        m_inputObject = null;
        m_group = null;
        if (removeInstalledJob && installedName.length()>0){
            ResourceManager rm = ResourceManager.getRootResourceManager();
            rm.removeInstalledJob(installedName, false);
        }
    }

    // %%%%%% Running jobs will hold onto Session object, which is a leak if they go zombie
    public void run(){
        try {
            if (m_closing) return;
            if ( session == null ) {
                Log.error(Job.class, "Can't run() because session is null. Exiting.");
                close();
                return;
            }
            String fullname = getFullName();
            Log.debug(Job.class, "Job run() "+fullname+" "+Tools.nowLocale());
            String threadName = Thread.currentThread().getName();
            int jc = getJobCount();
            if ((jc != -1) && (getCount() >= jc)){
                throw new Exception("job count exceeded ("+getCount()+" > "+getJobCount()+"), can't run.");
            }
            session.logJobStart(m_eventName, threadName);
            if ( m_startDelay>0 ) {
                session.logJob(m_eventName, null, fullname, "Job sleeping for "+m_startDelay+" ms");
                try {Thread.sleep(m_startDelay);} catch (Exception e)  {}
                if (m_closing) return;
                session.logJob(m_eventName, null, fullname, "Job running after sleep");
            } else {
                session.logJob(m_eventName, null, fullname, "Job running");
            }
            m_startDelay = 0; //only do it for the first time, not when we are restarted by the ResourceManager when jobCount>1.
            ScriptEvent event = null;
            m_count++;
            try {
                event = session.fireJobEvent(this, m_eventName, m_inputObject, m_useDetachedInterpreter);
            } catch (Throwable t) {
                boolean closed = handleThrowable(t);
                if ( closed ) {
                    return;
                }
            }
            if ( event == null ) {
                Log.error(Job.class, "ScriptEvent was null, forced Job close for: "+toString());
                return;
            }
            if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                boolean closed = handleThrowable(null);
                if ( closed ) {
                    return;
                }
            }
            boolean doLogDebug = Log.isDebugEnabled(this);
            if (doLogDebug){
                String dump = event.dump();
                Log.debug(Job.class, "Job finished one run. count:"+getCount()
                                    +" getJobCount: "+getJobCount()+" dump:\r\n"+dump);
            }
            session.logJob(m_eventName, event, fullname, "");
            session.onJobDone(this);
            jc = getJobCount();
            if ((jc != -1) && (getCount() >= jc)){
                close();
                /////////////////// AFTER THIS POINT, SESSION CAN BE NULL, SINCE close() NULLIFIES IT.
                session = null;
            } else {
                updateNextStartTime();
            }
        } catch (Throwable e){
            Log.error(Job.class, "Job.run() in "+getFullName(), e);
            try {
                session.logJob(m_eventName, null, getFullName(), "Job Exception: "+StringTools.escape(e.toString()));
            } catch (Exception e2)  {
                //Just attempting some logging.  If it fails, oh well.
            }
            handleThrowable(e);
        } finally {
            this.m_busy = false;
        }
    }

    private boolean handleThrowable(Throwable e){
        try {
            ScriptEvent event = session.fireJobEvent(this, "application_onJobError", e, true);
            if (event != null && event.resultCode == ScriptEvent.RC_OK){
                boolean closeJob = Tools.isTrue(event.getOutputObject());
                if ( closeJob ) {
                    Log.error(Job.class, getFullName()+" Job closing after error; application_onJobError returned true to close.", e);
                    close();
                    return true;
                }
            } else {
                Log.error(Job.class, getFullName()+" Job closing after error, and application_onJobError not found.", e);
                close();
                return true;
            }
        } catch (Throwable e3)  {
            close();
            return true;
        }
        return false;
    }

}
