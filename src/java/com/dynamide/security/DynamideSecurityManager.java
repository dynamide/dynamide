package com.dynamide.security;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.Permission;
import java.util.List;
import java.util.Vector;

import com.dynamide.util.Log;
import com.dynamide.util.Tools;

/** Code like this will fail if called anywhere in a Dynamide worker thread:
 * <pre>
        //This code raises an exception, as it should:
        System.out.println("Installing new HackerSecurityManager...");
        System.setSecurityManager(new com.dynamide.security.HackerSecurityManager());
 * </pre>
 */
public class DynamideSecurityManager extends SecurityManager {

/** Dynamide specifically disallows anyone setting any SecurityManager other than com.dynamide.security.SecurityManager
 * and disallows other class loaders.
 */
    public DynamideSecurityManager(){
        super();
        if ( Thread.currentThread().getName().startsWith("DynamideWorkerThread") ) {
            deny("constructor");
        }
        //System.out.println("DynamideSecurityManager instantiated.");
    }

    public static void checkSecurityManagerInit()
    throws Exception {
        //Prevent the hack of just instantiating the Servlet and providing your own ServeletConfig
        if ( DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("ERROR: [74] [Dynamide Security] can't re-run security manager check from within worker threads"); //or from within servlets other than the top-level one.
        }
        Object sm = System.getSecurityManager();
        if (sm != null && sm instanceof com.dynamide.security.DynamideSecurityManager){
            //already done.
        } else {
            //Log.info(DynamideSecurityManager.class, "Installing new SecurityManager...", null);
            System.setSecurityManager(new DynamideSecurityManager());
        }
        sm = System.getSecurityManager();
        if ( ! (sm instanceof com.dynamide.security.DynamideSecurityManager)) {
            throw new SecurityException("ERROR: [75] [Dynamide Security] unable to install DynamideSecurityManager");
        }
    }

    //experimental:
    public boolean isThreadEnabledFor(String action){
        Thread cur = Thread.currentThread();
        /*look up restrictions on this thread.
          Code may call restrictThread() and allowThread() or releaseRestrictions() in try...finally
          to call client code that may be unsafe.  They should pass in a group of actions to allow or
          deny.
         *
         * if m_restrictedThreads.indexOf(cur)... find which restrictions...
         */
        return false;
    }

    public static boolean isCurrentThreadWorker(){
        return isThreadWorker(Thread.currentThread());
    }

    public static boolean isThreadWorker(Thread g){
        //String stack = "";
        String parentname = "";
        boolean isWorker = false;
        ThreadGroup parent = g.getThreadGroup();
        while ( parent != null ) {
            parentname = parent.getName();
            //stack += " :: "+parentname;
            if ( parentname.startsWith("DynamideWorkerThreadGroup" ) ) {
                isWorker = true;
                return true; // early return here.
            }
            parent = parent.getParent();
        }
        return isWorker;
        /*for debugging:
        if ( isWorker ) {
            System.out.println("Thread is worker: "+g.getName()+stack);
            return true;
        } else {
            System.out.println("Thread is NOT worker: "+g.getName()+stack);
            return false;
        }
        */
    }

    private void deny(String reason){
        Log.error(DynamideSecurityManager.class, "requested permission denied: "+reason);
        com.dynamide.util.Tools.printStackTrace();
        throw new SecurityException();
    }

    public static class StackTraceElementDM {
        public StackTraceElementDM(String classname, String methodname){
            super();
            this.classname = classname;
            this.methodname = methodname;
        }
        public String toString(){
            return classname+"::"+methodname;
        }
        public String classname;
        public String methodname;
    }

    public static List java14_StackTraceElementDM(Throwable t){
        StackTraceElement[] elements = t.getStackTrace();  // %% Java 1.4 only %%
        int elements_size = elements.length;
        StackTraceElement element;
        Vector v = new Vector();
        for (int i=0; i< elements_size; i++){
             element = elements[i];
             String classname  = element.getClassName();
             String methodname = element.getMethodName();
             v.add(new StackTraceElementDM(classname, methodname));
        }
        return v;
    }

