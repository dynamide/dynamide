package com.dynamide.resource;

import java.util.Iterator;
import java.util.List;

import org.jaxen.XPath;
import org.jdom.Element;

import com.dynamide.DynamideException;
import com.dynamide.DynamideObject;
import com.dynamide.util.Log;
import com.dynamide.util.Tools;
import com.dynamide.util.XDB;

public class WebApps extends DynamideObject {
    
    public static final String INITLOG = "WebAppsInitLog"; 
    public static final String INITLOG_DETAIL = "Detail";
    

    /** Forbidden
     */
    private WebApps (DynamideObject owner)
    throws DynamideException {
        super(owner);
        throw new DynamideException("ERROR: [71] can't directly create an instance of this class.");
    }

    /** Forbidden
     */
    public Object clone(){
        return null;
    }

    /** @param home This is the name of the account -- user "joe" would have home "joe".
     *  @param assembly This is the name of the enclosing Assembly, if there is one, or the empty string ("")
     *  @param interfaceNumber This is the interface number of the assembly
     *  if there isn't:  there won't be an enclosing Assembly if the web-apps file is in the home dir or in the homes dir.
     *  @param node  The sub-context from the root resource manager where the Assembly or Home is stored.
     *  @param logNode The context from the root resource manager where all the web-apps entries are stored, as a global reference.
     */
    public static IContext initWebApps(IContext node, String webappsFullPath, String home, String assembly, String interfaceNumber, IContext logNode)
    throws ObjectAlreadyBoundException, Exception {

        String URI,
               APPNAME,
               HOME,
               ASSEMBLY,
               SECURE,
               build,
               loadOnStartup,
               verboseMode;

        XPath  xpURI,
               xpAPPNAME,
               xpHOME,
               xpASSEMBLYname,
               xpSECURE,
               xpInterfaceNumber,
               xpBuild,
               xpLoadOnStartup,
               xpVerboseMode;

        XDB xdb = new XDB();
        Element root = xdb.openXML(webappsFullPath, webappsFullPath); //use webappsFullPath as the repository key to prevent conflicts.
        try {

            //Log.debug(WebApps.class, "initWebApps starting. root: "+root+" filename: "+webappsFullPath);
            //Log.debug(WebApps.class, "initWebApps starting. root dump: "+JDOMFile.output(root));

            xpURI = xdb.prepare(WebAppEntry.URI);
            xpAPPNAME = xdb.prepare(WebAppEntry.APPNAME);
            xpHOME = xdb.prepare(WebAppEntry.HOME);
            xpSECURE = xdb.prepare(WebAppEntry.SECURE);
            xpLoadOnStartup = xdb.prepare(WebAppEntry.LOADONSTARTUP);
            xpVerboseMode = xdb.prepare(WebAppEntry.VERBOSEMODE);
            xpASSEMBLYname = xdb.prepare("ASSEMBLY/name");
            xpInterfaceNumber = xdb.prepare("ASSEMBLY/interface");
            xpBuild = xdb.prepare("ASSEMBLY/build");

            node.bindAttribute(Assembly.FULLPATH, webappsFullPath);
            logNode.bindAttribute(Assembly.FULLPATH, webappsFullPath);
            //sb.append("<br />binding FULLPATH: "+webappsFullPath);
            //sb.append("<hr />");
            //String log = (String)logNode.getAttribute(WebApps.INITLOG_DETAIL);
            //logNode.bindAttribute(WebApps.INITLOG_DETAIL, log+sb.toString());            

            //Log.debug(WebApps.class, "WebApps: "+webappsFullPath);
            StringBuffer sb = new StringBuffer();
            List applist = xdb.select(root, "//app");
            for (Iterator iter = applist.iterator(); iter.hasNext(); ) {
                //Log.debug(WebApps.class, "initWebApps in iter");
                Element appEl = (Element)iter.next();
                URI = xpURI.valueOf(appEl);
                //Log.debug(WebApps.class, "initWebApps URI: "+URI);
                if ( URI != null && URI.length() > 0 ) {
                    APPNAME = xpAPPNAME.valueOf(appEl);

                    if ( home.length()>0 ) {
                        HOME = home;
                    } else {
                        HOME = xpHOME.valueOf(appEl);
                    }

                    if ( assembly.length()>0 ) {
                        ASSEMBLY = assembly;
                    } else {
                        ASSEMBLY = xpASSEMBLYname.valueOf(appEl);
                    }

                    SECURE = ""+com.dynamide.util.Tools.isTrue(xpSECURE.valueOf(appEl));

                    if ( interfaceNumber.length()==0 ) {
                        interfaceNumber = xpInterfaceNumber.valueOf(appEl);
                    }

                    build = xpBuild.valueOf(appEl);

                    loadOnStartup = xpLoadOnStartup.valueOf(appEl);

                    verboseMode = xpVerboseMode.valueOf(appEl);

                    WebAppEntry entry = new WebAppEntry(URI, APPNAME, ASSEMBLY, HOME,
                                                        interfaceNumber, build, webappsFullPath, SECURE,
                                                        loadOnStartup, verboseMode);
                    //6/23/2003 9:31AM node.bindAttribute(URI, entry);
                    Log.debug(WebApps.class, "binding URI: "+URI+" for "+entry);
                    if (node.hasContext(URI)){
                        Log.warn(WebApps.class, "REBINDING "+URI
                            +" and URI was already bound.  Please check for duplicates."
                            +"The last one won: "+entry);
                    }
                    node.rebind(URI, entry);
                    logNode.rebind(URI, entry);  //duplicate reference in the root context for ease of retrieval.
                }
            }
            
        } finally{
            xdb.closeXML(webappsFullPath);
        }
        return node;
    }


