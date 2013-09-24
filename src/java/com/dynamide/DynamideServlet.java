package com.dynamide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webmacro.Template;
import org.webmacro.WM;
import org.webmacro.WebMacro;
import org.webmacro.servlet.WebContext;

import com.dynamide.resource.ResourceManager;
import com.dynamide.security.DynamideSecurityManager;
import com.dynamide.util.Log;
import com.dynamide.util.Profiler;
import com.dynamide.util.Tools;

//new

public class DynamideServlet extends HttpServlet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DynamideServlet() {
    }

    private static long gWorkerThreadCount = 1;
    private static long gWorkerThreadGroupCount = 1;

    private static final boolean DEBUG_SERVLET = false;

    public String dumpServletInfo(ServletContext ctx) {
        String res = ""//"servlet name: " + config.getServletName()
                + "\r\nservlet context: " + ctx.getServletContextName() + "\r\nservlet init names:";
        Enumeration en = ctx.getInitParameterNames();
        while (en.hasMoreElements()) {
            String key = "" + en.nextElement();
            res = res + "\r\n" + key;//+":"+ctx.getInitParameter(key);
        }
        res = res + "\r\nservlet attribute names: ";
        Enumeration ena = ctx.getAttributeNames();
        while (ena.hasMoreElements()) {
            String keya = "" + ena.nextElement();
            res = res + "\r\n" + keya;//+":"+ctx.getAttribute(keya);
        }
        return res;
    }

    public void init(ServletConfig config) throws ServletException {
        try {
            super.init(config);

            if (_wm == null) {
                _wm = new WM(this);
            }

            if (DEBUG_SERVLET) {
                System.out.println("\r\n==================================\r\n     init \r\n==============================\r\n");
                System.out.println("\r\n==================================\r\n     getInitParameter \r\n==============================\r\n"
                                         +config.getInitParameter("DYNAMIDE_HOME"));

                System.out.println("\r\n***** DynamideServlet init() *****\r\n" + Tools.now() + dumpServletInfo(config.getServletContext()) + "\r\n");

                ServletContext context = getServletContext();

                System.out.println("INIT_STATE var: " + context.getAttribute(DynamideServlet_INIT_STATE));
                System.out.println(dumpServletInfo(context));
               // context.setAttribute(DynamideServlet_INIT_STATE, STATE_PENDING);
                System.out.println(dumpServletInfo(context));
                System.out.println("INIT_STATE var: " + context.getAttribute(DynamideServlet_INIT_STATE));
                System.out.println("CONTEXTP var: " + context.getInitParameter("CONTEXTP"));
            }
            checkInit();
            Log.debug(DynamideServlet.class, "DynamideServlet init() complete");
            if (DEBUG_SERVLET) {
                System.out.println("\r\n***** DynamideServlet init() complete *****\r\n");
                System.out.println("\r\n==================================\r\n     done init \r\n==============================\r\n");
            }
        } catch (Throwable t) {
            try {
                Log.error(DynamideServlet.class, "ERROR in DynamideServlet.init()", t);
            } catch (Throwable t2) {
                System.err.println("ERROR: [104] Couldn't log this error: " + t + " because of this error: " + t2);
            }
        }
    }

    public void destroy() {
        System.out.println("\r\n\r\n\r\n\r\n\r\n\r\n\r\n==================================\r\n     Servlet destroy \r\n==============================\r\n\r\n\r\n\r\n\r\n\r\n");
    }

    private static final Integer STATE_ZERO = new Integer(0);
    private static final Integer STATE_PENDING = new Integer(1);
    private static final Integer STATE_DONE = new Integer(2);

    private static final String DynamideServlet_INIT_STATE = "DynamideServlet.INIT_STATE";

    private Integer getInitState(ServletContext ctx) {
        Object o = ctx.getAttribute(DynamideServlet_INIT_STATE);
        if (o != null) {
            return (Integer) o;
        }
        return STATE_ZERO;
    }

    private synchronized void checkInit() throws ServletException {
        if (DEBUG_SERVLET) System.out.println("==== checkInit. " + "\r\nclassloader: " + this.getClass().getClassLoader() + "\r\nsystemclassloader: " + ClassLoader.getSystemClassLoader());
        // IMPORTANT: keep this in sync with com.dynamide.Main.init(),
        // which does the equivalent things for command-line apps.
        ServletContext context = getServletContext();
        Integer state = getInitState(context);
        if (state == STATE_DONE) {
            return;
        }
        if (state == STATE_PENDING) {
            try {
                while (getInitState(context) != STATE_DONE) {
                    Thread.sleep(500);
                }
                return;
            } catch (Exception te) {
                System.out.println("Error sleeping in DynamideServlet.checkInit");
            }
        }
        context.setAttribute("Dynamide.INIT_STATE", STATE_PENDING);

        try {
            DynamideSecurityManager.checkSecurityManagerInit();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        //1/7/2005 5:14PM found a leak.  Disable for now.
        //ensure that com.dynamide.Constants.PROFILE = false

        ResourceManager rootResourceManager = (ResourceManager) context.getAttribute("DynamideRootResourceManager");
        if (rootResourceManager == null) {
            String DYNAMIDE_HOME = getInitParameter(Constants.DYNAMIDE_HOME);
            String DYNAMIDE_RESOURCE_ROOT = getInitParameter(Constants.DYNAMIDE_RESOURCE_ROOT);
            String DYNAMIDE_STATIC_ROOT = getInitParameter(Constants.DYNAMIDE_STATIC_ROOT);
            String DYNAMIDE_CONTEXT_CONF = getInitParameter(Constants.DYNAMIDE_CONTEXT_CONF);


            if (DEBUG_SERVLET) {
                System.out.println("a @@@@@@@ init param: DYNAMIDE_HOME: " + DYNAMIDE_HOME);
                System.out.println("a @@@@@@@ init param: DYNAMIDE_RESOURCE_ROOT: " + DYNAMIDE_RESOURCE_ROOT);
                System.out.println("a @@@@@@@ init param: DYNAMIDE_STATIC_ROOT: " + DYNAMIDE_STATIC_ROOT);
                System.out.println("a @@@@@@@ init param: v: DYNAMIDE_CONTEXT_CONF" + DYNAMIDE_CONTEXT_CONF);
            }
            if (DYNAMIDE_HOME != null && DYNAMIDE_HOME.indexOf("$") > -1) {
                String msg = "ERROR. Expansion of the character '$' is not supported from servlet in init param: DYNAMIDE_HOME: " + DYNAMIDE_HOME;
                System.out.println(msg);
                throw new ServletException(msg);
            }
            try {
                //This next line also calls ResourceManager.configureLog()
                rootResourceManager = ResourceManager.installSingletonRootResourceManager(DYNAMIDE_RESOURCE_ROOT, DYNAMIDE_CONTEXT_CONF, DYNAMIDE_STATIC_ROOT);
                rootResourceManager.setStaticRoot(DYNAMIDE_STATIC_ROOT);
                rootResourceManager.setDynamideContextConf(DYNAMIDE_CONTEXT_CONF);
                rootResourceManager.startLoadOnStartupApps();

                //log is now initialized with correct format string.
                context.setAttribute("DynamideRootResourceManager", rootResourceManager);

                Log.info(DynamideServlet.class, "DYNAMIDE_HOME: " + DYNAMIDE_HOME);
                Log.info(DynamideServlet.class, "DYNAMIDE_RESOURCE_ROOT: " + (String) rootResourceManager.getAttribute("RESOURCE_ROOT"));
                Log.info(DynamideServlet.class, "DYNAMIDE_STATIC_ROOT: " + rootResourceManager.getStaticRoot());
                Log.info(DynamideServlet.class, "ResourceManager.getStaticPrefix: " + ResourceManager.getStaticPrefix());
                Log.info(DynamideServlet.class, "ResourceManager.ID: " + rootResourceManager.getID());

            } catch (Exception rme) {
                Log.error(DynamideServlet.class, "error in checkInit", rme);
                throw new ServletException(rme);
            }
            initConfParameters();
        }
        context.setAttribute("Dynamide.INIT_STATE", STATE_DONE);
        if (DEBUG_SERVLET) System.out.println(dumpServletInfo(context));
    }

    public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        try {
            doGetPost(req, response);
        } catch (Throwable t) {
            Log.error(DynamideServlet.class, "in doGet()", t);
            printResult(response, "text/plain", "Error in DynamideServlet.");
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        try {
            doGetPost(req, response);
        } catch (Throwable t) {
            Log.error(DynamideServlet.class, "in doPost()", t);
            printResult(response, "text/plain", "Error in DynamideServlet.");
        }
    }

    private static final boolean PARANOID_GC = false;  //do not turn this on in jdk 1.4 esp. on Solaris, it makes GC be concurrent!

    public void doGetPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Log.debug(DynamideServlet.class, "DynamideServlet classloader >>>>>>>>>>>>>>>>>> "+getClass().getClassLoader().getClass().getName());
        ResourceManager rm1 = ResourceManager.getRootResourceManager();
        String rmid = "";
        if (rm1 != null) {
            rmid = rm1.getID();
        }
        if (DEBUG_SERVLET)
            System.out.println("\r\n==================================\r\n     doGetPost " + rmid + "\r\n==============================\r\n");

        long startDoGetPost = Tools.now().longValue();
        String fullURI = request.getRequestURI();
        //System.out.println("servlet.doGetPost: "+fullURI);
        com.dynamide.util.Profiler profiler = null;
        boolean PROFILE = com.dynamide.Constants.PROFILE;
        if (PROFILE) {
            profiler = com.dynamide.util.Profiler.getThreadSharedProfiler();
            profiler.enter("servlet.doGetPost: " + fullURI);
        }
        try {
            if (Constants.DEBUG_OBJECT_LIFECYCLE) com.dynamide.util.Profiler.getSharedProfiler().outputObjects();
            //com.dynamide.util.Profiler.clearMemoryMonitor();

            ServletContext context = getServletContext();

            // %% kludge:
            ResourceManager rootResourceManager = (ResourceManager) context.getAttribute("DynamideRootResourceManager");

            //Move these to the admin application.
            String servletAction = request.getParameter("servletAction");
            if (servletAction != null) {
                if (servletAction.equals("dumpInit")) {
                    response.setContentType("text/html");
                    PrintWriter out = response.getWriter();
                    try {
                        dumpInitParameters(out);
                        dumpRootResourceManager(rootResourceManager, out);
                        com.dynamide.util.JNDIWalk.dump(getServletContext(), out);
                        //dumps to std-out: dumpThreadGroups(out);
                    } finally {
                        out.close();
                    }
                    return;
                } else if (servletAction.equals("webmacroTest")) {
                    doWebmacroTest(request, response);
                    return;
                } else if (servletAction.equals("beanshellTest")) {
                    doBeanshellTest(request, response);
                    return;
                } else if (servletAction.equals("dumpThreadGroups")) {
                    dumpThreadGroups();
                    printResult(response, "text/html", "OK: see std-out");
                    return;
                } else if (servletAction.equals("dumpRequestInfo")) {
                    printResult(response, "text/html", com.dynamide.util.ServletTools.dumpRequestInfo(request, true, "#FFAD00"));
                    return;
                } else if (servletAction.equals("shutdown")) {
                    String ALLOW_SHUTDOWN = getInitParameter("ALLOW_SHUTDOWN");
                    if (ALLOW_SHUTDOWN != null && ALLOW_SHUTDOWN.equals("true")) {
                        printResult(response, "text/html", "<H1>Server shutdown at " + Tools.now() + "</H1>");
                        System.exit(0);
                    } else {
                        printResult(response, "text/html", "<H1>Shutdown denied</H1>");
                        return;
                    }
                } else if (servletAction.equals("reloadSettings")) {
                    try {
                        ResourceManager.getRootResourceManager().reloadSettings();
                        printResult(response, "text/html", "<H1>OK</H1>");
                    } catch (Exception e) {
                        printResult(response, "text/html", "<html><body><h2>Settings could not be reloaded: exception: " + e + "</h2></body></html>");
                    }
                    return;
                } else if (servletAction.equals("reinitHome")) {
                    try {
                        ResourceManager.getRootResourceManager().reinitHome(request.getParameter("account"));
                        printResult(response, "text/html", "<H1>OK</H1>");
                    } catch (Exception e) {
                        printResult(response, "text/html", "<H1>ERROR</H1>" + e.toString());
                    }
                    return;
                } else if (servletAction.equals("dumpAddedContexts")) {
                    printResult(response, "text/html", ResourceManager.getRootResourceManager().dumpAddedContexts());
                    return;
                } else if (servletAction.equals("dumpRootContext")) { //%%%%% This should be protected, in the super-user app only.
                    printResult(response, "text/html", ResourceManager.getRootResourceManager().dumpRootContextPage());
                    return;
                } else if (servletAction.equals("dumpContext")) { //%%%%% This should be protected, in the super-user app only.
                    String search = request.getParameter("search");
                    search = search == null ? "" : search;
                    String page = ResourceManager.getRootResourceManager().dumpContext(search, "    ", true);
                    page = "<html><title>Dynamide ResourceManager context: " + search + "</title><body><pre>" + page + "</pre></body></html>";
                    printResult(response, "text/html", page);
                    return;
                } else if (servletAction.equals("printTimestamp")) {
                    String ts = "" + Tools.nowLocale();
                    String ots = "\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n" + ts;
                    printResult(response, "text/html", ts);
                    System.out.println(ots);
                    return;
                } else if (servletAction.equals("reconfigureLog")) {
                    try {
                        ResourceManager.getRootResourceManager().reconfigureLog();
                        printResult(response, "text/html", "<html><body><h2>Log Reloaded</h2></body></html>");
                    } catch (Exception e) {
                        printResult(response, "text/html", "<html><body><h2>Log could not be reloaded: exception: " + e + "</h2></body></html>");
                    }
                    return;
                }
            }

            if (PROFILE) profiler.enter("servlet.create/initHandler");
            DynamideHandler handler = new DynamideHandler();
            HandlerResult result;  // = new HandlerResult("ERROR: [105] thread didn't complete [DynamideServlet]");
            handler.init(request, response, this); //%% maybe allow a more user-centric data dir.
            if (PROFILE) profiler.leave("servlet.create/initHandler");

            //Thread thr = ResourceManager.createThread(handler);
            ThreadGroup threadGroup = new ThreadGroup("DynamideWorkerThreadGroup" + (gWorkerThreadGroupCount++));
            try {
                Thread thr = new Thread(threadGroup, handler, "DynamideWorkerThread" + (gWorkerThreadCount++));
                //System.out.println("dumpThreadGroups before starting worker: ");
                //dumpThreadGroups(response.getWriter());
                if (PROFILE) profiler.log("servlet.starting handler thread");
                thr.start();
                long now;
                long timeout = 1000 * 360;  //%% hardcoded to 3 minutes for now.  change this.
                long start = Tools.now().longValue();
                while (!handler.finished) {
                    //if (PROFILE) profiler.log("servlet.looping");
                    now = Tools.now().longValue();
                    if (now - start > timeout) {
                        break;
                    }
                    try {
                        Thread.sleep(10); //or, waitFor()...
                    } catch (InterruptedException iex) {
                    }
                }
                if (PROFILE) profiler.log("servlet.looping DONE");
                if (handler.finished) {
                    result = handler.getResult();//handleHttpRequest(request);   //todo: result.mimeType, result.forward, result.htmlsrc etc.
                } else {
                    result = new HandlerResult("TIMEOUT");
                }

                if (result.expires > 0) {
                    now = Tools.now().longValue();
                    response.setDateHeader("Expires", now + result.expires);
                    //See com.dynamide.DynamideHandler where cached files get expires set.
                }
                //reponse.setDateHeader(result.getDateHeader());
                if (result.notModified) {
                    response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                } else if (result.redirectURL.length() > 0) {
                    String url = result.redirectURL;
                    Log.debug(DynamideServlet.class, "Sending redirect: " + url);
                    response.sendRedirect(url);
                } else if (result.binary) {
                    if (com.dynamide.Constants.PROFILE) profiler.enter("servlet.writeResult: " + fullURI);
                    writeResult(response, result.mimeType, result.binaryResult);
                    if (com.dynamide.Constants.PROFILE) profiler.leave("servlet.writeResult: " + fullURI);
                } else if (result.getResponseCode() > 0) {
                    response.sendError(result.getResponseCode(), result.getErrorMessage());
                } else {
                    if (com.dynamide.Constants.PROFILE) profiler.enter("servlet.printResult: " + fullURI);
                    printResult(response, result.mimeType, result.result);
                    if (com.dynamide.Constants.PROFILE) profiler.leave("servlet.printResult: " + fullURI);
                }

            } finally {
                handler = null;
                //System.out.println("threadGroup.activeCount: "+threadGroup.activeCount());
                try {
                    if (PARANOID_GC) {
                        System.gc();
                        System.runFinalization();
                        Thread.yield();
                    }

                    if (threadGroup.isDestroyed()) {
                        System.out.println("********** threadgroup already destroyed: " + threadGroup.toString());
                    } else {
                        if (threadGroup.activeCount() == 0 && threadGroup.activeGroupCount() == 0) {
                            threadGroup.destroy();
                        } else {
                            System.out.println("********** threadgroup not ready to be destroyed: " + threadGroup.toString() + " count:" + threadGroup.activeCount() + " groupCount:" + threadGroup.activeGroupCount());

                            System.out.println("---------list Thread.currentThread -------");
                            Thread.currentThread().getThreadGroup().list();
                            System.out.println("---------list-------");
                            threadGroup.list();
                            System.out.println("---------list END-------");
                        }
                    }
                } catch (IllegalThreadStateException itse) {
                    System.out.println("****** in DynamideServlet.doGetPost destroying threadGroup: IllegalThreadStateException: " + itse);
                }
            }
            threadGroup = null;
            if (PARANOID_GC) {
                System.gc();
                System.runFinalization();
                Thread.yield();
            }
            //System.out.println("dumpThreadGroups after, leaving servlet");
            //dumpThreadGroups(response.getWriter());
        } finally {
            if (com.dynamide.Constants.PROFILE) {
                profiler.leave("servlet.doGetPost: " + fullURI);
                Log.debug(DynamideServlet.class, "\r\n" + profiler.getOutputString());
                long now = Tools.now().longValue();
                Log.debug(DynamideServlet.class, "millis since last request complete: " + (now - lastRequestAt));
                lastRequestAt = now;
                Profiler.releaseThreadSharedProfiler();
            }
            if (com.dynamide.Constants.PROFILE) {
                //separate so that you can just do this one liner per resource.
                Log.debug(DynamideServlet.class, fullURI + " in " + (Tools.now().longValue() - startDoGetPost) + " millis");
            }
        }
    }

    private static long lastRequestAt = 0;

    private void printResult(HttpServletResponse response, String contentType, String result) throws IOException {
        //response.setContentType("text/html");
        response.setContentType(contentType);
        PrintWriter out = response.getWriter();
        try {
            out.print(result);
        } finally {
            out.close();
        }
    }

    private void writeResult(HttpServletResponse response, String contentType, byte[] result) throws IOException {
        response.setContentType(contentType);
        ServletOutputStream out = response.getOutputStream();
        try {
            out.write(result);
        } finally {
            out.close();
        }
    }

    private void initConfParameters() {
    }

    private void dumpRootResourceManager(ResourceManager rm, PrintWriter pw) {
        pw.println("<h1>Dynamide ResourceManager Root Context:</h1>");
        pw.println("<pre>" + rm.dumpRootContext(true) + "</pre>");
    }

    private void dumpInitParameters(PrintWriter pw) {
        Enumeration e = getInitParameterNames();

        if (!e.hasMoreElements()) {
            return;
        }

        pw.println("<h1>Servlet Init Parameters:</h1>");
        pw.println("<table border='1' cellpadding='0' cellspacing='0'>");

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            pw.print("<tr><td>");
            pw.print(name);
            pw.print("</td><td>");
            pw.println(getInitParameter(name));
            pw.print("</td></tr>");
        }

        pw.println("</table>");
    }

    private void dumpThreadGroups() {
        //System.out.println("list of current Thread: ");
        //Thread.currentThread().getThreadGroup().list();
        ThreadGroup grp, parent;
        grp = Thread.currentThread().getThreadGroup();
        while ((parent = grp.getParent()) != null) {
            grp = parent;
        }
        System.out.println("-----list of top parent Thread:-----");
        grp.list();
    }

    private WebMacro _wm = null;

    private void doWebmacroTest(HttpServletRequest req, HttpServletResponse resp) {
        try {
            try {
                // create a context for the current request
                WebContext c = _wm.getWebContext(req, resp);

                // fill up the context with our data
                c.put("Today", new Date());
                c.put("Number", new Long(23));

                // WebContext provides some utilities as well
                String other = c.getForm("other");
                if (other == null) {
                    c.put("hello", "hello again!"); // put this into the hash
                } else {
                    c.put("hello", other);          // else put this in
                }

                // get the template we intend to execute
                Template t = _wm.getTemplate("standalone.wm");

                // write the template to the output, using our context
                t.write(resp.getOutputStream(), resp.getCharacterEncoding(), c);
            } catch (org.webmacro.ResourceException e) {
                resp.getWriter().write("ERROR!  Could not locate template standalone.wm, check that your template path is set properly in WebMacro.properties");
            } catch (org.webmacro.ContextException e) {
                resp.getWriter().write("ERROR!  Could not locate required data in the Context.");
            }
        } catch (java.io.IOException e) {
            // what else can we do?
            System.out.println("ERROR: IOException while writing to servlet output stream.");
        }
    }

    private void doBeanshellTest(HttpServletRequest req, HttpServletResponse response) throws IOException {
        try {
            //String script = "System.out.println(\"Hello, DynamideServlet Bsh test.\");";
            String script = "System.out.println(\"Hello, DynamideServlet Bsh test.\");com.dynamide.Session.createSession(\"/dynamide/doco\");";
            StringBuffer scriptOutput = new StringBuffer();
            Object scriptResult = evalScript(script, scriptOutput);
            printResult(response, "text/html", scriptOutput.toString());
        } catch (Exception e) {
            printResult(response, "text/html", "error: " + e.toString());
        }
    }

    private void doBeanshellTestStandalone() throws IOException {
        try {
            String script = "System.out.println(\"Hello, DynamideServlet Bsh test.\");com.dynamide.Session.createSession(\"/dynamide/doco\");";
            StringBuffer scriptOutput = new StringBuffer();
            Object scriptResult = evalScript(script, scriptOutput);
            System.out.println(scriptOutput.toString());
        } catch (Exception e) {
            System.out.println("error: " + e.toString());
        }
    }

    private Object evalScript(String script, StringBuffer scriptOutput) throws Exception {
        // Create a PrintStream to capture output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(baos);

        // Create an interpreter instance with a null inputstream,
        // the capture out/err stream, non-interactive
        bsh.Interpreter bsh = new bsh.Interpreter(null, pout, pout, false);

        // Eval the text, gathering the return value or any error.
        Object result = null;
        String error = null;
        // Eval the user text
        result = bsh.eval(script);
        scriptOutput.append(baos.toString());
        return result;
    }

    /*
    public static void main(String args[]) throws Exception {
        String RESOURCE_ROOT = "C:/dynamide/build/ROOT";
        com.dynamide.resource.ResourceManager.installSingletonRootResourceManager(RESOURCE_ROOT, DYNAMIDE_CONTEXT_CONF);

        DynamideServlet s = new DynamideServlet();
        s.doBeanshellTestStandalone();  //this throws an exception from bsh 2.04b1 because classloader  loads classes separately. Though, it works when run as a servlet in tomcat!  arg?!?!?!?
    }
     */

}
