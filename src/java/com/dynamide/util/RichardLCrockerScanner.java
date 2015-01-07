package com.dynamide.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class RichardLCrockerScanner {

    public RichardLCrockerScanner() {

    }

    enum TAG {NONE, SONG, LATINLYRICS, TRANSLATION, LITURGICALREFS, VERSEREFS}

    private TAG lastTag = TAG.NONE;

    private String outFilename;

    private List<String> lines = new ArrayList<String>();

    private List<String> list;

    private int idx = -1;
    private int id = 0;

    private boolean listDepleted(){
        if (list.size() <= idx){
            return true;
        }
        return false;
    }

    private static void append(StringBuffer b, String s){
        b.append(s).append("\r\n");
    }

    private void normalizeList(){
        List<String> resultList = new ArrayList<String>();
        for (String line: list){
            line = line.replaceAll("\\&", "&amp;");
            resultList.add(line);
        }
        list = resultList;
        String base = "/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2";
        String filename = "list" + this.outFilename;
        FileTools.linesToFile(list, base, filename);
    }

    public void run(String filename, String outFilename) throws FileNotFoundException {
        this.outFilename = outFilename;
        File f = new File(filename);
        list = FileTools.fileToLines(f);
        normalizeList();
        String chunk;
        List<String> lines;
        StringBuffer b = new StringBuffer();
        b.append("<?xml version=\"1.0\"?>\n" +
                        "<!DOCTYPE songs>\n" +
                        "<songs>");

        /*
                song:
                latin-lyrics:
                verse-refs:
                liturgical-refs:
                latin-translations:
         */

        //start off and look for first song, skipping all front matter:
        expect("song:");

        while ( ! listDepleted()) {
            chunk = expect("latin-lyrics:"); //song#
            append(b, "<song id='" + chunk.trim() + "'>");

            chunk = expect("verse-refs:");
            chunk = chunk.trim().replaceAll("\\r\\n", "\r\n&lt;br />");  //stash the <br /> as &lt;br /> so it is plain text to the <ref> element, and not a child BR element.
            append(b, "<latin-lyrics>" + chunk + "</latin-lyrics>");

            lines = expectList("liturgical-refs:");
            append(b, "<verse-refs>");
            for (String line : lines) {
                append(b, "<ref>" + line + "</ref>");
            }
            append(b, "</verse-refs>");

            lines = expectList("latin-translations:");
            append(b, "<liturgical-refs>");
            for (String line : lines) {
                append(b, "<ref>" + line + "</ref>");
            }
            append(b, "</liturgical-refs>");

            chunk = expect("song:");
            chunk = chunk.trim().replaceAll("\\r\\n", "\r\n&lt;br />");
            append(b, "<latin-translation>" + chunk + "</latin-translation>");
            append(b, "</song>");
        }
        b.append("</songs>\r\n");
        System.out.println("b:\r\n"+b.toString());
        FileTools.saveFile("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com", outFilename, b.toString());
    }

    public void run2(String filename, String outFilename) throws FileNotFoundException {
        System.out.println("processing file: "+filename);
        this.outFilename = outFilename;
        File f = new File(filename);
        list = FileTools.fileToLines(f);
        normalizeList();
        String chunk;
        List<String> lines;
        StringBuffer b = new StringBuffer();
        b.append("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE songs>\n" +
                "<songs>");

        doit(b);
        b.append("</songs>\r\n");
        System.out.println("b:\r\n"+b.toString());
        FileTools.saveFile("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/db", outFilename, b.toString());
    }

    private static int linesToInt(List<String> lines){
        String all = "";
        for (String line: lines) {
            all += line;
        }
        String trimmed = all.trim();
        if (trimmed.length()==0){
            return 0;
        }
        return Integer.parseInt(trimmed);
    }

    private static String linesToPRE(List<String> lines){
        int lastLine = lines.size()-1;
        for (int i=lines.size()-1; i>-1; i--){
            if (lines.get(i).trim().length()==0){
                lastLine--;
            } else {
                break;
            }
        }


        //stash the <br /> as &lt;br /> so it is plain text to the <ref> element, and not a child BR element.
        String all = "";
        int i = 0;
        for (String line: lines) {
            if (i>lastLine){
                break;
            }
            if (i>0){
                all += "\r\n&lt;br />";
            }
            all += line;
            i++;
        }
        return all.trim();
    }

    /*
    } else {
        for (String line: lines) {
            System.err.println("front matter skipped: " + line);
        }
    }
     */
    private static void processLastTag(TAG lastTag, List<String> lines, int songIndex, StringBuffer b){
        switch (lastTag) {
            case SONG:
                if (songIndex > 1) {
                    append(b, "</song>");
                }
                append(b, "<song id='" + linesToInt(lines) + "' data-idx='"+songIndex+"'>");
                break;
            case LATINLYRICS:
                append(b, "<latin-lyrics>" + linesToPRE(lines) + "</latin-lyrics>");
                break;
            case VERSEREFS:
                append(b, "<verse-refs>");
                for (String line : lines) {
                    if (line.trim().length()>0) {
                        append(b, "<ref>" + line + "</ref>");
                    }
                }
                append(b, "</verse-refs>");
                break;
            case LITURGICALREFS:
                append(b, "<liturgical-refs>");
                for (String line : lines) {
                    if (line.trim().length()>0) {
                        append(b, "<ref>" + line + "</ref>");
                    }
                }
                append(b, "</liturgical-refs>");
                break;
            case TRANSLATION:
                append(b, "<latin-translation>" + linesToPRE(lines) + "</latin-translation>");
                break;
            case NONE:
                break;
        }

    }

    private void doit(StringBuffer b){
        int songIndex = 0;
        while (id < list.size()) {
            String current = list.get(id);
            TAG tagEnum = tagToEnum(current);
            if (tagEnum.equals(TAG.NONE)) {
                // keep scanning.
                lines.add(current);
            } else {
                if (lastTag.equals(TAG.SONG)){
                    songIndex++;
                }
                processLastTag(lastTag, lines, songIndex, b);   // lastTag is initialized as TAG.NONE
                lines.clear();
                lastTag = tagEnum;
            }
            id++;
        }
        processLastTag(lastTag, lines, songIndex, b);   // lastTag is initialized as TAG.NONE
        append(b, "</song>\r\n");
    }


    private List<String> expectList(String tag){
        List<String> res = new ArrayList<String>();
        idx++;

        while (list.size() > idx) {
            String current = list.get(idx);
            System.out.println("list line "+idx+":"+current);
            if (current.indexOf(tag) == 0) {
                System.out.println("    list found: "+res);
                return res;
            } else {
                if (!Tools.isBlank(current)){
                    res.add(current);
                }
            }
            idx++;
        }
        return res;
    }

    /** @return everything up to, but not including the line with the tag it finds. */
    private String expect(String tag){
        String res = "";
        idx++;
        while (list.size() > idx) {
            String current = list.get(idx);
            System.out.println("line "+idx+":"+current);
            if (current.indexOf(tag) == 0) {
                System.out.println("    found: "+res);
                return res;
            } else {
                res = res + "\r\n" + current;
            }
            idx++;
        }
        return res;
    }
    

    private TAG tagToEnum(String tag){
        tag = tag.trim();
        if (tag.equalsIgnoreCase("song:")){
            return TAG.SONG;
        } else if (tag.equalsIgnoreCase("latin-lyrics:")) {
            return TAG.LATINLYRICS;
        } else if (tag.equalsIgnoreCase("verse-refs:")) {
            return TAG.VERSEREFS;
        } else if (tag.equalsIgnoreCase("liturgical-refs:")) {
            return TAG.LITURGICALREFS;
        } else if (tag.equalsIgnoreCase("latin-translations:")) {
            return TAG.TRANSLATION;
        }
        return TAG.NONE;
    }

    public static void main(String[]args) throws FileNotFoundException {
        new RichardLCrockerScanner().run2("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2/mode34.txt",        "mode34.db.xml");
        new RichardLCrockerScanner().run2("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2/mode5.saints.txt",  "mode5.saints.db.xml");
        new RichardLCrockerScanner().run2("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2/mode5.txt",         "mode5.db.xml");
        new RichardLCrockerScanner().run2("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2/modes17.txt",       "modes17.db.xml");
        new RichardLCrockerScanner().run2("/Users/vcrocla/src/doc.lar/projects/nate/richardlcrocker.com/batch2/modes28.txt",       "modes28.db.xml");
    }
}
