package com.dynamide.util;

import java.io.*;
import java.util.*;

import org.webmacro.*;

import com.dynamide.util.TemplateProvider;

public class WebMacroTools {

    public WebMacroTools(String templateRoot, String propertiesFileFullPath, boolean cache)
    throws InitException {
        webmacro = new org.webmacro.WM(propertiesFileFullPath);
        broker = webmacro.getBroker();
        broker.addProvider(new TemplateProvider("template", templateRoot, cache), "template");
        //wm2.01b broker.startClient();  //pump up the ref count one.
                               // HACK. Without this, the ref count internal to webmacro broker drops to zero,
                               // and they expect that you will re-read the config file
                               // each time to re-initialize the broker.
    }

    public void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    private Opts m_opts = null;
    public Opts getOpts(){
        return m_opts;
    }

    private org.webmacro.WM webmacro = null;
    private Broker broker = null;

    private boolean m_strictLength = false;
    public boolean getStrictLength(){return m_strictLength;}
    public void setStrictLength(boolean new_value){m_strictLength = new_value;}

    private boolean m_emulatePage = false;
    public void setEmulatePage(boolean val){m_emulatePage = val;}

    public Context getContext(){
        Context c = webmacro.getContext();
        return c;
    }

    public void releaseContext(Context c){
        if ( c != null ) {
            // remove all items from the context with this:
            //wm2.01b c.recycle();   //webmacro will return this object to the pool.
            c.clear(); //wm2.01b
        }
    }

    public void destroy(){
        if ( webmacro !=  null ) {
            org.webmacro.Broker brokerOld = webmacro.getBroker();
            //wm2.01b brokerOld.stopClient();
            //wm2.01b brokerOld.shutdown();
            //wm2.01b webmacro.destroy();
            webmacro = null;
        }
    }

    public void quit(){
        System.exit(0);
    }

    public void exit(){
        System.exit(0);
    }


