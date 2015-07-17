package com.dynamide.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

import java.io.*;

import java.util.*;

import com.dynamide.DynamideObject;

public class Tools {
    public Tools(){
    }

    public static String NEW_LINE = System.getProperty("line.separator"); //initialize once for VM.

    /** Instead of this convenience funtion, you can just place the integer in a
      * string context, e.g. String s = "my string" + i;  If you just have
      * a single integer with no leading string, call this method, or just use
      * String s = ""+i;
      */
    public static String intToString(int i){
        return ""+i;
    }
    /** Returns a fundamental type int.
     * For StringToInteger, just create a new Integer object, using the
     *  constructor Integer(String).
     * <p>Switched to Integer.parseInt in hopes of avoiding object overhead.
     * MattD 20010414.</p>
     */
    public static int stringToInt(String source){
        if (source == null) {
            throw new NumberFormatException();
        }
        return Integer.parseInt(source.trim());
    }

    public static int stringToIntSafe(Object source, int def){
        if ( source != null ) {
            return stringToIntSafe(source.toString(), def);
        }
        return def;
    }
    /**
     * Returns a fundamental type int. Returns zero if the source string can't be
     * parsed instead of throwing a NumberFormatException.
     */
    public static int stringToIntSafe(String source, int def){
        int retval;
        if (source == null) {
            return def;
        }
        try {
            retval = Integer.parseInt(source.trim());
        } catch (NumberFormatException nfe) {
            retval = def;
        }
        return retval;
    }

    /**  Turn any whitespace delimited list into a Vector.  There is no way to define
      *  an empty element, so if your syntax has an empty element character sequence,
      *  just walk the Vector when this method returns and look for that sequence.  For
      *  example, if you use "", then the characters "" will be an element on the list.
      *  You could search out each occurence and replace with an empty string.
      */
    public static Vector listToVector(String whiteSpaceDelimitedElements){
        StringTokenizer st = new StringTokenizer(whiteSpaceDelimitedElements);
        Vector result = new Vector();
        while (st.hasMoreTokens()){
            result.addElement(st.nextToken());
        }
        return result;
    }

    public static String enumerationToString(Enumeration stringList, String delim){
        String result = "";
        if (stringList == null)
            return "";
        Object elem;
        while(stringList.hasMoreElements()){
            elem = stringList.nextElement();
            result += elem.toString();
            if (stringList.hasMoreElements()){
                result += delim;
            }
        }
        return result;
    }

    public static String vectorToString(Vector stringList){
        return vectorToString(stringList, "\r\n");
    }

    public static String vectorToString(Vector stringList, String delim){
        return vectorToString(stringList, delim, "", 0);
    }

    /** Formats the vector into a String, using the supplied delimiter, and, optionally, the
     *  supplied line number template, which can contain the magic variable "$linenumber", without
     *  the quotes.
     */
    public static String vectorToString(Vector stringList, String delim, String linenumberTemplate, int startingNumber){
        String result = "";
        if (stringList == null){
            return "";
        }
        boolean linenums = linenumberTemplate.length()>0;
        int stringList_size = stringList.size();
        for ( int i = 0; i<(stringList_size-1); i++ ) {
            if (linenums){
                result += StringTools.searchAndReplaceAll(linenumberTemplate, "$linenumber", ""+(i+startingNumber));
            }
            result += stringList.elementAt(i).toString() + delim;
        }
        if (  stringList_size > 0 ) {
            if (linenums){
                result += StringTools.searchAndReplaceAll(linenumberTemplate, "$linenumber", ""+(stringList_size-1+startingNumber));
            }
            result += stringList.elementAt(stringList_size - 1).toString() ;
        }
        return result;
    }

    /** Lists with null elements are not allowed. */
    public static String listToString(List list){
        return collectionToString(list, "\r\n");
    }

    /** Lists with null elements are not allowed. */
    public static String listToString(List list, String delim){
        return collectionToString(list, delim);
    }

    /** Lists with null elements are not allowed. */
    public static String collectionToString(Collection list){
        return collectionToString(list, "\r\n");
    }

