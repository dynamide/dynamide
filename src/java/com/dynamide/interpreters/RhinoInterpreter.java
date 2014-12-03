package com.dynamide.interpreters;

import com.dynamide.DynamideObject;
import com.dynamide.Session;
import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Enumeration;

public class RhinoInterpreter extends DynamideObject implements IInterpreter {

    //null constructor required by call to Class.forName by factory
    public RhinoInterpreter(){
        super(null);
    }

    public RhinoInterpreter(DynamideObject owner){
        super(owner);
        if (owner instanceof Session){
            setSession((Session)owner);
        }
    }

    private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    private ScriptEngine m_Interpreter;

    private StringList m_variables = new StringList();

    private Session m_session = null;
    public Session getSession(){
        return m_session;
    }
    public void setSession(Session session){
        m_session = session;
    }

    public void close() throws Throwable {

    }

    public Object eval(String source) throws Exception {
        return getInterpreter().eval(source);
    }

    public String getVersion() {
        return "Rhino";
    }

    public void setVariable(String name, Object value) {
        m_variables.addObject(name, value);
    }

    public void unsetVariable(String name) {
        Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove(name);
        m_variables.remove(name);
    }

    public String getOutputBuffer() {
        return "";
    }

    public String emptyOutputBuffer() {
        return "";
    }


    public ScriptEvent fireEvent(ScriptEvent event, String procName, ScriptEventSource eventSource, boolean sourceOnly) {
        profileEnter("RhinoInterpreter.fireEvent");
        try{
            getInterpreter();
            synchronized (m_Interpreter){
                if (event == null){
                    logError("event object is null in RhinoInterpreter.fireEvent");
                    return event;
                }
                Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);

                ScriptEvent oldEvent = (ScriptEvent)bindings.get("event");
                setVariable("event", event);//m_Interpreter.setVariable("event", event);

                Session oldSession = null;
                try {
                    oldSession = (Session)bindings.get("session");
                } catch (Throwable eGet) {
                    System.out.println("RhinoInterpreter error getting 'session' from m_Interpreter variables: "+eGet);
                }

                setVariable("session", event.session);//m_Interpreter.setVariable("session", event.session);

                //Now add all the variables in m_variables (added by setVariable) into the bindings for the eval call.
                String key;
                Object obj;
                Enumeration en = m_variables.keys();
                while ( en.hasMoreElements() ) {
                    key = (String)en.nextElement();
                    obj = m_variables.getObject(key);
                    bindings.put(key, obj);
                }

                ScriptContext ctx = m_Interpreter.getContext();
                ctx.setAttribute(ScriptEngine.FILENAME, eventSource.resourceName+"::"+eventSource.name, ScriptContext.ENGINE_SCOPE);

                profileEnter("RhinoInterpreter.m_Interpreter.eval");
                try {
                    if ( sourceOnly ) {
                        m_Interpreter.eval(eventSource.source);  //pull in functions, vars.
                    } else {
                        m_Interpreter.eval(eventSource.source);
                        m_Interpreter.eval(procName+"(event);");  //call that function.
                    }
                } finally {
                    profileLeave("RhinoInterpreter.m_Interpreter.eval");
                    unsetVariable("event");
                    unsetVariable("session");
                    if ( oldSession!=null ) {
                        bindings.put("session", oldSession);
                    }
                    if ( oldEvent!=null ) {
                        bindings.put("event", oldEvent);
                    }
                    en = m_variables.keys();
                    while ( en.hasMoreElements() ) {
                        key = (String)en.nextElement();
                        bindings.remove(key);
                    }
                }
            } //end-synchronized
        //TODO: handle catchable interp errors the same way BshInterpreter does.
        // } catch (bsh.EvalError e){
        //Notably, calls like this should be customized for Rhino:
        //        String msgHTML = formatError(e, true, eventSource.source, event);
        //        com.dynamide.resource.ResourceManager.writeErrorLog(
        //        event.resultCode = ScriptEvent.RC_ERROR;
        //        event.evalErrorMsg = msgHTML;
        } catch (Exception e2){
            logError("[42.3] Error eval'ing with bsh", e2);
            event.resultCode = ScriptEvent.RC_ERROR;
            event.evalErrorMsg = StringTools.escape(e2.toString())
                    +"<hr/>STACK TRACE<hr/><pre><small>"
                    +StringTools.escape(Tools.getStackTrace(e2))
                    +"</small></pre><hr/>END STACK TRACE<hr/>";

            String id = (event.session != null)
                    ? event.session.getSessionID()
                    : (m_session!=null ? m_session.getSessionID() : "");
            com.dynamide.resource.ResourceManager.writeErrorLog(getSession(), id, event.evalErrorMsg, event.evalErrorMsg, Tools.getStackTrace(e2),e2.getClass().getName());
            return event;
        }
        profileLeave("RhinoInterpreter.fireEvent");
        return event;
    }

    //=================== Implementation ===============================================================================

    private ScriptEngine getInterpreter() throws Exception {
        if (m_Interpreter == null){
            m_Interpreter = scriptEngineManager.getEngineByName("javascript");
            Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("stdout", System.out);
        }
        return m_Interpreter;
    }

    public static void main(String[]args) throws Exception {
        RhinoInterpreter interp = new RhinoInterpreter(null);
        interp.eval("stdout.println('hello');");
    }
}
