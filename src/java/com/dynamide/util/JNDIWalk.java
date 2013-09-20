package com.dynamide.util;

import java.io.StringWriter;
import java.io.Writer;

import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;

import javax.servlet.ServletContext;

public class JNDIWalk {

      public static void walk(Object c, String indent, String indentIncr, Writer out){
        try {
            if (c instanceof Context){
                Enumeration enumObj = ((Context)c).listBindings("");
                while (enumObj.hasMoreElements()){
                    Object elem = enumObj.nextElement();
                    if (enumObj instanceof Binding){
                        Binding b = (Binding)enumObj;
                        if (b!=null) {
                            out.write("\r\n");
                            out.write(indent+"name: "+b.getName()+"\r\n");
                            out.write(indent+"class: "+b.getClassName()+"\r\n");
                            Object o = b.getObject();
                            if (o!=null){
                                out.write(indent+"object: "+o.toString()+"\r\n");
                                walk(o, indent+indentIncr, indentIncr, out);
                            }
                        }
                    } else {
                        out.write(indent+"bound: "+enumObj.getClass().getName()+" toString: "+enumObj.toString()+"\r\n");
                    }
                }
            } else {
                out.write("Object not a Context Object\r\n");
            }
        } catch (Exception e){
            e.printStackTrace();
            try {out.write("ERROR: [80] [walk] "+e+"\r\n");} catch (Exception e2){System.out.println("ERROR: no out writer");}
        }
     }

    public static String dump(ServletContext context){
        Writer out = new StringWriter();
        dump(context, out);
        return out.toString();
    }

    public static void dump(ServletContext context, Writer out){
        try {
            out.write("<h2>Servlet Context</h2>"+"\r\n");
            out.write("<table border='1' width='500'>"+"\r\n");
            Enumeration atts = context.getAttributeNames();
            while (atts.hasMoreElements()){
                String s = (String)atts.nextElement();
                Object o = context.getAttribute(s);
                if (o!=null) {
                    out.write("<tr><td>"+s + "</td><td>" + o.getClass().getName()+"</td><td style='font-size: 8pt;'>"+o.toString()+"</td></tr>"+"\r\n");
                }
            }
            out.write("</table>"+"\r\n");

            /* resin only:*/
            Object ccp = context.getAttribute("caucho.class.path");
            if(ccp!=null){
                out.write("<h2>caucho.class.path</h2>"+"\r\n");
                out.write("<pre style='font-size: 8pt;'>"+"\r\n");
                StringTokenizer st = new StringTokenizer((String)ccp, ";");
                while (st.hasMoreTokens()) {
                    out.write("<br />"+st.nextToken()+"\r\n");
                }
                out.write("</pre>"+"\r\n");
            }


            InitialContext ictx = new InitialContext();
            //Enumeration enumObj = ictx.list("java:comp");
            out.write("<h2>JNDI Context</h2>"+"\r\n");
            out.write("<pre style='font-size: 8pt;'>"+"\r\n");
            walk(ictx, "     ", "     ", out);
            out.write("</pre>"+"\r\n");

            out.write("<hr />"+"\r\n");
        } catch (Exception e) {
           try{ out.write("ERROR: [81] "+e); } catch (Exception bige) {System.out.println("ERROR: no writer.  Nested exception: "+e);}
        }
    }

    public static void main(String [] args)
    throws Exception {
        InitialContext ictx = new InitialContext();
        StringWriter sw = new StringWriter();
        walk(ictx, "     ", "     ", sw);
        System.out.println("walk: " + sw.toString());
    }
}