    /** Lists with null elements are not allowed. */
    public static String collectionToString(Collection list, String delim){
        StringBuffer b = new StringBuffer();
        Iterator i = list.iterator();
        int c = 0;
        while (i.hasNext()) {
            if (c>0) b.append(delim);
            c++;
            b.append(i.next().toString());
        }
        return b.toString();
    }

    public static String iteratorToString(Iterator i, String delim){
        StringBuffer b = new StringBuffer();
        int c = 0;
        while (i.hasNext()) {
            if (c>0) b.append(delim);
            c++;
            b.append(i.next().toString());
        }
        return b.toString();
    }

    /** Sets with null elements are not allowed. */
    public static String setToString(Set list){
        return setToString(list, "\r\n");
    }

    /** Sets with null elements are not allowed. */
    public static String setToString(Set list, String delim){
        StringBuffer b = new StringBuffer();
        Iterator i = list.iterator();
        int c = 0;
        while (i.hasNext()) {
            if (c>0) b.append(delim);
            c++;
            b.append(i.next().toString());
        }
        return b.toString();
    }

    public static String[] vectorToStringArray(Vector source){
        String [] result;
        int iLen = source.size();
        result = new String[iLen];

        for (int i = 0; i < iLen; i++){
            result[i] = new String(source.elementAt(i).toString());
        }
        return result;
    }

    public static String arrayToString(Object[] array, String delimiter){
        String result="";
        int iLen = array.length;
        for (int i = 0; i < iLen; i++){
            result += array[i].toString();
            if ( i < iLen-1 ) {
                result += delimiter;
            }
        }
        return result;
    }

    public static String arrayToString(Object[] array){
        StringBuffer result= new StringBuffer();
        result.append('{');
        int iLen = array.length;
        for (int i = 0; i < iLen; i++){
            if ( array[i] == null ) {
                result.append("null");
            } else {
                result.append(array[i].toString());
            }
            if ( i < iLen-1 ) {
                result.append(',');
            }
        }
        result.append('}');
        return result.toString();
    }

    /** Creates a TreeMap with a case-insenstive collator using the US Locale.
     */
    public static Map createSortedCaseInsensitiveMap(){
        java.text.Collator usCollator = java.text.Collator.getInstance(java.util.Locale.US);
        usCollator.setStrength(java.text.Collator.PRIMARY);
        Map m = new TreeMap(usCollator);
        return m;
    }

    /**  Use this static method to determine if a string represents boolean True.
     *  Note that numbers other than one return false.
     * @return boolean "true" only if value (CASE-INSENSITIVE) is TRUE, T, YES, Y, 1 or ON.
     */
    public static boolean isTrue(String arg){
        if (arg==null){
            return false;
        }
        boolean b = false;
        if (arg.length() == 0)
            return false;
        String [] arr = new String [] {"TRUE", "T", "YES", "Y", "1", "ON"};
        arg = arg.toUpperCase();
        for (int i = 0; i<arr.length; i++){
            if ( arg.equals(arr[i]) ) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTrue(Object arg){
        if ( arg == null ) {
            return false;
        }
        return isTrue(arg.toString());
    }


    public static boolean isChecked(String value){
        if (value==null) return false;
        return value.toUpperCase().equals("ON");
    }

    /** format the correct checked value for browsers, based on a boolean.
     */
    public static String formatCheckedValue(boolean isTrue){
        if (isTrue) return "on";
        return "off";
    }


    /** Handles null strings as empty.  */
    public static boolean isEmpty(String str){
        return !notEmpty(str);
    }

    /** nulls, empty strings, and empty after trim() are considered blank.
     *  @return boolean "true" if value is null or only whitespace.
     */
    public static boolean isBlank(String str){
        return !notBlank(str);
    }

    /** Handles null strings as empty.  */
    public static boolean notEmpty(String str){
        if (str==null) return false;
        if (str.length()==0) return false;
        return true;
    }
    public static boolean notBlank(String str){
        if (str==null) return false;
        if (str.length()==0) return false;
        if (str.trim().length()==0){
            return false;
        }
        return true;
    }

    public static void sleep(Integer millis){
        sleep(millis.longValue());
    }

    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e){
        }
    }

