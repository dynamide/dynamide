package com.dynamide.security;

import junit.framework.*;

import com.dynamide.*;

public class DynamideSecurityManagerTest extends DynamideTestCase {
    public DynamideSecurityManagerTest(String name){
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DynamideSecurityManagerTest.class);
    }

    public void testCheckCallStackCaller(){
        try {
            ensureCallerIsCorrect();
        } catch (SecurityException e){
            fail("testCheckCallStackCaller");
        }
    }

    private void ensureCallerIsCorrect(){
        //insist our for our caller's name must be testCheckCallStackCaller in com.dynamide.security.DynamideSecurityManagerTest
        DynamideSecurityManager.checkCallStack("com.dynamide.security.DynamideSecurityManagerTest",
                                                "testCheckCallStackCaller",
                                                1,
                                                true);
    }

    public void testCheckCallStack0(){
        try {
            DynamideSecurityManager.checkCallStack("com.dynamide.security.DynamideSecurityManagerTest",
                                                    "testCheckCallStack0",   //check for *our* name
                                                    0,
                                                    true);
        } catch (SecurityException e){
            fail("testCheckCallStack0");
        }
    }

    //====== The rest of thes are *supposed* to throw exceptions, so fail if they *don't*. ========

    public void testCheckCallStackWrongIndex(){
        try {DynamideSecurityManager.checkCallStack("com.dynamide.security.DynamideSecurityManagerTest",
                                                    "testCheckCallStackWrongIndex",
                                                    3,  //index is wrong
                                                    false);    //don't be verbose, so that junit tests aren't sprinkled with log.WARN
        } catch (SecurityException e){
            return;
        }
        fail("testCheckCallStackWrongIndex");
    }

    public void testCheckCallStackBad(){
        try {DynamideSecurityManager.checkCallStack("com.dynamide.security.DynamideSecurityManagerTest",
                                                    "foo",     //won't be found
                                                    0,
                                                    false);    //don't be verbose, so that junit tests aren't sprinkled with log.WARN
        } catch (SecurityException e){
            return;
        }
        fail("testCheckCallStackBad");
    }

    public void testCheckCallStackOutOfRange(){
        try {DynamideSecurityManager.checkCallStack("com.dynamide.security.DynamideSecurityManagerTest",
                                                    "foo",     //won't be found
                                                    100,
                                                    false);    //don't be verbose, so that junit tests aren't sprinkled with log.WARN
        } catch (SecurityException e){
            return;
        }
        fail("testCheckCallStackOutOfRange");
    }

    public static void main(String [] args){
        junit.textui.TestRunner.run(suite());
    }
}