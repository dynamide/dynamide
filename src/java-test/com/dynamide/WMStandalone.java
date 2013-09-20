package com.dynamide;

/*
 * Copyright (c) 1998, 1999, 2000 Semiotek Inc. All Rights Reserved.
 *
 * This software is the confidential intellectual property of
 * of Semiotek Inc.; it is copyrighted and licensed, not sold.
 * You may use it under the terms of the GNU General Public License,
 * version 2, as published by the Free Software Foundation. If you
 * do not want to use the GPL, you may still use the software after
 * purchasing a proprietary developers license from Semiotek Inc.
 *
 * This software is provided "as is", with NO WARRANTY, not even the
 * implied warranties of fitness to purpose, or merchantability. You
 * assume all risks and liabilities associated with its use.
 *
 * See the attached License.html file for details, or contact us
 * by e-mail at info@semiotek.com to get a copy.
 */


import org.webmacro.*;
import org.webmacro.servlet.WebContext;
import java.util.Date;
import java.io.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
  * This example demonstrates using WebMacro in "standalone" mode. Instead
  * of subclassing from WMServlet you create and maintain your own WebMacro
  * object, and you are free to subclass from another servlet. Also, this
  * technique can be used outside the servlet context.
  * <p>
  * The WebMacro master object is initialized when the servlet is initialized
  * and destroyed when the servlet is destroyed. There is some overhead
  * involved in creating the interface so you should prefer not to create one
  * on every request.
  * <p>
  * This servlet can be compiled and installed as an ordinary servlet. You
  * need to ensure that your WebMacro.defaults (and possible WebMacro.properties)
  * file is properly configured and available on your CLASSPATH.
  * <p>
  * When setting up WebMacro.properties make sure that the TemplatePath is
  * correctly set and that the template used by this servlet, "standalone.wm",
  * is available on that path.
  * <p>
  * If you do not create a custom WebMacro.properties, WebMacro will attempt
  * to load templates from the CLASSPATH.
  */
public class WMStandalone
{

   /**
     * This is the core WebMacro interface which we use to create Contexts,
     * load Templates, and begin other WebMacro operations.
     */
   private static org.webmacro.WebMacro _wm = null;

   /**
     * The init() method will be called by your servlet runner whenever
     * it wants to instantiate a new copy of your servlet. We initialize
     * WebMacro here so as to avoid the expense of initializing it on
     * every request.
     */
   public WMStandalone()
   throws org.webmacro.InitException {
     if (_wm == null) {
        //_wm = new WM();
        _wm = new org.webmacro.WM("com/dynamide/conf/WebMacroDynamide.properties"); //%% todo -- figure out how to move to RESOURCE_ROOT/conf
     }
   }

   /**
     * It's not strictly necessary to destroy the WebMacro object when
     * you're done with it--garbage collection will eventually get it--but
     * it makes sure the resources get freed. You really ought to do this,
     * since it might be a lot of resources.
     */
   public void finalize()
   throws Exception {
       //_wm = null;
   }


