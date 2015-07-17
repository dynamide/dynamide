// java com.dynamide.resource.Assembly -testDiver C:\temp\dynamide-sandboxes\RESOURCE_ROOT
// java com.dynamide.resource.Assembly -test C:\temp\dynamide-sandboxes\assembly-test\homes\dynamide\assemblies\com-dynamide-apps -root C:\temp\dynamide-sandboxes\assembly-test -account dynamide
package com.dynamide.resource;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jdom.Element;

import com.dynamide.Constants;
import com.dynamide.DynamideException;
import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.security.DynamideSecurityManager;
import com.dynamide.util.FileTools;
import com.dynamide.util.IFileDiverListener;
import com.dynamide.util.Log;
import com.dynamide.util.Opts;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

    /**
    <p>
    Each assembly turns into a cache of resources at runtime.  This Assembly class extends Pool, and keeps
    each resource in an IContext node.
    </p>

    <p>
    The context key is the path of the resource name of the resource, which is the name relative to the assembly
    root or to the resources directories inside the assembly or application.
    When the resource is not in cache, the context object is null, but the key
    is present.  So hasContext() will return true, but getContext will still return null.
    When the resource is in cache, the IContext returned by getContext(key) will have information about the
    resource's contents.  It will have the following attributes:
    <ul>
    <li>ACCESSED - java.lang.Long representing the system time the contents were last accessed.</li>
    <li>BINARY - String value of "true" or "false"</li>
    <li>CACHED - java.lang.Long representing the system time the contents were cached.</li>
    <li>CACHE_REL - String value of the URI, without the ResourceManager.CACHE_URI prefix.</li>
    <li>CONTENT - String, or bytes[] if binary resource</li>
    <li>FULLPATH - String representing the system name where the resource was located.</li>
    <li>HITS - java.lang.Long representing the number of times the resource has been requested.</li>
    <li>MODIFIED - java.lang.Long representing the system time the File as last modified.</li>
    </ul>
    The most important is the CONTENT attribute, which is the resource's contents.
    The string names of these attributes are defined in this class, such as Assembly.CONTENT.

    </p>
    <p>Attributes associated with <i>this</i> object are:
    <ul>
    <li>ASSEMBLY - referrence to <i>this</i> assembly</li>
    <li>ASSEMBLYDIR - directory associated with <i>this</i> assembly</li>
    <li>ASSEMBLY_STANDARD_NAME - filename and extension to use for assembly xml description file.</li>
    <li>BASENAME - name associated with <i>this</i> assembly</li>
    <li>INTERFACE - interface number associated with <i>this</i> assembly</li>
    <li>BUILD - build string associated with <i>this</i> assembly</li>
    <li>IMPORTS - list of AssemblyImport objects that point to possible imports, and actual imports.
                  each of these objects also points to basename/build/interface numbers that are compatible.</li>
    <li></li>
    </ul>
     * Note that you should use the symbolic name, since they may map to some other string,
     * e.g. Assembly.BASENAME == "basename".
    </p>
    */

public class Assembly extends Pool implements IFileDiverListener, IContext {

    /** Illegal to call directly - use ResourceManger.createAssembly() instead, from the Root ResourceManger.
     *
     * @throws SecurityException if the direct caller is not ResourceManger.createAssembly() on the Root ResourceManger.
     */
    public Assembly(DynamideObject owner)
    throws SecurityException {
        super(owner);
        throw new SecurityException("FORBIDDEN"); //must have constructor to be legal java, but you can't use this one in practice.
    }

    /** Illegal to call directly - use ResourceManger.createAssembly() instead, from the Root ResourceManger.
     *
     * @throws SecurityException if the direct caller is not ResourceManger.createAssembly() on the Root ResourceManger.
     */
    public Assembly(DynamideObject owner, String key, String account, String RESOURCE_ROOT)
    throws SecurityException {
        super(owner, key);
        temporaryBuildName = key+"-CONSTRUCTING";
        checkCallStack();
        m_account = account;
        LOG_CACHING = ( Log.getInstance().isEnabledFor(Constants.LOG_CACHING_CATEGORY, org.apache.log4j.Priority.DEBUG) );
        rebindAttribute("RESOURCE_ROOT", RESOURCE_ROOT);
    }

    //private constructor, doesn't check call stack.  For testing in main() only.  Also ensures that no one can call new Assembly().
    private Assembly (String account) {
        super(null);
        m_account = account;
        LOG_CACHING = ( Log.getInstance().isEnabledFor(Constants.LOG_CACHING_CATEGORY, org.apache.log4j.Priority.DEBUG) );
    }


    //=============================================================================
    // ==  IMPORTANT: keep class javadoc up-to-date when you add magic attributes.
    //=============================================================================


    //== Standard attributes associated with any resource node:
    public final static String ACCESSED = "ACCESSED";
    public final static String BINARY = "BINARY";
    public final static String CACHED = "CACHED";
    public final static String CACHE_REL = "CACHE_REL";
    public final static String CONTENT = "CONTENT";
    public final static String FULLPATH = "FULLPATH";
    public final static String HITS = "HITS";
    public final static String MODIFIED = "MODIFIED";

    //== Attributes associated with *this* object are:
    public final static String ASSEMBLY = "ASSEMBLY";
    public final static String ASSEMBLY_STANDARD_NAME = "assembly.xml";
    public final static String ASSEMBLYDIR = "ASSEMBLYDIR";
    public final static String BASENAME = "basename";
    public final static String BUILD = "build";
    public final static String IMPORTS = "imports";
    public final static String INTERFACE = "interface";
    public final static String NAME = "name";

    /** key for the collection of basenames IContexts, which hold the
     *  list of assembly-build names that provide this basename.
     */
    public final static String BASENAMES = "basenames";

    public final static String CAT_CACHE = "com.dynamide.resource.Assembly.Caching";
    public final static String DEFAULT_INTERFACE_NUMBER = "0";
    public final static String FALSE = "false";
    public final static String TRUE = "true";
    public final static String IN_MEMORY_RESOURCE = "IN_MEMORY_RESOURCE";
    protected static boolean LOG_CACHING = false;
    public final static String WIDGETS_DIR_REL_RESOURCES = "widgets/";

