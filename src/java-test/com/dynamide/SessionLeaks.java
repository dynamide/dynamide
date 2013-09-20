package com.dynamide;

/*
 * 1/7/2005 4:27AM
 *   I added a disableProfiler in DynamideObject.
 *    -jdomFile test seems to run without leaks now.
 *    -minimal runs fine, too.
 *    For these I ran with 1000 loops, and watched the Windows memory monitor.
 *
 * -loadPage went from 15M to 35M usage
 * -loadPage without call to get page by id also leaks. different session constructor than -minimal.
 * -outputPage works now, stable at 23,400K to 23,600K max.  There seems to be a growing memory pool
 *   from startup, but if you run 1000 runs, it stabilizes.
 *

*/
import com.dynamide.*;
import com.dynamide.util.*;
import com.dynamide.resource.*;
import com.dynamide.event.*;

public class SessionLeaks {
    public class TestThread extends Thread {
        public TestThread(String test, int MAX, String filename, SessionLeaks caller,
                          String uri, String url, String pageID, boolean doWaits){
            this.MAX = MAX;
            this.filename = filename;
            this.test = test;
            this.caller = caller;
            this.uri = uri;
            this.url = url;
            this.pageID = pageID;
            this.doWaits = doWaits;
        }
        private String filename;
        private int MAX = 1;
        private String test;
        private SessionLeaks caller;
        private String uri;
        private String url;
        private String pageID;
        private boolean doWaits;

        public void run() {
            try {
                System.out.println("MAX: "+MAX);
                for (int p=0; p < MAX; p++) {
                    System.out.println(Thread.currentThread().getName()+" >>>>>>>>>>>>>>>>>>>>>> "+p);
                    Profiler profiler = Profiler.getSharedProfiler();
                    if (profiler!=null) profiler.clear();
                    System.out.print(".");
                    if (p % 100 == 0){
                        System.out.println("===== SessionLeaks.  Runs: "+p);
                    }
                    try {
                        if (test.equals("allPages")){
                            sessionAllPages();
                        } else if (test.equals("outputPage")) {
                            sessionOutputPage(pageID);
                        } else if (test.equals("minimal")) {
                            sessionMinimal(p);
                        } else if (test.equals("loadPage")) {
                            sessionGetPageByID(pageID);
                        } else if (test.equals("contextNode")) {
                            contextNode(p);
                        } else if (test.equals("field")) {
                            field(p);
                        } else if (test.equals("fireEvent")) {
                            sessionFireAppEvent();
                        } else if (test.equals("jdomFile")) {
                            jdomFile(filename);
                        }
                    } catch (Exception e)  {
                        System.out.println(Thread.currentThread().getName()+" caught Exception "+e+"\r\n"+Tools.errorToString(e, true, true));
                    }
                    System.gc();
                    Thread.yield();
                    if (doWaits){
                        System.out.println("\r\n****************** One test complete. Press Enter key. **************");
                        System.in.read();
                        System.in.read();
                        ResourceManager rm = ResourceManager.getRootResourceManager();
                        String res = rm.dumpRootContext();
                        FileTools.saveFile("\\tmp", "SessionLeaks.log."+p, res);
                    }

                }
            } catch (Exception e)  {
                System.out.println("Exception in thread: "+e);
            } finally {
                caller.threadComplete();
            }
        }
        private void sessionAllPages()
        throws Exception {
            Session session1 = Session.createSession(uri, url);  //url calls session.setDebugURL
            session1.loadAllPages();
            session1.close();
        }

        private void sessionMinimal(int p)
        throws Exception {
            Session session1 = Session.createTestSession("TEST1."+p, uri, "admin", true);
            if (session1 != null) session1.close();
        }

        private void sessionMinimalMulti(int p)
        throws Exception {
            Session session1 = Session.createTestSession("TEST1."+p, uri, "admin", true);
            Session session2 = Session.createTestSession("TEST2."+p, uri, "admin", true);
            Session session3 = Session.createTestSession("TEST3."+p, uri, "admin", true);

            if (session1 != null) session1.close();
            if (session2 != null) session2.close();
            if (session3 != null) session3.close();
        }


        private void sessionOutputPage(String pageID)
        throws Exception {
            Session session1 = Session.createSession(uri, url);  //url calls session.setDebugURL
            //System.out.println("session1: "+session1);
            //System.out.println("page:::::::::::::::::::"+
            session1.getPageByID(pageID).outputPage();
            session1.close();
        }

