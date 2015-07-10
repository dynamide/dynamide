/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import com.dynamide.interpreters.InterpreterTools;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileItemFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.webmacro.Context;
import org.webmacro.FastWriter;
import org.webmacro.InitException;
import org.webmacro.WebMacro;

import com.dynamide.datatypes.Datatype;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.datatypes.EnumeratedDatatype;
import com.dynamide.datatypes.LinkOptions;
import com.dynamide.datatypes.PublishParamsDatatype;
import com.dynamide.db.IDatasource;
import com.dynamide.event.ErrorHandlerInfo;
import com.dynamide.event.ErrorHandlerResult;
import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.event.WorkflowResult;
import com.dynamide.interpreters.IInterpreter;
import com.dynamide.resource.Assembly;
import com.dynamide.resource.ContextNode;
import com.dynamide.resource.IContext;
import com.dynamide.resource.Job;
import com.dynamide.resource.ResourceManager;
import com.dynamide.resource.WebAppEntry;
import com.dynamide.util.FileTools;
import com.dynamide.util.IComposite;
import com.dynamide.util.ISessionItem;
import com.dynamide.util.ISessionTableItem;
import com.dynamide.util.Log;
import com.dynamide.util.Profiler;
import com.dynamide.util.RegisteredActions;
import com.dynamide.util.ServletTools;
import com.dynamide.util.ShellHelper;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;
import com.dynamide.util.URLDecoder;
import com.dynamide.util.ValueBeanHelper;
import com.dynamide.util.trace.ExpansionLog;

//import com.dynamide.util.*;



/** This class manages the user interaction across Page objects, and is the Java-side representation
 *  of the application.xml file, which has the page list, page order, and the application-level events.
 *  <p>
 * <b>NOTE: The Session is NOT thread-safe.</b>  The DynamideServlet serializes access to the Session for each web request.
 * So each Session is single-threaded while handling a web request, just like the Servlet API spec.
 *  For Jobs, the Job runs in a separate thread, but the app programmer must not make any calls that change the
 *  state of the session.  Legal things to do in a job are tasks such as sending mail, updating external resources,
 *  such as files and databases that you have some other synchronization methods for, etc.
 *  </p>
 * @todo Make an ISession interface.
 */
