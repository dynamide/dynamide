/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dynamide.resource.Assembly;
import com.dynamide.resource.IContext;
import com.dynamide.resource.ResourceManager;
import com.dynamide.util.Log;
import com.dynamide.util.Opts;
import com.dynamide.util.ServletTools;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class DynamideHandler extends DynamideObject implements Runnable {

    //Constructor:     See also-- init()
    public DynamideHandler() {
        this(null);
    }

    public DynamideHandler(DynamideObject owner){
        super(owner);
    }

    public void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }


    private void cleanup(){
        m_request = null;
        m_response = null;
        m_servlet = null;
    }

    //private String lastViewedPageName = "";

    //private static final String READ_TEMPLATE_ERROR = "ERROR: [45] File not found: ";

    private HttpServletRequest m_request = null;
    public HttpServletRequest getRequest(){return m_request;}

    private HttpServletResponse m_response = null;
    public HttpServletResponse getResponse(){return m_response;}

    private HttpServlet m_servlet = null;
    public HttpServlet getServlet(){return m_servlet;}

    private String  m_debugURL = "?DynamideHandlerTest=1";
    public String  getDebugURI(){return m_debugURL;}
    public void setDebugURL(String  new_value){m_debugURL = new_value;}

    private HandlerResult  m_result;
    public HandlerResult getResult(){
        if (m_result == null){
            return new HandlerResult("INCOMPLETE REQUEST");
        } else {
            return m_result;
        }
    }

    public void init(HttpServletRequest request,
                     HttpServletResponse response,
                     HttpServlet servlet){
        m_request = request;
        m_response = response;
        m_servlet = servlet;
    }

    //Use of prettyPrintHTML:
    //For netscape, you MUST expand empties, or <script src=''/> will not end and will hide the whole page
    //For IE, <br></br> may cause two lines:
    // ==> use nonXHTML and use expand in pro mode.
    // skipping newlines doesn't save much.  For /dynamide/demo, sqeezing whitespace, but keeping newlines
    // save 10Kbytes (from 25K down to 15K)  Squeezing newlines saves another 600 bytes or so.  ==> don't do it.

    public boolean finished = false;

    public void run(){
        com.dynamide.util.Profiler profiler = null;
        if (com.dynamide.Constants.PROFILE) profiler = com.dynamide.util.Profiler.getSharedProfiler();
        //logDebug("DynamideHandler starting run() "+getObjectID());
        try {
            finished = false;
            m_result = handleHttpRequest();
            if (com.dynamide.Constants.PROFILE) profiler.log("handleHttpRequest returned");
        } finally {
            m_request = null;
            m_response = null;
            m_servlet = null;
            finished = true;
            if (com.dynamide.Constants.PROFILE) profiler.log("handler.finished");
        }
        //logDebug("DynamideHandler leaving run() "+getObjectID());
    }

    /** This function does three jobs: one is to set the request globally
     *  so that all methods of this object can rely on it; second
     *  is to grab the last page presented in case the user is being snooped.
     *  Snooping is for client service to watch what someone is doing so
     *  they can help them over the phone while the client and the client
     *  service agent are watching the same virtual screen.
     *  Third is to check the machine id, which I call VkLocation.
     */
    private HandlerResult handleHttpRequest(){
        String fullUri = m_request != null ? m_request.getRequestURI() : "";
        com.dynamide.util.Profiler profiler = null;
        if (com.dynamide.Constants.PROFILE) profiler = com.dynamide.util.Profiler.getSharedProfiler();

        //m_request = request;  //This allows any method of this object to access
        // the current request, while this method is active.
        try { // catch all errors.
            //return NOT FOUND for favicon, since multiple apps cannot use the same favicon.
            if (fullUri.equals("/favicon.ico")){
                HandlerResult hr = new HandlerResult("");
                hr.setResponseCode(HttpServletResponse.SC_NOT_FOUND);
                hr.setErrorMessage("No favicon.ico");
                return hr;
            }
            //this.lastViewedPageName = "";
            HandlerResult result;
            if (com.dynamide.Constants.PROFILE) profiler.enter("handleHttpRequest: "+fullUri);
            try {
                //MEMORY_LEAKS test 2
                result = handleHttpRequest_Internal();
            } finally {
                if (com.dynamide.Constants.PROFILE) profiler.leave("handleHttpRequest: "+fullUri);
            }
            if (com.dynamide.Constants.PROFILE) profiler.enter("prettyPrintHTML");
            try {
                if (result.redirectURL.length()==0 && result.result != null && result.prettyPrint){
                    boolean debugTransformations = Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
                    if (debugTransformations){
                        logDebug("DynamideHandler.handleHttpRequest before prettyPrintHTML:\r\n-----------------\r\n"+result.result+"\r\n-----------------");
                    }
                    
                    //MEMORY_LEAKS:
                    //System.out.println("Before pretty print\r\n"+Tools.cleanAndReportMemory());
                    result.result = prettyPrintHTMLSafe(result.result);
                    //System.out.println("After pretty print\r\n"+Tools.cleanAndReportMemory());
                    System.gc();
                    
                    if (debugTransformations){
                        logDebug("DynamideHandler.handleHttpRequest after prettyPrintHTML:\r\n-----------------\r\n"+result.result+"\r\n-----------------");
                    }
                }
                logTranscript(result.result);
                return result;
            } finally {
                if (com.dynamide.Constants.PROFILE) profiler.leave("prettyPrintHTML");
            }
        } catch (ApplicationNotFoundException e)  {
            HandlerResult hr = new HandlerResult("");
            String errmsg = "Application Not Found<br/>"
                   +"<br />URI: "+e.getURI()
                   +((e.getMessage().length()==0) ? "" : "<br /><br />Message: "+e.getMessage())
                   ;
            hr.setResponseCode(HttpServletResponse.SC_NOT_FOUND);
            hr.setErrorMessage(errmsg);
            return hr;
        } catch (Throwable e) {
            logError("UNHANDLED ERROR IN Dynamide MAIN LOOP. See result page.");//, e);
            //return new HandlerResult(formatErrorPage(e, "NN1"), false);
            String more = "";
            String header = "<h3>Request</h3> "+ServletTools.dumpRequestInfo(m_request)
                            +"<br/><hr/>";
            String msg = "<h3>Message</h3>"+StringTools.escape(e.getMessage());
            if (e instanceof DynamideUncaughtException){
                msg += "\r\n<h3>Extra Message</h3>"+((DynamideUncaughtException)e).getExtra();  //extra message is html.  Before I was escaping it.
            }
            Throwable t = null;
            if (Tools.isJVM13()){
                if (e instanceof DynamideException){
                    t = ((DynamideException)e).getCause();
                }
            } else {
                t = e.getCause();
            }
            if (t != null && t instanceof Exception){
                more = "\r\n<h3>Inner Message</h3><pre>"+StringTools.escape(((Exception)t).getMessage())+"</pre>";
            }

            if (t != null && t instanceof TemplateSyntaxException){
                more += "\r\n<h3>Error html</h3><pre>"+StringTools.escape(((TemplateSyntaxException)t).getErrorHTML())+"</pre>";
            }
            more += "<hr/><h3>Exception Trace</h3><pre>"+StringTools.escape(Tools.errorToString(e, true, false))+"</pre>";
            String page = header+msg+more;
            String errorID = ResourceManager.writeErrorLog(null, "unhandled-exception", "", page, Tools.getStackTrace(e), e.getClass().getName());
            //return new HandlerResult("<html><body><h2>Dynamide :: Unhandled Exception</h2><hr/>"+header+msg+more+"</body></html>", false);
            HandlerResult result = new HandlerResult("<html><body><h2>Dynamide :: Unhandled Exception</h2><hr/>"
                                     +"<a href='"+ResourceManager.errorIDToHref(errorID)+"'>"+errorID+"</a>"
                                     +"</body></html>", false);
            result.setResponseCode(500);
            result.setErrorMessage("Server Error: "+errorID);
            //TODO: setErrorMessage doesn't seem to go through to client: result.setErrorMessage("<a href='"+ResourceManager.errorIDToHref(errorID)+"'>"+errorID+"</a>");
            //TODO: 20160729  That's because you need to set it in here and pass it through in DynamideServlet:368
            result.setMimeType("text/html");
            return result;
        }
    }

    public static String prettyPrintHTMLSafe(String src){
        try {
            return prettyPrintHTML(src);
        } catch (XMLFormatException e)  {
            Log.error(DynamideHandler.class, "prettyPrintHTMLSafe hiding error by returning src only. partial src: \r\n=======\r\n"+StringTools.ellipses(src, 300)+"\r\n======", e);
            return src;
        }
    }

    public static String prettyPrintHTML(String src)
    throws XMLFormatException {
        return JDOMFile.prettyPrintHTML(src,    //String html
                                "true",   //String newlines -- Netscape can't handle superlong lines.
                                "true",    //String trim    -- for normal requests, save lots of space. (view source in ide uses different options.
                                true,      //boolean xhtml -- don't break DOM parser in IDE.
                                true,      //boolean expandEmpty -- don't break Netscape with <script/> tags.
                                false,    //boolean indent   --save lots of space.
                                false);   //hide errors  --only do if explicitly asked
    }


    /**Rather than have one button with different values, all the submit buttons have
     * different names.  request.getParameter will return null if this name is not found
     * in the POST params.  If it is found, then that button was clicked.  This way,
     * we are allowed to change the button's value, which is the caption of the button
     * that the user sees, without breaking the code below.
     */
    private HandlerResult handleHttpRequest_Internal()
    throws Throwable {
        long start = Tools.now().longValue();
        Session theSession = null;
        String fullURI = m_request != null ? m_request.getRequestURI() : "";

        if ( fullURI.startsWith(ResourceManager.CACHE_URI) ) {
            HandlerResult res = checkNotModifiedForCache(fullURI);
            if ( res != null ) {
                return res;
            }
        }

        HandlerResult relRes = checkForRelResource(fullURI);
        if (relRes != null) {
            return relRes;
        }

        ResourceManager root = ResourceManager.getRootResourceManager();
        HandlerResult handlerResult = new HandlerResult("", true);

        String action = ServletTools.getParameterValue(m_request, Constants.action);
        String sessionID = ServletTools.getParameterValue(m_request, "SESSIONID");
        theSession = (Session)root.getSession(sessionID); //%% TODO: make it ask for the right Account, probably from uri, which means we have to resolve BEFORE going into createSession.

        if ( theSession != null && action.equals(Constants.Restart) ) {
            closeSession(sessionID, theSession);
            theSession = null;
        }


        if (theSession==null){
            String forcedSessionID = "";
            if (sessionID != null && sessionID.length()>0){
                forcedSessionID = sessionID;
                logInfo("Recycling SESSIONID: "+sessionID);
            }

            String debugURL = "";
            if (m_request == null){
                debugURL = m_debugURL;
                fullURI = debugURL;
            }

            //5/3/2003 12:50PM the start:boolean param is new.  I used to use action start, so the IDE may depend on that.
            theSession = Session.createSession(fullURI, fullURI, Constants.SESSION_PREFIX, forcedSessionID, true, Session.MODE_NONE, this, debugURL, handlerResult);
            if (handlerResult.redirectURL.length()>0){
                theSession.busy = false;
                doFinallyForSession(theSession, root);
                System.out.println("1 session.busy: "+theSession.busy+" id: "+theSession);
                return handlerResult;
            }
            //4/25/2004 theSession.busy = true;  //now it is busy with this request.
            //done in session itself, now: theSession.logHandlerProc("Created Session", theSession.formatRequestLineForLogHandlerProc(m_request));
        }

        if (action.equals("dynamideHandlerTest")){
            return new HandlerResult("<html><body><pre>"+Tools.now()+"</pre></body></html>");
        }
        

        //======== theSession is now not null and ready to handle just this request =====
        synchronized (theSession){
            theSession.busy = true;  //now it is busy with this request.
            theSession.setHandler(this);
            try {
                if ( action.equals(Constants.Close) ) {
                    return closeSession(sessionID, theSession);
                } else {
                    try {
                        //======== TADA!  Now we call the session to do it's bit: ==========
                        handlerResult = theSession.handleAction(this, action, handlerResult);
                        //MEMORY_LEAKS test 1
                        //==================================================================
                        if (ServletTools.getParameterValue(m_request, "dmFormat").equals("SOURCE") ) {
                            handlerResult = returnSOURCE(theSession, handlerResult);
                        }
                        String url = handlerResult.redirectURL;
                        if (url.length()>0){
                            if ( ! url.startsWith("http") ) {
                                String protoHostPort = (String)root.find("/conf/VirtualHost/protoHostPort");
                                if ( protoHostPort != null ) {
                                    url = protoHostPort+url;
                                    handlerResult.redirectURL = url;
                                }
                            }
                            theSession.logHandlerProc("REDIRECT", url);
                        }
                        logSessionComplete(theSession, start, null);
                        return handlerResult;
                    } catch (Throwable t){
                        logSessionComplete(theSession, start, t);
                        throw t;
                    }
                }
            } finally {
                doFinallyForSession(theSession, root);
            }
        }
        //====================================================================================
    }

    private void doFinallyForSession(Session theSession, ResourceManager root){
        //System.out.println("doFinallyForSession: "+theSession);
        theSession.setHandler(null);
        theSession.busy = false;
        if (theSession.getCloseWhenRequestComplete()){
            System.out.println("closing session:"+theSession);
            theSession.close();
        } else if ( theSession.getModeFlags() == Session.MODE_NONE
             && Tools.isBlank(theSession.getDebugURI()))  {
            //nothing fancy, must be a re-useable session.
            if (!theSession.isClosing()){
                root.repoolSession(theSession);
            }
        }
    }

    private void logSessionComplete(Session theSession, long start, Throwable t){
        try {
            long end = Tools.now().longValue();
            String replayURL = ServletTools.getURL(m_request);
            theSession.handlerProcComplete(end-start, replayURL, t);
        } catch (Throwable t2){
            logError("Couldn't call handlerProcComplete on session with error: ", t2);
        }
    }

    private HandlerResult closeSession(String sessionID, Session theSession){
        if (theSession != null){
            theSession.close();  //this also sets the handler to null.
            return new HandlerResult("<html><body>Session closed: "+sessionID+" at time: "+Tools.now()+"</body></html>");
        } else {
            return new HandlerResult("<html><body>Invalid SESSIONID:"+sessionID+"</body></html>");
        }
    }

    private HandlerResult returnSOURCE (Session theSession, HandlerResult handlerResult){
        String page = handlerResult.result;
        try {
            if (handlerResult.prettyPrint) page = JDOMFile.prettyPrintHTML(page);
        } catch (com.dynamide.XMLFormatException e){
            return new HandlerResult(e.getMessage(), Constants.NO_EXPIRES, false);
        }
        theSession.logHandlerProc("TIDY", "Tidied source returned");
        String title = ServletTools.getParameterValue(m_request, "next");
        title = title != null ? title : "";
        return new HandlerResult("<html><body><pre>"+StringTools.makeLineNumbers(StringTools.escape(page))+"</pre>");
    }

    private HandlerResult extractResourceContentWithExpiresHandling(String fullURI, IContext resource) throws com.dynamide.resource.ResourceException {
        if (resource != null) {
            //compare date time stamp and return
            boolean modified = false;
            long since = m_request != null ? m_request.getDateHeader("If-Modified-Since") : -1;
            //look in the IContext for the modified stamp, and process it.
            if (since > 0) {  //will be -1 if header is not present.
                Long resourceModified = (Long) resource.getAttribute(Assembly.MODIFIED);
                if (resourceModified != null && (since - resourceModified.longValue() < 0)) {
                    modified = true;
                }
                String ifms = m_request != null ? m_request.getHeader("If-Modified-Since") : "";
                System.out.println("If-Modified-Since: " + ifms + " value: " + since + " modified: " + modified + " last: " + resourceModified);
                if (!modified) {
                    HandlerResult res = new HandlerResult("", Constants.DEFAULT_EXPIRES, false);
                    res.notModified = true;
                    return res;
                }
            }
            if (Assembly.isBinaryResource(resource)) {
                byte[] bcontent = Assembly.extractBinaryResourceContent(resource);
                if (bcontent == null) {
                    return new HandlerResult("ERROR: [89] binary resource not found: " + resource, 0, false);
                }
                String name = (String)resource.getAttribute(Assembly.FULLPATH);
                return new HandlerResult(bcontent, Constants.DEFAULT_EXPIRES, false, Assembly.getMimeType(name));
            } else {
                String scontent = Assembly.extractResourceContent(resource);
                if (scontent == null) {
                    return new HandlerResult("ERROR: [90] resource not found: " + resource, 0, false);
                }
                String name = (String)resource.getAttribute(Assembly.FULLPATH);
                return new HandlerResult(scontent, Constants.DEFAULT_EXPIRES, false, Assembly.getMimeType(name));
            }
        }
        return null;
    }

    private HandlerResult checkNotModifiedForCache(String fullURI)
    throws com.dynamide.resource.ResourceException {
        ResourceManager root = ResourceManager.getRootResourceManager();
        IContext resource = root.getCachedResource(fullURI);
        //System.out.println("DynamideHandler: "+fullURI+" :: "+resource);
        return extractResourceContentWithExpiresHandling(fullURI, resource);
    }

    private HandlerResult checkForRelResource(String fullURI)
    throws com.dynamide.resource.ResourceException, ApplicationNotFoundException {
        ResourceManager root = ResourceManager.getRootResourceManager();
        IContext resource = root.findStaticResource(fullURI);
        //System.out.println("DynamideHandler: "+fullURI+" :: "+resource);
        return extractResourceContentWithExpiresHandling(fullURI, resource);  //will
    }

    private void logTranscript(String result){
        if ( Log.getInstance().isEnabledFor(Constants.LOG_TRANSCRIPTS_CATEGORY, org.apache.log4j.Priority.DEBUG) ) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("REQUEST: [");
            buffer.append(Tools.nowLocale());
            buffer.append("]\r\n------------------------------------");
            buffer.append(ServletTools.dumpRequestInfo(m_request, false, "#FFAD00", null));
            buffer.append("\r\n------------------------------------");
            buffer.append(result);
            buffer.append("\r\n------------------------------------\r\n");
            Log.debug(Constants.LOG_TRANSCRIPTS_CATEGORY, buffer.toString());
        }
    }


    //================ main ===================================

    public static void main(String [] args){
        try{
            com.dynamide.resource.ResourceManager.createStandalone();
            Opts opts = new Opts(args);
            if ( opts.getOptionBool("-transcript") ) {
                (new DynamideHandler()).logInfo("started");
                Log.debug(Constants.LOG_TRANSCRIPTS_CATEGORY,
                      "If this were a transcript, and "+Constants.LOG_TRANSCRIPTS_CATEGORY+" were set to DEBUG, it would be logged.");
                System.exit(0);
            }
            if ( opts.getOptionBool("-load") ) {
                DynamideHandler h;
                for (int j=0; j < 3; j++) {
                    //for (int i=0; i < 3; i++) {
                        h = new DynamideHandler();
                        //h.setDebugURL("/dynamide/admin?USER=laramie&SESSIONID=test"+j+"."+i);
                        h.setDebugURL("/swarm?USER=laramie&SESSIONID=test"+j);
                       //(new Thread(h)).start();
                        h.run();
                        h=null;

                    //}
                    try {
                        Thread.sleep(500);
                    } catch (Exception e)  {
                    }
                }

                try {
                    Thread.sleep(8000);
                } catch (Exception e)  {
                }
                ResourceManager root = ResourceManager.getRootResourceManager();
                IContext sessions = (IContext)root.find("/homes/dynamide/sessions"); //%%
                String dump = ResourceManager.dumpContext(sessions, "    ", false);
                System.out.println("sessions: "+dump);

                Vector v = new Vector();

                Iterator it = sessions.getAttributes().values().iterator();   //change this to getContexts when you make Session IContext.
                while ( it.hasNext() ) {
                    Session target = (Session)it.next();
                    v.add(target);
                }

                Iterator it2 = v.iterator();
                while ( it2.hasNext() ) {
                    Session target = (Session)it2.next();
                    System.out.println("Test case closing Session: "+target.toString());
                    target.close();
                    target = null;
                }

                try {
                    Thread.sleep(8000);
                } catch (Exception e)  {
                }
                sessions = (IContext)root.find("/homes/dynamide/sessions"); //%%
                dump = ResourceManager.dumpContext(sessions, "    ", false);
                System.out.println("sessions, after: "+dump);
                System.gc();
                try {
                    Thread.sleep(8000);
                } catch (Exception e)  {
                }
                //dump = root.dumpRootContext();
                //System.out.println("root, after: "+dump);
                System.gc();
                try {
                    Thread.sleep(8000);
                } catch (Exception e)  {
                }


            }
        } catch (Exception e){
            System.out.println("Usage: DynamideHandler -test");
            System.out.println("exception: "+e);
        }
    }


}
