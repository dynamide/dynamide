package com.dynamide;

import org.webmacro.*;
import java.io.*;

import junit.framework.*;

import com.dynamide.*;


public class WebmacroTest extends DynamideTestCase {
    public WebmacroTest(String name){
        super(name);
    }

    public void setUp()
    throws Exception {
        com.dynamide.resource.ResourceManager.configureLogForTest();
    }

    public static Test suite() {
        return new TestSuite(WebmacroTest.class);
    }

    public void testCategory()
    throws Exception {
        int loops = 10;
        int wmloops = 9000;
        logInfo("[version 4] Webmacro memory test ["+loops+" loops] ...");
        //Tools.readln("Press Enter");     //uncomment to measure initial state, if you like.
        logInfo(cleanAndReportMemory());
        org.webmacro.WM webmacro = new org.webmacro.WM();
        org.webmacro.Broker broker = webmacro.getBroker();
        for (int i=0; i < loops; i++) {
            for (int j=0; j < wmloops; j++) {
                //Note: everything is static, and everything is done in a function call,
                // so the VM will clean up that call's stack frame.
                // On my win2k machine, this routine burns about 3 megs each call.
                runContextTest(webmacro);
            }
            //Tools.readln("Press Enter");
        }
        logInfo(cleanAndReportMemory());
    }

    //This shows the memory leaks on std-out.
    //It also pauses so you can use an OS-dependent tool, such as perfmon or whatever.
    //Note that I'm not doing anything, just getting contexts, cloning them, and cleaning them up.
    public static void main(String [] args)
    throws Exception {
        int loops = 10;
        int wmloops = 30000;
        System.out.println("[version 3] Webmacro memory test ["+loops+" loops] ...");
        //readln("Press Enter");     //uncomment to measure initial state, if you like.
        org.webmacro.WM webmacro = new org.webmacro.WM();
        org.webmacro.Broker broker = webmacro.getBroker();
        for (int i=0; i < loops; i++) {
            for (int j=0; j < wmloops; j++) {
                //Note: everything is static, and everything is done in a function call,
                // so the VM will clean up that call's stack frame.
                // On my win2k machine, this routine burns about 3 megs each call.
                runContextTest(webmacro);
            }
            System.out.println(cleanAndReportMemory());
            com.dynamide.util.Tools.readln("Press Enter");
        }
    }

    private static Context getContext(WebMacro wm)
    throws InitException  {
        //IMPORTANT: call cloneContext() so you get one context per thread, as dictated in the Webmacro docs.
        //Context initialContext = wm.getContext();
        //Context c = initialContext.cloneContext();
        Context c = wm.getContext();
        return c;
    }

    private static void runContextTest(WebMacro wm)
    throws org.webmacro.InitException {
        Context c = getContext(wm);
        // I would expand templates here.  but leak has already happened.
        // clean up, and return, then measure loss.
        //wm2.01b c.recycle();   // webmacro will return this object to the pool.
        c.clear(); //wm2.01b
        c = null;
    }

    public static String cleanAndReportMemory(){
        System.gc();
        Thread.currentThread().yield();
        System.runFinalization();
        System.gc();
        try {Thread.currentThread().sleep(200);} catch (Exception e){}
        System.gc();
        System.gc();
        try {Thread.currentThread().sleep(200);} catch (Exception e){}
        System.gc();
        System.runFinalization();
        //If the VM hasn't cleaned up now, it ain't a gonna!

        String msg = "  Consumed VM Memory:  "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" (bytes)";
        return msg;
    }



}