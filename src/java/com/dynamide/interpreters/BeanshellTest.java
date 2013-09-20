//This really goes in the test tree.
package com.dynamide.interpreters;

import bsh.Interpreter;

import com.dynamide.event.ScriptEvent;

public class BeanshellTest {
    public static void main(String args[]){
        System.out.println("version 2");
        String msg = "  Total VM Memory: "+Runtime.getRuntime().totalMemory()+" (bytes)\r\n"
                    +"  Available VM Memory: "+Runtime.getRuntime().freeMemory()+" (bytes)\r\n";
        System.out.println("memory before: \r\n"+msg);
        for (int i = 0; i< 10; i++) runInterp();
        System.gc();
        Thread.yield();
        System.runFinalization();
        System.gc();
        try {Thread.sleep(1000);} catch (Exception e){}
        System.gc();
        System.gc();
        System.gc();
        System.runFinalization();
        msg = "  Total VM Memory: "+Runtime.getRuntime().totalMemory()+" (bytes)\r\n"
                    +"  Available VM Memory: "+Runtime.getRuntime().freeMemory()+" (bytes)\r\n";
        System.out.println("memory after: \r\n"+msg);
    }

    private static void runInterp(){
        ScriptEvent event = new ScriptEvent();

        NullConsole m_console = new NullConsole("test");
        Interpreter m_Interpreter;
        //m_Interpreter = new bsh.Interpreter();
        m_Interpreter = new bsh.Interpreter(m_console);
        m_Interpreter.setClassLoader(BeanshellTest.class.getClassLoader());

        String eventSource = "print(hello);";
        String hello = "HELLO";

        try {
            m_Interpreter.set("hello", hello);
            m_Interpreter.eval(eventSource);
            m_Interpreter.unset("hello");
        } catch (bsh.EvalError e){
            System.out.println("[42test] bsh.EvalError."+e);
        }
        m_Interpreter = null;
        m_console = null;

        StringBuffer big = new StringBuffer();
        String pad = "aaaaaaaaaa";
        for (int i = 0; i< 10000; i++) big.append(pad);
        big.setLength(0);
        big = null;

    }
}