    public static Assembly createAssembly(ResourceManager resourceManager,
                                          IContext assemblies,
                                          File assemblyDir,
                                          String assemblyName, // name w/o path
                                          String account,
                                          String RESOURCE_ROOT,
                                          boolean isCacheOn)
    throws Exception {
        com.dynamide.util.Log.info(Assembly.class, "Assembly.createAssembly: "+assemblyName+":"+account+":"+isCacheOn);
        String assemblyXMLName = assemblyDir.getCanonicalPath()+File.separator+"assembly.xml";
        File assemblyXML = new File(assemblyXMLName);
        if ( ! assemblyXML.exists() ) {
            throw new DynamideException("assembly.xml does not exist in assembly.  Should be: "+assemblyXML);
        }
        String assemblyDirName = assemblyDir.getName();
        String assemblyDirPath = assemblyDir.getCanonicalPath();

        JDOMFile assemblyJDF = new JDOMFile(null, assemblyXMLName);
        String assemblyBasename = assemblyJDF.valueOf("basename");
        String assemblyInterfaceNumber = assemblyJDF.valueOf("interface");
        String assemblyBuild = assemblyJDF.valueOf(BUILD);
        String assemblyNameBuild = assemblyBasename+'-'+assemblyBuild;
        if ( ! assemblyName.equals(assemblyNameBuild) ) {
            Log.warn(Assembly.class, "assembly filename ("+assemblyName+") should be: ("+assemblyNameBuild+"), specified in "+assemblyXMLName);
        }
        assemblyInterfaceNumber = assemblyInterfaceNumber == null ? "" : assemblyInterfaceNumber.trim();
        if (assemblyInterfaceNumber.length()==0){
            assemblyInterfaceNumber = Assembly.DEFAULT_INTERFACE_NUMBER;
            Log.warn(Assembly.class, "/assembly/interface element not found in assembly.xml in "+assemblyXMLName
                   +" Using default interface number: "+Assembly.DEFAULT_INTERFACE_NUMBER);
        }
        com.dynamide.util.Log.info(Assembly.class, "basename: "+assemblyBasename+" interface: "+assemblyInterfaceNumber+" xml: "+assemblyXMLName+" RESOURCE_ROOT:"+RESOURCE_ROOT);
        Assembly assembly = resourceManager.createAssembly(resourceManager, assemblyNameBuild, account, RESOURCE_ROOT);
        assembly.setAssemblyDirectory(assemblyDirPath);
        assembly.bindAttribute(BUILD, assemblyBuild);
        assembly.bindAttribute("interface", assemblyInterfaceNumber);
        //this was a temporary hack. 2005-09-19. assembly.bindAttribute(ResourceManager.CACHE_ON, ""+isCacheOn);
        assemblies.bind(assemblyNameBuild, assembly);
        assembly.temporaryBuildName = null;  //once it is null again, toString will call getBuildName()

        updateBasenamesInterfacesMap(assemblies, assembly, assemblyDirPath, assemblyBasename, assemblyInterfaceNumber);

        String webappsFullPath = Tools.fixFilename(assemblyDirPath+"/web-apps.xml");
        if ( (new File(webappsFullPath)).exists() ) {
            //use assemblyInterfaceNumber instead. String interfaceNumber = (String)assembly.getAttribute("interface");
            Log.debug(Assembly.class, "calling callWebApps: "+assembly+";"+webappsFullPath+";"+account+";"+assemblyName+";"+assemblyInterfaceNumber);
            resourceManager.callWebApps(assembly, webappsFullPath, account, assemblyBasename/*assemblyName*/, assemblyInterfaceNumber); //filesi is the name of the assembly.
        } else {
            String appsFullPath = Tools.fixFilename(assemblyDirPath+"/apps");
            if ((new File(appsFullPath)).exists()){
                //otherwise it is a libary, and has no apps, and that's OK
                Log.warn(Assembly.class, "Assembly has an apps directory, but didn't have a web-apps.xml file, which should have been named: "+Tools.fixFilename(webappsFullPath));
            }
        }
        return assembly;
    }

    /** Get the content of the resource as a byte array.
     *
     *  @return The byte array, unless the resource was not binary, in which case
     *  an excpetion is thrown.
     */
    public static byte[] extractBinaryResourceContent(IContext node)
    throws ResourceException {
        if ( node == null ) {
            Log.warn(Assembly.class, "extractBinaryResourceContent was passed a null node");
            return new byte[0];
        }
        Object o = node.getAttribute(CONTENT);
        if ( Tools.isTrue((String)node.getAttribute(BINARY)) ) {
            return (byte[])o;
        }
        throw new ResourceException("Resource was not binary: "+node.getKey());
    }


    /** Get the content of the resource as a String from an IContext node.
     *
     * @return A String, unless the resource was binary, in which case
     *  an exception is thrown.
     *
     * @throws ResourceException
     */
    public static String extractResourceContent(IContext node)
    throws ResourceException {
        if (LOG_CACHING) Log.debug(CAT_CACHE, "extractResourceContent: "+node);
        if ( node == null ) {
            Log.warn(Assembly.class, "extractResourceContent was passed a null node");
            return "";
        }
        synchronized (node) {
            if ( Tools.isTrue((String)node.getAttribute(BINARY)) ) {
                throw new ResourceException("Resource is binary: "+node.getKey());
            }

            if ("true".equals(ResourceManager.getRootResourceManager().getAttribute(ResourceManager.CACHE_ON))){
                return (String)node.getAttribute(CONTENT);
            } else {
                    if (LOG_CACHING) Log.debug(CAT_CACHE, "extractResourceContent calling readResource: "+node);
                    readResource(node);  //makes sure the node's content is up-to-date
                    String res = (String)node.getAttribute(CONTENT);
                    if (res==null){
                        throw new ResourceException("Content not found in: "+node);
                    }
                    return res;
                // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NOT SURE THIS IS A GOOD IDEA -- leaving it out for now.
                // =============================
                //   read content always.
                //   but you must have this in a non-static method.
                //   See: line 257: getResource(String key)
                //   By allowing this, you might be circumventing the security of an Assembly.
                //  Probably better to use /static or /cache as a place.
                // =============================
                // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
            }
        }
    }

    /** Find Assembly in "assemblies" IContext param, matching basename param, with these rules:
     * <ul>
     *   <li>First rule: find assembly for build number</li>
     *   <li>Second rule: find assembly for interface number</li>
     * </ul>
     */
    public static Assembly findAssembly(IContext assemblies, String basename, String interfaceNumber, String build){
        Assembly result;
        String assembliesFullpath = (String)assemblies.getAttribute(FULLPATH);
        // First rule: find assembly pointer for build number
        if (build!=null && build.length()>0){
            com.dynamide.util.Log.debug(Assembly.class, "find by build: "+build);
            result = (Assembly)assemblies.getContext(basename+'-'+build);
            if ( result != null ) {
               com.dynamide.util.Log.debug(Assembly.class, "found: "+result);
                return result;
            }
            com.dynamide.util.Log.debug(Assembly.class, "NOT found");
        }
        // Second rule: find assembly pointer for interface number
        IContext basenames = (IContext)assemblies.getContext(BASENAMES);
        if ( basenames != null ) {
            IContext basenameContext = (IContext)basenames.getContext(basename);
            if ( basenameContext != null ) {
                result = (Assembly)basenameContext.getAttribute(interfaceNumber);
                if ( result != null ) {
                    if (LOG_CACHING) Log.debug(Assembly.class, "basenameContext.getContext('"+basename+"') FOUND in "+assembliesFullpath);
                    return result;
                } else {
                    if (LOG_CACHING) Log.debug(Assembly.class, "basenameContext.getContext('"+basename+"') found null "+assembliesFullpath);
                }
            } else {
                if (LOG_CACHING) Log.debug(Assembly.class, "basenameContext '"+basename+"' not found in "+assembliesFullpath);
            }
        } else {
            if (LOG_CACHING) Log.debug(Assembly.class, "basenames not found in "+assembliesFullpath);
        }
        if (LOG_CACHING) Log.debug(Assembly.class, "basename: '"+basename+"' interfaceNumber: '"+interfaceNumber+"' build: '"+build+"' not found in "+assembliesFullpath);
        return null;
    }

