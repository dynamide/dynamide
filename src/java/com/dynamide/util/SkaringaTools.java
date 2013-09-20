package com.dynamide.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.skaringa.javaxml.ObjectTransformer;
import com.skaringa.javaxml.ObjectTransformerFactory;

//skaringa

public class SkaringaTools {

    public static Object xmlFileToObject(String filename)
    throws Exception {
        ObjectTransformer trans = ObjectTransformerFactory.getInstance().getImplementation();
        //System.out.println("transformer::::::::"+trans.getClass().getName());
        trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        //not supported: trans.setProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
        Object result = trans.deserialize(new StreamSource(new File(filename)));
        return result;
    }

    public static Object xmlToObject(String source)
    throws Exception {
        //Log.debug(SkaringaTools.class, "SkaringaTools classloader >>>>>>>>>>>>>>>>>> "+SkaringaTools.class.getClassLoader().getClass().getName());
        ObjectTransformer trans = ObjectTransformerFactory.getInstance().getImplementation();
        //Log.debug(SkaringaTools.class, "transformer::::::::"+trans.getClass().getName());
        trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        //not supported: trans.setProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
        StringReader reader = new StringReader(source);
        Object result = trans.deserialize(new StreamSource(reader));
        return result;
    }


    public static String objectToXML(Object source) throws Exception {
        ObjectTransformer trans = ObjectTransformerFactory.getInstance().getImplementation();
        trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        //not supported: trans.setProperty(javax.xml.transform.OutputKeys.METHOD, "xml");

        // Serialize the object into a String.
        StringWriter writer = new StringWriter();
        trans.serialize(source, new StreamResult(writer));
        return writer.toString();
    }


    public static String objectToXMLFile(Object source, String filename) throws Exception {
        ObjectTransformer trans = ObjectTransformerFactory.getInstance().getImplementation();
        trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        //not supported: trans.setProperty(javax.xml.transform.OutputKeys.METHOD, "xml");

        // Serialize the object into a file.
        File file = new File(filename);
        trans.serialize(source, new StreamResult(file));
        return file.getCanonicalPath();
    }

    public static String writeSchema(Class sourceClass, String schemaFilename) throws Exception {
        ObjectTransformer trans = ObjectTransformerFactory.getInstance().getImplementation();
        trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        //not supported: trans.setProperty(javax.xml.transform.OutputKeys.METHOD, "xml");

        // Create an XML schema file that describes the class.
        File schemaFile = new File(schemaFilename);
        trans.writeXMLSchema(sourceClass, new StreamResult(schemaFile));
        return schemaFile.getCanonicalPath();
    }

}
