package com.dynamide.interpreters;

// See old examples at end. 5/28/2003 9:19PM

import com.dynamide.DynamideException;
import com.dynamide.DynamideObject;
import com.dynamide.Session;

import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;

import com.dynamide.resource.ResourceManager;

import com.dynamide.util.StringList;

import tcl.lang.Interp;
import tcl.lang.ReflectObject;
import tcl.lang.TclObject;

public class TclInterpreter extends DynamideObject
implements IInterpreter {

    //null constructor required by call to Class.forName by factory
    public TclInterpreter(){
        super(null);
    }

    public TclInterpreter(DynamideObject owner){
        super(owner);
        if (owner instanceof Session){
            setSession((Session)owner);
        }
    }

    public void close() throws Throwable {
        //m_Interpreter.setConsole(null);
        setOwner(null);
        setSession(null);
        m_Interpreter = null;
        m_session = null;
        if (m_variables != null){
            m_variables.clear();
        }
        m_variables = null;
        m_console = null;
    }

    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private Session m_session = null;
    public Session getSession(){
        return m_session;
    }
    public void setSession(Session session){
        m_session = session;
    }

    //%% todo: make sure user is on ACL:
    /** Evaluate any string in the tcl/jacl interpreter, the only variable in the context already
     *  is a pointer to this session, in a variable called "session".  However, the session variable
     *  gives you the keys to the kingdom.  See also DynamideObject.expand(String) which is used to
     *  expand expressions in the Webmacro interpreter.
     */
    public Object eval(String source)
    throws Exception {
        Session session = getSession();
        if ( session == null ) {
            throw new DynamideException("Session is null in TclInterpreter.eval");
        }
        try {
            Interp interp = getInterpreter();

            ScriptEvent event = new ScriptEvent(session);
            event.currentPageID = "myPage";
            TclObject s = ReflectObject.newInstance(interp, session.getClass(), session);
            interp.setVar("session", s, 0);
            try {
                interp.eval(source);
                TclObject tclobj = interp.getResult();
                System.out.println("[com.dynamide.interpreters.TclInterpreter] evaling source. tcl obj to string:"+tclobj.toString());
                return tclobj.toString();
            } finally {
                interp.unsetVar("session", 0);
            }
        } catch (Exception tcle){
            System.out.println("Tcl exception: "+tcle);
            tcle.printStackTrace();
            return "ERROR";
        }
    }

     public ScriptEvent fireEvent(ScriptEvent event, String procName, ScriptEventSource eventSource, boolean sourceOnly){
            try {
                Interp interp = getInterpreter();
                TclObject r = ReflectObject.newInstance(interp, event.getClass(), event);
                interp.setVar("event", r, 0);
                TclObject s = ReflectObject.newInstance(interp, getSession().getClass(), getSession());
                interp.setVar("session", s, 0);
                try {
                    interp.eval(eventSource.body);
                    TclObject tclobj = interp.getResult();
                    event.outputObject = tclobj.toString(); //%% might want to get it as an Object, or let them set it in the event.
                    System.out.println("[com.dynamide.interpreters.TclInterpreter.fireEvent()] evaling source. tcl obj to string:"+tclobj.toString());
                } finally {
                    interp.unsetVar("session", 0);
                    interp.unsetVar("event", 0);
                }
            } catch (Exception e){
                logError("TclInterpreter error", e);
                event.evalErrorMsg = ""+e;
                event.resultCode = ScriptEvent.RC_ERROR;
            }
            return event;
     }

    public String getVersion(){
         return "Tcl Interpreter, Version: ";//+getInterpreter().VERSION;
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

    private tcl.lang.Interp  m_Interpreter;

    private tcl.lang.Interp getInterpreter()
    throws Exception {
        if (m_Interpreter == null){
            m_Interpreter = new tcl.lang.Interp();
            m_Interpreter.eval("package require java");
            m_console = new BufferConsole("tcl interpreter console");
        }
        return m_Interpreter;
    }

    //tests:

        //  java com.dynamide.interpreters.TclInterpreter "C:\dynamide\build\ROOT" "/dynamide/demo" "set mojo bar;"

        //  java com.dynamide.interpreters.TclInterpreter "C:\dynamide\build\ROOT" "/dynamide/demo" "puts  [$event getCurrentPageID]; set event;"

    public static void main(String args[])
    throws Exception {
        if ( args.length==0 ) {
            System.out.println("Error in usage: TclInterpreter <expression>");
            System.exit(0);
        }
        String RESOURCE_ROOT = args[0];
        String urlPath = args[1];
        String source = args[2];

        ResourceManager rootResourceManager = ResourceManager.createStandalone(RESOURCE_ROOT);

        Session s = Session.createSession(urlPath);

        //Opts opts = new Opts(args);
        TclInterpreter interpreter = new TclInterpreter(s);
        interpreter.eval(source);
    }
}


    /********************
     * This all worked:
     *
    public Object eval(String source)
    throws Exception {
        Session session = getSession();
        if ( session == null ) {
          //  throw new DynamideException("Session is null in TclInterpreter.eval");
        }
        try {
            Interp interp = getInterpreter();
            interp.eval("package require java");


            ScriptEvent event = new ScriptEvent(session);
            event.currentPageID = "myPage";
            TclObject r = ReflectObject.newInstance(interp, event.getClass(), event);
            interp.setVar("event", r, 0);
            TclObject s = ReflectObject.newInstance(interp, session.getClass(), session);
            interp.setVar("session", s, 0);
            try {
            //works: interp.eval("set event [java::new com.dynamide.event.ScriptEvent]");
     *      // works: interp.eval("puts \"[java::field $event RC_OK]\"");
     *
     * // SEE: C:\install\jacl\jacl1.2.6\docs\TclJava\contents.html
     *
     * //Warning: there may be a separate classloader if you use java::new and so on.
     *
            //interp.eval("set event");
            //interp.setValue("event", tobj, 0);
            //tcl.lang.TclObject eventAsObj = interp.getResult();
            //System.out.println(eventAsObj.toString());
            interp.eval("puts [$event dump]");
            tcl.lang.TclObject tclobj = interp.getResult();
            System.out.println("tcl obj to string:"+tclobj.toString());
            interp.eval(source);
            tclobj = interp.getResult();
            System.out.println("evaling source. tcl obj to string:"+tclobj.toString());
            } finally {
                interp.unsetVar("session", 0);
            }
            return tclobj.toString();
        } catch (Exception tcle){
            System.out.println("Tcl exception: "+tcle);
            tcle.printStackTrace();
            return "ERROR";
        }
    }

     */
