package com.dynamide.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

import com.dynamide.resource.ResourceManager;

public class DBAppender extends AppenderSkeleton {

  public void close(){
  }

  //called by AppenderSkeleton.doAppend(event)
  public void append(LoggingEvent event) {
    if ( ! event.priority.isGreaterOrEqual(Priority.WARN)  ) {
        return;
    }
    System.out.println("in DBAppender.append "+event);
    String text = event.message;
    if ( this.layout != null ) {
        text = this.layout.format(event);
    }
    //log4j 1.2.8: String trace = Tools.getStackTrace(event.getThrowableInformation().getThrowable());
    //log4j 1.0.4:
    String trace = event.getThrowableInformation();
    //System.out.println(text+trace);
    try {
    	com.dynamide.resource.ILogger syslog = ResourceManager.getErrorLogger();
        if ( syslog == null ) {
            System.err.println("DBSysLogger not connected yet.  Skipping log entry: "+text);
            return;
        }
        syslog.log(event.priority,
                   event.categoryName, //     String sessionid,
                   event.threadName, //     String threadid,
                   event.getThrowableInformation(), //     String stacktrace,
                   text, //String message
                   "");
    } catch (Exception e)  {
        System.err.println("Could not log in DBAppender: "+e);
    }
  }

  public boolean requiresLayout() {
    return true;
  }
}


