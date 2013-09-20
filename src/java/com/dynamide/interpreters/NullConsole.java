package com.dynamide.interpreters;

import java.io.StringReader;

import com.dynamide.DynamideObject;

public class NullConsole extends DynamideObject implements IConsole, bsh.ConsoleInterface {

    public NullConsole(String id){
        this((DynamideObject)null);
        m_id = id;
    }
    public NullConsole(DynamideObject owner){
        super(owner);
        m_in = new StringReader("");
    }

    public void close(){
        m_in = null;
        //System.out.println("closing NullConsole"+gIndex+": ");
        gIndex++;
        setOwner(null);
    }

    private String m_id = "";
    private static int gIndex = 0;
    private StringReader m_in;

    public void finalize() throws Throwable{
        close();
        super.finalize();
    }

    public String getBuffer(){
        return "";
    }

    public String emptyBuffer(){
        return "";
    }


    //========= interface ConsoleInterface ===============================

    public void error(java.lang.Object o) {
    }
    public void error(java.lang.String s) {
    }

    public java.io.PrintStream getErr() {
        return System.err;
    }

    public java.io.Reader getIn() {
        return m_in;
    }

    public java.io.PrintStream getOut() {
        return System.out;
    }

    public void print(java.lang.String s) {
    }
    
    public void print(java.lang.Object o) {
    }

    public void println(java.lang.Object o) {
    }

    public void println(java.lang.String s) {
    }
    //========================================

}
