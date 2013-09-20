package com.dynamide.util;


public interface ISessionTableItem extends ISessionItem {
    public String getSessionID();
    public String getUserName();
    public String getHostName();
    public String getAttachToLink();
    public String getActiveItemTag();
    public String getCurrentPageID();
    public String getModeListing();
    public String getReportLink();
    public String getCloseLink();
    public String getExtraInfo();
    public long   getStartTime();
    public long   getLastAccessTime();
}