    /** This override operates on an IContext node that represents a resource in the Assembly cache.
     *  The resource is binary if its BINARY attribute is TRUE, which is determined by using
     *  an internal table at resource read time that maps resource extensions to binary or text resources.
     * This table is accessed by calling isBinaryResource(String).
     */
    public static boolean isBinaryResource(IContext node){
        if ( node == null ) {
            Log.warn(Assembly.class, "isBinaryResource was passed a null node");
            return false;
        }
        if ( Tools.isTrue((String)node.getAttribute(BINARY)) ) {
            return true;
        }
        if ( node.getAttribute(CONTENT) == null ) {
            return isBinaryResource(node.getKey());
        }
        return false;
    }

    /** This override operates on a String resource name, and inspects the "extension" (the characters after the last dot in the name)
     * to determine if the resourcetype is binary.  The current implementation returns true for jpeg's, gif's, etc.
     */
    public static boolean isBinaryResource(String key){
        //make this bindable to some system list, and/or allow people to specifiy that some resource is binary.
        key = key.toUpperCase();
        if (
              key.endsWith(".JPEG")
           || key.endsWith(".JPG")
           || key.endsWith(".GIF")
           || key.endsWith(".BMP")
           || key.endsWith(".PNG")
           || key.endsWith(".DOC")
           || key.endsWith(".XLS")
           || key.endsWith(".PDF")
           )
        {
            return true;
        }
        return false;
    }

        static Map<String, String> mimeMap = new HashMap<String, String>();
        static {
            mimeMap.put(".JPG", "image/jpeg");
            mimeMap.put(".JPEG", "image/jpeg");
            mimeMap.put(".BMP", "image/bmp");
            mimeMap.put(".GIF", "image/gif");
            mimeMap.put(".CSS", "text/css");
            mimeMap.put(".HTML", "text/html");
            mimeMap.put(".ICO", "image/x-icon");
            mimeMap.put(".JS", "application/javascript");
            mimeMap.put(".MP3", "audio/mpeg");
            mimeMap.put(".TXT", "text/plain");
            mimeMap.put(".PNG", "image/png");
            mimeMap.put(".PDF", "application/pdf");
        }

        //TODO: replace this with Tomcat default handling.
        public static String getMimeType(String key) {
            //System.out.println("getMimeType====================================[[[[[[[ "+key);
            String orig = key;
            int dot = key.lastIndexOf(".");
            key = key.substring(dot);
            key = key.toUpperCase();
            String mimeType = mimeMap.get(key);
            if (mimeType == null) {
                System.out.println("WARN: ========================= mime type not found, using text/html for key: "+key);
                //System.out.println("WARN: ========================= mime map: "+mimeMap.toString());
                mimeType = "text/html";
            }
            //System.out.println("====> name: "+orig+" key: "+key+" mime: "+mimeType);
            return mimeType;
        }

    public static String listResource(IContext context){
        //Log.debug(Assembly.class, "listResource: "+context);
        String result = "name: "+context.getKey();
        Object o = context.getAttribute(CONTENT);
        if ( o instanceof String ) {
            result += "\r\ncontent-length: "+((String)o).length();
        } else if (o instanceof byte[]) {
            result += "\r\ncontent-length: "+((byte[])o).length;
        } else {
            result += "\r\ncontent is of unknown type, can't calculate length";
        }
        result += "\r\n"+BINARY+": "+context.getAttribute(BINARY);
        result += "\r\n"+CACHED+": "+Tools.dateLongToLocale((Long)context.getAttribute(CACHED));
        result += "\r\n"+ACCESSED+": "+Tools.dateLongToLocale((Long)context.getAttribute(ACCESSED));
        result += "\r\n"+HITS+": "+(Long)context.getAttribute(HITS);
        result += "\r\n"+ASSEMBLYDIR+": "+context.getAttribute(ASSEMBLYDIR);
        result += "\r\n"+FULLPATH+": "+context.getAttribute(FULLPATH);
        //result += "\r\nSTACK_TRACE:" +context.getAttribute("STACK_TRACE");
        return result;
    }

    public static IContext readResource(IContext context){
        synchronized (context) {
            if (context==null){
                Log.warn(Assembly.class, "ERROR: [72] getCachedResource(IContext) was called with a null context");
                return null;
            }
            String key = (String)context.getKey();
            String fullpath = (String)context.getAttribute(FULLPATH);
            IContext res = readResource(context, key, fullpath);
            if ( res == null ) {
                Log.warn(Assembly.class, "readResource didn't find file: "+fullpath);
            }
            return res;
        }
    }

    /** Important: doesn't rebind the ASSEMBLYDIR attribute, since this method is static.*/
    protected static IContext readResource(IContext context, String key, String fullpath){
        synchronized (context){
            // ======  IMPORTANT: keep javadoc up-to-date when you add magic attributes.
            if (LOG_CACHING) Log.debug(CAT_CACHE, "readResource: "+key+" fullpath: "+fullpath);
            if (Tools.isTrue(context.getAttribute(IN_MEMORY_RESOURCE))){
                if (LOG_CACHING) Log.debug(CAT_CACHE, "readResource skipping IN_MEMORY_RESOURCE");
                return context; //in-memory, don't try to read from disk.
            }
            Object lv = (Long)context.getAttribute(Assembly.MODIFIED);
            long wasLastModified = lv != null ? ((Long)lv).longValue() : 0;
            FileTools.FileInfo info;
            if ( isBinaryResource(key) ) {
                info = FileTools.readBinaryFileInfo("", fullpath, wasLastModified);
                if ( !info.getFileNotFound() && ! info.getNotModified() ) {
                    byte[] buff = info.getBytes();
                    Long lastModified = new Long(info.getFile().lastModified());
                    if (LOG_CACHING) Log.debug(Assembly.class, "readResource rebinding  FULLPATH: "+fullpath+ " for "+context);
                    setBinaryContent(context, buff, fullpath, lastModified, false);
                }
            } else {
                info = FileTools.readFileInfo("", fullpath, wasLastModified);
                if ( !info.getFileNotFound() && ! info.getNotModified() ) {
                    String content = info.getAsString();
                    File theFile = info.getFile();
                    Long lastModified = new Long(theFile.lastModified());
                    if (LOG_CACHING) Log.debug(Assembly.class, "readResource rebinding  FULLPATH: "+fullpath+ " for "+context+" with timestamp: "+theFile.lastModified());
                    setContent(context, content, fullpath, lastModified, false);
                } else {
                    if (LOG_CACHING) Log.debug(CAT_CACHE, "readResource not setting timestamp for '"+key+"' lastModified: "+wasLastModified+" info: "+info);
                }
            }
            if ( info.getFileNotFound() ) {
                Log.debug(Assembly.class, "readResource can't find '"+key+"' in '"+fullpath+"'"+info);
                return null;
            }
            return context;
        }
    }

