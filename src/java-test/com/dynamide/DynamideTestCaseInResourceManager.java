package com.dynamide;

import java.io.*;

import junit.framework.*;

import com.dynamide.*;
import com.dynamide.resource.*;
import com.dynamide.util.*;

public class DynamideTestCaseInResourceManager extends DynamideTestCase {
    public DynamideTestCaseInResourceManager(String name){
        super(name);
    }

    public void setUp() {
        try {
            //Log.info(DynamideTestCaseInResourceManager.class, "setUp()");
            ResourceManager.createStandaloneForTest();
        } catch( Exception e ){
            fail("Exception in setUp: "+e);
        }
    }

    public void tearDown() {
        //Log.info(DynamideTestCaseInResourceManager.class, "tearDown()");
        try {
            ResourceManager.shutdown();
        } catch( Exception e ){
            fail("Exception in tearDown: "+e);
        }
    }

}