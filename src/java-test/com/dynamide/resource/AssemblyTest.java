package com.dynamide.resource;

import junit.framework.*;

import com.dynamide.*;
import com.dynamide.resource.*;

public class AssemblyTest extends DynamideTestCase {
    public AssemblyTest(String name){
        super(name);
    }

    public void setUp()
    throws Exception {
        com.dynamide.resource.ResourceManager.createStandaloneForTest();
    }

    public void tearDown()
    throws Exception {
        com.dynamide.resource.ResourceManager.shutdown();
    }

    public static Test suite() {
        return new TestSuite(AssemblyTest.class);
    }

    public void testGetResource(){
        Session session = null;

        try {
            session = Session.createSession("/dynamide/test-suite/test-resources");
        } catch ( Exception e )  {
            fail("Couldn't create session /dynamide/test-suite/test-resources"+e);
        }

        Assembly assembly = session.getAssembly();
        IContext ic;

        //turn this on to see a list of all the resources in assembly:
        //print(assembly.listResourceNames("\r\n"));
        //print("\r\n\r\n");
        try {
            ic =            session.getApplicationResource("page1.xml");
            assertNotNull("session.getApplicationResource(\"page1.xml\")", ic);
        } catch (Exception e)  {
            fail("Couldn't getApplicationResource: page1.xml"+e);
        }

        ic =         assembly.getResource("page1.xml", false);
        assertNull("assembly.getResource(\"page1.xml\", false)", ic);

        ic =        assembly.getResource("apps/test-suite/test-resources/page1.xml", false);
        assertNotNull("assembly.getResource(\"apps/test-suite/test-resources/page1.xml\", false)", ic);

        ic =         assembly.getResource("dynamide.css", false);
        assertNull("assembly.getResource(\"dynamide.css\", false)", ic);

        ic =         assembly.getResource("resources/css/dynamide.css", false);
        assertNull("assembly.getResource(\"resources/css/dynamide.css\", false)", ic);

        ic =         assembly.getResource("resources/css/dynamide.css", true);
        assertNotNull("assembly.getResource(\"resources/css/dynamide.css\", true)", ic);

        ic =         assembly.getResource("resources/css/test-resources.css", true);
        assertNull("assembly.getResource(\"resources/css/test-resources.css\", true)", ic);

        ic =         assembly.getResource("resources/css/test-resources.css", false);
        assertNull("assembly.getResource(\"resources/css/test-resources.css\", false)", ic);

        ic =    assembly.getApplicationResource("test-suite/test-resources", "resources/css/test-resources.css");
        assertNotNull("assembly.getApplicationResource(\"test-suite/test-resources\", \"resources/css/test-resources.css\")", ic);


    }

    public static void main(String [] args){
        junit.textui.TestRunner.run(suite());
    }
}