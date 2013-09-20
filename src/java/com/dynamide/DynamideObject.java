/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import com.dynamide.util.Log;
import com.dynamide.util.Profiler;
import com.dynamide.util.StringList;

/** The base class for all Objects that would normally descend from
  *  java.lang.Object.
  * @author Laramie Crocker
  */
public abstract class DynamideObject implements com.dynamide.util.IGet {

    private DynamideObject () throws Exception {
        throw new Exception("FORBIDDEN");
    }

    public DynamideObject(DynamideObject owner){
        m_owner = owner;
        if (Constants.DEBUG_OBJECT_LIFECYCLE) {
            String aname = getClass().getName()+"/"+getObjectID();
            if (owner!=null){
                aname = aname+":"+owner.getDotName();
            }
            Profiler.getSharedProfiler().add(this, aname);
        }
    }

    public void finalize() throws Throwable {
        if (Constants.DEBUG_OBJECT_LIFECYCLE) {
            String n = "";
            try {n = getDotName();} catch (Exception e){}
            //System.out.println("DynamideObject.finalize: "+getClass().getName() + n);
            Profiler.getSharedProfiler().remove(this);
        }
        m_owner = null;
        m_objectID = null;
        m_id = null;
        m_dotName = null;
        super.finalize();
    }

    private String m_objectID = (new Object()).toString();
    public String getObjectID(){return m_objectID;}
    public void setObjectID(String new_value){m_objectID = new_value;}

    //Page and Widget use getID now, but perhaps others could as well... %%
    protected String  m_id = getObjectID();
    public String  getID(){return m_id;}
    public void setID(String  new_value){
        m_id = new_value; //this is risky to make public.  Have it done from a generator function instead.
    }

    public String getCategoryID(){
        String fqn = null;
        Session  parent = findParentSession();
        if ( parent != null ) {
            fqn = "com.dynamide.Session."+parent.getSessionID()+'.'+getClass().getName();
        }
        if (fqn == null) {
            String className = getClass().getName();
            String tostring = toString();
            if ( tostring.startsWith(className) ) {
                fqn = tostring;
            } else {
                fqn = className + '.' + tostring;
            }
        }
        return fqn;
    }

    private DynamideObject m_owner = null;
    public DynamideObject getOwner(){return m_owner;}
    public void setOwner(DynamideObject new_value){m_owner = new_value;}

    private String  m_dotName = "";
    public String  getDotName(){return m_dotName;}
    public void setDotName(String  new_value){m_dotName = new_value;}

    /** Add linebreaks for viewing in a console; subclasses can override this with something more descriptive, for logging and debugging.*/
    public String dump(boolean linebreaks){
        return toString();
    }

    /** Subclasses can override this with something more descriptive, for logging and debugging.*/
    public String dump(){
        return toString();
    }

    /** Subclasses can override this with something more descriptive, for logging and debugging.*/
    public String dumpHTML(){
        return toString();
    }

    /** Subclasses can override this.*/
    public Object get(String what)
    throws Exception {
        if (m_nextGetter != null && m_nextGetter != this){
            return m_nextGetter.get(what);
        }
        return null;
    }

    private com.dynamide.util.IGet m_nextGetter = null;
    public void setNextGetter(com.dynamide.util.IGet nextGetterInChain){
        m_nextGetter = nextGetterInChain;
    }
    public com.dynamide.util.IGet getNextGetter(){
        return m_nextGetter;
    }

    public boolean isDebugEnabled() {
        return Log.isDebugEnabled(this);
    }

    public boolean isInfoEnabled() {
        return Log.isInfoEnabled(this);
    }

    public void logDebug(String message) {
        Log.debug(this, message, null);
    }

    public void logDebug(String categoryID, String message) {
        Log.debug(categoryID, message);
    }

    public void logDebug(String categoryID, String message, Throwable throwable) {
        Log.debug(categoryID, message, throwable);
    }

    public void logDebug(String message, Throwable throwable) {
        Log.debug(this, message, throwable);
    }

    public void logInfo(String message) {
        Log.info(this, message, null);
    }

    public void logInfo(String categoryID, String message) {
        Log.info(categoryID, message);
    }

    public void logInfo(String categoryID, String message, Throwable throwable) {
        Log.info(categoryID, message, throwable);
    }

    public void logInfo(String message, Throwable throwable) {
        Log.info(this, message, throwable);
    }

    public void logWarn(String message) {
        Log.warn(this, message, null);
    }

    public void logWarn(String categoryID, String message) {
        Log.warn(categoryID, message);
    }

    public void logWarn(String categoryID, String message, Throwable throwable) {
        Log.warn(categoryID, message, throwable);
    }

