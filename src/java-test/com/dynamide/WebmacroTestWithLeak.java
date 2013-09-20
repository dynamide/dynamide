package com.dynamide;

import org.webmacro.*;
import java.io.*;

public class WebmacroTestWithLeak {

    //This shows the memory leaks on std-out.
    //It also pauses so you can use an OS-dependent tool, such as perfmon or whatever.
    //Note that I'm not doing anything, just getting contexts, cloning them, and cleaning them up.
    public static void main(String [] args)
    throws Exception {
        int loops = 10;
        int wmloops = 9000;
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
            System.out.println("\r\n"+cleanAndReportMemory());
            readln("Press Enter");
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
        //wm2.01b c.recycle();   //webmacro will return this object to the pool.
        c.clear(); //wm2.01b
        c = null;
    }

    public static String cleanAndReportMemory(){
        System.gc();
        Thread.currentThread().yield();
        System.runFinalization();
        System.gc();
        try {Thread.currentThread().sleep(1000);} catch (Exception e){}
        System.gc();
        System.gc();
        try {Thread.currentThread().sleep(1000);} catch (Exception e){}
        System.gc();
        System.runFinalization();
        //If the VM hasn't cleaned up now, it ain't a gonna!

        String msg = "  Consumed VM Memory:  "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" (bytes)\r\n";
        return msg;
    }

    public static String readln(String prompt) {
        try {
            if ( prompt != null && prompt.length() > 0 ) {
                System.out.println(prompt);
            }
            return readln(System.in);
        } catch (IOException e){
            System.err.println("ERROR reading input line.");
            return "";
        }
    }

    /** WARNING: Some unix terminal drivers expect \r as a line end char,
      *  although we look for strictly a \n here.
      */
    public static String readln(InputStream in)
    throws IOException {
        StringBuffer result = new StringBuffer();
        int ch;
        while ((ch = in.read()) != -1){
            if(ch==13){
                continue;
            }
            if (ch==10){
                break;
            }
            result.append((char)ch);

        }
        return result.toString();
    }


}