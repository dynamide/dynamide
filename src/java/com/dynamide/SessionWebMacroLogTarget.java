package com.dynamide;

// I casn't make this work.  It is not in use.  If I register for null, I get "system" categories, but if
//i register for others, I get nothing.

import java.io.FileNotFoundException;

import com.dynamide.util.Tools;

    public class SessionWebMacroLogTarget implements org.webmacro.util.LogTarget {

        public SessionWebMacroLogTarget()  throws FileNotFoundException {
            System.out.println("SessionWebMacroLogTarget create");
        }

        private StringBuffer m_buffer = new StringBuffer();

        public String dump(){
            return m_buffer.toString();
        }

        public void log(String message){
                m_buffer.append(message);
        }

        //---------- Interface LogTarget --------------------

        public void log(java.util.Date date, String type, String level, String message, Throwable e){
                m_buffer.append(""+type+" "+message + "\r\n"+ (e==null?"":Tools.errorToString(e, true)));
                System.out.println("WMSL: "+message);
        }

        /**
            * Flush the log. This will be called after writing methods that
            * are notice, warning, or error messages.
            */
        public void flush(){
                m_buffer.append("\r\n");
                System.out.println("WMSL flush");
        }

        /**
            * Return true or false if this log target would like to receive
            * log messages for the named category, type, and logLevel. This
            * method must return the same value every time it is called with
            * the same arguments.
            * <p>
            * The logLevel you will be called with is one of the integers
            * Log.ALL, Log.DEBUG, Log.INFO, Log.NOTICE, Log.WARNING,
            * Log.ERROR, and Log.NONE in ascending order (Log.ERROR is a higher
            * number than Log.WARNING which is a higher number than Log.DEBUG).
            * In other words, the higher the logLevel the more important the
            * log message is.
            */
        public boolean subscribe(String category, String type, int logLevel){
                //boolean val = super.subscribe(category, type, logLevel);
                System.out.println("WMSL: subscribe "+category+" type: "+type);
                return true;
                //return (logLevel >= org.webmacro.util.LogSystem.WARNING);
        }

        /**
            * A LogSystem will register itself though this method in order to
            * detect changes to the LogTarget. LogTargets should notify all
            * observers when any setting changes that might affect the
            * return value of the subscribe(...) method.
            */
        public void addObserver(org.webmacro.util.LogSystem ls){
                System.out.println("WMSL addObserver");
                ls.update(this,"parser");

        }

        /**
            * A LogSystem may remove itself through this method if it
            * de-registeres the LogTarget. After this method the supplied
            * observer should no longer receive notification of updates.
            */
        public void removeObserver(org.webmacro.util.LogSystem ls){
                System.out.println("WMSL removeObserver");
        }

    }
