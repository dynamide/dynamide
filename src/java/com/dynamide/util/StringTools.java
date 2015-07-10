package com.dynamide.util;

import java.util.Vector;

public class StringTools {

    public final static int PAD_LEFT = 0;
    public final static int PAD_RIGHT = 1;
    public final static int PAD_CENTER = 2;

    public final static int SUBSTRING_END = -1;


    public final static String string(Object source){
        if (source == null){
            return "";
        }
        return source.toString();
    }

    public final static String fill(char fillChar, int width){
        if (width<=0){
            return "";
        }
        java.lang.StringBuffer result = new java.lang.StringBuffer(width);
        for (int i = 0; i < width; i++){
            result.append(fillChar);
        }
        return result.toString();
    }

    public final static String fill(String fillString, int width){
        if (width<=0){
            return "";
        }
        java.lang.StringBuffer result = new java.lang.StringBuffer(width*fillString.length());
        for (int i = 0; i < width; i++){
            result.append(fillString);
        }
        return result.toString();
    }

    public static String pad(String source, int width, int alignment){
        java.lang.StringBuffer p = new java.lang.StringBuffer(width);
        int slen = source.length();
        switch ( alignment ) {
        case PAD_RIGHT :
            p.append(fill(' ', width - slen));
            p.append(source);
            break;
        case PAD_CENTER :
            int diff = width - slen;
            if ( diff > 0 ) {
                int lp = diff / 2;
                int rp = diff - lp;
                p.append(fill(' ', lp));
                p.append(source);
                p.append(fill(' ', rp));
            } else {
                return source;
            }
            break;
        case PAD_LEFT :
        default :
            p.append(source);
            p.append(fill(' ', width - slen));
        }
        return p.toString();
    }

    /** @return All elements, except does not add an extra element if the string ends with separator.
     */
    public static Vector parseSeparatedValues(String source, String separator){
        return parseSeparatedValues(source, separator, false);
    }

    /** @return an extra element if the string ends with separator and param wantEmptyLastItem is true.
     */
    public static Vector parseSeparatedValues(String source, String separator, boolean wantEmptyLastItem){
        Vector result = new Vector();
        if ( separator.length() == 0 ) {
            result.addElement(source);
            return result;
        }
        if ( source == null || source.length()==0 ) {
            return result;
        }
        int start = 0;
        int pos;
        pos = source.indexOf(separator);
        while ( pos > -1 ) {
            result.addElement(source.substring(start, pos));
            start = pos+separator.length();
            pos = source.indexOf(separator, start);
        }
        String remaining = source.substring(start);
        if (remaining.length() == 0 && !wantEmptyLastItem)
            return result;
        result.addElement(remaining);
        return result;
    }

    //designed for utility, not speed.
    public static String printArray(String[] arr, String delim){
        String result = "";
        for ( int i = 0; i<arr.length; i++ ) {
            if ( i>0 ) {
                result += delim;
            }
            result += arr[i];
        }
        return result;
    }

    public static String remove(String substring, String source){
        return searchAndReplaceAll(source, substring, "");
    }

    /** Search for a section of text marked with string identifying blocks, and replace
     *   with a single string between the markers.  Removes the markers from the result.
     */
    public static String searchAndReplace(String source, String startStr, String endStr, String replaceWith){
       return searchAndReplace(source, startStr, endStr, replaceWith, false);
    }

    /** Search for a section of text marked with string identifying blocks, and replace
     *   with a single string between the markers.
     *  @param keepStartAndEndStrs Optionally leave the markers in place
     *   by setting this to true, or remove them from the result with false.
     */
    public static String searchAndReplace(String source, String startStr, String endStr,
                                   String replaceWith, boolean keepStartAndEndStrs){
        int repStart = source.indexOf(startStr);
        int repEnd =   source.indexOf(endStr);
        if ( repStart >=0 && repEnd >= 0 ) {
            if ( keepStartAndEndStrs ) {
                repStart += startStr.length();
            } else {
                repEnd += endStr.length();
            }
            return  source.substring(0, repStart)  //this had a bug where it did repStart+1, and was giving an extra char. 991105 Laramie.
                  + replaceWith
                  + source.substring(repEnd);
        }
        return source;
    }

