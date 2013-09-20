// java com.dynamide.util.IETidy c:/java/pagepostIE.html debug

package com.dynamide.util;

import java.util.HashSet;
import java.util.Set;

/*
 * New strategy:
 *  write a token() method.  returns comment, text, elementopen, elementclose, elementcomplete
 *  don't need to handle CDATA, processing instruction, etc.
 *
 *    <html><!-- comment --><body attr=mojo>some text <input name=i2> <p></p> </body></html>
 *   returns tokens:
 *
 *    <html>               push
 *    <!-- comment -->     no push
 *    <body attr=mojo>     push
 *    "some text "         no push
 *    <input name=i2>      push
 *    " "
 *    <p></p>
 *    " "
 *    </body>
 *    </html>
 *
 *
 *  Then, you just walk the elements and push/pop to make sure they are matched.
 */

public class IETidy {

    //states:
    private static final int OTHER = 1;
    private static final int READING_TAG = 2;
    private static final int READING_HAKKA = 3;
    private static final int READING_CLOSETAG = 4;
    private static final int IN_ELEMENT = 5;

    // %% todo: make this loadable from a resource.
    public static Set loadUnclosedElementSet() {
        //"P" is unclosable, but IE pairs it with a close element always.
        //Same with "SCRIPT", which it seems to do correctly.
        //others are commented out since I haven't needed them.
        //The generated html breaks either way (commented or not) if the browser messes up.
        Set unclosedElementSet = new HashSet();
        //unclosedElementSet.add( "AREA" );
        //unclosedElementSet.add( "BASE" );
        unclosedElementSet.add( "BR" );
        //unclosedElementSet.add( "COL" );
        unclosedElementSet.add( "HR" );
        unclosedElementSet.add( "IMG" );
        unclosedElementSet.add( "INPUT" );
        unclosedElementSet.add( "LINK" );
        unclosedElementSet.add( "META" );
        //unclosedElementSet.add( "PARAM" );
        return unclosedElementSet;
    }

    public static boolean closeSingleTag(String tag, Set set, boolean debug){
        String utag = tag.toUpperCase();
        if (set.contains(utag)){
            if (debug) System.out.println("closeSingleTag: "+tag);
            return true;
        }
        if (debug) System.out.println("don't closeSingleTag: "+tag);
        return false;
    }


   /*
    *       <mojo at1 at2='foo' at3/>
    *       <mojo
    *          ^ READING_TAG
    *       <mojo
    *            ^ IN_ELEMENT
    *       <mojo at1   at2='foo' at3/>
    *                ^!    ^-1
    *
    */
    public static String parse(String instr){
        return parse(instr, false);
    }

