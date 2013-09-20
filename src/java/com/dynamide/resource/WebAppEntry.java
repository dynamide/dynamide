package com.dynamide.resource;

import com.dynamide.DynamideObject;

public final class WebAppEntry extends ContextNode {
    public WebAppEntry(DynamideObject owner){
        super(owner);
    }

    public WebAppEntry(DynamideObject owner, String key){
        super(owner, key);
    }

    public WebAppEntry(String key){
        super(key);
    }

    public WebAppEntry(String uri, String appname, String assembly, String home,
                       String interfaceNumber, String build, String webappsFullPath, String secure,
                       String loadOnStartup, String verboseMode){
        this(null, uri);
        setURI(uri);
        rebindAttribute(APPNAME, appname);
        rebindAttribute(ASSEMBLY, assembly);
        rebindAttribute(HOME, home);
        setInterface(interfaceNumber);
        rebindAttribute(BUILD, build);
        rebindAttribute(SECURE, secure);
        rebindAttribute(Assembly.FULLPATH, webappsFullPath);
        rebindAttribute(LOADONSTARTUP, loadOnStartup);
        rebindAttribute(VERBOSEMODE, verboseMode);
        //Log.debug(WebAppEntry.class, "WebAppEntry: "+toString());
    }

    public static final String APP = "app";
    public static final String APPNAME = "APPNAME";
    public static final String ASSEMBLY = "ASSEMBLY";
    public static final String HOME = "HOME";
    public static final String INTERFACE = "INTERFACE";
    public static final String BUILD = "BUILD";
    public static final String SECURE = "SECURE";
    public static final String URI = "URI";
    public static final String LOADONSTARTUP = "load-on-startup";
    public static final String VERBOSEMODE = "verbose-mode";

    public String  getURI(){return (String)getAttribute(URI);}
    public void setURI(String  new_value){rebindAttribute(URI, new_value);}

    public String getAppname(){return (String)getAttribute(APPNAME);}

    public String  getAssembly(){return (String)getAttribute(ASSEMBLY);}

    public String  getHome(){return (String)getAttribute(HOME);}

    public String  getInterface(){return (String)getAttribute(INTERFACE);}
    public void setInterface(String new_value){rebindAttribute(INTERFACE, new_value);}

    public String  getBuild(){return (String)getAttribute(BUILD);}

    public String  getWebappsFullPath(){return (String)getAttribute(Assembly.FULLPATH);}

    public boolean getSecure(){
        return com.dynamide.util.Tools.isTrue(getAttribute(SECURE));
    }

    public boolean getLoadOnStartup(){
        return com.dynamide.util.Tools.isTrue(getAttribute(LOADONSTARTUP));
    }

    public boolean getVerboseMode(){
        return com.dynamide.util.Tools.isTrue(getAttribute(VERBOSEMODE));
    }

    /* Not used.
    public String toHTML(){
        StringBuffer sb = new StringBuffer();
        sb.append("{URI:"+getURI())
        .append("<br />\r\n").append("APPNAME:"+ getAppname())
          .append(",HOME:"+ getHome())
          .append(",interface:"+ getInterface())
          .append(",build:"+getBuild()) 
          .append(",secure:"+getSecure())
          .append(","+LOADONSTARTUP+":"+getLoadOnStartup())
          .append(","+VERBOSEMODE+":"+getVerboseMode())
        .append("<br />\r\n").append(",webappsFullPath:"+getWebappsFullPath())
        .append("<br />\r\n").append(",ASSEMBLY::{basename:"+ getAssembly()+'}');
        return sb.toString();
    }
    */
    
    public String toString(){
        return "{URI:"+getURI() +",APPNAME:"+ getAppname()
                 +",HOME:"+ getHome()
                 +",ASSEMBLY::{basename:"+ getAssembly()
                 +",interface:"+ getInterface()
                 +",build:"+getBuild()
                 +",secure:"+getSecure()
                 +","+LOADONSTARTUP+":"+getLoadOnStartup()
                 +","+VERBOSEMODE+":"+getVerboseMode()
                 +",webappsFullPath:"+getWebappsFullPath()+'}';
    }
}
    