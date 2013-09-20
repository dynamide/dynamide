package com.dynamide.util;

import com.dynamide.DynamideObject;

public class RegisteredAction {
    public DynamideObject sender;
    public String action;
    public String eventName;
    public Object inputObject;
    public String toString(){
        return "RegisteredAction:{action:"+action+",sender:"+sender+",eventName:"+eventName+",inputObject:"+inputObject+"}";
    }
    public String dumpHTML(){
        return
         "<table border='1' cellpadding='2' cellspacing='0'>"
          +"<tr><td>action</td><td>"+action+"</td></tr>"
          +"<tr><td>sender</td><td>"+sender+"</td></tr>"
          +"<tr><td>eventName</td><td>"+eventName+"</td></tr>"
          +"<tr><td>inputObject</td><td>"+inputObject+"</td></tr>"
        +"</table>";
    }
}
