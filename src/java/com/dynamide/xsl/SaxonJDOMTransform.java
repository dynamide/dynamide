package com.dynamide.xsl;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import com.dynamide.Session;
import com.dynamide.util.Opts;
import com.icl.saxon.Controller;
import com.icl.saxon.ExtendedInputSource;
import com.icl.saxon.ParameterSet;
import com.icl.saxon.TransformerFactoryImpl;
import com.icl.saxon.jdom.DocumentWrapper;
import com.icl.saxon.om.NamePool;

/** I can't get the Saxon JDOM DocumentWrapper to work as advertised, for for now this class
 *  simply accepts the source and style filenames, not the JDOM node.
 */
public class SaxonJDOMTransform /*implements javax.xml.transform.URIResolver*/ {

    public boolean DEBUG = false;


  /* requires java 1.4, java.net.URI won't build with java 1.3
   *public Source resolve(String href, String base)
    throws TransformerException {
        System.out.println("resolve: href: "+href);
        System.out.println("resolve: base: "+base);

        //first, try to see if the base is a full path:
        try {
            File b = new File(new java.net.URI(base));   // %% Java 1.4 only %%
            String parentdir = b.getParentFile().getCanonicalPath();
            File s = new File(parentdir, href);
            if ( s.exists() ) {
                if (DEBUG) System.out.println("resolved as file relative to parent: "+parentdir+" file: "+href);
                return new StreamSource(new File(parentdir, href));
            }
        } catch( Exception e ){
            //System.out.println("error getting b: "+e);
        }

        //next, try to see if the href exists in the current dir:
        try {
            File c = new File(href);
            if ( c.exists() ) {
                if (DEBUG) System.out.println("resolved base as file: "+c.getCanonicalPath());
                return new StreamSource(c);
            }
        } catch( Exception e2 ){
            //System.out.println("error resolving href as standalone URI: "+e2);
        }

        //next, try to see if the href exists as a URL:
        try {
            if (DEBUG) System.out.println("resolved href as a remote URL: "+href);
            return new StreamSource(href);
        } catch( Exception e3 ){
            System.out.println("error resolving href as standalone URL: "+e3);
        }

        throw new TransformerException("ERROR: SaxonJDOMTransform.transform(): can't resolve href: "+href +" base: "+base);
    } */

    /**
     * transform, given only filenames. Defaults a null session, and to NOT using the Dynamide controller and NOT using JDOM.
     */
    public static String transform(String sourceFileName,
                                   String styleFileName)
    throws TransformerException,
           TransformerConfigurationException,
           IOException,
           org.jdom.JDOMException {                     //if you change these defaults, change the javadoc above.
        return transform(sourceFileName, styleFileName, null, false, false);
    }

    /**
     * transform, given only filenames. Defaults to using the Dynamide controller, and NOT using JDOM.
     */
    public static String transform(String sourceFileName,
                                   String styleFileName,
                                   Session session)
    throws TransformerException,
           TransformerConfigurationException,
           IOException,
           org.jdom.JDOMException {                      //if you change these defaults, change the javadoc above.
        return transform(sourceFileName, styleFileName, session, true, false);
    }

