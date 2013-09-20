package com.dynamide.resource;

import java.util.*;

import com.dynamide.resource.*;

/** Test the different memory consumption stats for several strategies of storing context information.

 * Typical run:
 * <pre>
 *
 * java com.dynamide.resource.ContextTest
 *
 Usage for 10000 HashMaps
memory: 269864 end: 2653064 per object: 238

Usage for 10000 FileRecords
memory: 2654728 end: 4042312 per object: 138

Usage for 10000 ContextNodes
memory: 4043664 end: 7944352 per object: 390
 * </pre>

 */
public class ContextProfiletest{

    private static class FileRecord {
        public String name = "";
        public boolean incache = false;
        public byte[] bytes;
        public String content;
    }

    public static void main(String [] args) throws Exception {
        long st = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        HashMap maps = new HashMap();
        int COUNT = 10000;
        for (int i=0; i < COUNT; i++) {
            HashMap m = new HashMap();
            m.put("hello", "world");
            maps.put(""+i, m);
        }
        long     en = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        System.out.println("\r\nUsage for "+COUNT+" HashMaps");
        System.out.println("memory: " + st + " end: "+en+" per object: "+((en-st)/COUNT));

        st = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        for (int i=0; i < COUNT; i++) {
            FileRecord fr = new FileRecord();
            maps.put("fr"+i, fr);
        }
        en = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        System.out.println("\r\nUsage for "+COUNT+" FileRecords");
        System.out.println("memory: " + st + " end: "+en+" per object: "+((en-st)/COUNT));

        st = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        for (int i=0; i < COUNT; i++) {
            ContextNode c = new ContextNode("test");
            c.bindAttribute("hello", "world");
            maps.put("c"+i, c);
        }
        en = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        System.out.println("\r\nUsage for "+COUNT+" ContextNodes");
        System.out.println("memory: " + st + " end: "+en+" per object: "+((en-st)/COUNT));

    }

}