    /** Search for a section of text marked with string identifying blocks, and replace
     *   with a single string between the markers.
     *  @param keepStartAndEndStrs Optionally leave the markers in place
     *   using the keepStartAndEndStrs parameter, or remove them from the result.
     */
    public static String searchAndReplaceAll(String source, String startStr, String endStr,
                                   String replaceWith, boolean keepStartAndEndStrs, boolean doAll){
        int repStart, repEnd, endIndex;
        String temp;
        repStart = source.indexOf(startStr);
        repEnd =   source.indexOf(endStr, repStart);

        while ( repStart >=0 && repEnd >= 0 ) {
            if ( keepStartAndEndStrs ) {
                repStart += startStr.length();
            } else {
                repEnd += endStr.length();
            }
            temp = source.substring(0, repStart)
                            + replaceWith;
            endIndex = temp.length();
            source = temp + source.substring(repEnd);
            if ( ! doAll ) {
                return source;
            } else {
                //If we kept endStr, bump past it so we don't find it again.
                if ( keepStartAndEndStrs )
                    endIndex += endStr.length();
                repStart = repEnd;
                repStart = source.indexOf(startStr, endIndex);
                repEnd =   source.indexOf(endStr,   repStart);
            }
        }
        return source;
    }

    /** Overloaded version with ignoreCase defaulted to false.  This version
     *  searches case sensitively
     */
    public static String searchAndReplaceAll(String source, String searchFor, String replaceWith){
        return searchAndReplaceAll(source, searchFor, replaceWith, false);
    }

    /**  Search the "source" string for instances of the "searchFor" string, and replace all instances with
      *  "replaceWith".  "ignoreCase" will perform case insensitive searches.
      *  @return A String with all replacements made.
      */
    public static String searchAndReplaceAll(String source, String searchFor,
                                             String replaceWith, boolean ignoreCase){
        if ( source == null ){
            return null;
        }
        if ( searchFor == null || replaceWith == null ) {
            return source;
        }
        java.lang.StringBuffer dest = new java.lang.StringBuffer(source.length());  //get something of the same size initially.
        //System.out.println("searchAndReplaceAll, source: "+source+ " for: "+searchFor + " with: "+replaceWith);
        String sourceBuff;
        if (ignoreCase){
            sourceBuff = source.toUpperCase();
            searchFor = searchFor.toUpperCase();
        }
        else
            sourceBuff = source;
        boolean more = true;
        int index = -1;
        int sourceStart = 0;
        int searchForLen = searchFor.length();
        while(more){
            index = sourceBuff.indexOf(searchFor, sourceStart);
            if (index > -1){
                dest.append(source.substring(sourceStart, index));
                dest.append(replaceWith);
                sourceStart = index + searchForLen;
            } else {
                more = false;
            }
        }
        if (sourceStart < source.length())
            dest.append(source.substring(sourceStart)); //append rest of string.
        return dest.toString();
    }

    /** Returns a substring of source containing everything up to
     *  but excluding endsBefore; if does not end in endsBefore, returns "".
     */
    public static String substring(String source, String endsBefore){
        if ( source.length()>0 ) {
            int i = source.indexOf(endsBefore);
            if ( i > -1 ) {
                return source.substring(0, i);
            }
        }
        return "";
    }

    /** Returns substring delimited by delimiter, counting through the list
     *  from zero, stopping at index.  For example, substring("a_b_c", "_", 1)
     *  returns "b"; note: substring("_a_b_c", "_", 0) returns "", since the leading
     *  delimiter makes an empty element.
     */
    public static String substring(String source, String delimiter, int index){
        Vector values = parseSeparatedValues(source, delimiter);
        if ( values.size()>index ) {
            return ((String)values.elementAt(index)).trim();
        }
        return "";
    }

