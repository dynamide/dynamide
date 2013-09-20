package com.dynamide.test;

import java.util.Map;
import java.util.TreeMap;

import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

public class HashtableTest {
    public static void main(String[]args){
        test(10);
        test(100);
        test(2000);
    }

    private static void test(int LOOPS){
        System.out.println("\r\nLOOPS: "+LOOPS);
        Map h = new TreeMap();
        String [] ar = new String[LOOPS];
        StringList sl = new StringList(2*LOOPS);
        String str, strres;
        for (int i = 0; i<LOOPS; i++){
            str = "myStringThatIsIndexed:"+i;
            ar[i] = str;
            h.put(str, str);
            sl.addObject(str, str);
        }

        long start = Tools.now().longValue();
        for (int i = 0; i<LOOPS; i++){
            str = "myStringThatIsIndexed:"+i;
            strres = (String)h.get(str);
        }
        System.out.println("hashtable searches: "+(Tools.now().longValue() - start));

        start = Tools.now().longValue();
        for (int i = 0; i<LOOPS; i++){
            str = "myStringThatIsIndexed:"+i;
            strres = findInArray(ar,str);
            if ( strres == null ) {
                System.out.println("ERROR: string not found");
                return;
            }
        }
        System.out.println("String[] searches: "+(Tools.now().longValue() - start));

        start = Tools.now().longValue();
        for (int i = 0; i<LOOPS; i++){
            str = "myStringThatIsIndexed:"+i;
            strres = (String)sl.get(str);
            if ( strres == null ) {
                System.out.println("ERROR: string not found");
                return;
            }
        }
        System.out.println("StringList searches: "+(Tools.now().longValue() - start));
    }

    private static String findInArray(String[]ar, String target){
        int arlen = ar.length;
        for (int i = 0; i<arlen; i++){
            if ( ar[i].equals(target) ) {
                return ar[i];
            }
        }
        return null;
    }

}
