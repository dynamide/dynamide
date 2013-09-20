/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import org.jdom.input.SAXBuilder;

public class Driver {
    private static final String DEFAULT_SAX_DRIVER_CLASS =
        "org.apache.xerces.parsers.SAXParser";
    private SAXBuilder builder;
    public Driver(String saxDriverClass) {
        builder = new SAXBuilder(saxDriverClass);
    }

    private Session session = null;
    public Session getSession(){return session;}
    public void setSession(Session new_value){session = new_value;}

    public Document read(String filename)
    throws IOException, JDOMException {
        // Build the JDOM Document
        return builder.build(new File(filename));
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 2) {
            out.println("Usage: com.dynamide.Driver <myfile.xml> [sax-driver-class]");
            System.exit(1);
            return;
        }
        // Load filename and SAX driver class
        String filename = args[0];
        String saxDriverClass = DEFAULT_SAX_DRIVER_CLASS;
        if (args.length == 3) {
            saxDriverClass = args[2];
        }
        try {
            Driver reader = new Driver(saxDriverClass);
            Document doc = reader.read(filename);
            Session session = Session.createSession("/dynamide/demo");
            reader.setSession(session);
            XMLWidgetOutputter outputter = new XMLWidgetOutputter();
            outputter.setExpandEmptyElements(true);
            outputter.setSession(session);
            Element start = doc.getRootElement().getChild("html");
            outputter.output(start, System.out);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
