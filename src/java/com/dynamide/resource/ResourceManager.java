package com.dynamide.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.JDOMException;

import com.bitmechanic.sql.ConnectionPool;
import com.bitmechanic.sql.ConnectionPoolManager;
import com.dynamide.ApplicationNotFoundException;
import com.dynamide.Constants;
import com.dynamide.DynamideException;
import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.resource.ILogger;
import com.dynamide.Session;
import com.dynamide.SessionBusyException;
import com.dynamide.security.DynamideSecurityManager;
import com.dynamide.util.FileTools;
import com.dynamide.util.Log;
import com.dynamide.util.Opts;
import com.dynamide.util.SessionDatabase;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;
import com.dynamide.util.XDB;
import com.dynamide.widgetbeans.TreeBean;

import javax.servlet.ServletException;

//jdbcpool:





/**
 * The ResourceManager is required by all Dynamide applications; the servlet calls installSingletonRootResourceManager()
 * while command line applications should call createStandalone().
 *
 * The ResourceManager holds an instance of the SessionPool in its own pool.
 * You need to have the correct permissions to be able to get the SessionPool, or to ask the SessionPool for a Session.
 * SessionPool extends Pool.
 */
public final class ResourceManager extends Pool {


    public final static String CAT_CACHE = "com.dynamide.resource.ResourceManager.Caching";
    protected static boolean LOG_CACHING = false;

    public final void logCaching(String msg){logDebug(CAT_CACHE, "RM."+msg);}

    public final static String RESOURCE_ROOT_KEY = "RESOURCE_ROOT";

    // These can also be set to "/cache"
    public final static String CACHE = "/dynamide/cache";                                   // %% todo: look CACHE up in properties file.
    public final static String CACHE_URI = "/dynamide/cache";
    //this can also be set to "cache" if CACHE_URI is "/cache"
    public final static String CACHE_ICONTEXT_NAME = "dynamide/cache";       // %% todo: look CACHE_ICONTEXT_NAME up in properties file.
    public final static String CACHE_IS_PUBLISH_CACHE = "IS_PUBLISH_CACHE";
    public final static String CACHE_PREFIX = "cachePrefix";
    public final static String CACHE_PREFIX_URI = "cachePrefixURI";
    public final static String SESSIONS_POOLED_NAME = "sessions-pooled";

    /** Call isCacheOn() to see if caching is on -- that is, disk won't be checked
     *  for objects once they are in cache.
     */
    public final static String CACHE_ON = "CACHE_ON";

    public static final String ROOT_KEY = "root.ResourceManager";

    private static String g_errorlogsDir = "";
    public String getErrorLogsDir(){
        return g_errorlogsDir;
    }

    public static String ERROR_LOGS_REL = "errorlogs";
    public static String getErrorLogsURI(){
        return getStaticPrefix()+'/'+ERROR_LOGS_REL;
    }

    private static ResourceManager _singleton = null;
    private static JobMasterThread m_jobMasterThread = null;

    private ResourceManager () throws Exception {
        this(null);
        throw new Exception("FORBIDDEN");
    }

    protected ResourceManager (DynamideObject owner) {
        super(owner);
    }


    /** This is specifically denied.
     * @throws SecurityException
     */
    public Object clone(){
        throw new SecurityException();
    }

    /**
      *  @dynamide.factorymethod
      */
    public static ResourceManager installSingletonRootResourceManager(String RESOURCE_ROOT,
                                                                                                          String DYNAMIDE_CONTEXT_CONF,
                                                                                                          String DYNAMIDE_STATIC_ROOT)
                                                                                                         throws Exception {
        return installSingletonRootResourceManager(RESOURCE_ROOT,DYNAMIDE_CONTEXT_CONF, DYNAMIDE_STATIC_ROOT, Constants.LOGCONF_DYNAMIDE);
    }

    private static void requireValidRESOURCE_ROOT(String resource_root) throws ServletException {
              if (StringTools.isEmpty(resource_root)) {
                String msg = "DYNAMIDE_RESOURCE_ROOT was not specified." ;
                System.err.println(msg);
                throw new ServletException(msg);
            }
         if ( ! FileTools.directoryExists(resource_root)) {
                String msg = "DYNAMIDE_RESOURCE_ROOT directory specified does not exist ==>"+resource_root+"<==" ;
                System.err.println(msg);
                throw new ServletException(msg);
            }
    }

    /**
      *  @dynamide.factorymethod
      */
    public static synchronized ResourceManager installSingletonRootResourceManager(String RESOURCE_ROOT,
                                                                                                                              String DYNAMIDE_CONTEXT_CONF,
                                                                                                                              String DYNAMIDE_STATIC_ROOT,
                                                                                                                              String logConfFilename)
    throws Exception {
        if ( _singleton == null ) {
            requireValidRESOURCE_ROOT(RESOURCE_ROOT);

            configureLog(RESOURCE_ROOT, logConfFilename);

            //_singleton = createResourceManager(null);
            _singleton = new ResourceManager(null);
            _singleton.initAsRootContext(RESOURCE_ROOT, DYNAMIDE_CONTEXT_CONF, DYNAMIDE_STATIC_ROOT);

            //this code can throw a org.webmacro.InitException
            org.webmacro.WM webmacro = new org.webmacro.WM("com/dynamide/conf/WebMacroDynamide.properties"); //%% todo -- figure out how to move to RESOURCE_ROOT/conf
            //org.webmacro.Broker broker = webmacro.getBroker();
            //System.out.println("=========== broker.EE "+broker.getEvaluationExceptionHandler());
            _singleton.rebindAttribute("Webmacro-Singleton", webmacro);
            //wm2.01b broker.startClient();  //pump up the ref count one. HACK. Without this, the ref count internal to webmacro broker drops to zero,
                                   // and they expect that you will re-read the config file each time to re-initialize the broker.
            //Log.debug(ResourceManager.class, "Created Root Context: \r\n"+rm.dumpRootContext());
        }
        return _singleton;
    }

