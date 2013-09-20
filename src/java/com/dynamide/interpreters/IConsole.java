package com.dynamide.interpreters;

public interface IConsole {
    public void close();
    public java.lang.String getBuffer();
    public java.lang.String emptyBuffer();
    public void error(java.lang.String s);
    public java.io.PrintStream getErr();
    public java.io.Reader getIn();
    public java.io.PrintStream getOut();
    public void print(java.lang.String s);
    public void println(java.lang.String s);
}

