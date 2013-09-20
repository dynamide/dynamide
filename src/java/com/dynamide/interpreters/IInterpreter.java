package com.dynamide.interpreters;

public interface IInterpreter {
    public com.dynamide.Session getSession();
    public void close() throws Throwable;
    public void setSession(com.dynamide.Session session);
    public Object eval(String source) throws Exception;
    public com.dynamide.event.ScriptEvent fireEvent(com.dynamide.event.ScriptEvent event,
                                                    String procName,
                                                    com.dynamide.event.ScriptEventSource eventSource,
                                                    boolean sourceOnly);
    public String getVersion();
    public void setVariable(String name, Object value);
    public void unsetVariable(String name);
    public String getOutputBuffer();
    public String emptyOutputBuffer();


}