    /** @param inMemory Indicates that the resource is not to be found on disk,
     * so subsequent calls to getResource will NOT attempt to look for the latest resource from disk,
     * which is useful if you construct a resource in memory or pull if off  a socket, etc.
     */
    public static IContext setBinaryContent(IContext context, byte[] buff, String fullpath, Long lastModified, boolean inMemory){
        synchronized (context) {
            context.rebindAttribute(BINARY, TRUE);
            context.rebindAttribute(CONTENT, buff);
            context.rebindAttribute(MODIFIED, lastModified);
            context.rebindAttribute(CACHED, Tools.now());
            context.rebindAttribute(FULLPATH, fullpath);
            return context;
        }
    }

    /** @param inMemory Indicates that the resource is not to be found on disk,
     * so subsequent calls to getResource will NOT attempt to look for the latest resource from disk,
     * which is useful if you construct a resource in memory or pull if off  a socket, etc.
     */
    public static IContext setContent(IContext context, String content, String fullpath, Long lastModified, boolean inMemory){
        synchronized (context) {
            context.rebindAttribute(BINARY, FALSE);
            context.rebindAttribute(CONTENT, content);
            context.rebindAttribute(MODIFIED, lastModified);
            context.rebindAttribute(CACHED, Tools.now());
            context.rebindAttribute(FULLPATH, fullpath);
            return context;
        }
    }

    private static void updateBasenamesInterfacesMap(IContext assemblies,
                                                     IContext assembly,
                                                     String assemblyDirPath,
                                                     String assemblyBasename,
                                                     String assemblyInterfaceNumber)
    throws Exception {
        IContext basenames = (IContext)assemblies.getContext(BASENAMES);
        if ( basenames == null ) {
            basenames = assemblies.bind(new ContextNode(BASENAMES));
        }
        IContext basenameContext = basenames.getContext(assemblyBasename);
        if ( basenameContext == null ) {
            basenameContext = basenames.bind(new ContextNode(assemblyBasename));
        }
        synchronized (basenameContext) {
            Assembly prevHigh = (Assembly)basenameContext.getAttribute(assemblyInterfaceNumber);
            if ( prevHigh == null ) {
                basenameContext.bindAttribute(assemblyInterfaceNumber, assembly);  //first one, so it wins.
                //Log.info(Assembly.class, "Successfully bound "+assemblyInterfaceNumber+" to assembly: "+assembly);
            } else {
                String prevBuildNumber = (String)prevHigh.getAttribute(Assembly.BUILD);
                if ( prevBuildNumber == null ) {
                    throw new DynamideException("[114] No prevBuildNumber found in "+BASENAMES+'/'+assemblyBasename+'/'+assemblyInterfaceNumber+'/'+prevHigh);
                } else {
                    int prevBN = Tools.stringToIntSafe(prevBuildNumber, -1);
                    int currBN = Tools.stringToIntSafe((String)assembly.getAttribute(BUILD), -1);
                    if ( prevBN == -1 ) {
                        Log.warn(Assembly.class, "[115] prevBN was -1 in "+assemblyDirPath);
                    }
                    if ( currBN == -1 ) {
                        Log.warn(Assembly.class, "[116] currBN was -1 in "+assemblyDirPath);
                    }
                    if ( prevBN == currBN ) {
                        Log.warn(Assembly.class, "[117] previous BuildNumber was the same as current BuildNumber ("+currBN+") in "+assemblyDirPath);
                    }
                    if ( currBN > prevBN ) {
                        basenameContext.rebindAttribute(""+currBN, assembly);  //higher number, so the new one wins.
                        //Log.info(Assembly.class, "Successfully bound "+assemblyInterfaceNumber+" to assembly: "+assembly+" which has build "+currBN);
                    } else {
                        //Log.info(Assembly.class, "NOT binding "+assemblyInterfaceNumber+" to "+currBN+" since previous was higher: "+prevBN);
                    }
                }
            }
        }
    }

    private String  m_account = "";

    private String  m_assemblyDirectory = "";

    private StringList m_imports = new StringList();

    private void checkCallStack() throws SecurityException {
        //ensure that we have been called directly by the ResoureManager:
        DynamideSecurityManager.checkCallStack("com.dynamide.resource.ResourceManager", "createAssembly", 2, false);
    }

    public static void accessed(IContext context){
        if (context!=null) {
            synchronized (context) {
                context.rebindAttribute(ACCESSED, Tools.now());
                Long hits = (Long)context.getAttribute(HITS);
                long lhits = 1;
                if ( hits!=null ) {
                    lhits = hits.longValue()+1;
                }
                context.rebindAttribute(HITS, new Long(lhits));
            }
        }
    }

    private IContext checkForUpdates(String key) {
        if (LOG_CACHING) logCaching("checkForUpdates: "+key +" in "+toString());
        IContext context = getContext(key);
        if (isCacheOn()){
            accessed(context);
            return context;
        } else {
            String filename = Tools.fixFilename(key);
            String assemblyDirectory = getAssemblyDirectory();
            String fullpath = assemblyDirectory+File.separator+filename;
            boolean contextWasInTree = true;
            if ( context == null ) {
                contextWasInTree = false;
                context = new ContextNode(null, key);
            }
            synchronized (context) {
                //NOT caching, read it every time:
                if ( key == null || key.trim().length() ==0 ) {
                    logInfo("Context key was blank. fullpath: "+fullpath+" stack-trace: "+Tools.getStackTrace());
                    return null;
                }
                IContext test = readResource(context, key, fullpath);
                if (test == null){
                    if (contextWasInTree){
                        remove(key);
                        logInfo("Context was in tree, but was not found on disk. key: '"+key+"' fullpath: "+fullpath+" File was probably removed.");
                    }
                    if (LOG_CACHING) logCaching("checkForUpdates 2 didn't find key: "+key);
                    return null;
                } else {
                    context.rebindAttribute(ASSEMBLYDIR, m_assemblyDirectory);
                    context.rebindAttribute(ASSEMBLY, this);
                    rebind(key, context);
                    accessed(context);
                    if (LOG_CACHING) logCaching("############################## checkForUpdates REBINDING fullpath("+fullpath+") to "+context);
                    return context;
                }
            }
        }
    }