    /** Returns substring between start and end strings. If end string is "", cut to end of string.
     *  For example, substring("mojo=bar.thing", "mojo=", "")
     *  returns "bar.thing"
     *  whereas substring("mojo=bar.thing", "mojo=", ".thing")
     *  returns "bar".
     */
    public static String substring(String source, String start, String end){
        int iEnd;
        if (end.length()==0){
            iEnd = source.length();
        } else {
            iEnd = source.indexOf(end);
            if (iEnd == -1) {
               iEnd = source.length();
            }
        }
        int iStart = source.indexOf(start);
        if (iStart == -1) {
            iStart = 0;
        } else {
            iStart = iStart + start.length();
        }
        return source.substring(iStart, iEnd);
    }

    public static String substring(String source, int start){
        return substring(source, start, SUBSTRING_END);
    }

    public static String substring(String source, int start, int end){
        if ( source == null ) {
            return "";
        }
        if ( source.length() == 0  ) {
            return "";
        }
        if ( end > source.length() || end <= SUBSTRING_END ) {
            return source.substring(start);
        }
        return source.substring(start, end);
    }

    public static String rtrim(String source){
        int e = source.length();
        char c;
        while ( true ){
            if (e<=1) {
                break;
            }
            c = source.charAt(e-1);
            if ( (int)c < 33 ) {
                e--;
            } else {
                break;
            }
        }
        source=source.substring(0,e);
        return source;
    }

    public static String ellipses(String source, int maxLength){
        return truncate(source, maxLength, "...");
    }

    /** Indents a block by wrapping it in an HTML TABLE tag, with two columns, the left column being the
     *  indent width, in pixels.  The entire table can be sized by passing a width, either a number or a
     *  percentage, as a string, for example indent(body, 40, "100%") or indent(body, 40, "200").
     *  If you pass an empty string for "width", the table will not have a width attribute.
     *  The indent cell is filled with a transparent GIF image that is present in all Dynamide installations.
     *   @see #indent(String,int,String,int,String)
     */
    public static String indent(String body, int indent, String width, int padding){
        return indent(body, indent, width, padding, "/static/dynamide/images/transparent.gif");
    }


    /** Indents a block by wrapping it in an HTML TABLE tag, similar to indent(String,int,String),
     * but allows you to specify the location of the transparent image used to fill the indent cell.
     *   @see #indent(String,int,String,int)
     */
    public static String indent(String body, int indent, String tablewidth, int padding, String imageLocation){
        if ( indent == 0 ) {
            return body;
        }
        String widthparam = tablewidth.length()==0 ? "" : " width='"+tablewidth+"' ";
        String imgWidthparam = " width='"+indent+"' ";
        String  sindent =
                "<td"+imgWidthparam+"><img src='"
                +imageLocation
                +"' "+imgWidthparam+" height='1'/>"
                +"</td>";

        StringBuffer b = new StringBuffer();
        b.append("<table border='0' cellpadding='"+padding+"' cellspacing='0' ");
        b.append(widthparam);
        b.append("><tr>");
        b.append(sindent);
        b.append("<td>");
        b.append(body);
        b.append("</td></tr></table>");
        return b.toString();
    }

    public static String spacer(com.dynamide.Session session, String width){
        return spacer(session, width, "1");
    }

    public static String spacer(com.dynamide.Session session, String width, String factor){
        if ( width == null || width.length() ==0 ) {
            return "";
        }
        int calcWidth = Tools.stringToInt(width) * Tools.stringToIntSafe(factor, 1);
        String transparentImgSrc = session.getInclude("resources/images/transparent.gif");
        String transparentImg = "<img src='"+transparentImgSrc+"' border='0' height='1' width='"+calcWidth+"'/>";
        return transparentImg;
    }