    public static String parse(String instr, boolean debug){
        FileTools.saveFile("/java", "IETidyIN.html", instr);
        Set set = loadUnclosedElementSet();
        StringBuffer in = new StringBuffer(instr);
        StringBuffer out = new StringBuffer();
        String tag = "";
        int state = OTHER;
        int tagStart = 0;
        int currentAttributeStart = -1;
        String currentAttribute = "";

        char attrQuoteChar = ' ';
        char c;
        char cPeek;
        int i = 0;
        int instr_len = instr.length();
        while ( i < instr_len ) {
            c = in.charAt(i);
            switch ( c ) {
            case '<' :
                out.append(c);
                c = in.charAt(i+1);  //illegal to end a file with '<', so this is OK.
                if ( c == '/' ) {
                    //in a close tag.
                    i++;
                    out.append('/');
                    state = READING_CLOSETAG;
                }  else {
                    state = READING_TAG;
                }
                tag = "";
                tagStart = i+1;
                currentAttributeStart = -1;
                break;
            case '>' :
                currentAttributeStart = -1;
                if (state == READING_TAG){
                    tag = in.substring(tagStart, i);
                    if (debug) System.out.println(" TAG,>: "+tag);
                } else if (state == READING_CLOSETAG){
                    tag = in.substring(tagStart, i);
                    if (debug) System.out.println(" CLOSETAG,>: "+tag);
                }
                if ( closeSingleTag(tag, set, debug) ) {
                    out.append(" />");
                } else {
                    out.append('>');
                }
                state = OTHER;
                break;
            case '-' :
                out.append('-');
                // look back for <!--
                if ( i > 2 && in.charAt(i-1) == '-' && in.charAt(i-2) == '!' && in.charAt(i-3) == '<' ) {
                    //scan for close comment: -->
                    while ( true ) {
                        i++;            // %% todo: do I have to lop one off since there is a ++ at end, or do I do a continue...?
                        c = in.charAt(i);
                        out.append(c);
                        if ( c == '>' && i > 1 && in.charAt(i-1) == '-' && in.charAt(i-2) == '-'  ) {
                            currentAttributeStart = -1;
                            state = OTHER;
                            break;
                        }
                    }
                }
                break;
            case ' '  :
            case '\r' :
            case '\n' :
            case '\t' :
            case '/' :
                if (state == READING_HAKKA){
                    out.append(c);
                    state = READING_TAG;
                    tag = "";
                    tagStart = i+1;
                    currentAttributeStart = -1;
                    break;
                } else if (state == READING_TAG){
                    out.append(c);
                    tag = in.substring(tagStart, i);
                    if (debug) System.out.println(" TAG,/: "+tag);
                    state = IN_ELEMENT;
                    currentAttribute = "";
                    currentAttributeStart = i+1;
                } else if (state == IN_ELEMENT){
                    if ( currentAttributeStart > -1 ) {
                        if (debug){
                            String dbg = "[["+state+","+currentAttributeStart+","+c+"]]";
                            System.out.println("dbg: "+dbg);
                        }
                        out.append("='true'");
                        currentAttributeStart = -1;
                    }
                    out.append(c);
                } else {
                    currentAttributeStart = -1;
                    out.append(c);
                }
                break;
            case '=' :
                currentAttributeStart = -1;
                out.append('=');
                if ( i < instr_len -1 ) {
                    cPeek = in.charAt(i+1);
                    //out.append("\r\n******** cPeek is: "+cPeek);
                    if ( cPeek != '\'' && cPeek != '\"' ) {
                        //out.append("\r\n cPeek is NOT a quote");
                        attrQuoteChar = '\'';
                        out.append(attrQuoteChar);
                        whileLoop: while ( true ) {
                            i++;
                            c = in.charAt(i);
                            switch ( c ) {
                            case ' '  :
                            case '\r' :
                            case '\n' :
                            case '\t' :
                                out.append(attrQuoteChar);
                                out.append(c);
                                break whileLoop;
                            case '>' :
                                out.append(attrQuoteChar);
                                ////////tag = in.substring(tagStart, i);
                                if (debug) System.out.println(" TAG,=*>: "+tag);
                                if ( closeSingleTag(tag, set, debug) ) {
                                    out.append(" />");
                                } else {
                                    out.append('>');
                                }
                                state = OTHER;
                                break whileLoop;
                            default   :
                                out.append(c);
                            }
                        }
                    } else {
                        //out.append("\r\n cPeek was a quote");
                        attrQuoteChar = cPeek;
                        out.append(attrQuoteChar);
                        i++;
                        whileLoop2: while ( true ) {
                            i++;
                            c = in.charAt(i);
                            out.append(c);
                            if (c == attrQuoteChar) {
                                break whileLoop2;
                            }
                        }
                    }
                }
                break;
            default:
                out.append(c);
                //System.out.print(""+c);
                currentAttributeStart = i+1;
            }
            i++;
        }
        String res = out.toString();
        FileTools.saveFile("/java", "IETidyOUT.xml", res);
        return res;
    }

    public static void main(String [] args){
        if (args.length == 0){
            System.out.println("Usage: com.dynamide.util.IETidy <filename> [-debug]");
            System.exit(0);
        }
        System.out.println(parse(FileTools.readFile(args[0]), args.length>1 ));
    }
}