    /** Milliseconds from start time as definded by the Date class. */
    public static Long now(){
        return new Long((new java.util.Date()).getTime());
    }

    public static java.sql.Timestamp nowSQL(){
        return new java.sql.Timestamp((new java.util.Date()).getTime());
    }

    public static String nowLocale(){
        java.util.Date date = new java.util.Date();
        String result = java.text.DateFormat.getDateTimeInstance().format(date);
        date = null;
        return result;
    }

    public static String dateLongToLocale(String longdatestr){
        if (longdatestr == null || longdatestr.length()==0){
            Log.error(Tools.class, "Empty string passed to dateLongToLocale(String)");
            return "";
        }
        Long longdate = new Long(longdatestr);
        return dateLongToLocale(longdate);
    }

    public static String dateLongToLocale(Long longdate){
        if (longdate == null) return "null";
        java.util.Date date = new java.util.Date(longdate.longValue());
        return dateToLocale(date);
    }

    public static String dateLongToLocale(long longdate){
        java.util.Date date = new java.util.Date(longdate);
        return dateToLocale(date);
    }

    public static String dateLongToLocale(java.sql.Timestamp timestamp){
        long longdate = timestamp.getTime();
        java.util.Date date = new java.util.Date(longdate);
        return dateToLocale(date);
    }

    public static String dateToLocale(java.util.Date date){
        String result = java.text.DateFormat.getDateTimeInstance().format(date);
        date = null;
        return result;
    }


    public static String formatUptime(long startTime){
        String elapsed;
        long runningTime, hours, minutes, seconds, remainder;
        java.util.Date dStartTime;
        long now = Tools.now().longValue();
        dStartTime = (new java.util.Date(startTime));
        runningTime = now - dStartTime.getTime();
        runningTime = runningTime / 1000; //convert to seconds from millis.
        hours = runningTime / 3600;
        remainder = runningTime % 3600;
        minutes = remainder / 60;
        seconds = remainder % 60;
        elapsed = ""+ hours
                + ( (""+minutes).length()==1 ? ":0" : ":" ) + minutes
                + ( (""+seconds).length()==1 ? ":0" : ":" ) + seconds;
        return elapsed;
    }

    public static String DATE_SEPARATOR = "/";
    public static String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    public static String getGMTTimestamp(){
        long longTime = now().longValue();
        return getGMTTimestamp(longTime);
    }

    @SuppressWarnings("deprecation")
	public static String getGMTTimestamp(long longTime){
        Date date = new Date(longTime);
        String gmtString = date.toGMTString();
        gmtString = gmtString.substring(0, gmtString.length()-4).trim();   //get rid of  " GMT"
        Date gmtDate = new Date(gmtString);  //get same date and time, in our timezone, as if we were in GMT
        return daysOfWeek[gmtDate.getDay()] +", "+ date.toGMTString(); //find the day IN GMT ZONE, NOT OURS.
    }

    /** Show all control chars in string, except CRLF.
     * @see #showControlChars(String,boolean)
     *
     */
    public static String showControlChars(String inString){
        return showControlChars(inString, false);
    }

    /** Shows all control chars (less than ASCII 32) by converting them
      *  to the format #nnn, where nnn is a 1 to 3 digit decimal number.
      *  For debugging only, does not encode the character # in any way if found
      *  in the string.
      *  @return  A copy of the string passed in, with any ASCII control
      *      characters converted.
      */
    public static String showControlChars(String inString, boolean showCRLF){
        int slen = inString.length();
        java.lang.StringBuffer buf = new java.lang.StringBuffer(slen);
        char c;
        String sRep;
        int v;
        for (int i=0; i<slen; i++){
            c = inString.charAt(i);
            v = (int)c;
            if (v < 32 ) {
                if ( showCRLF || (v != 10 && v != 13) ) {
                    sRep = "#" + v;
                    buf.append(sRep);
                    if ( v == 10 ) {
                        buf.append("\r\n");
                    }
                } else {
                    buf.append(c);//actual CR or LF.
                }
            }
            else
                buf.append(c);
        }
        return buf.toString();
    }

