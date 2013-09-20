package com.dynamide.util;

import java.util.Timer;
import java.util.TimerTask;

public class CloseSessionTimerTask extends TimerTask {
    public CloseSessionTimerTask(ISessionItem sessionItem){
         m_sessionItem = sessionItem;
    }
    private ISessionItem m_sessionItem;

    /**The time that elapses before we actually close the ISessionItem and remove it from the db.
     */
    public static final int CLOSE_TIMER_INTERVAL_MILLIS = 1000;

    /** If you are waiting for the table to be clear, use this number.
     *  %% KLUDGE. Really we should pend the deletes in the session database,
     *  mark them as killed, move this code into sessiondatabase, and not show killed sessions
     *  except to superuser. %% TODO.
     */
    public static final int CLOSE_TIMER_SAFE_MILLIS = 2000;

    public void run(){
        if (m_sessionItem != null){
            try{
                Log.debug(this, "running shutdown for "+m_sessionItem.getSessionID());
                m_sessionItem.shutdown();
            } catch (Throwable t){
                Log.error(this, "throwable caught in run()", t);
            }
        }
        m_sessionItem = null;
    }

    public static void startCloseSessionTimerTask(ISessionItem sessionItem){
        Timer timer = new Timer(true);
        CloseSessionTimerTask task = new CloseSessionTimerTask(sessionItem);
        timer.schedule(task, 2000);//run two seconds from now.
    }
}
