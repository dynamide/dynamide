package com.dynamide.util;

import org.webmacro.Context;

public class WEBMACRO_CONTEXTPointer {
    private Context m_context = null;
    public Context getContext(){
        return m_context;
    }
    public void setContext(Context c){
        m_context = c;
    }
    public String toString(){
        return "WEBMACRO_CONTEXTPointer";
    }
}