    /**
      * Enables all Components in an array if value parameter is true,
      *  <b><i>else disables them</i></b>
      */
    public static void enableIf(Component[] components, boolean value){
        int iSize = components.length;
        for (int i = 0; i < iSize; i++)
            components[i].setEnabled(value);
    }

    /** Reverses the enabled state for all Components in an array if value parameter is true,
      *  <b><i>else leaves them alone</i></b>
      */
    public static void reverseEnabledIf(Component[] components, boolean value){
        int iSize = components.length;
        for (int i = 0; i < iSize; i++)
            if (value)
                components[i].setEnabled(components[i].isEnabled());
    }

    /**
      *  Makes all Components in an array visible if value parameter is true,
      *  <b><i>else makes them invisible</i></b>
      */
    public static void makeVisibleIf(Component[] components, boolean value){
        int iSize = components.length;
        for (int i = 0; i < iSize; i++)
            components[i].setVisible(value);
    }

    /** Reverses the visible state for all Components in an array if value parameter is true,
      *  <b><i>else leaves them alone</i></b>
      */
    public static void reverseVisibleIf(Component[] components, boolean value){
        int iSize = components.length;
        for (int i = 0; i < iSize; i++)
            if (value)
                components[i].setVisible(components[i].isVisible());
    }

    public static void setBackground(Component comp, Color newColor){
        comp.setBackground(newColor);
        if (! (comp instanceof Container))
            return;
        Container container = (Container)comp;
        int childCount = container.countComponents();
        for (int c = 0; c<childCount; c++)
            setBackground(container.getComponent(c), newColor);
    }

    public static String readln() {
        return readln("");
    }

    public static String readln(String prompt) {
        try {
            if ( prompt != null && prompt.length() > 0 ) {
                System.out.println(prompt);
            }
            return readln(System.in);
        } catch (IOException e){
            System.err.println("ERROR reading input line.");
            return "";
        }
    }

    /** WARNING: Some unix terminal drivers expect \r as a line end char,
      *  although we look for strictly a \n here.
      */
    public static String readln(InputStream in)
    throws IOException {
        StringBuffer result = new StringBuffer();
        int ch;
        while ((ch = in.read()) != -1){
            if(ch==13){
                continue;
            }
            if (ch==10){
                break;
            }
            result.append((char)ch);

        }
        return result.toString();
    }

    public static final int FS_UNIX = 0;
    public static final int FS_DOS  = 1;
    public static final int FS_MAC  = 2;

    /** fsType is one of FS_UNIX, FS_DOS, FS_MAC
     */
    public static String readln(InputStream in, int fsType)
    throws IOException {
        StringBuffer result = new StringBuffer();
        int ch;
        ch = in.read();
        if (ch == -1){
            return null;
        }
        while (ch != -1){
            if(ch==13){
                if (fsType == FS_MAC){
                    // %% may want to chomp char 10 in case it was written by a non-mac, but is being read by a mac.
                    return result.toString();
                }
                ch = in.read();
                continue;
            }
            if (ch==10){
                if (fsType == FS_UNIX || fsType == FS_DOS){
                    return result.toString();
                }
                ch = in.read();
                continue;
            }
            result.append((char)ch);
            ch = in.read();
        }
        return result.toString();
    }

    static boolean m_fileSystemIsDOS = "\\".equals(File.separator);
    public static boolean fileSystemIsDOS(){return m_fileSystemIsDOS;}

    public static String fixFilename(String filename){
        if ( m_fileSystemIsDOS ) {
            return filename.replace('/', '\\');
        }
        return filename.replace('\\','/');
    }

    /** Copy a binary source file to a destination file.  Filenames are platform dependent.
      * Returns true for copying a zero byte file.
      * Returns true if the file copy was succesful.
      */
    public static boolean copyFile(String sourceFileName, String destFileName) {
        return FileTools.copyFile(sourceFileName, destFileName);
    }