public class Session extends Persistent
implements ISession, ISessionItem, ISessionTableItem, IDatasource, IContext {

    public void printStdOut(String message) {
        System.out.println("!!!!!!!!!!!!!! "+message);
    }

    //================= Constructor ==============================================================
    /** Call the factory method: createSession rather than using this constructor outside of this class.*/
    private Session(DynamideObject owner,
                    String newSessionID,
                    String applicationXMLFilename,
                    String relativeProjectPath,
                    String appname,
                    Assembly assembly,
                    DynamideHandler handler,
                    int newModeFlags)
    throws IOException, JDOMException, PageLoadException, DatatypeException, Exception {
        super(owner, Tools.fixFilename(applicationXMLFilename), null); //enforce that the application
//15500 but stable if return here.
        //logDebug("Session.create "+newSessionID);
        profileEnter("Session.constructor");
        try {
            setModeFlags(newModeFlags);
            //logDebug("Session classloader >>>>>>>>>>>>>>>>>> "+getClass().getClassLoader().getClass().getName());
            setHandler(handler);
            m_fields = Tools.createSortedCaseInsensitiveMap();
            m_pageNames = new Vector();  //the Page names.
            m_loadedPages = Tools.createSortedCaseInsensitiveMap(); //the Page objects.
            m_visitedPages = new StringList(8); //just the string names.
            LOG_HANDLER_PROC_TIMING = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_TIMING_CATEGORY, org.apache.log4j.Priority.DEBUG);
            LOG_HANDLER_PROC = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_CATEGORY, org.apache.log4j.Priority.DEBUG);
            if (!LOG_HANDLER_PROC){
                LOG_HANDLER_PROC = getVerboseMode();
            }
            LOG_EXPANSIONS = Log.getInstance().isEnabledFor(Constants.LOG_EXPANSIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
            if (!LOG_EXPANSIONS){
                LOG_EXPANSIONS = getVerboseMode();
            }
            // %% this isn't working: !! LOG_HANDLER_PROC_MISSING_EVENTS = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY, org.apache.log4j.Priority.DEBUG);
            //System.out.println("!!!!!!!!!!"+Constants.LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY+" "
             //                   +" LOG_HANDLER_PROC_MISSING_EVENTS: "+LOG_HANDLER_PROC_MISSING_EVENTS
             //                   +" log: "+Log.getInstance());
             //
            LOG_HANDLER_PROC_REQUEST_HEADERS = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_REQUEST_HEADERS, org.apache.log4j.Priority.DEBUG);

//1000 runs 15340 if return here

            setSession(this); //silly, but why have a null pointer.  It's in Persistent.
            setSessionID(newSessionID);  // initializes special field: SESSIONID
            setID(newSessionID);
            //m_context = new ContextNode(newSessionID);

            //Any time after this it is safe to call logging.

            //setFieldValue(Constants.USER, ""); //initialize this special field: USER.

            m_applicationPath = relativeProjectPath;  //sets get("applicationPath") and getApplicationPath(),
                                                     // alias is also getURLPath()
            m_appname = appname;

            //m_searchLocations = sl;
            m_assembly = assembly;

            //logInfo("Session created: "+relativeProjectPath);
            //file is always the application.xml
            //and all applications are in separate directories.
            logHandlerProcRequest();

            logHandlerProc("INFO", "Creating session <b>"+relativeProjectPath+"&#160;&#160;</b> SESSIONID="+newSessionID);
            if ( debug ) logDebug("CREATING Session "+relativeProjectPath);

//15496 if return here, 1000 runs

//15916 without this one
            loadFields();

//LEAK-TEST %%  with new jdom 10, this stops at 16440, but is stable.
//16408 here, 1000 runs

//16536 with this line:
//16408 without it
            readPagesElement();  //load all the page names, but not the pages.

            // %% TODO: need to get base-default properties, a la AbstractWidget.setType().
//

//16412 with all lines up to and including this one:
            Element propertiesElement = getRootElement().getChild("properties");
            Persistent.mergeProperties(this, propertiesElement, getSessionID());
            //All properties are now loaded from application.xml.
            loadInternationalization(getPropertyStringValue("defaultLanguage"));

            if ( getString("applicationID").length()==0) {
                logWarn("application has no 'applicationID' property in application.xml. file: "+getFilename());
            }

        } finally {
            profileLeave("Session.constructor");
        }
    }

    //================= Factory Methods ==========================================================


    /** Create a separate session than this one, and start it with startSession()  Used by the IDE.
     * @todo Make the IDE be able to pass in the debugQueryString.
     * @dynamide.keymethod Key method used by the IDE
     */
    public String createNewSession(String uri, boolean designMode, String debugQueryString){
        try {
            int newModeFlags = MODE_NONE;
            if ( designMode ) {
                newModeFlags = MODE_DESIGN;
            }
            Session newsession = createSession(uri, uri, Constants.SESSION_PREFIX, "", true/*start*/, newModeFlags, null, debugQueryString, null);
            String err = newsession.m_sessionStartupStatus;
            if ( err.length()>0 ) {
                logError("createNewSession error starting session: "+err);
                return err;
            }
            return newsession.getSessionID();
        } catch (Exception e){
            logError(""+e.toString());
            return "ERROR: [93] [createNewSession] "+uri+","+designMode+","+e;
        }
    }

    /** Public factory method using defaults for fullURI, prefix, and not allowing a forcedSessionID,
      *  creates and starts session, with designMode==false.
      *  @dynamide.factorymethod Create a Session from a URI string.
      *   @throws IOException when the application.xml file cannot be read
      *   @throws JDOMException when the application.xml file is not valid XML
      *   @throws Exception
      */
    public static Session createSession(String uri)
    throws IOException, JDOMException, Exception {
        return createSession(uri, uri, Constants.SESSION_PREFIX, "", true, MODE_NONE, null, "", null);
    }

    /**
     *  @dynamide.factorymethod Create a Session from a registered URI string, with a debugQueryString
      *   @throws IOException when the application.xml file cannot be read
      *   @throws JDOMException when the application.xml file is not valid XML
      *   @throws Exception
     */
    public static Session createSession(String uri, String debugQueryString)
    throws IOException, JDOMException, Exception {
        return createSession(uri, uri, Constants.SESSION_PREFIX, "", true, MODE_NONE, null, debugQueryString, null);
    }

    /** WARNING: if you set the modeFlags to include MODE_DEBUG, the session won't start.
     *  This is a useful feature for debuggers, but the Session won't be truly initialized.
     *  Only call this method if you know what you are doing.
     *  This public factory method creates a session with modes as specified by modeFlags,
     *  which are bitwise combinations of Session.MODE_* constants, e.g.
     * <pre>modeFlags = Session.MODE_DESIGN|Session.MODE_DEBUG.</pre>
     *  @dynamide.factorymethod Create session with debugQueryString and modeFlags
      *   @throws IOException when the application.xml file cannot be read
      *   @throws JDOMException when the application.xml file is not valid XML
      *   @throws Exception
     */
    public static Session createSession(String uri, String debugQueryString, int modeFlags)
    throws IOException, JDOMException, Exception {
        boolean start = (modeFlags & MODE_DEBUG) == MODE_DEBUG;
        if (!start){
            Log.info(Session.class, "Session will be created by NOT STARTED. URI:"+uri);
        }
        Session session = createSession(uri, uri, Constants.SESSION_PREFIX, "", start, modeFlags, null, debugQueryString, null);
        session.startSession();
        return session;
    }

    /* Future feature, to be put into createSession, below.
     * static sessions can be recycled -- they don't keep user state.
     * This is up to the app programmer, to declare them as static, so that the handler puts
     * them back in the pool at the end of the request.
     * Session session = sessionPool.findAvailable(urlPath);
     *   if ( session == null ) {
     *       //May throw a PageLoadException:
     *       Session session = new Session(null, sessionID, applicationXmlFilename, urlPath, searchLocations);
     *   } else {
     *       lock pooled object.
     *   }
     *
     * We might be much better off to allow caching of JDOM objects, since these can be immutable, e.g.
     * create the application, clone it, and cache the top N ones in a memory pool.  Unused ones time out.
     */


    /** Public factory method, creates session, but only starts the Session if param "start" is true.
     *  @dynamide.factorymethod Creates a session with all startup parameters exposed.
      *   @throws IOException when the application.xml file cannot be read
      *   @throws JDOMException when the application.xml file is not valid XML
      *   @throws Exception
     */
    public static Session createSession(String uri,
                                        String fullURI,
                                        String prefix,
                                        String forcedSessionID,
                                        boolean start,
                                        int modeFlags,
                                        DynamideHandler handler,
                                        String debugQueryString,
                                        HandlerResult handlerResult)
    throws IOException, JDOMException, Exception {
        Profiler profiler = null;
        if (com.dynamide.Constants.PROFILE) profiler = Profiler.getSharedProfiler();
        if (com.dynamide.Constants.PROFILE)profiler.enter("Session.createSession");
        ResourceManager rootResourceManager = ResourceManager.getRootResourceManager();
        if ( rootResourceManager == null ) {
            Log.error(Session.class, "ResourceManager is null.  Did you call com.dynamide.resource.ResourceManager.createStandalone()?");
            return null;
        }

        String userID = "anonymous"; // %%%%%%%%%%% DOOOOOOOOOOOOOOOOOOP!!!!!!!!!!!!

        URLDecoder debugURLDecoder = null;

        String sessionID = (forcedSessionID.length()>0) ? forcedSessionID : "";
        if ( sessionID.length()==0 ) {
            if ( debugQueryString.length() > 0 ) {
                //Log.debug(Session.class, "Session calling URLDecoder to extract url:"+debugQueryString);
                debugURLDecoder = new URLDecoder(debugQueryString);
                String sessionIDParam = debugURLDecoder.getFieldValue("SESSIONID");
                if (sessionIDParam.length()>0){
                    sessionID = sessionIDParam;
                }
                uri = debugURLDecoder.getPath();
                if ( uri.length()>0 ) {
                    fullURI = uri;
                    Log.debug(Session.class, "Session using debugQueryString: "+debugQueryString+" which had URI: "+fullURI);
                } else {
                    Log.debug(Session.class, "Session using debugQueryString: "+debugQueryString+" which had no path. Using: "+fullURI);
                }
            }
            if ( sessionID.length()==0 ) {
                sessionID = rootResourceManager.getNextSessionID();
            }
        }

        while ( uri.endsWith("/") ) {
            uri = uri.substring(0, uri.length()-1);
        }

        WebAppEntry entry = rootResourceManager.uriToApp(fullURI);
        if ( entry == null ) {
            throw new ApplicationNotFoundException(fullURI, "");
        }
System.out.println("========== uriToApp("+fullURI+") ==> "+entry);
        /* %TODO%
         *  Figure out the account, and get the session pool for that account,
         *  rather than sharing the session database globally.
         * String account = entry.getHome(); may need to extract account from "/homes/dynamide"...
         *
         * Pool sessionPool = (Pool)m_rootResourceManager.find("/homes/"+account+"/sessions", "/");
         * if ( sessionPool == null ) {
         *    throw new DynamideException("Session Pool not initialized");   //%%%%%%%%%%%%% initialize for each legit account.
         * }
         */

        String applicationPath = entry.getURI();
        String appname = entry.getAppname();
        String userSessionContextName = "/homes/dynamide/appdata/"+appname+"/userSessions";

        HttpServletRequest request = null;
        if (handler!= null){
            request = handler.getRequest();
        }
        String USER = "";
        if (debugURLDecoder != null){
            USER = debugURLDecoder.getFieldValue(Constants.USER);
        } else {
            if (request != null) USER = ServletTools.getParameterValue(request, Constants.USER);
        }
        if ( USER == null || USER.length()==0 && request != null ) {
            String USERCookie = ServletTools.getCookieValue(request, "DynamideUser");
            if ( !Tools.isBlank(USERCookie) ) {
                USER = USERCookie;
            }
        }

        System.out.println("Session ~1~~~~~~~~~~~~ "+USER+" "+Thread.currentThread().getName());
        if ( !Tools.isBlank(USER) ){
            IContext sessions = (IContext)rootResourceManager.find(userSessionContextName);
            if ( sessions != null ) {
                IContext session = (IContext)sessions.getContext(USER);
                if ( session != null && session instanceof Session) {
                    Session userSession = (Session)session;

                    // MEMORY_LEAKS 1/21/2005 added lock detection.
                    int busycount = 0;
                    while (userSession.busy){
                        //it's a user-singleton session, but it is busy.  Let's try to wait for a short time.
                            Log.warn(Session.class, "session busy. try: "+busycount+" sessionid: "+userSession.getSessionID());
                            Tools.sleep(500);
                            if ( ! userSession.busy ) {
                                userSession.busy = true;
                                System.out.println("Locking session: "+userSession.getSessionID());
                                break;
                            }
                            if ( busycount > 10 ) {
                                throw new Exception("session was locked by other request. sessionid: "+userSession.getID());
                            }
                            busycount++;
                    }

                    // MEMORY_LEAKS 1/21/2005 turned from debug to info
                    Log.info(Session.class, "RETURNING Session "+userSession.toString()+", found by user: "+USER);
                    
                    
                    /*  %% This is a big problem.  If anyone else guesses the username, they are in.
                     *  Better would be to allow them in, but revoke their authentication, sending them
                     *  back to the login page.  Maybe a session.USER and session.USER_AUTHENTICATED.
                     *  Or, if their cookie and timeout are acceptable, then that would authenticate them.
                     */
                     
                     
                    userSession.busy = true;
                    System.out.println("Session locked: "+userSession.getSessionID());
                    return userSession;
                }
            }
        }

        if ( modeFlags == MODE_NONE && Tools.isBlank(debugQueryString) && (start)) {
            //nothing fancy, must be a re-useable session.
            Session pooledSession = rootResourceManager.findPooledSession(entry);
            if ( pooledSession != null ) {
                //pooledSession.logDebug("Using pooled session");
                return pooledSession;  //it is automatically removed from pool.
            }
            if (rootResourceManager.isPoolMaxed(applicationPath)){
                //it's a pooled session, but it is busy.  Let's try to wait for a short time.
                for (int i=0; i < 10; i++) {
                    Log.debug(Session.class, "poolMaxed "+i);
                    Tools.sleep(500);
                    pooledSession = rootResourceManager.findPooledSession(entry);
                    if ( pooledSession != null ) {
                        //pooledSession.logDebug("Using pooled session");
                        return pooledSession;  //it is automatically removed from pool.
                    }
                }
                throw new Exception("pooled uri is unavailable");
            }
        }

        if (entry .getVerboseMode()){
            modeFlags |= MODE_VERBOSE;
        }

        Assembly assembly = rootResourceManager.findAssembly(entry.getHome(),
                                                             entry.getAssembly(),
                                                             entry.getInterface(),
                                                             entry.getBuild());
        if ( assembly == null  ) {
            throw new DynamideException("Couldn't find assembly: "
                                        +entry.getAssembly()
                                        +" in "+entry.getHome()
                                        +" web-apps entry : "+entry);
        }

        IContext applicationFileNode = assembly.getApplicationResource(appname, "application.xml");
        if (applicationFileNode == null){
            throw new FileNotFoundException(applicationPath+" :: application.xml");
        }
        String applicationXmlFilename = (String)applicationFileNode.getAttribute(Assembly.FULLPATH);
        if (applicationXmlFilename == null || applicationXmlFilename.startsWith("ERROR:")){
            throw new FileNotFoundException(applicationXmlFilename);
        }

        //May throw a PageLoadException:
        Session session = new Session(null,
                                      sessionID,
                                      applicationXmlFilename,
                                      applicationPath/*10/24/2002 1:39AM uri*/,
                                      appname,
                                      assembly,
                                      handler,  //session may temporarily use handler during construction, but unsets when constructed.
                                      modeFlags);
        session.busy = true;  //1/21/2005 MEMORY_LEAKS . added
        session.setSecure(entry.getSecure());
        session.setHandler(handler);
        session.setDebugURL(debugQueryString);//OK if empty
        if ( !Tools.isBlank(USER) ) {
            session.setUSER(USER);
        }
        //this can change, but set it now to be a good default.  It will be handled in Session.get(String).
        //5/27/2003 6:31AM  This is a bad idea.   Get users in the habit of calling get("requestPath") instead.
        //session.setFieldValue("requestPath", fullURI);

        //This stays the same for the life of the Session:
        //session.setFieldValue("applicationPath", applicationPath/*uri*/);

        //session.logDebug("applicationPath: "+applicationPath/*uri*/);

        //session.logDebug("Creating new session, adding to rootResourceManager");
        rootResourceManager.addSession(sessionID, session); //throws exception if in use
        if ( !Tools.isBlank(USER) && !session.isPoolable() ) {
            String tn = Thread.currentThread().getName();
            System.out.println("\r\ntrying to addUserSession-------------"+sessionID+"--------"+tn);//+Tools.getStackTrace());
            session.busy = true;
            rootResourceManager.addUserSession(userSessionContextName, USER, session);
            System.out.println("added addUserSession--------------"+sessionID+"-------"+tn);
        }

        if (start){
            String err = "";
            try {
                session.setHandlerResult(handlerResult);
                err = session.startSession();
            } finally {
                session.setHandlerResult(null);
            }

            if ( err.length()>0 ) {
                Log.error(Session.class, "Couldn't start session.  See result page");
                // %% todo: log this: session.hookException(err, (Throwable)null, null, ErrorHandlerInfo.EC_SESSION, true);
                throw new DynamideException("Couldn't start session: "+err);
            }
        }
        if (com.dynamide.Constants.PROFILE)profiler.leave("Session.createSession");
        return session;
    }


    public static Session createTestSession(String sessionID,
                                            String applicationPath,
                                            String appname,
                                            boolean callConstructor)
    throws Exception {
        ResourceManager rootResourceManager = ResourceManager.getRootResourceManager();
        WebAppEntry entry = rootResourceManager.uriToApp(applicationPath);
        Assembly assembly = rootResourceManager.findAssembly(entry.getHome(),
                                                             entry.getAssembly(),
                                                             entry.getInterface(),
                                                             entry.getBuild());
        if ( assembly == null  ) {
            throw new DynamideException("Couldn't find assembly: "
                                        +entry.getAssembly()
                                        +" in "+entry.getHome()
                                        +" web-apps entry : "+entry);
        }
        IContext applicationFileNode = assembly.getApplicationResource(appname, "application.xml");
        if (applicationFileNode == null){
            throw new FileNotFoundException(applicationPath+" :: application.xml");
        }
        String applicationXmlFilename = (String)applicationFileNode.getAttribute(Assembly.FULLPATH);
        if (applicationXmlFilename == null || applicationXmlFilename.startsWith("ERROR:")){
            throw new FileNotFoundException(applicationXmlFilename);
        }

        //May throw a PageLoadException:
        Session session = null;
        if (callConstructor) new Session(null,
                                      sessionID,
                                      applicationXmlFilename,
                                      applicationPath,
                                      appname,
                                      assembly,
                                      null,
                                      MODE_NONE);
        return session;
    }

    //===============================================================================

    public String startSession()
    throws DynamideUncaughtException {
        profileEnter("Session.startSession");
        if ( m_sessionStarted ) {
            logDebug("startSession called, but already started.");
            return "";
        }
        //logDebug("startSession called");
        m_sessionStarted = true;
        profileEnter("Session.application_onImport");
        ScriptEventSource application_onImport = getEventSourceBody("application_onImport");
        if ( application_onImport.source.length()>0 ) {
            ScriptEvent event = fireEvent(this, "application_onImport", "", "", "", application_onImport, getFilename(),  true);
            if ( event.resultCode != ScriptEvent.RC_OK ) {
                logError("ERROR: [39a] see event.evalErrorMsg");
                //hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                m_sessionStartupStatus = event.evalErrorMsg;
                return m_sessionStartupStatus;
            }
        } else {
            //logDebug("No application_onImport defined.");
        }
        profileLeave("Session.application_onImport");


        profileEnter("Session.application_onStart");
        ScriptEventSource onStart = getEventSource("application_onStart");// let event create more fields or initialize.
        if ( onStart.source.length()>0 ) {
            ScriptEvent event = fireEvent(this, "application_onStart", "",  "", "", onStart, getFilename(), false);
            if ( event.resultCode != ScriptEvent.RC_OK ) {
                logError("ERROR: [39] see event.evalErrorMsg");
                //hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                m_sessionStartupStatus = event.evalErrorMsg;
                return m_sessionStartupStatus;
            }
        } else {
            //logDebug("No application_onStart defined.");
        }
        profileLeave("Session.application_onStart");


        if (getPublishMode()){
            try {
                profileEnter("Session.doPublish");
                doPublish();
                profileLeave("Session.doPublish");
            } catch ( Exception e ) {
                logError("Couldn't publish in startSession()", e);
                return e.toString();
            }
        }

        profileLeave("Session.startSession");
        return "";
    }

    public void finalize() throws Throwable {
        String n = "";
        setSession(null);//was "this"
        try {n = getDotName();} catch (Exception e){}
        m_webmacro = null;
        super.finalize();
        //System.out.println("Session.finalized: "+sessionID);
    }

    //================= Fields, Getters, Setters ======================================

    public static final String PROPERTY_NAME_PUBLISH_PARAMS = "publishParams";
    public static final String PROPERTY_NAME_PUBLISH_URLS = "publishURLs";

    private boolean m_sessionStarted = false;

    private String m_sessionStartupStatus = "";


    private boolean LOG_HANDLER_PROC = false;
    private boolean LOG_HANDLER_PROC_TIMING = false;
    private boolean LOG_EXPANSIONS = false;
    private boolean LOG_HANDLER_PROC_MISSING_EVENTS = false;
    private boolean LOG_HANDLER_PROC_REQUEST_HEADERS = false;

    private ResourceManager m_rootResourceManager = ResourceManager.getRootResourceManager();
    public ResourceManager getResourceManager(){
        return m_rootResourceManager;
    }

    public void checkDirectoryWrite(String directoryPath)
    throws SecurityException {
        getResourceManager().checkDirectoryWrite(this, directoryPath);
    }

    public String getStaticRoot()
    throws Exception {
        return getResourceManager().getStaticRoot();
    }

    public String getStaticDir()
    throws Exception {
        return FileTools.join(getResourceManager().getStaticRoot(), getHome()+"/"+getString("applicationID"));
    }

    private String sessionID = "";
    public String getSessionID(){return sessionID;}
    public void setSessionID(String new_value){
        sessionID = new_value;
        /*Field field = new Field(this, this);
        field.set("name", "SESSIONID");
        field.set("value", sessionID);
        addField("SESSIONID", field);
        out 4/15/2004 7:18PM
        */
        //logHandlerProc("INFO", "New SESSIONID="+sessionID);
    }

    private boolean m_secure = false;
    public boolean getSecure(){return m_secure;}
    private void setSecure(boolean new_value){m_secure = new_value;}


    /*package-access*/ boolean busy = false;
    public boolean isBusy(){
        return busy;
    }

    //%% replace this with some log4j thingy...
    public static final boolean LOG_EVENTS_TO_HANDLERLOG = false;

    private String  m_appname = "";
    public String  getAppname(){return m_appname;}

    public String getFullAppname(){
        String home = getHome();
        return home.length()>0 ? home+"."+m_appname : m_appname;
    }

    public String getAccount(){return m_assembly.getAccount();}

    public String getHomeDir()
    throws Exception {
        return FileTools.join(FileTools.join(ResourceManager.getResourceRoot(), "homes"), getHome());
    }

    public String getHome(){return m_assembly.getAccount();}

    public String getAppDirectory(){
        try {
            return (new File(getFilename())).getParentFile().getCanonicalPath(); //%% todo: make this kosher with ResourceManager
        } catch (Exception e){
            return "";
        }
    }

    private Assembly m_assembly = null;
    public Assembly getAssembly(){return m_assembly;}

    public String getDotName(){
        return "";   // "session";   used to do this, but then everything has it.  don't need for internationalization
                     //  If needed for global evals, just glue "session." on the front of any eval name.
    }

    private long m_startTime = Tools.now().longValue();

    private long m_lastAccessTime = Tools.now().longValue();
    public void touchLastAccessTime(){
        m_lastAccessTime = Tools.now().longValue();
    }

    private String m_userToken = "";
    public String getUserToken(){
        String user = getUSER();
        if ( user.length()==0 ) {
            return "";
        }
        if ( m_userToken.length()==0 ) {
            m_userToken = com.dynamide.security.Login.crypt(user, false);
        }
        return m_userToken;
    }

    private String m_USER = "";
    private String m_USERAUTH = "";

    public String getUSER(){
        return m_USER;
    }

    /* Only sets it if the new value isn't empty.  To clear, use clearUSER. */
    public void setUSER(String new_value){
        if (new_value != null && new_value.length() > 0){
            m_USER = new_value;
        }
    }

    public void clearUSER(){
        m_USER = "";
    }
    
    public void setUSERAUTH(String auth){
        m_USERAUTH = auth;   
    }
    
    public String getUSERAUTH(){
        return m_USERAUTH;   
    }

    private static boolean cacheXml = true;  //THIS IS HARDCODED FOR NOW. %%

    private Map m_widgetTypePool = Tools.createSortedCaseInsensitiveMap();
    private Map m_widgetPool = Tools.createSortedCaseInsensitiveMap();
    private Map m_internationalizedValues = Tools.createSortedCaseInsensitiveMap();

    private static boolean debug = false;

    private Map m_fields;
    public Set getFieldNames(){
        return m_fields.keySet();
    }

    private String m_lastErrorDump = "";
    public String getLastErrorDump(){return m_lastErrorDump;}
    public String getErrorDump(){
        StringBuffer buf = new StringBuffer();
        String ers = dumpErrorsHTML();
        if ( ers.length()>0 ) {
            buf.append("<b>in Session:</b>");
            buf.append(ers);
        }
        Iterator it = m_datasources.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry en = (Map.Entry)it.next();
            String key = (String)en.getKey();
            IDatasource ds = (IDatasource)en.getValue();
            if (ds != this) {
                ers = ds.dumpErrorsHTML();
                if ( ers.length()>0 ) {
                    buf.append("<b> in "+ds.getID()+":<br />");
                    buf.append(ers);
                }
            }
        }
        return buf.toString();
    }
    
    public static class ErrorURI{
    	public String uri;
    	public long datestamp; 
    	public ErrorURI(String uri){
    		this.datestamp = Tools.now().longValue();
    		this.uri = uri;
    	}
    }
    private ArrayList m_errorURIs = new ArrayList();
    public ErrorURI[] getErrorURIs(){
    	return (ErrorURI[])m_errorURIs.toArray(new ErrorURI[m_errorURIs.size()]);
    
    }
    public void addErrorURI(String uri) {
    	m_errorURIs.add(new ErrorURI(uri));	
	}
    public String formatErrorURIs(){
    	StringBuffer sb = new StringBuffer();
    	ErrorURI[] arr = getErrorURIs();
    	ErrorURI uriObj;
    	for (int i = 0; i < arr.length; i++) {
    		uriObj = arr[i];
    		sb.append(uriObj.datestamp).append(": ");
    		sb.append("<a href='"+uriObj.uri+"'>"+uriObj.uri+"</a><br />");	
		}
    	return sb.toString();
    }
    
    private Vector m_pageNames;
    private Map m_loadedPages;

    public boolean hasPage(String pageID){
        if ( m_pageNames.indexOf(pageID)>-1 ) {
            return true;
        }
        if ( m_loadedPages.get(pageID)!=null ) {
            return true;
        }
        return false;
    }

    private Map m_Interpreters = Tools.createSortedCaseInsensitiveMap();

    private Map m_datasources = Tools.createSortedCaseInsensitiveMap();

    /** @return a Set of IDatasource names registered with the Session, use getDatasource(String) with the value returned
     * to get the actual IDatasource.
     */
    public Set getDatasources(){
        return m_datasources.keySet();
    }

    private Map m_submitActions = Tools.createSortedCaseInsensitiveMap();
    public Set getSubmitActions(){return m_submitActions.entrySet();}
    public void registerSubmitAction(String fieldID, String action){
        m_submitActions.put(fieldID, action);
    }

    private class RegisteredActionEvent{
        ScriptEventSource scriptEventSource;
        Object inputObject;
        String action;
    }
    private int g_registeredActionEvents = 0;
    private Map m_registeredActionEvents = Tools.createSortedCaseInsensitiveMap();
    public String registerActionEvent(String action, ScriptEventSource scriptEventSource, Object inputObject){
        if ( action.length()==0 ) {
            action = "action_"+(g_registeredActionEvents++);
        }
        RegisteredActionEvent ra = new RegisteredActionEvent();
        ra.scriptEventSource = scriptEventSource;
        ra.action = action;
        ra.inputObject = inputObject;
        m_registeredActionEvents.put(action, ra);
        return action;
    }


    private RegisteredActions m_registeredActions = null;
    private void checkRegisteredActionsRef(){
        if ( m_registeredActions == null ) {
            m_registeredActions = new RegisteredActions(getSession());
        }
    }
    public RegisteredActions getRegisteredActions(){
        checkRegisteredActionsRef();
        return m_registeredActions;
    }
    public ScriptEvent fireRegisteredAction(String action){
        checkRegisteredActionsRef();
        return m_registeredActions.fireRegisteredAction(action);
    }
    public void registerAction(String action,
                               DynamideObject sender,
                               String eventName,
                               Object inputObject){
        checkRegisteredActionsRef();
        m_registeredActions.registerAction(action, sender, eventName, inputObject);
    }

    public void unregisterAction(String action){
        checkRegisteredActionsRef();
        m_registeredActions.unregisterAction(action);
    }



    private String m_currentAction = "";
    public String getCurrentAction(){return m_currentAction;}
    public void setCurrentAction(String  new_value){m_currentAction = new_value;}


    private boolean m_stdoutEcho = false;
    public boolean getStdoutEcho(){return m_stdoutEcho;}
    public void setStdoutEcho(boolean new_value){m_stdoutEcho = new_value;}

    private boolean m_pooled = false;
    public boolean getPooled(){return m_pooled;}
    public void setPooled(boolean new_value){
        m_pooled = new_value;
        if (new_value){
            StringBuffer buf = new StringBuffer();
            buf.append(dumpErrorsHTML());

            //means we are back in the pool. clear non-shareable values.
            //and also:
            //  fire("onRepool");
            Iterator it = m_loadedPages.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry en = (Map.Entry)it.next();
                String key = (String)en.getKey();
                Page page = (Page)en.getValue();
                page.clearErrors();
            }

            it = m_datasources.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry en = (Map.Entry)it.next();
                String key = (String)en.getKey();
                IDatasource ds = (IDatasource)en.getValue();
                if (ds != this) {
                    buf.append(ds.dumpErrorsHTML());
                    ds.clear();
                }
            }
            this.clear();
            this.m_lastErrorDump = buf.toString();
        }
    }

    public boolean isPoolable(){
        String poolable = getPropertyStringValue("poolable");
        if (! Tools.isBlank(poolable) && poolable.equals("true") ) {
           return true;
        }
        return false;
    }

    public String getThreadID(){
        return Thread.currentThread().getName();
    }

    public String getThreadGroupID(){
        return Thread.currentThread().getThreadGroup().getName();
    }


    //================= Modes ==============================================================

    public static final int MODE_NONE = 0;
    public static final int MODE_DEBUG = 1;
    public static final int MODE_TEST = 2;
    public static final int MODE_DESIGN = 4;
    public static final int MODE_BATCH = 8;
    public static final int MODE_PUBLISH = 16;
    public static final int MODE_VERBOSE = 32;

    private int modeFlags = 0;
    public int getModeFlags(){
        return modeFlags;
    }
    protected void setModeFlags(int modes){
        modeFlags = modes;
    }

    private void flipModeBits(boolean new_value, int MODE){
        if ( new_value ) {
            modeFlags |= MODE;
        } else {
            modeFlags = modeFlags & (~MODE);
        }
    }

    public boolean getBatchMode(){return (modeFlags & MODE_BATCH) == MODE_BATCH;}
    public void setBatchMode(boolean new_value){flipModeBits(new_value, MODE_BATCH);}

    public boolean getDebugMode(){return (modeFlags & MODE_DEBUG) == MODE_DEBUG;}
    public void setDebugMode(boolean new_value){flipModeBits(new_value, MODE_DEBUG);}

    public boolean getDesignMode(){return (modeFlags & MODE_DESIGN) == MODE_DESIGN;}
    public void setDesignMode(boolean new_value){flipModeBits(new_value, MODE_DESIGN);}

    /** There is no setPublishMode() -- use publish() instead -- once you have
     *  entered publish mode, getPublishMode will return true.  It is a true mode, reflected
     *  in the modeFlags, but there are a number of side effects that must be set.  Use publish()
     *  to safely set all the options.  The biggest issue is that if you attempt to publish
     *  a running session, resources found with Session.getInclude(...) will not be trapped.
     */
    public boolean getPublishMode(){return (modeFlags & MODE_PUBLISH) == MODE_PUBLISH;}

    public boolean getTestMode(){return (modeFlags & MODE_TEST) == MODE_TEST;}
    public void setTestMode(boolean new_value){flipModeBits(new_value, MODE_TEST);}

    public boolean getVerboseMode(){return (modeFlags & MODE_VERBOSE) == MODE_VERBOSE;}
    public void setVerboseMode(boolean new_value){flipModeBits(new_value, MODE_VERBOSE);}

    public String getModeString(){
        String r = ""+modeFlags+';';
        r = getBatchMode()  ? r+"BATCH;"  : r;
        r = getDebugMode()  ? r+"DEBUG;"  : r;
        r = getDesignMode() ? r+"DESIGN;" : r;
        r = getPublishMode()   ? r+"PUBLISH;"   : r;
        r = getTestMode()   ? r+"TEST;"   : r;
        r = getVerboseMode()   ? r+"VERBOSE;"   : r;
        return r;
    }

    /** Shows the modes represented as single-characters, like a Unix file listing:
     *  from <code>-----</code> (no modes) to <code>BDGPT</code> (all modes set).
     *  <p>For example: <code>-D---</code> would be Debug mode, and
     *  <code>B--P-</code> would be Publishing mode and  Batch mode.
     * <i><b>Do NOT</b></i> parse this string: it is human-readable only and is subject to change
     * if more modes are needed.  Instead,
     * use the API calls getBatchMode(), getDesignMode(), getDebugMode, getPublishMode(),
     * and getTestMode().
     * </p>
     * <p> Modes are encoded as follows:
     *      <table border='1' cellpadding='2' cellspacing='0'>
     *        <tr>    <th><b>&nbsp;</th>  <th>Mode</th>         <th>Mnemonic</th>                 </tr>
     *        <tr>    <td>B</td>          <td>MODE_BATCH</td>   <td><b>B</b>atch</td>             </tr>
     *        <tr>    <td>D</td>          <td>MODE_DESIGN</td>  <td><b>D</b>esign</td>            </tr>
     *        <tr>    <td>G</td>          <td>MODE_DEBUG</td>   <td>debu<b>G</b></td>             </tr>
     *        <tr>    <td>P</td>          <td>MODE_PUBLISH</td> <td><b>P</b>ublish</td>           </tr>
     *        <tr>    <td>T</td>          <td>MODE_TEST</td>    <td><b>T</b>est</td>              </tr>
     *        <tr>    <td>T</td>          <td>MODE_VERBOSE</td>    <td><b>V</b>erbose</td>        </tr>
     *      </table>
     * </p>
     * @see #getModeString
     * @see #getModeListing
     */
    public String getModeStringShort(){
        String r =   (getBatchMode()   ? "B"  : "-")
                    +(getDesignMode()  ? "D"  : "-")
                    +(getDebugMode()   ? "G"  : "-")
                    +(getPublishMode() ? "P"  : "-")
                    +(getTestMode()    ? "T"  : "-")
                    +(getVerboseMode()    ? "V"  : "-");
        return r;
    }


    //================= Publishing ==============================================================

    private String publishProtoHostPort = "";
    public String getPublishProtoHostPort(){ return publishProtoHostPort;}
    private void setPublishProtoHostPort(String newvalue){ publishProtoHostPort = newvalue;}

    private String m_publishCacheName = "";
    public String getPublishCacheName(){return m_publishCacheName;}
    private void setPublishCacheName(String new_value){m_publishCacheName = new_value;}

    private StringBuffer m_publishStatus = new StringBuffer();
    public String getPublishStatus(){
        return "<table border='1' class='publishStatus'>"+m_publishStatus.toString()+"</table>";
    }
    public void addPublishStatusLine(String s){
        m_publishStatus.append("\r\n<tr><td>");
        m_publishStatus.append(s);
        m_publishStatus.append("</td></tr>");
    }

    private boolean m_publishComplete = false;
    public boolean isPublishComplete(){
        return m_publishComplete;
    }
    private void setPublishComplete(boolean newvalue){
        m_publishComplete = newvalue;
    }

    public PublishParamsDatatype getPublishParams()
    throws DynamideException {
        Property publishParamsProp = getProperty(Session.PROPERTY_NAME_PUBLISH_PARAMS);
        if ( publishParamsProp == null ) {
            throw new DynamideException("application property '"+Session.PROPERTY_NAME_PUBLISH_PARAMS+"' was not found.");
        }
        return (PublishParamsDatatype)publishParamsProp.getValue();
    }

    /** Being in publish mode means you can send all output to a static directory,
     *  which can then be used without Dynamide.  There are a few things to set up, so
     *  the safest thing to do is to call this method.
     */
    private void enterPublishMode(String protoHostPort, String uriPrefix, String defaultExtension, String publishCacheName){
        m_publishCacheName = publishCacheName;
        flipModeBits(true, MODE_PUBLISH);
        setBatchMode(true);
        setURIPrefix(uriPrefix);
        setPublishProtoHostPort(protoHostPort);
        LinkOptions lo = new LinkOptions();
        lo.setIncludeUser(false);
        lo.setUseSessionID(false);
        lo.setRelative(protoHostPort.length()==0);
        lo.setExtension(defaultExtension);
        setLinkOptions(lo);
    }

    public static Session publish(String applicationPath)
    throws Exception {
        String dir = ResourceManager.getResourceRoot();
        return publish(applicationPath, null, null, "", true);
    }

    public static Session publish(String applicationPath, String outputDir)
    throws Exception {
        return publish(applicationPath, null, null, outputDir, true);
    }

    /** Publish a dynamic Dynamide web application to a static set of web pages.
     * @param overrideParams can be null - if not null, overrides everything set in publishParams in application.xml
     * @param outputDir Specifically override the output directory specified in either the application.xml or the overrideParams.
     */
    public static Session publish(String applicationPath,
                                  PublishParamsDatatype overrideParams,
                                  com.dynamide.datatypes.EnumeratedDatatype overridePublishURLs,
                                  String outputDir,
                                  boolean start)
    throws Exception {
        String debugQueryString = "";
        Session session = createSession(applicationPath,
                                        applicationPath,
                                        Constants.SESSION_PREFIX,
                                        "",
                                        false,
                                        MODE_PUBLISH,
                                        null,
                                        debugQueryString,
                                        null);
        if ( overrideParams != null ) {
            Property publishParamsProp = session.getProperty(PROPERTY_NAME_PUBLISH_PARAMS);
            if (publishParamsProp == null){
                publishParamsProp = new Property(session, PROPERTY_NAME_PUBLISH_PARAMS, overrideParams);
                session.setProperty(PROPERTY_NAME_PUBLISH_PARAMS, publishParamsProp);
            } else {
                publishParamsProp.setValue(overrideParams);
            }
        }
        if ( overridePublishURLs != null ) {
            Property publisURLsProp = session.getProperty(PROPERTY_NAME_PUBLISH_URLS);
            if (publisURLsProp == null){
                publisURLsProp = new Property(session, PROPERTY_NAME_PUBLISH_URLS, overridePublishURLs);
                session.setProperty(PROPERTY_NAME_PUBLISH_URLS, publisURLsProp);
            } else {
                publisURLsProp.setValue(overridePublishURLs);
            }
        }
        PublishParamsDatatype publishParams = session.getPublishParams();
        if ( outputDir != null && outputDir.length()>0) {
            publishParams.setOutputDir(outputDir);
        }

        if ( publishParams.getOutputDir().length() == 0 ) {
            publishParams.setOutputDir(FileTools.join(ResourceManager.getResourceRoot(), "publish"));
        } else {
            publishParams.setOutputDir(session.expand(publishParams.getOutputDir()));
        }

        session.checkDirectoryWrite(publishParams.getOutputDir());

        session.addPublishStatusLine("Starting publish mode with: applicationPath: "+applicationPath
                                    +" publishParams: "+publishParams.toString());
        session.setDebugURL(publishParams.getURL());
        if (start){
            String err = session.startSession();  //if MODE_PUBLISH, will call doPublish().
            if ( err != null && err.length()>0) {
                Log.error(Session.class, err);
            }
        }
        return session;
    }

    /**
       @todo
           make a page showing all applications and whether they can be published and whether they *need* to be published
           based on filetimestamps.  Add this to site audit.
           See, then you publish to /static. :)
           An application is publish-able if it has the correct publish property/elements.
     */
    protected String doPublish()
    throws Exception {
        ScriptEventSource beforePublish = getEventSource("application_beforePublish");// let event create more fields or initialize.
        if ( beforePublish.source.length()>0 ) {
            ScriptEvent event = fireEvent(this, "application_beforePublish", "",  "", "", beforePublish, getFilename(), false);
            if ( event.resultCode != ScriptEvent.RC_OK ) {
                logError("ERROR: [39] see event.evalErrorMsg");
                //hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                m_sessionStartupStatus = event.evalErrorMsg;
                return m_sessionStartupStatus;
            }
        }
        try {
            Property publishParamsProp = (Property)getProperty(Session.PROPERTY_NAME_PUBLISH_PARAMS);
            if ( publishParamsProp == null ) {
                throw new DynamideException("Cannot publish: application property '"+Session.PROPERTY_NAME_PUBLISH_PARAMS+"' was not found.");
            }
            PublishParamsDatatype publishParams = (PublishParamsDatatype)publishParamsProp.getValue();
            addPublishStatusLine("In doPublish() with: "+publishParams.toString());

            String publishPrefixURI = publishParams.getURIPrefix();
            String publishOutputDir = publishParams.getOutputDir();
            String pagePublishOutputDir = publishOutputDir; //maybe later we'll support publishing dependencies and pages to separate dirs. %%
            String publishProtocolHostPort = publishParams.getProtocolHostPort();
            String publishDefaultExtension = publishParams.getDefaultExtension();
            String publishPagesList = publishParams.getPublishPagesList();
            String publishHideApplicationPath = publishParams.getHideApplicationPath();
            String publishSkipResourcesCopy = publishParams.getSkipResourcesCopy();


            String applicationID = getString("applicationID");
            if ( applicationID.length()==0) {
                applicationID = getApplicationPath();
                logWarn("application has no 'applicationID' property in application.xml. Using path: "+applicationID+" file: "+getFilename());
            }
            String publishPrefix = "cache:"+applicationID+':'+getAssembly().getBuildName();
            setFieldValue("publishPrefix", publishPrefix);

            if (publishHideApplicationPath.length()>0 && Tools.isTrue(publishHideApplicationPath)){
                m_applicationPath = ""; // NEW! 7/7/2003 2:44AM
                addPublishStatusLine("Hiding ApplicationPath: publishParams.hideApplicationPath == "+publishHideApplicationPath);
            }

            ResourceManager rm = getResourceManager();
            try {
                rm.destroyPublishCache(publishPrefix);
            } catch (Throwable t){
                System.out.println("Unable to destroyPublishCache: '"+publishPrefix+"'");
            }
            rm.createPublishCache(publishPrefix, publishPrefixURI);
            addPublishStatusLine("attempting enterPublishMode("+publishProtocolHostPort+",\""+publishPrefixURI+"\","+publishDefaultExtension+","+publishPrefix+")");
            enterPublishMode(publishProtocolHostPort, publishPrefixURI, publishDefaultExtension, publishPrefix);

            WorkflowResult flow = new WorkflowResult();
            ScriptEventSource onPublish = getEventSource("application_onPublish");// let event create more fields or initialize.
            if ( onPublish.source.length()>0 ) {
                ScriptEvent event = fireEvent(this, flow, "application_onPublish", "",  "", "", onPublish, getFilename(), false, null, "");
                if ( event.resultCode != ScriptEvent.RC_OK ) {
                    logError("ERROR: [39] see event.evalErrorMsg");
                    //hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                    m_sessionStartupStatus = event.evalErrorMsg;
                    return m_sessionStartupStatus;
                }
            }

            if ( flow.getAbort() ) {
                addPublishStatusLine("<b>ABORT: </b>"+flow.getMessage());
                return flow.getMessage();
            }

            Property publishURLsProp = getProperty("publishURLs");
            if ( publishURLsProp != null){
                Object obj = publishURLsProp.getValue();
                if (obj instanceof EnumeratedDatatype){
                    EnumeratedDatatype publishURLs = publishURLsProp.getEnumeratedDatatype();
                    if (publishURLs.size()>0 ) {
                        for (Iterator it = publishURLs.getCollection().iterator();it.hasNext();){
                            try {
                                String value = it.next().toString();
                                String url = expand(value);
                                HandlerResult handlerResult = new HandlerResult("", true);
                                String action = "";
                                setDebugURL(url);
                                String path = getRequestPath();
                                DynamideHandler fakeHandler = new DynamideHandler();
                                handlerResult = handleAction(fakeHandler, action, handlerResult);
                                String content = handlerResult.getResult();
                                if (handlerResult.getPrettyPrint()){
                                    boolean debugTransformations = Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
                                    if (debugTransformations){
                                        logDebug("Session.doPublish before prettyPrintHTML:\r\n-----------------\r\n"+content+"\r\n-----------------");
                                    }
                                    content = DynamideHandler.prettyPrintHTML(content);
                                    if (debugTransformations){
                                        logDebug("Session.doPublish after prettyPrintHTML:\r\n-----------------\r\n"+content+"\r\n-----------------");
                                    }

                                }
                                String pubfileName = FileTools.joinExt(path, publishDefaultExtension);
                                logDebug("pubfile path: "+path+" ext: "+publishDefaultExtension+" joined: "+pubfileName);
                                File pubfile = FileTools.saveFile(pagePublishOutputDir, pubfileName, content);
                                addPublishStatusLine("pubfile: "+pubfile.getCanonicalPath());
                            } catch (Exception e){
                                addPublishStatusLine("error: "+e);
                            }
                        }
                    }
                }
            }

            if ( publishPagesList != null && publishPagesList.equals("false") ) {
                addPublishStatusLine("<b>NOT</b> publishing pages in 'pages' list: publishParams.publishPagesList was false");
            } else {
                if ( flow.getDoDefaultForThis() ) {
                    addPublishStatusLine("Starting default publishing.");
                    String appURL = getApplicationPath(); // e.g. "/dynamide/doco"
                    Iterator en = getPages().iterator();  //list of all page names in project
                    while ( en.hasNext() ) {
                        String pagename = en.next().toString();
                        addPublishStatusLine("doing page: "+pagename);
                        // %% TODO: make a linkToPage() method that deals with either
                        //           /my/app?next=page1
                        //  versus
                        //           /my/app/page1.html
                        try {
                            Page p = getPageByID(pagename);
                            String content = p.outputPageResult();
                            File pubfile = FileTools.saveFile(pagePublishOutputDir, appURL+'/'+pagename+publishDefaultExtension, content);
                            addPublishStatusLine("pubfile: "+pubfile.getCanonicalPath());
                        } catch (Exception e){
                            addPublishStatusLine("error: "+e);
                        }
                    }

                }
            }

            if (Tools.isTrue(publishSkipResourcesCopy)){
                logDebug("SKIPPING resource copy because skipResourcesCopy was set");
                addPublishStatusLine("SKIPPING: skipResourcesCopy was set, so no files will be copied from resources/");
            } else {
                String resourcesHtml = FileTools.join(getAppDirectory(), "resources/html");

                if ( FileTools.directoryExists(resourcesHtml) ) {
                    try {
                        logDebug("publishing resources/html directory: "+resourcesHtml);
                        List outputList = FileTools.copyDir(resourcesHtml, pagePublishOutputDir);
                        addPublishStatusLine("Copied directories and files: <pre>\r\n"+Tools.collectionToString(outputList, "\r\n")+"</pre>");
                    } catch (Exception e)  {
                        logError("Couldn't copy directory: "+resourcesHtml, e);
                    }
                } else {
                    logDebug("publisher did not find a resources/html directory: "+resourcesHtml);
                }
            }


            addPublishStatusLine("Done publishing pages.  flow.getStatusMessage(): "+flow.getStatusMessage());

            addPublishStatusLine("attempting writeCache(\""+publishPrefix+","+publishOutputDir+"\")");
            File resultdir = rm.writeCache(publishPrefix, publishOutputDir);
            addPublishStatusLine("Session.doPublish() wrote cache '"+publishPrefix+"' as '"+rm.getCachePrefixURI(publishPrefix)
                                 +"' in directory: "+resultdir.getCanonicalPath());
            logInfo(m_publishStatus.toString());
            setPublishComplete(true);

            //leave in memory for debugging, or destroy if not debugging:
            //rm.destroyPublishCache(publishPrefix);

            //for each page in project, do page_onPublish, and attempt to publish.
            return "";
        } finally{
            fireAfterPublish();
        }
    }

    private void fireAfterPublish(){
        ScriptEventSource afterPublish = getEventSource("application_afterPublish");// let event create more fields or initialize.
        if ( afterPublish.source.length()>0 ) {
            ScriptEvent event = fireEvent(this, "application_afterPublish", "",  "", "", afterPublish, getFilename(), false);
            if ( event.resultCode != ScriptEvent.RC_OK ) {
                logError("ERROR: [39c] see event.evalErrorMsg");
                //hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                m_sessionStartupStatus = event.evalErrorMsg;
            }
        }
    }

    /** WARNING: this method turns off publish mode, batch mode, clears PublishProtoHost and URIPrefix,
     *  and nulls out the LinkOptions -- if you wish to preserve these values,
     *  do so before calling enterPublishMode().
     */
    public void leavePublishMode(){
        flipModeBits(false, MODE_PUBLISH);
        clearLinkOptions();
        setPublishProtoHostPort("");
        setURIPrefix("");
    }

    //================= URL and Link management ==============================================

    private String  m_lastPathInfo = "";

    /** When the request is complete, get("pathInfo") will return an empty string -- use this
     *  to find out what the value was when you are not in an active ServletRequest handler, e.g. in a Job,
     *  or looking at another Session.
     */
    public String getLastPathInfo(){return m_lastPathInfo;}

    private String  m_lastRequestPath = "";

    private String  m_URIPrefix = "";
    public String  getURIPrefix(){return m_URIPrefix;}
    private void setURIPrefix(String  new_value){m_URIPrefix = new_value;}

    /** When the request is complete, get("requestPath") will return an empty string -- use this
     *  to find out what the value was when you are not in an active ServletRequest handler, e.g. in a Job,
     *  or looking at another Session.
     */
    public String getLastRequestPath(){return m_lastRequestPath;}

    public String getRequestPath(){
        if ( getRequest() != null ) {
            return getURIPrefix()+getRequest().getRequestURI();
        } else {
            return m_debugRequestPath; //return getFieldValue("requestPath");
        }
    }

    private String  m_applicationPath = "";
    /** This value is always valid after startup, event if no
     * ServletRequest handler is active.
     */
    public String  getApplicationPath(){
        return getURIPrefix() + m_applicationPath;
    }
    /** Alias for getApplicationPath()
     */
    public String  getURLPath(){return getApplicationPath();}

    public String getPathInfo(){
        String requestPath = getRequestPath();
        String applicationPath = getApplicationPath();
        // get("requestPath").toString();
        try {
            String pathInfo =  requestPath.substring(applicationPath.length());
            while ( pathInfo.endsWith("/") ) {
                //logDebug("pathInfo before: "+pathInfo);
                pathInfo = pathInfo.substring(0, pathInfo.length()-1);
            }
            //logDebug("applicationPath: "+applicationPath+" requestPath: "+requestPath+" pathInfo: "+pathInfo);
            return pathInfo;
        } catch (Exception e){
            logError("pathInfo can't be calculated because one of the components is the wrong length. applicationPath: "+m_applicationPath+" requestPath: "+requestPath);
            return "";
        }
    }

    public String getAppURL(){
        if (m_linkOptions!=null){
            return getAppURL(m_linkOptions.getIncludeUser(),
                             m_linkOptions.getRelative(),
                             m_linkOptions.getUseSessionID(),
                             "",
                             "",
                             m_linkOptions.getExtension());
        } else {
            return getAppURL(true, true, true, "", "", "");
        }
    }

    public String getAppURL(String moreParams){
        if (m_linkOptions!=null){
            return getAppURL(m_linkOptions.getIncludeUser(),
                             m_linkOptions.getRelative(),
                             m_linkOptions.getUseSessionID(),
                             moreParams,
                             "",
                             m_linkOptions.getExtension());
        } else {
            return getAppURL(true, true, true, moreParams, "", "");
        }

    }

    public String getAppURL(String moreParams, String morePath){
        if (m_linkOptions!=null){
            return getAppURL(m_linkOptions.getIncludeUser(),
                             m_linkOptions.getRelative(),
                             m_linkOptions.getUseSessionID(),
                             moreParams,
                             morePath,
                             m_linkOptions.getExtension());
        } else {
            return getAppURL(true, true, true, moreParams, morePath, "");
        }
    }

    public String getAppURL(LinkOptions lo, String moreParams){
        return getAppURL(lo.getIncludeUser(), lo.getRelative(), lo.getUseSessionID(), moreParams, "", lo.getExtension());
    }

    public String getAppURL(LinkOptions lo, String moreParams, String morePath){
        return getAppURL(lo.getIncludeUser(), lo.getRelative(), lo.getUseSessionID(), moreParams, morePath, lo.getExtension());
    }

    public String getAppURL(boolean user, boolean relative, boolean sessionid, String moreParams,
                            String morePath, String extension){
        try {
            char sep = '?';
            StringBuffer b = new StringBuffer();
            if (!relative) {
                HttpServletRequest request = getRequest();
                if ( request != null ) {
                    b.append(ServletTools.getProtoHostPort(request));
                } else {
                    java.net.URL url = getDebugURLObject();
                    if ( url != null ) {
                        b.append(ServletTools.getProtoHostPort(url));
                    }
                }
            }
            String suri = "";
            Object uri = get("requestPath");
            if (uri == null || morePath.length()>0){
                suri = getApplicationPath();
            } else {
                suri = uri.toString();
                if (suri.length()==0){
                    suri = getApplicationPath();
                }
            }
            //6/19/2003 1:28PM suri = getURIPrefix() + suri;
            b.append(suri);
            b.append(morePath);
            if (  ! morePath.endsWith(".html")
               && ! morePath.endsWith(".htm")){
                b.append(extension);
            }
                //if (user){
                //    String theUser = getString("USER");
                //    if ( theUser.length()>0 ) {
                //        b.append("?USER="+get("USER"));
                //        sep = '&';
                //    }
                //}
            if ( moreParams.length()>0 ) {
                b.append(sep);
                sep = '&';
                b.append(moreParams);
            }
            if ( ! getBatchMode() && !isPoolable() ){
                if ( sessionid) {
                    b.append(sep);
                    sep = '&';
                    b.append("SESSIONID="+getSessionID());
                }
            }
            return b.toString();
        } catch( Exception e ){
            logError("in getAppURL(boolean,boolean,boolean)", e);
            return "";
        }
    }

    private LinkOptions m_linkOptions = null;

    public LinkOptions getLinkOptions(){
        return m_linkOptions;
    }

    /** Sets options to be used by the link*() methods.
     *  Clear these by calling setLinkOptions(null) or by calling clearLinkOptions().
     */
    public void setLinkOptions(LinkOptions newOptions){
        m_linkOptions = newOptions;
    }

    public void clearLinkOptions(){
        m_linkOptions = null;
    }

    public void setLinkOptions(boolean user, boolean relative, boolean sessionid){
        m_linkOptions = createLinkOptions(user, relative, sessionid);
    }

    public static LinkOptions createLinkOptions(){
        LinkOptions lo = new LinkOptions();
        return lo;
    }

    public static LinkOptions createLinkOptions(boolean user, boolean relative, boolean sessionid){
        LinkOptions lo = new LinkOptions();
        lo.setIncludeUser(user);
        lo.setRelative(relative);
        lo.setUseSessionID(sessionid);
        return lo;
    }

    /** convenience method, simply calls getInclude(resourcePath)
     */
    public String href(String resourcePath){
        return getInclude(resourcePath);
    }

    /** Create a hyperlink based on the current applicationPath, the current LinkOptions (see setLinkOptions)
     *  and the current value of getBatchMode().
     *  You can think of the href as
     *  <pre>
     *    [protocol://][host][:port][prefix][applicationPath][morePath][?queryParams]
     *       protocol, host, and port are present if relative is specified
     *  </pre>
     *  <br /><b>Example:</b><br />
     *   <pre>link("Click me")</pre>
     *
     * @param linkText The text the user sees.
     */
     public String link(String linkText){
        return linkHref(getAppURL(), "", linkText);
    }

    /**
     * Format a hyperlink (&lt;A> tag) with the supplied parameters.
     *
     *  <br /><b>Example:</b><br />
     *   <pre>link("myParam=some+text&my2ndParam=3", "Click me")</pre>
     *
     * @param linkText The text the user sees.
     */
    public String link(String moreParams, String linkText){
        return linkHref(getAppURL(moreParams), "", linkText);
    }

    /**
     * Format a hyperlink (&lt;A> tag) with the supplied parameters.
     *
     *  <br /><b>Example:</b><br />
     *   <pre>link("myParam=some+text&my2ndParam=3", "/doc/help", "Click me")</pre>
     *
     * @param linkText The text the user sees.
     */
    public String link(String moreParams, String morePath, String linkText){
        return linkHref(getAppURL(moreParams, morePath), "", linkText);
    }

    /**
     * Format a hyperlink (&lt;A> tag) with the supplied parameters.
     *
     *  <br /><b>Example:</b><br />
     *   <pre>link("myParam=some+text&my2ndParam=3", "/doc/help", "target='_blank'", "Click me")</pre>
     * If this were used in an application registered to handle /dynamide/links as its applicaitonURI,
     *  then this would display a link such as:<br />
     * <pre>
     * &lt;a href='/dynamide/links/doc/help?SESSIONID=1234&myParam=some+text&my2ndParam=3' target='blank>Click me&lt;/a>
     * </pre>
     * @param linkText The text the user sees.
     */
    public String link(String moreParams, String morePath, String attributes, String linkText){
        return linkHref(getAppURL(moreParams, morePath), attributes, linkText);
    }

    public String link(String moreParams, String morePath, String attributes, String linkText, String extension){
        String url;
        if (extension.length()>0){
            LinkOptions lo = getLinkOptions();
            if ( lo == null ) {
                if ( getPublishMode() ) {
                    logError("Application called Session.link(...) while in publish mode but did not have LinkOptions set");
                }
                lo = createLinkOptions();
                lo.setIncludeUser(true);
                lo.setRelative(true);
                lo.setUseSessionID(true);
                lo.setExtension(extension);
            }
            String prevExtension = lo.getExtension();
            try {
                url = getAppURL(lo, moreParams, morePath);
            } finally {
                lo.setExtension(prevExtension);
            }
        } else {
            url = getAppURL(moreParams, morePath);
        }
        return linkHref(url, attributes, linkText);
    }

    /**
     * Format a hyperlink (&lt;A> tag) with the supplied parameters.
     *
     *  <br /><b>Example:</b><br />
     *   <pre>linkHref("/foo/bar?zanzibar", "Click me")</pre>
     *
     * @param href Relative or absolute URL
     * @param linkText The text the user sees.
     */
    public String linkHref(String href, String linkText){
        return linkHref(href, "", linkText);
    }

    /**
     * Format a hyperlink (&lt;A> tag) with the supplied parameters.
     *
     *  <br /><b>Examples:</b><br />
     *   <pre>linkHref("http://mojo.com:8080/foo/bar?zanzibar", "target='_blank' class='biglink'", "Click me")</pre>
     *   <pre>linkHref("/foo/bar?zanzibar", "target='_blank' class='biglink'", "Click me")</pre>
     *
     * @param attributes Send in any extra attributes, such as target and class, but don't send in href.
     * @param linkText The text the user sees.
     */
    public String linkHref(String href, String attributes, String linkText){
        return "<a href='"+href+"' "+attributes+">"+linkText+"</a>";
    }

    //=============== String Informational function for use in shell ======================

    public String dumpFields(){
       //m_fields.sort();
       return ShellHelper.dumpMap(m_fields);
    }
    public String listFields(){
       return ShellHelper.listMap(m_fields);//.dump();
    }

    public String listAllEventsLong(){
        return listAllEvents(true);
    }

    public String listAllEvents(){
        return listAllEvents(false);
    }

    public String listAllEvents(boolean longListing){
        StringBuffer b = new StringBuffer();
        if ( longListing ) {
            b.append("---- application events ----\r\n");
        }
        b.append(listEvents());
        Iterator pages = m_pageNames.iterator();
        while(pages.hasNext()){
            String pageID = (String)pages.next();
            Page page = null;
            try {
                page = getPageByID(pageID);
            } catch (Exception e) {
                page = null;
            }
            if ( page == null ) {
                logError("Page could not be loaded in listAllEvents: "+pageID);
            } else  {
                String se = page.listEvents();
                if ( longListing ) {
                    b.append("\r\n");
                    b.append("---- "+pageID+" events ----");
                }
                if ( se.length()>0 ) {
                    b.append("\r\n");
                    b.append(se);
                }
            }
        }
        return b.toString();
    }

    public String listAllEventsHTML(boolean longListing, boolean loadPages){
        StringBuffer b = new StringBuffer(StringList.DUMPHTML_START);
        if ( longListing ) {
            b.append("<b>application</b><dm_nbsp/>events");
        }
        b.append(listEventsHTML());
        b.append(StringList.DUMPHTML_LINESEP);
        //m_pageNames.sort();
        Iterator pages = getPages().iterator();
        while(pages.hasNext()){
            String pageID = (String)pages.next();
            if ( loadPages == false ) {
                if (!isPageLoaded(pageID)){
                    continue;
                }
            }
            Page page = null;
            try {
                page = getPageByID(pageID);
            } catch (Exception e) {
                page = null;
            }
            if ( page == null ) {
                logError("Page could not be loaded in listAllEvents: "+pageID);
            } else  {
                String se = page.listEventsHTML();
                if ( longListing ) {
                    b.append("<b>"+pageID+"</b><dm_nbsp/>events");
                }
                if ( se.length()>0 ) {
                    b.append(se);
                    b.append(StringList.DUMPHTML_LINESEP);
                }
            }
        }
        b.append(StringList.DUMPHTML_END);
        return b.toString();
    }

    public String dumpPages(){
       return ShellHelper.dumpMap(m_loadedPages);
    }

    public String dumpWidgetTypePool(){
        return Tools.dumpHTML(m_widgetTypePool);
    }

    private String dumpStringList(StringList list){
        return list.dumpHTML();
    }

    /** If you want a Map, use $WEBMACRO_CONTEXT.getMap() */
    public static String dumpContext(Context WEBMACRO_CONTEXT){
        return com.dynamide.util.WebMacroTools.dumpContext(WEBMACRO_CONTEXT);
    }

    public String dumpRequestInfo(){
        return ServletTools.dumpRequestInfo(getRequest(), true, "#FFAD00", m_requestUploadFileItems);
    }

    public String dumpRequestParams(){
        return ServletTools.dumpRequestParams(getRequest(), "#FFAD00", m_requestUploadFileItems);
    }

    //================= The main handler entry point =====================================

    /** Handles all actions.  The full documentation
     * for the event model is here:
     * <a target="_blank" href="/dynamide/doco/doc/help/FAQ#eventFlowchart">Event Flowchart (local)</a>
     * or here: <a target="_blank" href="http://apps.dynamide.com:7080/dynamide/doco/doc/help/FAQ#eventFlowchart">Event Flowchart (website)</a>
     * Among items permformed here:
     * <ul>
     *   <li> set currentAction, adjust action from special handler
     *       used by com.dynamide.submit: Session.registerSubmitAction(...)
     *   <li> if magic value Constants.dmLayoutChanged, then perform the update.
     *   <li> put all query field values into session.  No validation now %% .
     *   <li> allow Page to handleAction
     *   <ul>    <li> this fires handleAction event</ul>
     *   <li> fire queryNextPage
     *   <li> output new page
     *   <li> clear currentAction
     *  </ul>
     */
    public HandlerResult handleAction(DynamideHandler handler, String action, HandlerResult handlerResult)
    throws Exception {

        //MEMORY_LEAKS:
        //1/7/2005 6:20PM
        //this kills all session activity, but still results in small leakage, I think.
        //if (true) return new HandlerResult("<html><body>Session SHORT_CIRCUIT</body></html>", false);

        try {
            return handleAction_inner(handler, action, handlerResult);
        } catch( DynamideLoggedException e ){
            String USER = e.getUSER();
            String SI = USER.length() == 0 ? "" : "&SESSIONID=admin-"+USER+"&USER="+USER;
            String SUB = "&SUBSESSIONID="+e.getSessionID();
            String msg =
                "<p>See the details for the whole session "
                +"<a href='"
                +"/dynamide/admin?next=expansionLog"+SI+SUB
                +"'>here</a></p><hr />Error Details:<hr />"
                +e.dump();
            String hooked = hookException(msg, e, null, ErrorHandlerInfo.EC_SESSION, false);
            if ( hooked.startsWith(HOOK_EXCEPTION_NO_ERROR_PAGE) ) {
                hooked = "<html><body>"
                         +"<h2>Application Error</h2>"
                         + msg
                         +"</body></html>";
            }
            HandlerResult hr = new HandlerResult(hooked, false);
            return hr;
        } catch( Throwable t ){
            String s = hookException("handleAction caught Error", t, null, ErrorHandlerInfo.EC_SESSION, true);
            HandlerResult hr = new HandlerResult(s, false);
            return hr;
        }
    }

    private  List<FileItem> m_requestUploadFileItems = null;
    public List<FileItem> getUploadFileItemsRaw(){
        return m_requestUploadFileItems;
    }
    public List<FileItem> getUploadFileItems(){
        List<FileItem> result = new ArrayList<FileItem>();
        if (m_requestUploadFileItems!=null) {
            for (FileItem item : m_requestUploadFileItems) {
                if (item.isFormField() == false) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public static class UploadedFile{
        public File file = null;
        public String filename = "";
        public String mimeType = "";
        public InputStream stream = null;
    }

    public UploadedFile getUploadedFile(String fileInputID, boolean saveFile)
    throws Exception {
        return getUploadedFile(fileInputID, "", saveFile);
    }

    /** returns the first uploaded file found that matches the fileIputID, which is the ID of the HTML form field.
     *  If you upload multiple, @see getUploadFileItems().  If saveFile is false, the stream pointer is saved in the UploadFile object.*/
    public UploadedFile getUploadedFile(String fileInputID, String optionalDirectory, boolean saveFile)
    throws Exception {
        UploadedFile result = new UploadedFile();
        List items = getUploadFileItems();
        if (items != null) {
            Iterator<FileItem> iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = iter.next();

                if (!item.isFormField() && item.getFieldName().equals(fileInputID)) {   //fileInputID is the name of the form field.
                    String fileName = item.getName();
                    String newFilename = fileName.replace(' ', '_');
                    newFilename = newFilename.replace('\'', '_');
                    newFilename = newFilename.replace('\"', '_');
                    if (!FileTools.isWebSafeFileName(newFilename)) {
                        throw new DynamideException("ERROR: invalid filename: " + fileName + ", even when substituted: " + newFilename);
                    }
                    if (Tools.isBlank(optionalDirectory)){
                        optionalDirectory = System.getProperty("java.io.tmpdir");
                    }
                    result.mimeType = item.getContentType();
                    result.filename = newFilename;
                    if (saveFile) {
                        result.file = new File(optionalDirectory, newFilename);
                        item.write(result.file);
                        //System.out.println("new file written: " + result.getCanonicalPath());
                    } else {
                        result.stream = item.getInputStream();
                    }
                    break;
                }
            }
        }
        return result;
    }

    private synchronized HandlerResult handleAction_inner(DynamideHandler handler, String action, HandlerResult handlerResult)
    throws Exception {

        incrHits();
        setHandler(handler);
        setHandlerResult(handlerResult);

        enterHandlerProc("Session.handleAction", "<b>["+action+"]</b>");
        try {


            //===================
        //MEMORY_LEAKS:
            //1/7/2005
            //    minimal leak:
            //    38,280K --> 38,760K
            //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 2</body></html>";
            //    if (true) return handlerResult;
            //===================

            HttpServletRequest request = m_handler.getRequest();

            // 2012-02-15 Laramie adding Apache Commons Fileupload tool, since query params are in
            //                   form values when using POST, and they are not pulled in automatically.
            // 2015-07-08 Moved this before logHandlerProcRequest so upload files and multipart form fields will get logged.

            if (ServletFileUpload.isMultipartContent(request)){
                m_requestHasUpload = true;
                m_requestUploadFileItems =   getUploadFileItems(request);
            }

            logHandlerProcRequest();

            m_lastRequestPath = get("requestPath").toString();

            String pathInfo = getPathInfo();
            m_lastPathInfo = pathInfo;

            if ( checkForSecureRedirect(handler, handlerResult) ) {
                setActivePage("");
                return handlerResult;
            }

            nextPageID = getQueryParam(Constants.nextPageID);
            String pageID = getQueryParam(Constants.pageID);
            boolean reloadPageRequested = Tools.isTrue(getQueryParam(Constants.reloadPage));

            //before going anywhere else (except secureredirect), fire the page_onLeavePage event
            String prevPage = getActivePage();
            if ( !Tools.isBlank(prevPage) && (!prevPage.equals(pageID)) ) {
                Page prev = getPageByID(prevPage);
                if ( prev != null ) {
                    prev.fire_onLeave();
                    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    // %% todo: handle if onLeave wishes to force another page...
                    // %% also todo: maybe fire it always, not just on abandon.
                    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                }
            }

            String resourceName = findResourcesHtmlFile(pathInfo);
            if ( resourceName != null ) {
                logDebug("------ Returning for plain HTML file :"+resourceName);
                javax.activation.MimetypesFileTypeMap map = new javax.activation.MimetypesFileTypeMap();
                String type = map.getContentType(resourceName);
                IContext ctx = getApplicationResource(resourceName);
                if (Assembly.isBinaryResource(ctx)){
                    handlerResult.binaryResult = Assembly.extractBinaryResourceContent(ctx);
                    handlerResult.setBinary(true);
                } else {
                    handlerResult.result = Assembly.extractResourceContent(ctx);
                }
                handlerResult.setMimeType(type);
                handlerResult.prettyPrint = false;
                setActivePage("");
                return handlerResult;
            }

            if ( getDesignMode() ) {
                if ( nextPageID.length() > 0 ) {
                    enterHandlerProc("Session.handleAction [designMode page requested]", "<b>["+nextPageID+"]</b>");
                    try {
                        Page page = loadPage(nextPageID, reloadPageRequested, null);
                        if ( page != null ) {
                            setActivePage(nextPageID);
                            return page.outputPage(handlerResult);
                        } else  {
                            logHandlerProc("designMode page request failed", "page not found in project: "+nextPageID);
                            handlerResult.prettyPrint = false;
                            handlerResult.result = hookException("Page '"+nextPageID+"' not found in project '"+getApplicationPath()+"'", (Throwable)null, null, ErrorHandlerInfo.EC_SESSION, true);
                            setActivePage("error-page");
                            return handlerResult;
                        }
                    } finally {
                        leaveHandlerProc("Session.handleAction [designMode page requested]", "");
                    }
                }
            }

            // 4/10/2004 must loadPage before putAllFieldsIntoSession
            Page page = loadPage(pageID, reloadPageRequested, null);

            //5/18/2003 7:26AM moved this here:
            action = putAllFieldsIntoSession(request, action);
            setCurrentAction(action);
            //log("action: "+action);
            if ( action.length()==0 ) {
                logHandlerProc("INFO", "no '"+Constants.action+"' query param sent.");
                //action = Constants.dmStart; //this may be heavy-handed.  should be able to handle no action without a restart. %%
            }

            if ( action.equalsIgnoreCase("showInternationalizationForm")){
                    handlerResult.result = showInternationalizationForm(request);
                    setActivePage("");
                    return handlerResult;
            } else if ( getQueryParam("showQueryParams").length()>0 ) {
                    handlerResult.result = handleShowQueryParams();
                    setActivePage("");
                    return handlerResult;
            }

            //===================
            //MEMORY_LEAKS:
            //1/7/2005      36,900-->38,236
            //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 3</body></html>";
            //    if (true) return handlerResult;
            //===================

            ScriptEvent event = null;

            boolean handledByRegisteredEvent = false;
            //First, deal with any actions that pages or widgets have specifically registered.
            RegisteredActionEvent ra = (RegisteredActionEvent)m_registeredActionEvents.get(action);
            if ( ra != null ) {
                event = fireEvent(this, ra.inputObject, ra.scriptEventSource.name, pageID, nextPageID, action, ra.scriptEventSource, "", false, null, "");
                handlerResult.prettyPrint = event.prettyPrint;
                handlerResult.mimeType = event.mimeType;
                if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                    logError("[handleAction_inner]"+ event.evalErrorMsg);
                    handlerResult.result = hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                    setActivePage("error-page");
                    return handlerResult;
                }
                handledByRegisteredEvent = true;
            }

            // System.out.println("session 351");

            if ( ! handledByRegisteredEvent ) {
                ScriptEventSource appActionSource = getEventSource("application_onAction");
                //log("appActionSource: "+appActionSource);
                event = fireEvent(this, "application_onAction", pageID, nextPageID, action, appActionSource, "", false);
                handlerResult.prettyPrint = event.prettyPrint;
            // System.out.println("session 352");

                if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                    logError("[handleAction_inner.2]"+event.evalErrorMsg);
                    handlerResult.result = hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_SESSION, true);
                    setActivePage("error-page");
            // System.out.println("session 353");
                    return handlerResult;
                } else if ( event.resultCode != ScriptEvent.RC_NO_EVENT_SOURCE ) {
                    //log("OK: "+event.resultSrc);
                    if ((event.resultAction == ScriptEvent.RA_RETURN_SOURCE) && (event.resultSrc.length()>0)){
                        logHandlerProc("INFO", "returning event.resultSrc");
                        handlerResult.result = event.resultSrc;
                        handlerResult.mimeType = event.mimeType;
                        handlerResult.setResponseCode(event.getResponseCode());
                        setActivePage("");
                        return handlerResult;
                    }
                }
            }
            // System.out.println("session 356");

            //===================
        //MEMORY_LEAKS:
            //1/7/2005     38,056 -->
            //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 4</body></html>";
            //    if (true) return handlerResult;
            //===================



            //*********************************************************************************************************
            //by now, event is not null, either because it was fired from registered event or from application_onAction.
            //*********************************************************************************************************

            boolean redirectFromApp = false;

            if (handledByRegisteredEvent){
                logHandlerProc("SKIP", "<b>Skipping remaining page events</b>, since registered event handled action");
                if (event.redirectURL.length()>0){
                    handlerResult.redirectURL = event.redirectURL;
                    redirectFromApp = true;
                }
            } else if (event.redirectURL.length()>0){
                logHandlerProc("SKIP", "<b>Skipping remaining page events</b>, since application requested a redirect"+event.redirectURL);
                handlerResult.redirectURL = event.redirectURL;
                redirectFromApp = true;
            } else if (event.resultAction == ScriptEvent.RA_JUMP_TO_PAGE){
                logHandlerProc("<b>SKIP</b>", "Skipping page events, since application requested RA_JUMP_TO_PAGE");
            } else {
                if ( page != null ) {
                    logHandlerProc("INFO", Constants.pageID + " param found: "+pageID);
                    if ( reloadPageRequested ) {
                        logHandlerProc("INFO", "reloadPage was requested for "+pageID);
                    }

                    event = page.handleAction(action); // request is already in the context so events can get at query fields.
                    //===================
                    //MEMORY_LEAKS:
                    //12/6/05 -->  per request: no persistent leaks.  It builds, but drops again.
                    //test with http://apps.dynamide.com:8080/dynamide/admin?USER=laramie&page=sessionsPage&action=OK
                    //    handlerResult.prettyPrint = false;
                    //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 3.532</body></html>";
                    //    if (true) return handlerResult;
                    //===================
                    handlerResult.prettyPrint = event.prettyPrint;

                    if ( event.resultCode != ScriptEvent.RC_OK
                         && event.resultCode != ScriptEvent.RC_NO_EVENT_SOURCE ) {  //not having source for an event is not an error, just return the page.
                            //Early return, this won't happen below:
                            if (LOG_EVENTS_TO_HANDLERLOG) logHandlerProc("DEBUG", "Result of page.handleAction: "+ event.dumpHTML());
                            //logError("ERROR: [34] ", "<pre>"+StringTools.escape(StringTools.makeLineNumbers(event.evalErrorMsg))+"</pre>");  //don't need to call logHandlerProc with logError calls.
                            logError("ERROR: [34]");  //don't need to call logHandlerProc with logError calls.
                            //handlerResult.result = event.evalErrorMsg;
                            handlerResult.prettyPrint = false;
                            handlerResult.result = hookException(event.evalErrorMsg, (Throwable)null, event, ErrorHandlerInfo.EC_PAGE, true);
                            setActivePage("error-page");
                            return handlerResult;
                    }
                    if ( page.hasFieldErrors() ) {   // new 2/18/2002 8:58PM
                        logHandlerProc("ERROR", "<b>Errors</b> in fields on page.  Reshowing page.");
                        m_pageID = pageID;
                        //Early return, this won't happen below:
                        if (LOG_EVENTS_TO_HANDLERLOG) logHandlerProc("DEBUG", "Result of page.handleAction: "+ event.dumpHTML());
                        setActivePage(pageID);
                        return page.outputPage(handlerResult);  //re-present, errors will take care of themselves.
                    } else {
                        logHandlerProc("INFO", "Page validated: "+pageID);
                    }
                    if ((event.resultAction == ScriptEvent.RA_RETURN_STREAM)) {
                        handlerResult.setBinaryStream(event.getBinaryResultStream());
                        handlerResult.mimeType = event.mimeType;
                        handlerResult.setPrettyPrint(false);
                        handlerResult.setResponseCode(event.getResponseCode());
                        setActivePage("");
                    } else if ((event.resultAction == ScriptEvent.RA_RETURN_SOURCE) && (event.resultSrc.length()>0)){
                        logHandlerProc("INFO", "returning event.resultSrc");
                        handlerResult.result = event.resultSrc;
                        handlerResult.mimeType = event.mimeType;
                        handlerResult.prettyPrint = event.prettyPrint;
                        handlerResult.setResponseCode(event.getResponseCode());
                        setActivePage("");
                        
                        
                        //===================
                        //MEMORY_LEAKS:
                        //12/6/05 -->  admin app: per request: 35,653 35,708 35,880, 35,992
                        //12/7/05 -->  memory-leaks app: per request: not significant
                        //test with http://apps.dynamide.com:8080/dynamide/admin?USER=laramie&page=sessionsPage&action=OK
                        //    handlerResult.prettyPrint = false;
                        //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 3.52</body></html>";
                        //    if (true) return handlerResult;
                        //===================
                        //without this there are leaks.
                        //System.out.println("returning handlerResult 3.52"+Tools.getStackTrace());
                        //MEMORY_LEAKS: 
                        //stops leaks if this is uncommented: handlerResult.prettyPrint = false;
                        
                        return handlerResult;
                    }
                }
            }//end page handling.

            if (LOG_EVENTS_TO_HANDLERLOG) if (event != null) logHandlerProc("DEBUG", event.dumpHTML());

            //NOTE: *** event *** is now either the result of page onAction or application onAction.

            //redirect takes precedence over jumpToPage
            if (event!=null && event.redirectURL.length()>0 ) {
                if ( ! redirectFromApp) logHandlerProc("REDIRECT", "from page onAction: "+event.redirectURL);
                //else already logged for session.
                handlerResult.redirectURL = event.redirectURL;
                setActivePage("");
                return handlerResult; //servlet will redirect
                //application_queryNextPage will get fired on the way back in if the redirect points at this app.
            }

            if (event != null && event.resultAction == ScriptEvent.RA_JUMP_TO_PAGE){
                logHandlerProc("RA_JUMP_TO_PAGE", event.nextPageID);
                nextPageID = event.nextPageID;
            }

            if (nextPageID.length()>0){
                logHandlerProc("INFO", "'"+Constants.nextPageID + "' param found: "+nextPageID);
            }
            //Now run everything through application_queryNextPage, even jumpToPage results
            // so that application_queryNextPage becomes the validator of page flow.

            event = null;

            String nextPageIDFromEventBefore_application_queryNextPage = nextPageID;

            ScriptEventSource application_queryNextPage = getEventSource("application_queryNextPage");
            if ( application_queryNextPage.source.length() > 0 ) {
                event = fireEvent(this, "application_queryNextPage", pageID, nextPageID, "", application_queryNextPage, getFilename(), false);

                //%%%% ARG!!! no hookException here, either.  Maybe hook this entire method and just throw the dang exceptions.

                handlerResult.prettyPrint = event.prettyPrint;
                //System.out.println("event after application_queryNextPage: "+event);
                //pick up the nextPageID as set or reset in the event.  If they did nothing,
                // then it will still be what we set it to.
                nextPageID = event.nextPageID;
                if (event.redirectURL.length()>0){
                    handlerResult.redirectURL = event.redirectURL;
                    logHandlerProc("SKIP", "<b>Skipping page events</b>, since application requested a redirect in application_queryNextPage: "+event.redirectURL);
                    setActivePage("");
                    return handlerResult;
                }
                if ((event.resultAction == ScriptEvent.RA_RETURN_SOURCE) && (event.resultSrc.length()>0)){
                    logHandlerProc("INFO", "returning event.resultSrc from application_queryNextPage");
                    handlerResult.result = event.resultSrc;
                    handlerResult.prettyPrint = event.prettyPrint;
                    handlerResult.setResponseCode(event.getResponseCode());
                    setActivePage("");
                    return handlerResult;
                }

            }


            if (LOG_EVENTS_TO_HANDLERLOG) logHandlerProc("DEBUG", "Result of application_queryNextPage: "+ event.dumpHTML());
            if ( event == null
                || event.resultCode == ScriptEvent.RC_NO_EVENT_SOURCE
                || nextPageID.length()==0 ) {
                //no application_queryNextPage was found.  Do them in application page order.
                if (nextPageID.length()==0){
                    nextPageID = getNextPageIDInOrder(pageID);
                    logHandlerProc("INFO", "No nextPageID returned, using page order: "+nextPageID+" current: "+pageID);
                }
            } else if ( event != null && (event.resultCode != ScriptEvent.RC_OK) ) {
                logError("ERROR: [33] detail skipped");
                //skipped: logError("ERROR: [33] ", "<pre>"+StringTools.escape(event.evalErrorMsg)+"</pre>");
                handlerResult.prettyPrint = false;
                handlerResult.result = event.evalErrorMsg;
                setActivePage("error-page");
                return handlerResult;
            } else {
                if (nextPageID != null && nextPageID.equals(nextPageIDFromEventBefore_application_queryNextPage)){
                    logHandlerProc("INFO", "application_queryNextPage returned the default page to go, possibly from a previous RA_JUMP_TO_PAGE: "+nextPageID);
                } else {
                    logHandlerProc("INFO", "application_queryNextPage returned the page to go to: "+nextPageID);
                }
            }

            //===================
        //MEMORY_LEAKS:
            //1/7/2005  single request-thread: 100 requests: 25,000--> 28,464
            // 10 threads, 10 requests: 28,464 --> 41,548
            // 10x10, again: 41,548 --> 46,196
            //1/20/2005
            //I was returning from this point, but now I removed it.
            //    handlerResult.result = "<html><body>TEST SHORT-CIRCUIT 6</body></html>";
            //    if (true) return handlerResult;
            //===================


            if ( nextPageID.length() > 0 ) {


                //===================
        //MEMORY_LEAKS:
                //1/7/2005
                //handlerResult.result = "<html><body>TEST SHORT-CIRCUIT</body></html>";
                //if (true) return handlerResult;
                //===================


                page = loadPage(nextPageID, reloadPageRequested, null);
                if ( page == null ) {
                    logError("ERROR: [30] nextPageID not in project: "+nextPageID);
                    // %%%%%%% todo, show error page instead:
                    handlerResult.result = "<html><body>ERROR: [30] page for nextPageID ('"+nextPageID+".xml') not defined in application.xml</body></html>";
                    setActivePage("error-page");
                    return handlerResult;
                }
                if ( reloadPageRequested ) {
                    logHandlerProc("INFO", "reloadPage was requested for "+nextPageID);
                }
                pageVisited(nextPageID);
                m_pageID = nextPageID;
                logHandlerProc("outputPage", nextPageID);
                setActivePage(nextPageID);
                return page.outputPage(handlerResult);
            } else {
                if ( page == null ) {
                    String msg =  "[30a] page not found in project: '"+pageID+"', or queryNextPage failed, or no pages in project.";
                    logWarn(msg+event);
                    // %%%%%%% todo, show error page instead:
                    handlerResult.result = "<html><body>ERROR: "
                                           +msg
                                           +(event!=null?event.dumpHTML():"")
                                           +dumpRequestParams()
                                           +"</body></html>";
                    setActivePage("error-page");
                    return handlerResult;
                }
                logHandlerProc("INFO", "(output page) No nextPageID was found, re-showing page: "+page.getID()+": "+event.dumpHTML());
                setActivePage(nextPageID);
                return page.outputPage(handlerResult); //reshows current.
            }
        } finally {
            setCurrentAction("");
            setHandlerResult(null);
            m_requestHasUpload = false;
            m_requestUploadFileItems = null;
            leaveHandlerProc("Session.handleAction", "<b>["+action+"]</b>");
        }
    }

    public HandlerResult handleURL(String url)
    throws Exception {
        setDebugURL(url);
        DynamideHandler fakeHandler = new DynamideHandler();
        String action = getQueryParam(Constants.action);
        HandlerResult handlerResult = new HandlerResult("", true);
        handlerResult = handleAction(fakeHandler, action, handlerResult);
        return handlerResult;
    }

    //================= General Methods =====================================

    public void sendRedirect(String url){
        logHandlerProc("REDIRECT", "application requested a redirect:   "+url);
        HandlerResult r = getHandlerResult();
        if (r != null){
            r.redirectURL = url;
        } else {
            throw new Error("can't send redirect in Session, HandlerResult is null");
        }

    }

    private boolean checkForSecureRedirect(DynamideHandler handler, HandlerResult handlerResult){
        HttpServletRequest request = handler.getRequest();
        if ( request == null) {
            return false;
        }

        String protocol = ServletTools.getProto(request);
        String currentHost = request.getServerName();
        String currentPort = ""+request.getServerPort();
        String currentURL = ServletTools.getURL(request);//without protohostport.
        String url = generateSecureRedirect(
            protocol,
            currentHost,
            currentPort,
            currentURL
        );
        if ( url.length()>0 ) {
            handlerResult.redirectURL = url;
            return true;
        }
        return false;
    }

    String generateSecureRedirect(String protocol, String currentHost, String currentPort, String currentURL){
        // Make sure that if SECURE == true, then we redirect to https
        String redirectURL = "";
        if ( getSecure() ) {
            logDebug("secure app: generateSecureRedirect("+protocol+","+currentHost+","+currentPort+","+currentURL+")");
            //logDebug("generateSecureRedirect in secure app");
            //This means that in the WebAppEntry, SECURE is true, which it got from web-apps.xml::app/SECURE==true
            IContext securectx = (IContext)getResourceManager().find("/conf/secure");
            String secureProtocol = (String)securectx.getAttribute("protocol");
            secureProtocol = (secureProtocol != null && secureProtocol.length()>0) ? secureProtocol : "https";
            String securePort = (String)securectx.getAttribute("port");
            securePort = securePort != null ? securePort : "443";
            String secureHost = (String)securectx.getAttribute("host");
            secureHost = (secureHost != null) ? secureHost : "";
            //Fill in defaults:
            String redirectHost = secureHost.length()>0 ? secureHost : currentHost;
            String redirectPort = securePort.length()>0 ? securePort : "";
            String redirectProtocol = "http";

            boolean doRedirect = false;
            //CASE: protocol==https AND secureProtocol==https
                //hunky-dory
            //CASE: protocol==http AND secureProtocol==http
                //hunky-dory
            //CASE: protocol==https AND secureProtocol==http
                //allow them to be MORE secure if they want.
            //CASE: protocol==http AND secureProtocol==https
                //redirect
            if ( ! protocol.equals(secureProtocol)){
                if ( protocol.equals("http") ) {
                    doRedirect = true;
                    redirectProtocol = secureProtocol;
                } else {
                    redirectProtocol = protocol; //protocol is https, but secureProtocol is http.  Allow more secure.
                }
            }
            //logDebug("redirectProtocol: "+redirectProtocol);

            //Now also check to see if a redirect was specified because of different host or port:
            if (!doRedirect && (!currentHost.equals(secureHost))){
                doRedirect = true;
            }

            if (!doRedirect && (!(""+currentPort).equals(securePort))){
                doRedirect = true;
            }


            String debugFrom;
            boolean debug = true;
            if (doRedirect){
                String portstring = "";
                if ( redirectProtocol.equals("https") && (redirectPort.length()>0) && (!redirectPort.equals("443"))) {
                    portstring = ":"+redirectPort;
                }
                if ( redirectProtocol.equals("http") && (redirectPort.length()>0) && (!redirectPort.equals("80"))) {
                    portstring = ":"+redirectPort;
                }
                redirectURL = redirectProtocol+"://"+redirectHost+portstring+currentURL;
                logHandlerProc("REDIRECT", "<b>Redirect</b> from Session, to secure protocol: "+redirectURL);
                if (debug){
                    debugFrom = protocol+"://"+currentHost+":"+currentPort+currentURL;
                    String msg = "redirect:"
                        +"\r\n request:  "+debugFrom
                        +"\r\n redirect: "+redirectURL
                      //  +"\r\n currentPort: "+currentPort
                      //  +"\r\n redirectPort: "+redirectPort
                      //  +"\r\n portstring: "+portstring
                        ;
                    logDebug(msg);
                }
                return redirectURL;   //EARLY RETURN SINCE REDIRECT.
            }
            if (debug){
                debugFrom = protocol+":"+currentHost+":"+currentPort+currentURL;
                logDebug("not redirecting: "+debugFrom);
            }
            System.out.println("session.getSecure: "+getSecure()+" protocol: "+secureProtocol);

        }
        //==============================================================================
        return "";
    }

    private int m_hits = 0;
    public int getHits(){return m_hits;}
    private void incrHits(){m_hits++;}

    public String findResourcesHtmlFile(String pathInfo){
        //First, look for anything that might be under resources/html, with any extension:
        String resourceName = FileTools.joinURI("resources/html", pathInfo);
        String filename = findAppFilename(resourceName);
        //Not found. Now look for anything in there with a matching .html extension.
        if ( filename == null ) {
            resourceName = FileTools.joinURIExt("resources/html", pathInfo, ".html");
            filename = findAppFilename(resourceName);
        }
        if ( filename != null ) {
            return resourceName;
        }
        return null;
    }

    public String getTemplateDirs(){
        return "UNIMPLEMENTED";//m_searchLocations.getTemplatePaths();
    }

    //private String  m_paletteDir = "";
    //public String  getPaletteDir(){return m_paletteDir;}
    //public void setPaletteDir(String  new_value){m_paletteDir = new_value;}
    public Map getPaletteList(){
        Map paletteList = Tools.createSortedCaseInsensitiveMap();
        //paletteList.setAllowDuplicates(true);  // %% this is weird. you should exclude these, since the right one will be found.
        //ultimately, you need to have the treeview call this to get a list of lists.




        String[] paletteDirs = {""};//%%SITES m_searchLocations.getTemplateDirs();






        int paletteDirs_size = paletteDirs.length;
        for (int p=0; p < paletteDirs_size; p++) {
            String palettedir = paletteDirs[p]+Assembly.WIDGETS_DIR_REL_RESOURCES;
            File directory = new File(palettedir);
            if ( directory.exists() ) {
                String[] files = directory.list();
                if (files == null || files.length == 0){
                    String dirnm = "";
                    try {
                        dirnm = directory.getCanonicalPath();
                    } catch (Exception foo){
                    }
                    logWarn("getPaletteList: empty directory (ok if app template dir): '"+dirnm+"'");//empty directory, I think.
                } else {
                    for ( int i = 0; i<files.length; i++ ) {
                        String upper = files[i].toUpperCase();
                        if (
                                //fixed by moving to apps/ide/templates. (upper.indexOf("COM.DYNAMIDE.IDE.") > -1)||
                                (upper.indexOf("DESIGNERS/") > -1)
                             || (upper.indexOf("EDITORS/") > -1)
                            )
                        {
                            //exclude from list -- you just have to hand select these ones.
                        } else if ( upper.endsWith(".XML") ) {
                            paletteList.put(files[i].substring(0, files[i].length()-4), "");
                        }
                    }
                }
            } else {
                logError("palettedir does not exist: "+palettedir);
            }
        }
        return paletteList;
    }


    public Session findSession(String otherSessionID){
        return findSession(otherSessionID, true);
    }

    public Session findSession(String otherSessionID, boolean touchLastAccessTime){
        if ( otherSessionID == null ) {
            return null;
        }
        Session result = (Session)getResourceManager().getSession(otherSessionID);
        if ( result != null && touchLastAccessTime) {
            result.touchLastAccessTime();
        }
        return result;
    }

    /** Looks for the special subsession named in the Session field "SUBSESSIONID".
     *  You can have multiple subsessions, but a common pattern is to create a subsession and store
     *  its sessionid in the SUBSESSIONID field when you know you'll have exactly one subsession.
     *  @return null if not found.
     */
    public Session getSubsession()
    throws Exception {
        Object obj = get("SUBSESSIONID");
        if (obj == null){
            return null;
        }
        return (Session)findSession(obj.toString());
    }


    private boolean m_closeWhenRequestComplete = false;
    public boolean getCloseWhenRequestComplete(){
        return m_closeWhenRequestComplete;
    }
    public void setCloseWhenRequestComplete(boolean value){
        m_closeWhenRequestComplete = value;
    }

    public void closeWhenRequestComplete()
    throws SecurityException {
        setCloseWhenRequestComplete(true);
    }

    private boolean m_isClosing = false;
    public boolean isClosing(){
        return m_isClosing;
    }

    public synchronized void close()
    throws SecurityException {
        if ( m_isClosing ) {
            return;
        }
        m_isClosing = true;
        //logDebug("Session.close~~~~~~ "+Thread.currentThread().getName()+ " session close: "+toString()+" at "+Tools.getStackTrace());

        //logInfo("Application closed");
        logHandlerProc("INFO", "Session closed");
        try {
            ScriptEventSource onClose = getEventSource("application_onClose");// let event create more fields or initialize.
            if ( onClose.source.length()>0 ) {
                ScriptEvent event = fireEvent(this, "application_onClose", "",  "", "CLOSE", onClose, getFilename(), false);
                if ( event.resultCode != ScriptEvent.RC_OK ) {
                    logError("ERROR: [38] see event.evalErrorMsg");
                }
            } else {
                //logDebug("No application_onClose defined.");
            }
        } catch (Throwable t2)  {
            logError("couldn't run application_onClose", t2);
        }

        getResourceManager().removeSession(this); //throws SecurityException if not called from THIS method.

        setHandler(null);
        m_rootResourceManager = null;
        m_assembly = null;
        m_widgetPool.clear();
        m_widgetPool = null;
        m_widgetTypePool = null; //not sure if I should clear if it is shared.... %%
        m_pageNames.clear();
        m_pageNames = null;
        try {
            Iterator it = m_Interpreters.values().iterator();
            while ( it.hasNext() ) {
                IInterpreter interp = (IInterpreter)it.next();
                interp.close();
            }
        } catch (Throwable t)  {
            logError("Couldn't clear interps list", t);
        }

        m_Interpreters.clear();
        m_Interpreters = null;
    }

    public String toString(){
        return getApplicationPath()+':'+sessionID+':'+getUSER();
    }

    public String getCategoryID(){
        return getApplicationPath();
    }

    public void readPagesElement(){
        Element pages = findFirstElement("pages");
        if (pages!=null){
            List pageElements = pages.getChildren("page");
            Iterator i = pageElements.iterator();
            //lazy loading of pages, and just load the page names.
            //because loading the page loads all the widgets....
            //%% todo: allow an application deployment to specify the loading, either preload or lazy per page.
            while ( i.hasNext() ) {
                Element pageElement = (Element) i.next();
                String pageID = pageElement.getAttributeValue("name");
                if (pageID != null){
                    //Page page = new Page(this, pageID, Tools.fixFilename(m_searchLocations.findAppFilename(pageName+".xml")), this);
                    //if (debug) log(page.outputPage());
                    m_pageNames.add(pageID); //,page

                    String isDefault = pageElement.getAttributeValue("default");
                    if ( isDefault!=null && isDefault.length()>0 ) {
                        m_defaultPage = pageID;
                    }
                } else {
                    logWarn("WARNING: ------- pageID was null: "+pageElement);
                }
            }
        }
    }

    public String getInclude(String includeName){  // e.g. "js/page.js"
        if ( getPublishMode() ) {
            return m_assembly.getInclude(m_applicationPath, getAppname(), includeName, m_publishCacheName);  //%%
        } else  {
            return m_assembly.getInclude(m_applicationPath, getAppname(), includeName);
        }
    }

    /** Returns a non-zero-length string for the file named by resourceID if found,
     *  else returns the empty string.
     */
    public String findResourceFilename(String resourceID){
        IContext context = m_assembly.getResource(resourceID, true);
        if ( context != null ) {
            return (String)context.getAttribute(Assembly.FULLPATH);
        }
        logDebug("File not found in Session.findResourceFilename: "+resourceID);
        return "";
    }

    /** Returns a non-zero-length string for the file named by resourceID if found,
     *  else returns the empty string.
     */
    public String findAppFilename(String resourceID){
        return findAppFilename(resourceID, true);
    }

    public String findAppFilename(String resourceID, boolean quiet){
        if (m_assembly == null){
            logError("m_assembly is null in Session: '"+getSessionID()+"', looking for resource: "+resourceID);
            return null;
        }
        IContext context = m_assembly.getApplicationResource(getAppname(), resourceID);
        if ( context != null ) {
            return (String)context.getAttribute(Assembly.FULLPATH);
        }
        if (!quiet)logDebug("File not found in Session.findAppFilename: "+resourceID);
        return null;
    }

    /** Always returns a non-zero-length string for the file named by resourceID, or throws a FileNotFoundException.
     */
    public String getAppFilename(String resourceID)
    throws FileNotFoundException {
        String result = findAppFilename(resourceID);
        if (result !=null && result.length()>0){
            return result;
        }
        throw new FileNotFoundException(resourceID);
    }

    public IContext getApplicationResource(String resourceID)
    throws FileNotFoundException {
        return getAssembly().getApplicationResource(getAppname(), resourceID);
    }

        public String getAppFileContent(String resourceID)
    throws FileNotFoundException {
        IContext context = m_assembly.getApplicationResource(getAppname(), resourceID);
        if ( context != null ) {
            return (String)context.getAttribute(Assembly.CONTENT);
        }
        throw new FileNotFoundException("'"+resourceID+"' in app '"+getAppname()+"'"+m_assembly);
    }

    public byte[] getAppFileBytes(String resourceID)
    throws FileNotFoundException {
        IContext context = m_assembly.getApplicationResource(getAppname(), resourceID);
        if ( context != null ) {
            return (byte[])context.getAttribute(Assembly.CONTENT);
        }
        throw new FileNotFoundException(resourceID);
    }

    public String getInclude_NEW(String resourceID){
        return resourceID; // %% TODO
        /*   // if ( eventExists("onInclude") ) {
            fireEvent("onInclude", in object = reosurceID)
            out object ==> special resource ID
            if ( special reource id,  ) {
                use it, it will end up firing an event to get the resource from the session.
            } else {
                format it using the global cache.
            }
         */
    }

    public Application getApplication()
    throws Exception {
        ResourceManager rm = getResourceManager();
        IContext dm = (IContext)rm.find("/homes/dynamide");
        IContext apps = (IContext)dm.find("applications");
        if ( apps == null ) {
            apps = dm.rebind(new ContextNode("applications"));
        }
        IContext doco = (IContext)apps.find("doco"); //%%%%%%%%%%%%%%%%%%%%%%% JUST FOR PROTOTYPING...... %%%%%%%%%%
        if ( doco == null ) {
            doco = new Application(null, null);
            doco.setKey("doco");
            apps.rebind("doco", doco);
        }
        return (Application)doco;
    }

    public IContext getAppdata(){
        ResourceManager rm = getResourceManager();
        String appdataContextName = "/homes/dynamide/appdata/"+getAppname();
        //rm.ensureContextPath(appdataContextName);
        return (IContext)rm.find(appdataContextName);
    }

    public String readDataFileToString(String relativeToHome)
    throws Exception {
        return FileTools.readFile(getHomeDir(), relativeToHome);
    }

    private static final String CACHE_NAME = "cache:Session-content";

    private IContext checkCache(){
        IContext result = getContext(CACHE_NAME);
        if ( result == null ) {
            result = new ContextNode(CACHE_NAME);
            rebind(CACHE_NAME, result);
        }
        return result;
    }

    public IContext putInCache(String key, Object obj, boolean applicationShared)
    throws Exception {
        if ( applicationShared ) {
            return getApplication().putInCache(key, obj);
        }
        IContext item = getFromCache(key, applicationShared);
        if ( item == null ) {
            item = checkCache().rebind(key, new ContextNode(key));
        }
        item.rebindAttribute(Assembly.CONTENT, obj);
        Assembly.accessed(item);
        return item;
    }

    public IContext getFromCache(String key, boolean applicationShared)
    throws Exception {
        if ( applicationShared ) {
            return getApplication().getFromCache(key);
        }
        IContext result = checkCache().getContext(key);
        Assembly.accessed(result);
        return result;
    }

    public Object getContentFromCache(String key, boolean applicationShared)
    throws Exception {
        IContext item = getFromCache(key, applicationShared);
        if ( item != null ) {
            return item.getAttribute(Assembly.CONTENT);
        }
        return null;
    }

    public void commitInMemoryChangesToDOM() throws Exception {
        Property property = new Property(this, "lastSave", Tools.now().toString());
        property.set("readOnly", "true");
        property.set("datatype", "java.util.Date");
        setProperty("lastSave", property);

        Element root = getRootElement();
        root.removeChild("pages");
        Element pagesElement = new Element("pages");
        root.addContent(pagesElement);
        Element pageElement;
        String pageID;
        Iterator it = m_pageNames.iterator();
        while (it.hasNext()){
            pageID = ((String)it.next());
            pageElement = new Element("page");
            pageElement.setAttribute("name", pageID);
            pagesElement.addContent(pageElement);
        }
        Element properties = getPropertiesElement();
        AbstractWidget.cleanProperties(properties);
        root.removeChild("properties");
        root.addContent(properties);
    }

    /**
      * @dynamide.keymethod Key method for finding objects in Dynamide, returns null if not found.
     */

    public Object findDotted(String dottedName){
        Vector v = StringTools.parseSeparatedValues(dottedName, ".");
        int len = v.size();
        int i = 0;
        DynamideObject curr = this;
        DynamideObject next = null;
        Object obj;
        while ( i < len ) {
            String name = (String)v.elementAt(i);
            try {
                obj = curr.get(name);
            } catch (Exception e){
                logError("Exception in Session.find() calling curr.get(\""+name+"\") on curr: "+curr);
                return null;
            }
            if ( obj == null ) {
                return null;
            } else if (obj.toString().length()==0){
                return null;  //this is used for finding objects, not string values.""; //got something like "value", but it was empty.
            }
            if ( obj instanceof DynamideObject ) {
                next = (DynamideObject)obj;
            } else {
                if ( i == len-1 ) {
                    return obj;  //This is the last in the line, but it is some kind of fundamental type: String, Enumeration, etc.  Just return it.
                } else {
                    return null; //there are more objects to go, but the next one won't have a .get(String) method, so we are hosed.
                }
            }
            curr = next;
            i++;
        }
        return next;
    }

    public Object safeFind(String dottedName){
        Object o = null;
        try {
            o = findDotted(dottedName);
        } catch (Exception e2){
            logError("[122] Error in Session.safeFind", e2);
        }
        return o != null ? o : "";
    }

    public void throwException(String message)
    throws DynamideException {
        throw new DynamideException(message);
    }

    public void assertNotNull(Object test, String message, Object caller)
    throws DynamideException {
        if ( test == null ) {
            throw new DynamideException("Assertion: Object was null. Message: '"+message+"' caller: "+caller);
        }
    }

    public void assertNotEmpty(String test, String message, Object caller)
    throws DynamideException {
        if ( test == null || test.length() == 0 ) {
            throw new DynamideException("Assertion: String was empty. Message: '"+message+"' caller: "+caller);
        }
    }

    public String getString(String what){
        try {
            Object o = get(what);
            if ( o != null ) {
                return o.toString();
            } else  {
                return "";
            }
        } catch( Exception e ){
            logError("getString(\""+what+"\")", e);
            return "";
        }
    }

    /** Returns values by searching in this order: specially named values,  named pages, Properties, then registered Field objects.
     * These are the specially named values, in order:
"SESSIONID",
"session",
"application",
"page",
"next",
"nextPageID",
"USER",
"requestPath",
"applicationPath",
"pathInfo",
"account",
"RESOURCE_ROOT",
"lastAccessTime",
"startTime",
"currentPageID",
"hits",
"modeStringShort",
"pooled".
     */
    public Object get(String what)
    throws Exception {
        if ( what.equalsIgnoreCase("SESSIONID") ) {
            return sessionID;
        } else if ( what.equalsIgnoreCase("session") ) {
            return this;
        } else if ( what.equalsIgnoreCase("application") ) {
            return this;
        } else if ( what.equalsIgnoreCase("page") ) {
            return m_pageID;
        } else if ( what.equalsIgnoreCase("next") ) {
            return nextPageID;
        } else if ( what.equalsIgnoreCase("nextPageID") ) {
            return nextPageID;
        } else if ( what.equalsIgnoreCase("USER") ) {
            return getUSER();  //will be found by lookupInternationalizedValue, but let's save a step.
        } else if ( what.equalsIgnoreCase("requestPath") ) {
            return getRequestPath();
        } else if ( what.equalsIgnoreCase("applicationPath") ) {
            return getApplicationPath();
        } else if ( what.equalsIgnoreCase("pathInfo") ) {
            return getPathInfo();
        } else if ( what.equalsIgnoreCase("account") ) {
            return getAccount();
        } else if ( what.equalsIgnoreCase("RESOURCE_ROOT") ) {
            return ResourceManager.getResourceRoot();
        } else if ( what.equalsIgnoreCase("lastAccessTime") ) {
            return ""+getLastAccessTime();
        } else if ( what.equalsIgnoreCase("startTime") ) {
            return ""+getStartTime();
        } else if ( what.equalsIgnoreCase("currentPageID") ) {
            return getCurrentPageID();
        } else if ( what.equalsIgnoreCase("hits") ) {
            return ""+getHits();
        } else if ( what.equalsIgnoreCase("modeStringShort") ) {
            return ""+getModeStringShort();
        } else if ( what.equalsIgnoreCase("pooled") ) {
            return ""+getPooled();
        } else {
            String value = lookupInternationalizedValue("session."+what);
            if (value == null){
                value = getPropertyStringValue(what);
            }
            if (value.length() > 0){
                return value;
            }
            Page page = findPageByID(what);
            if ( page != null ) {
                return page;
            }
            Field f = getField(what);
            if ( f != null ) {
                return f;
            }
            IDatasource ds = getDatasource(what);
            if ( ds != null ) {
                return ds;
            }
            Object obj = super.get(what);
            if (obj!=null){
                return obj;
            }
            return getQueryParam(what); //returns an empty string if not found.
            //specifically does NOT call super.get(), which is mostly for AbstractWidget and subclasses.
        }
    }

    /** Returns whether a String value of a Field was "true", ignoring case.
     */
    public boolean isValueTrue(String fieldName){
        try {
            return get(fieldName).toString().equalsIgnoreCase("true");
        } catch (Exception e){
            logError("[Session.isValueTrue] isValueTrue caught exception", e);
            return false;
        }
    }

    public boolean isValueNull(Object value){
        return value == null;
    }

    public String[] getVisitedPages(){
        return m_visitedPages.toStringArray();
    }

    public int getVisitedPagesCount(){
        return m_visitedPages.size();
    }

    private String m_activePage = "";
    protected void setActivePage(String pageID){
        m_activePage = pageID;
    }
    public String getActivePage(){
        return m_activePage;
    }

    public boolean isBeforeFirstPage(){
        return (m_visitedPages.size()==0);
    }

    private String m_pageID = "";

    private String nextPageID = "";
    public String getNextPageID(){return nextPageID;}
    public void setNextPageID(String  new_value){nextPageID = new_value;}

    public boolean isPageLoaded(String pageID){
        return m_loadedPages.containsKey(pageID);
    }

    public Collection getLoadedPages(){
        return m_loadedPages.values(); //todo: %% make it clone or be immutable
    }
    public Map getLoadedPagesMap(){
        return m_loadedPages; //todo: %% make it clone or be immutable
    }
    public List getPages(){
        return m_pageNames;
    }

    /** Just add a page to the list of names in the project.  No checking is done,
     *  and the page will only become part of application.xml if you call saveToFile().
     */
    public void addPageName(String name){
        m_pageNames.add(name);
    }

    public List getObjects(){
        Vector v = new Vector(m_pageNames);
        v.insertElementAt("application",0);
        return v;
    }

    public StringList getPageList(){
        return new StringList(m_pageNames);
    }
    public String listPages(){
        String nl = "\r\n";
        String indent  = "    ";
        StringBuffer b = new StringBuffer();
        StringList sl = new StringList(m_pageNames);
        sl.sort();
        int sl_size = sl.size();
        for (int i=0; i < sl_size; i++) {
            String key = sl.getString(i);
            b.append(indent);
            b.append(key);
            b.append(nl);
        }
        return b.toString();
    }

    public StringList getPageListSorted(){
        StringList pageList = getPageList();
        pageList.sort();
        return pageList;
    }

    public String getNextPageIDInOrder(String currentPageID){
        String autoIncrementNextPage = getPropertyStringValue("autoIncrementNextPage");
        boolean bAuto = Tools.isTrue(autoIncrementNextPage);
        boolean bHasPage = hasPage(currentPageID);
        if (m_visitedPages.size()==0){ //they just started.
            if (m_pageNames.size() == 0 && bHasPage == false){
                logWarn("No pages in project!");
                return "";
            }
            if ( bAuto ) {
                return getDefaultPage();
            } else if (hasPage(currentPageID)) {
                return currentPageID;
            }
        }
        int indx = m_pageNames.indexOf(currentPageID);
        if ( hasPage(currentPageID) ) {
            if ( bAuto ) {
                if (m_pageNames.size() <= indx+1){
                    return currentPageID;
                } else {
                    return m_pageNames.get(indx + 1).toString();
                }
            } else {
                return currentPageID;
            }
        }
        return getDefaultPage();
    }

    private String m_defaultPage = "";

    public String getDefaultPage(){
        if ( m_defaultPage.length()==0 ) {
            if (m_pageNames.size() == 0){
                logWarn("No pages in project!");
                return "";
            } else {
                return m_pageNames.get(0).toString();
            }
        } else {
            return m_defaultPage;
        }
    }

    public StringList getTestStringList(){
        StringList foo = new StringList();
        foo.add("hi");
        return foo;
    }

    private StringList m_visitedPages;

    private WebMacro m_webmacro = null; // %% It is supposed to be OK to hang on to this, as long as you
                                        // don't hang onto the Context, since that should be one per thread.

    private HttpServletRequest m_request = null;
    public HttpServletRequest getRequest(){
        if (m_request!=null){
            return m_request;
        } else if (m_handler != null) {
            return m_handler.getRequest();
        } else {
            return null;
        }
    }
    private void setRequest(HttpServletRequest new_value){
        m_request = new_value;
        if (m_request != null){
            setBrowserID(ServletTools.findBrowserID(m_request));
        }
    }

    public HttpServletResponse getResponse(){
        if (m_handler==null){
            return null;
        } else {
            return m_handler.getResponse();
        }
    }

    /**   (Note: it is possible that there could be some threading issue, but I think
     *     the DynamideServlet doesn't let two requests live in the same session. )
     *
     * Usage:
        fileItems[0].getFieldName()
        fileItems[0].getFileName()
     */
    public List<FileItem> getUploadFileItems(HttpServletRequest request) throws FileUploadException {
        if (request!=null && ServletFileUpload.isMultipartContent(request)){
            ServletFileUpload upload = getServletFileUpload();
            List fileItems = upload.parseRequest(request);
            return fileItems;
        }
        return new ArrayList<FileItem>();
    }

    public ServletFileUpload getServletFileUpload(){
        FileItemFactory factory = new DiskFileItemFactory();  //disk-based file items
        return new ServletFileUpload(factory);
    }

    private DynamideHandler m_handler = null;
    public DynamideHandler getHandler(){return m_handler;}
    public void setHandler(DynamideHandler new_value){
        //System.out.println("setHandler:"+sessionID+" "+Thread.currentThread().getName()+" "+m_handler+", "+new_value);
        m_handler = new_value;
    }

    private HandlerResult m_handlerResult = null;
    public HandlerResult getHandlerResult(){return m_handlerResult;}
    public void setHandlerResult(HandlerResult new_value){m_handlerResult = new_value;}

    private String m_debugURL = "";
    private String m_debugRequestPath = "";
    private URLDecoder m_debugURLDecoder = null;
    public String getDebugURI(){return m_debugURL;}
    public void setDebugURL(String url){
        if ( url == null || url.length() == 0 ) {
            m_debugURL = "";
            m_debugURLDecoder = null;
        } else  {
            m_debugURL = url;
            m_debugURLDecoder = new URLDecoder(url);
            m_debugRequestPath = m_debugURLDecoder.getRawValue();
            //logDebug("setting m_debugRequestPath: "+m_debugRequestPath +" from "+url);
        }
    }
    public java.net.URL getDebugURLObject()
    throws Exception {
        return m_debugURLDecoder == null ? null : new java.net.URL(m_debugURLDecoder.getRawValue());
    }

    private boolean m_requestHasUpload = false;

    public String queryParam(String paramName){
        return getQueryParam(paramName);
    }

    public String getQueryParam(String paramName){
        //keep this function in sync with hasQueryParam()

        if ( m_debugURLDecoder != null ) {
            //logDebug("m_debugURLDecoder: "+m_debugURLDecoder);
            return m_debugURLDecoder.getFieldValue(paramName);
        }
        HttpServletRequest request = getRequest();
        if ( request == null ) {
            if (getDesignMode() == false){
                //in design mode, request can be null, since the ide may just call createNewSession which calls startSession, etc.
                //also, in shell or MODE_BATCH, etc....
                //There's just lots of times when it's OK.
                //logError("[getQueryParam] request was null");//+Tools.getStackTrace());
            }
            return "";
        }
        String result = "";

        if (m_requestHasUpload){
            if ( request.getParameter(paramName) != null){
                result = ServletTools.getParameterValue(request, paramName);
            } else {
                for(FileItem item : m_requestUploadFileItems){
                    String fieldName = item.getFieldName();
                    if ( ! StringTools.isEmpty(fieldName ) && fieldName.equals(paramName)) {
                        result = item.getString();   break;
                    }
                }
            }
        } else {
            result = ServletTools.getParameterValue(request, paramName);
        }
        return result;
    }

    /** During a request, this method sees if there is a query parameter by name in the request,
     *    either in the URL query string, or in the POST fields, or, if this is an upload, then also checks
     *    for POST fields sent within the upload multipart.  For POSTs, the query string goes in the action attribute,
     *    e.g. &lt;FORM action="/dynamide/doco?page=menu" ...
     */
    public boolean hasQueryParam(String paramName){
        //keep this function in sync with getQueryParam()

        if ( m_debugURLDecoder != null ) {
            //logDebug("m_debugURLDecoder: "+m_debugURLDecoder);
            return m_debugURLDecoder.hasField(paramName);
        }

         if (m_requestHasUpload && m_request != null){
            if (m_request.getParameter(paramName) != null){
                return true;
            }
         }

        HttpServletRequest request = getRequest();
        if ( request == null ) {
            return false;
        }
        return null != request.getParameter(paramName);
    }

    private String browser = "IE";
    private int browserID = ServletTools.BROWSER_IE;
    public int getBrowserID(){
        if (getRequest()!=null){
            browserID = ServletTools.findBrowserID(getRequest());
        }
        return browserID;
    }

    /** %% OOOPS: this is inherantly un-threadsafe.  Two simultaneous requests from different browsers
     *  would trip this up.  In theory, you only come in with one browser at a time on one Session,
     *  but if someone decided that they wanted a global, shared session this would be a problem.
     *  Better to always do this through the request.
     */
    public void setBrowserID(int new_value){
        browserID = new_value;
        browser = ServletTools.getBrowserStringFromID(browserID);
    }
    public String getBrowserStringID(){
        return browser;
    }

    /** Don't call this method from outside this class.  Instead, use expandTemplate().
     */
    public WebMacro getWebMacro()
    throws InitException {
        if ( m_webmacro == null ) {
            m_webmacro = initWebmacro();
        }
        return m_webmacro;
    }

    private static final WebMacro initWebmacro()
    throws InitException {
            //USE THIS when you run in a normal VM:
            org.webmacro.WM webmacro;
            webmacro = (org.webmacro.WM)ResourceManager.getRootResourceManager().getAttribute("Webmacro-Singleton");
            if ( webmacro == null ) {
                throw new org.webmacro.InitException("Webmacro-Singleton not found by Dynamide");
            }
            // %% todo:
            //Broker broker = webmacro.getBroker();
            //boolean cache = false;
            //String templateRoot = "";//ResourceManager
            //broker.addProvider(new TemplateProvider("template", templateRoot, cache), "template");
            return webmacro;
            //USE THIS FOR running in Together or other debuggers.  This is a full path:
            //org.webmacro.WM webmacro = new org.webmacro.WM("/java/com/dynamide/conf/WebMacroDynamide.properties");
            //m_webmacro = new WM();
            /*
             *  Well, a lot of this was to grab the info that went to stdout but not
             *  to the exceptions, but now I just use Cranky evaluator and everything is an exception,
             *  especially since this logging stuff never worked anyway.
             *  checkWebmacroLogRef();
                org.webmacro.util.LogSystem.getInstance("parser").addTarget(m_webmacroLog);
                org.webmacro.util.LogSystem.getInstance("system").addTarget(m_webmacroLog);
                org.webmacro.util.LogSystem.getInstance("engine").addTarget(m_webmacroLog);
                org.webmacro.util.LogSystem.getInstance().update(m_webmacroLog, "parser");
                org.webmacro.util.LogSystem.getInstance().update(m_webmacroLog, "engine");
             */
    }


    /** Don't call this method from outside this class.  Instead, use expandTemplate().
     */
    public Context getContext()
    throws InitException  {
        Context c = getContext(getWebMacro(), getDesignMode(), sessionID, getRequest());
        c.remove("session");
        c.put("session", this);
        return c;
    }

    /** Don't call this method from outside this class.  Instead, use expandTemplate().
     */
    public Context getContext(WebMacro wm)
    throws InitException  {
        Context c = getContext(wm, getDesignMode(), sessionID, getRequest());
        c.remove("session");
        c.put("session", this);
        return c;
    }

    /** It's OK to call will request == null, but then the request won't be in the context as a variable.  So the
     *  template you are expanding should not be a widget or page.
     */
    public static Context getContext(WebMacro wm, boolean designMode, String sessionID, HttpServletRequest request)
    throws InitException  {
        //IMPORTANT: call cloneContext() so you get one context per thread, as dictated in the Webmacro docs.
        //was: Context c = wm.getContext().cloneContext();
        //Context c1 = wm.getContext();
        Context c = wm.getContext();
        //Context c = c1.cloneContext();
        //c1.recycle();
        c.put("SESSIONID", new String(sessionID));
        c.put("request", request);
        if (designMode == true){
            c.put("designMode", new String("true"));
        } else {
            c.put("designMode", new String("false"));
        }
        return c;
    }

    public static void releaseContext(Context c){
        if ( c != null ) {
            c.remove("SESSIONID");
            c.remove("session");
            c.remove("designMode");
            c.remove("request");
            //c.clear();
            //3/22/2004 c.recycle();   //webmacro will return this object to the pool.
            c.clear();     //webmacro 2.01b they are back to clear, not recycle. 3/22/2004 11:40PM
        }
    }

    /** Written to support beanshell exception messages.  There's a different one for webmacro errors.  JDOM errors tend not to have linenums.
     */
    public static int getSyntaxExceptionLineNum(Exception parseException){
        //parseException.printStackTrace();
        String parserMessage = parseException.getMessage();
        int linenum = -1;
        int istart = parserMessage.indexOf("at line");
        if ( istart > -1 ) {
            int iend = parserMessage.indexOf(",", istart);
            if ( iend > -1 ) {
                linenum = Tools.stringToInt(parserMessage.substring(istart + "at line".length(), iend).trim());
            }
        }
        //Log.error(Session.class, "******************************************************\r\n"
        //+"Exception parsing template\n error<<" + parseException+">>"
        //+"\r\n*********************************************, returning as TemplateSyntaxException, with linenum: "+linenum);
        return linenum;
    }

    public static int getJDOMExceptionLineNum(Exception jdomException){
        //parseException.printStackTrace();
        String STARTTAG = "on line";
        String message = jdomException.getMessage();
        int linenum = -1;
        int istart = message.indexOf(STARTTAG);
        if ( istart > -1 ) {
            int iend = message.indexOf(":", istart);
            if ( iend > -1 ) {
                linenum = Tools.stringToInt(message.substring(istart + STARTTAG.length(), iend).trim());
            }
        }
        return linenum;
    }

    public static TemplateSyntaxException createTemplateSyntaxException(String errorMessage, String templateText){
        return createTemplateSyntaxException(errorMessage, templateText, -1);
    }

    public static TemplateSyntaxException createTemplateSyntaxException(String errorMessage, String templateText, int linenum){
        String errorHTML = formatTemplateSyntaxException(errorMessage, templateText, linenum);
        com.dynamide.TemplateSyntaxException te = new com.dynamide.TemplateSyntaxException("hidden message");//parseException.getMessage());
        te.setErrorHTML(errorHTML);
        return te;
    }

    public static String formatTemplateSyntaxException(String errorMessage, String templateText, int linenum){
        return "<table border='1'><tr><td><pre>"
                +StringTools.escape(errorMessage)
                +"\r\n<hr/>\r\n"
                +StringTools.makeLineNumbers(StringTools.escape(StringTools.escapeAmpersands(templateText)), linenum)
                +"</pre></td></tr></table>";
    }

    public Page getContainer(String pageID, String pageName, DynamideObject newOwner)
    throws Exception {
        if ( newOwner == null ) {
            newOwner = this;
        }
        Page page = new Page(newOwner,
                             pageID,
                             Tools.fixFilename(getAppFilename(pageName+".xml")),
                             this);
        page.load();
        return page;
    }

    public String expandContainer(String pageName)
    throws DynamideException {
        return expandContainer(pageName/*should be pageID %%*/, pageName, null, null);
    }

    /** @param c OK to pass null for param c, the context won't be inherited by the container.
     *  Container's entries into the context are never propogated back to the caller.
     */
    public String expandContainer(String pageID, String pageName, Context c, AbstractWidget parent)
    throws DynamideException {
        try {
            StringList variables = new StringList();
            //Page targetPage = getContainer(pageName);
            //logDebug("Session.expandContainer using parent: "+parent);
            Page targetPage = findPageByID(pageID, parent);
            if ( targetPage == null ) {
                //logDebug("Session.expandContainer using getContainer");
                targetPage = getContainer(pageID, pageName, parent);  // %%%%%% KLUDGE-CENTRAL.  There should be a different method for
                                                      // this, if you are sure that the page will not be found in the project,
                                                      // as the designers do, such as ServerSideEvent.designer.  getContainer creates a new page!!!
                if ( targetPage == null ) {
                    return "ERROR: [95] [expandContainer] targetPage not found or not initialized: "+pageID+" of type: "+pageName;
                }
                m_loadedPages.put(pageID, targetPage);
            }
            //logDebug("Session.expandContainer using target page: "+targetPage+" and parent: "+parent+" and targetPage.getOwner(): "+targetPage.getOwner());
            String logName = targetPage.get("type").toString();
            logWhichExpansion(logName);
            //WebMacro wm2 = getWebMacro();
            //Context c2 = getContext(wm2);
            try {
                Map map = null;
                if (c != null) {
                    map = c.getMap();
                }

                //if (map != null) {
                //    c2.putAll(map);
                //}
                //c2.remove("page");
                //c2.put("page", targetPage);
               // c2.remove("page");
                //c2.put("page", targetPage.getID());

                //%% todo, none of this fanciness above is used, now I just ask the targetPage to ref itself, which will break
                // new tac:
                if (parent == null && c != null) {
                    parent = (AbstractWidget)c.get("page");
                }

                if (map != null){
                    Set es = map.entrySet();
                    Iterator it = es.iterator();
                    while (it.hasNext()){
                        Map.Entry entry = (Map.Entry)it.next();
                        String key = entry.getKey().toString();
                        variables.remove(key);
                        variables.addObject(key, entry.getValue());
                    }
                }
                variables.remove("page");  //remove the parent as "page"
                variables.remove("pageID");
                variables.remove("parent");
                variables.addObject("page", targetPage);
                variables.addObject("pageID", targetPage.getID());
                variables.addObject("parent", parent);

                return targetPage.outputContainer(variables);

                //String src = targetPage.getSource("*");
                //return expandTemplate(variables, src, logName, wm2, c2, null);
            } finally {
                //releaseContext(c2);
            }
        } catch (com.dynamide.DynamideUncaughtException due){
            throw due;
        } catch (Exception e){
            String emsg = "ERROR: [36a] "+Tools.errorToString(e, true);
            logError(emsg, e);
            return getSession().hookException(emsg, e, ErrorHandlerInfo.EC_WIDGET);
        }
    }

    public String expandResource(String resourceID, String logName)
    throws Exception {
        return expandResource(resourceID, new StringList(), logName);
    }

    public String expandResource(String resourceID, StringList variables, String logName)
    throws Exception {
        String templateText = getAppFileContent(resourceID);
        return expandTemplate(variables, templateText, resourceID+"::"+logName);
    }

    public String expandTemplate(StringList variables, String templateText, String logName)
    throws DynamideUncaughtException {
        logWhichExpansion(logName);
        return expandTemplate(variables, templateText, logName, null, null, true);
    }

    private StringList m_emptyVars = new StringList();

    public String expandTemplate(String templateText, String logName, org.webmacro.Context c)
    throws DynamideUncaughtException {
        logWhichExpansion(logName);
        return expandTemplate(m_emptyVars, templateText, logName, null, c, false);
    }

    
    private String expandTemplate(StringList variables,
                                        String templateText,
                                        String logName,
                                        WebMacro wm,
                                        Context c,
                                        boolean addContextToContext)
    throws DynamideUncaughtException{
        boolean expansionError = false;
        String tmpl = "ERROR[expandTemplate]";
        boolean createdContext = false;
        IComposite expansionLogNode = null;
        ExpansionLog eLog = getExpansionLog();
        if (eLog!=null) {
            expansionLogNode
                = eLog.enterExpansion(this, logName, "templateSource", "text/html", ExpansionLog.formatEscapePre(templateText), "Session.expandTemplate");
        }
        try {
            if (c == null){
                createdContext = true;
                c = getContext();
            }
            if (wm == null){
                wm = getWebMacro();
            }
            
            templateText += "\r\n"; //add safe chars at the end, and strip it off later. WM tends to put a junk char
            //on the end, so we'll strip that instead, leaving the space, which is benign.
            org.webmacro.Broker broker = wm.getBroker();
            org.webmacro.Template t = new org.webmacro.engine.StringTemplate(broker, templateText, logName);
            //FastWriter fw = FastWriter.getInstance(broker, "US-ASCII"); //
                FastWriter fw = new FastWriter(broker, "US-ASCII");
            //3/22/2004 11:41PM out again for wm 2.01b fw.setAsciiHack(true);//10/29/2003 1:28AM
            StringReader sr = new StringReader(templateText);

            try {
                try {
                    org.webmacro.engine.Parser parser = (org.webmacro.engine.Parser) broker.get("parser", "wm");
                    //another, simpler way to do it: parser.parseBlock("template", sr);
                    parser.parseBlock(logName+".templateText", sr);
                } finally {
                    try {
                        sr.close();
                    } catch (Exception biggie){
                        System.out.println("exception trying to close StringWriter in Session"+biggie);
                        //oh, well, we tried.
                    }
                    sr = null;
                }
            } catch (Exception parseException) {
              //10/29/2003 1:47AM  fw.close();
                fw = null;
                int linenum = getSyntaxExceptionLineNum(parseException);
                String errorHTML = formatTemplateSyntaxException(parseException.getMessage(), templateText, linenum);
                if (eLog!=null) {
                    eLog.logExpansionError(expansionLogNode, "error", "text/html", errorHTML, "Session.expandTemplate");
                    expansionError = true;
                    throw new DynamideLoggedException(getSession(),
                                                      "ERROR in expanding "+logName+" got "+parseException.getClass().getName()+" at line "+linenum,
                                                      eLog.printExpansionLog(expansionLogNode));
                } else {
                    throw new DynamideUncaughtException("Error in expandTemplate", ErrorHandlerInfo.EC_SESSION, parseException);
                }
            }

            if (eLog!=null) {
                if (variables != null && variables.size()!=0) {
                    eLog.logExpansion(expansionLogNode, "variables", "text/html", variables.dumpHTML(true, false), "Session.expandTemplate");
                }
                eLog.logExpansion(expansionLogNode, "context", "text/html", dumpContext(c), "Session.expandTemplate");
            }
            int variables_size = variables.size();
            for (int i = 0; i<variables_size; i++){
                c.put(variables.getString(i), variables.getObjectAt(i));
            }
            com.dynamide.util.WEBMACRO_CONTEXTPointer p = new com.dynamide.util.WEBMACRO_CONTEXTPointer();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                if (addContextToContext){
                    p.setContext(c);
                    c.put("WEBMACRO_CONTEXT", p);
                    c.put("WEBMACRO", wm);
                }
                //System.out.println("=============== EE "+c.getEvaluationExceptionHandler().getClass().getName());
                //wm2.01b t.write(fw, c);
                t.write(bos, "US-ASCII", c);//wm2.01b
            } finally {
                if (addContextToContext){
                    c.remove("WEBMACRO_CONTEXT");
                    c.remove("WEBMACRO");
                    p.setContext(null);
                }
                for (int i = 0; i<variables_size; i++){
                    c.remove(variables.getString(i));
                }
            }
            //this causes an error.  It was never needed before I saw it in the doco, though: fw.flush();
            //wm2.01b  tmpl = fw.toString();
            tmpl = bos.toString();//wm2.01b
            //10/29/2003 1:47AM fw.close();
            fw = null;
            if (tmpl.length()>0){
                tmpl = tmpl.substring(0,tmpl.length()-2); //remove the extra chars we added, or the random one that WM generates, leaving the extra space we added.
            }
            boolean debugTransformations = com.dynamide.util.Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
            if (debugTransformations){
                Log.debug(Session.class, "Session.expandTemplate before webmacro expansion: '"+logName+"'\r\n-----------------\r\n"+templateText+"\r\n-----------------");
                if (variables != null && variables.size()!=0) {
                    Log.debug(Session.class, "Session.expandTemplate variables:\r\n-----------------\r\n"+variables.toString()+"\r\n-----------------");
                }
                Log.debug(Session.class, "Session.expandTemplate after webmacro expansion: '"+logName+"'\r\n-----------------\r\n"+tmpl+"\r\n-----------------");
            }
        } catch (Exception e)  {
            if (eLog!=null){
                eLog.logExpansionError(expansionLogNode, "error", "text/html", ExpansionLog.formatEscapePre(Tools.errorToString(e, true)), "Session.expandTemplate");
                expansionError = true;
                throw new DynamideLoggedException(getSession(),
                                                  "ERROR in expanding "+logName+" got "+e.getClass().getName(),
                                                  eLog.printExpansionLog(expansionLogNode));
            }
            logError("ERROR: [36] expandTemplate failed on logName: "+logName, e);
            return hookException("\r\n<br/>ERROR: [36] expandTemplate failed on logName: "+logName, e, ErrorHandlerInfo.EC_SESSION);
        } finally {
            if (eLog!=null && !expansionError) eLog.leaveExpansion(expansionLogNode, "result", "text/html", ExpansionLog.formatEscapePre(tmpl), "Session.expandTemplate");
            if ( createdContext ) {
                releaseContext(c);
            }
        }
        return tmpl;
    }

    public String transform(String xmlResourceID, String xslResourceID)
    throws Exception {
        String xml = findAppFilename(xslResourceID);
        String xsl = findAppFilename(xmlResourceID);
        if (xml == null || xml.length() == 0 || xsl == null || xsl.length() == 0){
            logError("Couldn't transform(\""+xmlResourceID+"\",\""+xslResourceID+"\"), files were missing.");
            return "ERROR";
        }
        return com.dynamide.xsl.SaxonJDOMTransform.transform(xml, xsl, this);
    }

    public WidgetType findWidgetType(String type){
        WidgetType widgetType = (WidgetType)m_widgetTypePool.get(type);
        if ( widgetType == null ) {
            try {
                //Short-cut: allow short names in widgets, if they are under the resources/widgets/ directory:
                String resname = findAppFilename(Constants.widgetsRelDir+'/'+type+".xml");  // widgetsRelDir+ is "resources/widgets"
                if (resname == null || resname.length()==0){
                    //Short-cut didn't work, try canonical loction.
                    resname = findAppFilename(type+".xml");
                }
                if (resname == null || resname.length()==0){
                    logError("Can't find WigdetType: "+type);
                    return null;
                }
                String fn = Tools.fixFilename(resname);
                File f = new File(fn);
                if (f.exists()){
                    //log("Opening WidgetType file: "+fn);
                    widgetType = new WidgetType(this, fn, this);
                    m_widgetTypePool.put(type, widgetType);
                } else {
                    logError("Can't find WigdetType file: "+fn);
                }
            } catch( Exception e ) {
                e.printStackTrace() ;
                return null;
            }
        }
        return widgetType;
    }

    public void clearWidgetTypePool(){
        m_widgetTypePool.clear();
    }

    /** %% TODO, you may have a namespace problem if the same name is picked up from the different palette directories.
     */
    public void clearWidgetTypePool(String widgetTypeName){
        m_widgetTypePool.remove(widgetTypeName);
    }

    //ISession interface:
    public void log(String message){
        logInfo(message);
    }

    public void logError(String message) {
        logHandlerProc("ERROR", message);
        //logError(message);
        super.logError(message);
    }

    public void logError(String message, String detail) {
        logHandlerProc("ERROR", "message: "+message+" detail: "+detail);
        //logError(message);
        super.logError(message + " " + detail);
    }

    /** This is returned from hookException if throwIfUnhandled is false and there is no error handler or error page static.
     */
    public static final String HOOK_EXCEPTION_NO_ERROR_PAGE = "<html><body>NO ERROR PAGE</body></html>";

    /** Throws the exception if not handled.
     *  @param t may be null */
    public String hookException(String message, Throwable t, int category)
    throws DynamideUncaughtException {
        return hookException(message, t, null, category, true);
    }

    /** @param t may be null */
    public String hookException(String message, Throwable t, ScriptEvent event, int category, boolean throwIfUnhandled)
    throws DynamideUncaughtException {
        //logError("hookException: "+message, t);  logs error dump:  verbose to stdout.
        String resourceName = "";
        if (event != null && event.sender != null){
            Persistent p = (Persistent)event.sender;
            resourceName = p.getFilename();
        }
        String fullID = "";
        if (t instanceof DynamideLoggedException){
        	fullID = ((DynamideLoggedException)t).getErrorID();
        } else {
        	fullID = ResourceManager.writeErrorLog(this, getSessionID(), resourceName, message, t);
        	
        }
        String eLink = "<a href='"+ResourceManager.errorIDToHref(fullID)+"'>"+fullID+"</a>";
        logError("hookException (message omitted) see: "+eLink);
        
        ScriptEventSource appOnErrorSource = getEventSource("application_onError");
        if ( appOnErrorSource.source.length()>0 ) {
            logInfo("FOUND EVENT ERROR SOURCE");
            // input object contains
            //     type of error: page, widget, application
            ErrorHandlerInfo input = new ErrorHandlerInfo(); //input
            input.category = category;
            input.throwable = t;
            input.event = event;
            input.errorLink = eLink;

            // output object contains
            //     what to do: display the result, display the code instead of the widget and continue with page...
            ErrorHandlerResult output = new ErrorHandlerResult(); //output

            //%%%%%%%%%%%%%%%%% output object not used!!!!!!!!!!!!11
            ScriptEvent se = fireEvent(this, input, "application_onError", "",  "", "", appOnErrorSource, getFilename(), false, null, "");
            output = (ErrorHandlerResult)se.getOutputObject();
            if ( se.redirectURL.length()>0 ) {
                sendRedirect(se.redirectURL);
                logInfo("Returning redirect from hookException: "+se.redirectURL);
                return " "; //servlet will redirect
            }
            try {
                if (se.resultAction == ScriptEvent.RA_RETURN_SOURCE){
                    return se.resultSrc;
                }
            } catch (Throwable t2){
                return showErrorPage("Error returning source from error handler.", t, category, throwIfUnhandled);
                //tries user-defined error page, else user-defined error static page, else system-defined error static page.
            }
            if ( output != null && output.action == ErrorHandlerResult.RETURN_EMPTY ) {
                logError("RETURN_EMPTY not implemented");
            }
            String displayMessage = "ERROR (but application_onError did not provide an output displayMessage)";
            if (output != null){
                displayMessage = output.displayMessage;
            }
            logInfo("Returning output.displayMessage from hookException "+displayMessage);
            return showErrorPage(displayMessage, t, category, throwIfUnhandled); //tries user-defined error page, else user-defined error static page, else system-defined error static page.
        }
        return showErrorPage(message, t, category, throwIfUnhandled); //tries user-defined error page, else user-defined error static page, else system-defined error static page.
    }

    /** @bug Includes workaround for Webmacro bug where it trims last character off stream
      */
    public String showErrorPage(String message, Throwable t, int category, boolean throwIfUnhandled)
    throws DynamideUncaughtException {
        if (getTestMode()==false){  //in test mode, don't show error pages, we want errors to bubble up.
            //logDebug("showErrorPage:test mode was false.");
            String pagename = getPropertyStringValue("errorPage");
            //%% todo: try to do Page.outputPage() on errorPage, if any exception just show static page.
            //  but be sure to prevent recursion.

            pagename = getPropertyStringValue("errorPageStatic");
            if ( ! Tools.isBlank(pagename) ) {
                try {
                    String fileContent = getAppFileContent(pagename);
                    //logDebug("******************* errorPageStatic: "+fileContent);
                    if (fileContent != null){
                        //logDebug("showErrorPage: returning errorPageStatic: "+pagename);
                        return fileContent+" ";
                        //extra space at end in case we are inside template expander, which assumes it can trim one space when done (webmacro bug workaround).
                    }
                } catch (FileNotFoundException e){
                } catch (Exception e2)  {
                    return StringTools.escape(message+" throwable: "+t);
                }
            }
        }
        if ( throwIfUnhandled ) {
            DynamideUncaughtException de = new DynamideUncaughtException("<table border='1'><tr><td><font size='-2' color='red'>ERROR: [96] no error handler</font></td></tr>"
                                            +"<tr><td>"+message+"</td></tr>"
                                            +"</table> ",
                                            category,
                                            t);
            String throwablestr = t!=null ? StringTools.truncate(t.toString(), 100, "...") : "";
            logDebug("############# showErrorPage throwing new DynamideUncaughtException: "+throwablestr+"###################");
            throw de;
        } else {
            return HOOK_EXCEPTION_NO_ERROR_PAGE;
        }
    }

    public String hookWidgetError(AbstractWidget source, String widgetID, String message, Throwable t)
    throws Throwable {
        logError("hookWidgetError: "+message, t);
        String fullID = ResourceManager.writeErrorLog(this, getSessionID(), source.getFilename(), "hookWidgetError: "+message, t);

        //event.intputObject = t;
        ScriptEventSource appOnWidgetErrorSource = getEventSource("application_onWidgetError");
        if ( appOnWidgetErrorSource.source.length()>0 ) {
            ScriptEvent event = fireEvent(source, "application_onWidgetError",  "", "", "", appOnWidgetErrorSource, getFilename(), false);
            if ( event.redirectURL.length()>0 ) {
                sendRedirect(event.redirectURL);
                logInfo("Returning redirect from hookException: "+event.redirectURL);
                return " "; //servlet will redirect
            }
            if (event.resultAction == ScriptEvent.RA_RETURN_SOURCE){
                return event.resultSrc;
            }
            if (event.resultAction == ScriptEvent.RA_THROW_EXCEPTION){
                if ( event.getOutputObject() != null ) {
                    throw (Throwable)(event.getOutputObject());
                } else if (t != null){
                    throw t;
                }
            }
        }
        return "\r\n<!-- Dynamide ERROR in "+widgetID+"-->\r\n ";  //default is silent.
    }

    private StringBuffer m_consoleLog = new StringBuffer();

    public void print(String msg){
        if (m_stdoutEcho){
            System.out.print(msg);
        }
        m_consoleLog.append(msg);
    }

    public void println(String msg){
        if (m_stdoutEcho){
            System.out.println(msg);
        }
        m_consoleLog.append(msg);
        m_consoleLog.append(Tools.NEW_LINE);
    }

    public String getConsoleLog(){
        return m_consoleLog.toString();
    }

    public static final StringTools getStringTools(){
        return new StringTools();
    }

    public static final Tools getTools(){
        return new Tools();
    }
    public static final ServletTools getServletTools(){
        return new ServletTools();
    }

    /** Use this to load any class with a null contructor (no arguments) from a scripting context.
     */
    public static final Object loadClass(String className)     //%% might want to lock this down a bit.  Now you can create any class from webmacro. :(
    throws Exception {                                         // %% maybe by role of user.
        return Class.forName(className).newInstance();
    }

    /** If you have logic or other complications that cannot be handled in WebMacro or JSP or whatever
     *  template engine you have plugged in, you can call this method to get a custom bean loaded from your template.
     *  @return a valid class (must have a no-arg constructor) if found by the classloader, else return null.
     */
    public Object loadWidetBean(String className){
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e){
            return null;
        }
    }

    //%% todo: make sure user is on ACL:
    /** Evaluate any string in the pluggable Interpreter, the only variable in the context already
     *  is a pointer to this session, in a variable called "session".  However, the session variable
     *  gives you the keys to the kingdom.  See also DynamideObject.expand(String) which is used to
     *  expand expressions in the Webmacro interpreter.
     */
    public Object eval(String source)
    throws Exception {
        IInterpreter interp = getInterpreter();
        try {
            return interp.eval(source);
        } catch( Exception e ){
            try {
                Object res = interp.eval("com.dynamide.util.Tools.arrayToString(this.namespace.getMethodNames());");
                String info = res != null ? res.toString() : "null";
                logError("[102] Session.eval. info: "+info, e);
            } catch (Exception e2)  {
                logError("[102.1] Session.eval", e);
            }
            //return "ERROR: [102] [Session.eval] "+e;
            throw e;
        }
    }


    //%% todo: make sure user is on ACL:
    /** eval the source, and return a valid html chunk. Even if there is an error, you'll get formatted
     *  html, with the offending line highlighted.
     */
    public static String handleEval(String SessionID, String source, String USERID, HttpServletRequest request){
        String result = "";
        Object obj = ResourceManager.getRootResourceManager().getSession(SessionID);
        if ( obj != null && obj instanceof Session ) {
            Session session = ((Session)obj);
            IInterpreter interp = session.getInterpreter();

            String discard = interp.emptyOutputBuffer();
            Log.debug(Session.class, "DISCARDING Interpreter console buffer: "+discard);
            try{
                interp.setVariable("session", session);
                interp.setVariable("request", request);
                interp.setVariable("SESSIONID", SessionID);
                try {
                    Object res = interp.eval(source);
                    if ( res == null ) {
                        //6/4/2003 4:38AM res = "NO RESULT";
                    }
                    result = "<hr /><font size='+2'><b>OUTPUT</b></font><hr />\r\n<pre>"
                            +interp.emptyOutputBuffer()
                            +"</pre><hr /><font size='+2'><b>RESULT</b></font><hr />\r\n<pre>"+res
                            +"</pre>";
                } finally {
                    interp.unsetVariable("session");
                    interp.unsetVariable("request");
                    interp.unsetVariable("SESSIONID");
                }
            } catch (bsh.EvalError e){
                result = com.dynamide.interpreters.BshInterpreter.formatError(e, true, source, null, "", "");
            } catch (Exception e2){
                result = "ERROR: [25] "+e2;
            }

        } else {
            result = "ERROR: [24] SESSIONID is invalid";
        }
        return result;
    }


    public void setWidgetError(String widgetDotName, String message){
        Object o = findDotted(widgetDotName);
        if ( o!=null && o instanceof Widget) {
            Widget w  = (Widget)o;
            w.setErrorMessage(message);
        } else {
            logError("Widget not found in Session.setWidgetError: "+widgetDotName);
        }
    }

    private StringList m_valueBeans = new StringList();

    public void addValueBean(String name, IValueBean bean){
        m_valueBeans.addObject(name, bean);
    }

    public IValueBean getValueBean(String name){
        return (IValueBean)m_valueBeans.getObject(name);
    }

    public ValueBeanHelper getValueBeanHelper(String name){
        return new ValueBeanHelper(this, (IValueBean)m_valueBeans.getObject(name));
    }

    /** This overload does not pass in an inputObject.
     */
     protected ScriptEvent fireEvent(Object sender,
                                     String procName,
                                     String currentPageID,
                                     String nextPageID,
                                     String action,
                                     ScriptEventSource eventSource,
                                     String eventSourceFilename,
                                     boolean sourceOnly){
        return fireEvent(sender, null, procName, currentPageID, nextPageID, action, eventSource, eventSourceFilename, sourceOnly, null, "");
     }

    /**
     *  @param inputObject is any object the caller would like to pass in as the ScriptEvent.inputObject
     *  to the event.  If the event calls event.setOutputObject() or sets event.outputObject, then ScriptEvent.outputObject
     *  is available.  This is the return value used in Session.call().  Otherwise all events return nothing, since they
     *   have a signature with void return type.
     */
     protected ScriptEvent fireEvent(Object sender,
                                     Object inputObject,
                                     String procName,
                                     String currentPageID,
                                     String nextPageID,
                                     String action,
                                     ScriptEventSource eventSource,
                                     String eventSourceFilename,
                                     boolean sourceOnly,
                                     IInterpreter detachedInterp,
                                     String dmFormat) {
        if (dmFormat.length()==0) dmFormat = getQueryParam(Constants.dmFormatParamName);
        ScriptEvent event = new ScriptEvent();
        event.session = this;

        String sSource = eventSource.source;
        String language = eventSource.language;

        if ( sSource.length()==0 ) {
            //logWarn("LOG_HANDLER_PROC_MISSING_EVENTS "+LOG_HANDLER_PROC_MISSING_EVENTS);
            if (LOG_HANDLER_PROC_MISSING_EVENTS) logHandlerProc("fireEvent", procName+"  [no event source]");   //or use INFO
            event.resultCode = ScriptEvent.RC_NO_EVENT_SOURCE;
            if ( dmFormat.equalsIgnoreCase(Constants.dmFormatXML) ) {
                event.evalErrorMsg = Constants.XMLERROR_START + "No Event Source for "+procName+Constants.XMLERROR_END;
            } else {
                String body = "<html><body>Event source not found\r\n"
                            + "<br />procName: "+procName
                            + "<br />eventSourceFilename: "+eventSourceFilename +"</body></html>";
                event.evalErrorMsg = body;
            }
            return event;
        }
        
        String senderID = "";
        if ( sender instanceof DynamideObject ) {
            senderID = "::"+((DynamideObject)sender).getID();
        }
        String dressedProcName ;
        if ( (!procName.startsWith("application")) && (!procName.startsWith(eventSource.ownerID)) ) {
            dressedProcName = "["+eventSource.ownerID+"]"+procName;
        } else {
            dressedProcName = procName;
        }
        String handlerprocInfo = "<b>"+dressedProcName+"</b>  [sender:"+sender.getClass().getName()+senderID+"]";
        enterHandlerProc("fireEvent", handlerprocInfo);
        IComposite expansionLogNode = null;
        ExpansionLog eLog = getExpansionLog();
        if (eLog!=null) {
            expansionLogNode
                = eLog.enterExpansion(this, dressedProcName, "eventSource", "text/html", ExpansionLog.formatEscapePre(eventSource.source), "Session.fireEvent");
        }
        boolean expansionError = false;
        try {
            event.sender = sender;
            event.inputObject = inputObject;
            event.request = getRequest();
            event.response = getResponse();
            event.resultAction = ScriptEvent.RA_DEFAULT;
            event.resultCode = ScriptEvent.RC_OK;
            event.action = action;
            event.nextPageID = nextPageID;
            event.eventName = procName;

            if (sender instanceof Page){
                event.currentPage = (Page)sender;
            }
            if (currentPageID.length()>0){
                event.currentPageID = currentPageID;
                if ( event.currentPage == null ) {
                    event.currentPage = findPageByID(currentPageID);
                }
            } else {
                if ( event.currentPage != null ) {
                    event.currentPageID = event.currentPage.getID();
                }
            }

            if (detachedInterp != null){
                event = detachedInterp.fireEvent(event, procName, eventSource, sourceOnly);
            } else {
                event = callInterpreter(event, procName, eventSource, language, sourceOnly);
            }
            if (event.resultCode == ScriptEvent.RC_ERROR){
                boolean holdEcho = false;
                if ( dmFormat.equalsIgnoreCase("XML") ) {
                    String escapedMsg = StringTools.escape(event.evalErrorMsg); //need extra escape since we won't pass through formatTemplatePage
                    holdEcho = getStdoutEcho();
                    setStdoutEcho(false);
                    event.println(escapedMsg);
                    setStdoutEcho(holdEcho);
                    event.evalErrorMsg = Constants.XMLERROR_START + escapedMsg +Constants.XMLERROR_END;
                    if ( eLog != null ) {
                        eLog.logExpansionError(expansionLogNode, "error", "text/html", escapedMsg, "Session.expandTemplate");
                        expansionError = true;
                        throw new DynamideLoggedException(getSession(),
                                                          "ERROR in expanding "+dressedProcName+" got ScriptEvent.RC_ERROR at "+event.errorLineNumber,
                                                          eLog.printExpansionLog(expansionLogNode));
                    }
                } else {
                    eventSourceFilename = StringTools.isEmpty(eventSourceFilename)
                                          ? eventSource.resourceName
                                          : eventSourceFilename;
                    String body = "<html><body><h3>An error occurred in an event.</h3>"
                        +"<hr />\r\nEvent Filename: "+eventSourceFilename
                        +"<hr />\r\n<pre style='white-space: pre-wrap;'>"+event.evalErrorMsg
                        +"</pre><hr />\r\nEvent Source: \r\n<pre>"
                        +StringTools.makeLineNumbers(StringTools.escape(sSource), event.errorLineNumber)
                        +"</pre>"
                        +"\r\n<hr /><small>(Hint: check the lines preceding the error for missing semicolons, etc.)</small></body></html>";
                    event.evalErrorMsg = body;
                    expansionError = true;
                    if (eLog!=null) {
                        eLog.logExpansionError(expansionLogNode, "error", "text/html", body, "Session.expandTemplate");
                        throw new DynamideLoggedException(getSession(),
                                                          "ERROR in expanding "+dressedProcName+" got ScriptEvent.RC_ERROR at "+event.errorLineNumber,
                                                          eLog.printExpansionLog(expansionLogNode));
                    } else {
                        throw new DynamideUncaughtException("ERROR in expanding "+dressedProcName+" got ScriptEvent.RC_ERROR at "+event.errorLineNumber);
                    }
                }
            }
        } finally {
            if ( ! expansionError ) {
                if (eLog!=null) {
                    eLog.leaveExpansion(expansionLogNode, "result", "text/html", event.dumpHTML(), "Session.expandTemplate");
                }
            }
            leaveHandlerProc("fireEvent", handlerprocInfo);
            if ( event != null ) {
                event.setRequest(null);
                event.setResponse(null);
            }
        }
        return event;
    }

    public IInterpreter getInterpreter(){
        //return getInterpreter("Tcl");
        return getInterpreter("beanshell");
    }

    public IInterpreter getInterpreter(String language){
        String classname = InterpreterTools.mapInterpreter(language);
        IInterpreter interp = (IInterpreter)m_Interpreters.get(classname);
        if (interp == null){
            try {
                Object o = Class.forName(classname).newInstance();
                interp = (IInterpreter)o;
            } catch (Exception e){
                logError("Couldn't instantiate interpreter. language: '"+language+"' classname: '"+classname+"'", e);
                return null;
            }
            try {
                interp.setSession(this);
                m_Interpreters.put(classname, interp);
            } catch (Exception e)  {
               interp.setSession(null);
               if (m_Interpreters != null) m_Interpreters.remove(classname);
               return null;
            }
        }
        return interp;
    }

    public IInterpreter getDetachedInterpreter(String language){
        String classname = InterpreterTools.mapInterpreter(language);
            IInterpreter interp = null;
            try {
                Object o = Class.forName(classname).newInstance();
                interp = (IInterpreter)o;
            } catch (Exception e){
                logError("Couldn't instantiate interpreter. language: '"+language+"' classname: '"+classname+"'", e);
                return null;
            }
            if ( interp == null ) {
                return null;
            } else {
                interp.setSession(this);
                ScriptEventSource application_onImport = getEventSourceBody("application_onImport");
                if ( application_onImport.source.length()>0 ) {
                    ScriptEvent event = fireEvent(this, new Object(), "application_onImport", "", "", "", application_onImport, getFilename(),  true, interp, "");
                    if ( event.resultCode != ScriptEvent.RC_OK ) {
                        logError("ERROR: [39a1] see event.evalErrorMsg");
                        return null;
                    }
                }
                return interp;
            }
    }

    public ScriptEvent callInterpreter(ScriptEvent event, String procName, ScriptEventSource eventSource, String language, boolean sourceOnly){
        IInterpreter interp = getInterpreter(language);
        if (interp != null){
            return interp.fireEvent(event, procName, eventSource, sourceOnly);
        } else {
            event.evalErrorMsg = "Event language not supported: "+language;
            event.resultCode = ScriptEvent.RC_ERROR;
            return event;
        }
     }

    public ScriptEvent fireWidgetMethod(Widget widget, String procName) {
        WidgetType widgetType = findWidgetType(widget.getPropertyStringValue("type"));
        ScriptEventSource eventSource = widgetType.getEventSource(procName);
        return fireEvent(widget, procName, ""/*pageID*/,  ""/*nextPageID*/,""/*action*/, eventSource, widget.getFilename(), false);
    }

    public void fireWidgetImports(Widget widget,
                                    String procName){
        WidgetType widgetType = findWidgetType(widget.getPropertyStringValue("type"));
        ScriptEventSource widget_onImport = widgetType.getEventSourceBody(procName);
        if ( widget_onImport.source.length()>0 ) {
            ScriptEvent event = fireEvent(widget, procName, "", "", "", widget_onImport, widget.getFilename(), true);
            //logInfo("widget_onImport fired for "+widget.getID());
        } else {
            logInfo("No widget_onImport defined, but fireWidgetImports('"+widget.getID()+", "+procName+"') was called.");
        }
    }

    // %%% todo: handle session.onPool, session.onAttach

    public ScriptEvent fireApplicationEvent(String eventName){
        return fireApplicationEvent(eventName, "");
    }

    public ScriptEvent fireApplicationEvent(String eventName, String action){
        ScriptEventSource appActionSource = getEventSource(eventName);
        return fireEvent(this, eventName, "", "", action, appActionSource, getFilename(), false);
    }

    public ScriptEvent fireApplicationEvent(String eventName, String action, Object inputObject){
        ScriptEventSource appActionSource = getEventSource(eventName);
        return fireEvent(this, inputObject, eventName, "", "", action, appActionSource, getFilename(), false, null, "");
    }

    public ScriptEvent debugApplicationEvent(String inputObjectEval,
                                            String eventName,
                                            String currentPageID,
                                            String nextPageID,
                                            String action,
                                            String sourceDotName)
    throws Exception {
        ScriptEventSource eventSource = getEventSource(eventName);
        Object inputObject = eval(inputObjectEval);
        Object source;
        if (sourceDotName.length()==0){
            source = this;
        } else {
            source = findDotted(sourceDotName);
        }
        if (inputObject != null && inputObject.toString().startsWith("ERROR")){
            throw new DynamideException(""+inputObject);
        }
        return fireEvent(source, inputObject, eventName, currentPageID, nextPageID, action, eventSource, getFilename(), false, null, "");
    }

    public ScriptEvent debugPageEvent(String sourcePageID,
                                      String inputObjectEval,
                                      String eventName,
                                      String currentPageID,
                                      String nextPageID,
                                      String action,
                                      String sourceDotName)
    throws Exception {
        Page page = findPageByID(sourcePageID);
        if (page == null){
            throw new DynamideException("Couldn't find page in debugPageEvent: "+sourcePageID);
        }
        ScriptEventSource eventSource = page.getEventSource(eventName);
        Object inputObject = eval(inputObjectEval);
        Object source;
        if (sourceDotName.length()==0){
            source = page;
        } else {
            source = findDotted(sourceDotName);
        }
        if (inputObject != null && inputObject.toString().startsWith("ERROR")){
            throw new DynamideException(""+inputObject);
        }
        return fireEvent(source, inputObject, eventName, currentPageID, nextPageID, action, eventSource, page.getFilename(), false, null, "");
    }

    public ScriptEvent auditEvent(String senderID, String eventName)
    throws DynamideException {
        Object sender = findDotted(senderID); //will find the magic "application" id, or a page by id.
        ScriptEventSource eventSource;
        if (sender instanceof Session){
            eventSource = ((Session)sender).getEventSource(eventName);
        } else if (sender instanceof Page){
            eventSource = ((Page)sender).getEventSource(eventName);
        } else if (sender instanceof Widget){
            eventSource = ((Widget)sender).getWidgetType().getEventSource(eventName);
        } else {
            throw new DynamideException("[auditEvent] sender not of type Session, Page or Widget"+senderID);
        }
        //By this point, sender is of type Persistent.
        return fireEvent(sender, null, eventName, "", "", "", eventSource, ((Persistent)sender).getFilename(), true, null, ""/*Constants.dmFormatXML*/);
    }

    public ScriptEvent fireJobEvent(Job sender, String eventName, Object inputObject, boolean useDetachedInterpreter){
        ScriptEventSource appActionSource = getEventSource(eventName);
        IInterpreter interp;
        if (useDetachedInterpreter){
            interp = getDetachedInterpreter(appActionSource.language);
        } else {
            interp = getInterpreter(appActionSource.language);  //will be catched in the Session.
        }
        return fireEvent(sender, inputObject, eventName, "", "", ""/*action*/, appActionSource, getFilename(), false, interp, "");
    }

    public Object call(String eventFullName){
        return fireApplicationEvent(eventFullName, "").getOutputObject();
    }

    public Object call(String eventFullName, Object inputObject){
        return fireApplicationEvent(eventFullName, "", inputObject).getOutputObject();
    }



    /** This method is geared for the web IDE that uses the return string; to work 
    with objects, first get a ref to the Page object, then call Page.createWidget,
    with will return the Widget object.
    @see Page#createWidget */
    public String createWidget(String widgetType, String targetPageID)
    throws Exception {
        String body = "ERROR";
        try {
            Page thepage = getPageByID(targetPageID);
            if (thepage != null){
                Widget wd = thepage.createWidget(widgetType);
                body = wd.getID();//widgetName;
            }  else {
                body = "ERROR: [98] [createWidget] page is null: "+targetPageID;
            }
        } catch (Exception e){
            body = "ERROR: [29] "+Tools.errorToString(e, true);
        }
        return body;
    }

    public String renderWidget(String widgetName, String targetPageID, HttpServletRequest request){
        String body = "ERROR";
        try {
            Page thepage = getPageByID(targetPageID);
            if (thepage != null){
                Widget wd = thepage.getWidgetByID(widgetName);
                if ( wd != null ) {
                    body = renderWidget(wd, targetPageID, request);
                } else {
                    body = "ERROR: [99] [renderWidget] widget is null: '"+widgetName+'\'';
                }
            } else {
                body = "ERROR: [100] [renderWidget] page is null: '"+targetPageID+'\'';
            }
        } catch (Exception e){
            return "ERROR: [28] "+Tools.errorToString(e, true);
        }
        return body;

    }

    public String renderWidget(Widget wd, String targetPageID, HttpServletRequest request){
        try {
            String body = "";
            Page targetPage = getPageByID(targetPageID);
            Page prevPagePointer = wd.getPage();
            try {
                if (targetPageID != null){
                    wd.setPage(targetPage);
                }
                body = wd.render();
                body = com.dynamide.util.StringTools.searchAndReplaceAll(body, "&nbsp;", "&amp;nbsp;");
            } finally {
                wd.setPage(prevPagePointer);  //can be null.
            }
            return body;
        } catch (Exception e){
            return "ERROR: [27] "+e.toString();
        }
    }

    public Page getPageByFile(File f)
    throws Exception {
        String p = f.getName();
        String base = p.substring(0,p.length()-".xml".length());
        return getPageByID(base);
    }

    /** silent about exceptions, returns null if not found or error - if you want to see the exceptions, call getPageByID instead.
     */
    public Page findPageByID(String pageID){
        return findPageByID(pageID, null);
    }

    public Page findPageByID(String pageID, DynamideObject newOwner){
        try {
            if (m_pageNames.indexOf(pageID) > -1){  //was found in application.xml or added since with newPage().
                return loadPage(pageID, false, newOwner);
            }
            try {
                return (Page)m_loadedPages.get(pageID); // returns CASE-INSENSITIVE or null
                //int indx =  m_loadedPages.indexOf(pageID);
                //if ( indx > -1 ) {
                //    return (Page)m_loadedPages.getObjectAt(indx);  //was found in a dynamically loaded page, not part of the project proper.
                //}
            } catch (Exception e){
               //couldn't load page, oh, well.
            }
        } catch (Exception e){   // throws IOException, JDOMException, PageLoadException, DatatypeException
            return null;
        }
        return null; //not found in application, loaded or not.
    }

    /** Finds and loads a Page, given the pageID, which is relative to your project.  This method is
     *  only for files in a Web Application's project, that is, not for system dialogs or dialogs in libraries.
     *  For those, use findOrCreateDialog(...) so that you can specify the local id.   If you wish to find if a page is
     *  in a project or is loadable from the project without catching the PageLoadException, call findPageByID(...).
     */
    public Page getPageByID(String pageID)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        return loadPage(pageID, false, null);
    }

    /** Finds and loads a Page, given the pageID, which is relative to your project.  This method is
     *  only for files in a Web Application's project, that is, not for system dialogs or dialogs in libraries.
     *  For those, use findOrCreateDialog(...) so that you can specify the local id.   If you wish to find if a page is
     *  in a project or is loadable from the project without catching the PageLoadException, call findPageByID(...).
     */
    public Page getPage(String pageID)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        return loadPage(pageID, false, null);
    }


    /** @param appRelativeName is the name of the resource relative to the application or library,
     *  e.g. findOrCreateDialog("myDialog", "resources/dialogs/com.dynamide.imgview");  Note that appRelativeName
     *  contains neither the file extension (.xml) nor the full path, but it does contain the
     *  "resources/" part of the path.
     */
    public Page findOrCreateDialog(String pageID, String appRelativeName)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        return loadPage(pageID, appRelativeName+".xml", false, false, null);
    }

    public void destroyDialog(String pageID){
        unloadPage(pageID);
    }

    public String createDialogURL(String pageID, String appRelativeName, String params)
    throws Exception {
        //"myImgView", "resources/widgets/dialogs/com.dynamide.imgview", "img=resources/images/eventFlowChart.gif&imageWidth=&imgHeight=&title=");
        Page dialog = findOrCreateDialog(pageID, appRelativeName);
        return getAppURL("next="+pageID+'&'+params);
    }

    public void loadAllPages(){
        Iterator pages = m_pageNames.iterator();
        while(pages.hasNext()){
            String pageID = (String)pages.next();
            try {
                loadPage(pageID);
            } catch (Throwable t)  {
                logError("Couldn't load page in Session.loadAllPages: "+pageID, t);
            }
        }
    }

    public Page loadPage(String pageID)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        return loadPage(pageID, false, null);
    }

    public Page loadPage(String pageID, boolean forceReload, DynamideObject newOwner)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        return loadPage(pageID, pageID+".xml", forceReload, true, newOwner);
    }

    private Page loadPage(String pageID, String appRelFilename, boolean forceReload, boolean isProjectFile, DynamideObject newOwner)
    throws IOException, JDOMException, PageLoadException, DatatypeException {
        if (newOwner == null){
            newOwner = this;
        }
        Page page = (Page)m_loadedPages.get(pageID);
        if ( forceReload && page != null ) { // forceReload wins over caching preference.
            unloadPage(pageID);
            page = null;
        }
        boolean justCreated = false;
        if ( page == null && pageID.length()>0 ) {
            if ( isProjectFile && m_pageNames.indexOf(pageID) == -1 ) {
                String msg = "Page '"+pageID+"' is not part of project.  Please add to application.xml: "+getFilename();
                logHandlerProc("WARNING", msg);
                println(msg);
                logWarn(msg);
            }
            page = new Page(newOwner, pageID, getAppFilename(appRelFilename), this);
            // only need this in the constructor and when calling newPage. m_pageNames.add(pageID);
            m_loadedPages.put(pageID, page);
            justCreated = true;
            page.load();  //have to do this *after* adding to the m_loadedPages list.
        }
        if ( ! cacheXml && ! justCreated) {
            if (page != null) page.reload();
        }
        return page;
    }

    public void unloadPage(String pageID){
        Page page = (Page)m_loadedPages.get(pageID);
        if ( page != null ) {
            logDebug("removing loaded page: "+pageID);
            m_loadedPages.remove(pageID);
            page.cleanup(); //will also call Widget.cleanup() on all contained widgets.
        }
    }

    public void pageVisited(String pageID){
        if (pageID.length()>0){
            m_visitedPages.add(pageID);
        }
    }

    public Page newPage(String pageID, String pagetype) throws Exception {
        String normalpage = getAppFilename(FileTools.joinURI(Constants.widgetsRelDir, pagetype+".xml")); //3/12/2003 9:37PM was: getAppFilename("resources/widgets/pagetypes/"+pagetype+".xml");
        String pageFilename = findAppFilename(pageID+".xml");
        if ( pageFilename == null || pageFilename.length()==0 ) {
            pageFilename = (new File(getFilename())).getParentFile().getCanonicalPath()+File.separator+pageID+".xml";
        } else {
            if ( (new File(pageFilename)).exists() ) {
                throw new Exception("ERROR: [101] [Session.newPage] Filename is in use: "+pageFilename);
            }
        }
        //FileTools.copyFile(normalpage, pageFilename);
        Page page = new Page(this, pageID, normalpage, this);
        page.setFilename(pageFilename);
        logDebug("page-props-before-load: "+page.dumpProperties());
        page.load();//12/27/2003 2:16AM experimental --Widgets weren't being loaded.
        logDebug("page-props-after-load:  "+page.dumpProperties());
        page.saveToFile();
        logDebug("page-props-after-save:  "+page.dumpProperties());
        getAssembly().registerApplicationResource(getAppname(), pageID+".xml");
        m_pageNames.add(pageID);
        m_loadedPages.put(pageID, page);
        page.load();
        return page;
    }


    /** take posted query fields and map them to Field fields, and return a new action
     * if one of the submit buttons overrides or sets it. */
    private String putAllFieldsIntoSession(HttpServletRequest request, String action){
        boolean debug = false; //true;
        Field aField;
        String value;
        Enumeration en;
        if (request != null){
            en = request.getParameterNames();
        } else {
            en = m_debugURLDecoder.getFieldNames();
        }
        while (en.hasMoreElements()){
            String fieldName = (String)en.nextElement();
            if (debug) log("putAllFieldsIntoSession: fieldName:"+fieldName);
            value = getQueryParam(fieldName);
            String theAction = (String)m_submitActions.get(fieldName);
            if (theAction != null){
               action = theAction;  //modify the return value, overriding the action= found on the URL
                if ( action.length()>0 ){
                    logHandlerProc("INFO", "Action mapped from submitActions: <b>"+action+"</b>");
                    if (debug) log("putAllFieldsIntoSession: action:"+action);
                }
            } else if (fieldName.startsWith("checkbox_") && value.equals("checkbox:verify")) {
                //special checkbox hack, for both browsers:  (fortunately, radiobuttons behave already).
                //the com.dynamide.checkbox widget includes  a special hidden field.
                String cbFieldName = fieldName.substring("checkbox_".length());
                String cbValue = getQueryParam(cbFieldName);
                if ( cbValue.length()==0 ) {
                    //no "on" has been sent for the real checkbox, which means the cb is off,
                    //  so the damn browser skips it.  In this case the checkbox is unchecked, which means "off".
                    aField = findField(cbFieldName);
                    if ( null != aField ) {
                        aField.set("value", "false");
                        if (debug) log("putAllFieldsIntoSession: checkbox value: false");
                    }
                } else {
                    boolean isChecked = Tools.isTrue(cbValue.trim());
                    aField = findField(cbFieldName);
                    if ( null != aField ) {
                        aField.set("value", ""+isChecked);
                        if (debug) log("putAllFieldsIntoSession: checkbox value: "+isChecked);
                    }
                }
            } else {  //normal input control.
                if ( null != (aField = findField(fieldName)) ) {
                    aField.set("value", value);
                    if (debug) log("putAllFieldsIntoSession: field: "+fieldName+" value: "+value+" aField.value: "+aField.getValue());
                    //%% todo: make sure field value handling is correct.  See todo notes. log("value:"+value+" set in field: "+aField.getID()); //dump());//aField.getObjectID());
                } else {
                    if (debug) log("putAllFieldsIntoSession: can't find field: '"+fieldName+"' \r\n...Fields...\r\n"+listFields());
                }
            }
        }
        return action;
    }

    private void loadFields()
    throws Exception {
        try {
            String fielddefsDefaultFilename = findAppFilename("fielddefs.xml");
            String fielddefsFilename = "";
            Element fieldsElement = this.getRootElement().getChild("fields");
            if ( fieldsElement != null ) {
                String filename = fieldsElement.getAttributeValue("href");
                if ( filename != null) {
                    List fieldList = fieldsElement.getChildren("field");
                    if ( fieldList != null && fieldList.size() != 0 ) {
                        String msg = ("fields element in application.xml had both an external filename attribute and \"field\" elements.  \"field\" elements will be ignored.");
                        logWarn(msg);
                        if (getTestMode()){
                            throw new Exception(msg);
                        }
                    }
                    fielddefsFilename = getAppFilename(filename);
                    if ( fielddefsFilename == null ) {
                        String msg = ("filename specified in application.xml::fields::filename was null: "+filename);
                        logWarn(msg);
                        if (getTestMode()){
                            throw new Exception(msg);
                        }
                    } else {
                        if (!readFieldsIntoSessionFromFile(fielddefsFilename)){
                            String msg = "Error reading external file for fields, specified in application.xml::fields element: '"+fielddefsFilename+"'";
                            logWarn(msg);
                            if (getTestMode()){
                                throw new Exception(msg);
                            }
                        }
                    }
                } else {
                    readFieldsIntoSession(fieldsElement);
                }
                if (fielddefsDefaultFilename != null && fielddefsDefaultFilename != null && ! fielddefsDefaultFilename.equals(fielddefsFilename)){
                    String msg = ("fielddefs.xml file will be ignored, since application.xml specified a 'fields' element.");
                    logWarn(msg);
                    if (getTestMode()){
                        throw new Exception(msg);
                    }
                }
            } else {
                if (fielddefsDefaultFilename != null){
                    //logDebug("case fielddefsDefaultFilename!=null: '"+fielddefsDefaultFilename+"'");
                    if (!readFieldsIntoSessionFromFile(fielddefsDefaultFilename)){
                        String msg = ("Default external file for fields does not exist (fielddefs.xml in application directory");
                        logWarn(msg);
                        if (getTestMode()){
                            throw new Exception(msg);
                        }
                    }
                }
            }
        } catch (Exception e)  {
            String msg = ("loadFields caught exception"+e);
            logError(msg);
            if (getTestMode()){
                throw new Exception(msg, e);
            }
        }
    }

    private boolean readFieldsIntoSessionFromFile(String filename)
    throws Exception {
        //LEAK-TEST %%  15624 for 1000
        if ( FileTools.fileExists(filename)) {
            JDOMFile fielddefsFile = new JDOMFile(null, filename);
            //LEAK-TEST %%  16316 for 1000
            Element root = fielddefsFile.getDocument().getRootElement();
            //LEAK-TEST %%  16360 for 1000
            readFieldsIntoSession(root);
            fielddefsFile = null;
            return true;
        } else {
            String msg = "readFieldsIntoSessionFromFile(\""+filename+"\") could not read file";
            logWarn(msg);
            if (getTestMode()){
                throw new Exception(msg);
            }
            return false;
        }
    }

    /** read the fields from the fielddefs file. */
    public void readFieldsIntoSession(Element root)  throws JDOMException, DatatypeException  {
        //LEAK-TEST %%   return here: 16330 for 1000
        List fields = root.getChildren("field");
        Iterator i = fields.iterator();
        while (i.hasNext()) {
            Element fieldElement = (Element) i.next();
            Field field = new Field(this, this, fieldElement, getDotName()+"{"+getFilename()+"}");
            addField(field.get("name").toString(), field);
        }
    }

    public String handleLayoutViewChanged(HttpServletRequest request){
        return "You sent: "+getQueryParam("htmlSource");
    }

    public String renderWidgetHelp(String widgetType){
        String result = "";
        if (widgetType == null || widgetType.length() == 0){
            return result;
        }
        try {
            String fn = getAppFilename(Constants.widgetsRelDir+'/'+widgetType+".xml");
            JDOMFile jdf = new JDOMFile(this, Tools.fixFilename(fn)); //%% todo: search all palette dirs, not just the system one.
            Element componentHelp = jdf.findFirstElement("componentHelp");
            if ( componentHelp == null ) {
                result = "";
            } else {
                result = JDOMFile.output(componentHelp);
            }
        } catch (Exception e){
            result = "ERROR getting widget componentHelp: "+e;
        }
        return result;
    }

    //================= Field Handling ==========================================================

    public Map getFields(){
        return (Map)((TreeMap)m_fields).clone(); //performs a shallow clone.
    }

    /** Note: returns an empty string if field is not found or Field value object is null;
     *  see getFieldObject to get direct access to the object or null.
     */
    public Object getFieldValue(String fieldName){
        Object val = getFieldObject(fieldName);
        return val == null ? "" : val;
    }

    /** Note: returns an empty string if field is not found or Field value object is null;
     *  see getFieldObject to get direct access to the object or null.
     */
    public String getFieldStringValue(String fieldName){
        return getFieldValue(fieldName).toString();
    }

    /** Note: returns null if the field has no "value" object, or if the object is null;
     *  use getFieldValue or getFieldStringValue if you want a null-safe method.
     */
    public Object getFieldObject(String fieldName){
        ///fieldName = fieldName.toUpperCase(); //CASE-INSENSITIVE
        String look = lookupInternationalizedValue(fieldName);  //%% todo: make CASE-INSENSITIVE
        if ( look != null && look.length() > 0 ) {
            return look;
        }
        Object val = null;
        Field field = (Field)m_fields.get(fieldName);
        if ( field != null ) {
            val = field.get("value");
        }
        return val;        //10/2/2003 5:27PM    return val == null ? "" : val;
    }

    //in IDatasource:
    public Field getField(String fieldName){
        ///fieldName = fieldName.toUpperCase(); //CASE-INSENSITIVE
        return (Field)m_fields.get(fieldName);
    }

    //in IDatasource:
    public Field getField(String fieldName, String fieldIndex) {
        return getField(fieldName);
    }

    //in IDatasource:
    public void clear() {
        Iterator it = m_fields.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry en = (Map.Entry)it.next();
            String key = (String)en.getKey();
            Field field = (Field)en.getValue();
            field.clear();//6/24/2004 clearError();
        }
    }

    public String dumpErrorsHTML(){
        StringBuffer b = new StringBuffer();
        Iterator it = m_fields.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry en = (Map.Entry)it.next();
            Field field = (Field)en.getValue();
            if ( field.getError() ) {
                b.append(field.dumpHTML());
            }
        }
        return b.toString();
    }

    public void setFieldValue(String fieldName, Object value)
    throws DatatypeException {
        Field aField = findField(fieldName);
        if (aField != null){
            aField.set("value", value);
        } else {
            aField = addField(fieldName, new Field(this, this, fieldName, value));
        }
    }

    /** Permissively sets the field value, ignoring the fieldIndex
     */
    public boolean setFieldValue(String fieldName, Object value, String fieldIndex){
        try {
            setFieldValue(fieldName, value);
        } catch (Exception e)  {
            logError("couldn't setFieldValue by index", e);
            return false;
        }
        return true;
    }

    public void setFieldObject(String fieldName, Object value) throws DatatypeException {
        Field aField = findField(fieldName);
        if (aField != null){
            aField.set("value", value);
        } else {
            addField(fieldName, new Field(this, this, fieldName, value));
        }
    }

    /** You can use this to rebind a Field, or add a new one.  Calls addField(field.get("name"), field, false).
     */
    public Field setField(Field field){
        return addField(field.getName(), field, false);
    }

    /** You can use this to rebind a Field, or add a new one.  Calls addField(fieldName, field, false).
     */
    public Field setField(String fieldName, Field field){
        return addField(fieldName, field, false);
    }

    public Field addField(Field field){
        return addField(field.getName(), field, true);
    }

    public Field addField(String fieldName, Field field){
        return addField(fieldName, field, true);
    }

    public Field addField(String fieldName, Field field, boolean logWarnIfExists){
        ///fieldName = fieldName.toUpperCase();//CASE-INSENSITIVE
        if (m_fields.get(fieldName) != null){
            if (logWarnIfExists) logWarn("WARNING: addField(...) removing field "+fieldName+" with value: "
                                +((Field)m_fields.get(fieldName)).get("value")
                                +" and replacing with value: "+field.get("value"));
            m_fields.remove(fieldName);
        }
        m_fields.put(fieldName, field);
        return field;
    }

    private List m_autoFieldIDs = new ArrayList();
    public List getAutoFieldIDs(){return m_autoFieldIDs;}
    public void setAutoFieldIDs(List new_value){m_autoFieldIDs = new_value;}

    public void logAutoFieldID(String id){
        m_autoFieldIDs.add(id);
    }

    public Field createField(String fieldName, String value)
    throws DynamideException {
        return createField(fieldName, value, false);
    }

    public Field createField(String fieldName, String value, boolean override)
    throws DynamideException {
        Field aField = findField(fieldName);
        if (aField != null){
            if (override){
                removeField(fieldName);
            } else {
                throw new DynamideException("ERROR: [97] [createField] Field already exists: "+fieldName);
            }
        }
        aField = new Field(this, this, fieldName, value);
        addField(fieldName, aField);
        return aField;
    }

    public Field findField(String fieldName){
        if (fieldName == null || fieldName.length()==0){
            logDebug("findField found null for fieldName");
            return null;
        }
        Field result;
        if ( fieldName.indexOf('$')>0 ) {
            String datasource = StringTools.substring(fieldName, "$", 0);
            String unindexedName = StringTools.substring(fieldName, "$", 1);
            String index = StringTools.substring(fieldName, "$", 2);
            IDatasource ds = getDatasource(datasource);
            if ( ds != null ) {
                if ( index.length()>0 ) {
                    result = ds.getField(unindexedName, index);
                    if ( result != null ) {
                        logDebug("+++++ found field ("+fieldName+") in datasource ("+datasource+") by index: "+result);
                        return result;
                    }
                } else {
                    result = ds.getField(unindexedName);
                    if ( result != null ) {
                        logDebug("+++++ found field ("+fieldName+") in datasource ("+datasource+"): "+result);
                        return result;
                    }
                }
            }
        }
        //logDebug("findField fieldName: "+fieldName+" m_fields: "+m_fields);
        return (Field)m_fields.get(fieldName);
    }

    public Field removeField(String fieldName){
        if (fieldName == null || fieldName.length()==0){
            return null;
        }
        ///fieldName = fieldName.toUpperCase(); //CASE-INSENSITIVE
        Field old = (Field)m_fields.get(fieldName);
        m_fields.remove(fieldName);
        return old;
    }

    public void setFieldError(String fieldName, String message){
        Field f = findField(fieldName);
        if ( f!=null ) {
            f.setErrorMessage(message);
        } else {
            logError("field not found in Session.setFieldError: "+fieldName+" message was: "+message);
        }
    }

    //%%or.... add(datasource) to datasources....
    /* %%actually, the only thing that needs to know is the widgets, should
    call getMangledFieldName() instead of $widget.fieldName
    if datasource present, then datsource_fieldName else fieldname
    */
    /*public void registerField(IDatasource caller, Field field){
        String fieldName = field.get("name").toString();
        String id = caller.getID();
        addField(id+'_'+fieldName, field);
    }

    public void unregisterField(IDatasource caller, Field field){
        String fieldName = field.get("name").toString();
        String id = caller.getID();
        removeField(id+'_'+fieldName);
    }
     */

    public void registerDatasource(IDatasource caller){
        String id = caller.getID();
        m_datasources.put(id, caller);
    }

    public void unregisterDatasource(IDatasource caller){
        String id = caller.getID();
        m_datasources.remove(id);
    }

    public IDatasource getDatasource(String id){
        return (IDatasource)m_datasources.get(id);
    }

    //============ ExpansionLog Logging ====================================

    private ExpansionLog m_expansionLog = null;
    public ExpansionLog getExpansionLog(){
        if (LOG_EXPANSIONS){
            if (m_expansionLog == null){
                m_expansionLog = new ExpansionLog();
            }
            return m_expansionLog;
        }
        return null;
    }

    //============ Request Handler Logging =================================

    /* These are the scenarios we are trying to handle:
    <pre>

    enter A
    leave A
    enter B
    leave B

    which should format as:

    enter-and-leave A
    enter-and-leave B

     and

    enter A
        enter B
        leave B
    leave A

    </pre>
    New revision, see 1.135 for old version.
    */

    public void logHandlerProcCollapsed(String procName, Object message){
        logHandlerProc(procName, message);
    }

    //Used by enterHandlerProc and leaveHandlerProc:
    public final static int HANDLER_PROC_INDENT_INIT = 2;
    public final static int HANDLER_PROC_INDENT_INCR = 2;
    private StringList m_handlerProcs = new StringList();
    private int m_handlerProcIndent = HANDLER_PROC_INDENT_INIT;


    public static int gHandlerProcItemSeq = 0;
    public class HandlerProcItem {

        public HandlerProcItem(String procName, Object info, String lastThreadID){
            this.m_id = ""+(gHandlerProcItemSeq++);
            this.procName = procName;
            this.info = info;
            String theThreadID = Thread.currentThread().getName();
            if (!lastThreadID.equals(theThreadID)){
                this.threadID = theThreadID;
            }
        }
        public String procName;
        public Object info;
        public String threadID = "";
        Vector m_children = new Vector();
        public void add(HandlerProcItem child){
            m_children.add(child);
        }
        private String m_printed = "";

        private String m_id = "";
        public String getID(){return m_id;}

        private boolean m_collapsed = false;
        public boolean getCollapsed(){return m_collapsed;}
        public void setCollapsed(boolean new_value){m_collapsed = new_value;}

        public String toString(){
            if (m_collapsed){
                return procName ; //%%do this for admin panel with session/subsession, and toggle the collapese bit. + "<a href='?SESSIONID= ";
            }
            return m_printed;
        }
        public String print(int indent, String parentThreadID, Object endInfo){
            StringBuffer b = new StringBuffer();
            String thrstr = this.threadID;
            b.append("<table border='0' cellpadding='2' cellspacing='0' width='100%'><tr><td>");
            if (procName.length()>0){
                b.append("<span class='"+procName+"'>"); //handlerprocs.css defines standard css classes .INFO and .ERROR
                b.append(procName);
                b.append("</span><b>:</b>&#160;");
            }
            b.append(info);
            b.append("</td><td align='right' valign='top'><b>");
            b.append(thrstr);
            b.append("</b></td></tr>");
            for(Iterator it=m_children.iterator();it.hasNext();){
                HandlerProcItem child = (HandlerProcItem)it.next();
                b.append("<tr><td colspan='2'>");
                String childstr = child.print(/*indent+*/40, this.threadID, null);
                b.append(childstr);
                b.append("</td></tr>");
            }
            if ( endInfo != null){
                if (!endInfo.equals("") && (!endInfo.equals(info)) ){
                    //uses the green double-hakka (Euro begin quote) to show "leaving":
                    b.append("<tr><td><font color='green'><b>&#171;</b></font>");
                    b.append(procName);
                    b.append("<b>:</b>&#160;");
                    b.append(endInfo);
                    b.append("</td></tr>");
                }
            }
            b.append("</table>");
            m_printed = StringTools.indent(b.toString(), indent, "100%", 0);
            return m_printed;
        }
    }

    private String lastRequestThreadID = "";

    private Stack m_handlerProcStack = new Stack();

    private boolean m_logHandlerProcInited = false;

    private void logHandlerProcCheckInit(){
        if (m_logHandlerProcInited) return;
        logHandlerProcInit();
        m_logHandlerProcInited = true;
    }

    private void logHandlerProcInit(){
        if (LOG_HANDLER_PROC == false){
             return;
        }
        m_handlerProcs.setAllowDuplicates(true);
        m_handlerProcs.addObject("", "<table border='0' cellpadding='5' cellspacing='0' bgcolor='lightgrey'><tr><td width='100%'>"+
            "<b><u>Details</u></b></td><td align='right'><nobr><b><u>Thread ID</u></b></nobr></td></tr></table><br /><br /><hr />") ;
        //m_handlerProcs.addObject("<b><u>Action path</u></b>", "<table border='0' cellpadding='0' cellspacing='0'><tr><td width='100%'>"+
        //    "<b><u>Details</u></b></td><td align='right'><nobr><b><u>Thread ID</u></b></nobr></td></tr></table>") ;
    }


    private String decorateInfoOLD(Object info) {
        return "<table border='0' cellpadding='0' cellspacing='0'><tr><td width='100%'>"+
              info + "</td><td align='right'><nobr>["+Thread.currentThread().getName()+"]</nobr></td></tr></table>" ;
    }

    private String decorateInfo(Object info, String procName) {
        return "<span class='"+procName+"'>"+info.toString()+"</span>";
    }

    public String formatRequestLineForLogHandlerProc(HttpServletRequest request){
        if (request == null){
            return "[null request]";
        }
        String dump = "";
        if ( LOG_HANDLER_PROC_REQUEST_HEADERS ) {
            dump = ServletTools.dumpRequestInfo(request, true, "#FFAD00", m_requestUploadFileItems);
        } else {
            dump = ServletTools.dumpRequestParams(request, "#FFAD00", m_requestUploadFileItems);
        }
        return "<b>"+request.getMethod()+" "+request.getRequestURI()
                           +"</b> <br />"
                           +StringTools.indent(
                                dump,
                                100,
                                "",
                                10
                            );
    }


    public void logHandlerProcRequest(){
        String thisThreadID = Thread.currentThread().getName();
        if (lastRequestThreadID.equals(thisThreadID)){
            //logged the request info already for this thread/request
            return;
        }
        logHandlerProc("Request", formatRequestLineForLogHandlerProc(getRequest()));
        lastRequestThreadID = thisThreadID;
    }

    public void enterHandlerProc(String procName, Object info){
        if (LOG_HANDLER_PROC == false){
             return;
        }
        synchronized (m_handlerProcStack){
            logHandlerProcCheckInit();
            m_handlerProcStack.push(new HandlerProcItem(procName, info, lastRequestThreadID));
        }
    }

    /** @param info Can be empty, especially if you already documented something in the enterHandlerProc
     *   info proc, for example, the calling URL, since it won't have changed.
     */
    public synchronized void leaveHandlerProc(String procName, Object info){
        if (LOG_HANDLER_PROC == false){
             return;
        }
        synchronized (m_handlerProcStack){
            HandlerProcItem item  = (m_handlerProcStack.size()==0) ? null : (HandlerProcItem)m_handlerProcStack.pop();
            HandlerProcItem parent = (m_handlerProcStack.size()==0) ? null : (HandlerProcItem)m_handlerProcStack.peek();
            if (item!=null) {
                if ( parent == null ) {
                    String sitem  = item.print(0, "", info);
                    //System.out.println("item: \r\n"+sitem);
                    m_handlerProcs.addObject(""/*item.procName*/, sitem);
                } else {
                    parent.add(item);
                }
            }
        }
    }

    /** Just add it at one indent in, but don't hang on to the indent: this is one log line.
     */
    public void logHandlerProc(String procName, Object info){
        logHandlerProc(procName, info, true);
    }

    // IMPORTANT: WE MAY BE CALLED OUTSIDE OF THE DynamideHandler'S  PROTECTED BLOCK,
    //            SO DO NOT USE ANY CALLS THAT MAY USE m_handler.
    public void logHandlerProc(String procName, Object info, boolean doIndent){
        if (LOG_HANDLER_PROC == false){
             return;
        }
        logHandlerProcCheckInit();
        synchronized(m_handlerProcs){
            enterHandlerProc(procName, decorateInfo(info, procName));
            leaveHandlerProc(procName, "");
        }
    }

    // IMPORTANT: WE ARE BEING CALLED OUTSIDE OF THE DynamideHandler'S  PROTECTED BLOCK,
    //            SO DO NOT USE ANY CALLS THAT MAY USE m_handler.
    public void handlerProcComplete(long elapsedMillis, String replayURL){
        handlerProcComplete(elapsedMillis, replayURL, null);
    }

    // IMPORTANT: WE ARE BEING CALLED OUTSIDE OF THE DynamideHandler'S  PROTECTED BLOCK,
    //            SO DO NOT USE ANY CALLS THAT MAY USE m_handler.
    public void handlerProcComplete(long elapsedMillis, String replayURL, Throwable t){
        if (LOG_HANDLER_PROC_TIMING == true){
            logInfo("proc complete in "+elapsedMillis+" url: "+replayURL);
        }
        if (LOG_HANDLER_PROC == false){
            return;
        }
        logHandlerProcCheckInit();
        boolean indent = m_handlerProcIndent > HANDLER_PROC_INDENT_INIT ;
        String errMessage = "";
        if (t!=null){
            errMessage = " with error: "+t.getClass().getName()+":"+t.getMessage();
        }
        // %% this ends up with an extra ":" being printed just before the "Request Complete" line.  There should
        // %% be an overload to get rid of this, since the "procName" is blank.
        logHandlerProc("",
                       "<table border='1' cellpadding='3' cellspacing='0' width='100%'>"
                            +"<tr><td bgcolor='lightgrey'><b>Request Complete: </b>"
                            +errMessage+""+elapsedMillis+" milliseconds.  "
                            +"Click here to "+" <b>[<a href='"+replayURL+"' target='_blank'>replay</a>]</b>"
                            +" "+Tools.nowLocale()+"</td></tr></table><br /><br /><hr />",
                       indent);
    }

    public boolean isHandlerProcLogOn(){
        return LOG_HANDLER_PROC;
    }

    public String printHandlerProcLog(){
        if (LOG_HANDLER_PROC == false){
             return "Handler log is off. "
                +"\r\n<br />Set the following value in log.conf if you want it on:"
                +"<pre>\r\n    log4j.category.com.dynamide.Constants.LOG_HANDLER_PROC=DEBUG</pre>"
                +"\r\n<br />Alternatively, add the following Element to &lt;app> in web-apps.xml for this application:"
                +"<pre>\r\n    &lt;verbose-mode>true&lt;/verbose-mode></pre>";

        }
        logHandlerProcCheckInit();
        StringBuffer b = new StringBuffer();
        b.append("<table width='100%' border='0' cellpadding='3' cellspacing='0'>");
        Enumeration objs = m_handlerProcs.objects();
        while ( objs.hasMoreElements() ) {
            String s = (String)objs.nextElement();
            b.append("<tr><td>"+s+"</td></tr>");
        }
        b.append("</table>");
        return b.toString();
    }

    public String formatQueryStringDetailLink(HttpServletRequest request){
        if (request == null){
            return "NULL HttpServletRequest";
        }
        String params = request.getQueryString();
        if ( params == null ) {
            return "";
        }
        params = ServletTools.encodeURLString(params.trim());
        //you have to do it twice:
        //params = ServletTools.encodeURLString(params);
        if (params.length()>0){
            return " <b>[<a href='"+m_applicationPath+"?USER="+getUSER()+"&showQueryParams="+params+"'>view&#160;parameters</a>]</b>";
        } else {
            return " <br />"+dumpRequestInfo();
        }
    }


    //These are encoded by formatQueryStringDetailLink, above.
    //test: /dynamide?showQueryParams=qty%3D3000%26customer%3DLaramie%26com_dynamide_radiogroup1%3DContact%2BUs
    //test: /dynamide?showQueryParams=com_dynamide_radiogroup1%3DContact%2BUs
    private String handleShowQueryParams()
    throws Exception {
        String result = "<tr><th>Name</th><th>Value</th></tr>\r\n";
        String params = ServletTools.decodeURLString(getRequest(), "showQueryParams");
        String name, value;
        Enumeration namevalues = new com.dynamide.util.URLDecoder(params).getFields();
        while(namevalues.hasMoreElements()){
            // %% doesn't deal with array parameters.
            name = (String)namevalues.nextElement(); //name
            value = (String)namevalues.nextElement(); //value
            result += "<tr><td>"+name + "</td><td>" + value + "</td></tr>\r\n";
        }
        StringList variables = new StringList();
        variables.addObject("tableRows", result);
        String page = expandResource("resources/system/showQueryParams.xml", variables, "handleShowQueryParams");
        return page;
    }

    // =============== Jobs ===========================================

    private List m_jobLog = new Vector();
    public List getJobLog(){
        return m_jobLog;
    }

    public void logJobStart(String eventName, String jobName)
    throws Exception {
        StringList vars = new StringList();
        vars.addObject("eventName", eventName);
        vars.addObject("jobName", jobName);
        vars.addObject("jobStatus", "Job started");
        String entry = expandResource(Constants.jobLogItemTemplate, vars, "JobLog-for-item:"+eventName);
        m_jobLog.add(entry);
    }

    /** event can be null, in which case be sure to pass in status param
     */
    public void logJob(String eventName, ScriptEvent event, String jobName, String status)
    throws Exception {
        StringList vars = new StringList();
        vars.addObject("event", event);
        vars.addObject("eventName", eventName);
        vars.addObject("jobName", jobName);
        vars.addObject("jobStatus", status);
        String entry = expandResource(Constants.jobLogItemTemplate, vars, "JobLog-start-for-item:"+eventName);
        m_jobLog.add(entry);
    }

    public String printJobLog()
    throws Exception {
        StringList vars = new StringList();
        vars.addObject("log", m_jobLog);
        String res = expandResource(Constants.jobLogTemplate, vars, "JobLog-for-session:"+toString());
        return res;
    }

    /** Fires application_onJobDone with the Job object as the inputObject
     */
    public void onJobDone(Job job){
        fireApplicationEvent("application_onJobDone", "", job);
    }

    // ===========================================================================


    //============= Property Editors and Designers ==================================

    public Enumeration getApplicationProperties(){
        sortPropertiesTable();
        return getPropertiesTable().objects();
    }

    public String getApplicationPropertyEditor(Session session,
                                               Session subsession,
                                               Property property,
                                               String onkeydown,
                                               String onchange){
        StringList variables = new StringList();
        variables.addObject("target", subsession);
        variables.addObject("targetID", "application");
        variables.addObject("targetOwnerID", "application");
        variables.addObject("targetClass", "Session");
        return getPropertyEditor(session, subsession, "Application["+getSessionID()+"]", property, null, onkeydown, onchange, variables);
    }


    public String getWidgetPropertyEditor(Session session,
                                          Session subsession,
                                          Page page,
                                          Widget widget,
                                          Property property,
                                          Property defaultProperty,
                                          String onkeydown,
                                          String onchange){
        String widgetID = widget.getID();
        StringList variables = new StringList();
        variables.addObject("widget", widget);
        variables.addObject("widgetID", widgetID);
        variables.addObject("abstractWidget", widget);
        variables.addObject("page", page);
        variables.addObject("parent", null);
        variables.addObject("pageID", page.getName());
        variables.addObject("target", widget);
        variables.addObject("targetID", widgetID);
        variables.addObject("targetOwnerID", page.getName());
        variables.addObject("targetClass", "Widget");
        String logName = getSessionID()+".getWidgetPropertyEditor("+widget.getID()+','+property.getName()+"...)";
        return getPropertyEditor(session, subsession, logName, property, defaultProperty, onkeydown, onchange, variables);
    }

    public String getPagePropertyEditor(Session session, Session subsession, Page page, Property property, String onkeydown, String onchange){
        StringList variables = new StringList();
        variables.addObject("abstractWidget", page);
        variables.addObject("page", page);
        variables.addObject("pageID", page.getName());
        variables.addObject("parent", null);
        variables.addObject("target", page);
        variables.addObject("targetID", page.getID());
        variables.addObject("targetOwnerID", page.getID());
        variables.addObject("targetClass", "Page");
        String logName = page.getName()+".getWidgetPropertyEditor("+page.getID()+','+property.getName()+"...)";
        return getPropertyEditor(session, subsession, logName, property, null, onkeydown, onchange, variables);
    }

    public static String getPropertyEditor(Session session,
                                           Session subsession,
                                           String logName,
                                           Property property,
                                           Property defaultProperty,
                                           String onkeydown,
                                           String onchange,
                                           StringList variables){
        try {
            if ( variables == null ) {
                variables = new StringList();
            }
            variables.addObject("property", property);
            variables.addObject("defaultProperty", defaultProperty);
            variables.addObject("onkeydown", onkeydown);
            variables.addObject("onchange", onchange);
            String datatype = property.get("datatype").toString();
            System.out.println("\n\n===============>>> datatype: "+datatype);
            if ( datatype.length()==0 ) {
                //System.out.println("ERROR: property.get(datatype); didn't work!! "
                //   +" propertyName:"+propertyName
                //   +" property:"+property.dump()
                //   +" (in Page.java)");
            }
            String propertyDataType = Datatype.getDatatypeClassName(datatype); //defaults to StringDatatype if datatype == "".
            System.out.println("===============>>> propertyDataType: "+propertyDataType);
            variables.addObject("propertyDataType", propertyDataType);
            WidgetType wt = session.findWidgetType("editors/"+propertyDataType+".editor");  //%% todo: also do designers.
            if ( wt == null ) {
                return "ERROR: [16] type not found: "+"editors/"+propertyDataType+".editor";
            }
            String templateText = wt.getRawHTMLSource(session.getBrowserStringID());
            String htmlText = "";
            //try {
                htmlText = subsession.expandTemplate(variables, templateText, logName);
            //} catch (com.dynamide.TemplateSyntaxException te){
            //    session.logHandlerProc("ERROR", "caught TemplateSyntaxException in getAbstractWidgetEditor()");
            //    htmlText = te.getErrorHTML();
            //}
            System.out.println("===============>>> htmlText: "+htmlText);
            return htmlText;
        } catch (Exception e2){
            session.logHandlerProc("ERROR", "caught Exception in getPropertyEditor() "+logName);
            return "ERROR: [17] \r\n<pre>"+Tools.errorToString(e2, true)+"</pre>";
        }
    }



    //============= Internationalization / Localization =============================

    public String getTimestampLocale(){
        return Tools.nowLocale();
    }

    private String  m_currentLanguageCode = "";
    public String  getCurrentLanguageCode(){return m_currentLanguageCode;}
    private void setCurrentLanguageCode(String  new_value){m_currentLanguageCode = new_value;}

    /** @return true if languageCode passed in is null or empty, otherwise returns true if found.
    */
    public boolean loadInternationalization(String languageCode){
        if ( languageCode == null ||  languageCode.length() == 0) {
            return true;
        }
        setCurrentLanguageCode(languageCode);
        String filename = Constants.internationalizationRelDir+"/intlres-"+languageCode+".properties";
        String actualFilename = findAppFilename(filename);
        if ( actualFilename == null || actualFilename.length()==0 ) {
            logWarn("loadInternationalization did not find a resource file for language: "+languageCode+" looking for "+filename);
            return false;
        }
        actualFilename = Tools.fixFilename(actualFilename);
        Map props = FileTools.loadPropertiesFromFile("", actualFilename);
        if (m_internationalizedValues != null){
            Map m2 = Tools.createSortedCaseInsensitiveMap();
            m2.putAll(props);
            m_internationalizedValues = m2;
            //logDebug("loadInternationalization: "+languageCode+"\r\n"+m_internationalizedValues);
            return true;
        }
        logWarn("loadInternationalization failed: "+languageCode);
        return false;
    }

    /** @return a non-null String if found, otherwise a null String.
     */
    public String lookupInternationalizedValue(String name){
        if (m_internationalizedValues != null){
            String res = (String)m_internationalizedValues.get(name);//may be null.
            //if (res!=null)logDebug("lookupInternationalizedValue: "+name+" => "+res);
            //else logDebug("lookupInternationalizedValue, null res. name: "+name);
            return res;
        }
        //logDebug("lookupInternationalizedValue: "+name+" [empty]");
        return null;
    }

    public String internationalize(String name, String defaultValue){
        String result = lookupInternationalizedValue(name);
        if ( result == null || result.length() == 0 ) {
            return defaultValue;
        }
        return result;
    }

    // need a url like this /dynamide?USER=laramie&which=page&page=page1&action=showInternationalizationForm&SESSIONID=DM_1015106718969
    public String showInternationalizationForm(HttpServletRequest request)
    throws Exception {
        HttpServletRequest prevHandler = getRequest();
        setRequest(request);
        try {
            String language = getQueryParam("language");
            if (language != null && language.length()>0){
                loadInternationalization(language);
            }
            String body = "";
            Session subsession = this;
            m_widgetTypePool.remove("com.dynamide.ide.internationalize");
            Widget wd = new Widget(subsession, subsession);
            wd.setSession(subsession);
            try {
                wd.setType("com.dynamide.ide.internationalize");
                wd.setProperty("name", "internationalize1");
            } catch( Exception e ){
                logError("couldn't set name property in showInternationalizationForm.  Continuing.");
            }
            int theBrowserID = getBrowserID();
            //wd.setBrowserID(theBrowserID);
            String pageID = getQueryParam("page");
            if (pageID.length()==0){
                //System.out.println("no page specified");
            } else {
                try{
                    loadPage(pageID, false, null);
                } catch (Exception e){
                    logError("loading page", e);
                }
            }
            body = renderWidget(wd, pageID, request);
            StringList variables = new StringList();
            variables.addObject("title", "Detail");
            variables.addObject("body", body);
            return expandResource("resources/system/systemPage.xml", variables, "Session.showInternationalizationForm");
        } finally {
            setRequest(prevHandler);
        }
    }

    //================ SessionWebMacroLogTarget =================

    private SessionWebMacroLogTarget m_webmacroLog = null;
    public void checkWebmacroLogRef(){
        /*
         * logDebug("Checking m_webmacroLog...");
        if ( m_webmacroLog == null ) {
            try{
                logDebug("Calling constructor...");
                m_webmacroLog = new SessionWebMacroLogTarget();
                m_webmacroLog.log(org.webmacro.util.Clock.getDate(), "LogFile", "NOTICE", "--- SessionWebMacroLogTarget Started ---", null);
                m_webmacroLog.log(org.webmacro.util.Clock.getDate(), "LogFile", "WARNING", "--- SessionWebMacroLogTarget Started ---", null);
                logDebug("...Done constructor");
            } catch (Exception e){
                logDebug(Tools.errorToString(e, true));
            }
        }
        logDebug("Checking m_webmacroLog: "+m_webmacroLog);
         */
    }
    public void logWhichExpansion(String msg){
        //checkWebmacroLogRef();
        //if (m_webmacroLog != null) m_webmacroLog.log("Expanding: "+msg+"\r\n");
    }
    public String showWebMacroLog(){
        return "NOT IMPLEMENTED";
        //org.webmacro.util.LogSystem.flushAll();
        //return m_webmacroLog.dump();
    }

    //================ ISessionItem ===================
    public boolean timeout(long now){
        if ( now - m_lastAccessTime > 1000*60*60 ) {//60 minutes.
            logInfo("Session expired");
            return true;
        }
        return false;
    }

    /** The shutdown method is called in a new thread, and you will be out of the session
     *  table by the time this gets called.
     */
    public void shutdown(){
        close();
    }
    public boolean isCritical(){
        return false;
    }
    public String getParentSessionID(){
        return "";
    }
    public boolean hasChildSession(String childSessionID){
        return false;
    }
    //=========== ISessionTableItem ===========================
    public String getUserName(){
        return getUSER();
    }

    public String getHostName(){
        return "localhost"; //not really used for anything except reporting.
    }

    	
    public String getAttachToLink(){
    	return getAttachToLink(getURLPath());
    }
    
    public String getAttachToLink(String caption){
	    String path = getURLPath();
	    return "<a href='"+path+"?"+Constants.SESSIONID+"="+sessionID+"'>"+caption+"</a>";
    }

    public String getActiveItemTag(){
        return "";
    }

    public String getCurrentPageID(){
        return m_pageID;
    }

    /** Returns the mode listing for the ISessionTableItem interface, by calling getModeStringShort().
     *
     * @see #getModeStringShort
     */
     public String getModeListing(){
        return getModeStringShort();
    }

    public String getReportLink(){
        return "";
    }

    public String getCloseLink(){
        return "<a href='?"+Constants.SESSIONID+"="+sessionID+"&"+Constants.action+"="+Constants.Close+"'>close</a>";
    }

    public String getExtraInfo(){
        return "<a href='/dynamide/admin?showSessionObjectDetail="+sessionID+"&USER=laramie'>"+sessionID+"</a>";
    }

    public long   getStartTime(){
        return m_startTime;
    }

    public long getLastAccessTime(){
        return m_lastAccessTime;
    }

    //=================== IDatasource interface =============================

    public class SessionDatasourceIterator implements Iterator {
        private Session m_target;
        private int m_iterCount = 0;
        public SessionDatasourceIterator(Session target){
            m_target = target;
        }
        public Object next(){
            m_iterCount++;
            return m_target;
        }
        public boolean hasNext(){
            return (m_iterCount < 1);
        }
        public void remove(){
            throw new UnsupportedOperationException();
        }
    }

    /** @return A pointer to this Session, since Session is an IDatasource.
     */
    public IDatasource getDatasourceHelper(){
        return this;
    }

    public Iterator iterator(){
        return new SessionDatasourceIterator(this);
    }

    public boolean isReadOnly(){
        return false;
    }

    public boolean post(){
        return false;
    }
    
    public void cancel() {
    	//do nothing.  No datasource to cancel.
    }

    public boolean go(int distance){
        return false;
    }

    public boolean seek(int zeroBasedIndex){
        return false;
    }

    public boolean seekBegin(){
        return false;
    }

    public boolean seekEnd(){
        return false;
    }

    public void onRowChanged(){
    }

    public boolean insertRow(int index){
        return false;
    }

    public boolean isRowCountAllowed(){
        return true;
    }

    public int getRowCount(){
        return 1;
    }

    public int getCurrentRowIndex(){
        return 0;
    }

    public boolean isCurrentRow(){
        return true;
    }

    //================== IContext interface =================================

    //Mostly, I just inherit the interface from JDOMFile, which extends ContextNode.
    //See also: get(String) where I peek in super.get(String) as well.

    public Object find(String path){
        Object res = super.find(path);
        if ( res != null ) {
            return res;
        }
        return findDotted(path);
    }

    public String getKey(){
        return getSessionID();
    }

    //================ tests ===================================

    private void pageTest() throws Exception {
        Page p = findPageByID("page1");
        p.outputPage();
        p = null;
    }

    private static void runContextTest(WebMacro wm)
    throws org.webmacro.InitException {
        Context c = getContext(wm, false, "DM_TESTSESSION", null /*HttpServletRequest*/);
        //logDebug(dumpContext(c));
        releaseContext(c);
        c=null;
    }

    private void webmacroTest()
    throws Exception {
            try{
                getWebMacro();
            } catch (Exception e){
                Log.error(Session.class, "Exception calling getWebMacro()...continuing", e);
            }
            //Page page = s.getPageByFile(new File(Tools.fixFilename(args[0]+"/com/dynamide/sites/dynamide/apps/dynamide/demo/page1.xml")));
            //logDebug(page.outputPage());
            com.dynamide.util.Profiler prof = com.dynamide.util.Profiler.getSharedProfiler();
            //for (int i=0; i < 10000; i++) { //171 ms / 10000 loops ==> 0.0000171 seconds to get a webmacro context.
            //    Context c = s.getContext();
            //}
            StringList variables = new StringList();
            variables.addObject("foo", "bar");
            String sTest = expandTemplate(variables, "mojo is $foo", "test");
            int loops = 4;
            int memloops = 1;
            int wmloops = 5000;
            logDebug("Beginning profile test...["+loops+" loops]");
            String template = "This is a template.  session: $session foo: $foo #if ($foo == $foo){ $foo }";
            for (int i=0; i < loops; i++) {
                prof.enter("start");
                for (int j=0; j < memloops; j++) {
                    //12000 ms / 10000 loops ==> 1.2 milliseconds to use a webmacro context.
                    //81000 ms / 70000 loops ==> 1.1 milliseconds to use a webmacro context.

                    // sTest = s.expandTemplate(variables, template, "test");
                    pageTest();
                    //logDebug("sTest: "+sTest);
                    //logDebug("sTest: "+sTest);
                }
                prof.leave("start");
                logDebug("\r\n"+Tools.cleanAndReportMemory());
                Tools.readln("Press Enter");
            }

            logDebug("Done profile test.");
            logDebug("profile results:\r\n"+prof.getOutputString());

            logDebug("WebMacro memory test ...");
            Tools.readln("Press Enter");
            WebMacro wm = initWebmacro();
            for (int i=0; i < loops; i++) {
                for (int j=0; j < wmloops; j++) {
                    //logDebug(dumpContext(c));
                    runContextTest(wm);
                    //logDebug("\r\n"+Tools.cleanAndReportMemory());
                }
                logDebug("\r\n"+Tools.cleanAndReportMemory());
                Tools.readln("Press Enter");
            }

    }

    private void redirectTest(String uri)
    throws Exception {
        StringBuffer result = new StringBuffer();
        ResourceManager rm = ResourceManager.getRootResourceManager();
        IContext context = (IContext)rm.find("/conf/secure");
        result.append(redirectTestInner(uri, context, "https", "TEST.DYNAMIDE.COM", "8080"));
        result.append(redirectTestInner(uri, context, "https", "TEST.DYNAMIDE.COM", "7080"));
        result.append(redirectTestInner(uri, context, "http", "apps.dynamide.com", "8080"));
        result.append(redirectTestInner(uri, context, "https", "apps.dynamide.com", "7080"));
        result.append(redirectTestInner(uri, context, "http", "apps.dynamide.com", "7080"));
        result.append(redirectTestInner(uri, context, "https", "", ""));
        result.append(redirectTestInner(uri, context, "http", "", ""));
        result.append(redirectTestInner(uri, context, "", "", ""));
        result.append(redirectTestInner(uri, context, "", "apps.dynamide.com", ""));
        result.append(redirectTestInner(uri, context, "", "", "8080"));
        result.append(redirectTestInner(uri, context, "https", "", "8080"));

        System.out.println("redirectTest result: "+result.toString());
    }

    private String redirectTestInner(String uri, IContext context, String protocol, String host, String port)
    throws Exception {
        StringBuffer result = new StringBuffer();
        context.rebindAttribute("protocol", protocol);
        context.rebindAttribute("port", port);
        context.rebindAttribute("host", host);
        result.append("\r\n----------------------------------");
        result.append("\r\nREDIRECT conf: "+protocol+','+host+','+port);
        //System.out.println(ResourceManager.getRootResourceManager().dumpContext("/conf", "  ", false));
        Session s = Session.createSession(uri);
        //generateSecureRedirect(String protocol, String currentHost, String currentPort, String currentURL){
        String red = s.generateSecureRedirect("http", "FROM.DYNAMIDE.COM", "7080", uri);
        result.append("\r\nREDIRECT RESULT: "+red);
        return (result.toString());
    }

    //================ main ===================================

    private static void usage(String[]args){
            StringBuffer sb = new StringBuffer();
            for (String s : Arrays.asList(args)){
                sb.append(s);
                sb.append(" ");
            }
            System.out.println("\r\nYou passed in: java ... com.dynamide.Session "+ sb.toString());
            System.out.println("\r\nUsage: java $JAVA_OPTS com.dynamide.Session <DYNAMIDE_RESOURCE_ROOT> <urlPath> [-webmacroTest]");
            System.out.println("   for example: java com.dynamide.Session C:\\dynamide_resource_root /dynamide/demo ");
            System.out.println("   or for example: java com.dynamide.Session /usr/local/dynamide/build/dynamide_resource_root  /dynamide/demo ");
    }

    public static void main(String [] args) {

        try{
            if (args.length < 2){
                usage(args);
                System.exit(1);
            }
            String RESOURCE_ROOT = args[0];
            String urlPath = args[1];
            ResourceManager rootResourceManager = ResourceManager.createStandalone(RESOURCE_ROOT);

            Session s = Session.createSession(urlPath);

            if ( args.length > 2 && args[2].equalsIgnoreCase("-webmacroTest") ) {
                s.webmacroTest();
            }

            if ( args.length > 2 && args[2].equalsIgnoreCase("-redirectTest") ) {
                //test with a secured application in web-apps.xml, such as /dynamide/test-secure
                s.redirectTest(urlPath);
            }

        } catch (Exception e){
            usage(args);
            System.out.println("\r\nException:");
            e.printStackTrace();
        }
        System.exit(0);
    }

	

}
