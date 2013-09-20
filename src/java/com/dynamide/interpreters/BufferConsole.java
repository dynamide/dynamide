package com.dynamide.interpreters;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import com.dynamide.DynamideObject;

public class BufferConsole extends DynamideObject implements IConsole, bsh.ConsoleInterface {

    public BufferConsole(String id){
        super(null);
        m_id = id;
        m_bos = new java.io.ByteArrayOutputStream();
        m_stream = new java.io.PrintStream(m_bos);
        m_in = new StringReader("");
    }
    public BufferConsole(DynamideObject owner){
        this(owner.getObjectID());
        setOwner(owner);
    }


    private String m_id = "";

    private static int gIndex = 0;


    public void finalize() throws Throwable{
        close();
        super.finalize();
    }

    public void close(){
        if ( m_stream != null ) {
            m_stream.flush();
            try {if(m_bos!=null)m_bos.reset();} catch (Exception e)  {System.out.println("ERROR: couldn't reset() m_bos in BufferConsole"+e);}
            m_stream.close();
        }
        m_stream = null;
        m_bos = null;

        if (m_in != null){
            m_in.close();
        }
        m_in = null;
        setOwner(null);
        gIndex++;
    }

    private ByteArrayOutputStream m_bos;
    private PrintStream m_stream;
    private StringReader m_in;

    public String getBuffer(){
        try{ m_bos.flush(); } catch (Exception e){ logError("[51] can't flush m_bos");}
        m_stream.flush();
        String res = m_bos.toString();
        return res;
    }

    public String emptyBuffer(){
        String res = getBuffer();
        m_bos.reset();
        return res;
    }

    //========= interface ConsoleInterface ===============================

    public void error(java.lang.Object o) {
    	error(o.toString());
    }
    
    public void error(java.lang.String s) {
        m_stream.println(s);
        try  { m_bos.flush(); } catch (Exception e){ logError("[50] can't flush m_bos");}
        m_stream.flush();
        System.out.println("BufferConsole:: "+s);
    }

    public java.io.PrintStream getErr() {
        return m_stream;
    }

    public java.io.Reader getIn() {
        return m_in;
    }

    public java.io.PrintStream getOut() {
        return m_stream;
    }

    public void print(java.lang.Object o) {
    	print(o.toString());
    }
    
    public void print(java.lang.String s) {
       	m_stream.print(s);
        try { m_bos.flush(); } catch (Exception e){logError("[49] can't flush m_bos");}
        m_stream.flush();
        System.out.println("BufferConsole:: "+s);
    }

    public void println(java.lang.Object o) {
    	println(o.toString());
    }
    
    public void println(java.lang.String s) {
    	m_stream.println(s);
        try { m_bos.flush();  } catch (Exception e){ logError("[48] can't flush m_bos");}
        m_stream.flush();
        System.out.println("BufferConsole:: "+s);
    }

    //========================================

}
