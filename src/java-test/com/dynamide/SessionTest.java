package com.dynamide;
//run like this:
//  java -Ddynamide.RESOURCE_ROOT="C:/dynamide/build/ROOT" com.dynamide.SessionTest -junit
//
//or like this:
//  java -Ddynamide.RESOURCE_ROOT="C:/dynamide/build/ROOT" com.dynamide.SessionTest -uri /dynamide/demo -params "?USER=laramie" -page page1
import java.io.*;

import junit.framework.*;

import com.dynamide.resource.*;
import com.dynamide.util.*;

public class SessionTest extends DynamideTestCase {
    public SessionTest(String name){
        super(name);
    }

    public void setUp() { 
        try {
            Log.info(SessionTest.class, "setUp()");
            ResourceManager.createStandaloneForTest();
        } catch( Exception e ){
            fail("Exception in setUp: "+e);
        }
    }

    public void tearDown() {
        //Log.info(SessionTest.class, "tearDown()");
        try {
            ResourceManager.shutdown();
        } catch( Exception e ){
            fail("Exception in tearDown: "+e);
        }
    }

    public static Test suite() {
        //Log.info(SessionTest.class, "suite()");
        return new TestSuite(SessionTest.class);
    }

    //========= junit tests ===================

    public void testDemoAppText()
    throws Exception {
        String url = "?SESSIONID=TEST_DEMO_TEXT&USER=testuser";
        runJUnitApplicationTest("/dynamide/demo",  url, "page1", ServletTools.BROWSER_DYNAMIDE_TEXT);
    }

    public void testDemoApp()
    throws Exception {
        String url = "?SESSIONID=TEST_DEMO&USER=testuser";
        runJUnitApplicationTest("/dynamide/demo",  url, "*", ServletTools.BROWSER_UNKNOWN);
    }

    public void testAdminApp()
    throws Exception {
        String url = "?SESSIONID=TEST_ADMIN&USER=testuser";
        runJUnitApplicationTest("/dynamide/admin", url, "*", ServletTools.BROWSER_UNKNOWN);
    }

    public void testDocoApp()
    throws Exception {
        String url = "?SESSIONID=TEST_DOCO&USER=testuser";
        runJUnitApplicationTest("/dynamide/doco",  url, "*",  ServletTools.BROWSER_UNKNOWN);
    }

    public void testIDEApp()
    throws Exception {
        String url = "?SESSIONID=TEST_IDE&USER=testuser";
        runJUnitApplicationTest("/dynamide/ide",  url, "*", ServletTools.BROWSER_UNKNOWN);
    }

    public void testTestsuiteUploadApp()
    throws Exception {
        String url = "?SESSIONID=TEST_UPLOAD&USER=testuser";
        runJUnitApplicationTest("/dynamide/test-suite/upload",  url, "*", ServletTools.BROWSER_UNKNOWN);
    }

    public void testNonexistentApp()
    throws Exception {
        try {
            runJUnitApplicationTest("/dynamide/thisDoesntExist", "", "*", ServletTools.BROWSER_UNKNOWN);
        } catch (Exception e){
            return;
        }
        fail("Non-existent application should have failed search by URI");
    }

    public void testModes()
    throws Exception{
        Session session = Session.createSession("/dynamide/test-suite/upload", "?USER=testmodes");
        session.setModeFlags(0);

        session.setDesignMode(true);
        System.out.println("getModeString: design: "+session.getModeString());
        assertTrue(session.getDesignMode());

        session.setDesignMode(false);
        System.out.println("getModeString: : "+session.getModeString());
        assertFalse(session.getDesignMode());

        session.setBatchMode(true);
        session.setDebugMode(true);
        session.setDesignMode(true);
        session.setTestMode(true);
        System.out.println("getModeString: batch:debug:design:test "+session.getModeString());
        assertTrue(session.getBatchMode());
        assertTrue(session.getDebugMode());
        assertTrue(session.getDesignMode());
        assertTrue(session.getTestMode());

        session.setDebugMode(false);
        System.out.println("getModeString: batch:design:test "+session.getModeString());
        assertTrue(session.getBatchMode());
        assertFalse(session.getDebugMode());
        assertTrue(session.getDesignMode());
        assertTrue(session.getTestMode());

        session.setTestMode(false);
        System.out.println("getModeString: batch:design "+session.getModeString());
        assertTrue(session.getBatchMode());
        assertFalse(session.getDebugMode());
        assertTrue(session.getDesignMode());
        assertFalse(session.getTestMode());

        session.setTestMode(true);
        System.out.println("getModeString: batch:design:test "+session.getModeString());
        assertTrue(session.getBatchMode());
        assertFalse(session.getDebugMode());
        assertTrue(session.getDesignMode());
        assertTrue(session.getTestMode());
    }

