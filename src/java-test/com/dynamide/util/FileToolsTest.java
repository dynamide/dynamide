package com.dynamide.util;


import java.io.*;

import junit.framework.*;

import com.dynamide.*;
import com.dynamide.resource.*;
import com.dynamide.util.*;


public class FileToolsTest extends DynamideTestCaseInResourceManager {
    public FileToolsTest(String name){
        super(name);
    }

    //setUp and tearDown handled by DynamideTestCaseInResourceManager

    public static Test suite() {
        return new TestSuite(FileToolsTest.class);
    }

    private String m_home = "";
    private ResourceManager m_resman = null;

    public void setUp() {
        super.setUp();
        m_resman = ResourceManager.getRootResourceManager();
        m_home = m_resman.getDynamideHomeFromEnv();
    }

    //========= junit tests ===================

    public void testResolve()
    throws Exception {
        String s;
        String name = "C:\\Program Files\\foo";
        s = FileTools.resolve("C:\\Program Files\\foo");
        Log.warn(FileToolsTest.class, "s: "+s+" name: "+name);
        assertTrue(name.equals(s));
        s = FileTools.resolve("\""+name+"\"");
        assertTrue(name.equals(s));
    }

    public void testOpenFile()
    throws Exception {
        File f = FileTools.openFile("", Tools.fixFilename(m_home+"/dynamide.version"));
        if ( ! f.exists() ){
            fail("openFile(\"\", \"dynamide.version\")");
        }

        f = FileTools.openFile("", Tools.fixFilename(m_home+"/foobar"));
        if ( f.exists() ){
            fail("openFile(\"\", \"foobar\") should have failed");
        }

        String path = Tools.fixFilename(m_home+"/build/temp/test/FileToolsTest.out");
        f = FileTools.openFile("", path, true);
        String parentpath = Tools.fixFilename(m_home+"/build/temp/test");
        File parent = FileTools.openFile("", parentpath, false);
        if ( ! parent.exists()){
            fail("couldn't create parent, but just forced dirs: "+parentpath);
        }
        if ( ! parent.getCanonicalPath().equals(f.getParentFile().getCanonicalPath())) {
            fail("Force directories didn't work");
        }
    }

    public void testJoins(){
        if (FileTools.fileSystemIsDOS()){
            assertEquals(FileTools.join("C:\\mojo\\foo\\", "\\bar"), "C:\\mojo\\foo\\bar");
            assertEquals(FileTools.join("C:/mojo/foo/", "/bar"), "C:\\mojo\\foo\\bar");
            assertEquals(FileTools.join("C:/mojo/foo/", "\\bar"), "C:\\mojo\\foo\\bar");
            assertEquals(FileTools.join("C:/mojo/foo/", "/bar/"), "C:\\mojo\\foo\\bar\\");
            assertEquals(FileTools.join("C:/mojo/foo", "bar/"), "C:\\mojo\\foo\\bar\\");
            assertEquals(FileTools.join("C:/mojo/foo", "bar"), "C:\\mojo\\foo\\bar");

            assertEquals(FileTools.joinExt("C:/mojo/foo/", ".txt"), "C:\\mojo\\foo\\.txt"); //wacky, but correct.
            assertEquals(FileTools.joinExt("C:/mojo/foo/bar", ".txt"), "C:\\mojo\\foo\\bar.txt");
        } else {
            assertEquals(FileTools.join("C:\\mojo\\foo\\", "\\bar"), "C:/mojo/foo/bar");
            assertEquals(FileTools.join("\\mojo\\foo\\", "\\bar"), "/mojo/foo/bar");
            assertEquals(FileTools.join("/mojo/foo/", "/bar"), "/mojo/foo/bar");
            assertEquals(FileTools.join("/mojo/foo/", "\\bar"), "/mojo/foo/bar");
            assertEquals(FileTools.join("/mojo/foo/", "/bar/"), "/mojo/foo/bar/");
            assertEquals(FileTools.join("/mojo/foo", "bar/"), "/mojo/foo/bar/");
            assertEquals(FileTools.join("/mojo/foo", "bar"), "/mojo/foo/bar");

            assertEquals(FileTools.joinExt("mojo/foo/", ".txt"), "/mojo/foo/.txt"); //wacky, but correct.
            assertEquals(FileTools.joinExt("mojo/foo/bar", ".txt"), "/mojo/foo/bar.txt");
        }
        assertEquals(FileTools.joinURIExt("/mojo/foo/", "/bar", ".txt"), "/mojo/foo/bar.txt");
        assertEquals(FileTools.joinURIExt("\\mojo/foo\\", "/bar", ".txt"), "/mojo/foo/bar.txt");
        assertEquals(FileTools.joinURIExt("/mojo/foo", ".txt"), "/mojo/foo.txt");
        assertEquals(FileTools.joinURIExt("\\mojo\\foo", ".txt"), "/mojo/foo.txt");
    }
}