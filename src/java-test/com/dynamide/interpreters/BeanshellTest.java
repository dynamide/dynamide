package com.dynamide.interpreters;

import junit.framework.*;

import java.util.*;

import com.dynamide.*;
import com.dynamide.util.Tools;

import bsh.*;

/** Do NOT use this class in a production environment where you have other threads running --
 *  it calls System.gc() several times in a row, which can single-thread the entire JVM for the
 *  duration of the test.
 */
public class BeanshellTest extends DynamideTestCase {
    public BeanshellTest(String name){
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new BeanshellTest("testBshMemoryUsage"));
        return suite;
    }

    public void setUp()
    throws Exception {
        com.dynamide.resource.ResourceManager.createStandaloneForTest();
    }

    public void tearDown()
    throws Exception {
        com.dynamide.resource.ResourceManager.shutdown();
    }

    public void testBshMemoryUsage()
    throws Exception {
        try {
            boolean doEval = false;
            System.out.println("##################### in testBshMemoryUsage ####################");
            long used = memoryUsage(doEval);
            if ( used > 10000){
                fail("Memory used in loops: "+used+" was too much.");
            }
        } catch (Throwable t)  {
            System.out.println("Caught Throwable in testBshMemoryUsage"+t);
        }
    }

    private static long baseline = 0;

    private long memoryUsage(boolean doEval)
    throws Exception {
        long consumed0 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        //System.out.println("version 1");
        System.gc();
        Thread.currentThread().yield();
        System.gc();
        Thread.currentThread().yield();
        System.gc();

        long mbt, mbf, mat, maf, mbc, mac;
        mat = Runtime.getRuntime().totalMemory();
        mbt = Runtime.getRuntime().totalMemory();
        mbf = Runtime.getRuntime().freeMemory();
        mac = mat-mbf;


        mbc = mbt-mbf;
        String msg = "  Total VM Memory: "+mbt+" (bytes)\r\n"
                    +"  Available VM Memory: "+mbf+" (bytes)\r\n"
                    +"  Consumed VM Memory: "+mbc+" (bytes)\r\n";
        //System.out.println("memory before: \r\n"+msg);
        System.out.println("[before] "+mat+"    "+mbf+"    "+mac+"    "+(mbf-baseline));

        for (int i = 0; i< 100; i++) {
            //======Do Tetst==============
            runInterp(doEval);
        }

        try {
            Thread.sleep(6000);
        } catch( Exception e ){
            //
        }

        System.gc();
        Thread.currentThread().yield();
        try {
            Thread.sleep(2000);
        } catch( Exception e ){
            //
        }
        System.gc();
        Thread.currentThread().yield();
        System.gc();
        mat = Runtime.getRuntime().totalMemory();
        maf = Runtime.getRuntime().freeMemory();
        mac = mat-maf;

        msg = "  Total VM Memory: "+mat+" (bytes)\r\n"
                    +"  Available VM Memory: "+maf+" (bytes)\r\n"
                    +"  Consumed VM Memory: "+mac+" (bytes)\r\n";
        //System.out.println("memory after: \r\n"+msg);
        //System.out.println("Leaked memory?: "+(mac-mbc));
        //System.out.println("Leaked memory over baseline: "+(baseline-maf));
        System.out.println("[after ] "+mat+"    "+maf+"    "+mac+"    "+(maf-baseline));
        long consumed1 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        return consumed1-consumed0;
    }

    static String testScript =
      "import com.dynamide.util.StringTools;"
     +"import com.dynamide.util.Tools;"
     +"byte[] ba = new byte[20000]; ba[10000] = (byte)'a';"
     +"String sbig = new String(ba)+Tools.now();"
     +"s = new StringTools();"
     +"a = \"hello\";"
     +"b = \", world\";"
     +"biggie=s.searchAndReplaceAll(a, \"h\", \"H\")+b;"
     +"/*System.out.println(\"test \"+Tools.nowLocale());*/";

    private static String bigstring;
    static {
            StringBuffer sb = new StringBuffer();
            for (int i=0; i < 100; i++) {
                sb.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                sb.append(Tools.nowLocale());
            }
            bigstring = sb.toString();
    }

    private static void runInterp(boolean doEval)
    throws Exception {

        boolean useConsole = true;
        boolean noInterp = false;//to just test the JVM's caching of big strings.

        Interpreter interpreter;
        bsh.ConsoleInterface console;

        String bigUnique = bigstring+Tools.nowLocale();

        if ( noInterp ) {
            return;
        }

        if (useConsole){
            console = new NullConsole("BeanshellTest.nullconsole");
            //console = new BufferConsole("BeanshellTest.bufferConsole");


            //interpreter = new bsh.Interpreter(console);

            interpreter = new bsh.Interpreter(console.getIn(), console.getOut(), console.getErr(), false, null);
            //Reader in, PrintStream out, PrintStream err, boolean interactive, NameSpace namespace)
            interpreter.setConsole(console);

        } else {
            interpreter = new bsh.Interpreter();
        }

        interpreter.setClassLoader(BeanshellTest.class.getClassLoader());

        if (doEval) {
            interpreter.set("big"+Tools.nowLocale(), bigUnique);
            interpreter.eval(testScript);
        }

        interpreter = null;
        console = null;
    }

    public static void main(String args[])
    throws Exception {
        baseline = Runtime.getRuntime().freeMemory();
        BeanshellTest b = (new BeanshellTest("main"));
        for (int i=0; i < 10; i++) {
                 b.memoryUsage(false);
        }
    }

}