    public void testLoadFields()
    throws Exception {
        Session.createSession("/dynamide/test-suite/load-fields/default", "", Session.MODE_TEST);
        Session.createSession("/dynamide/test-suite/load-fields/inline", "", Session.MODE_TEST);
        Session.createSession("/dynamide/test-suite/load-fields/external", "", Session.MODE_TEST);
        try {
            Session.createSession("/dynamide/test-suite/load-fields/inline-external", "", Session.MODE_TEST);
            fail("createSession should have failed on 'inline-external'");
        } catch (Exception e)  {
            //good.
        }
        try {
            Session.createSession("/dynamide/test-suite/load-fields/wrong-filename", "", Session.MODE_TEST);
            fail("createSession should have failed on 'wrong-filename'");
        } catch (Exception e)  {
            //good.
        }
    }

    public void testSecureRedirects()
    throws Exception {
        ResourceManager rm = ResourceManager.getRootResourceManager();
        IContext context = (IContext)rm.find("/conf/secure");

        context.rebindAttribute("protocol", "https");
        context.rebindAttribute("port", "8080");
        context.rebindAttribute("host", "TEST.DYNAMIDE.COM");
        Session s = Session.createSession("/dynamide/test-suite/enums");
        //generateSecureRedirect(String protocol, String currentHost, String currentPort, String currentURL){
        String red = s.generateSecureRedirect("http", "FROM.DYNAMIDE.COM", "7080", "/dynamide/test-suite/radiogroup");
        System.out.println("REDIRECT RESULT: "+red);

    }



    //========= implementation ===================

    public void runJUnitApplicationTest(String uri, String debugURL, String page, int browserID)
    throws Exception {
        int errCount = runApplicationTest(uri, debugURL, page, browserID);
        assertTrue(errCount==0);
    }

    public int runApplicationTest(String uri, String debugURL, String page, int browserID)
    throws Exception {
        int startErr = DynamideLogAppender.getAppender().getErrorCount();
        runApplication(uri, debugURL, page, browserID);
        int endErr = DynamideLogAppender.getAppender().getErrorCount();
        return endErr-startErr;
    }

    public String runApplication(String uri, String debugURL, String page, int browserID)
    throws Exception {
        Session session = null;
        try {
            session = Session.createSession(uri, debugURL);  //url calls session.setDebugURL
            if (page.equals("*")){
                String pagesList = "";
                java.util.Iterator pages = session.getPages().iterator();
                while ( pages.hasNext() ) {
                    String pageName = (String)pages.next();
                    Page p = session.getPageByID(pageName);
                    pagesList = pagesList + ";"+ pageName;
                    assertNotNull("page by id: "+pageName, p);
                    logInfo("testing page: "+pageName);
                    p.outputPage();
                }
                return pagesList;
            } else if (page.length()>0){
                Page p = session.getPageByID(page);
                return p.outputPage().result;
            }
            String id = session.toString();
            return id;
        } finally {
            if (session!=null) session.close();
        }
    }

    public static void main(String args[])
    throws Exception {
        try {
            Opts opts = new Opts(args);
            if (opts.getOptionBool("-junit")){
                junit.textui.TestRunner.run(suite());
            } else if (args.length>0) {
                String uri = opts.getOption("-uri");
                String params = opts.getOption("-params");
                String page = opts.getOption("-page");
                if ( uri.length()>0 ) {
                    SessionTest test = new SessionTest("testApplication");
                    test.setUp();
                    System.out.println(test.runApplication(uri, params, page, ServletTools.BROWSER_UNKNOWN));
                }
            } else {
                System.out.println("Usage: ");
                System.out.println("Syntax 1: ");
                System.out.println("    SessionTest -uri <App URI> [-params <Query Params>] [-page <pageID>]");
                System.out.println("      example: ");
                System.out.println("        java ");
                System.out.println("           -DDYNAMIDE_HOME=\"C:/dynamide\"");
                System.out.println("           com.dynamide.SessionTest");
                System.out.println("           -uri /dynamide/demo");
                System.out.println("           -params \"?SESSIONID=mytest\"");
                System.out.println("           -page page1");
                System.out.println("");
                System.out.println("Syntax 2: ");
                System.out.println("    SessionTest -junit");
            }
        } catch (Throwable t) {
            System.err.println("ERROR in SessionTest: "+t);
            t.printStackTrace();
        }
        System.exit(0);
    }

}