    /** Get the content of the resource as a String, by resourceID.
     *
     * @return A String, unless the resource was binary, in which case
     *  an exception is thrown.
     *
     * @throws ResourceException
     */
    public String extractResourceContent(String resourceID)
    throws ResourceException {
        IContext node = getResource(resourceID);
        return extractResourceContent(node);
    }

    public Assembly findImport(String assemblyName, String assemblyInterfaceNumber, String assemblyBuild){
        return ResourceManager.getRootResourceManager().findAssembly(this, assemblyName, assemblyInterfaceNumber, assemblyBuild);
    }
    public String getAccount(){
        return m_account;
    }

    /**  Each assembly has two places to look for resources:
     *  <ul>
     *   <li>in the resources folder of the assembly</li>
     *   <li>in the apps/${appname}/resources folder of the assembly</li>
     *  </ul>
     *  So resources that are not things like page1.xml or application.xml should go in one of those
     *  folders and thus begin with "resources/".
     */
    public IContext getApplicationResource(String applicationName, String resourceID){
        //   resourceID will be like "resources/css/dynamide.css"
        //   or "page1.xml"
        String appRelName = "apps/"+FileTools.joinURI(applicationName, resourceID);  //   {assembly}/apps/demo/resources/css/dynamide.css
        IContext result = null;
        if (LOG_CACHING) logCaching("getApplicationResource 1: "+appRelName);
        result = getResource(appRelName, false); //it is prepended with apps/yadayada/, so it CANNOT be in imports.
        if ( result != null )  {
            if (LOG_CACHING) logCaching("getApplicationResource 2: "+result);
            return result;
        }
        //  now look for:  {assembly}/resources/css/dynamide.css AND check imports.
        result = this.getResource(resourceID, true);
        if (LOG_CACHING) logCaching("getApplicationResource 3: "+result);
        return result;
    }


    /** The name of the directory that the Assembly expanded to.*/
    public String  getAssemblyDirectory(){return m_assemblyDirectory;}

    public String getBuildName(){
        return ""+getAttribute(BASENAME)+'/'+getAttribute(INTERFACE)+'/'+getAttribute(BUILD) ;
    }

    public List getImports(){
        synchronized (m_imports) {
            Vector v = new Vector();
            Assembly current;
            int imports_size = m_imports.size();
            for (int i=0; i < imports_size; i++) {
                AssemblyImport ai = (AssemblyImport)m_imports.getObjectAt(i);
                current = ai.assembly;
                if ( current == null ) {
                    int interfaces_size = ai.interfaceNumbers.size();
                    for (int n=0; n < interfaces_size; n++) {
                        current = findImport(ai.basename, ai.interfaceNumbers.getString(n), ai.build);
                        if ( current == null ) {
                            continue;
                        }
                        break;
                    }
                    ai.assembly = current;
                }
                if ( current != null ) {
                    v.addElement(current);
                }
            }
            return v;
        }
    }

    public String getImportsFullPath(){
        return getImportsFullPath(";");
    }

    public String getImportsFullPath(String separator){
        StringBuffer result = new StringBuffer();
        IContext res = getImportsFullPathAsContext();
        if ( res == null ) {
            logError("[119] getImportsFullPathAsContext returned null");
            return "";
        }
        int res_size = res.getAttributeCount();
        for (int i=0; i < res_size; i++) {
            result.append((String)res.getAttribute(""+i));
            if (i<(res_size-1)){
                result.append(separator);
            }
        }
        return result.toString();
    }

    /** @return An IContext that contains the paths, listed as
    *  attributes in order of search: the first search fullpath is stored in the attribute "0",
    *  the next in attribute 1, and so on.
    */
    public IContext getImportsFullPathAsContext(){
        try {
            return getResourceImports("phony.resource.name");
        }catch (Exception e){
            logError("[118] getImportsFullPathAsContext caught an unexpected error", e);
            return null;
        }
    }

    public String getImportsPath(){
        synchronized (m_imports) {
            String result = "";
            boolean first = true;
            for (Iterator iter = m_imports.keysList().iterator(); iter.hasNext(); ) {
                if ( first ) {
                     first = false;
                } else {
                    result += ";";
                }
                result += (String)iter.next();
            }
            return result;
        }
    }

    /*
    contexts, in order: thisAssembly, importedAssemblies[]
    hmmmm, aren't these methods of Assembly, then? checkImports(), addImport()
    walk the assemblies in import order, ask each of them for the file.
    to process an addImport statement, you ask the ResourceManager, and it checks permissions for you
    and .iff. allowed, gives you the pointer to the resource
    */

    /* %% remove this
     * public boolean addImport(Element importEl){
        String assemblyName = importEl.getChildText("assembly");
        List interfaces = select(
        importEl.getText();
        boolean result = false;
        m_imports.addObject(assemblyName, null); //This is called from init, and so the other assemblies might not
                                                 // be available.  Delay adding the object ref until findImport is called.
        return result;
    }
     */

    /** Turn a relative resource name into an include statement for HTML.
     *  examples:
     *  <pre>
     *  js/page.js
     *  images/map.jpg
     *  </pre>
     */
    public String getInclude(String applicationPath, String applicationName, String includeName){
        return getInclude(applicationPath, applicationName, includeName, "");
    }

