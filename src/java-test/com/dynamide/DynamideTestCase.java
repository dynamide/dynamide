package com.dynamide;

import junit.framework.*;

import com.dynamide.*;
import com.dynamide.util.*;


/** This class presents a simplified logging API over DynamideObject -- this class is configured to
  *  ignore DEBUG level anyway, and you shouldn't be catching Exceptions in test cases -- let
  *  them bubble up to be caught by JUnit.
 *   <p>The Dynamide ResourceManager is initialized in standalone mode automatically.  If
 *   you don't want this, simply extend junit.framework.TestCase instead.
 *   </p>
  */
public class DynamideTestCase extends TestCase
{
    public DynamideTestCase(String name){
        super(name);
    }

    public void logInfo(String message) {
        Log.info(this, message, null);
    }

    public void logInfo(String categoryID, String message) {
        Log.info(categoryID, message);
    }

    public void logWarn(String message) {
        Log.warn(this, message, null);
    }

    public void logWarn(String categoryID, String message) {
        Log.warn(categoryID, message);
    }

    public void logError(String message) {
        Log.error(this, message, null);
    }

    public void logError(String categoryID, String message) {
        Log.error(categoryID, message);
    }

    /** Simply calls ResourceManager.configureLogForTest() so that logging is consistent.
     *  If your test requires the ResourceManager, overload setUp() to call
     *  <pre>
            public void setUp()
            throws Exception {
                com.dynamide.resource.ResourceManager.createStandaloneForTest();
            }

            public void tearDown()
            throws Exception {
                com.dynamide.resource.ResourceManager.shutdown();
            }
     * </pre>
     */
    public void setUp()
    throws Exception {
        com.dynamide.resource.ResourceManager.configureLogForTest();
    }

    protected void setUpForResourceManager()
    throws Exception {
        com.dynamide.resource.ResourceManager.configureLogForTest();

    }

}