    public static File openFile(String dir, String relPath){
        return FileTools.openFile(dir, relPath);
    }

    public static String readFile(String fullPath){
        return FileTools.readFile("", fullPath);
    }

    public static String readFile(String dir, String relPath){
        return FileTools.readFile(dir, relPath);
    }

    public static File saveFile(String dir, String relativeName, String content){
        return FileTools.saveFile(dir, relativeName, content);
    }

    /** Doesn't throw any exceptions: returns null if the file was not found, etc.
     */
     public static Vector readFileToVector(String dir, String relPath){
        try {
            File theFile = openFile(dir, relPath.toString());
            if (theFile==null){
                System.out.println("Tools.readFileToVector. File not present. directory: "+dir+" path: "+relPath);
                return null;
            }
            if (!theFile.isDirectory()){
                FileInputStream fis = new FileInputStream(theFile);
                BufferedReader d = new BufferedReader(new InputStreamReader(fis));
                Vector v = new Vector();
                String line = d.readLine();
                while (line != null){
                   v.addElement(line);
                   line = d.readLine();
                }
                fis.close();
                return v;
            }
        } catch (Exception e) {  // can't find the file
        }
        return null;
    }

    /** @return the File object of the saved file, or null if there was an error.
     */
    public static File saveVectorToFile(Vector vector, String delim, String dir, String relPath){
        try {
            String content = "";
            int vector_size = vector.size();
            for ( int i=0; i<vector_size; i++ ) {
                if ( i == vector_size-1 )
                    content += vector.elementAt(i);
                else
                    content += vector.elementAt(i) + delim;
            }
            return saveFile(dir, relPath, content);
        } catch (Exception e) {  // can't find the file
            com.dynamide.util.Log.error(Tools.class, "Error writing file. dir: "+dir+" relPath: "+relPath+" error: "+e);
            return null;
        }
    }

    public static Vector readIniFile(String dir, String relPath){
        return readFileToVector(dir, relPath);
    }

    public static Vector readIniFile(String filename){
        return readFileToVector("", filename);
    }

    public static File saveIniFile(Vector iniValues, String dir, String relPath){
        return saveVectorToFile(iniValues, "\r\n", dir, relPath);
    }

    public static File saveIniFile(Vector iniValues, String filename){
        return saveVectorToFile(iniValues, "\r\n", "", filename);
    }

    public static int getIniValueInt(Vector iniValues, String section, String key, int defaultValue){
        try{
            return stringToInt(getIniValue(iniValues, section, key, ""+defaultValue));
        }catch(Exception e){
            return defaultValue;
        }
    }

    public static boolean getIniValueBoolean(Vector iniValues, String section, String key, boolean defaultValue){
        try{
            return isTrue(getIniValue(iniValues, section, key, ""+defaultValue));
        }catch(Exception e){
            return defaultValue;
        }
    }


    public static String getIniValue(String iniFile, String section, String key, String defaultValue){
        Vector iniValues = readIniFile(iniFile);
        if (iniValues != null){
            return getIniValue(iniValues, section, key, defaultValue);
        }
        return defaultValue;
    }

    public static String getIniValue(String directory, String iniFile, String section, String key, String defaultValue){
        Vector iniValues = readIniFile(directory, iniFile);
        if (iniValues != null){
            return getIniValue(iniValues, section, key, defaultValue);
        }
        return defaultValue;
    }

