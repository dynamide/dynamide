package com.dynamide.util;

import junit.framework.*;

import com.dynamide.*;

public class URLDecoderTest extends DynamideTestCase {
    public URLDecoderTest(String name){
        super(name);
    }

    public static Test suite() {
        return new TestSuite(URLDecoderTest.class);
    }

    public void testParseURL(){
        try {
            String url = "http://myhost:82/mojo/nixon/index.html?p=1&q=2#target";
            URLDecoder decoder = new URLDecoder(url);
            assertTrue( "decoder was: "+decoder, decoder.getFieldValue("p").equals("1"));
            assertTrue(decoder.getFieldValue("q").equals("2"));

            url = "http://myhost:82/mojo/nixon/index.html?p=1&q=2#target?this=illegal&will=beIgnored";
            decoder = new URLDecoder(url);
            assertTrue(decoder.getFieldValue("p").equals("1"));
            assertTrue(decoder.getFieldValue("q").equals("2"));

            url = "?p=1&q=2#target?this=illegal&will=beIgnored";
            decoder = new URLDecoder(url);
            assertTrue(decoder.getFieldValue("p").equals("1"));
            assertTrue(decoder.getFieldValue("q").equals("2"));

        } catch (SecurityException e){
            fail("testCheckCallStackCaller");
        }
    }

    public static void main(String [] args){
        junit.textui.TestRunner.run(suite());
    }
}