    public String getInclude(String applicationPath, String applicationName, String includeName, String cachePrefix){
        try {
            if (LOG_CACHING) logCaching("getInclude cachePrefix: "+cachePrefix+" includeName: "+includeName);

            ResourceManager root = ResourceManager.getRootResourceManager();
            synchronized (root){
                //String cacheRelName = applicationName+'/'+includeName;
                String cacheRelName = applicationPath+'/'+includeName;
                String cachePrefixURI;
                if ( cachePrefix.length() == 0 ) {
                    cachePrefix = ResourceManager.CACHE_ICONTEXT_NAME;
                    cachePrefixURI = ResourceManager.CACHE_URI;
                } else {
                    cachePrefixURI = "/"+cachePrefix;
                }
                //logDebug("getInclude 2 cachePrefix: "+cachePrefix+" cachePrefixURI: "+cachePrefixURI);

                cachePrefixURI = root.getCachePrefixURI(cachePrefix);
                String uri = cachePrefixURI+cacheRelName;
                IContext resource = root.getCachedResource(cachePrefix, cacheRelName);
                if ( resource == null) {
                    resource = getApplicationResource(applicationName, includeName);
                    if ( resource == null ) {
                        logWarn("Assembly.getInclude: FileNotFound: "+includeName);
                        return includeName; //111209, was just: ""; Now we return the requested name, since if it is a URL, then it won't be found on disk.
                    }
                    root.putCachedResource(cachePrefix, cacheRelName, resource);
                    //logDebug("added cached resource: "+cacheRelName+" ==> "+includeName);
                } else {
                    //logDebug(" RESOURCE FOUND FOR INCLUDE: "+cacheRelName+" ==> "+includeName);//ResourceManager.dumpContext(resource, "  ", false));
                }
                String includeNameUpper = includeName.toUpperCase();
                if (includeNameUpper.endsWith(".JS")){
                    return "<SCRIPT language=\"javascript\" src=\""+uri+"\"></SCRIPT>";
                } else if (includeNameUpper.endsWith(".CSS")){
                    return "<LINK href=\""+uri+"\" src=\""+uri+"\" type=\"text/css\" rel=\"STYLESHEET\" />";
                } else {
                    return uri;
                }
            }
        } catch (Exception e){
            logError("Couldn't locate resource: '"+includeName+"' in application '"+applicationName+"'", e);
            return "\r\nERROR: [73] Couldn't locate resource: '"+includeName+"' in application '"+applicationName+"'";
        }
    }

    public String getResourceFilename(String resourceID){
        IContext context = getResource(resourceID, true);
        if ( context != null ) {
            return (String)context.getAttribute(Assembly.FULLPATH);
        }
        return "";
    }

    /** <p>
     * Get the IContext node that contains the references to the resource named by key, where key is
     *  relative to the root of this assembly. Use the functions in Assembly, namely isBinaryResource(IContext),
     *  extractBinaryResourceContent, and extractResourceContent to get to the actual resource content.
     *  <br/><br/>
     *  For example:</p>
     *
     *  <pre>
     *  IContext context = assembly.getResource("css/dynamide.css");
     *  String mycss = Assembly.extractResourceContent(context);
     *  </pre>
     *
     *  <p>Also, this would be legal if you didn't know if the resource were binary:</p>
     *
     *  <pre>
     *  IContext context = assembly.getResource("css/dynamide.css");
     *  if (Assembly.isBinaryResource(context)){
     *       String mycontents = Assembly.extractResourceContent(context);
     *       ...
     *  } else {
     *       byte [] mybytes = Assembly.extractBinaryResourceContent(context);
     *       ...
     *  }
     *  </pre>
     *
     */
    public IContext getResource(String key){
        //7/1/2003 11:44AM return checkForUpdates(key);
        return getResource(key, true);
    }

    /** Get a resource, optionally looking in all imported assemblies.
     */
    public IContext getResource(String resourceID, boolean checkImports){
        if (LOG_CACHING) Log.debug(CAT_CACHE, "getResource: "+resourceID+" checkImports: "+checkImports);
        String where = "";
        IContext result = checkForUpdates(resourceID);
        if ( result != null){
            return result;
        }
        if ( checkImports ) {
            Assembly current;
            synchronized (m_imports){
                int imports_size = m_imports.size();
                for (int i=0; i < imports_size; i++) {
                    AssemblyImport ai = (AssemblyImport)m_imports.getObjectAt(i);
                    current = ai.assembly;
                    if ( current == null ) {
                        int interfaces_size = ai.interfaceNumbers.size();
                        for (int n=0; n < interfaces_size; n++) {
                            current = findImport(ai.basename, ai.interfaceNumbers.getString(n), ai.build);
                            if ( current == null ) {
                                Log.error(Assembly.class, "******** IMPORT NOT FOUND ******** but was listed: "+ai+" #"+n+" of "+interfaces_size+"\r\n");
                                continue;
                            } else {
                                break;
                            }
                        }
                        ai.assembly = current;
                    }
                    if ( current != null ) {
                        where += current.getAttribute(Assembly.FULLPATH)+"\r\n";
                        //Note current cannot be not "this" (recursion: hasResource can call getResource) but it will check cache.
                        if ( current != this) {
                            result = current.getResource(resourceID, false);
                            if (result != null){
                                if (LOG_CACHING) Log.debug(Assembly.class, "******** ******* getResource result: "+result+" resourceID"+resourceID+" imports: "+ai+" in \r\n"+where);
                                return result;
                            }
                        }
                    }
                }
            }
        }
        if (LOG_CACHING) Log.debug(CAT_CACHE, "getResource could not find '"+resourceID+"' in Assembly '"+getBuildName()+"'  or imported assemblies: "+where);
        return result;
    }

    /**This method provides the search path for the resource --
     *  an IContext is returned that contains the paths, listed as
     *  attributes in order of search: the first search fullpath is stored in the attribute "0" ,
     *  the next in attribute 1, and so on.
     */
    private IContext getResourceImports(String resourceID){
        int iwhich = 0;
        IContext result = new ContextNode(null, "ResourceSearchPath");
        result.rebindAttribute(""+(iwhich++), (String)getAttribute(Assembly.FULLPATH));
        String where = "";
        Assembly current;
        synchronized (m_imports) {
            int imports_size = m_imports.size();
            for (int i=0; i < imports_size; i++) {
                AssemblyImport ai = (AssemblyImport)m_imports.getObjectAt(i);
                current = ai.assembly;
                if ( current == null ) {
                    int interfaces_size = ai.interfaceNumbers.size();
                    for (int n=0; n < interfaces_size; n++) {
                        current = findImport(ai.basename, ai.interfaceNumbers.getString(n), ai.build);
                        if ( current == null ) {
                            continue;
                        } else {
                            break;
                        }
                    }
                    ai.assembly = current;
                }
                if ( current != null ) {
                    result.rebindAttribute(""+(iwhich++), (String)current.getAttribute(Assembly.FULLPATH));
                }
            }
        }
        return result;
    }

    /** Resources are listed in the manifest of this Assembly at run time only, and that entry indicates
     * that the resource can be found in this Assembly context.
     */
    public boolean hasManifestEntry(String key){
        return hasContext(key);
    }

    public synchronized boolean hasResource(String key){
        if (!isCacheOn()){
            if (LOG_CACHING) logCaching("Trying to kick cache: "+key+" in "+toString()+ " hasContext: ("+key+") "+hasContext(key));
            IContext ires = checkForUpdates(key); //kicks the cache
            if (LOG_CACHING) if (ires == null) logCaching("FileNotFound while trying to kick cache: "+key+" in "+toString());
        }
        boolean res = hasContext(key);
        if (LOG_CACHING) logCaching("hasContext(\""+key+"\")  in "+toString());
        return res;
    }