    public void logWarn(String message, Throwable throwable) {
        Log.warn(this, message, throwable);
    }

    public void logError(String message) {
        Log.error(this, message, null);
        
    }

    public void logError(String categoryID, String message) {
        Log.error(categoryID, message);
    }

    public void logError(String categoryID, String message, Throwable throwable) {
        Log.error(categoryID, message, throwable);
    }

    public void logError(String message, Throwable throwable) {
        Log.error(this, message, throwable);
    }

    public void pushLogContext(String context) {
        Log.push(context);
    }

    public String popLogContext() {
        return Log.pop();
    }

    private Profiler g_profiler;

    public Profiler getProfiler(){
        return g_profiler;
    }

    public void useProfiler(){
        if ( ! com.dynamide.Constants.PROFILE){
            g_profiler = Profiler.createProfiler();
        }
    }
    public void printProfiler(){
        if ( g_profiler != null ) g_profiler.dump();
    }
    public void clearProfiler(){
        if ( g_profiler != null ) g_profiler.clear();
    }
    public void profileEnter(String msg){
        if ( g_profiler != null ) {
            g_profiler.enter(msg);
        }
    }

    public void profileLeave(String msg){
        if ( g_profiler != null ) {
            g_profiler.leave(msg);
        }
    }

    public Page findParentPage(){
        DynamideObject o, cur;
        cur = this;
        while ( (o = cur.getOwner()) != null ) {
            if (o instanceof Page){
                return (Page)o;
            }
            cur = o;
        }
        return null;
    }

    public Page findTopParentPage(){
        DynamideObject o, cur;
        cur = this;
        Page top = null;
        if ( this instanceof Page ) {
            top = (Page)this;
        }
        while ( (o = cur.getOwner()) != null ) {
            if (o instanceof Page){
                top = (Page)o;
            }
            cur = o;
        }
        return top;
    }

    public Session findParentSession(){
        DynamideObject o, cur;
        cur = this;
        while ( (o = cur.getOwner()) != null ) {
            if (o instanceof Session){
                return (Session)o;
            }
            cur = o;
        }
        return null;
    }

    public DynamideObject findParentExpander(){
        DynamideObject o, cur;
        cur = this;
        if (cur instanceof Session){
            return (Session)cur;
        }
        while ( (o = cur.getOwner()) != null ) {
            if (o instanceof Page){
                return (Page)o;
            }
            if (o instanceof Session){
                return (Session)o;
            }
            cur = o;
        }
        logDebug("No parentExpander found for DynamideObject '"+getDotName()+'\'');
        return null;
    }

    public String walkParents(){
        DynamideObject o, cur;
        cur = this;
        String res = "";
        while ( (o = cur.getOwner()) != null ) {
            res += " :: "+o.getClass().getName();
            cur = o;
        }
        return res;
    }

    //Time trials showed that for /dynamide/demo page1, this can add up to 300 ms to show the page.
    //If that becomes an issue, raw/cooked state, or "needsExpansion" could be stored in the property/xml.
    //For now, it's not significant, and it means you can use dotted names and expressions in properties.

    /** Evaluate any string in the Webmacro interpreter.
     *  "session" is already in the context
     *  which gives you the keys to the kingdom.
     *  See also Session.eval(String) which is used to
     *  expand expressions in the beanshell interpreter.
     */
    public String expand(String raw)
    throws DynamideException{
        if (raw.length()==0){
            return raw;
        }
        DynamideObject expander = findParentExpander();
        StringList variables = new StringList(2);
        variables.addObject("source", this);
        if ( this instanceof Widget ) {
            variables.addObject("widget", this);
        }
        //try {
            if ( expander != null && expander instanceof Page ) {
                return ((Page)expander).expandTemplate(raw, variables, getDotName()); //ensures that the Page determines what goes in the context, e.g. $page and $pageID.
            } else if ( expander != null && expander instanceof Session ) {
                //No Page owner, usually WidgetType is first owner.  Just expand anyway.
                return ((Session)expander).expandTemplate(variables, raw, getDotName());
            } else {
                String ownerstring = getOwner() != null ? " owner: "+walkParents() : "null" ;
                System.out.println("DynamideObject.expand couldn't find a parent Page or Session: "+getClass().getName()+" : "+raw + " ownerstring: "+ownerstring);
                return raw;
            }
        //} catch (TemplateSyntaxException e){
        //    return e.getErrorHTML();
        //}
    }

    public static int printf(String format, Object[] args){
        return lava.clib.Stdio.printf(format+'\n', args);
    }

    public static String sprintf(String format, Object[] args){
        StringBuffer buffer = new StringBuffer();
        lava.clib.Stdio.sprintf(buffer, format, args);
        return buffer.toString();
    }



}
