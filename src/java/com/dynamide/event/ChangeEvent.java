package com.dynamide.event;

public class ChangeEvent {
    public String fieldName = "";
    public Object sender;
    public Object oldValue;
    public Object newValue;
    public String toString(){
        return "ChangeEvent:{fieldName["+fieldName+"],sender["+sender+"],oldValue["+oldValue+"],newValue["+newValue+"]}";
    }
}