    /** @param section Use 'null' to ignore section names, scanning all sections, or if section
     *   names are not used in the file.
     */
    public static String getIniValue(Vector iniValues, String section, String key, String defaultValue){
        String line;
        int iniValues_size = iniValues.size();
        int j=0;

        /* DEBUG:
         * System.out.println("-----------------\r\ngetIniValue key: "+key);
         * (new Exception()).printStackTrace();
         * System.out.println("-----------------");
         */
        boolean inSection = false;
        boolean ignoreSections = (section == null);

        while (j<iniValues_size){
            line = ((String)iniValues.elementAt(j)).trim();//added trim 2000-08-26.
            //System.out.println(">"+line+"<"+j);
            //boolean short-circuit protects against section being evaluated if null.
            //null means ignore sections.
            if ( inSection && !ignoreSections ) {
                if ( line.startsWith("[") ) {
                    return defaultValue;//into next section and still not found.
                }
            }
            if ( ignoreSections || line.startsWith('['+section+']') ) {
                //Found a section or no section
                inSection = true;
                if (section != null){
                   j = j+1;
                   continue;
                   //bump past section line since section was not null
                   // else no section line, so just continue with same line.
                }
            }
            if (inSection){
                if ( line.startsWith(key+'=') ) {
                    int jEQ = line.indexOf('=');
                    if (jEQ+1 > line.length()-1){
                        return defaultValue;
                    } else {
                        String res = line.substring(jEQ+1).trim();
                        return res.length()>0 ? res : defaultValue;
                    }
                }
            }
            j++;
        }
        return defaultValue;
    }


    public static void setIniValue(Vector iniValues, String section, String key, String value){
        String line;
        String row = key +'='+ value;
        int j;
        int iniValues_size = iniValues.size();
        for ( int i=0; i<iniValues_size; i++ ) {
            line = (String)iniValues.elementAt(i);
            if ( line.startsWith('['+section+']') ) {
                j = i+1;
                while (j<iniValues_size){
                    line = (String)iniValues.elementAt(j);
                    if (line.startsWith("[")){
                        //Section found, but no key.  Insert key=value.
                        iniValues.insertElementAt(row, j);
                        return;
                    }
                    if ( line.startsWith(key+'=') ) {
                        iniValues.setElementAt(row, j);
                        return;
                    }
                    j++;
                }
                //Done with section because end of vector, but no key.  Add it.
                iniValues.addElement(row);
                return;
            }

        }
        //Section not found:
        iniValues.addElement(" ");
        iniValues.addElement('['+section+']');
        iniValues.addElement(row);
    }

    public static void printStackTrace(){
        (new Exception()).printStackTrace();
    }

    public static String getStackTrace(){
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(bos);
        (new Exception()).printStackTrace(ps);
        String result = bos.toString();
        try {
            if(bos!=null)bos.reset();
            else System.out.println("bos was null, not closing");
        } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in Tools "+e);}
        return result;
    }

    public static String getStackTrace(Throwable t){
        if ( t == null ) {
            return "";
        }
        if (System.getProperty("java.specification.version").startsWith("1.3")){
            Log.warn(Tools.class,
               "JVM version 1.3 does not support Throwable.getStackTrace.  Using Throwable.printStackTrace().");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String trace = sw.toString();
            return trace;
        } else {
            StringBuffer result = new StringBuffer();
            StackTraceElement[] elements = t.getStackTrace();  // %% Java 1.4 only %%
            int elements_size = elements.length;
            StackTraceElement element;
            String test;
            for (int i=0; i < elements_size; i++) {
                 element = elements[i];
                 test = element.toString();
                 result.append(test);
                 result.append("\r\n");
            }
            return result.toString();
        }
    }

    public static String getStackTraceWithMessage(Throwable e){
        if (e==null){
            return "";
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(bos);
        e.printStackTrace(ps);
        String result = bos.toString();
        try {
            if(bos!=null)bos.reset();
            else System.out.println("bos was null, not closing");
        } catch (Exception e2)  {System.out.println("ERROR: couldn't reset() bos in Tools "+e2);}

        return result;
    }

    /** Takes an Exception object and formats a message that provides more debug information
      * suitable for developers for printing to System.out or for logging.  Not suitable for
      * presentation of error messages to clients.
      */
    public static String errorToString(Throwable e, boolean stackTraceOnException){
        return errorToString(e, stackTraceOnException, true);
    }

    public static String errorToString(Throwable e, boolean stackTraceOnException, boolean showmsg){
        if (e==null){
            return "";
        }
        String s = showmsg ? e.toString() + "\r\n  -- message: " + e.getMessage() : "";
        if (stackTraceOnException == true ){
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(bos);
            e.printStackTrace(ps);
            s = s + "\r\n  -- Stack Trace: \r\n  --      " + bos.toString();
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
            } catch (Exception e2)  {System.out.println("ERROR: couldn't reset() bos in Tools "+e2);}

        }
        return s;
    }