    public static String truncate(String source, int maxLength, String ellipses){
        if ( source.length()>maxLength ) {
            return source.substring(0,maxLength)+ellipses;
        }
        return source;
    }

    public static String truncate(String source, int maxLength){
        return truncate(source, maxLength, "");
    }

    //todo: add tests:  com.dynamide.util.StringTools.wrap("hello world mojo nixon\r\nthis new line is really long", 4);
    public static String wrap(String source, int lineMinLength){
        int p = 0;
        int col = 0;
        StringBuffer result = new StringBuffer();
        int len = source.length();
        while ( p < len ) {
            char c = source.charAt(p);
            result.append(c);
            p++;
            col++;
            switch (c){
                case ' '  :
                    if (col>lineMinLength){
                        col = 0;
                        result.append("\r\n");
                    }
                    break;
                case '\r' :
                case '\n' :
                    col = 0;
                    break;
            }
        }
        return result.toString();
    }

    /**
     * Chops the package "path" off a class name and returns the "tail." In other
     * words "com.dynamide.reporter.ReportSection" becomes "ReportSection."
     * @param cls The Class to trim
     * @return The class name, without all the package stuff
     */
    public static String trimClass(Class cls) {
        String s = cls.getName();
        int dot = s.lastIndexOf(".");
        if (++dot >= 0) {
            s = s.substring(dot);
        }
        return s;
    }

    public static void test_removeExtraTrailingWakka(){
        removeExtraTrailingWakka(">>");
        removeExtraTrailingWakka("a>>");
        removeExtraTrailingWakka(">");
        removeExtraTrailingWakka("a");
        removeExtraTrailingWakka("");
        removeExtraTrailingWakka("a b c >>");
        removeExtraTrailingWakka("a b c > >");
        removeExtraTrailingWakka("a b c >d>");
        removeExtraTrailingWakka("a b c >d >a");
        removeExtraTrailingWakka("a b c >d > a");
        removeExtraTrailingWakka("a b c >d > a ");
    }