    /** Removes the resources content from the cache.
     *
     *  @param key If key is currently cached, remove the contents from the cache, but keep the key
     *  to imply that the resource is found in our manifest, but since the value is null, the
     *  Assembly should once again look on disk or other repository for the resource.
     */
    public synchronized void invalidate(String key){
        if (LOG_CACHING) logCaching("invalidate: "+key);
        if (hasContext(key)){
            rebind(key, null);
        }
    }

    public boolean isCacheOn(){
        Object att = getAttribute(ResourceManager.CACHE_ON);
        if ( att == null ) {
            if (LOG_CACHING) logCaching("isCacheOn: att was null, returning false");
            return false;
        }
        boolean res = att.toString().equals("true");
        if (LOG_CACHING) logCaching("isCacheOn: result: "+res+" att: "+att);
        return res;
    }



    /** Produces a listing showing the attributes of this cached resource.
     *
     * @return a String with information about the resource, will be the empty String if resource is absent.
     */
    public String listResource(String key){
        IContext context = getContext(key);
        if ( context == null ) {
            context = getResource(key);  //lazy get.
            if (context == null){
                logWarn("Resource not found: "+key);
                return "";
            }
            if ( context == null ) {
               return "";
            }
        }
        return listResource(context);
    }

    /** Produces a StringList containing a bare listing of relative path resource names that have been read into the memory cache.
     *  You can transform a StringList to a Collection in this instance by using StringList.keysList().
     *
     *  @see #listResourceNames
     */
    public StringList listResourceNames(){
        String key;
        StringList stringlist = new StringList();
        Iterator keys = getContexts().keySet().iterator();
        while ( keys.hasNext() ) {
            key = (String)keys.next();
            if (key != null){
                stringlist.add(key) ;
            }
        }
        stringlist.sort();
        return stringlist;
        //.keysList();
        //return stringlist.dump("\r\n");
    }


    /** Produces a bare listing of relative path resource names that have been read into the memory cache.
     *  @see #listResourceNames
     */
    public String listResourceNames(String lineBreak){
        return listResourceNames().dump(lineBreak);
    }


    /** Produces a bare listing of relative path resource names that have been read into the memory cache.
     *  @see #listResourceNames
     */
    public String listResourceNamesInCache(){
        //return ResourceManager.dumpContext(this, "  ");
        String result = "";
        Iterator contexts = getContexts().entrySet().iterator();
        boolean firstdone = false;
        while ( contexts.hasNext() ) {
            Map.Entry entry = (Map.Entry)contexts.next();
            IContext child = (IContext)entry.getValue();
            if (child != null && child.hasAttribute(CONTENT)){
                if (firstdone){
                    result += "\r\n";
                } else {
                    firstdone = true;
                }
                result += entry.getKey();
            }
        }
        return result;
    }


    public Map listTemplates(String relDirectory)
    throws Exception {
        String filename;
        String dirname;
        Map files;
        Map templates = Tools.createSortedCaseInsensitiveMap();

        dirname = Tools.fixFilename(m_assemblyDirectory+'/'+relDirectory);
        files = FileTools.list(dirname, FileTools.LT_FILES, FileTools.LN_FILENAME_NO_EXT);
        templates.putAll(files);
        return templates;
    }

