package com.dynamide.util;

public interface ISessionItem{
    /** Check the sessions table for timeouts. */
    public static final long TIMEOUT_CHECK_INTERVAL = 60*1000; //1 minute, in milliseconds.
    /** @return true if OK to timeout and remove from sessions table.  If true,
     *   implementing object should perform its cleanup.  It does not need to
     *   remove itself from the SessionDatabase's session table.*/
    public boolean timeout(long now);
    /** The shutdown method is called in a new thread, and you will be out of the session
     *  table by the time this gets called.
     */
    public void shutdown();
    public boolean isCritical();
    public String getSessionID();
    public String getParentSessionID();
    public boolean hasChildSession(String childSessionID);
}