    public static String transform(String sourceFileName,
                                   String styleFileName,
                                   Session session,
                                   boolean useDynamideController,
                                   boolean useJDOM)
    throws TransformerException,
           TransformerConfigurationException,
           IOException,
           org.jdom.JDOMException
     {
        //com.dynamide.util.Log.debug(SaxonJDOMTransform.class, "SaxonJDOMTransform.transform(\""+sourceFileName+"\", \""+styleFileName+"\", \""+session+"\"), "+useDynamideController+", "+useJDOM);
        ParameterSet params = new ParameterSet();
        TransformerFactoryImpl factory = new TransformerFactoryImpl();
        //can't do this in java 1.3: factory.setURIResolver(this);

        File sheetFile = new File(styleFileName);
        ExtendedInputSource eisStyle = new ExtendedInputSource(sheetFile);
        Source styleSource = new SAXSource(factory.getStyleParser(), eisStyle);

        Templates sheet = factory.newTemplates(styleSource);

        java.io.StringWriter out = new java.io.StringWriter();  //Could also be a ByteArrayOutputStream
        Result result = new StreamResult(out);

        Transformer instance;

        if (useDynamideController){
            //BEGIN-SKIP-FACTORY
                //  Normally, you simply call this next line to create the correct controller,
                //   which implements Transformer:
                //Transformer instance = sheet.newTransformer();
                //  Here is what gets called when the above line would execute
                //public Transformer newTransformer() {
                //    Controller c = new Controller(factory);
                //    c.setPreparedStyleSheet(this);
                //    return c;
                //}
                //   So instead, I create one directly, and replace with my own class:
                instance = new com.dynamide.xsl.DynamideSaxonController(factory);
                ((Controller)instance).setPreparedStyleSheet((com.icl.saxon.PreparedStyleSheet)sheet);
                ((Controller)instance).setParams(params);

                DynamideSaxonController dmController = ((DynamideSaxonController)instance);
                dmController.setRootWriter(out);
                dmController.setSession(session);
                ((com.dynamide.xsl.DynamideSaxonController)instance).setRootWriter(out);
            //END-SKIP-FACTORY
        } else {
                //Do it the normal Saxon way:
                instance = sheet.newTransformer();
        }

        if (useJDOM){
            //This line is critical, but it means we won't be using the dynamide hack in a nested call.
            System.setProperty("javax.xml.transform.TransformerFactory",
                               "com.icl.saxon.TransformerFactoryImpl");
            org.jdom.Document doc = (new com.dynamide.JDOMFile(null, sourceFileName)).getDocument();
            DocumentWrapper docw = new DocumentWrapper(doc, sourceFileName);
            NamePool pool = NamePool.getDefaultNamePool();
            docw.setNamePool(pool);

            instance.transform(docw, result);
        } else {
            File sourceFile = new File(sourceFileName);
            ExtendedInputSource eis = new ExtendedInputSource(sourceFile);
            Source sourceInput = new SAXSource(factory.getSourceParser(), eis);
            eis.setEstimatedLength((int)sourceFile.length());

            instance.transform(sourceInput, result);
        }
        return out.toString();
    }

    public static java.io.StringWriter g_out = null;

    /**
     * Method main
     */
    public static void main(String argv[])
    throws Exception {
        String sourceFileName = "C:\\temp\\glossary.xml";   //test file, in main()
        String styleFileName = "C:\\temp\\glossary.xsl";    //test file, in main()
        //String outputFileName = "C:\\temp\\glossary.html";

        //String res = (new SaxonJDOMTransform()).transform(sourceFileName, styleFileName);
        //System.out.println(res);
        //if (true)
        //    return;
        if (argv.length < 2) {
            System.out.println("Usage: SaxonJDOMTransform source.xml style.xsl >out.xml");
        } else {
            Session session = null;
            Opts opts = new Opts(argv);

            if (opts.getOptionBool("session")){
                //get the options, provide local defaults for my laptop.
                String RESOURCE_ROOT = opts.getOption("-root", "C:/dynamide/build/ROOT");
                String urlpath = opts.getOption("-url", "/dynamide/doco");
                System.out.println("Using RESOURCE_ROOT: "+RESOURCE_ROOT+"url: "+ urlpath);
                com.dynamide.resource.ResourceManager.createStandalone(RESOURCE_ROOT);  //creates a global singleton.
                session = Session.createSession(urlpath);
            }

            String xml = opts.getOption("xml");
            String xsl = opts.getOption("xsl");
            boolean hack = opts.getOptionBool("hack");
            boolean jdom = opts.getOptionBool("jdom");
            if (xml.length()==0 || xsl.length()==0){
                System.out.println(" ");
                System.out.println("Usage: java com.dynamide.xsl.SaxonJDOMTransform [options]");
                System.out.println(" ");
                System.out.println(" Required Options:");
                System.out.println("  -xml <xml>           Source document filename");
                System.out.println("  -xsl <stylesheet>    Style document filename");
                System.out.println(" ");
                System.out.println(" Optional:");
                System.out.println("   -o   <output>        Send to file, not stdout");
                System.out.println("   -session             Create a dynamide Session");
                System.out.println("   -hack                use Dynamide hack for extension elements");
                System.out.println("   -jdom                Use JDOM Object Model");
                //System.out.println(opts.dump());
                System.exit(1);
            }
            String output = opts.getOption("o");
            String res;
            if (session != null){
                res = SaxonJDOMTransform.transform(xml, xsl, session, hack, jdom);
            } else {
                res = SaxonJDOMTransform.transform(xml, xsl);
            }
            if (output.length()>0){
               com.dynamide.util.FileTools.saveFile("", output, res);
            } else {
                System.out.println(res);
            }
        }
        System.exit(0);
    }



}