        private void sessionGetPageByID(String pageID)
        throws Exception {
            Session session1 = Session.createSession(uri, url);  //url calls session.setDebugURL
            //4/11/2004 5:49PM 19,000K start.  20,176 end. == 1960 bytes per session lost.
            System.out.println("session1 id: "+session1.getSessionID());
            session1.getPageByID(pageID);
            session1.close();
            System.out.println("\r\n#######################\r\nsessionGetPageByID closing "+session1.getSessionID()+ " in thread "+Thread.currentThread().getName());
        }

        private void sessionFireAppEvent()
        throws Exception {        //18,000K -->
            Session session = Session.createSession("/dynamide/doco", "/dynamide/doco?foo=bar");  //url calls session.setDebugURL
            ScriptEventSource appActionSource = session.getEventSource("application_onAction");
            //System.out.println("event source: "+appActionSource.source);
            ScriptEvent event = session.fireEvent(session, "application_onAction", "main", "main", "", appActionSource, "", false);
            //System.out.println("event: "+event);
            session.close();
        }

        private void jdomFile(String fn)
        throws Exception {
            JDOMFile jdf = new JDOMFile(null, fn);
            jdf = null;
        }

        private void contextNode(int p)
        throws Exception {
            ContextNode node = new ContextNode("TEST"+p);
            node = null;
        }

        private void field(int p)
        throws Exception {
            Field f = new Field(null, null, "TEST"+p, ""+p);
            f = null;
        }


    }


    //=================================================================================

    private int numComplete = 0;

    public synchronized void threadComplete(){
        numComplete++;
    }

    private void runThreads(String test, int MAX, String filename, int numThreads, boolean doWaits,
                            String uri, String url, String pageID)
    throws Exception {
        for (int i=0; i < numThreads; i++) {
            TestThread t = new TestThread(test, MAX, filename, this, uri, url, pageID, doWaits);
            t.start();
        }
        while ( numComplete < numThreads ) {
            Thread.yield();
        }

        System.out.println("rm dump: "+ResourceManager.getRootResourceManager().getSadd());

        if (doWaits){
            System.out.println("\r\nTest complete. Press Enter key.");
            System.in.read();
            System.in.read();
        }

        com.dynamide.resource.ResourceManager.shutdown();
        System.gc();
        try {Thread.sleep(100);} catch (Exception e)  { }
        System.gc();
        Thread.yield();

        if (doWaits){
            System.out.println("\r\nResourceManager shut down. Read memory, then press Enter key.");
            System.in.read();
            System.in.read();
        }

        System.exit(0);
    }

    public static void main(String [] args)
    throws Exception {

        Opts opts = new Opts(args);
        opts.addOption("-nowaits", Boolean.class, false);

        int NUM_THREADS = opts.getOptionInt("-threads", 1);           //Tools.stringToInt(args[0]);
        int MAX = opts.getOptionInt("-loops", 1);         //Tools.stringToInt(args[1]);
        String test = opts.getOption("-testname"); //args[2];
        if ( "".equals(test) ) {
            System.out.println("Usage: SessionLeaks {-nowaits} {-threads numThreads} {-loops numLoops} {-testname allPages|outputPage|minimal|loadPage|contextNode|field|fireEvent|jdom -file relFilename}");
            System.exit(1);
        }

        boolean doWaits = true;
        if ( opts.getOptionBool("-nowaits") ) {
            System.out.println("-nowaits: true");
            doWaits = false;
        }
        com.dynamide.resource.ResourceManager.createStandaloneForTest();
        if (doWaits){
            System.out.println("ResourceManager created. Press any key to continue test.");
            System.in.read();
            System.in.read();
        }

        String home = com.dynamide.resource.ResourceManager.getDynamideHomeFromEnv();
        String filename = opts.getOption("-file");
        if (filename.length()>=4){
            filename = home+"/"+filename;
        }

        String uri = "/dynamide/admin";
        String url = "?USER=laramie";

        uri = opts.getOption("-uri");
        url = opts.getOption("-url");

        String pageID = opts.getOption("-pageid");

        System.out.println("using uri: "+uri + " url: "+url);
        SessionLeaks s = new SessionLeaks();
        s.runThreads(test, MAX, filename, NUM_THREADS, doWaits, uri, url, pageID);

    }
    //4/11/2004 7:15PM -minimal - no leaks if actual Session constructor called.

    /*
     * examples
     *
     *   java -DDYNAMIDE_HOME=c:\dynamide  com.dynamide.SessionLeaks 1000 -jdomFile build\ROOT\homes\dynamide\assemblies\com-dynamide-apps-1\apps\admin\application.xml
     *   java -DDYNAMIDE_HOME=c:\dynamide  com.dynamide.SessionLeaks 1000 -jdomFile build\ROOT\homes\dynamide\assemblies\com-dynamide-apps-1\apps\admin\fielddefs.xml
     *
     * */
}