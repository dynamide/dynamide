package com.dynamide.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URL;

import java.util.Properties;

import com.dynamide.DynamideObject;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.NDC;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;

/**
 * This is a wrapper class around log4j.
 * See <a href="http://jakarta.apache.org/log4j/javadoc/overview-summary.html">log4j
 * javadoc</a> .
 *
 * This file doesn't report its log location automatically.  If you want to know where the conf
 * comes from, use: getConfigFile() .
 *
 */

public class Log {
    /**
     * Singleton.
     */
    protected static Log log = null;

    /**
     * Get the singleton Log instance, instantiating if necessary.
     */
    public static Log getInstance() {
        if (log == null) {
            log = new Log();
        }
        return log;
    }

    /**
     * Flag showing whether the default config file has been overridden.
     */
    private static boolean g_configured = false;
    /**
     * The name of a config file other than the default, if a different config
     * file is to be used.
     */
    private static String  g_configFile = "";
    /**
     * Returns the name of the config file being used, if a config file other
     * than the default is to be used.
     */
    public String getConfigFile(){return g_configFile;}

    /**
     * Configure logging from the given config file name.
     *
     * @param configFile The name of a config file to use
     */
    public static Log configure(String configFile) {
        //System.out.println("Log.configure: "+configFile);
        try {
            Properties props = new  Properties();
            props.load(new FileInputStream(new File(configFile)));
            PropertyConfigurator.configure(props);
            g_configFile = configFile;
            g_configured = true;
        } catch (IOException e){
            System.err.println("Could not 'configure' based on config file: "+configFile);
        }
        Log log = getInstance();
        return log;
    }

    /**
     * Private constructor to enforce the singleton. Gets the root
     * <kbd>org.apache.log4j.Category</kbd> instance and, if necessary,
     * configures it. If a <kbd>configure</kbd> has been called to use
     * a config file other than the default, no configuration is done here.
     * Otherwise, the default config file is used. If the default config file
     * doesn't exist, the <kbd>org.apache.log4j.BasicConfigurator</kbd> is used.
     *
     * @see #configure(java.lang.String configFile)
     */
    private Log() {
    	//String className = getClass().getName().replace('.', File.separatorChar);
    	String className = getClass().getName().replace('.', '/');
        String config = className + DEFAULT_EXT;

        Category root = Category.getRoot();
        ClassLoader cl = getClass().getClassLoader();
        if (g_configured) {
            root.info("Properties file used: "+g_configFile);
        } else {
            URL url = cl.getResource(config);
            if (url == null) {
                BasicConfigurator.configure();
                //root.info("No properties file found. Using BasicConfigurator");
            } else {
                PropertyConfigurator.configure(url);
                root.info("Found properties file (" + url + "). Using PropertyConfigurator");
            }
        }
    }

    public boolean isEnabledFor(String categoryID, Priority priority) {
        Category cat = Category.getInstance(categoryID);
        return cat.isEnabledFor(priority);
    }

    protected boolean isEnabledFor(Class catClass, Priority priority) {
        String fqn = catClass.getName();
        Category cat = Category.getInstance(fqn);
        return cat.isEnabledFor(priority);
    }

    public static boolean isDebugEnabled(Object caller) {
        return getInstance().isEnabledFor(caller.getClass(), Priority.DEBUG);
    }

    public static boolean isDebugEnabled(Class catClass) {
        return getInstance().isEnabledFor(catClass, Priority.DEBUG);
    }

    public static boolean isInfoEnabled(Object caller) {
        return getInstance().isEnabledFor(caller.getClass(), Priority.INFO);
    }

    public static boolean isInfoEnabled(Class catClass) {
        return getInstance().isEnabledFor(catClass, Priority.INFO);
    }

    protected void logMessage(Class catClass, Priority priority, String message, Throwable throwable) {
        String categoryID = catClass.getName();
        Category cat = Category.getInstance(categoryID);
        logMessage(categoryID, priority, message, throwable);
    }

    protected void logMessage(Object object, Priority priority, String message, Throwable throwable) {
        String categoryID = null;
        if (object instanceof DynamideObject){
            categoryID = ((DynamideObject)object).getCategoryID();
        }
        if (categoryID == null) {
            categoryID = object.getClass().getName();
        }
        logMessage(categoryID, priority, message, throwable);
    }