    public static String removeExtraTrailingWakka(String expandedHtmlText){
        //in the case where the spurrious char was actually a '>', it won't get removed above.
        String after = expandedHtmlText;
        int elen = expandedHtmlText.length();
        if ( elen-1 >=0 && expandedHtmlText.charAt(elen-1) == '>' ) {
            int p = elen -2;//bump prior to last wakka and look for prev wakka.
            boolean foundWakkaFirst = false;
            boolean foundOtherFirst = false;
            boolean keepGoing = true;
            while ( p >=0 & keepGoing ) {
                char c = expandedHtmlText.charAt(p);
                switch (c){
                    case '>'  :
                        foundWakkaFirst = true;
                        keepGoing = false;
                        break;
                    case ' '  :
                    case '\r' :
                    case '\n' :
                    case '\t' :
                        p--;
                        break;
                    default:
                        foundOtherFirst = true;
                        keepGoing = false;  //for any other char, stop, don't remove char
                }
            }
            if ( foundWakkaFirst ) {
                after = expandedHtmlText.substring(0, p+1);
            }
        }
        System.out.println("BEFORE: '"+expandedHtmlText+"' AFTER: '"+after+'\'');
        return after;
    }
    public static String makeLineNumbers(String source, int linenum, int charnum, boolean html){
        if (source==null){
            return "";
        }
        StringBuffer result = new StringBuffer();
        Vector v = StringTools.parseSeparatedValues(source, "\n");
        String linenumStr;
        int v_size = v.size();
        for ( int i = 0; i<v_size; i++ ) {
            String s = (String)v.elementAt(i);
            s=StringTools.rtrim(s);
            linenumStr = StringTools.pad(""+(i+1)+':', 5, StringTools.PAD_RIGHT)+" ";
            if (i == linenum-1){
                if (html) {
                    result.append("<span class='sourceError'><font color='red'><b>");
                    result.append(linenumStr);
                    result.append(s);
                    result.append("</b></font></span>");
                } else {
                    result.append("===>"+s);
                }
            } else {
                if (html) {
                    //result.append("<b>");
                    result.append(linenumStr);
                    result.append(s);
                    //result.append("</b>");
                } else {
                    result.append(s);
                }
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static String makeLineNumbers(String source, int linenum){
        return makeLineNumbers(source, linenum, -1, true);
    }

    public static String makeLineNumbers(String source){
        if (source==null){
            return "";
        }
        return makeLineNumbers(source, -1, -1, true);
    }

    public static boolean isEmpty(String in){
    	if (in == null)
    		return true;
    	if (in.length()==0)
    		return true;
    	return false;
    }

    public static boolean notEmpty(String in){
    	return !isEmpty(in);
    }

    public static boolean isValidXHTML(String src){
        try {
            String res = com.dynamide.JDOMFile.prettyPrintHTML(src,    //String html
                                "true",   //String newlines -- Netscape can't handle superlong lines.
                                "false",    //String trim    -- save time by not trimming
                                true,      //boolean xhtml -- don't break DOM parser in IDE.
                                false,      //boolean expandEmpty -- not needed for this test
                                false,    //boolean indent
                                false);   //hide errors  --only do if explicitly asked
            return true;
        } catch (com.dynamide.XMLFormatException e) {
            return false;
        }
    }

    /** Does NOT escape ampersands -- use escapeAmpersands for that, either before or after calling this function.
     */
    public static String escape(String msg){
        if (msg==null){
            return "";
        }
        msg = StringTools.searchAndReplaceAll(msg, "<", "&lt;");
        msg = StringTools.searchAndReplaceAll(msg, ">", "&gt;");
        return msg;
    }

    public static String escapeForWebmacro(String msg){
        if (msg==null){
            return "";
        }
        msg = StringTools.searchAndReplaceAll(msg, "$", "\\$");
        msg = StringTools.searchAndReplaceAll(msg, "#", "\\#");
        msg = StringTools.searchAndReplaceAll(msg, "{", "\\{");
        msg = StringTools.searchAndReplaceAll(msg, "}", "\\}");
        return msg;
    }

    public static String escapeForJavascript(String msg){
        if (msg==null){
            return "";
        }
        msg = escape(msg);
        msg = StringTools.searchAndReplaceAll(msg, "\'", "&#39;");
        msg = StringTools.searchAndReplaceAll(msg, "\"", "&quot;");
        return msg;
    }


    public static String escapeAmpersands(String msg){
        if (msg==null){
            return "";
        }
        msg = StringTools.searchAndReplaceAll(msg, "\u0026", "\u0026amp;");
        return msg;
    }

    public static String unescape(String msg){
        if (msg==null){
            return "";
        }
        msg = StringTools.searchAndReplaceAll(msg, "&lt;", "<");
        msg = StringTools.searchAndReplaceAll(msg, "&gt;", ">");
        msg = StringTools.searchAndReplaceAll(msg, "&amp;", "&");
        return msg;
    }

    public static void checkSQLSafe(String value)
    throws Exception {
        if (value.indexOf(";")>-1){
            throw new Exception("UNSAFE: value rejected because of unsafe sql characters: "+value);
        }
    }

    public static String sql(Object value)
    throws Exception {
        if (value == null){
            return "";
        }
        return sql(value.toString());
    }

    public static String sql(String value)
    throws Exception {
        checkSQLSafe(value);
        return escapeForPostgres(value);
    }

    public static String escapeForPostgres(String val){
        val = StringTools.searchAndReplaceAll(val, "\\", "\\\\\\\\");
        val = StringTools.searchAndReplaceAll(val, "'", "\\'");
        return val;
    }

    public static String identifier(String source){
        StringBuffer r = new StringBuffer();
        int len = source.length();
        int i = 0;
        char c;
        int ic;
        while ( i<len ) {
            c = source.charAt(i);
            if ( i==0 ) {
                if (Character.isJavaIdentifierStart(c)) {
                    r.append(c);
                }
            } else  {
                if (c == '.'){
                    r.append('_');
                } else if ( Character.isJavaIdentifierPart(c) ) {
                    r.append(c);
                }
            }
            i++;
        }
        return r.toString();
    }

    public static String decodeBytea(Object o){
        String res;
        try {
            if ( o == null ) {
                return null;
            }
            if (o instanceof String){
                System.out.println("decodeBytea String");
                return (String)o;
            }
            try {
                System.out.println("trying decodeBytea byte[]");
                res = new String((byte[])o);
                return res;
            } catch (Exception e)  {
                System.out.println("decodeBytea found a non-array");
            }
            //if (java.lang.reflect.Array.getLength(o)>0){
            //    System.out.println("decodeBytea byte[]");
            //    res = new String((byte[])o);
            //} else {
                System.out.println("decodeBytea unknown");
                res = o.toString();
            //}
        } catch (Exception e)  {
            Log.error(StringTools.class, "Error in unescape("+o+") class: "+o.getClass().getName(), e);
            res = (o != null) ? o.toString() : "";
        }
        return res;
    }

    public static String dm_nbsp(String source){
        source = searchAndReplaceAll(source, "<dm_nbsp/>", "&#160;"); // %% Hack I can figure out how to turn off entities.
        source = searchAndReplaceAll(source, "<dm_nbsp />", "&#160;"); // %% Hack I can figure out how to turn off entities.
        source = searchAndReplaceAll(source, "<dm_nbsp></dm_nbsp>", "&#160;"); // %% Hack I can figure out how to turn off entities.
             //basically until then, they transform this [ select onclick="alert('hi')" ] to this: [ select onclick="alert(&apos;hi&apos;)" ]
        return source;
    }

    public static final String CR = "\r";
    public static final String LF = "\n";
    public static final String CRLF = "\r\n";

    /** Formats a string based on the count: zero, one or many.  One variable
     *  is expanded: $count is replaced with the String value of the count sent.
     *  <br/>
     * Examples:
     *  <pre>
     *  plural(0, "no items", "one item", "$count items")
     *    ==> no items
     *
     *  plural(1, "no items", "one item", "$count items")
     *    ==> one item
     *
     *  plural(8, "no items", "one item", "$count items")
     *    ==> 8 items
     * </pre>
     * In a Page or widget source, you could write
     * <pre>
     *  #set $count = $session.getFieldValue("shoppingCartCount")
     *  You have
     *     $tools.plural($count , "no items", "one item", "$count items")
     *     in your basket
     * </pre>
     *  If the field value was 8, the result would be:
     * <pre>
     *       You have 8 items in your basket
     * </pre>
     */
    public static String plural(int count, String zero, String one, String many){
        if (count==0){
            return searchAndReplaceAll(zero, "$count", ""+count);
        } else if (count==1){
            return searchAndReplaceAll(one, "$count", ""+count);
        } else {
            return searchAndReplaceAll(many, "$count", ""+count);
        }
    }

    public static String upperCaseFirstLetter(String src){
        if ( src.length()==0 ) {
            return "";
        }
        char fl = Character.toUpperCase(src.charAt(0));
        return fl+substring(src, 1);
    }



    public static void main(String [] args){
        try{
            String s = com.dynamide.util.FileTools.readFile(args[0]);
            String searchfor = "\r";//args[1];
            String replace = "\r\n";//args[2];
            //System.out.println("Test: "+searchAndReplaceAll(s, "<input", ">", "", false, true));
            System.out.println("Test: "+com.dynamide.util.Tools.showControlChars(searchAndReplaceAll(s, searchfor, replace, false), true));
        } catch (Exception e){
            System.out.println("Usage: StringTools <test-file> <search-for> <replace-with>");
            System.out.println("exception:"+e);
        }
        System.exit(0);
    }


}
