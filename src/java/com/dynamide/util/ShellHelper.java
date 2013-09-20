package com.dynamide.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import com.dynamide.DynamideObject;

public class ShellHelper {

    public static final String DUMPHTML_START = "<table border='1' cellspacing='0' cellpadding='2'>\r\n<tr><td>";
    public static final String DUMPHTML_LINESEP = "</td></tr>\r\n<tr><td>";
    public static final String DUMPHTML_ELEMSEP = "</td><td>";
    public static final String DUMPHTML_OBJPLACEHOLDER = "&#160;";
    public static final String DUMPHTML_END = "</td></tr></table>\r\n";

    /** dump is for html, list is for shell text, or you could use toHTML()...
      */
    public static String dumpMap(Map map){
        return dump(map.entrySet(),          //Collection
                    DUMPHTML_START,          //start
                    DUMPHTML_LINESEP,        //lineSeparator
                    DUMPHTML_ELEMSEP,        //elementSeparator
                    DUMPHTML_OBJPLACEHOLDER, //objectPlaceholder
                    DUMPHTML_END,
                    true);
    }

    public static String listMap(Map map){
        return dump(map.entrySet(),"", "\r\n", "; ", "", "", false);
    }

    public static String dump(Collection col,
                          String start,
                          String lineSeparator,
                          String elementSeparator,
                          String objectPlaceholder,
                          String end,
                          boolean dumphtml){
        StringBuffer buff = new StringBuffer(start);
        String sd;
        for (Iterator it = col.iterator(); it.hasNext(); ){
            Map.Entry entry =  (Map.Entry)it.next();
            buff.append(entry.getKey().toString());
            Object obj = entry.getValue();
            if (obj!=null){
                buff.append(elementSeparator);
                if ( obj instanceof DynamideObject ) {
                    if (dumphtml) {
                        sd = ((DynamideObject)obj).dumpHTML();
                    } else {
                        sd = ((DynamideObject)obj).dump();
                    }
                } else {
                    sd = obj.toString();
                }
                buff.append(sd);
            } else {
                buff.append(elementSeparator);
                buff.append(objectPlaceholder);
            }
            if (it.hasNext()) {
                buff.append(lineSeparator);
            }
        }
        buff.append(end);
        return buff.toString();
    }

}