    public static void shutdown(){
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [3.1]");
        }
        SessionDatabase.getSharedDB().shutdown();
        if (m_jobMasterThread != null) m_jobMasterThread.terminated = true;
        _singleton = null;
    }

    /** Use this when you wish to simply run from the command line (using the environment var or java
     *  system property named by Constants.RESOURCE_ROOT_ENV for the RESOURCE_ROOT), and not in a web framework.
     *  NOTE: once you do this, you must explicitly call System.exit(code) when you are done,
     *  because the ResourceManager singleton hangs around in memory.
     *
     *  @dynamide.factorymethod
     */
    public static ResourceManager createStandalone()
    throws Exception {
        String home = getDynamideHomeFromEnv();
        Properties props = readPropertiesFromHome(home);
        return createStandalone( (String)props.get("DYNAMIDE_RESOURCE_ROOT"),
                                    (String)props.get("DYNAMIDE_CONTEXT_CONF"),
                                    (String)props.get("DYNAMIDE_STATIC_ROOT"),
                                    Constants.LOGCONF_DYNAMIDE_JUNIT);
    }

    /**
      *  @dynamide.factorymethod
      */
    public static ResourceManager createStandalone(String resourceRoot)
    throws Exception {
        return createStandalone(resourceRoot,
                                            resourceRoot+Constants.CONF_CONTEXT_REL,
                                            resourceRoot+Constants.STATIC_DIR_REL,
                                            Constants.LOGCONF_DYNAMIDE);
    }

    /** Use this when you wish to simply run from the command line, and not in a web framework.
     *  NOTE: once you do this, you must explicitly call System.exit(code) when you are done,
     *  because the ResourceManager singleton hangs around in memory.
     *
     *  @param resourceRoot would be something like: "C:/dynamide/build/DYNAMIDE_RESOUCE_ROOT"
     *  @dynamide.factorymethod
     */
    public static ResourceManager createStandalone(String resourceRoot, String contextFilename, String staticRoot, String logConfFilename)
    throws Exception {
        com.dynamide.security.DynamideSecurityManager.checkSecurityManagerInit();
        ResourceManager rm = installSingletonRootResourceManager(resourceRoot, contextFilename, staticRoot, logConfFilename);
        return rm;
    }

    /** Use this when running in JUnit
     * @dynamide.factorymethod
     */
    public static void  createStandaloneForTest()
    throws Exception {
        String home = getDynamideHomeFromEnv();
        Properties props = readPropertiesFromHome(home);
        createStandalone( (String)props.get(Constants.DYNAMIDE_RESOURCE_ROOT),
                                   (String)props.get(Constants.DYNAMIDE_CONTEXT_CONF),
                                   (String)props.get(Constants.DYNAMIDE_STATIC_ROOT),
                                   Constants.LOGCONF_DYNAMIDE_JUNIT);
    }

    /** @return null if not initialized by a call to installSingletonRootResourceManage()
     */
    public static ResourceManager getRootResourceManager()
    throws SecurityException {
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            // %%%%%%%%%%%%%%%%%%%%%%%%%% come up with a way to do this.  for now, don't throw it: throw new SecurityException("Forbidden [1]");
            // the app should either have its own resource mananger, or ResourceManager should decide on each call if the caller can get to the context.
        }
        if ( _singleton == null ) {
            throw new SecurityException("ResourceManager not initialized.  If you are running outside of the DynamideServlet, you should call one of the ResourceManager factory methods.");
        }
        return _singleton;
    }

    public static void configureLog(String RESOURCE_ROOT, String logConfFilename)
    throws Exception {
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [9]");
        }
        System.out.println("configureLog(\""+RESOURCE_ROOT+"\", \""+logConfFilename+"\") called");
        String logconf;
        if (logConfFilename.length()>0){
             if (RESOURCE_ROOT == null || RESOURCE_ROOT.length() ==  0){
                logconf = Tools.fixFilename(getResourceRootFromEnv()+'/'+logConfFilename);
             } else {
                logconf = Tools.fixFilename(RESOURCE_ROOT+'/'+logConfFilename);
             }
        } else {
            if (RESOURCE_ROOT == null || RESOURCE_ROOT.length() ==  0){
                logconf = Tools.fixFilename(getResourceRootFromEnv()+'/'+Constants.LOGCONF_DYNAMIDE);
            } else {
                logconf = Tools.fixFilename(RESOURCE_ROOT+'/'+Constants.LOGCONF_DYNAMIDE);
            }
        }
        System.out.println("attempting to use log4j conf file: "+logconf);
        try {
            com.dynamide.util.Log.configure(logconf);
            //org.apache.log4j.Category.getRoot().setPriority(org.apache.log4j.Priority.INFO);
            //System.out.println("com.dynamide.resource.ResourceManager: log.conf file: "+logconf+" initialized at: "+Tools.nowLocale());
            com.dynamide.util.DynamideLogAppender.installAppender();
            LOG_CACHING = ( Log.getInstance().isEnabledFor(Constants.LOG_CACHING_CATEGORY, org.apache.log4j.Priority.DEBUG) );
        } catch (Throwable t){
            System.out.println("ERROR: [66] couldn't properly configure log4j: "+t);
        }
    }

    /** Use this when running in JUnit */
    public static void configureLogForTest()
    throws Exception {
        configureLog(getResourceRootFromEnv(), Constants.LOGCONF_DYNAMIDE_JUNIT);
    }

    public void reconfigureLog()
    throws Exception {
        String root = (String)getAttribute("RESOURCE_ROOT");
        if ( root != null ) {
            configureLog(root, "");
        }
    }

    /** All pretty benign stuff in here.  Won't damage anything to call this again, just reloads the
     * log.conf file, etc.  You must not be in a worker thread, however.
     */
    public void reloadSettings()
    throws Exception {
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [8]");
        }
        configureLog((String)getAttribute("RESOURCE_ROOT"), "");
        reloadContextFile();
        configureErrorlogs();
        resetJobMasterThread();
    }

    private void configureErrorlogs(){
        try {
            g_errorlogsDir = FileTools.createDirectory(getStaticRoot()+'/'+ERROR_LOGS_REL).getCanonicalPath();
            FileTools.cleanOneDir(g_errorlogsDir, "", "");
            logDebug("ResourceManager cleaning errorlogs directory: "+g_errorlogsDir);
            checkErrorLogger();
        } catch (Exception e)  {
            System.out.println("Couldn't create errorlogs directory "+e);
        }
    }

    public void checkMailPermission(String host)
    throws SecurityException {
        //%% todo
        Thread t= Thread.currentThread();
        //if current thread in list of threads that have called to get permission to send mail to this host,
        // then allow.  The list should be built on the actual application id.
    }


    public String getCategoryID(){
        return "com.dynamide.resource.ResourceManager."+getKey();
    }

    public void setKey(String new_value){
        //we need to be sure in dumpContext that we are not the root, which can't be dumped by a worker thread.
        // by extension, it means no one can reset the root resource manager's key id.
        if ( getKey().equals(ROOT_KEY) ){
            throw new SecurityException("Can't reset root key");
        }
        super.setKey(new_value);
    }

        //maybe convert name to a url, and look up resources like that: socket, file, db, ...?
        // or implement a number or methods:
    // ?? public com.dynamide.resource.File getFileResource or java.io.File if you can lock it down properly for rename, etc.

    /** Publicly allow all to see env vars for now.
     */
    public static String getEnvironmentVariable(String key){
        Object obj = System.getProperties().get(key);
        return obj != null ? obj.toString() : "";
    }

    public static String getDynamideHomeFromEnv(){
        return getEnvironmentVariable(Constants.DYNAMIDE_HOME_ENV);
    }

    /** lookup the RESOURCE_ROOT from the dynamide.local.properties file in the ${DYNAMIDE_HOME}/bin
     *  directory, using ${DYNAMIDE_HOME} from the environment.
     */
    public static String getResourceRootFromHome()
    throws Exception {
        String home = getDynamideHomeFromEnv();
        return getResourceRootFromHome(home);
    }

    /** lookup the RESOURCE_ROOT from the dynamide.local.properties file in the home
     *  directory specified.
     * @todo Optimize this so that it is not read every time.
     */
    public static String getResourceRootFromHome(String home)
    throws Exception {
        Properties props = readPropertiesFromHome(home);
        return (String)props.get("DYNAMIDE_RESOURCE_ROOT");
    }

     public static String getStaticRootFromHome()
    throws Exception {
        String home = getDynamideHomeFromEnv();
        return getStaticRootFromHome(home);
    }

    public static String getStaticRootFromHome(String home)
    throws Exception {
        Properties props = readPropertiesFromHome(home);
        return expandVariablesFromLocalProperties(home, props, "DYNAMIDE_STATIC_ROOT");
    }

    private String m_dynamideContextConf = "";

    public void setDynamideContextConf(String m_dynamideContextConf) {
        this.m_dynamideContextConf = m_dynamideContextConf;
    }

    public String getDynamideContextConf()
    throws Exception {
        if (StringTools.notEmpty(m_dynamideContextConf)){
            return m_dynamideContextConf;
        }
        String home = getDynamideHomeFromEnv();
        Properties props = readPropertiesFromHome(home);
        return (String)props.get("DYNAMIDE_CONTEXT_CONF");
    }

    /** <pre>==================================================
    * NOTE: this case is illegal :
    *   ${DYNAMIDE_RESOURCE_ROOT}/foobar/${DYNAMIDE_HOME}/foo         !! ILLEGAL !!
    *
    * These can nest DYNAMIDE_HOME > DYNAMIDE_BUILD > DYNAMIDE_RESOURCE_ROOT
    *  but they can't be in the same definition.
    * One of the levels has a rooted directory.
    * EXAMPLES:
    *  DYNAMIDE_BUILD
    *        ${DYNAMIDE_HOME}/build
    *        /usr/local/dynamide/build
    *
    * DYNAMIDE_RESOURCE_ROOT
    *       ${DYNAMIDE_HOME}/build/dynamide_resource_root
    *       ${DYNAMIDE_BUILD}/dynamide_resource_root
    *       /usr/local/dynamide/build/dynamide_resource_root
    *
    * DYNAMIDE_CONTEXT_CONF
    *      ${DYNAMIDE_HOME}/build/dynamide_resource_root/context.xml
    *      ${DYNAMIDE_BUILD}/dynamide_resource_root/context.xml
    *      ${DYNAMIDE_RESOURCE_ROOT}t/context.xml
    *      /usr/local/dynamide/build/dynamide_resource_root /context.xml
    * ==================================================
    * Expands macros:
    *  DYNAMIDE_BUILD can reference DYNAMIDE_HOME
    *  DYNAMIDE_RESOURCE_ROOT can reference DYNAMIDE_HOME and DYNAMIDE_BUILD
    *  DYNAMIDE_CONTEXT_CONF   can reference DYNAMIDE_HOME and DYNAMIDE_BUILD and DYNAMIDE_RESOURCE_ROOT
   </pre>*/
    public static Properties readPropertiesFromHome(String home)
    throws Exception {
        if (home == null || home.length()==0){
            String errmsg = "getResourceRootFromHome(\""+home+"\") failed. Error: no "+Constants.DYNAMIDE_HOME_ENV+" specified.";
            System.out.println("\r\n    Please set Java variable "+Constants.DYNAMIDE_HOME_ENV+", for example: ");
            System.out.println("       java -D"+Constants.DYNAMIDE_HOME_ENV+"=C:/dynamide <main-class>");
            System.out.println("    or: ");
            System.out.println("       java -D"+Constants.DYNAMIDE_HOME_ENV+"=/usr/local/dynamide <main-class>\r\n\r\n");
            Log.error(ResourceManager.class, errmsg);
            DynamideException de = new DynamideException(errmsg);
            System.out.println("       "+Tools.errorToString(de,  true, true));
            throw de;
        }
        Properties props = FileTools.loadPropertiesFromFile(Tools.fixFilename(home), Constants.DYNAMIDE_LOCAL_PROPERTIES_FILENAME);
        if (props == null){
            String errmsg = "readPropertiesFromHome(\""+home+"\") failed. Error: couldn't find file dynamide.local.properties in directory: '"+home+"'";
            Log.error(ResourceManager.class, errmsg);
            throw new DynamideException(errmsg);
        }

        String dynamide_build = props.getProperty("DYNAMIDE_BUILD");
        dynamide_build = StringTools.searchAndReplaceAll(dynamide_build, "${DYNAMIDE_HOME}", home);
        props.put("DYNAMIDE_BUILD", dynamide_build);

        String dynamide_resource_root = props.getProperty("DYNAMIDE_RESOURCE_ROOT");
        dynamide_resource_root = StringTools.searchAndReplaceAll(dynamide_resource_root, "${DYNAMIDE_BUILD}", dynamide_build);
        dynamide_resource_root = StringTools.searchAndReplaceAll(dynamide_resource_root, "${DYNAMIDE_HOME}", home);
        try {
            File f = new File(dynamide_resource_root);
            if (f!=null && f.exists()){
                dynamide_resource_root = f.getCanonicalPath();
            }
            props.put("DYNAMIDE_RESOURCE_ROOT",  dynamide_resource_root);
            //may or may not be correct...but it's what they specified.
        } catch (Exception e){
            String errmsg = "getResourceRootFromHome(\""+home+"\") failed.";
            Log.error(ResourceManager.class, errmsg, e);
            throw new Exception(errmsg, e);
        }

        String dynamide_context_conf = props.getProperty("DYNAMIDE_CONTEXT_CONF");
        if ( dynamide_context_conf == null || dynamide_context_conf.length()==0 ) {
            dynamide_context_conf = FileTools.join(dynamide_resource_root, "/conf/context.xml");
        } else {
            dynamide_context_conf = StringTools.searchAndReplaceAll(dynamide_context_conf, "${DYNAMIDE_HOME}", home);
            dynamide_context_conf = StringTools.searchAndReplaceAll(dynamide_context_conf, "${DYNAMIDE_BUILD}", dynamide_build);
            dynamide_context_conf = StringTools.searchAndReplaceAll(dynamide_context_conf, "${DYNAMIDE_RESOURCE_ROOT}", dynamide_resource_root);
        }
        props.put("DYNAMIDE_CONTEXT_CONF", dynamide_context_conf);
        return props;
    }


     private static String expandVariablesFromLocalProperties(String dynamide_home, Properties props, String propname){
        String property = props.getProperty(propname);
        String dynamide_build = props.getProperty(propname);
        String dynamide_resource_root = props.getProperty("DYNAMIDE_RESOURCE_ROOT");
        String dynamide_context_conf = props.getProperty("DYNAMIDE_CONTEXT_CONF");
         property = StringTools.searchAndReplaceAll(property, "${DYNAMIDE_CONTEXT_CONF}", dynamide_context_conf);
         property = StringTools.searchAndReplaceAll(property, "${DYNAMIDE_RESOURCE_ROOT}", dynamide_resource_root);
         property = StringTools.searchAndReplaceAll(property, "${DYNAMIDE_BUILD}", dynamide_build);
         property = StringTools.searchAndReplaceAll(property, "${DYNAMIDE_HOME}", dynamide_home);
        return property;
    }

    public static String getResourceRoot()
    throws Exception {
        return getResourceRootFromHome();
    }

    public static String getResourceRootFromEnv()
    throws Exception {
        return getResourceRootFromHome();
    }

    private String m_staticRoot = "";

    public void setStaticRoot(String staticRoot){
        m_staticRoot = staticRoot;
    }

    public String getStaticRoot()
    throws Exception {
        if (m_staticRoot.length()==0){
            m_staticRoot = getStaticRootFromHome();
        }
        return m_staticRoot;
    }

    public static String getStaticPrefix(){
        return Constants.STATIC_PREFIX;
    }

    public Object getResource(String name)
    throws SecurityException {
        return null;
    }

    public boolean hasResource(String key) {
        return false;
    }

    public static IContext wrapContext(String key, Object object){
        try {
            return wrapContext(null, key, object, null, null);
        } catch (ObjectAlreadyBoundException e){
            //This should never happen, since this overload doesn't bind attributes.
            Log.error(ResourceManager.class, "wrapContext caught an ObjectAlreadyBoundException, but silently returned null.");
            return null;
        }
    }

    public static IContext wrapContext(DynamideObject owner, String key, Object object, Map attributes)
    throws ObjectAlreadyBoundException {
        return wrapContext(owner, key, object, attributes, null);
    }

    /** Take any arbitrary object and wrap it in an IContext implementation,
     *  so that it can be added to an IContext as a node.
     *  You can also just add the object to the parent context as a non-IContext object with
     *  the key, using bind(String, Object).
     */
    public static IContext wrapContext(DynamideObject owner, String key, Object object, Map attributes, Permissions permissions)
    throws ObjectAlreadyBoundException {
        ContextNode context = new ContextNode(owner);
        context.setKey(key);
        if (attributes != null){
            context.bindAllAttributes(attributes);
        }
        context.bindAttribute("value", object);
        if (permissions != null) {
            context.lockPermissions(permissions);
        }
        return context;
    }

    private static final String INDENT_INCR = "   ";

    public String dumpRootContext(){
        return dumpRootContext(false);
    }

    public String dumpRootContext(boolean html){
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [3]");
        }
        String header = "<b>"+getKey()+"</b> [IContext]\r\n";
        return header + dumpContext(this, INDENT_INCR, html, "/");
    }

    public String dumpContext(String search, String indent, boolean html){
        IContext context = (IContext)find(search);
        return dumpContext(context, indent, html, "");
    }

    public static String dumpContext(IContext context, String indent, boolean html){
        return dumpContext(context, indent, html, "");
    }

    public static String dumpContext(IContext context, String indent, boolean html, String pathString){
        if ( context == null ) {
            //Log.debug(ResourceManager.class, "context was null in dumpContext");
            return "";
        }
        pathString = pathString + context.getKey()+'/';
        String kbe = "", kbs = "";
        if ( html ) {
            kbs = "<b>";
            kbe = "</b>";
        }
        String kwe = "", kws = "";
        if ( html ) {
            kws = "<font color='red'><b>";
            kwe = "</b></font>";
        }
        if ( context.getKey().equals(ROOT_KEY) && DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [4]");
        }

        String cid = "";
        if (context instanceof DynamideObject){
            cid = indent+"id: "+((DynamideObject)context).getID()+"\r\n";   
        }
        String result = indent + "key: "+context.getKey()+"\r\n"
                       +cid
                       +indent+"context-path: "+pathString+"\r\n"
                       +indent+"class: "+context.getClass().getName()+"\r\n";
        if (context.getAttributeCount() > 0){
            Iterator atts = context.getAttributes().entrySet().iterator();
            while ( atts.hasNext() ) {
                Map.Entry entry = (Map.Entry)atts.next();

                String attvalue;
                String entrykey = ""+entry.getKey();
                if ( entrykey.equals(Assembly.CONTENT) ) {
                    attvalue = "[...]" ;
                } else {
                    attvalue = StringTools.escape(""+entry.getValue());
                }

                result += indent + "attribute: " + entry.getKey() + " == "+ attvalue + "\r\n";
            }
        }
        if (context.getContextCount() > 0){
            String childKey;
            Object ekey;
            Iterator contexts = context.getContexts().entrySet().iterator();
            while ( contexts.hasNext() ) {
                Map.Entry entry = (Map.Entry)contexts.next();
                IContext child = (IContext)entry.getValue();
                childKey = "null";
                ekey = entry.getKey();
                if (child != null){
                    childKey = child.getKey();
                } else {
                    if ( ekey != null ) {
                        childKey = ekey.toString();
                    }
                }
                String sekey = ekey.toString();
                String keywarning = "";
                String displayName = childKey;
                if (!childKey.equals(sekey)){
                    //keywarning = kws+" Stored as: '"+sekey+"'"+kwe;
                    keywarning = kws+" (as stored). Key is: '"+childKey+"'"+kwe;
                    displayName = sekey;
                }
                String diveResult = dumpContext(child, indent + INDENT_INCR, html, pathString);
                result += indent + kbs+displayName+kbe +keywarning+" [IContext]\r\n"+ diveResult;
            }
        }
        return result;
    }

    //=============================================================================================

    private List m_addedContexts = new Vector();
    public List getAddedContexts(){return m_addedContexts;}
    public void setAddedContexts(List new_value){m_addedContexts = new_value;}

    public String dumpAddedContexts(){
        return Tools.collectionToString(m_addedContexts, "\r\n<br />");
    }

    // %% TODO: ensure in these functions that the caller is part of the home directory or assembly they affect.

    public IContext addElementToContext(org.jdom.Element elem, String path, String fullpath)
    throws DynamideException {
        ensureContextPath(path);
        Object obj = find(path);
        if ( obj == null ) {
            throw new DynamideException("path '"+path+"' not found in context: "+toString());
        }
        if ( ! (obj instanceof IContext)){
            throw new DynamideException("path '"+path+"' did not return an IContext: "+obj.getClass().getName());
        }
        IContext ctx  = (IContext)obj;

        synchronized (m_addedContexts){
             m_addedContexts.add("CONTEXT PATH: '"+path+"'<br />\r\nFILE: '"+fullpath
               +"' <br />\r\nAT: "+StringTools.escape(Tools.nowLocale())
               + "<pre>"+StringTools.escape(JDOMFile.output(elem))+"</pre>"
             );
        }
        return addElementToContext(elem, ctx, fullpath);
    }

    public static IContext addElementToContext(org.jdom.Element elem, IContext ctx, String fullpath){
        Iterator i = elem.getChildren().iterator();
        while (i.hasNext()) {
            org.jdom.Element child = (org.jdom.Element) i.next();
            diveElementToContext(child, ctx, fullpath);
        }
        return ctx;
    }

    private static void diveElementToContext(org.jdom.Element elem, IContext ctx, String fullpath){
        //System.out.println(""+elem.getName());
        if ( elem != null ) {
            IContext newc = ensureContext(ctx, elem.getName());
            newc.rebindAttribute(Assembly.FULLPATH, fullpath);
            Iterator ei = elem.getAttributes().iterator();
            while ( ei.hasNext() ) {
                org.jdom.Attribute att = (org.jdom.Attribute) ei.next();
                //System.out.println(""+att.getName()+"::"+att.getValue());
                newc.rebindAttribute(att.getName(), att.getValue());
            }

            List children = elem.getChildren();
            Iterator i = children.iterator();
            while (i.hasNext()) {
                org.jdom.Element child = (org.jdom.Element) i.next();
                diveElementToContext(child, newc, fullpath);
            }
        }
    }

    public IContext ensureContext(String name){
        return ensureContext(this, name);
    }

    public static IContext ensureContext(IContext cur, String name){
        IContext c = cur.getContext(name);
        if ( c == null  ) {
            cur = cur.rebind(new ContextNode(name));
           // System.out.println("adding "+name);
        } else {
            cur = c;
           // System.out.println("existed "+name);
        }
        return cur;
    }

    public IContext ensureContextPath(String path){
        return ensureContextPath(this, path);
    }

    public static IContext ensureContextPath(IContext rm, String path){
        IContext cur = rm;
        IContext result = null;
        List l = StringTools.parseSeparatedValues(path, "/");
        Iterator li = l.iterator();
        while ( li.hasNext() ) {
            String s = li.next().toString();
            if (s.length()>0){  //skip artifacts such as empty element for /foo, which gives {"", "foo"} or for /foo//bar
                //System.out.println("item: "+s);
                cur = ensureContext(cur, s);
                result = cur;
            }
        }
        return result;
    }

    //=============================================================================================

    public TreeBean createTestTree(){
        String tmpl = "resources/widgets/com.dynamide.treenode.wm";
        int id = 1;
        TreeBean root = new TreeBean("icontext"+id, "root", "", tmpl); //this one won't be displayed.
        TreeBean t;
        t = new TreeBean("icontextroot", "ResourceManager", "", tmpl);
        root.addChild(t);
        addContextToTree(++id, this, t, tmpl);
        return root;
    }

    //todo: %% totally insecure, for now...
    public void addContextToTree(int id, IContext context, TreeBean t, String tmpl){
        if ( context == null ) {
            //Log.debug(ResourceManager.class, "context was null in dumpContext");
            return;
        }

        if (context.getAttributeCount() > 0){
            Iterator atts = context.getAttributes().entrySet().iterator();
            while ( atts.hasNext() ) {
                Map.Entry entry = (Map.Entry)atts.next();
                String entrykey = ""+entry.getKey();
                String attvalue;
                if ( entrykey.equals(Assembly.CONTENT) ) {
                    attvalue = "[...]" ;
                } else {
                    attvalue = StringTools.escape(""+entry.getValue());
                }
                t.addChild(new TreeBean("icontext"+(id++), entrykey, attvalue, tmpl));
            }
        }
        if (context.getContextCount() > 0){
            String childKey;
            Object ekey;
            Iterator contexts = context.getContexts().entrySet().iterator();
            while ( contexts.hasNext() ) {
                Map.Entry entry = (Map.Entry)contexts.next();
                IContext child = (IContext)entry.getValue();
                childKey = "null";
                if (child != null){
                    childKey = child.getKey();
                } else {
                    ekey = entry.getKey();
                    if ( ekey != null ) {
                        childKey = ekey.toString();
                    }
                }
                TreeBean childBean = new TreeBean("icontext"+(id++), StringTools.escape(childKey), StringTools.escape(childKey), tmpl);
                t.addChild(childBean);
                addContextToTree(id, child, childBean, tmpl);
            }
        }
    }

    private void callAccountInit(IContext context, String accountXmlFullPath, String accountName){
        try {
            accountXmlFullPath = Tools.fixFilename(accountXmlFullPath);
            IContext account = context.rebind(new ContextNode(null, "account"));
            account.rebindAttribute(Assembly.FULLPATH, accountXmlFullPath);
            account.rebindAttribute("accountName", accountName);
            Account accountObj = new Account(null, accountXmlFullPath, accountName);
            account.rebindAttribute("Account", accountObj);
        } catch (Exception e){
            logError("Couldn't initialize "+accountName+"/account.xml: callAccountInit failed when calling "+accountXmlFullPath, e);
        }
    }

    private void reloadContextFile(){
        try {
            String contextXmlFullPath = getDynamideContextConf();// FileTools.join(getResourceRoot(), "/conf/context.xml");
            if ( FileTools.fileExists(contextXmlFullPath) ) {
                com.dynamide.util.XDB xdb = new com.dynamide.util.XDB();
                Element element = xdb.openXML("conf", contextXmlFullPath);
                addElementToContext(element, "/", contextXmlFullPath);
                IContext c = (IContext)find("/conf");
                //System.out.println(dumpContext(c, "    ", false));
                xdb.closeXML("conf");
                logDebug("DYNAMIDE_CONTEXT_CONF loaded: "+contextXmlFullPath);
            } else {
                logWarn("configuration file context.xml '"+contextXmlFullPath+"' does not exist.");
            }
        } catch (Exception e){
            logError("Couldn't reloadContextFile", e);
        }
    }

    private void callHomeContextInit(IContext context, String contextXmlFullPath,
                                     String contextXmlName, String contextParentPath){
        try {
            contextXmlFullPath = Tools.fixFilename(contextXmlFullPath);
            //IContext contextXml = context.bind(new ContextNode(null, contextXmlName));
            if ( FileTools.fileExists(contextXmlFullPath) ) {
                com.dynamide.util.XDB xdb = new com.dynamide.util.XDB();
                Element element = xdb.openXML("appdata", contextXmlFullPath);
                addElementToContext(element, contextParentPath, contextXmlFullPath);
                //c = find("/homes/dynamide/appdata");
                //System.out.println(dumpContext(c, "    ", false));
                xdb.closeXML("appdata");
            }
        } catch (Exception e){
            logError("Couldn't callHomeContextInit on  "+contextXmlFullPath, e);
        }
    }

    void callWebApps(IContext context, String webappsFullPath, String account,
                     String assemblyName, String interfaceNumber){
        try {
            webappsFullPath = Tools.fixFilename(webappsFullPath);
            IContext webapps = context.rebind(new ContextNode(null, "web-apps"));
            IContext webappsInitLog = ensureContext(WebApps.INITLOG); 
            IContext webappsCtx = ensureContext(webappsInitLog, webappsFullPath);
            webapps.unlockPermissions(null); //todo: replace with a real implementation. %%
            try {
                WebApps.initWebApps(webapps, webappsFullPath, account, assemblyName, interfaceNumber, webappsCtx);
            } finally {
                webapps.lockPermissions(null); //todo: replace with a real implementation. %%
            }
        } catch (Exception e){
            logError("callWebApps failed when calling "+webappsFullPath, e);
        }
    }

    private void  initHomes(IContext homes, String basePath, String RESOURCE_ROOT)
    throws IOException, ObjectAlreadyBoundException, JDOMException {
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [5]");
        }
        String path;
        String accountName;
        String filesi;

        File directory = new File(basePath);
        String[] files = directory.list();
        if (files == null || files.length == 0){
            logError("No account directories in homes basePath: "+basePath);
            return;
        }

        for ( int i = 0; i<files.length; i++ ) {
            filesi = files[i];
            if ( filesi.equals(".") || filesi.equals("..") || filesi.equals("CVS") ) {
                continue;
            }
            File target = new File(basePath, filesi);
            if (!target.isDirectory()){
                continue;
            }
            //System.out.println("doing dir: "+filesi);
            accountName = files[i];
            path = basePath + '/'+accountName ;
            initHome(homes, path, accountName);
        }
    }

    /** Re-initialize the context entries associated with a home directory/account.
     *  Does NOT remove running sessions or add assemblies.
     */
    //%% todo: make a function to clear sessions and add/remove assemblies.
    public void reinitHome(String accountName)
    throws Exception {
        if ( accountName == null ) {
            logWarn("null accountName passed to ResourceManager.reinitAccount");
            return;
        }
        IContext homeNode = (IContext)find("/homes/"+accountName);
        String path = homeNode.getAttribute(Assembly.FULLPATH).toString();
        callWebApps(homeNode, path+"/web-apps.xml", accountName, "", "");
        callAccountInit(homeNode, path+"/account.xml", accountName);
        callHomeContextInit(homeNode, path+"/context.xml", "context.xml", "/homes/"+accountName);
        initAssemblies(homeNode, path+File.separator+"assemblies", accountName, (String)getAttribute(RESOURCE_ROOT_KEY));
    }

    /** @param accountName is the name of the directory of the account, e.g. "dynamide"
     *  @see #reinitHome(String)
     */
    public void initHome(IContext homes, String path, String accountName)
    throws IOException, ObjectAlreadyBoundException, JDOMException {
        if (LOG_CACHING) logCaching("initHome path: "+path+" accountName: "+accountName);
        IContext homeNode = homes.bind(new ContextNode(null, accountName));
        synchronized (homeNode){
            homeNode.bindAttribute(Assembly.FULLPATH, Tools.fixFilename(path));
            homeNode.bindAttribute("name", accountName);
            callWebApps(homeNode, path+"/web-apps.xml", accountName, "", "");
            callAccountInit(homeNode, path+"/account.xml", accountName);

            //(IContext context, String contextXmlFullPath,String contextXmlName, String contextParentPath)
            callHomeContextInit(homeNode, path+"/context.xml", "context.xml", "/homes/"+accountName);
            String rr = (String)getAttribute(RESOURCE_ROOT_KEY);
            homeNode.bind(new Pool(null, "sessions"));
            homeNode.bind(new Pool(null, SESSIONS_POOLED_NAME));
            homeNode.bindAttribute("RESOURCE_ROOT", rr); //every account has a right to know this location, though they may not have read or write permission in subdirs...
            //secure sessions so that it can only be read by a user with access to homes, such as any user of that site.
            initAssemblies(homeNode, path+File.separator+"assemblies", accountName, rr);
        }
    }

    /** This is the only legal way to create an Assembly.
     */
    public Assembly createAssembly(DynamideObject owner, String id, String account, String RESOURCE_ROOT){
        if ( this != getRootResourceManager() ) {
            throw new SecurityException("Forbidden [7]");
        }
        return new Assembly(owner, id, account, RESOURCE_ROOT);
    }

    public Assembly getDynamideLibAssembly(){
        IContext dl = (IContext)find("/conf/dynamide-default-lib");
        String interfaceNum = (String)dl.getAttribute("interface");
        String basename = (String)dl.getAttribute("basename");
        String build = "";
        Assembly assembly = findSharedAssembly(basename, interfaceNum, build);
        return assembly;
    }

    private void initAssemblies(IContext context, String basePath, String account, String RESOURCE_ROOT)
    throws JDOMException {
        try {
            IContext assemblies = context.rebind(new ContextNode(null, "assemblies"));
            basePath = Tools.fixFilename(basePath);
            assemblies.rebindAttribute(Assembly.FULLPATH, basePath);
            File baseDir = new File(basePath);
            if ( ! baseDir.exists() ) {
                logWarn(basePath +" does not exist [3]");
            } else {
                String[] files = baseDir.list();
                String filesi;
                for (int i=0; i < files.length; i++) {
                    filesi = files[i];
                    if ( filesi == null || filesi.equals(".") || filesi.equals("..") || filesi.equals("CVS") ) {
                        continue;
                    }
                    File assemblyDir = new File(baseDir, filesi);
                    if ( assemblyDir.exists() && assemblyDir.isDirectory() ) {  // otherwise it is a file in the homedir.
                        try {
                            if (LOG_CACHING) logCaching("calling Assemby.createAssembly: "+basePath+" account: "+account);
                            Assembly.createAssembly(this,
                                                    assemblies,
                                                    assemblyDir,
                                                    filesi,          //assembly name, w/o path
                                                    account,
                                                    RESOURCE_ROOT,
                                                    isCacheOn());    //new 2005-09-16 to init Assembly with setting from ResourceManager.
                        } catch (Exception e){
                            logError("Couldn't init assembly.", e);
                        }
                    }
                }
            }
        } catch (Exception e){
            logError("initAssemblies failed to load from basePath: "+basePath, e);
        }
    }

    private void initAsRootContext(String RESOURCE_ROOT, String DYNAMIDE_CONTEXT_CONF, String DYNAMIDE_STATIC_ROOT)
    throws Exception {
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [6]");
        }
        super.setKey(ROOT_KEY);
        setDynamideContextConf(DYNAMIDE_CONTEXT_CONF);
        setStaticRoot(DYNAMIDE_STATIC_ROOT);
        LOG_CACHING = ( Log.getInstance().isEnabledFor(Constants.LOG_CACHING_CATEGORY, org.apache.log4j.Priority.DEBUG) );

        RESOURCE_ROOT = Tools.fixFilename(RESOURCE_ROOT);
        bindAttribute("RESOURCE_ROOT", RESOURCE_ROOT);

        //IContext cache = bind(new ContextNode(null, CACHE_ICONTEXT_NAME));
        try {
            IContext cache = _singleton.createPublishCache(CACHE_ICONTEXT_NAME/*cachePrefix*/, CACHE/*cachePrefixURI*/);
        } catch( Exception e ){
            Log.error(ResourceManager.class, "Couldn't createPublishCache for singleton", e);
        }

        reloadContextFile();

        initAssemblies(this, RESOURCE_ROOT+File.separator+"assemblies", "", RESOURCE_ROOT);

        IContext homes = bind(new ContextNode(null, "homes"));
        homes.bindAttribute(Assembly.FULLPATH, Tools.fixFilename(RESOURCE_ROOT+"/homes"));

        callWebApps(homes, RESOURCE_ROOT+"/homes/web-apps.xml", "", "", "");

        initHomes(homes, RESOURCE_ROOT+"/homes", RESOURCE_ROOT);
        //System.out.println("root context: \r\n"+dumpRootContext(false));

        //moved up: reloadContextFile();
        
        //the default context.xml is now loaded, which has the following entry, so that it is safe to connect
        // up to the error logger.
        configureErrorlogs();
    }
    
    public String reloadRootWebApps(){
        String RESOURCE_ROOT = getAttribute("RESOURCE_ROOT").toString();
        IContext homes = (IContext)find("/homes");
        callWebApps(homes, RESOURCE_ROOT+"/homes/web-apps.xml", "", "", "");
        return "<html><body><h1>OK</h1></body></html>";
    }

    public void addWebApp(String uri,
                          String appname,
                          String home,
                          String assemblyBasename,
                          String assemblyInterfaceNumber,
                          String build,
                          boolean loadOnStartup)
    throws Exception {
        //String webappsFullPath = findWebAppsInAccount(home).getAttribute(Assembly.FULLPATH);
        Assembly assembly = findAssemblyInAccount(home, assemblyBasename, assemblyInterfaceNumber, build);
        String webappsFullPath = (String)findWebAppsInAssembly(assembly).getAttribute(Assembly.FULLPATH);
        WebApps.addWebApp(webappsFullPath,
                          uri,
                          appname,
                          home,
                          assemblyBasename,
                          assemblyInterfaceNumber,
                          loadOnStartup
                          );
        reinitHome(home);
    }

    public void addWebApp(Assembly assembly,
                          String uri,
                          String appname,
                          boolean loadOnStartup)
    throws Exception {
        //String webappsFullPath = findWebAppsInAccount(home).getAttribute(Assembly.FULLPATH);
        String webappsFullPath = (String)findWebAppsInAssembly(assembly).getAttribute(Assembly.FULLPATH);
        WebApps.addWebApp(webappsFullPath,
                          uri,
                          appname,
                          null, //home,
                          null, //assemblyBasename,
                          null, //assemblyInterfaceNumber,
                          loadOnStartup
                          );
        reinitHome(assembly.getAccount());
    }

    public IContext findWebAppsRoot(){
        return (IContext)find("/homes/web-apps");
    }

    public IContext findWebAppsInAccount(String account){
        return (IContext)find("/homes/"+account+"/web-apps");
    }

    public IContext findWebAppsInAssembly(IContext assembly){
        return (IContext)assembly.find("web-apps");
    }

    public IContext findSharedAssemblies(){
        return (IContext)find("/assemblies", "/");
    }

    public IContext findAssembliesInAccount(String account){
        return (IContext)find("/homes/"+account+"/assemblies", "/");
    }

    public Assembly findSharedAssembly(String basename, String interfaceNumber, String build){
        Assembly assembly = Assembly.findAssembly(findSharedAssemblies(), basename, interfaceNumber, build);
        if ( assembly != null ) {
            logDebug("interfaceNumber: "+interfaceNumber+" interface: "+assembly.getAttribute(Assembly.INTERFACE));
            if ( interfaceNumber.equals(assembly.getAttribute(Assembly.INTERFACE)) ) {
                return assembly;
            }
        }
        //logInfo("Assembly FOUND looking for "+basename+'/'+interfaceNumber+" (shared)");
        return null;
    }

    public Assembly findAssemblyInAccount(String account, String basename, String interfaceNumber, String build){
        //helpful but noisy: System.out.println("in findAssemblyInAccount. account:"+account+" basename:"+basename
        //                    +" interfaceNumber: "+interfaceNumber+" build: "+build);
                            
                            
    //TODO: %%%%% when some of these params are empty, you will get a null pointer exeption.
    //  This is true, for example, when you have HOME and ASSEMBLY missing from WebAppEntry, when you put / pointing to doco in file:///C:/dynamide/build/DYNAMIDE_RESOURCE_ROOT/homes/web-apps.xml
        return Assembly.findAssembly(findAssembliesInAccount(account), basename, interfaceNumber, build);
    }

    public Assembly findAssembly(String account, String basename, String interfaceNumber, String build){
        Assembly result = null;
        result  = findAssemblyInAccount(account, basename, interfaceNumber, build);
        if (result == null){
            result = findSharedAssembly(basename, interfaceNumber, build);
        }
        return result;
    }

    public Assembly findAssembly(Assembly caller, String basename, String interfaceNumber, String build){
        String account = caller.getAccount();
        return findAssembly(account, basename, interfaceNumber, build);
    }

    /* I don't think this is a good idea.  The idea of an interface number is that
     *  wildcards don't apply.  You can still have multiple builds, but you should only have
     *  one build representing each interface number.  If there are two assemblies with the same
     *  interface number and basename it is a problem.
     *public Assembly findHighestInterfaceNumberAssembly(String account, String targetAssemblyName){  //may return null.
     *  Object o = find("/homes/"+account+"/assemblies/"+targetAssemblyName, "/");
     *  if ( o instanceof IContext ) {
     *      StringList sl = new StringList();
     *      Map m = ((IContext)o).getContexts();
     *      Iterator it = m.values().iterator();
     *      while (it.hasNext()) {
     *          IContext assembly = (IContext)it.next();
     *          sl.addObject(assembly.getKey(), assembly);
     *          System.out.println("adding assembly to sort list: "+assembly.getKey());
     *      }
     *      sl.sort();
     *      return (Assembly)sl.getObjectAt(0);
     *  }
     *  logError("assembly not found looking for wildcard interfaceNumber. targetAssemblyName: "+targetAssemblyName+" account: "+account+"Object o is: "+o);
     *  return null;
     *}
     */

    /** This calls getAttribute(CACHE_ON) to see if the value is "true" (resource caching is on for the global cache),
     *  else will contain the value "false" if the filesystem or resource database is
     *  to be read on each request.
     */
    public boolean isCacheOn(){
        if (true) return true;
        //%%%% Sat Aug 06, 2005 18:51 getting this going in Tomcat
        
        Object att = getAttribute(ResourceManager.CACHE_ON);
        if ( att == null ) {
            return false;
        }
        return att.toString().equals("true");
    }

    /** Kick the global cache.
     */
    public void updateCache(){
        //TODO: walk the list of things in cache and check the timestamps
    }

    /** Re-read a resource.
     */
    public IContext rereadResource(Session session, String appname, String resourceID)
    throws FileNotFoundException {
        if (LOG_CACHING) logCaching("rereadResource: appname: "+appname+" resourceID: "+resourceID);
        IContext context = session.getAssembly().getApplicationResource(appname, resourceID);
        if ( context != null ) {
            return Assembly.readResource(context, context.getKey(), (String)context.getAttribute(Assembly.FULLPATH));
        } else {
            return null;
        }
    }

    /** @return The File handle to the rootdir if successful, else null
     */
    public File writeCache(String prefix, String rootdir)
    throws Exception {
        IContext cache = getCache(prefix); //%% make sure this isn't a security breach, like: writeCache("sessions", myHomeDir);
        if ( cache != null && cache instanceof ContextNode ) {
            File dir = FileTools.createDirectory(rootdir);
            if ( dir == null ) {
                throw new SecurityException("No permission to create directory: \""+rootdir+"\"");

            }((ContextNode)cache).writeCache(prefix, dir);
            return dir;
        }
        throw new SecurityException("No permission to writeCache: "+rootdir+" prefix: "+prefix);
    }

    /** Ordinarily, cache points to ResourceManager.CACHE_ICONTEXT_NAME, but with this
     *  method, you can create your own cache, to store in-memory content. See also createPublishCache.
     */
    public IContext findOrCreateCache(String cachePrefix, String cachePrefixURI){
        IContext ctx = (IContext)getContext(cachePrefix);
        if ( ctx != null ) {
            return ctx;
        } else  {
            IContext res = rebind(new ContextNode(null, cachePrefix));
            res.rebindAttribute(CACHE_IS_PUBLISH_CACHE, "true");
            res.rebindAttribute(CACHE_PREFIX, cachePrefix);  //informational only.
            res.rebindAttribute(CACHE_PREFIX_URI, cachePrefixURI);  //required
            System.out.println("Root ResourceManager rebinding cache context.  ID: "+getID()+" context: "+res);
            return res;
        }
    }

    /** Ordinarily, cache points to ResourceManager.CACHE_ICONTEXT_NAME, but with this
     *  method, you can create your own cache, so that you can publish to it in Session.MODE_PUBLISH.
     */
    public IContext createPublishCache(String cachePrefix, String cachePrefixURI)
    throws ObjectAlreadyBoundException {
        IContext ctx = (IContext)getContext(cachePrefix);
        if ( ctx != null ) {
            throw new ObjectAlreadyBoundException("cachePrefix already registered: "+cachePrefix);
        } else  {
            IContext res = bind(new ContextNode(null, cachePrefix));
            res.bindAttribute(CACHE_IS_PUBLISH_CACHE, "true");
            res.bindAttribute(CACHE_PREFIX, cachePrefix);  //informational only.
            res.bindAttribute(CACHE_PREFIX_URI, cachePrefixURI);  //required, so that the storage in the context tree is not the URI the user may see, e.g. if they pass in "".
            return res;
        }
    }

    /** @todo Lock this down a bit more, by Account.
     */
    public void destroyPublishCache(String cachePrefix)
    throws Exception {
        if ( cachePrefix.length()==0 ) {
            throw new SecurityException("No permission to destroyPublishCache on root cache.");
        }
        IContext cache = getCache(cachePrefix);
        if (cache!=null && cache.getAttribute(CACHE_IS_PUBLISH_CACHE) != null){
            // %% todo: cache.getAttribute(account) ..... is user account, etc.
            remove(cachePrefix);
            return;
        }
        throw new SecurityException("No permission to destroyPublishCache: "+cachePrefix);
    }

    public void checkDirectoryWrite(Session session, String directoryPath)
    throws SecurityException {
        DynamideSecurityManager.checkCallStack("com.dynamide.Session", "checkDirectoryWrite", 1, false);
        //%% todo: implement checkDirectoryWrite.
    }

    public IContext getCache(){
        return getCache(CACHE_ICONTEXT_NAME); // %% todo: look this up in properties file.
    }

    public IContext getCache(String cachePrefix){
        ResourceManager root = ResourceManager.getRootResourceManager();
        IContext cache = root.getContext(cachePrefix);
        return cache;
    }

    public String getCachePrefixURI(String cachePrefix){
        IContext cache = getCache(cachePrefix);
        if ( cache != null ) {
            Object obj = cache.getAttribute(CACHE_PREFIX_URI);
            if ( obj != null ) {
                return obj.toString();
            }
        }
        logWarn("getCachePrefixURI could not find the cachePrefixURI for cachePrefix: "+cachePrefix);
        return "";
    }

    /** Safe to call with either /cache/yada/yada or /yada/yada
     */
    public IContext getCachedResource(String cacheRelName){
        return getCachedResource("", cacheRelName);
    }

    /** Don't include the leading slash on the cachePrefix, e.g. send "cache" NOT "/cache"
     */
    public IContext getCachedResource(String cachePrefix, String cacheRelName){
        if (LOG_CACHING) logCaching("getCachedResource. cachePrefix: '"+cachePrefix+"' cacheRelName: '"+cacheRelName+'\'');
        String cachePrefixURI;
        if ( cachePrefix != null && cachePrefix.length()>0) {
            ////cachePrefixURI = "/"+cachePrefix;  %% this seems funny that it is commented out, reviewed on 2005-12-01
            logDebug("getCachedResource [cachePrefix sent]: '"+cachePrefix+"' cacheRelName: "+cacheRelName);
        } else  {
            ////cachePrefixURI = CACHE_URI;
            cachePrefix = CACHE_ICONTEXT_NAME;
            logDebug("getCachedResource: '"+cachePrefix+"' cacheRelName: "+cacheRelName);
        }

        IContext cache = getCache(cachePrefix);
        if ( cache == null ) {
            //System.out.println("======= getCachedResource for cachePrefix: '"+cachePrefix+"' found null resource for cacheRelName '"+cacheRelName+"'");
            return null;
        }
        cachePrefixURI = cache.getAttribute(CACHE_PREFIX_URI).toString();

        if (cachePrefixURI.length()>0 && cacheRelName.startsWith(cachePrefixURI)){
            //This is legit, since the DynamideHandler checks for the full URI to see expires, etc.
            if (LOG_CACHING) logCaching("Trimming cacheRelName \""+cacheRelName+"\" since it already contained cachePrefixURI: \""+cachePrefixURI+"\"");
            cacheRelName = cacheRelName.substring(cachePrefixURI.length());
        }

        IContext resource = cache.getContext(cacheRelName);
        //System.out.println("======== cache.getContext("+cacheRelName+") :: "+resource);
        //if (resource==null){
        //    System.out.println(((ContextNode)cache).dump());  
        //}
        try {
            if (isCacheOn() && resource!=null){
                Long tstamp = (Long)resource.getAttribute(Assembly.MODIFIED);
                if (tstamp != null){
                    String fullpath = (String)resource.getAttribute(Assembly.FULLPATH);
                    if (fullpath!=null){
                        File f = new File(fullpath);
                        if (f != null){
                            if (f.lastModified() - tstamp.longValue() > 0){
                                //protected static IContext readResource(IContext context, String key, String fullpath)
                                System.out.println("==============Re-reading "+fullpath+" tstamp: "+tstamp);
                                resource = Assembly.readResource(resource, cacheRelName, fullpath); //resets MODIFIED, and CACHED.
                                cache.rebind(cacheRelName, resource);
                            }
                        }
                    }
                }
            }
        } catch (Exception e){
            logError("[getCachedResource] ", e);
        }
        return resource;
    }

    /** Safe to call with either /cache/yada/yada or /yada/yada
     */
    public IContext putCachedResource(String cacheRelName, IContext resource){
        return putCachedResource("", cacheRelName, resource);
    }

    public IContext putCachedResource(String cachePrefix, String cacheRelName, IContext resource){
        if (LOG_CACHING) logCaching("RM ID: "+getID()+"putCachedResource: "+cachePrefix+" cacheRelName: "+cacheRelName);
        IContext cache = getCache(cachePrefix);
        String cachePrefixURI = cache.getAttribute(CACHE_PREFIX_URI).toString();
        if (cacheRelName.startsWith(cachePrefixURI)){
            cacheRelName = cacheRelName.substring(cachePrefixURI.length());
        }
        cache.rebind(cacheRelName, resource);
        resource.rebindAttribute(Assembly.CACHED, Tools.now());
        resource.rebindAttribute(Assembly.CACHE_REL, cacheRelName);
        //System.out.println(dumpContext(cache,"  ",false));
        return resource;
    }

    private WebAppEntry findUriOrPartial(IContext context, String [] searchArray){
        logDebug("\r\n\tsearching in "+context.getKey()+ "\r\n\tarray: "+Tools.arrayToString(searchArray));
        if (searchArray.length == 0){
            logDebug("searchArray is zero length");
            return null;
        }
        Object found;
        String uribase = searchArray[searchArray.length-1];
        int slashIndex = uribase.lastIndexOf("/");
        while (slashIndex > -1){        // ends when uri is just root: /
            //logDebug("\tfor: \t'"+searchArray[searchArray.length-1]+"'");
            found = context.find(searchArray);
            if ( found != null && (found instanceof WebAppEntry) ) {
                logDebug("\tfound");
                return (WebAppEntry)found;
            }
            uribase = uribase.substring(0, slashIndex);
            searchArray[searchArray.length-1] = uribase;
            slashIndex = uribase.lastIndexOf("/");
        }
        /* This is for searching for the /, but it gets invoked if /foobar is called, since foobar isn't found.
         * searchArray[searchArray.length-1] = "/";
        System.out.println("\tfor: \t'"+searchArray[searchArray.length-1]+"'");
        found = context.find(searchArray);  // Look for uri "/", in case there is a registered root app
        if ( found != null ){
            if (found instanceof WebAppEntry) {
                System.out.println("\tfound");
                return (WebAppEntry)found;
            } else {
                System.out.println("\tfound, but wrong class: "+found.getClass().getName());
            }
        }
         */
        return null;
    }

    public IContext findStaticResource(String fullURI) throws ResourceException, ApplicationNotFoundException {
        WebAppEntry entry = uriToApp(fullURI);
        if (entry != null) {
            //helpful but noisy: System.out.println("========== findStaticResource ==> uriToApp(" + fullURI + ") ==> " + entry);
            Assembly assembly = findAssembly(entry.getHome(), entry.getAssembly(), entry.getInterface(), entry.getBuild());
            if (assembly != null && fullURI.startsWith(entry.getURI()))  {
                    String relName = fullURI.substring(entry.getURI().length());
                    IContext applicationFileNode = assembly.getApplicationResource(entry.getAppname(), FileTools.join("resources/static",relName));
                    if (applicationFileNode != null) {
                        //helpful but noisy: System.out.println("========== findStaticResource ==> found ==>"+applicationFileNode);
                        //These were helpful to find mime type errors, for example.
                        // System.out.println("========== findStaticResource ==> dump attr ==>"+applicationFileNode.dumpAttributes(false));
                        //System.out.println("========== findStaticResource ==> dump  ctx ==>"+applicationFileNode.dumpContext(false));
                        //Tools.printStackTrace();
                        return applicationFileNode;
                    }
            }
        }
        return null;
    }

    public WebAppEntry uriToApp(String uri)
    throws ApplicationNotFoundException, ResourceException {
        if ( uri == null ) {
            throw new ResourceException("[113] null uri was passed to ResourceManager.uriToApp(String)");
        }
        String where = "";
        String errmsg;
        Object result;
        if ( uri.length() == 0 || uri.equals("/") ) {
            String [] s0 = {"homes", "web-apps", "/"};
            result = findUriOrPartial(this, s0);
            if ( result != null && (result instanceof WebAppEntry)){
                WebAppEntry entry = (WebAppEntry)result;
                logDebug("uriToApp returning default / registered in homes/web-apps.xml for uri: '"+uri+"' which is app: "+entry.getAppname());
                return entry;
            }
        }
        String [] s = {"homes", "web-apps", uri};
        where = "homes/web-apps.xml";
        logDebug("\r\n\tfind: "+Tools.arrayToString(s));
        result = findUriOrPartial(this, s);
        //System.out.println("result 1: "+result);
        if ( result != null ) {
            if (! (result instanceof WebAppEntry)){
                errmsg = "[ResourceManager.uriToApp] IContext found in homes/web-apps.xml, but is the wrong class (should have been WebAppEntry): "+result.getClass().getName()+"::"+result.toString();
                throw new ResourceException(errmsg);
            } else {
                WebAppEntry entry = (WebAppEntry)result;
                String HOME = entry.getHome();
                String ASSEMBLY = entry.getAssembly();
                if ( HOME.length() == 0 || ASSEMBLY.length() == 0) {
                    throw new ResourceException("WebAppEntry found looking for '"+uri+"', but in homes/web-apps.xml this entry requires an ASSEMBLY (was '"+ASSEMBLY+"') and a HOME (was '"+HOME+"'): "+entry);
                }
                logDebug("uriToApp found '"+uri+"' in homes/web-apps.xml");
                return (WebAppEntry)result;
            }
        }
        // %% todo: if not found, start trimming down the uri and look for partial matches of URI/pathinfo.

        // %% Now look in URI to see if it begins with a home name:
//for each...
        //cheat, for now. %%  DOOOOP!!!!!!!!!!!!!!!!!!
        UriToAppForHomeResult utahres;
        utahres = uriToAppForHome(uri, "dynamide", where);
        where = utahres.where;
        if (utahres.webAppEntry != null){
            return utahres.webAppEntry;  
        }
        utahres = uriToAppForHome(uri, "laramie", where);
        if (utahres.webAppEntry == null){
            errmsg = "@@@@@@@@@@@@@@@@@@@@@ WebAppEntry Not found looking for '"+uri+"' in: \r\n"+utahres.where;
            logError(errmsg);
            throw new ApplicationNotFoundException(uri, "");//Don't send the user the error message with path info: security breach.  If they are a developer, they can see the logs.
        }
        return utahres.webAppEntry;
    }
    
    private class UriToAppForHomeResult {
        public UriToAppForHomeResult(String where, WebAppEntry w){
            this.where = where;
            this.webAppEntry = w;
        }
        public String where = "";
        public WebAppEntry webAppEntry = null;
    }
    
    public UriToAppForHomeResult uriToAppForHome(String uri, String home, String where)
    throws ApplicationNotFoundException, ResourceException {
        String errmsg;
        Object result;

        String [] s1 = {"homes", home, "web-apps", uri};
        logDebug("\r\nfind: "+Tools.arrayToString(s1));
        result = findUriOrPartial(this, s1);
        //System.out.println("result 2: "+result);
        where += "\r\nhomes/"+home+"/web-apps.xml";
        if ( result != null ) {
            if (! (result instanceof WebAppEntry)){
                errmsg = "[ResourceManager.uriToApp] IContext found in homes/"+home+"/web-apps.xml, but is the wrong class (should have been WebAppEntry): "+result.getClass().getName()+"::"+result.toString();
                throw new ResourceException(errmsg);
            }
            WebAppEntry entry = (WebAppEntry)result;
            if ( entry.getAssembly().length() == 0 ) {
                throw new ResourceException("WebAppEntry found looking for '"+uri+"', but in homes/"+home+"/web-apps.xml this entry requires an assembly: "+entry);
            }
            logDebug("uriToApp found '"+uri+"' in homes/"+home+"/web-apps.xml");
            return new UriToAppForHomeResult(where, (WebAppEntry)result);
        }

        String [] sAssemblies = {"homes", home, "assemblies"};
        String [] sAccount = {"homes", home, "account"};
        IContext account = (IContext)find(sAccount);
        IContext assemblies = (IContext)find(sAssemblies);
        if ( assemblies != null && account != null ) {
            // the "assemblies" context contains all installed assemblies.
            // the "account" context contains default interface numbers for mapping urls to assemblies
            Account accountObj = (Account)account.getAttribute("Account");
            if (assemblies.getContextCount() > 0){
                Iterator it = assemblies.getContexts().values().iterator();
                while (it.hasNext()) {
                    IContext assembly = (IContext)it.next();
                    if ( Assembly.BASENAMES.equals(assembly.getKey()) ) {
                        continue;
                    }
                    String interfaceNumber = "";
                    //if (accountObj != null){
                    //    interfaceNumber = accountObj.lookupAssemblyDefault(assembly.getKey());
                    //} else {
                    //    interfaceNumber = "";
                    //}
                    //String [] s3 = {interfaceNumber, "web-apps", uri};
                    String [] s3 = {"web-apps", uri};
                    String wherepath = "homes/"+home+"/assemblies/"+assembly.getKey()+"/web-apps.xml";
                    where += "\r\n"+wherepath;
                    result = findUriOrPartial(assembly, s3);
                    if ( result != null ) {
                        if (! (result instanceof WebAppEntry)){
                            errmsg = "[ResourceManager.uriToApp] IContext found in '"+wherepath+"', but is the wrong class (should have been WebAppEntry): "+result.getClass().getName()+"::"+result.toString();
                            throw new ResourceException(errmsg);
                        }
                        logDebug("uriToApp found '"+uri+"' in '"+wherepath+"' entry:\r\n"+result);
                        return new UriToAppForHomeResult(where, (WebAppEntry)result);
                     } else {
                        logDebug("uriToApp didn't find uri: "+uri); // in: \r\n"+dumpContext(assembly, "  ", false));
                    }
                }
            }
        }
        return new UriToAppForHomeResult(where, (WebAppEntry)null);
    }


    private int gJobWorkerThreadCount = 1;
    ThreadGroup jobThreadGroup = new ThreadGroup("DynamideJobThreadGroup");

    /** Only application events can be started as Jobs -- Page and Widget events are not supported.
     *  Partly, this is for security: only application code should be able to kick off background threads,
     *  not necessarily imported pages and widgets.  Be sure not to hang on to the returned reference
     *  after you need to, since it keeps the Job/Thread object in memory.
     */
    public Job startJob(Session session, String eventName, Object inputObject){
        return startJob(session, eventName, inputObject, 0);
    }

    public Job startJob(Session session, String eventName, Object inputObject, long startDelay){
        Job job = new Job(jobThreadGroup,"DynamideWorkerThread_job_"+(gJobWorkerThreadCount++), session, "", eventName, inputObject, startDelay);
        job.start();
        return job;
    }

    public Job installJob(String applicationURI,
                          Object inputObject)
    throws Exception {
        Session s = Session.createSession(applicationURI);
        return installJob(s,
                          s.getFullAppname(),
                          inputObject);
    }

    /** Call this to install a Job or to re-install a Job
     *  -- if a Job with the same ID (which is Session.getFullAppname()) is
     *  currently running, it is removed first -- be sure that closeOnJobClose is false if you
     *  do this from the Session, otherwise the Session will be closed for you.
     */
    public Job installJob(Session session,
                          String installedName,
                          Object inputObject)
    throws Exception {
        removeInstalledJob(installedName, true);
        int jobInterval = Tools.stringToIntSafe(session.get("jobInterval"), -1);
        int jobCount = Tools.stringToIntSafe(session.get("jobCount"), -1);
        int jobDelay = Tools.stringToIntSafe(session.get("jobDelay"), -1);

        Job job = new Job(jobThreadGroup,
                          "DynamideWorkerThread_job_"+(gJobWorkerThreadCount++),
                          session,
                          installedName,
                          "application_runJob",
                          inputObject,
                          jobDelay);
        job.setJobInterval(jobInterval);
        job.setJobCount(jobCount);
        job.start();
        IContext jobs = getJobs();
        jobs.bindAttribute(installedName, job);
        return job;
    }

    public Job findJob(String installedName){
        IContext jobs = getJobs();
        return (Job)jobs.getAttribute(installedName);
    }

    public Job removeInstalledJob(String installedName, boolean closeJob){
        Job job = (Job)(getJobs().removeAttribute(installedName));
        if ( job != null ){
            logInfo("removing installed job: "+installedName);
            if ( closeJob ) {
                job.close(false/*false implies DON'T call removeInstalledJob*/);
                //job checks session."closeOnJobClose" and possibly closes session also.
            }
        } else {
            logDebug("removing installed job, but job not installed: "+installedName);
        }
        return job;
    }

    private IContext getJobs(){
        IContext jobs = (IContext)find("/jobs");
        if ( jobs == null ) {
            jobs = new ContextNode(this, "jobs");
            rebind("jobs", jobs);
        }
        return jobs;
    }

    private void checkJobs(){
        long now = Tools.now().longValue();
        IContext jobs = getJobs();
        if ( jobs != null ) {
            Map m = jobs.getAttributes();
            Iterator it = m.values().iterator();
            while ( it.hasNext() ) {
                Job j = (Job)it.next();
                try {
                    Session s = j.getSession();
                    int count = j.getCount();
                    int jobCount = j.getJobCount();
                    if ( ((jobCount==-1)||count<jobCount) && ! j.getBusy()){
                        long st = j.getNextStartTime();
                        if (now>=st){
                            logDebug("starting job: "+j);
                            j.start();
                        }
                    }
                } catch (Exception e)  {
                    logError("couldn't start job in checkJobs()", e);
                }
            }
        }
    }

    private void startApp(final String uri){
        Thread thr = new Thread(){
            public String m_uri = uri;
            public void run(){
                try {
                    Session session = Session.createSession(m_uri);
                    int jobCount = Tools.stringToIntSafe(session.getPropertyStringValue("jobCount"), 0);
                    boolean autostart = Tools.isTrue(session.getPropertyStringValue("jobAutostart"));
                    //jobCount:
                    //   positive number: number of runs.
                    //   negative one (-1): infinite runs.
                    //   zero: don't install.
                    if ( autostart ) {
                        installJob(session, session.getFullAppname(), null);
                    }
                    if (session.isPoolable()) {
                        repoolSession(session);
                    }
                } catch (Exception e)  {
                    logError("could not start session for load-on-startup: '"+m_uri+"'", e);
                }
            }
        };
        thr.start();
    }

    /** Fallback value, in milliseconds, of how often the ResourceManager will check for Jobs that
     *  need to run, unless /conf/ResourceManager/jobMasterInterval is set in context.xml
     */
    public static final int JOB_MASTER_INTERVAL_DEFAULT = 60000;

    public class JobMasterThread extends Thread {
        public JobMasterThread(String name){
            super(name);
        }
        public boolean terminated = false;
        public void run(){
            while ( !terminated ) {
                try {
                    Thread.sleep(jobThreadInterval);
                } catch( Exception e ){
                }
                checkJobs();
            }
        }
        protected int jobThreadInterval = JOB_MASTER_INTERVAL_DEFAULT; //in milliseconds
    }

    public void startLoadOnStartupApps(){
        IContext homes = (IContext)find("/homes");
        Map m = homes.getContexts();
        Iterator it = m.values().iterator();
        while (it.hasNext()) {
            IContext home = (IContext)it.next();
            if (home!=null){
                IContext webapps = (IContext)home.getContext("web-apps");
                if ( webapps!=null ) {
                    Map webappsMap = webapps.getContexts();
                    if ( webappsMap != null ) {
                        Iterator webit = webappsMap.values().iterator();
                        while ( webit.hasNext() ) {
                            WebAppEntry wae = (WebAppEntry)webit.next();
                            if ( wae.getLoadOnStartup() ) {
                                logInfo("starting Session '"+wae.getAppname()+"' for load-on-startup in webapps '"
                                         +webapps.getAttribute(Assembly.FULLPATH)+"'");
                                startApp(wae.getURI());
                            }
                        }
                    }
                }
            }
        }
        resetJobMasterThread();
    }

    private void resetJobMasterThread(){
        if (m_jobMasterThread!=null){
            m_jobMasterThread.terminated = true;
            m_jobMasterThread = null;
        }
        m_jobMasterThread = new JobMasterThread("Dynamide-JobMasterThread");
        m_jobMasterThread.jobThreadInterval = Tools.stringToIntSafe(find("/conf/ResourceManager/jobMasterInterval"), JOB_MASTER_INTERVAL_DEFAULT);
        m_jobMasterThread.start();
    }

    public static String crypt(String passwd){
        try{
            java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-1");
            sha.update( passwd.getBytes() );
            String c = new String(sha.digest());
            return c;
        }
        catch (java.security.NoSuchAlgorithmException e) {
            System.out.println(e.toString());
            return "";
        }
    }

    private static long maxID;

    private void sadd(String s){
        _sadd = _sadd+"\r\n"+s;
    }
    private static String _sadd = "";

    public String getSadd(){

        return _sadd;
   }

    public synchronized String getNextSessionID(){
       // sadd("entering "+Thread.currentThread().getName());
        long d = Tools.now().longValue();
        maxID++;
        String res = ""+d+'.'+maxID;
        sadd("getNextSessionID: "+res+" maxID: "+maxID);
        String cr = crypt(res);
        //sadd("crypt: "+cr);
        String ret =  toNumeric(cr);
        sadd("ret: "+ret);
        //sadd("leaving "+Thread.currentThread().getName());
        return ret+'.'+maxID;
    }

    //this functions takes a sequence of characters and returns a sequence of numbers
    private static String toNumeric(String passwd){
        String sequence = "";
        for (int a = 0; a < passwd.length(); a++){
            char c = passwd.charAt(a);
            int num = (int) c;
            if ((num<58 && num>47) || (num > 64 && num < 91) || (num > 96 && num < 123)) {
                sequence += c;
            } else {
                sequence += Integer.toHexString(num);
            }
        }
        return sequence;
    }

    private String m_hostName = null;
    /** The hostname value is cached for the life of the ResourceManager.
     */
    public String getHostName()
    throws java.net.UnknownHostException {
        if (m_hostName==null){
            m_hostName = java.net.InetAddress.getLocalHost().getHostName();
        }
        return m_hostName;
    }

    public boolean isUserLocalDeveloper(String USER){
        try {
            Object oHost = find("/conf/localDeveloper/host");
            if (oHost==null){
                logError("/conf/localDeveloper/host not found in context.  Can't validate user: '"+USER+"'");
                return false;    
            }
            oHost = oHost.toString();
            String allowedHost = (oHost == null) ? null : oHost.toString();
            Object oUser = find("/conf/localDeveloper/user").toString();
            String user = (oUser == null) ? null : oUser.toString();
            String localhostName = getHostName();
            logWarn("isUserLocalDeveloper USER: "+USER+" user: "+user+" allowedHost: "+allowedHost+" host: "+localhostName);
            if (user != null && user.equals(USER)){
                if (allowedHost != null && allowedHost.equalsIgnoreCase(localhostName) ) {
                    return true;
                } else {
                    logError("isUserLocalDeveloper found valid local user, but not an allowed host. in conf: "
                              +allowedHost+"getHostName: "+localhostName);
                    return false;   
                }
            }
        } catch (Exception e)  {
            logError("isUserLocalDeveloper error", e);
        }
        return false;
    }


    public Session/*change to IContext*/ getSession(String sessionID){
        IContext ctx = (IContext)find("/homes/dynamide/sessions");
        if (ctx == null){
            throw new RuntimeException("Dynamide homes not set up: expected to find /homes/dynamide/");
        }
        return (Session)ctx.getAttribute(sessionID);
    }

    public synchronized void rebindSession(String id, Object sessionData){
        IContext ctx = (IContext)find("/homes/dynamide/sessions");
        ctx.rebindAttribute(id, sessionData); //ContextNode synchronizes its own list.
    }

    public synchronized void addSession(String id, Object sessionData)
    throws ObjectAlreadyBoundException {
        IContext ctx = (IContext)find("/homes/dynamide/sessions");
        ctx.bindAttribute(id, sessionData); //ContextNode synchronizes its own list.
    }

    public void addUserSession(String userSessionContextName, String USER, Session session)
    throws SessionBusyException {
        IContext sessions = ensureContextPath(userSessionContextName);
        String tn = Thread.currentThread().getName();
        if ( sessions != null ) {
            synchronized (sessions){
                Session oldsession = (Session)sessions.getContext(USER);
                if ( oldsession != null ) {
                    System.out.println("\r\n=====1 "+session+" thread "+tn);
                    if (oldsession.isBusy()){
                        System.out.println("\r\n=====2 "+session+" thread "+tn);
                        System.out.println("concurrent lock on Session for user "+USER+" old session id: "+oldsession.getSessionID()+" new session id: "+session.getSessionID());
                        throw new SessionBusyException("concurrent lock on Session for user "+USER+" old session id: "+oldsession.getSessionID()+" new session id: "+session.getSessionID());
                    }
                    if (oldsession == session){
                        System.out.println("\r\n=====2 "+session+" thread "+tn);
                    } else {
                        System.out.println("\r\n=====3 "+session+" thread "+tn);
                        oldsession.close();
                        logWarn("ResourceManager closed redundant usersession: "+oldsession.toString());
                        oldsession = null;
                    }
                }
                System.out.println("\r\n=====4 "+session+" thread "+tn);
                sessions.rebind(USER, session);
                session.rebindAttribute("UserSessionsIContext", sessions);
                session.rebindAttribute("UserSessionsIContext_USER", USER);
            }
        } else {
                    System.out.println("\r\n=====ERROR "+session+" thread "+tn);
            logError("Could not addUserSession "+USER+" in "+userSessionContextName);
        }
                    System.out.println("\r\n=====5 "+session+" thread "+tn);

    }

    private void removeUserSession(Session session){


        IContext sessionsList = (IContext)session.getAttribute("UserSessionsIContext");
        if ( sessionsList != null ) {
            //MEMORY_LEAKS: this next line is just a test.  noisy.
            // ..... logInfo(dumpContext(sessionsList, "  ", false));

            synchronized (sessionsList){
                Object sessionsListUSER = session.getAttribute("UserSessionsIContext_USER");
                if (sessionsListUSER != null){
                    String USER = sessionsListUSER.toString();
                    Object obj = sessionsList.remove(USER);
                    logInfo("**** removeUserSession: remove "+USER+" from UserSessionsIContext for "+session+" which has getUSER()=="+session.getUSER()+" OBJECT::::::"+obj);
                } else {
                    logInfo("**** removeUserSession: UserSessionsIContext_USER was null for "+session);
                }
            }
            //logInfo(dumpRootContext(false));

        } else {
            logInfo("**** removeUserSession: UserSessionsIContext was null for "+session);
        }
    }

    /** Can only be called from com.dynamide.Session.close(), otherwise throws SecurityException.
     * @throws SecurityException if not called from com.dynamide.Session.close()
     */
    public synchronized void removeSession(com.dynamide.Session session)
    throws SecurityException {
        // %% TODO: lock down the context object so that you can't remove the session from the context
        // manually without this method.  See IContext.lockPermissions.

        logDebug("removeSession: "+session);
        DynamideSecurityManager.checkCallStack("com.dynamide.Session", "close", 1, false);
        removeInstalledJob(session.getFullAppname(), true);
        IContext ctx = (IContext)find("/homes/dynamide/sessions"); //%% hardcoded "dynamide"
        ctx.removeAttribute(session.getSessionID());
        removeSessionFromPool(session);
        removeUserSession(session);
    }

    public static final int POOL_ACTION_REPOOL = 1;
    public static final int POOL_ACTION_REMOVE= 2;


    public synchronized void initPool(String uri){
        String [] findarray = {"homes", "dynamide", SESSIONS_POOLED_NAME};
        IContext ctx = (IContext)find(findarray);
        if ( ctx != null ) {
            IContext urictx = ctx.getContext(uri);
            if ( urictx == null ) {
                urictx = ctx.rebind(new ContextNode(uri));
            }
        }
    }

    public void repoolSession(Session session){
        doPoolAction(POOL_ACTION_REPOOL, session);
    }

    public void removeSessionFromPool(Session session){
        doPoolAction(POOL_ACTION_REMOVE, session);
    }

    public synchronized void doPoolAction(int action, Session session){ //%% todo: change Session to IContext.
        if ( session == null ) {
            logWarn("Can't doPoolAction, session was null");
            return;
        }
        if (session.isPoolable()) {
            String sessionID = session.getSessionID();
            String uri = session.getApplicationPath();
            String [] findarray = {"homes", "dynamide", SESSIONS_POOLED_NAME};
            IContext ctx = (IContext)find(findarray);
            if ( ctx != null ) {
                Pool urictx;
                synchronized (ctx){
                    urictx = (Pool)ctx.getContext(uri);
                    if ( urictx == null && (action != POOL_ACTION_REMOVE)) {
                        urictx = new Pool(null, uri);
                        ctx.rebind(urictx);
                        logInfo("Initializing pool for "+uri+" since findPooledSession context was null: "+Tools.arrayToString(findarray));
                    }
                }
                switch (action) {
                    case POOL_ACTION_REPOOL:
                        int poolMax = Tools.stringToIntSafe(session.getPropertyStringValue("poolMax"), -1);
                        int poolCount = urictx.getContextCount();
                        if (poolMax > -1 && (poolMax<=poolCount)){
                            logWarn("Pool limit reached for session: "+sessionID+" poolMax: "+poolMax+" poolCount: "+poolCount);
                            break;
                        }
                        session.setPooled(true);
                        urictx.rebind(session.getSessionID(), session);
                        urictx.setPoolMax(session.getPropertyStringValue("poolMax"));
                        Assembly.accessed(urictx);
                        Assembly.accessed(session);
                        logInfo("Session repooled: "+uri+" id: "+sessionID);//+" context, after: \r\n"+dumpContext(urictx, "  ", false)+"\r\nat\r\n"+Tools.getStackTrace());
                        break;
                    case POOL_ACTION_REMOVE:
                        session.setPooled(false);
                        //logInfo("context, before: \r\n"+dumpContext(urictx, "  ", false));
                        if (urictx!=null){
                            //it would be null if the session had not been pooled before
                            // and this time the action was just to remove it.
                            urictx.remove(sessionID);
                            logInfo("Session removed from pool: "+uri+" id: "+sessionID);//+" context, after: \r\n"+dumpContext(urictx, "  ", false));
                        }
                        break;
                    default:
                        logError("No known action in doPoolAction: "+action);
                }
            }
        }
    }

    public synchronized boolean isPoolMaxed(String applicationPath){
        String [] findarray = {"homes", "dynamide", SESSIONS_POOLED_NAME, applicationPath};
        Pool pool = (Pool)find(findarray);
        if ( pool != null ) {
            return pool.getPoolMax()<=pool.getContextCount();
        }
        return false;
    }

    public Session findPooledSession(WebAppEntry entry){
        return findPooledSession(entry.getURI());
    }

    public synchronized Session findPooledSession(String uri){
        Session session = null;
        //String [] findarray = {"homes", "dynamide", SESSIONS_POOLED_NAME, uri};
        String [] findarray = {"homes", "dynamide", SESSIONS_POOLED_NAME};
        IContext pooled = (IContext)find(findarray);
        //synchronized (pooled){
            IContext ctx = (IContext)pooled.getContext(uri);
            if (ctx!=null){
                IContext sc = ctx.removeFirstContext();
                if (sc != null && sc instanceof Session){
                    session = (Session)sc;
                    session.setPooled(false);
                }
            } else {
                //initPool(uri);
                //logInfo("Initializing pool for "+uri+" since findPooledSession context was null: "+Tools.arrayToString(findarray));
            }
        //}
        return session;
    }


    private static com.bitmechanic.sql.ConnectionPoolManager poolMgr = null;

    public static com.bitmechanic.sql.ConnectionPoolManager getConnectionPoolManager()
    throws java.sql.SQLException {
        if ( poolMgr == null ) {
            poolMgr = new ConnectionPoolManager(120); //number of seconds to wait between pool checks.
            //poolMgr.setTracing(true); //turn to true to send trace of activity to stderr.
        }
        return poolMgr;
    }

    public String dumpConnectionPoolStats(boolean full){
        StringBuffer buf = new StringBuffer();
        buf.append("Datbase PoolManager Stats");
        try {
            Enumeration en = getConnectionPoolManager().getPools();
            while (en.hasMoreElements()){
                ConnectionPool p = (ConnectionPool)en.nextElement();
                buf.append("\r\nPool Statistics: ["+p.getAlias()+"]");
                buf.append("\r\n  Current size: " + p.size() + " of " + p.getMaxConn());
                buf.append("\r\n  Connection requests: " + p.getNumRequests());
                buf.append("\r\n  Number of waits: " + p.getNumWaits());
                buf.append("\r\n  Number of timeouts: " +p.getNumCheckoutTimeouts());
                if (full) buf.append("\r\n  DumpInfo: \r\n" +p.dumpInfo());
            }

        } catch( Exception e ){
            buf.append("ERROR getting stats: "+e);
        }
        return buf.toString();
    }

    /** @param dbContextName The name of an element in the context /conf/databases in the
     *  root Dynamide context tree.  This tree is protected by the RootResourceManager.
     *  In the element are the username, driver name, password, and so on.
     */
    public synchronized com.dynamide.db.RDBDatabase openDatabase(String dbContextName, Session session){
        IContext dbc = (IContext)find("/conf/databases/"+dbContextName);
        if ( dbc != null ) {
            String debugSQL = (String)dbc.getAttribute("debugSQL");
            boolean bDebugSQL = Tools.isTrue(debugSQL);
            String user = (String)dbc.getAttribute("user");
            String password = (String)dbc.getAttribute("password");
            String uri = (String)dbc.getAttribute("uri");
            String driverName = (String)dbc.getAttribute("driverName");
            String name = (String)dbc.getAttribute("name"); //just the database name, e.g. template1, should match the node name, but is easier as an attribute.
            String account = (String)dbc.getAttribute("account");
            // if account is caller's account, yada yada. %%
            com.dynamide.db.RDBDatabase db = new com.dynamide.db.RDBDatabase(uri, driverName, user, password, session);
            db.setContextName(dbContextName);//for widgets to know which database they opened.
            db.setDebugSQL(bDebugSQL);
            return db;
        }
        logError("Couldn't find database context: "+dbContextName);
        return null;
    }

    private static long g_errorID = 0;
    public static long getNextErrorID(){
        return g_errorID++;
    }
    
    public String errorIDToFilename(String id){
    	return FileTools.fixFilename(getErrorLogsDir()+'/'+id+".html");
    }

    public static String errorIDToHref(String id){
        return getErrorLogsURI()+'/'+id+".html";
    }

    /** @return the id of the error - which can be turned into a URL with
    */
    public static String writeErrorLog(Session session, String id, String resourcename, String message, String stackTrace, String errorClassname){
        String fullID = id+'.'+(g_errorID++);
        try {
            String fn = fullID+".html";
            FileTools.saveFile(g_errorlogsDir, fn, "<html><body><h2>Application Error</h2>"
            +"<b>Target ID:</b> "+fullID
            +"<hr />Message<hr /><pre>"+message
            +"</pre><hr />Resource Name<hr /><pre>"+resourcename
            +"</pre><hr />Stack Trace<hr /><pre>"+stackTrace
            +"</pre></body></html>");
            String sessionid = "";
            String applicationid = "";
            String threadid = Thread.currentThread().getName();
            String account = "";  //%%%
            String login = "";  //%%%
            String uri = getErrorLogsURI()+'/'+fn;
            System.out.println("---> ResourceManager writing ERROR LOG "+uri);
            if ( session != null ) {
                sessionid = session.getSessionID();
                applicationid = (String)session.get("applicationID");
                account = session.getAccount();
                login = session.getUSER();
                session.addErrorURI(uri);
            }
            getErrorLogger().log(org.apache.log4j.Priority.ERROR,
                                 sessionid,
                                 applicationid,
                                 threadid,
                                 account,
                                 login,
                                 resourcename,
                                 stackTrace,
                                 message,        //StringTools.escape(message))
                                 errorClassname,
                                 message.substring(0,20)+"...",       //StringTools.escape(message));
                                 uri);

            return fullID;
        } catch (Throwable e)  {
            System.out.println("ERROR: ResourceManager.writeErrorLog couldn't log error. "+e);
            return fullID;
        }
    }

    public static String writeErrorLog(Session session, String id, String resourcename, String message, Throwable t){
        String stackTrace = Tools.errorToString(t, true);
        String throwableName = t != null ? t.getClass().getName() : "";
        return writeErrorLog(session, id, resourcename, message, stackTrace, throwableName);
    }

    public void flushErrorLog(){
    	checkErrorLogger();
    	m_errorLogger.flush();
    }

    public static ILogger getErrorLogger()
    throws Exception {
        if (_singleton == null){
            throw new Exception("Cannot log in ResourceManager without calling installSingletonRootResourceManager() or "
                                 +" createStandalone() first.");
        }
        _singleton.checkErrorLogger();
        return _singleton.m_errorLogger;
    }
    
    private void checkErrorLogger() {
    	if (m_errorLogger != null){
    		return;
    	}
    	//DO NOT CALL getErrorLogger() in here. 
        String loggingClassName = (String)find("/conf/ResourceManager/loggingClass");
        String confString = ""; //%% could make this a conf string in the context.hxml file.
        ILogger il = null;
        try {
        	il = (ILogger)Class.forName(loggingClassName).newInstance();
        } catch (Throwable t){
        	il = new com.dynamide.resource.HtmlLogger();        	
        }
        try {
        	if (il != null) il.connect(this, confString);
        	m_errorLogger = il;
        } catch (Exception e){
        	System.err.println("***** BIG ERROR ***** in ResourceManager: cannont connect ErrorLogger:"+e);
        }
    }

    private ILogger m_errorLogger = null;
    
    public String dumpRootContextPage(){
        String res = "<html><title>Dynamide ResourceManager root context</title><body><pre>"+dumpRootContext(true)+"</pre></body></html>";
        return res;
    }

    public final Session createSession(String urlpath)
    throws Exception {
        // %% what's to prevent you from calling this on another account's sessions....
        Session session = Session.createSession(urlpath);
        return session;
    }

    public final void createApplication(Assembly assembly, String appName)
    throws Exception {
        if (assembly == null){
            String body = "ERROR: assembly is null createApplication(null, \""+appName+"\").";
            System.out.println(body);
            throw new Exception(body);
        }
        // %% check that thread's home/account is the same as the one being requested.
        synchronized (assembly) {
            String assemblyDir = (String)assembly.getAttribute(Assembly.FULLPATH);
            logDebug("using assemblyDir: "+assemblyDir);
            File appDir = new File(assemblyDir, Tools.fixFilename("apps/"+appName));
            if (appDir.exists()){
                String body = "ERROR: project directory already exists ("+appDir.getCanonicalPath()+").  Choose a new name.";
                System.out.println(body);
                return;
            } else {
                java.io.File f = FileTools.createDirectory(assemblyDir, Tools.fixFilename("apps/"+appName));
                String appDirName = (f!=null) ? f.getCanonicalPath() : "";
                if (appDirName.length()>0){
                    logDebug("Created Application Directory: "+appDirName);
                    FileTools.createDirectory(appDirName, "resources");
                    FileTools.createDirectory(appDirName, "resources/css");
                    FileTools.createDirectory(appDirName, "resources/images");
                    FileTools.createDirectory(appDirName, "resources/widgets");
                    copySystemFile(assembly, "resources/system/application.xml", appDirName, "application.xml" );
                    copySystemFile(assembly, "resources/system/fielddefs.xml", appDirName, "fielddefs.xml" );
                    String applicationXml = Tools.fixFilename(appDirName+"/application.xml");
                    XDB x = new XDB();
                    Element root = x.openXML(applicationXml);
                    Element valueEl = x.selectFirst(root, "/application/properties/property[@name='applicationID']/value");
                    valueEl.setText(appName);//%% could use a longer application ID
                    x.saveXML(applicationXml);
                }
            }
        }
    }

    private boolean copySystemFile(Assembly assembly, String sourceID, String appDirName, String filename)
    throws DynamideException {
        String fn = assembly.getResourceFilename("resources/system/application.xml");
        if (fn.length() == 0 ) {
            throw new DynamideException("Could not find system resource: "+sourceID);
        }
        return FileTools.copyFile(fn, Tools.fixFilename(appDirName+"/"+filename));
    }

    public final Object loadClass(String className)     //%% might want to lock this down a bit.  Now you can create any class from webmacro. :(
    throws Exception {                                         // %% maybe by role of user.
        return Class.forName(className).newInstance();
    }

    public final Class loadClassStatic(String className)     //%% might want to lock this down a bit.  Now you can create any class from webmacro. :(
    throws Exception {                                         // %% maybe by role of user.
        return Class.forName(className);
    }

    public final Object callStaticMethod(String className, String methodName)
    throws Exception {
        Class theClass = loadClassStatic(className);
        Class[] params = {};
        Object[] invokeParams = {};
        return callStaticMethod(theClass, methodName, params, invokeParams);
    }

    public final Object callStaticMethod(Class theClass, String methodName)
    throws Exception {
        Class[] params = {};
        Object[] invokeParams = {};
        return callStaticMethod(theClass, methodName, params, invokeParams);
    }

    public final Object callStaticMethod(Class theClass, String methodName, Class[] params, Object[] invokeParams)
    throws Exception {
        java.lang.reflect.Method method = theClass.getMethod(methodName, params);
        Object result = method.invoke(null, invokeParams);
        return result;

    }


    public static void usage(){
        System.out.println("Usage: ");
        System.out.println("java com.dynamide.resource.ResourceManager [-root <resource-root>] <option>");
        System.out.println("   options: ");
        System.out.println("      -testFind <options>");
        System.out.println("          [-uri uri]");
        System.out.println("          [-home <home> -assembly <assembly-name> -interface <number> -build <number>]");
        System.out.println("         test the IContext.find() call from ResourceManager.");
        System.out.println("      -testImports");
        System.out.println("             test imports in assemblies from ResourceManager.");
        System.out.println("      -testSession -uri uri -root resource_root");
        System.out.println("             test creating a Session.");
        System.out.println("      -dumpContext <path> [-separator sep]");
        System.out.println("             where sep is / or some other character, and path is a context path.");
        System.out.println("      -createAssembly: test the security checks when creating an assembly");
    }


    public static void main(String args[])
    throws Exception {
        //System.out.println("args used: "+Tools.arrayToString(args, " "));
        if ( args.length==0 ) {
            usage();
            System.exit(0);
        }
        try {
            Opts opts = new Opts(args);
            String resroot = opts.getOption("-root");

            String RESOURCE_ROOT =  resroot.length() > 0
               ? resroot
               : getResourceRootFromEnv();

            ResourceManager rm = createStandalone(RESOURCE_ROOT);
            if (opts.getOptionBool("-dumpRoot")){
                if (opts.getOptionBool("-html")){
                    String res = rm.dumpRootContextPage();
                    String outfile = opts.getOption("-o");
                    if (outfile.length()>0){
                        FileTools.saveFile("", outfile, res);
                    } else {
                        //IContext context = (IContext)rm.find(opts.getOption("-dumpContext"), opts.getOption("-separator"));
                        System.out.println(res);
                    }
                } else {
                    System.out.println(ResourceManager.dumpContext(rm, "", false, ""));
                }
            } else if (opts.getOptionBool("-dumpContext")){
                    System.out.println("========= dumpContext test: =======");
                    IContext context = (IContext)rm.find(opts.getOption("-dumpContext"), opts.getOption("-separator"));
                    System.out.println("Context: \r\n"+ResourceManager.dumpContext(context, "", false, ""));
                    System.out.println("===================================");
            } else if (opts.getOptionBool("-testFind")){

                //works, but verbose: System.out.println("Root Context: \r\n"+rm.dumpRootContext());

                System.out.println("========= IContext.find test: ===========");

                //These first two form the beginning of a policy to find URI mappings.
                //First look in the global one, then look in homes.  See uriToApp().  See also findAssembly(String account, String targetAssemblyName)

                String [] s = {"homes", "web-apps", "/dynamide/demo"};
                System.out.println("\r\n find: "+Tools.arrayToString(s));
                System.out.println("    "+rm.find(s));

                String [] s1 = {"homes", "dynamide", "web-apps", "/dynamide/demo"};
                System.out.println("\r\n find: "+Tools.arrayToString(s1));
                Object found = rm.find(s1);
                System.out.println("    found.class: "+found.getClass().getName()+" found: "+found);

                String [] s2 = {"homes", "web-apps"};
                System.out.println("\r\n find: "+Tools.arrayToString(s2));
                System.out.println("    "+rm.find(s2));

                String [] s3 = {"homes", "web-apps", "foobar"};
                System.out.println("\r\n find (foobar is not in context): "+Tools.arrayToString(s3));
                System.out.println("    "+rm.find(s3));

                System.out.println("\r\n uriToApp(\""+opts.getOption("-uri")+"\"): "+rm.uriToApp(opts.getOption("-uri")));

                String assemblyName = opts.getOption("-assembly");
                String home = opts.getOption("-home");
                String interfaceNumber = opts.getOption("-interface");
                String build = opts.getOption("-build");
                if (home.length()>0){
                    System.out.println("\r\n findAssembly(\""+home+"\", \""+assemblyName+"\", \""+interfaceNumber+"\"): \r\n"
                                      +rm.findAssembly(home, assemblyName, interfaceNumber, build));
                    System.out.println("\r\n\r\n findSharedAssemblies: "+rm.findSharedAssemblies());
                    System.out.println("\r\n\r\n findAssembliesInAccount '"+home+"' : "+rm.findAssembliesInAccount(home));
                }
                System.out.println("===================================");

            } else if (opts.getOptionBool("-testImports")){

                System.out.println("========= imports test: ===========");
                System.out.println("Find build 1 of com-dynamide-apps assembly in");
                System.out.println(" /homes/dynamide/assemblies/com-dynamide-apps/1");
                String dmappsPath = "/homes/dynamide/assemblies/com-dynamide-apps/1";  //find build 1
                Object obj = rm.find(dmappsPath, "/");
                System.out.println("ctx: "+obj.getClass().getName()+"::"+obj);
                Assembly dmapps = (Assembly)obj;
                if ( dmapps != null ) {
                    System.out.println("find(\""+dmappsPath+"\") == "+dmapps.getAttribute(Assembly.FULLPATH));
                    System.out.println("getImportsPath: "+dmapps.getImportsPath());
                    IContext ctx;
                    String res;
                    ctx = (IContext)dmapps.getResource("resources/css/dynamide.css", true);
                    res = Assembly.listResource(ctx);
                    System.out.println("\r\ngetResource(\"resources/css/dynamide.css\")\r\n---------------\r\n"+(res));
                    System.out.println("---------------");
                    ctx = (IContext)dmapps.getApplicationResource("demo", "resources/css/dynamide.css");
                    res = Assembly.listResource(ctx);
                    System.out.println("\r\ngetApplicationResource(\"demo\", \"resources/css/dynamide.css\")\r\n---------------\r\n"+(res));
                    System.out.println("---------------");
                    ctx = (IContext)dmapps.getApplicationResource("demo", "page1.xml");
                    res = Assembly.listResource(ctx);
                    System.out.println("\r\ngetApplicationResource(\"demo\", \"page1.xml\")\r\n---------------\r\n"+(res));
                    System.out.println("---------------");
                } else {
                    System.out.println("couldn't find "+dmappsPath);
                }

                System.out.println("===================================");
            } else if (opts.getOptionBool("-createAssembly")){
                System.out.println("========= createAssemblyLoop ======");
                ResourceManager root = getRootResourceManager();
                com.dynamide.util.Profiler profiler = com.dynamide.util.Profiler.getSharedProfiler();
                if (profiler!=null) profiler.enter("createAssemblyLoop");
                for (int i=0; i < 1000; i++) {
                    root.createAssembly(null, "", "", RESOURCE_ROOT);
                }
                if (profiler!=null) profiler.leave("createAssemblyLoop");
                if (profiler!=null) profiler.dump();
                System.out.println("===================================");
            } else if (opts.getOptionBool("-testSession")){
                String urlpath = opts.getOption("-url");
                Session session = Session.createSession(urlpath);
            } else if (opts.getOptionBool("-testSessionID")){
                ResourceManager root = getRootResourceManager();
                long start = Tools.now().longValue();
                for (int i=0; i < 100; i++) {
                    String s = root.getNextSessionID();
                }
                long finish = Tools.now().longValue();
                System.out.println("100 loops: "+(finish-start)+" ms");
            } else  {
                usage();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("ERROR: [70]"+e);
        }
        System.out.println("DONE.");
        System.exit(0);  // _singleton keeps us running.


    }



}



//=========== obviated ========================

    /*
     * public Pool addPool(String poolName)
    throws SecurityException {
        if ( getPool(poolName) != null ) {
            throw new SecurityException("Can't replace existing pool: "+poolName);
        }
        m_pools.put(poolName, new Pool());
    }
    */


    /** These overloaded methods get a resource from a named pool, the inherited ones look in the default pool.
     */
    /*public boolean hasPooledResource(String poolName, String key) {
        Pool pool = (Pool)getPool(poolName);
        if ( pool != null ) {
            return pool.hasResource(key);
        }
        return false;
    }
     */

    /** These overloaded methods get a resource from a named pool, the inherited ones look in the default pool.
     */
   /* public Object getPooledResource(String poolName, String key)
    throws SecurityException {
        return getPool(poolName).getResource(key);
    }
    */

    //don't use Objects for keys, since you'll have to run their toString method, in our trusted space.
    /** These overloaded methods get a resource from a named pool, the inherited ones look in the default pool.
     */
    /*public void addPooledResource(String poolName, String key, Object obj)
    throws SecurityException {

         Resource prev = getResource(key);
        if ( prev != null && prev.canPool() ) {
            Resource resource = new Resource(obj);
        }


    }
    */