    public static void loadWebApps(){
    }

    public static void addWebApp(String webappsFullPath,
                                 String uri,
                                 String appname,
                                 String home,
                                 String assembly,
                                 String interfaceNumber,
                                 boolean loadOnStartup) 
    throws DynamideException {
        XDB x = new XDB();
        String errmsg;
        String xmlname = "WebApps"+Tools.now().toString();
        Element root = x.openXML(xmlname, webappsFullPath);
        if ( root == null ) {
            errmsg = "web-apps filename could not be found in addWebApp(): "+webappsFullPath;
            Log.error(WebApps.class, errmsg);
            throw new DynamideException(errmsg);
        }
        List l = x.select(xmlname,"/web-apps/app[URI/text()='"+uri+"' and APPNAME/text()='"+appname+"']");
        System.out.println("\r\n\r\n...............found list: "+l.size());
        if (l.size() > 1){
            errmsg = "WebApps.addWebApp() found multiple, similar elements: "+webappsFullPath+" uri: "+uri+" appname: "+appname;
            Log.error(WebApps.class, errmsg);
            throw new DynamideException(errmsg);
        }
        if (l.size() == 1){
            //Modify:
            Element app = (Element)l.get(0);
            Element load = app.getChild("load-on-startup");
            if (load == null){
                app.addContent(new org.jdom.Element("load-on-startup").setText(""+loadOnStartup));
            } else {
                load.setText(""+loadOnStartup);   
            }
            return;
        }
        
        Element el = createAppElement(uri, appname, home, assembly, interfaceNumber, loadOnStartup);
        root.addContent(el);
        x.saveXML(xmlname, webappsFullPath);
        return;
    }

    public static Element createAppElement(
                                 String uri,
                                 String appname,
                                 String home,
                                 String assembly,
                                 String interfaceNumber,
                                 boolean loadOnStartup) {
         Element result = (new Element(WebAppEntry.APP));

         Element appnameEl = new Element(WebAppEntry.APPNAME);
         appnameEl.addContent(appname);
         Element uriEl = new Element(WebAppEntry.URI);
         uriEl.addContent(uri);

         result.addContent(uriEl);
         result.addContent(appnameEl);


         if ( assembly != null ) {
             Element assemblyEl = new Element(WebAppEntry.ASSEMBLY);
             result.addContent(assemblyEl);
                Element nameEl = new Element(Assembly.NAME);
                assemblyEl.addContent(nameEl);
                nameEl.addContent(assembly);

                Element interfaceEl = new Element(WebAppEntry.INTERFACE);
                assemblyEl.addContent(interfaceEl);
                interfaceEl.addContent(interfaceNumber);
         }
         if ( home != null ) {
                Element homeEl = new Element(WebAppEntry.HOME);
                result.addContent(homeEl);
                homeEl.addContent(home);
         }
         if ( loadOnStartup ) {
            Element loadEl = new Element(WebAppEntry.LOADONSTARTUP);
            result.addContent(loadEl);
            loadEl.addContent("true");
         }
         return result;
    }


    public static Element createAppElement(
                                 String uri,
                                 String appname,
                                 String assembly,
                                 String interfaceNumber,
                                 boolean loadOnStartup) {
        return createAppElement(uri, appname, null, assembly, interfaceNumber, loadOnStartup);
    }