    public static boolean isJVM13(){
        return System.getProperty("java.specification.version").startsWith("1.3");
    }

    public static boolean isJVM14(){
        return System.getProperty("java.specification.version").startsWith("1.4");
    }

    public static String cleanAndReportMemory(){
        return cleanAndReportMemory(100);
    }

    public static String cleanAndReportMemory(long millis){
        System.gc();
        Thread.yield();
        System.runFinalization();
        System.gc();
        sleep(millis);
        System.gc();
        System.gc();
        System.gc();
        System.runFinalization();

        String msg = "  Total VM Memory:     "+Runtime.getRuntime().totalMemory()+" (bytes)\r\n"
                    +"  Available VM Memory: "+Runtime.getRuntime().freeMemory()+" (bytes)\r\n"
                    +"  Consumed VM Memory:  "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())+" (bytes)\r\n";
        return msg;
    }

    //========== dump(...)==================================================

    public static final String DUMPHTML_START = "<table border='1' cellspacing='0' cellpadding='2'>\r\n<tr><td>";
    public static final String DUMPHTML_LINESEP = "</td></tr>\r\n<tr><td>";
    public static final String DUMPHTML_ELEMSEP = "</td><td>";
    public static final String DUMPHTML_OBJPLACEHOLDER = "&#160;";
    public static final String DUMPHTML_END = "</td></tr></table>\r\n";

    public static String dump(Map map){
        String lb = "\r\n\r\n";
        return dump(map, lb);
    }

    public static String dump(Map map, String lineBreak){
        return dump(map, "", lineBreak, "; ", "", "", false);
    }

    public static String dumpHTML(Map map){
        return dump(map,
                    DUMPHTML_START,          //start
                    DUMPHTML_LINESEP,        //lineSeparator
                    DUMPHTML_ELEMSEP,        //elementSeparator
                    DUMPHTML_OBJPLACEHOLDER, //objectPlaceholder
                    DUMPHTML_END,
                    true);
    }

    public static String dump(Map map, String start, String lineSeparator, String elementSeparator, String objectPlaceholder, String end, boolean dumphtml){
        StringBuffer buff = new StringBuffer(start);
        String sd;
        for (Iterator it=map.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry =  (Map.Entry)it.next();
            buff.append(entry.getKey());
            Object obj = entry.getValue();
            if (obj!=null){
                buff.append(elementSeparator);
                if ( obj instanceof DynamideObject ) {
                    if (dumphtml) {
                        sd = ((DynamideObject)obj).dumpHTML();
                    } else {
                        sd = ((DynamideObject)obj).dump();
                    }
                } else {
                    sd = obj.toString();
                }
                buff.append(sd);
            } else {
                buff.append(elementSeparator);
                buff.append(objectPlaceholder);
            }
            if (it.hasNext()) {
                buff.append(lineSeparator);
            }
        }
        buff.append(end);
        return buff.toString();
    }


    enum USER_INPUT_SAFETY_LEVEL {STRICT,QUOTES_OK,SQL}

    public static void safe(String instring)
    throws Exception {
        safe(instring, USER_INPUT_SAFETY_LEVEL.STRICT);
    }

    public static void safe(String instring, USER_INPUT_SAFETY_LEVEL level)
    throws Exception {
        if (instring == null || instring.length()==0){
            return;
        }
        if (instring.indexOf('\'')>=0){
            throw new Exception("User input invalid.  Single quotes not allowed");
        }
        if (instring.indexOf('"')>=0){
            throw new Exception("User input invalid.  Quotes not allowed");
        }
        if (instring.indexOf(';')>=0){
            throw new Exception("User input invalid.  Semicolon not allowed");
        }
    }




}