    public static List java13_StackTraceElementDM(Throwable t)
    throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String trace = sw.toString();
        //System.out.println("+++++++++++++++ trace: "+trace);
        int p = 0;
        int pLastDot = 0;
        int pParen = 0;
        int len = trace.length();
        String clm, classname, methodname;
        java.util.Vector v = new java.util.Vector();
        //System.out.println("AAAAAAAAAAAAAAAA "+p+" len "+len+ " trace: "+trace);
        while (p<len){
            //System.out.println("+++++ p "+p);
            p = trace.indexOf("at ", p);
            //  System.out.println("::::: p "+p+" j: "+j+" trace: "+trace);
            //  int k = "mojo foo.bar\r\n      at \r\n mojo at \r\n foo".indexOf(" at ", 0);
            //  System.out.println("k: "+k);
            if ( p > -1 ) {
                p = p + 3;
                pParen = trace.indexOf("(", p);
                //System.out.println("bbbbbbbbbbbbbbbAAAAAAAAAAAAAAAA");

                if ( pParen > -1 ) {
                    clm = trace.substring(p,pParen);
                    pLastDot = clm.lastIndexOf(".");
                    if ( pLastDot > -1 ) {
                        classname = clm.substring(0, pLastDot);
                        methodname = clm.substring(pLastDot+1);
                        //System.out.println("classname: "+classname+" methodname: "+methodname);
                        v.add(new  com.dynamide.security.DynamideSecurityManager.StackTraceElementDM(classname, methodname));
                    } else {
                        throw new Exception("stack trace element syntax error [expected ' at package....class.method  Element: "+trace.substring(p));
                    }
                    p = pParen+1;
                } else {
                    throw new Exception("stack trace element syntax error [expected ' at package....class.method(...]  Element: "+trace.substring(p));
                }
            } else {
                break;
            }
        }
        //System.out.println("java13_StackTraceElementDM: "+v);
        return v;
    }

    /** Inspect the current call stack, to see if a named class and method are down the call stack
     *  by the distance specified from the point of the call.  You do not need to include the
     *  call to checkCallStack in the distance.  Also, constructors consume one slot on the call stack,
     *  with a method name of "&lt;init&gt;".
     *
     *  <p>For example, suppose you wish to check that Bar.baz() was called by MyClass.foo().
     *  Given the following definitions, the call to checkCallStack succeeds:
     * <pre>
     *   package com.acme;
     *   public class MyClass{
     *      public void foo(){
     *         Bar.baz();
     *      }
     *   }
     *
     *   public class Bar {
     *         public static void baz(){
     *             checkCallStack("com.acme.MyClass", "foo", 1);
     *         }
     *   }
     *  </pre>
     * </p>
     * <p>To check a constructor, remember to index accordingly, since the constructor takes one slot.
     *
     *  Given the following definitions, the call to checkCallStack succeeds:
     * <pre>
     *   package com.acme;
     *   public class MyClass{
     *      public void foo(){
     *         new Bar();
     *      }
     *   }
     *
     *   public class Bar {
     *         public Bar(){
     *             checkCallStack("com.acme.MyClass", "foo", 2);
     *         }
     *   }
     *  </pre>
     * </p>
     * <p>
     * On a 850 MHz intel box, this method costs 0.1 milliseconds.  So it is safe to call routinely.
     * </p>
     *
     * @param verbose If this is on, then debug messages will go to log4j with category com.dynamide.security.DynamideSecurityManager
     *  and level INFO.
     *
     * @throws SecurityException if the className and methodName are not the specified distance down the call stack.
     */
    public static void checkCallStack(String className, String methodName, int distance, boolean verbose)
    throws SecurityException {
        List elements;
        if (Tools.isJVM13()){
               if ( ! java_version_warned ) {
                    Log.warn(DynamideSecurityManager.class,
                       "JVM version 1.3 does not support Throwable.getStackTrace.  Using StackTraceElementDM.");
                    java_version_warned = true;
               }
               try {
                   elements = java13_StackTraceElementDM(new Throwable());
               } catch (Exception e)  {
                   throw new SecurityException(e.getMessage());
               }
        } else {
            //Log.info(DynamideSecurityManager.class,
            //   "Using JVM version 1.4 for Throwable.getStackTrace.");
            elements = java14_StackTraceElementDM(new Throwable());
        }
        boolean debug = false;
        int callersDistance = distance;
        distance = distance+1; //add one for this method, so the distance is from the caller down to the target.
        int elements_size = elements.size();
        StackTraceElementDM element;
        boolean bad = true;
        if ( elements_size >= distance) {
             element = (StackTraceElementDM)elements.get(distance);
             if ( element.classname.equals(className) && element.methodname.equals(methodName) ) {
                if (debug) Log.info(DynamideSecurityManager.class, "OK: "+className+", "+methodName);
                bad = false;
             } else {
                if (debug || verbose)
                    Log.warn(DynamideSecurityManager.class, "class or method not matched: "
                             +element.classname+'.'+element.methodname+" != "+className+", "+methodName+", distance = "+callersDistance+"");
             }
        } else {
            if (debug || verbose)
              Log.warn(DynamideSecurityManager.class,
                      "distance ["+(callersDistance)+"] out of range: "+elements_size);
        }
        if ( bad ) {
            if (debug) {
                String res = "Stack Elements: ["+elements_size+"]";
                for (int i=1; i < elements_size; i++) {
                    //Skip this method, by indexing from 1
                    element = (StackTraceElementDM)elements.get(i);
                    res = res + "\r\n==> "+element.classname+'.'+element.methodname;
                }
                Log.warn(DynamideSecurityManager.class, res);
            }
            throw new SecurityException("Forbidden [10]");
        }
    }
    private static boolean java_version_warned = false;

    //=============== Overrides ===================================================

    public void checkCreateClassLoader() {
        //Actually, you must be able to create a classloader. Class.forName and a number of system calls require this
        //The big deal is to disallow checkPermission("setContextClassLoader").
    }

    public void checkAccess(Thread g) {
        //Actually, everything is OK.  You can't get out of your thread group.
    }

    public void checkAccess(ThreadGroup g) { }
    public void checkExit(int status) {
        if (isCurrentThreadWorker()) deny("checkExit");
    }

    public void checkExec(String cmd) {
        if (isCurrentThreadWorker()) deny("checkExec");
    }
    public void checkLink(String lib) {
        if (lib.equals("net")){
            System.out.println("\r\n\r\n\r\n==============================\r\nSECURITY MANAGER ALLOWING LIB: "+lib);
            System.out.println("\r\n================================\r\n\r\n\r\n");
            return; //OK for MongoDB, but: TODO: we must make this Mongo-specific, and also check what RDB's get through the dynamide security manager.
        } else if (  lib.equals("awt")
                  || lib.indexOf("liblwawt.dylib") > -1
                  || lib.indexOf("fontmanager") > -1
                  || lib.indexOf("AppleScriptEngine") > -1

                ) {
            System.out.println("\r\n\r\n\r\n==============================\r\nSECURITY MANAGER ALLOWING LIB: "+lib);
            return;
        } else {
            System.out.println("\r\n\r\n\r\n==============================\r\nSECURITY MANAGER ***NOT*** ALLOWING LIB: "+lib);
        }
        if (isCurrentThreadWorker()) deny("checkLink");
    }
    public void checkRead(FileDescriptor fd) { }
    public void checkRead(String file) { }
    public void checkRead(String file, Object context) { }
    public void checkWrite(FileDescriptor fd) { }
    public void checkWrite(String file) { }
    public void checkDelete(String file) { }
    public void checkConnect(String host, int port) { }
    public  void checkConnect(String host, int port, Object context) {
        deny("checkConnect");
    }

    public void checkListen(int port) {
        if (isCurrentThreadWorker()) deny("checkListen");
    }

    public void checkAccept(String host, int port) { }
    public void checkMulticast(InetAddress maddr) { }
    public void checkMulticast(InetAddress maddr, byte ttl) { }
    public void checkPermission(Permission perm) {
        checkPermission(perm, null);
    }
    /** We avoid code like this:
     *
     *
     <pre>
     t = new Thread();
     t.setContextClassLoader(new com.dynamide.security.DynamideClassLoader());</pre>
     *
     * We dissallow any and all context class loaders.
     */
    public void checkPermission(Permission perm, Object context) {
        //context may be null.
        String pName = perm.getName();
        String threadname = Thread.currentThread().getName();
        //if (threadname.indexOf("resin-") == -1){
        //    System.out.println("checkPermission(perm,context): ("+perm+","+context+") thr: "+threadname);
        //}
        if ( pName.equals("setSecurityManager") ) {
            if (isCurrentThreadWorker()){
               throw new SecurityException("ERROR: [76] can't create new SecurityManger to replace DynamideSecurityManager");
            }
        } else if (pName.equals("setContextClassLoader")){
            if (isCurrentThreadWorker()){
                throw new SecurityException("ERROR: [77] can't setContextClassLoader");
            }
        }
    }
    public void checkPropertiesAccess() { }
    public void checkPropertyAccess(String key) { }
    public void checkPropertyAccess(String key, String def) { }
    public boolean checkTopLevelWindow(Object window) {
        if (isCurrentThreadWorker())
            return false;
        else
            return true;
    }
    public void checkPrintJobAccess() {
        if (isCurrentThreadWorker())deny("checkPrintJobAccess");
    }
    public void checkSystemClipboardAccess() {
        if (isCurrentThreadWorker())deny("checkSystemClipboardAccess");
    }
    public void checkAwtEventQueueAccess() {
        if (isCurrentThreadWorker())deny("checkAwtEventQueueAccess");
    }
    public void checkPackageAccess(String pkg) { }
    public void checkPackageDefinition(String pkg) {
        if (isCurrentThreadWorker()) deny("checkPackageDefinition");
    }

    public void checkSetFactory() {
        //%% todo: either saxon or webmacro wants this.
        //%% figure out how to still check:  if (isCurrentThreadWorker()) deny("checkSetFactory");
    }

    public void checkMemberAccess(Class clazz, int which) { }
    public void checkSecurityAccess(String provider) { }

}