   /**
     * We only implement the GET method in this servlet.
     * <p>
     * We create a WebContext object, populate it with some data, select
     * a template, and write the output of that template to the output
     * stream. Note that you need to pass the WebContext object to the
     * template in order to write it, you also need an output stream, in
     * this case the HttpServletResponse output stream is used.
     * <p>
     * If you were using WebMacro outside a servlet context you would not
     * be able to construct a WebContext object, since you would not have
     * the HttpServletRequest and HttpServletResponse objects which you
     * need in order to do that. WebContext is a subclass of Context, so
     * outside of a servlet just construct a Context object. You will
     * obviously lose the ability to talk about servlet specific things
     * in your templates (such as "Cookies") but otherwise it's the same.
     * <p>
     * There are a few exceptions you have to deal with. First, WebMacro
     * may not be able to locate the template you've requested in which
     * case it'll throw a NotFoundException. Second, the template will
     * expect to find certain information in the Context, and if you fail
     * to provide that information a ContextException will be thrown. Aside
     * than WebMacro, in a servlet environment you also have to deal with
     * the IOException that can be thrown if something goes wrong with
     * the network connection back to the client.
     */
   public void doOne(int i, String big) {

      try {
         try {
            // create a context for the current request
            Context c = _wm.getContext();

            // fill up the context with our data
            c.put("Today", new Date());
            c.put("Number", new Long(i));
            c.put("big"+i, big+i);

            // get the template we intend to execute
            Template t = _wm.getTemplate("standalone.wm");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // write the template to the output, using our context
            t.write(bos, "ISO-8859-1", c);
            bos.toString();
            //System.out.println("=================================\r\n"+bos.toString());
            System.out.print(".");
            bos.reset();
            bos = null;
            c.clear();
         } catch (org.webmacro.ResourceException e) {
             System.out.println("ERROR!  Could not locate template standalone.wm, check that your template path is set properly in WebMacro.properties"+e);
         } catch (org.webmacro.ContextException e) {
             System.out.println("ERROR!  Could not locate required data in the Context."+e);
         }
      } catch (java.io.IOException e) {
          // what else can we do?
          System.out.println("ERROR: IOException while writing to servlet output stream."+e);
      }
   }

   private String doOneDM(int i, String big, String templateText)
   throws Exception {
        System.out.print(".");
        String logName = "WMStandalone-test";
        Context c = _wm.getContext();
        org.webmacro.Broker broker = _wm.getBroker();
        org.webmacro.Template t = new org.webmacro.engine.StringTemplate(broker, templateText, logName);
        FastWriter fw = new FastWriter(broker, "US-ASCII");

        //first, just do a syntax check.  This is done more elaborately by the Session class.
        StringReader sr = new StringReader(templateText);
        try {
            org.webmacro.engine.Parser parser = (org.webmacro.engine.Parser) broker.get("parser", "wm");
            //another, simpler way to do it: parser.parseBlock("template", sr);
            parser.parseBlock(logName+".templateText", sr);
        } finally {
            sr.close();
            sr = null;
        }

        com.dynamide.util.WEBMACRO_CONTEXTPointer p = new com.dynamide.util.WEBMACRO_CONTEXTPointer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            p.setContext(c);
            c.put("WEBMACRO_CONTEXT", p);
            c.put("WEBMACRO", _wm);
            c.put("big", big);
            t.write(bos, "US-ASCII", c);//wm2.01b
        } finally {
            c.remove("WEBMACRO_CONTEXT");
            c.remove("WEBMACRO");
            c.remove("big");
            p.setContext(null);
        }
        String tmpl = bos.toString();//wm2.01b
        return tmpl;
   }



   public void loop(String big)
   throws Exception {
            String tmpl = "big: $big    .";
            for (int j=0; j < 20; j++) {
                //doOne(j, big);
                doOneDM(j, big, tmpl);
            }
            System.out.println("loop ");
   }

   public static void main(String[]args)
   throws Exception {
        System.gc();
        System.gc();
        System.gc();
        StringBuffer big = new StringBuffer();
        String pad = "aaaaaaaaaa";
        for (int k = 0; k< 10000; k++) big.append(pad);
        String bigs = big.toString();

        (new WMStandalone()).loop(bigs);
        System.out.println("done 1. Press any key to continue: ");
        System.in.read();
        System.in.read();
        int MAXOUTER=10;
        int MAX = 20;
        for (int p=0; p < MAXOUTER; p++) {
            for (int i=0; i < MAX; i++) {
                (new WMStandalone()).loop(bigs);
                System.gc();
            }
            (new WMStandalone()).loop(bigs);
            System.gc();
            System.gc();
            Thread.yield();
            System.out.println("done "+MAX+"");
        }
        System.gc();
        System.gc();
        System.out.println("done "+(MAXOUTER*MAX)+". Press any key to continue: ");
        System.in.read();
        System.in.read();
   }
}