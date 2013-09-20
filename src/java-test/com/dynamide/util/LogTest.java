package com.dynamide.util;

import com.dynamide.*;

import junit.framework.*;


public class LogTest extends TestCase
{
    public LogTest(String name){
        super(name);
    }

    private Log m_log;

    private static void configureLog(String RESOURCE_ROOT){
        String logconf = Tools.fixFilename(RESOURCE_ROOT+"/conf/log.conf");
        //System.out.println("attempting to use log4j conf file: "+logconf);
        try {
            com.dynamide.util.Log.configure(logconf);
            Log.info("com.dynamide.util.LogTest", "log.conf file: "+logconf+" initialized at: "+Tools.nowLocale());
        } catch (Throwable t){
            System.out.println("ERROR: [66] couldn't properly configure log4j: "+t);
        }
    }

    public void testCategory(){
        assertNotNull(m_log);
        m_log.info("com.dynamide.util.LogTest.mycategory", "Hello, World!");
        m_log.info("com.dynamide.util.LogTest.mycategory.mysubisSoLongThatItMayBeTruncated", "Hello, World!");
    }

    public void setUp()
    throws Exception {
        String RESOURCE_ROOT =  com.dynamide.resource.ResourceManager.getResourceRootFromEnv();
        configureLog(RESOURCE_ROOT);
        m_log = Log.getInstance();
    }

    public static Test suite() {
        return new TestSuite(LogTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

}