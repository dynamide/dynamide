/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jdom.output.XMLOutputter;

import com.dynamide.util.FileTools;

public class JDOMTest {
    public static void main (String[] args){
        try {
            JDOMFile jdom = new JDOMFile(null);
            String source = FileTools.readFile(args[0]);
            jdom.readFromString(source);
            org.jdom.Element root = jdom.getRootElement();
            XMLOutputter xmloutputter = new XMLOutputter();
            xmloutputter.setFormat(xmloutputter.getFormat().setExpandEmptyElements(true));
            ByteArrayOutputStream bos =  new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bos);
            xmloutputter.output(root, out);
            //out.close();
            out.flush();
            String res = bos.toString();
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
            } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  JDOMFile "+e);}
            out.close();
            System.out.println("root: ============\r\n"+res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