    protected void logMessage(String categoryID, Priority priority, String message, Throwable throwable) {
        Category cat = Category.getInstance(categoryID);
        if (cat.isEnabledFor(priority)) {
            cat.log(categoryID, priority, message, throwable);
        }
    }

    //================ Public Methods ==================================================

    /** Sends immediately to System.out.println, so you don't have to have that in your code.
     */
    public static void println(String msg){
        System.out.println(msg);
    }

    // The getInstance method, above, is also public.

    public static void debug(String categoryID, String message) {
        getInstance().logMessage(categoryID, Priority.DEBUG, message, null);
    }

    public static void debug(String categoryID, String message, Throwable throwable) {
        getInstance().logMessage(categoryID, Priority.DEBUG, message, throwable);
    }

    public static void debug(Object caller, String message) {
        getInstance().logMessage(caller, Priority.DEBUG, message, null);
    }

    public static void debug(Object caller, String message, Throwable throwable) {
        getInstance().logMessage(caller, Priority.DEBUG, message, throwable);
    }

    public static void debug(Class catClass, String message) {
        getInstance().logMessage(catClass, Priority.DEBUG, message, null);
    }

    public static void debug(Class catClass, String message, Throwable throwable) {
        getInstance().logMessage(catClass, Priority.DEBUG, message, throwable);
    }

    public static void info(String categoryID, String message) {
        getInstance().logMessage(categoryID, Priority.DEBUG, message, null);
    }

    public static void info(String categoryID, String message, Throwable throwable) {
        getInstance().logMessage(categoryID, Priority.INFO, message, throwable);
    }

    public static void info(Object caller, String message) {
        getInstance().logMessage(caller, Priority.INFO, message, null);
    }

    public static void info(Object caller, String message, Throwable throwable) {
        getInstance().logMessage(caller, Priority.INFO, message, throwable);
    }

    public static void info(Class catClass, String message) {
        getInstance().logMessage(catClass, Priority.INFO, message, null);
    }

    public static void info(Class catClass, String message, Throwable throwable) {
        getInstance().logMessage(catClass, Priority.INFO, message, throwable);
    }

    public static void warn(String categoryID, String message) {
        getInstance().logMessage(categoryID, Priority.DEBUG, message, null);
    }

    public static void warn(String categoryID, String message, Throwable throwable) {
        getInstance().logMessage(categoryID, Priority.WARN, message, throwable);
    }

    public static void warn(Object caller, String message) {
        getInstance().logMessage(caller, Priority.WARN, message, null);
    }

    public static void warn(Object caller, String message, Throwable throwable) {
        getInstance().logMessage(caller, Priority.WARN, message, throwable);
    }

    public static void warn(Class catClass, String message) {
        getInstance().logMessage(catClass, Priority.WARN, message, null);
    }

    public static void warn(Class catClass, String message, Throwable throwable) {
        getInstance().logMessage(catClass, Priority.WARN, message, throwable);
    }

    public static void error(String categoryID, String message) {
        getInstance().logMessage(categoryID, Priority.DEBUG, message, null);
    }

    public static void error(String categoryID, String message, Throwable throwable) {
        getInstance().logMessage(categoryID, Priority.ERROR, message, throwable);
    }

    public static void error(Object caller, String message) {
        getInstance().logMessage(caller, Priority.ERROR, message, null);
    }

    public static void error(Object caller, String message, Throwable throwable) {
        getInstance().logMessage(caller, Priority.ERROR, message, throwable);
    }

    public static void error(Class catClass, String message) {
        getInstance().logMessage(catClass, Priority.ERROR, message, null);
    }

    public static void error(Class catClass, String message, Throwable throwable) {
        getInstance().logMessage(catClass, Priority.ERROR, message, throwable);
    }

    public static void push(String context) {
        NDC.push(context);
    }

    public static String pop() {
        return NDC.pop();
    }

    // Constants
    public static final String DEFAULT_EXT = ".conf";

    public static void main(String[]args){
        Opts opts = new Opts(args);
        Log.configure(opts.getOption("--logconf"));
    }

}
