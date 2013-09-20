/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;
//sample usage:
//java com.dynamide.JDOMFile E:\wwwroot\Dynamide\demo\application.xml

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.dom4j.DocumentHelper;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.dynamide.resource.ContextNode;
import com.dynamide.util.FileTools;
import com.dynamide.util.Log;
import com.dynamide.util.Opts;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class JDOMFile extends ContextNode implements org.xml.sax.EntityResolver {

    public JDOMFile(){
        this(null);
    }

    public JDOMFile(DynamideObject owner){
        super(owner);
        profileEnter("JDOMFile(DynamideObject)");

        //no-arg constructor for subclasses,
        //or for when you will initialize with readFromString.
        this.m_saxDriverClassname = DEFAULT_SAX_DRIVER_CLASS;
        m_builder = new SAXBuilder(m_saxDriverClassname);
        m_builder.setValidation(false);
        m_builder.setExpandEntities(true); //new 8/19/2002 5:43PM
        m_builder.setEntityResolver(this); //new 8/19/2002 5:43PM
        //System.out.println("JDOMFile A");
        profileLeave("JDOMFile(DynamideObject)");
    }

    public JDOMFile(DynamideObject owner, String filename) throws IOException, JDOMException {
        this(owner, filename, DEFAULT_SAX_DRIVER_CLASS);
    }

    public JDOMFile(DynamideObject owner, String filename, String saxDriverClassname) throws IOException, JDOMException {
        super(owner);
        profileEnter("JDOMFile(DynamideObject,String,String)");
        this.m_saxDriverClassname = saxDriverClassname;
        m_builder = new SAXBuilder(saxDriverClassname);
        m_builder.setValidation(false);
        m_builder.setExpandEntities(true);  //new 8/19/2002 5:43PM
        //3/12/2003 6:38PM m_builder.setExpandEntities(false);  //reset 3/12/2003 6:39PM
        m_builder.setEntityResolver(this); //new 8/19/2002 5:43PM
        //System.out.println("JDOMFile B");
        File file = new File(filename);
        if (!file.exists()){
            throw new FileNotFoundException(filename);
        }
        read(filename);
        profileLeave("JDOMFile(DynamideObject,String,String)");
    }

    /** Default SAX Driver to use */
    private static final String DEFAULT_SAX_DRIVER_CLASS =
        //"org.apache.xerces.parsers.SAXParser";
                "com.dynamide.util.DynamideSAXParser";

    private String m_saxDriverClassname;

    private SAXBuilder m_builder;

    public org.xml.sax.InputSource resolveEntity(java.lang.String publicId, java.lang.String systemId)
    throws org.xml.sax.SAXException, java.io.IOException {
        System.out.println("publicId: "+publicId+" systemId: "+systemId);
        return new org.xml.sax.InputSource(new StringBufferInputStream(publicId));
    }

    private String m_filename = "";
    public String getFilename(){
        return m_filename;
    }
    /** Warning: most operations will not heed this, since the API has filename parameters when needed.
     *  Setting the filename then saving it, will work, however.
     */
    public void setFilename(String newName){
        m_filename = newName;
    }

    private Document m_doc;
    public Document getDocument(){return m_doc;}

    public Element getRootElement(){
        if (m_doc == null){
            return null;
        }
        return m_doc.getRootElement();
    }


    public synchronized Document read(String filename) throws JDOMException, IOException {
        profileEnter("JDOMFile.read");
        m_filename = filename;
        // Build the JDOM Document
        try {
            m_builder.setEntityResolver(this); //new 8/19/2002 5:43PM
            m_doc = m_builder.build(new File(filename));
        } catch (JDOMException e2){
            System.out.println("JDOMFile exception: "+Tools.errorToString(e2, true)+"\r\n::filename was: "+filename);
            throw e2;
        }
        profileLeave("JDOMFile.read");
        return m_doc;
    }

    public synchronized Document readFromString(String source) throws IOException, JDOMException {
        // Build the JDOM Document
        //m_builder.setExpandEntities(false); //as of jdom beta 7, this method is valid.
        profileEnter("JDOMFile.readFromString");
        m_builder.setValidation(false); //has no effect, I think.
        m_builder.setEntityResolver(this); //new 8/19/2002 5:43PM
        m_doc = m_builder.build(new StringReader(source));
        profileLeave("JDOMFile.readFromString");
        return m_doc;
    }

    public void reload()
    throws IOException, JDOMException {
        read(m_filename);
    }

    public synchronized void output(PrintStream out)
    throws IOException, JDOMException {
        output(m_doc.getRootElement(), out);
    }

    public static void output(Element root, PrintStream out)
    throws IOException, JDOMException {
        //XMLOutputter outputter = new XMLOutputter();
        //outputter.setExpandEmptyElements(true);
        XMLOutputter outputter = createJDomXMLOutputter(false);
        //xmloutputter.setExpandEntities(false);
        outputter.output(root, out);
    }

    /** This overload returns a String and requires no output stream.*/
    public static String output(Element start){
        return output(start, false);
    }

    public static String output(Element start, boolean hideException){
        try {
            if ( start == null ) {
                return "";
            }
            ByteArrayOutputStream bos =  new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bos);
            output(start, out);
            //out.close();
            out.flush();
            String res = bos.toString();
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
            } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  JDOMFile "+e);}
            out.close();
            return res;
        } catch (Exception e) {
            if (hideException){
                Log.error(JDOMFile.class, "JDOMFile.output caught an error, but hiding result since hideException was true", e);
                return "";
            }
            Log.error(JDOMFile.class, "JDOMFile.output caught an error", e);
            return "ERROR: [40] "+e;
        }
    }

    public String output(){
        return output(getRootElement());
    }
    
    public static XMLOutputter createJDomXMLOutputter(boolean expandEmptyElements){
        //was: public XMLOutputter(String indent, boolean newlines) 
        //XMLOutputter xmloutputter = new XMLOutputter("  ", true);
        //xmloutputter.setExpandEmptyElements(true); //new 12/27/2001
        //xmloutputter.setOmitEncoding(true);//new 12/27/2001
        //Now: 
        org.jdom.output.Format f = org.jdom.output.Format.getPrettyFormat();
        f.setExpandEmptyElements(expandEmptyElements);
        f.setOmitEncoding(true);
        XMLOutputter xmloutputter = new XMLOutputter(f);
        return xmloutputter;                
    }


    /** Return just the un-rendered full xml source.*/
    public String getFullXMLSource(){
        try {
            XMLOutputter xmloutputter = createJDomXMLOutputter(false);
            Document start = getRootElement().getDocument();//new 12/27/2001, used to be just Element root.
            ByteArrayOutputStream bos =  new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bos);
            xmloutputter.output(start, out);
            //out.close();
            out.flush();
            String res = bos.toString();
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
            } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  JDOMFile "+e);}
            out.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR in Persistent.getFullXMLSource";
    }

    public static String hideEntities(String html){
        html = StringTools.searchAndReplaceAll(html, "&#", "AmPeRnUm");  //these literally cannot be measured in milliseconds.
        html = StringTools.searchAndReplaceAll(html, "&", "AmPeRNoNUM");
        return html;
    }

    public static String unHideEntities(String html){
        html = StringTools.searchAndReplaceAll(html, "AmPeRNoNUM", "&");
        html = StringTools.searchAndReplaceAll(html, "AmPeRnUm", "&#");
        html = StringTools.searchAndReplaceAll(html, "&apos;", "'");
        return html;
    }

    public static boolean isValidXHTMLFragment(String src){
        try {
            prettyPrintHTML("<fragment>"+src+"</fragment>", "true", "true", true, false, true, false);
            return true;
        } catch (XMLFormatException e)  {
            return false;
        }
    }

    public static boolean isValidXHTML(String src){
        try {
            prettyPrintHTML(src, "true", "true", true, false, true, false);
            return true;
        } catch (XMLFormatException e)  {
            return false;
        }
    }

    public static String prettyPrintHTMLSafe(String src){
        try {
            return prettyPrintHTML(src, "true", "true", true, false, true, false);
        } catch (XMLFormatException e)  {
            return src;
        }
    }

    public static String prettyPrintHTML(String html)
    throws XMLFormatException {
        return prettyPrintHTML(html, "true", "true", true, false, true, false);
    }

    public static String prettyPrintHTML(String html, String newlines, String trim, boolean xhtml, boolean expandEmpty, boolean indent, boolean hideErrrors)
    throws XMLFormatException {
        try {
            com.dynamide.util.Profiler profiler;
            if ( com.dynamide.Constants.PROFILE) {
                profiler = com.dynamide.util.Profiler.getSharedProfiler();
                profiler.enter("JDOMFile.prettyPrintHTML");
            }
            if (html == null){
                throw new XMLFormatException("null passed to JDOMFile.prettyPrintHTML(...)");
            }
            boolean debugTransformations = false; //Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
            if (debugTransformations){
                Log.debug(JDOMFile.class, "JDOMFile.prettyPrintHTML before:\r\n-----------------\r\n"+html+"\r\n-----------------");
            }

            //System.out.println("prettyPrintHTML");
            html = hideEntities(html);
            //return org.dom4j.io.HTMLWriter.prettyPrintHTML(html);
            StringWriter swriter = new StringWriter();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setNewlines(Tools.isTrue(newlines));
            format.setTrimText(Tools.isTrue(trim));
            format.setIndent(indent);
            format.setXHTML(xhtml);
            format.setExpandEmptyElements(expandEmpty);
            format.setLineSeparator(System.getProperty("line.separator")) ;
            HTMLWriter writer = new HTMLWriter(swriter, format);

            Set current = writer.getPreformattedTags();
            current.add("NOBR");
            writer.setPreformattedTags(current);

            //Specifically remove <P> from the set, so that <P/> becomes <P></P>, since otherwise
            // IE breaks when saving html files.   [6/1/2002 1:03AM]
            Set closeSet = writer.getOmitElementCloseSet();
            closeSet.remove("P");
            writer.setOmitElementCloseSet(closeSet);

            org.dom4j.Document document = DocumentHelper.parseText(html);
            writer.write(document);
            writer.flush();
            String prettyHTML = swriter.toString();
            //System.out.println("======================== before unHideEntities ==================\r\n"+prettyHTML);
            prettyHTML = unHideEntities(prettyHTML);
            //System.out.println("======================== after unHideEntities ==================\r\n"+prettyHTML);

            if (debugTransformations){
                Log.debug(JDOMFile.class, "JDOMFile.prettyPrintHTML after:\r\n-----------------\r\n"+prettyHTML+"\r\n-----------------");
            }

            if (com.dynamide.Constants.PROFILE) profiler.leave("JDOMFile.prettyPrintHTML");
            return prettyHTML;
        } catch (Exception e){
            if (hideErrrors){
                Log.error(JDOMFile.class, "prettyPrintHTMLSafe hiding error by returning src only."
                                          +"partial src: \r\n=======\r\n"
                                          +StringTools.ellipses(html, 300)
                                          //src: \r\n=======\r\n"
                                          //+html+"\r\n======"
                                          , e);
                return html;
            } else {
                String page = "<font color='red'><b>ERROR [prettyPrintHTML]: "
                                +StringTools.escape(""+e)+"</b></font><br/><hr/>Source:<hr/>"
                                +"<pre>"+StringTools.makeLineNumbers(StringTools.escape(html))+"</pre>";
                throw new XMLFormatException(page);
            }
        }
    }

    /** Called when persisitence is going to happen: gives subclasses an opportunity to
     *  sync the DOM with memory.
     */
    public void commitInMemoryChangesToDOM() throws Exception {
    }

    public boolean saveToFile(){
        return saveToFile(m_filename, true);
    }

    public boolean saveToFile(String filename, boolean backup){
        try {
            if ( backup ) {
                FileTools.backup(filename);
            }
            commitInMemoryChangesToDOM();
            String source = getFullXMLSource();
            //source = TidyWrapper.tidy(source).result;
            //Even in jdom beta 7 they haven't fixed this: they output source
            // with 0D 0A 0A  and also solitary 0A chars in the stream.  So I replace them
            //with the following scheme.  Don't worry about wasted cycles:
            //  on my PIII 850MHz, all three replacements took .9 milliseconds (as timed in a loop of 100).
            source = StringTools.searchAndReplaceAll(source, "\r\n\n", "\n");
            source = StringTools.searchAndReplaceAll(source, "\r\n", "\n");
            source = StringTools.searchAndReplaceAll(source, "\n", "\r\n");

            FileTools.saveFile("", filename, source);
            System.out.println("JDOMFile.saveToFile: "+filename);
            /*  %%%%% What a hack!
             *  This needs to use Assembly.saveFile(), which should be written so that there
             *  is no direct access of the filesystem.
             *  For now, this entry is made in Session.newPage()
             *
             *       Session myParent = findParentSession();
             *       if (myParent != null){
             *           Assembly assembly = myParent.getAssembly();
             *           System.out.println("findParentSession().getAssembly(): "+assembly);
             *           if ( assembly != null ) {
             *               assembly.updateFile(getFilename());
             *           }
             *       }
            */
            return true;
        } catch (Exception e){
            logError("ERROR in saveWithBackup: ", e);
            e.printStackTrace();
            return false;
        }
    }

    public Element findFirstElement(String findName) {
        return findFirstElement(getRootElement(), findName);
    }

    public static Element findFirstElement(Element current, String findName) {
        if ( current != null ) {
            synchronized (current){
                if ( current.getName().equals(findName) ) {
                    return current;
                }
                List children = current.getChildren();
                Iterator i = children.iterator();
                while (i.hasNext()) {
                    Element child = (Element) i.next();
                    Element res = findFirstElement(child, findName);
                    if ( res !=null ) {
                        return res;
                    }
                }
            }
        }
        return null;
    }

    public static Element findFirstElementWithAttribute(Element current, String findName,
                                      String attrName, String attrValue, boolean ignoreElementCase) {
        if ( current != null ) {
            synchronized (current){
                boolean nameEquals = ignoreElementCase
                ? current.getName().equalsIgnoreCase(findName)
                : current.getName().equals(findName);
                if ( nameEquals && getAttributeValue(current, attrName).equals(attrValue) ) {
                 return current;
                }
                List children = current.getChildren();
                Iterator i = children.iterator();
                while (i.hasNext()) {
                 Element child = (Element) i.next();
                 Element res = findFirstElementWithAttribute(child, findName, attrName, attrValue, ignoreElementCase);
                 if ( res !=null ) {
                     return res;
                 }
                }
            }
        }
        return null;
    }

    /** Safe to call: always returns a string, maybe empty, but never null.  If you want to know if
     *  an attribute is not present, use the normal jdom.Element.getAttributeValue()
     */
    public static String getAttributeValue(Element element, String attrName){
        if (element == null){
            return "";
        }
        String res = element.getAttributeValue(attrName);
        if ( res == null  ) {
            return "";
        }
        return res;
    }

    public static String safeGetElementChildText(Element element, String childName){
        Element child = element.getChild(childName);
        if ( child != null ) {
            String res = child.getText();
            return res != null ? res : "";
        }
        return "";
    }

    public void listNamedElement(Element root, PrintStream out, String elementName){
        List events = root.getChildren(elementName);
       // out.println("This file has "+ events.size() +"elements named "+ elementName);
        Iterator i = events.iterator();
        while (i.hasNext()) {
            Element event = (Element) i.next();
            out.print("\t" + event.getText());
        }
    }


    //=========== XPath stuff ==================================

    private StringBuffer errorLog = new StringBuffer();

    protected void addError(String error){
        errorLog.append(error);
        errorLog.append("\r\n");
        System.out.println("\r\nERROR in JDOMFile: "+error);
    }
    public void clearErrors(){
        errorLog.setLength(0);
    }
    public String getErrors(){
        return errorLog.toString();
    }

    public static XPath prepare(String xpathExpression) throws Exception {
        return new JDOMXPath(xpathExpression);
    }

    public List select(String xpathExpression){
        try {
            Element element = getRootElement();
            XPath xpath = new JDOMXPath(xpathExpression);
            return select(element, xpath);
        } catch (Exception e){
            addError("error: "+e+" while calling select for expression: "+xpathExpression);
        }
        return new Vector();
    }

    public List select(Element element, String xpathExpression){
        try {
            XPath xpath = new JDOMXPath(xpathExpression);
            return select(element, xpath);
        } catch (Exception e){
            addError("error: "+e+" while calling select for expression: "+xpathExpression);
        }
        return new Vector();
    }

    /** @see #prepare
     */
    public List select(Element element, XPath xpath){
        try {
            return xpath.selectNodes(element);
        } catch (Exception e){
            addError("error: [select(Element,XPath)]  "+e);
        }
        return new Vector();
    }


    public Element selectFirst(Element element, String xpathExpression){
        try {
            XPath xpath = new JDOMXPath(xpathExpression);
            return selectFirst(element, xpath);
        } catch (Exception e){
            addError("error: "+e+" while calling selectFirst using expression: "+xpathExpression);
        }
        return null;
    }

    /** @see #prepare
     */
    public Element selectFirst(Element element, XPath xpath){
        try {
            return (Element)xpath.selectSingleNode(element);
        } catch (Exception e){
            addError("error: [selectFirst(Element,XPath)]"+e);
        }
        return null;
    }

    public String valueOf(String xpathExpression){
        return valueOf(getRootElement(), xpathExpression);
    }

    public String valueOf(Element element, String xpathExpression){
        try {
            XPath xpath = (XPath)(new JDOMXPath(xpathExpression));
            return xpath.valueOf(element);
        } catch (Exception e){
            addError("error: "+e+" while getting valueOf for expression: "+xpathExpression);
        }
        return "";
    }


    //=========== main =========================================


    private static void printUsage(){
        System.out.println("Usage: com.dynamide.JDOMFile [options] <myfile.xml>");
        System.out.println("Options: \r\n"
                          +"    -expand \r\n"
                          +"    -indent \r\n"
                          +"    -newlines \r\n"
                          +"    -parser <parser>   (default is org.apache.xerces.parsers.SAXParser)\r\n"
                          +"    -pretty \r\n"
                          +"    -showctrl (show control characters on std out)\r\n"
                          +"    -showcrlf (show CR and LF characters [\\r and \\n] on std out)\r\n"
                          +"    -trim \r\n"
                          +"    -xhtml \r\n"
                          );
    }

    public static void main(String[] args) 
    throws Exception {

        //run like this :
        // set CLASSPATH=C:\bin\dom4j\dom4j-patched-1.3\build\classes;c:\java
        // java -classpath %classpath%   com.dynamide.JDOMFile -pretty -expand -xhtml C:\temp\xhtml.xml
        //
        //Optional parser string, using the default parser anyway:
        //  -parser org.apache.xerces.parsers.SAXParser


        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        Opts opts = new Opts(args);
        opts.addOption("-newlines", Boolean.class, false);
        opts.addOption("-xhtml", Boolean.class, false);
        opts.addOption("-expand", Boolean.class, false);
        opts.addOption("-indent", Boolean.class, false);
        opts.addOption("-quiet", Boolean.class, false);
        opts.addOption("-showcrlf", Boolean.class, false);
        opts.addOption("-showctrl", Boolean.class, false);
        opts.addOption("-trim", Boolean.class, false);
        opts.addOption("-pretty", Boolean.class, false);
        opts.addOption("-entity", Boolean.class, false);
        opts.addOption("-testPrettyPrint", Boolean.class, false);

        if ( opts.getOptionBool("-testPrettyPrint") ) {
            System.out.println(Tools.cleanAndReportMemory());
            testPrettyPrint(10);
            System.out.println(Tools.cleanAndReportMemory());
            testPrettyPrint(10);
            System.out.println(Tools.cleanAndReportMemory());
            testPrettyPrint(10);
            System.out.println(Tools.cleanAndReportMemory());
            System.exit(0);
        }
        
        //for optimizeit:
        if ( opts.getOptionBool("-testPrettyPrintOpt") ) {
            System.out.println("\r\n****************** Press Enter key to start test. **************");
            System.in.read();
            System.in.read();
            for (int i=0;i<20;i++){
                testPrettyPrint(10);
                System.out.println("\r\n****************** One test complete. Press Enter key. **************");
                System.in.read();
                System.in.read();
            }
            System.exit(0);
        }


        // Load filename and SAX driver class
        String filename = args[args.length-1];//args[0];
        if ( opts.getRemainingArgs().length == 0 ) {
            System.out.println(""+opts.dump());
            printUsage();
            System.exit(2);
        }
        String saxDriverClassname = DEFAULT_SAX_DRIVER_CLASS;
        if (opts.getOption("-parser").length()>0) {
            saxDriverClassname = opts.getOption("-parser");//args[1];
        }
        // Create an instance of the tester and test
        try {
            

            JDOMFile jdomfile = new JDOMFile(null, filename, saxDriverClassname);
            if ( opts.getOptionBool("-entity") ) {
                //html = StringTools.searchAndReplaceAll(html, "&#", "AmPeRnUm");  //these literally cannot be measured in milliseconds.
                System.out.println(Tools.showControlChars(jdomfile.getRootElement().getText(), true));
                return;
            }
            if ( opts.getOptionBool("-pretty") ) {
                String html = JDOMFile.output(jdomfile.getRootElement());
                String newlines = opts.getOptionBool("-newlines") ? "true" : "false";
                boolean trim = opts.getOptionBool("-trim");
                String strim = trim  ? "true" : "false";
                boolean showcrlf = opts.getOptionBool("-showcrlf");
                String resultStr = JDOMFile.prettyPrintHTML(html,
                                                            newlines,
                                                            strim,
                                                            opts.getOptionBool("-xhtml"),
                                                            opts.getOptionBool("-expand"),
                                                            opts.getOptionBool("-indent"),
                                                            false);
                if ( ! opts.getOptionBool("-quiet")){
                    if ( opts.getOptionBool("-showctrl")){
                        System.out.println(Tools.showControlChars(resultStr, showcrlf));
                    } else {
                        System.out.println(resultStr);
                    }

                }
                System.out.flush();
            } else {
                jdomfile.output(System.out);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void testPrettyPrint(int count)
    throws Exception {
        String src = "<html><body>"
        +"<b>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>2aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>3aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>4aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>5aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>6aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"<b>7aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b>"
        +"</body></html>";
        for (int i=0;i<count;i++){
            prettyPrintHTML(src,    //String html
                                    "true",   //String newlines -- Netscape can't handle superlong lines.
                                    "true",    //String trim    -- for normal requests, save lots of space. (view source in ide uses different options.
                                    true,      //boolean xhtml -- don't break DOM parser in IDE.
                                    true,      //boolean expandEmpty -- don't break Netscape with <script/> tags.
                                    false,    //boolean indent   --save lots of space.
                                    false);   //hide errors  --only do if explicitly asked
            System.out.println("one pretty print test.");
                                    
        }
        return;
    }

}