    public static Element createAppElement(
                                 String uri,
                                 String appname,
                                 boolean loadOnStartup){
        return createAppElement(uri, appname, null, null, null, loadOnStartup);
    }

}
    //============== The rest of this file is some attempts and security notes =============================

    //The attempts with JNDI are all obviated, some of the security notes are valid.


        /*
        WebApps has the capability to read and write the webapps.xml file
        Users get the capability to access the WebApps object and ask it questions

        class Capability
            Object target
            Object operation

        class WebApps extends Capability
            operation
            operation

        WebApps[root] gets handed only to root
        WebApps[home] gets handed to local-admin
        WebApps[assembly]  gets handed to user

        group          object               capability
        -----          ------               ----------
        root_admin     webapps_root         read
        root_admin     webapps_root         write

        home_admin    webapps_home         read
        home_admin    webapps_home         write
        home_admin    webapps_assembly     read

        developer      webapps_assembly     write
        developer      webapps_assembly     read

        but root_admin is a member of local_admin
        developer is not necessarily related to local_admin
         *
         * root_admin
         *      home_admin inherits="root_admin"
         * maybe allow multiple inherits as elements
         *   role
         *      name developer
         *      inherits home_admin
         *      inherits user
         *   objects
         *      webapps_home
         *      webapps_assembly
         *          capability
         *              write by owner
         *              write by group
         *              write by others
         * then maybe some concept of creating a resource with a unique name and saving it user-only
         * The webapps is id'd with WEBAPPS_ASSEMBLIES, which is unique, but shared.
         *
         *  read-write could be
         *
         *      read-modify
         *
         * WebApps
         *  if capabilities.canModify then
         *      modify
         *  if capabilities.canPersist then
         *      resourceManager.aquire(file)
         *   (you could also do this with exceptions: capability.checkCanModify(); //throws SecurityException
         *
         * The idea is that Capabilities are granted to each object when it is created by the
         * resource manager.  But the Capabilities must be looked up in advance for the user/group.
         * Then the Capabilities ride around with the protected object.   So the factory sets the caps.
         * And it may refuse to create the object at all.
         *     canCreate
         *     canRead
         *     canModify
         *     canPersist
         *     canPool (is this the same as canPersist?)
         *     canExecute  (for sockets, calls, etc.)
         *
         * There is an issue with the pools.  A user may be able to modify an object, but not pool it?  Pooling
         * it means replacing the shared instance, which is dangerous.  So the pools need to be scoped?
         *  a group gets canModifyPool permission, but need to have this per-object.
         *
         *  A user process asks to see the web-apps.  The web-apps is cloned, and given the capabilities of read only.
         *
         *
         *
        developer must be home_admin for his own account, but not for organization account.
        for a pro account, set only home_admin and no developers.  home_admin installs assemblies, sets web-apps file.

        arg: who hands out permissions.
        do I make each Capability class implement a rule so that it requires roles?
        Or do I make a map of capabilities of each object and map them to roles and users?

           Which changes more: groups or objects with capabilities.
         -- ask dan.


        object
            has capabilities
            -- are initialized for a user, based on user's group
         -- can be cloned and delegated
         *
         *
         * A user gets his own Pools?  So a developer gets his own WEBAPPS_ASSEMBLY object, and it is in the developer's pool
         * otherwise other users would see it.
         *
            globalpool
               webapps_root
               accountpool
                    webapps_home
                    userpool
                        webapps_assembly





        WebApps webapps = new WebApps(..., Security.ROOT_ADMIN, Security.WRITE);
        db.removeSession(ROOT_WEBAPPS);
        db.addSession(ROOT_WEBAPPS, webapps);

        All capabilities:
            WRITE_ROOT_WEBAPPS
            READ_ROOT_WEBAPPS
            WRITE_HOME_WEBAPPS
            READ_HOME_WEBAPPS
            WRITE_LOCAL_WEBAPPS
            READ_LOCAL_WEBAPPS
            ADD_TO_SECURE_POOL
            ADD_TO_HOME_POOL
            ADD_TO_USER_POOL

            or, each object decides if it can be used by a group/user (linux gives one group to each user...)

            WebApps[root]
            WebApps[home]
            WebApps[assembly]


        capabilities = WRITE_ROOT_WEBAPPS, READ_ROOT_WEBAPPS;

        securityManager.addToPool(ROOT_WEBAPPS, webapps, capabilities); //this call needs to come from the manager thread.
        db.removeSession(HOME_WEBAPPS);
        db.addSession(HOME_WEBAPPS, webapps);

        db.removeSession(LOCAL_WEBAPPS);
        db.addSession(LOCAL_WEBAPPS, webapps);



    public static WebAppsContext getWebApps(DynamideObject owner, File directory, User user)
    throws Exception {
       if ( userInRole(user, Security.ROOT_ADMIN ) {
           `statement`
        }
        //get from context.
        //if not available in context, create and put in context.
        InitialContext ictx = new InitialContext();

        DynamideContext wac = null, wacAss = null;
        try {
            wac = (DynamideContext)ictx.lookup("dynamide");
        } catch( Exception e ){
            wac = null;
        }

        if (wac == null){
            wac = new DynamideContext(null, "dynamide");
            ictx.rebind("dynamide", wac);
            wacAss = new DynamideContext(wac, "assemblies");
            wac.rebind("assemblies", wacAss);
            //    dynamide/assemblies/shared
            //    dynamide/assemblies/homes  -- how to make it so each user sees only their homes that they have permission to...
            //    dynamide/assemblies/homes/acme
            //    dynamide/assemblies/homes/acme/shared
            //    dynamide/assemblies/homes/acme/{assembly-name}

            // if jndi does not support access control, then DynamideContext should handle security.
        }
        if ( wac == null ) {
            throw new Exception("couldn't bind dynamide");
        }
        return wac;
    }



        public static class DynamideContext extends InitialContext {
            // supply an override of the default constructor
            public DynamideContext()
            throws NamingException {
                super();
            }
            public DynamideContext(Object owner, String filename)
            throws NamingException {
                super();

            }
        }


      */
