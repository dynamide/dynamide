package com.dynamide.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

public class DynamideLogAppender extends AppenderSkeleton {
    public void append(LoggingEvent event){
        //you have access to: event.priority and event.categoryName, etc.
        //but here all we do is count, so that JUnit tests can see if there were errors reported via the log system.
        //log4j 1.2.8:
        /*Level level = event.getLevel();
        if (level.equals(Level.WARN)){
            m_warnCount++;
        } else if (level.equals(Level.ERROR)){
            m_errorCount++;
        }
        */
        //log4j 1.0.4:
        if (event.priority.equals(Priority.WARN)){
            m_warnCount++;
        } else if (event.priority.equals(Priority.ERROR)){
            m_errorCount++;
        }
    }
    public boolean requiresLayout() {
        return false;
    }
    public void close() {
        return;
    }

    //=============================================================================

    private int m_errorCount = 0;

    /** @return The number of errors in this run.  This is not very useful for long-running processes,
     *  such as servlets, where multiple Sessions could be generating errors,
     * but is useful for JUnit testing, where the JVM runs with exactly one com.dynamide.Session.
     */
    public int getErrorCount(){return m_errorCount;}

    public void setErrorCount(int new_value){m_errorCount = new_value;}

    private int m_warnCount = 0;
    /** @return The number of warnings in this run.  This is not very useful for long-running processes,
     *  such as servlets, where multiple Sessions could be generating errors,
     * but is useful for JUnit testing, where the JVM runs with exactly one com.dynamide.Session.
     */
    public int getWarnCount(){return m_warnCount;}

    public void setWarnCount(int new_value){m_warnCount = new_value;}


    public int getWarnAndErrorCount(){return m_warnCount+m_errorCount;}

    private static DynamideLogAppender gDynamideLogAppender = null;

    public final static DynamideLogAppender getAppender(){
        return gDynamideLogAppender;
    }

    public final static DynamideLogAppender installAppender(){
        if ( gDynamideLogAppender != null ) {
            Category root = Category.getRoot();
            Log.debug(DynamideLogAppender.class, "Re-initializing DynamideLogAppender, (not re-adding DynamideLogAppender).");
        } else {
            Category root = Category.getRoot();
            gDynamideLogAppender = new DynamideLogAppender();
            root.addAppender(gDynamideLogAppender);
            //Log.debug(DynamideLogAppender.class, "Added DynamideLogAppender");
        }
        return gDynamideLogAppender;
    }

}
