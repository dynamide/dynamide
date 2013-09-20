package com.dynamide.interpreters;

import java.util.Enumeration;

import bsh.Interpreter;

import com.dynamide.DynamideException;
import com.dynamide.DynamideObject;
import com.dynamide.Session;
import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.util.Log;
import com.dynamide.util.Opts;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class BshInterpreter extends DynamideObject
implements IInterpreter {

    //null constructor required by call to Class.forName by factory
    public BshInterpreter(){
        super(null);
        //System.out.println("BshInterpreter(null) "+getObjectID());
    }

    public BshInterpreter(DynamideObject owner){
        super(owner);
        //System.out.println("BshInterpreter(owner:"+owner.toString()+") "+getObjectID());
    }

    public void close()
    throws Throwable {
        if ( globalNameSpace != null ) {
            //1.2b6 and higher:
            globalNameSpace.clear();
            globalNameSpace = null;
        }
        //m_Interpreter.setConsole(null);
        m_Interpreter.setNameSpace(null);
        m_console.close();
        m_Interpreter = null;
        m_session = null;
        setOwner(null);
        if (m_variables != null){
            m_variables.clear();
        }
        m_variables = null;
        m_console = null;
    }


    public void finalize() throws Throwable {
        close();
        super.finalize();
        //System.out.println("BshInterpreter.finalize: "+m_sessionID+" objectid: "+getObjectID() );
    }

    private String m_sessionID = "";
    private Session m_session = null;
    public Session getSession(){
        return m_session;
    }
    public void setSession(Session session){
        m_session = session;
        if ( m_session!=null ) {
            m_sessionID = m_session.getSessionID();
        }
    }

    //%% todo: make sure user is on ACL:
    /** Evaluate any string in the beanshell interpreter, the only variable in the context already
     *  is a pointer to this session, in a variable called "session".  However, the session variable
     *  gives you the keys to the kingdom.  See also DynamideObject.expand(String) which is used to
     *  expand expressions in the Webmacro interpreter.
     */
    public Object eval(String source)
    throws Exception {
        //will throw a bsh.EvalError, but we want this to be pluggable for different interpreters, so all Exceptions are allowed.
        // %% todo: make a com.dynamide.interpreters.InterpreterException and nest the exception and also grab and
        //  translate the line number and error message if available.
        Session session = getSession();
        if ( session == null ) {
            throw new DynamideException("Session is null in BshInterpreter.eval");
        }
        bsh.Interpreter interp = getInterpreter();
        interp.set("session", session);
        try {
            Object res = interp.eval(source);
            if ( res == null ) {
                res = null; //6/4/2003 4:37AM "NO RESULT";
            }
            return res;
        } finally {
            interp.unset("session");
        }
    }

    public ScriptEvent fireEvent(ScriptEvent event, String procName, ScriptEventSource eventSource, boolean sourceOnly){
    	    //System.out.println("BshInterpreter in "+procName);
            profileEnter("BshInterpreter.fireEvent");
            bsh.Interpreter interp = getInterpreter();
            try{
                synchronized (interp){
                    if (event == null){
                        logError("event object is null in BshInterpreter.fireEvent");
                        return event;
                    }
                    ScriptEvent oldEvent = (ScriptEvent)interp.get("event");
                    interp.set("event", event);//interp.setVariable("event", event);


                    //uuhhhhh, don't you want to hold onto the previous event,
                    //and restore it after the call, or use namespaces, maybe?

                    Session oldSession = null;
                    try {
                    	//System.out.println("BshInterpreter get session");
						oldSession = (Session)interp.get("session");
					} catch (Throwable eGet) {
						System.out.println("BshInterpreter error getting 'session' from interp variables: "+eGet);
					}
                    
                    interp.set("session", event.session);//interp.setVariable("session", event.session);
                    String key;
                    Object obj;
                    Enumeration en = m_variables.keys();
                    while ( en.hasMoreElements() ) {
                        key = (String)en.nextElement();
                        obj = m_variables.getObject(key);
                        interp.set(key, obj);//interp.setVariable(key, obj);
                    }
                    profileEnter("BshInterpreter.interp.eval");
                    try {
                        if ( sourceOnly ) {
                            interp.eval(eventSource.source);  //pull in procs, imports, etc.
                        } else {
                            interp.eval(eventSource.source);
                            //event.returnObject =
                            //well, the return is always null, since all procs are void return type. 5/25/2003 11:19AM
                            interp.eval("return "+procName+"(event);");
                            //System.out.println("event.returnObject from BshInterpreter: "+event.returnObject);
                        }
                    } finally {
                        profileLeave("BshInterpreter.interp.eval");
                        interp.unset("event");
                        interp.unset("session");
                        if ( oldSession!=null ) {
                            interp.set("session", oldSession);
                        }
                        if ( oldEvent!=null ) {
                            interp.set("event", oldEvent);
                        }
                        en = m_variables.keys();
                        while ( en.hasMoreElements() ) {
                            key = (String)en.nextElement();
                            interp.unset(key);
                        }
                    }
                } //end-synchronized
            } catch (bsh.EvalError e){
                if (event == null){
                    logError("[42.1] in bsh.EvalError, and event object is null.",e);
                } else {
                    String msgHTML = formatError(e, true, eventSource.source, event);
                    String msgTarget = "";
                    if (e!=null && e instanceof bsh.TargetError && ((bsh.TargetError)e).getTarget() != null ){
                    	msgTarget = Tools.errorToString(((bsh.TargetError)e).getTarget(), true, true);
                    	
                    }
                    //StringTools.makeLineNumbers(e.toString(), e.getErrorLineNumber(), -1, false);
                    String id = (event.session != null)
                       ? event.session.getSessionID()
                       : m_sessionID;
                    com.dynamide.resource.ResourceManager.writeErrorLog(
                                                         getSession(),
                                                         id,
                                                         eventSource.resourceName,
                                                         msgHTML+"<br />\r\n"+msgTarget,
                                                         Tools.getStackTrace(e),
                                                         e.getClass().getName());
                    event.resultCode = ScriptEvent.RC_ERROR;
                    event.evalErrorMsg = msgHTML;
                }
                return event;
            } catch (Exception e2){
                logError("[42.3] Error eval'ing with bsh", e2);
                event.resultCode = ScriptEvent.RC_ERROR;
                event.evalErrorMsg = StringTools.escape(e2.toString())
                            +"<hr/>STACK TRACE<hr/><pre><small>"
                            +StringTools.escape(Tools.getStackTrace(e2))
                            +"</small></pre><hr/>END STACK TRACE<hr/>";

                String id = (event.session != null)
                   ? event.session.getSessionID()
                   : m_sessionID;
                com.dynamide.resource.ResourceManager.writeErrorLog(getSession(), id, event.evalErrorMsg, event.evalErrorMsg, Tools.getStackTrace(e2),e2.getClass().getName());
                return event;
            }
            profileLeave("BshInterpreter.fireEvent");
            return event;
     }
    /* NOTE:
        * I tried lots of tricks to get the interpreter to use the real line number of the error,
        * but none seemed to work.  If you use the interpreter in interactive mode, it seems to
        * report the correct line number using "source" and "eval" from the shell.
        * But in compiled mode, it always reports line 1 as all errors.
        * %% Figure this out at some point.
        * For now, just do it the usual way.
        * String fn = "source"+Tools.now();
    * //FileTools.saveFile("C:\\temp", fn, eventSource+"\r\n"+procName+"(event);");
    * FileTools.saveFile("C:\\temp", fn, eventSource);
    * //interp.source("C:\\temp\\"+fn);
    * interp.eval("source(\"C:/temp/"+fn+"\");");

    //StringReader reader = new StringReader(eventSource);
    //BufferedReader in = new BufferedReader(new FileReader("C:\\temp\\"+fn));
    //interp.eval(in);
    StringReader reader = new StringReader(procName+"(event);");
    interp.eval(reader);
        */


    public String getVersion(){
         return "BeanShell Interpreter, Version: "+Interpreter.VERSION;
    }

    public String getOutputBuffer(){
        return getConsole().getBuffer();

    }

    public String emptyOutputBuffer(){
        return getConsole().emptyBuffer();
    }

    public void setVariable(String name, Object value){
        m_variables.remove(name);
        m_variables.addObject(name, value);
    }

    public void unsetVariable(String name){
        m_variables.remove(name);
    }

    //=================== implementation ==================================

    private StringList m_variables = new StringList();

    private IConsole m_console;
    private IConsole getConsole(){
        return m_console;
    }

    private bsh.Interpreter m_Interpreter;

    private bsh.NameSpace globalNameSpace = null;

    private static int g_count = 0;
    
    //for bsh 2.0
    private bsh.Interpreter getInterpreter(){
        if (m_Interpreter == null){
            m_console = new NullConsole("null-console-for:"+m_sessionID);
            //2.0 syntax, I haven't validated this against any sample code, but I just got it to work, on 20060924:
            String sourceFileInfo = m_sessionID != null ? m_sessionID : "nullsession";
            m_Interpreter = new bsh.Interpreter(m_console.getIn(), 
                                                m_console.getOut(), 
                                                m_console.getErr(), 
                                                false,                   //interactive
                                                null,//this.globalNameSpace     //namespace
                                                null,                    //parent
                                                sourceFileInfo
                                                );
            this.globalNameSpace = new bsh.NameSpace( m_Interpreter.getClassManager(), "localDynamide"+(g_count++));
            m_Interpreter.setNameSpace(this.globalNameSpace);   
            // %% NOt sure if this works, since some params are set in the constructor:   m_Interpreter.setConsole((bsh.ConsoleInterface)m_console);
            m_Interpreter.setClassLoader(getSession().getClass().getClassLoader());
            //System.out.println("BshInterpreter ["+toString()+"]new for Session: "+getSession());
        }
        return m_Interpreter;
    }
                                                    
    /* for bsh 1.2, 1.3 etc.
     private bsh.Interpreter getInterpreter(){
        if (m_Interpreter == null){
            //System.out.println("BshInterpreter ["+toString()+"]new for Session: "+getSession());

            m_console = new NullConsole("null-console-for:"+m_sessionID);
            //m_console = new BufferConsole("console-for:"+m_sessionID+":"+getObjectID());

        //if (BSH_2){
            bsh.BshClassManager bcm = bsh.BshClassManager.createClassManager(); //.getClassManager() in 2.0...?
            bcm.setClassLoader(getSession().getClass().getClassLoader());
            //2.0 syntax:
            this.globalNameSpace = new bsh.NameSpace( bcm, "localDynamide"+(g_count++));
        //} else {
            //using beanshell 1.2b3/1.3 syntax:
            //this.globalNameSpace = new bsh.NameSpace("localDynamide"+(g_count++));
        //}
            
            //m_Interpreter = new bsh.Interpreter((ConsoleInterface)m_console, globalNameSpace);
            //args: Reader in, PrintStream out, PrintStream err, boolean interactive, NameSpace namespace)
            m_Interpreter = new bsh.Interpreter(m_console.getIn(), m_console.getOut(), m_console.getErr(), false, globalNameSpace);
            m_Interpreter.setConsole((bsh.ConsoleInterface)m_console);
            //m_Interpreter = new bsh.Interpreter((ConsoleInterface)m_console);
            //m_Interpreter.strictJava = true;
            //System.out.println("======>  BeanShell classloader: "+m_Interpreter.getClass().getClassLoader().getClass().getName());
            m_Interpreter.setClassLoader(getSession().getClass().getClassLoader());
            //System.out.println("======>  BeanShell classloader: "+m_Interpreter.getClass().getClassLoader().getClass().getName());
            //System.out.println("======>  BeanShell session classloader: "+getSession().getClass().getClassLoader().getClass().getName());
            //m_Interpreter.setConsole(m_console);
            //System.out.println("======>  BeanShell Interpreter Version: "+m_Interpreter.VERSION);
            *try{
                System.out.print("======>  BeanShell ClassLoader: ");
                m_Interpreter.eval("System.out.println((new com.dynamide.Constants()).getClass().getClassLoader());");
            } catch (Exception e){
                e.printStackTrace();
            }*

            * This feature works, but is disabled, since it messes up when you try to load classes and compare their types.
             * for example:
             *     addClassPath(...);
             *     import com.Foo;
             *     if foo instanceof Foo <== this fails, since foo could be loaded by Resin classloader.
             *   Answer is to write a dynamide classloader some day....
             *
             * String path = Tools.fixFilename(m_session.getAppDirectory()+"/resources/classes");
             * File classesDir = new File(path);
             * if ( classesDir.exists() ) {
              *    try {
              *        path = StringTools.searchAndReplaceAll(path, "\\", "\\\\");
              *        String expr = "addClassPath(\""+path+"\");";
              *        //logDebug("======== expr: '"+expr+"'");
              *        m_Interpreter.eval(expr);
              *        //System.out.println("***************** WARNING ************ addClassPath is DISABLED.");
              *    } catch (Exception e){
              *        logDebug("Couldn't eval addClassPath in BshInterpreter.getInterpreter() path: "+path, e);
              *   }
             * }
             *

        }
        return m_Interpreter;
    }
    */

    private static void multiTest(){
        for (int i = 0; i< 10; i++) {
           BshInterpreter interp = new BshInterpreter();
           interp.testFire();
        }
    }

    private void testFire(){
        ScriptEvent event = new ScriptEvent();
        setVariable("foo", "bar");
        String body = "event.returnSource(\"hello, world.  foo == \"+foo);";
        String source =
                  " void mojo(event){\r\n"
                  +"   "+body+" \r\n"
                  +"}";

        event = fireEvent(event,
                  "mojo",
                  new ScriptEventSource("mojo", body, source, "beanshell", "testOwnerID", ""),
                  false);
        //System.out.println("\r\nevent: "+event.toString());
        unsetVariable("foo");
        System.out.println("buffer: "+event.resultSrc);
    }

    public static void usage(){
        System.out.println("Usage:");
        System.out.println("   BshInterpreter -testMemory");
        System.out.println("");
    }

    public static String formatError(Exception e, boolean html, String source, ScriptEvent event){
        String result;
            try {
                bsh.EvalError be = (bsh.EvalError)e;
                String errMsg = be.toString();
                int i =-1;
                try {
                	i = be.getErrorLineNumber();
                } catch (Exception ee) {
                	i = -1;
                }
                if ( i == -1 ) {
                    String AT_LINE = "at line ";  //can also be same with capitalizatiopn difference -- " at Line: 37 : " 
                    String AT_LINE2 = "at Line: ";  
                    int start;

                    if ( errMsg.indexOf(AT_LINE) > -1 ) {
                        start = errMsg.indexOf(AT_LINE);
                        if ( start > -1 ) {
                            int end = errMsg.indexOf(", ", start);
                            if ( end > -1 ) {
                                String snum = errMsg.substring(start+(AT_LINE.length()), end);
                                i = Tools.stringToInt(snum.trim());
                            }
                        }
                    } else if ( errMsg.indexOf(AT_LINE2) > -1 ) {
                        start = errMsg.indexOf(AT_LINE2);
                        if ( start > -1 ) {
                            int end = errMsg.indexOf(":", start);
                            if ( end > -1 ) {
                                String snum = errMsg.substring(start+(AT_LINE2.length()), end);
                                i = Tools.stringToInt(snum.trim());
                            }
                        }
                    }

                }
                if (event!=null) event.errorLineNumber = i;
                //System.out.println("mformatError: getErrorLineNumber: "+be.getErrorLineNumber()+" deduced line number: "+i);

                if (html){
                    result = "ERROR: [26] "+StringTools.wrap(StringTools.escape(errMsg), 60)
                            //+"<br/>errorText: "+StringTools.escape(be.getErrorText())
                            +"<hr/>"
                            +"<pre>"
                            +StringTools.makeLineNumbers(source, i)
                            +"</pre><hr/>STACK TRACE<hr/><pre><small>"
                            +StringTools.escape(Tools.getStackTrace(e))
                            +"</small></pre><hr/>END STACK TRACE<hr/>";
                } else {
                    result = errMsg + "\r\n\r\n========== Source ==========\r\n"+StringTools.makeLineNumbers(source, i, -1, false);

                }
            } catch (Exception badException){
                result = e.toString(); //just return the original exception, toString.
                Log.error(BshInterpreter.class, "Couldn't format interpreter exception because: "+badException);
            }
        return result;
    }

    public static void main(String args[])
    throws Exception {
        if ( args.length==0 ) {
            usage();
            System.exit(0);
        }

        Opts opts = new Opts(args);
        boolean testMemory = opts.getOptionBool("-testMemory");
        if (testMemory){


            System.out.println("version 1");

            System.out.println(Tools.cleanAndReportMemory());
            multiTest();
            System.out.println(Tools.cleanAndReportMemory());
            multiTest();
            System.out.println(Tools.cleanAndReportMemory());
            multiTest();
            System.out.println(Tools.cleanAndReportMemory());
            multiTest();
            System.out.println(Tools.cleanAndReportMemory());
            Thread.currentThread().getThreadGroup().list();

            /*System.out.println("press Enter");
            Tools.readln(System.in);
            for (int i = 0; i< 100; i++) interp.testFire();
            System.out.println("\r\npress Enter");
            Tools.readln(System.in);
            for (int i = 0; i< 100; i++) interp.testFire();
            interp = null;
            System.gc();
            Thread.currentThread().yield();
            Thread.currentThread().getThreadGroup().list();
            System.out.println("cleaned up. press Enter");
            Tools.readln(System.in);
            */

            System.out.println("done");
        }
        //C:\dynamide>java -DDYNAMIDE_HOME=C:/dynamide com.dynamide.interpreters.BshInterpreter -testSession
        boolean testSession = opts.getOptionBool("-testSession");
        if (testSession){
        	com.dynamide.resource.ResourceManager.createStandaloneForTest();
        	com.dynamide.Session.createSession("/dynamide/doco");
        	System.out.println("DONE");
        }
    }



}
