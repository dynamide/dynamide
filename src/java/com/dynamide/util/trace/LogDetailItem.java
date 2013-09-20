package com.dynamide.util.trace;

public class LogDetailItem {
    public LogDetailItem(String name, String type, String body, String location, String status){
        this.name = name;
        this.type = type;
        this.body = body;
        this.location = location;
        this.status = status;
        this.sequence = gSequence++;
    }
    public String name;
    public String type;
    public String body;
    public String location;
    public String status;
    public int sequence = -1;

    public static final String EC_START = "START";
    public static final String EC_DONE = "DONE";
    public static final String EC_INFO = "INFO";
    public static final String EC_ERROR = "ERROR";

    public String formatHtmlStatus(){
        return formatHtmlStatus(status);
    }

    public static String formatHtmlStatus(String status){
        if ( status.equals(EC_ERROR) ) {
            return "<font color='red'><b>"+status+"</b></font>";
        }
        return "<font color='darkblue'>"+status+"</font>";
    }

    public String toString(){
        return toHTML();
    }
    private static int gSequence = 0;
    public String toHTML(){
        return
         "<table border='1' cellpadding='0' cellspacing='0'>"
        +"  <tr>"
        +"    <td>name</td><td>"+name+"</td>\r\n"
        +"  </tr><tr>"
        +"    <td>location</td><td>"+location+"</td>\r\n"
        +"  </tr><tr>"
        +"    <td>status</td><td>"+formatHtmlStatus(status)+"</td>\r\n"
        +"  </tr><tr>"
        +"    <td>Time</td><td>["+sequence+"] "+com.dynamide.util.Tools.nowLocale()+" ["+System.currentTimeMillis()+"]</td>\r\n"
        +"  </tr><tr>"
        +"    <td>body</td><td>"+body+"</td>\r\n"
        +"  </tr>"
        +"</table>";
    }
}