    /** Before you call this method, you should call getContext(), then with that context,
     *  you should add any variables to the context with c.put(name, value), and be sure to
     *  wrap the call in a try/finally to remove the context:
     *  <pre>
     *  WebMacroTools wmt = new WebMacroTools("", "", false);
     *  Context c = wmt.getContext();
     *  c.put("foo", foo);
     *  c.put("bar", bar);
        try {
     *      wmt.expand(c, templateText);
        } finally {
     *      wmt.releaseContext(c);
     *      wmt.destroy();  //do this if you are done with WebMacroTools.
        }
     *
     * </pre>
     */
    public String expand(Context c, String templateText)
    throws Exception {
        String PAD = " PaDDinG";
        int PAD_LEN = PAD.length();
        templateText += PAD; //padding.
        org.webmacro.Template t = new org.webmacro.engine.StringTemplate(broker, templateText);
        FastWriter fw = new FastWriter(broker, "US-ASCII");
        StringReader sr = new StringReader(templateText);
        try {
            org.webmacro.engine.Parser parser = (org.webmacro.engine.Parser) broker.get("parser", "wm");
            parser.parseBlock("template", sr);
        } finally {
            sr.close();
            sr = null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //wm2.01b t.write(fw, c);
        t.write(bos, "US-ASCII", c);//wm2.01b

        String tmpl = bos.toString();//wm2.01b
        fw.close();
        fw = null;
        if ( tmpl.length() > 0 ) {
            if ( m_strictLength ) {
                tmpl = tmpl.substring(0,tmpl.length()-(PAD_LEN-1)); //remove padding.
            } else {
                tmpl = tmpl.substring(0,tmpl.length()-(PAD_LEN-2)); //remove padding.
            }
        }
        return tmpl;
    }

    public static String expand(String templateText) throws Exception{
        WebMacroTools wmt = new WebMacroTools("", "", false);
        Context c = wmt.getContext();
        return wmt.expand(c, templateText);
    }

    // you must use this with something like:
    /*
        <xsl:stylesheet
          version="1.0"
          xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
          xmlns:dynamide="http://www.dynamide.com/java/com.dynamide.util.WebMacroTools"
          >
         <xsl:output method="xml" omit-xml-declaration="yes" indent="yes" />
         <xsl:value-of disable-output-escaping="yes"  select="dynamide:expand()"/>
          ...
     */
    public static String expand(com.icl.saxon.Context saxonContext) throws Exception{
        //return "saxonContext: "+saxonContext.getContextNode().getNodeName()+" outputter: "+saxonContext.getOutputter();
        // getContextNode returns a w3c Node,
        // getCurrentNodeInfo returns a saxon NodeInfo, which is a wrapper that give you all the goodies.
        saxonContext.getCurrentNodeInfo().copy(saxonContext.getOutputter()); //this dumps the *xml* document to the output stream.
        saxonContext.getCurrentTemplate().copy(saxonContext.getOutputter());
        return " <== output. template: ==>"+saxonContext.getCurrentTemplate().getStringValue()+"<==";
    }

    public static String dumpContext(Context WEBMACRO_CONTEXT, boolean html){
        if ( html ) {
            return dumpContext(WEBMACRO_CONTEXT);
        } else {
            String WMC = WEBMACRO_CONTEXT.getMap().entrySet().toString();
            WMC = WMC.substring(1, WMC.length()-1);
            // println("WMC: "+WMC);
            Iterator it = StringTools.parseSeparatedValues(WMC, ", ").iterator();
            StringBuffer buff = new StringBuffer();
            while (it.hasNext()){
                String row = it.next().toString();
                //buff.append(StringTools.searchAndReplaceAll(row, "=", "</td><td>")+"</td></tr>");
                buff.append(row);
                buff.append("\r\n\r\n");
            }
            return buff.toString();
        }
    }

    /** If you want a Map, use $WEBMACRO_CONTEXT.getMap() */
    public static String dumpContext(Context WEBMACRO_CONTEXT){
        StringBuffer buff = new StringBuffer("<table border='1' cellpadding='0' cellspacing='0'>");
        Iterator it = WEBMACRO_CONTEXT.getMap().entrySet().iterator();
        Map.Entry en;
        while (it.hasNext()){
            en = (Map.Entry)it.next();
            buff.append("<tr><td>"+en.getKey()+"</td><td>"+en.getValue()+"</td></tr>");
        }
        buff.append("</table>");
        return buff.toString();
    }

    public static final Object loadClass(String className)     //%% might want to lock this down a bit.  Now you can create any class from webmacro. :(
    throws Exception {                                         // %% maybe by role of user.
        return Class.forName(className).newInstance();
    }

    public static final Class loadClassStatic(String className)     //%% might want to lock this down a bit.  Now you can create any class from webmacro. :(
    throws Exception {                                         // %% maybe by role of user.
        return Class.forName(className);
    }

    public static final Object callStaticMethod(Class theClass, String methodName)
    throws Exception {
        Class[] params = {};
        Object[] invokeParams = {};
        return callStaticMethod(theClass, methodName, params, invokeParams);
    }

    public static final Object callStaticMethod(Class theClass, String methodName, Class[] params, Object[] invokeParams)
    throws Exception {
        java.lang.reflect.Method method = theClass.getMethod(methodName, params);
        Object result = method.invoke(null, invokeParams);
        return result;

    }

    //=========== members for shell ================================

    private Vector oklist = new Vector();
    private String lastLine = "";
    private Context shellContext;


    private String  m_saveFilename = "";
    public String  getSaveFilename(){return m_saveFilename;}
    public void setSaveFilename(String  new_value){m_saveFilename = new_value;}

    private boolean m_lineNumbers = true;
    public boolean getLineNumbers(){return m_lineNumbers;}
    public void setLineNumbers(boolean new_value){m_lineNumbers = new_value;}

    private boolean m_auto = true;
    public boolean getAuto(){return m_auto;}
    public void setAuto(boolean new_value){m_auto = new_value;}

    private boolean m_braces = true;
    public boolean getBraces(){return m_braces;}
    public void setBraces(boolean new_value){m_braces = new_value;}

    private boolean m_quietInit = false;
    public boolean getQuietInit(){return m_quietInit;}
    public void setQuietInit(boolean new_value){m_quietInit = new_value;}

    private boolean m_noCopyrightNotice = false;
    public boolean getNoCopyrightNotice(){return m_noCopyrightNotice;}
    public void setNoCopyrightNotice(boolean new_value){m_noCopyrightNotice = new_value;}

    public final static String WEBMACROSHELL_QUIET_INIT = "WEBMACROSHELL_QUIET_INIT";
    //public final static String WEBMACROSHELL_QUIET_INIT = "WEBMACROSHELL_QUIET_INIT";

    private void doHistory(String num)
    throws Exception {
        int inum;
        if (num == ""){
            inum = oklist.size();
        } else {
            inum = Tools.stringToIntSafe(num, -1);
        }
        if (inum > 0 && inum <= oklist.size()){
            String expr = (String)oklist.elementAt(inum-1);
            shellExpand(expr);
        } else {
            println("ERROR: history index out of range: "+inum);
        }
    }

    public void source(String filename)
    throws Exception {
        String content = FileTools.readFile("", filename);
        if ( content != null ) {
            shellExpand(content);
            oklist.add(content);
        } else {
            println("ERROR reading file: "+filename);
        }
    }

    public void shellExpand(String line)
    throws Exception {
        shellExpand(line, true);
    }

    public String shellExpand(String line, boolean print)
    throws Exception {
        //println("shellExpand: "+print+" line: "+line);
        String res = expand(shellContext, line);
        if (m_emulatePage){  //%% todo -- make this pluggable, so that we don't compile in com.dynamide.Page.
            res = com.dynamide.Page.finalExpansions(true, res);
            res = com.dynamide.util.StringTools.unescape(res);
        }
        if (print){
            shellPrintResult(res);
        }
        m_results.append(res);
        return res;
    }

    private void shellPrintResult(String result){
        if (m_braces){
            println("==>"+result+"<==");
        } else {
            println(result);
        }
    }

    private StringBuffer m_results = new StringBuffer();

    public String getResults(){
        return m_results.toString();
    }

    public void clearResults(){
        m_results = new StringBuffer();
    }

    private String getShellVariable(String key){
        if (shellContext.containsKey(key)){
            return shellContext.get(key).toString();
        }
        return "";
    }


    private void shellInitContext(){
        if (shellContext != null){
            //wm2.01b shellContext.recycle();
            shellContext.clear(); //wm2.01b
        }
        shellContext = this.getContext();
        shellContext.put("shell", this);
        shellContext.put("context", shellContext);
    }

    private void shellInit(Opts opts)
    throws Exception {
        //do it this way so that programmatic initialization of m_quietInit is preserved before inspecting commandline:
        m_emulatePage = opts.getOptionBool("-emulatePage");
        m_quietInit = m_quietInit == true ? true : opts.getOptionBool("-quietInit");
        String init = opts.getOption("-init");
        boolean includeScript = opts.getOptionBool("-includeInit");
        String initContent;
        String initname;
        if ( init.length() > 0 ) {
            initname = init;
            initContent = FileTools.readFile("", init);
        } else {
            String homedir = System.getProperty("user.home", "");
            initContent = FileTools.readFile(homedir, ".webmacroshell");
            initname = homedir + File.separator + ".webmacroshell";
        }
        if ( initContent != null && initContent.length() > 0 ) {
            try {
                String result = shellExpand(initContent, false);
                //boolean quietinit = Tools.isTrue(getInitVariable(WEBMACROSHELL_QUIET_INIT));
                if (!getQuietInit()){
                    println("");
                    println("Dynamide shell for WebMacro, init(\""+initname+"\")");
                    shellPrintResult(result);
                }
                if (includeScript){
                    oklist.add(initContent);
                }
            } catch (Throwable t){
                System.err.println("ERROR reading init file: "+t);
            }
        }
    }

    private void shellRun(String filename, boolean print){
        //println("shellRun: "+filename);
        String content = FileTools.readFile("", filename);
        if ( content != null ) {
            try {
                m_braces = false;

                shellExpand(content, print);
            } catch (Throwable t){
                System.err.println("ERROR: in file '"+filename+"' error: "+t);
                System.exit(1);
            }
        } else {
            System.err.println("ERROR reading file: "+filename);
            System.exit(2);
        }
    }

    public String include(String filename)
    throws Exception {
        if (filename.startsWith("~/")){
            filename = System.getProperty("user.home", "") + File.separator + filename.substring(2);
        }
        if (!getQuietInit())println("Dynamide shell for WebMacro, include(\""+filename+"\")");
        String includeContent = FileTools.readFile("", filename);
        String result = shellExpand(includeContent, !getQuietInit());
        oklist.add(includeContent);
        return result;
    }

    public static void println(String msg){
        System.out.println(msg);
    }

    private void shell(Opts opts){
        try {
            m_opts = opts;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            this.setStrictLength(true);
            shellInitContext();
            if ( ! opts.getOptionBool("-noInit")){
                shellInit(opts);
            }
            String[] remaining = opts.getRemainingArgs();
            //println("remaining: "+Tools.arrayToString(remaining, "::")+"; quiet: "+getQuietInit());  //remaining is the args after the last arg defined in Opts init.
            String fn = opts.getOption("-o");
            boolean print = fn.length()<=0;
            for (int i=0; i < remaining.length; i++) {
                shellRun(remaining[i], print);
            }
            if ( fn.length()>0 ) {
                String content = getResults();
                FileTools.saveFile("", fn, content);
            }
            if ( remaining.length > 0 ) {
                System.exit(0);
            }

            if ( !m_noCopyrightNotice ){
                copyrightNotice();
            }
            boolean blockmode = false;
            StringBuffer block = new StringBuffer();
            while ( true ) {
                try {
                    System.out.print("webmacro% ");
                    String line = in.readLine().trim();
                    String lineUpper = line.toUpperCase();
                    
                    if (blockmode==true){
                        if ( lineUpper.startsWith("ENDBLOCK") ) {
                            blockmode = false;
                            line = block.toString();  //clear and replace line with whole block plus latest line.
                            block = new StringBuffer();
                            lastLine = line;
                            if ( m_auto && line.length()>0) {
                                oklist.add(line);
                            } 
                            println("Expanding BLOCK:\r\n"+line);
                            shellExpand(line);//line is now what was in block.
                        } else {
                            block.append(line);
                            block.append("\r\n");                            
                        }
                        continue;   
                    }
                    
                    if ( line.equals("!!") ) {
                        doHistory("");
                    } else if ( line.startsWith("!") ) {
                        doHistory(line.substring(1));
                    } else if ( lineUpper.startsWith("BLOCK") ) {
                        blockmode = true;
                        block = new StringBuffer();
                        line = line.substring("BLOCK".length());
                        block.append(line);
                        block.append("\r\n");
                        println("Enter block text, then ENDBLOCK at the begining of a line to finish...");
                        continue;
                    
                    } else if ( line.equalsIgnoreCase("ADD") ) {
                        oklist.add(lastLine);
                        println("Line added");
                    } else if ( line.equalsIgnoreCase("AUTO") ) {
                        m_auto = !m_auto;
                        println("Automatically add to history list: "+m_auto);
                    } else if ( line.equalsIgnoreCase("BRACES") ) {
                        m_braces = !m_braces;
                        println("Wrap result in display braces: "+m_braces);
                    } else if ( lineUpper.startsWith("CAT") ) {
                        String filename = line.substring("CAT".length()).trim();
                        String content = FileTools.readFile("", filename);
                        println(content);
                    } else if ( line.equalsIgnoreCase("CLEAR") ) {
                        oklist.clear();
                        println("History list cleared");
                    } else if ( line.equalsIgnoreCase("DUMP") ) {
                        println("Context:\r\n");
                        println(WebMacroTools.dumpContext(shellContext, false));
                    } else if ( line.equalsIgnoreCase("EXIT") ) {
                        System.exit(0);
                    } else if ( line.equalsIgnoreCase("HELP") ) {
                        WebMacroTools.shellHelp();
                    } else if ( lineUpper.startsWith("LINEN") ) {
                        m_lineNumbers = !m_lineNumbers;
                        println("Show line numbers in history list: "+m_lineNumbers);
                    } else if ( line.equalsIgnoreCase("LIST") || line.equalsIgnoreCase("HISTORY") ) {
                        if (m_lineNumbers){
                            println(Tools.vectorToString(oklist, "\r\n\r\n", "$linenumber: ", 1));
                        } else {
                            println(Tools.vectorToString(oklist, "\r\n\r\n"));
                        }
                        println("");
                    } else if ( lineUpper.startsWith("LOAD") ) {
                        String loadargs = line.substring("load".length()).trim();
                        StringTokenizer st = new StringTokenizer(loadargs);
                        String varname = "";
                        String classname = "";
                        if (st.hasMoreTokens()) {
                            varname = st.nextToken();
                        }
                        if (st.hasMoreTokens()) {
                            classname = st.nextToken();
                        }
                        if ( varname.length() > 0 && classname.length() > 0 ) {
                            println("Loading "+classname);
                            String content = "#set $"+varname+" = $tools.loadClass(\""+classname+"\")";
                            shellExpand(content);
                            oklist.add(content);
                        } else {
                            println("ERROR: incorrect args.  Should be 'load varname classname'");
                        }
                    } else if ( line.equalsIgnoreCase("NEW") ) {
                        shellInitContext();
                        println("New Context");
                    } else if ( line.equalsIgnoreCase("QUIT") ) {
                        System.exit(0);
                    } else if ( lineUpper.startsWith("READ") ) {
                        String filename = line.substring("READ".length()).trim();
                        String content = FileTools.readFile("", filename);
                        if ( content != null ) {
                            shellExpand(content);
                            oklist.add(content);
                        } else {
                            println("ERROR reading file: "+filename);
                        }
                    } else if (lineUpper.startsWith("SOURCE") ) {
                        String filename = line.substring("SOURCE".length()).trim();
                        source(filename);
                    } else if ( line.equalsIgnoreCase("RUN") ) {
                            shellExpand(Tools.vectorToString(oklist, "\r\n"));
                    } else if ( line.toUpperCase().startsWith("SAVE") ) {
                        if ( line.length() > "SAVE".length() ) {
                            m_saveFilename = line.substring("SAVE".length()).trim();
                        }
                        if ( m_saveFilename == "" ) {
                            System.out.print("Filename: ");
                            m_saveFilename = in.readLine().trim();
                        }
                        File fsaved = Tools.saveVectorToFile(oklist, "\r\n", "", m_saveFilename);
                        if (fsaved!=null) {
                            println("Saved: "+fsaved.getCanonicalPath());
                        } else {
                            println("WARNING: File was not saved!");
                        }
                    } else if ( lineUpper.startsWith("GET ") ){
                        try {
                            //REMOVED.  This was a test case, but now that work has been moved to tagland.sourceforge.net
                            //com.dynamide.tagoline.LoboBrowserFrame frame = new com.dynamide.tagoline.LoboBrowserFrame();
                            //frame.navigate(line.substring("GET ".length()));//NOTE EMBEDDED SPACE.
                            //println("See browser window...");
                            println("Lobo Browser support has been disabled.");
                        } catch (Exception lbe){
                            println("ERROR starting browser: "+lbe);   
                        }
                    } else {
                        shellExpand(line);
                        lastLine = line;
                        if ( m_auto && line.length()>0) {
                            oklist.add(line);
                        }
                    }
                } catch (Exception e){
                    println("Exception: "+e);
                }
            } //end-while
        } catch (Exception e){
            println("Exception: "+e);
        }
    }

    public static void copyrightNotice() {
        println("");
        println("Welcome to the Dynamide shell for WebMacro.");
        println("Type 'help' for shell commands.");
        println("Run with -help to see command-line options.");
        println("Copyright (c) 2003 DYNAMIDE.COM");
        println("");
    }

    private static void shellHelp(){
        println("Commands:");
        println("");
        println("  !!                  (Re-evaluate last line in history list)");
        println("  !<n>                (Re-evaluate line n in history list)");
        println("  <expression>        (Expand <expression>)");
        println("  $context            (Reference to this shell's context)");
        println("  $shell              (Reference to this shell is in context)");
        println("  add                 (Add last OK line to history list)");
        println("  auto                (Toggle automatically adding lines to the history list)");
        println("  cat <filename>      (Print a file from disk)");
        println("  clear               (Clear the history list)");
        println("  dump                (Show the webmacro context)");
        println("  exit                (Quit this shell)");
        println("  help                (Show this message)");
        println("  history             (Show the history list)");
        println("  linen               (Show line numbers in the history list)");
        println("  list                (Show the history list)");
        println("  load <var> <class>  (Load a class by dotname as $var)");
        println("  braces              (Toggle display braces around results)");
        println("  new                 (Get a new webmacro context)");
        println("  quit                (Quit this shell)");
        println("  read <file>         (Load a file into the history list and run it)");
        println("  source <file>       (Load a file into the history list and run it)");
        println("  run                 (Expands everything in the history list)");
        println("  save [filename]     (Saves the history list)");
        println("");
        println("  Run with -help to see command-line options");
        println("");
    }

    public static void usage(){
        println("");
        println("Usage:  ");
        println("  webmacro [options] [filename]");
        println("    When run with [filename], the shell expands the file, then quits.");
        println("    When run without [filename], the shell enters interactive mode.");
        println("  Options:");
        println("    -help                   (Show this message)");
        println("    -emulatePage            (Perform the same processing as com.dynamide.Page)");
        println("    -init [filename]        (a file to process on startup)");
        println("    -logconf [filename]     (a Log4j conf file to override the one on the classpath)");
        println("    -includeInit            (init file is included in saved scripts");
        println("    -noInit                 (Don't read .webmacroshell from home directory)");
        println("    -o [filename]           (ouput to file, without stderr)");
        println("    -quietInit              (Be quiet during init. [Can be reset in init script])");
        println("  Startup:");
        println("    The file .webmacroshell in the user's home directory is read.");
        println("    This file contains many more options.");
        println("");
    }

    // TODO: make a LOAD command to load a class.  There may be a webmacro macro already to do this.
    // alternatively, make a util class and add it to the context, such that you can say $tools.loadClass("com.foo.BAR")

    public static void main(String [] args)
    throws Exception {
        Opts opts = new Opts(args);
        opts.addOption("-help", Boolean.class, false);
        opts.addOption("-emulatePage", Boolean.class, false);
        opts.addOption("-includeInit", Boolean.class, false);
        opts.addOption("-init", String.class, false);
        opts.addOption("-logconf", String.class, false);
        opts.addOption("-noInit", Boolean.class, false);
        opts.addOption("-quietInit", Boolean.class, false);
        opts.addOption("-in", String.class, false);

        if ( opts.getOptionBool("-help") ) {
            usage();
            System.exit(0);
        }
        String logconf = opts.getOption("-logconf", "");
        if (logconf!= null && logconf.length()>0){
        	Log.configure(logconf);        	
        }
        //find the resource on the classpath
        //System.out.println("PWD: "+(new File(".")).getCanonicalPath());
        new WebMacroTools("", "com/dynamide/util/WebMacroTools.properties", false).shell(opts);
        System.exit(0);
    }


}
