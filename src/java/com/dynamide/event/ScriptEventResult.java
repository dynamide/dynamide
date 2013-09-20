package com.dynamide.event;

import com.dynamide.util.trace.ILoggable;

public class ScriptEventResult implements ILoggable {
    public String source = "";
    public String result = "";
    public String eventDetail = "";
    public String toString(){
        return "source: "+source+"\r\nresult: "+result; //%% and more...
    }
    public String toHTML(){
        return "source: "+source+"\r\nresult: "+result; //%% and more...
    }
}
