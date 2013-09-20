package com.dynamide.event;

/** When hookException is called to call the programmer's exception event, the exception category
 *  will be one of these values, which specify where the error occurred, e.g. EC_WIDGET means the
 *  exception occurred while trying to render a widget, while EC_PAGE means the exception occurred
 *  while trying to render a page.
 */
public class ErrorHandlerInfo {

    /**Exception Category*/
    public static final int EC_APPLICATION = 1;
    /**Exception Category*/
    public static final int EC_SESSION = 2;
    /**Exception Category*/
    public static final int EC_PAGE = 3;
    /**Exception Category*/
    public static final int EC_WIDGET = 4;
    /**Exception Category*/
    public static final int EC_SYSTEM = 5;

    public int category = EC_SYSTEM;
    public Throwable throwable = null;
    public ScriptEvent event = null;
    public String errorLink = "";

    public String getErrorCategory(){
        switch (category){
            case EC_APPLICATION:  return "APPLICATION";
            case EC_SESSION: return "SESSION";
            case EC_PAGE:    return "PAGE";
            case EC_WIDGET:  return "WIDGET";
            case EC_SYSTEM:  return "SYSTEM";
            default:         return "UNDEFINED_CATEGORY";
        }
    }

    public String toString(){
        return "ErrorHandlerInfo: "+getErrorCategory()+" "+errorLink+"\r\n"+com.dynamide.util.Tools.errorToString(throwable, true);
    }
}