    private synchronized void loadAssemblyXML()
    throws Exception {
        String assemblyContents = extractResourceContent("assembly.xml");
        JDOMFile dom = new JDOMFile(null);
        dom.readFromString(assemblyContents);
        Element assembly = dom.getRootElement();
        String thisInterfaceBasename = assembly.getChildText(BASENAME);
        rebindAttribute(BASENAME, thisInterfaceBasename);
        String thisInterfaceNumber = assembly.getChildText(INTERFACE);
        rebindAttribute(INTERFACE, thisInterfaceNumber);
        String thisInterfaceBuild = assembly.getChildText(BUILD);
        rebindAttribute(BUILD, thisInterfaceBuild);
        //System.out.println("### loadAssemblyXML: "+thisInterfaceBasename+" file: "+dumpAttributes(false));                                         //

        List list = dom.select("//imports/import");
        if ( list != null ) {
            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                Element importEl = (Element)iter.next();
                String impBasename = importEl.getChildText(BASENAME);
                String sbuild = importEl.getChildText(BUILD);
                StringList slNumbers = new StringList();
                List interfaces = dom.select(importEl, "interfaces/interface");
                if ( interfaces != null ) {
                    for (Iterator iterInterfaces = interfaces.iterator(); iterInterfaces.hasNext(); ) {
                        Element interfaceEl = (Element)iterInterfaces.next();
                        String interfaceNumber = interfaceEl.getText();
                        slNumbers.add(interfaceNumber);
                    }
                }
                AssemblyImport imp = new AssemblyImport(impBasename, sbuild, slNumbers);
                m_imports.addObject(impBasename, imp); //This is called from init, and so the other assemblies might not
                                                         // be available.  Delay adding the object ref until findImport is called.
                //System.out.println("###--- Added import: "+imp + " to basename: "+thisInterfaceBasename);                                         //
            }
        }
        bindAttribute(IMPORTS, m_imports);
    }

    private void loadManifest(){
        FileTools filetools = new FileTools();
        FileTools.directoryDiver("Assembly.loadManifest", m_assemblyDirectory, this, "/");
    }
    public final void logCaching(String msg){logDebug(CAT_CACHE+'.'+toString(), msg);}

    /** From: IFileDiverListener interface, and is for internal use only.
     */
    public boolean onDirectory(String diveID, File directory, String dirname, String relativePath){
        if ( dirname.equals("CVS") ) {
            return false;
        }
        return true;
    }

    /** From: IFileDiverListener interface, and is for internal use only.
     */
    public void onFile(String diveID, File directory, File file, String filename, String relativePath){
       //System.out.println("relativePath:   "+relativePath);
       rebind(relativePath, null);
    }

    /** Enter the resource into the Assembly's tree, but even though the file may not exist on disk yet, though
     *  it is best to save the resource to disk and then call registerApplicationResource.
     *  If isCacheOn() returns true, then any resource read after this call will not find the
     *  resource content.  If isCacheOn() returns false, then subsequent calls to getResource, etc.,
     *  will go look on disk for the content.  If your resource does not exist on disk, then you will
     *  want to set its content via the methods setContent() or setBinaryContent(). Those functions
     * accept the "inMemory" parameter, which indicates that the resource is not to be found on disk,
     * so subsequent calls to getResource will NOT attempt to look for the latest resource from disk.
     * Useful if you construct a resource in memory or pull if off  a socket, etc.
     */
    public synchronized IContext registerApplicationResource(String applicationName, String resourceID){
        if (LOG_CACHING) logCaching("registerApplicationResource: "+applicationName+" resourceID: "+resourceID);
        String appRelName = "apps/"+applicationName+"/"+resourceID;  // e.g.  {assembly}/apps/demo/resources/css/dynamide.css
        String assemblyDirectory = getAssemblyDirectory();
        String fullpath = assemblyDirectory+File.separator+Tools.fixFilename(appRelName);
        IContext context = new ContextNode(null, appRelName);
        synchronized (context){
            IContext test = readResource(context, appRelName, fullpath); //actually, OK
            context.rebindAttribute(ASSEMBLY, this);
            context.rebindAttribute(ASSEMBLYDIR, m_assemblyDirectory);
            rebind(appRelName, context);
            accessed(context);
            return context;
        }
    }

    /** The name of the directory that the Assembly expanded to.
     *  IMPORTANT: has the side effect of loading the manifest for this
     *   Assembly, which in this implementation means reading the directory tree.
     */
    public synchronized void setAssemblyDirectory(String  new_value)
    throws Exception {
        m_assemblyDirectory = Tools.fixFilename(new_value);
        rebindAttribute(Assembly.FULLPATH, m_assemblyDirectory);
        loadManifest();
        loadAssemblyXML();
    }

    
    private String temporaryBuildName = null;
    
    public String toString(){
        if (temporaryBuildName != null){
            return temporaryBuildName;
        }
        return getBuildName();
    }

    /** @todo This class should check disk for updates when this method is called.
     */
    public synchronized void update(){
    }

    public synchronized void updateResource(String relativePath){
       rebind(relativePath, null);
    }

    //=================== Main ========================================================

    private static void usage(String args[]){
        com.dynamide.util.Log.println("Usage: ");
        com.dynamide.util.Log.println("   com.dynamide.resource.Assembly <options>");
        com.dynamide.util.Log.println("   options:");
        com.dynamide.util.Log.println("       -root <resource-root>");
        com.dynamide.util.Log.println("       -dir  <assembly-directory>");
        com.dynamide.util.Log.println("       -account <account>");
        com.dynamide.util.Log.println("       [-list]");
        System.exit(1);
    }

    public static void main(String args[])
    throws Exception {
        //com.dynamide.util.Log.println("args used: "+Tools.arrayToString(args, " "));
        if ( args.length==0 ) {
            usage(args);
        }
        Opts opts = new Opts(args);
        String account = opts.getOption("-account");
        String resroot = opts.getOption("-root");
        String RESOURCE_ROOT =  resroot.length() > 0 ? resroot : "C:/dynamide/build/ROOT";
        String assemblyDirectory = opts.getOption("-dir");
        if ( assemblyDirectory.length() > 0 ) {
            ResourceManager rm = ResourceManager.createStandalone(RESOURCE_ROOT);
            //com.dynamide.util.Log.println("\r\n"+Tools.cleanAndReportMemory(100));
            Assembly assembly = new Assembly(account);//private constructor, doesn't check call stack.
            assembly.setAssemblyDirectory(assemblyDirectory);
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("BEGIN Assembly TESTS");
            com.dynamide.util.Log.println("===== assembly.listResource(\"web-apps.xml\") ======");
            com.dynamide.util.Log.println(assembly.listResource("web-apps.xml"));
            com.dynamide.util.Log.println("===== assembly.listResource(\"resources/css/ide.css\") ======");
            com.dynamide.util.Log.println(assembly.listResource("resources/css/ide.css"));
            com.dynamide.util.Log.println("===== ======");

            //com.dynamide.util.Log.println("Root Context: =============\r\n"+rm.dumpRootContext()+"\r\n=================");
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("TEST: (looking for resources/css/ide.css in manifest) "+assembly.hasManifestEntry("resources/css/ide.css"));
            com.dynamide.util.Log.println("TEST: (looking for resources/css/ide.css) "+assembly.hasResource("resources/css/ide.css"));
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("TEST: look for resources/css/ide.css");
            if (assembly.hasResource("resources/css/ide.css")){
                try {
                    IContext logcon = assembly.getResource("resources/css/ide.css");
                    if (logcon != null) {
                        com.dynamide.util.Log.println("TEST reading resources/css/ide.css succeeded.");
                        com.dynamide.util.Log.println("resource length: "+ extractResourceContent(logcon).length() );
                    }
                    //this throws an error, as it should: com.dynamide.util.Log.println("error: "+ extractBinaryResourceContent(logcon) );

                    com.dynamide.util.Log.println("listing: \r\n"+assembly.listResource("resources/css/ide.css"));
                } catch (Exception e){
                    com.dynamide.util.Log.println("getResource test failed: "+e);
                }
            }
            com.dynamide.util.Log.println("");
            com.dynamide.util.Log.println("TEST: look for assembly.xml");
            if (assembly.hasResource("assembly.xml")){
                try {
                    IContext logcon = assembly.getResource("assembly.xml");
                    if (logcon != null) {
                        com.dynamide.util.Log.println("TEST reading assembly.xml succeeded.");
                        com.dynamide.util.Log.println("assembly.xml length: "+ extractResourceContent(logcon).length() );
                    }
                    //this throws an error, as it should: com.dynamide.util.Log.println("error: "+ extractBinaryResourceContent(logcon) );

                    com.dynamide.util.Log.println("listing: \r\n"+assembly.listResource("assembly.xml"));
                } catch (Exception e){
                    com.dynamide.util.Log.println("getResource test failed: "+e);
                }
            }
            //com.dynamide.util.Log.println("\r\n"+Tools.cleanAndReportMemory(100));
            if (opts.getOptionBool("-list")){
                com.dynamide.util.Log.println("");
                com.dynamide.util.Log.println("TEST: show resources");
                com.dynamide.util.Log.println("Resources in cache:\r\n-----------------\r\n"+assembly.listResourceNamesInCache());
                com.dynamide.util.Log.println("-----------------\r\n");
                com.dynamide.util.Log.println("Resources in assembly:\r\n-----------------\r\n"+assembly.listResourceNames(""));
                com.dynamide.util.Log.println("-----------------\r\n");
                com.dynamide.util.Log.println("Assembly Dump: ---------------\r\n"
                                   +ResourceManager.dumpContext(assembly, "  ", false)
                                   +"\r\n-------------------");
            }
            com.dynamide.util.Log.println("DONE.");
            System.exit(0);  // ResourceManager _singleton keeps us running.
        } else {
            usage(args);
        }
        System.exit(1);

        //todo: make one Assembly per assembly, set its dir, and then walk all pertinent Assemblies, looking
        //fer cached resources.
    }


}
