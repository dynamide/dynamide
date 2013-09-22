/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

/** For now all these magic constants are collected here.  In theory, a user
 *  should be able to plug in a global text file and modify the mapped values
 *  of these constants.  Note that two such systems could be running in the same
 *  app server, and should be able to distinguish the namespaces.
 */
public class Constants {
    //Dont' allow construction of this class, it's just for constants.
    private Constants(){
    }
    public static final String DYNAMIDE_HOME_ENV  = "DYNAMIDE_HOME";
    public static final String DYNAMIDE_LOCAL_PROPERTIES_FILENAME = "dynamide.local.properties";

    //These are all keys in properties files:
    public static final String DYNAMIDE_HOME = "DYNAMIDE_HOME";
    public static final String DYNAMIDE_BUILD = "DYNAMIDE_BUILD";
    public static final String DYNAMIDE_RESOURCE_ROOT = "DYNAMIDE_RESOURCE_ROOT";
    public static final String DYNAMIDE_STATIC_ROOT = "DYNAMIDE_STATIC_ROOT";
    public static final String DYNAMIDE_CONTEXT_CONF = "DYNAMIDE_CONTEXT_CONF";

    public static final String SESSIONID = "SESSIONID";
    public static final String SESSION_PREFIX = "DM_";
    public static final String CONF_PREFIX = "conf.";
    public static final String LOGCONF_DYNAMIDE = "conf/log.conf";
    public static final String LOGCONF_DYNAMIDE_JUNIT = "conf/log-junit.conf";
    public static final String CONF_CONTEXT_REL = "/conf/context.xml";
    public static final String STATIC_DIR_REL = "/static";
    public static final String STATIC_PREFIX = "/static";
    public static final String DEFAULT_WEBAPPS = "/com/dynamide/conf/web-apps.xml";
    /** Expires header for static files, in milliseconds. */
    public static final int    DEFAULT_EXPIRES = 5*60*1000; //5 minutes for production, crank down to n*seconds for development.
    public static final int    NO_EXPIRES = 0;
    public static final String action = "action";
    public static final String Close = "Close";     // global action param
    public static final String Restart = "Restart"; // global action param
    public static final String nextPageID = "next";
    public static final String pageID = "page";
    public static final String reloadPage = "reloadPage";
    public static final String USER = "USER";
    public static final String dmFormatParamName = "dmFormat";
    public static final String dmFormatXML = "XML";

    /** Internationalization files (intl-res-&lt;language-code>.properties) are searched for in this directory.
     */
    public static final String internationalizationRelDir = "resources/intl";
    /** Widgets are searched for in this directory first, then relative to the project or library.
     */
    public static final String widgetsRelDir = "resources/widgets";

    public static final String jobLogItemTemplate = "resources/system/jobLogItem.wm";

    public static final String jobLogTemplate = "resources/system/jobLog.wm";

    public static final String XMLERROR_START = "<?xml version=\"1.0\"?><error>";
    public static final String XMLERROR_END = "</error>";

    public static final boolean PROFILE = false;
    /** This calls profiling, even if Constants.PROFILE is false. */
    public static final boolean DEBUG_OBJECT_LIFECYCLE = false;
    public static final String LOG_TRANSCRIPTS_CATEGORY = "com.dynamide.Constants.LOG_TRANSCRIPTS";
    public static final String LOG_HANDLER_PROC_CATEGORY = "com.dynamide.Constants.LOG_HANDLER_PROC";
    public static final String LOG_HANDLER_PROC_TIMING_CATEGORY = "com.dynamide.Constants.LOG_HANDLER_PROC_TIMING";
    public static final String LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY = "com.dynamide.Constants.LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY";
    public static final String LOG_HANDLER_PROC_REQUEST_HEADERS = "com.dynamide.Constants.LOG_HANDLER_PROC_REQUEST_HEADERS";
    public static final String LOG_EXPANSIONS_CATEGORY = "com.dynamide.Constants.LOG_EXPANSIONS";

    public static final String LOG_CACHING_CATEGORY = "com.dynamide.Constants.LOG_CACHING";
    public static final String LOG_TRANSFORMATIONS_CATEGORY = "com.dynamide.Constants.LOG_TRANSFORMATIONS";

}

