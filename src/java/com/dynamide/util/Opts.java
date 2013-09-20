package com.dynamide.util;


/** This class handles one command line options style:
 * All options must begin with a dash.  In this documentation, "switch" is the same
 * as "option", but refers specifically to the token with the dash.
 * Dashes may be single or double:
 * <pre>    -foo or --foo</pre>
 * Values may not have dashes.
 * E.g.
 *  <pre>   myprogram -foo -bar -file ./myfile -user "laramie" </pre><br>
 *
 * Note the two types of arguments:
 * <ul>
 *     <li> -foo and -bar are boolean arguments -- i.e. one switch sets a
 *           boolean value.
 *     <li> -file is a two-piece option with the name (-file)
 *           and the value (./myfile).  Similarly, -user has two pieces,
 *           the name (-user) and the value ("laramie"), but your shell
 *           will probably strip the quotes.
 * </ul>
 *
 * <p>The simplest way to think of this class is:
 * <ul>
 *   <li>all switches are assumed to be String switches: they take one arg.
 *   <li>Nothing is a boolean switch by default unless it is followed by switch, or is at the end.
 *   <li>A list is all non-switches following a switch until the next switch or the end.
 *   <li>If you don't call addOption, all switches will "consume" one non-switch param afterwards.
 *   <li>If you do call addOption, you can force a switch to consume (String) or not consume (Boolean)
 *       the following non-switch param.
 * </ul>
 *
 * <p>More details follow:
 *
 * <hr>
 *
 * <p>Boolean arguments can appear anywhere in the command line.
 * Any switch that is followed by another switch is considered a boolean argument.
 * (Except that anything after -- is not considered a switch).
 *
 * <p>To end switched arguments, uses "--" by itself, then any remaining
 * arguments.  The remaining arguments can then be retrieved by a
 * call to getRemainingArgs(), which returns a String[]. e.g.,
 * <pre>    -foo -bar mojo -- nixon finbar</pre> sets up a command line where calling
 * getRemainingArgs() would retrieve {"nixon", "finbar"} as a Java String array.
 * Parameters after -- can begin with dashes.
 *
 * <p>If you follow the rules, you don't need to use -- to begin the list of remaining args.
 * If command line is "java myclass -foo a b a", remaining args will be "b" and "a", unless
 * you call addOption("-foo", Boolean.class, false) or addOption("-foo", Boolean.class, true)
 * then remaining args is "a", "b", and "c".
 *
 * <p>Alternatively, you can call getOptionList() to get the list of arguments at the end,
 * or getOptionList(key) to see the non-switch arguments after "key", <b><i>EVEN if you
 * have defined "key" to be boolean</i></b>.  See
 * examples at those methods.  The getOptionList(key) method enables you to
 * use embedded lists: <pre>         -list1 a b c -list2 d e f</pre>
 *
 * <p>You cannot group option letters: "-abc" cannot be construed as options
 * "a", "b", and "c" all being true.  In this version, you must specify such
 * options as "-a -b -c"
 *
 * <p>You can have spaces in the values if the Java VM on your platform and you shell
 * handle spaces in quotes, by putting quotes around option values.  In csh, etc., you
 * can use single or double quotes, in NT CMD you must use double quotes.
 *
 * <p>This style is not supported: -b12 where this implies option "b" == 12.
 *
 * <p>If addOption is called, the options will be rescanned with the new rules
 *  before the next access of any option.  You can also call validate() after
 * addOption and the new rules will apply.
 *
 * <p>If the form you need to process is "java MyClass -foo a b c", then you have
 * three options: the default behavior is that -foo will be a String type and
 * getOptionList will return {"b", "c"} as an array; this will also be the behavior
 * if you call addOption("-foo", String.class)
 * or addOption("-foo", String.class, true); else you can call addOption("-foo", Boolean.class) etc.,
 * in which case getOptionList will return {"a", "b", "c"}
 *
 * <p>
 * <pre>
 * Example usage:
 *       Opts opts = new Opts(args);
 *       String iniFile = opts.getOption("-ini", "");
 *       if ( iniFile.length()==0 ) {
 *           usage();
 *       }
 *       String foo = opts.getOption("-foo");
 *       int ifoo = opts.getOptionInt("-foo", 4);
 *       System.out.println("getOptionBool(\"-bar\")="
 *                +opts.getOptionBool("-bar");
 *       String[] lastArgs = opts.getOptionList("");
 *       String [] list = opts.getOptionList("-list");
 *
 * Example 2:
 *       Opts opts = new Opts(args);
 *       opts.addOption("-foo", Integer.class, true);
 *       opts.addOption("-bar", true);
 *       String err = opts.validate();
 *       if ( err.length()>0 ) {
 *           System.out.println(err);
 *           System.exit(1);
 *       }
 * </pre>
 *
 *  <p>For more examples, see com.dynamide/util/Opts.java and
 *  run the default test cases:
 *  <pre>  java com.dynamide.util.Opts</pre>
 *  or run your own tests:
 *  <pre>  java com.dynamide.util.Opts -my -test -case foo bar</pre>
 *
 *
 */
public class Opts {
    public Opts(String[]args){
        m_args = args;
        //m_shortOptions = allowShortOptions;
        m_legalOptions = new StringList();
        m_list = getopts(args); //will reset m_needScan
    }

    private String[] m_args;
    private boolean m_shortOptions = false;
    private StringList m_list;
    private StringList m_legalOptions;
    private boolean m_needScan = true;

    public String toString(){
        return dump();
    }

    public int length(){
        return m_args!=null ? m_args.length : 0;
    }

    private void checkScan(){
        if (m_needScan){
            m_list = getopts(m_args); //will reset m_needScan
        }
    }

    /** Get the string value of option named by key, else return default
     *  e.g.<br>
     *  with {"-foo", "mojo"}, getOption("-foo", "bar") returns "mojo" <br>
     *  with {"-moxie", "mojo"}, getOption("-foo", "bar") returns "bar" <br>
     */
    public String getOption(String key, String defaultValue){
        String result = getOption(key);
        //if result == "true" it means we thought it was a boolean option.
        if ( result.length()==0 ) {
            result=defaultValue;
        }
        return result;
    }

    /** Get the string value of option named by key, else return. ""
     *  Also, allow getOption("foo") and getOption("-foo").
     *  e.g.<br>
     *  with {"-foo", "mojo"}, getOption("-foo") returns "mojo" <br>
     *  with {"-moxie", "mojo"}, getOption("-foo") returns "" <br>
     */
    public String getOption(String opt){
        checkScan();
        if (!opt.startsWith("-")) {
            opt = "-"+opt;
        }
        String value = (String)m_list.getObject(opt);
        if ( value == null){
            return "";
        }
        return value;
    }

    /** Return true if option present, false if option is absent.
     *  Also, allow getOptionBool("foo") and getOptionBool("-foo").
     *  <br>
     *  example<br>
     *  with {"-foo", "-mojo"}, getOptionBool("-foo") returns "true" <br>
     *  with {"-foo", "-mojo"}, getOptionBool("-bar") returns "false" <br>
     *  with {"-foo", "a", "-mojo"}, getOptionBool("-bar") returns "true" , even
     *  though "a" is adjacent -- with booleans, following parameters don't apply.<br>
     */
    public boolean getOptionBool(String opt){
        checkScan();
        if (!opt.startsWith("-")) {
            opt = "-"+opt;
        }
        int index = m_list.indexOf(opt);
        if ( index == -1 ) {
            return false;
        }
        return true;
    }

    /** Return integer value if option present and option parameter is a legal integer string.
     *  Returns default value if option absent or option parameter is not a legal integer string.
     *  e.g.<br>
     *  with {"-foo", "-mojo"}, getOptionInt("-foo", 3) returns 3 <br>
     *  with {"-foo", "20"}, getOptionInt("-foo") returns 20 <br>
     *  with {"-foo", "20"}, getOptionBool("-bar") returns 3 .
     */
    public int getOptionInt(String opt, int defaultValue){
        try {
            String optvalue = getOption(opt);
            if ( optvalue.length()==0 ) {
                return defaultValue;
            }
            int test = Tools.stringToInt(optvalue);
            return test;
        } catch (Exception e){
            return defaultValue;
        }
    }

    private int lastIndexOf(String key, String[] arr, boolean partial){
        int end = arr.length-1;
        for ( int i = 0; i < arr.length; i++ ) {
            if ( arr[i].equals("--") ) {
                end = i-1;
                break;
            }
        }


        for ( int i = end; i>=0; i-- ) {
            if ( (partial & arr[i].startsWith(key)) || arr[i].equals(key) ) {
                return i;
            }
        }
        return -1;
    }

    /** Same as calling getOptionList(""); */
    public String[] getOptionList(){
        return getOptionList("");
    }

    /** Gets the option list specified by key, that is, all the args
     * after -key until next "-" (start of a switch or --) or the end.<br><br>
     * For example:<br>
     *    <pre>java MyClass -list a b c -foo mojo </pre>
     *    <pre>getOptionList("-list") ==> a b c </pre>
     *    <pre>getOptionList() ==> []</pre>
     *    <pre>getOptionList("-foo") ==> mojo</pre>
     *    <br><br>
     * Example2:<br>
     *    <pre>java MyClass -list a b c -foo mojo nixon</pre>
     *    <pre>getOptionList("-list") ==> a b c </pre>
     *    <pre>getOptionList() ==> nixon</pre>
     *    <pre>getOptionList("-foo") ==> mojo nixon</pre>
     *    <br><br>
     * Example3:       <br>
     *    <pre>java MyClass a b c , then  </pre>
     *    <pre>getOptionList("") ==> a b c  </pre>
     *    <br><br>
     * Example3 is the same as calling the getOptionList() overload.*/
    public String[] getOptionList(String key){
        checkScan();
        if (key.length()==0){ //someone called getOptionList("")
            return getRemainingArgs();
        }
        StringList result = new StringList();
        result.setAllowDuplicates(true);
        int iPos = lastIndexOf(key, m_args, false);
        if ( iPos == -1 ) {
            return result.toStringArray();
        }

        for ( int i = iPos+1; i<m_args.length; i++ ) {
            String arg = m_args[i];
            if ( arg.startsWith("-") ) {
                break;
            }
            result.add(arg);
        }
        return result.toStringArray();
    }

    /** This is always the last arguments on the command line that are not switches,
     *  except that the leading value may be "consumed" by a String switch. <br><br>
     *  If you have something like <code>"-foo a b"</code>, and -foo is not declared to
     *  be Boolean by a call to addOption(), then b is "remaining", (and -foo will have the value "a").<br><br>
     *  To explicitly make -foo be Boolean, call <code>addOption("-foo", Boolean.class)</code><br><br>
     *  To explicitly make "a" and "b" both be "remaining" without declaring them, require
     *  the command-line user to put a -- in front: <code>-foo -- a b</code><br><br>
     *  Another example: "-foo -bar mo -- mojo nixon", where "mo" is the
     *  value of switch "-bar", and the two arguments "mojo" and "nixon"
     *  would be the remaining args.<br><br>
     *  If you need to get lists (adjacent parameters without dashes_, either at the end
     *  or in the middle of the command-line, see getOptionList(String key).
     */
    public String[] getRemainingArgs(){
        checkScan();
        int i = m_list.indexOf("--");
        if ( i>-1 ) {
            int sz = m_list.size();
            int j = 0;
            String[] result = new String[sz-i-1];
            while (i<sz-1) {
                result[j] = m_list.getString(i+1);
                j++;
                i++;
            }
            return result;
        } else {
            return new String[0];
        }
    }

    public String getRemainingArgAt(int index){
        checkScan();
        String[] remain = getRemainingArgs();
        if ( index >= remain.length ) {
            return "";
        }
        return remain[index];
    }

    public String getArgAt(int index){
        return m_args != null ? m_args[index] : "";
    }

    public class Option{
        public Option(String key, Class type, boolean required){
            this.key = key;
            this.type = type;
            this.required = required;
        }
        public String key;
        public Class type;
        public boolean required;
    }

    /**  Declare options to have types and be required/optional.  These are used in validate(),
     *  and to determine parsing order.  The most important parse change is that Boolean.class
     *  types will never "consume" the non-switch parameters that follow them.  Boolean are "stand-alone".
     *  For example:  with "-foo a -bar b", if -foo is declared to be Boolean.class, then
     *  getOption("-foo") will return true, since -foo is present.  -foo's value is NOT "a".
     *  By default, -foo would be String.class, and so getOption("-foo"); would return "a".
     *
     * @param type A Class type -- Legal values are: Boolean.class, Integer.class, and String.class
     */
    public void addOption(String key, Class type, boolean required){
        m_needScan = true;
        m_legalOptions.addObject(key, new Option(key, type, required));
    }

    private Class getOptionType(String key){
        Option option = (Option)m_legalOptions.getObject(key);
        if ( option != null ) {
            return option.type;
        }
        return String.class;
    }

    /** Call to getting the first error string when validatating based on
     *  parameter types and requirements passed in addOption.
     */
    public String validate(){
        boolean valid = false;
        checkScan();
        int legalOptions_size = m_legalOptions.size();
        for ( int i = 0; i < legalOptions_size; i++ ) {
            Option option = (Option)m_legalOptions.getObjectAt(i);
            //Check for require switches:
            String optvalue = getOption(option.key);
            if ( option.required ) {
                if ( m_list.indexOf(option.key) == -1 ) {
                    return "Option "+option.key+" must be provided.";
                }
                if ( optvalue.length()==0 ) {
                    return "Option "+option.key+" must have a value.";
                }
            }
            //Check for switch types:
            if ( option.type == Integer.class ) {
                try {
                    if ( optvalue.length()==0 ) {
                        return "Empty string is an invalid integer option for "+option.key;
                    }
                    int test = Tools.stringToInt(optvalue);
                } catch (Exception e){
                    return "Invalid integer option ["+optvalue+"] for "+option.key;
                }
            }
            //Umm, booleans are always valid, since they are present or absent,
            //  and strings are always valid.
        }

        //check for extraneous switches:
        int list_size = m_list.size();
        for ( int i = 0; i < list_size; i++ ) {
            String userOptionKey = m_list.getString(i);
            if ( userOptionKey.startsWith("-") && m_legalOptions.indexOf(userOptionKey) == -1 ) {
                if ( userOptionKey.equals("--") ) {
                    //don't complain about list delimiter, or anything after it
                    break;
                }
                return "Don't know about option "+userOptionKey;  //otherwise complain.
            }
        }

        return "";
    }


    private StringList addRemaining(StringList result, String[]args, int i){
        result.addObject("--", null);
        while (i<args.length) {
            String arg = args[i];
            result.addObject(arg, null);
            i++;
        }
        return result;
    }

    /* Use this static method if you wish to hang onto the options StringList yourself.
     * Otherwise use the Opts(String[]) constructor.
     */
    private StringList getopts(String[] args){
        m_needScan = false;
        StringList result = new StringList();
        result.setAllowDuplicates(true);
        int lastDash = 0;
        int i = 0;
        String arg2;

        int iLastDash = -1;
        for ( i=args.length-1; i>=0; i-- ) {
            if ( args[i].startsWith("-") ) {
                iLastDash = i;
                break;
            }
        }

        i = 0;
        while (i<args.length) {
            String arg = args[i];
            if ( arg.equals("--") ) {
                return addRemaining(result, args, i+1); //adds -- implicitly
            }
            if ( arg.startsWith("-")  ) {
                if ( (args.length > i+1) && args[i+1].equals("--") ) {
                    //end of args: -- , NOT longopt: --foo
                    result.addObject(arg, null);
                    return addRemaining(result, args, i+2);
                } else  if ( (args.length > i+1) && args[i+1].startsWith("-")) {
                    //boolean switch: -foo or --foo
                    result.addObject(arg, "true");
                    i++;
                } else {
                    //either "-foo a" or "-foo a b c" or "-foo a -bar b" or "-foo"
                    if (args.length == i+1) {  //  -foo
                        result.addObject(arg, "true");
                        return result;
                    } else if (args.length == i+2){  // -foo a
                        if ( getOptionType(arg) == Boolean.class ) {  // [-foo] [a]
                            result.addObject(arg, "true");
                            return addRemaining(result, args, i+1);
                        } else {
                            arg2 = args[i+1];
                            result.addObject(arg, arg2);  // [-foo a]
                            return result;
                        }
                    } else if (args.length > i+2){  // -foo a b c   |   -foo a -bar...
                        arg2 = args[i+1];
                        if ( i == iLastDash ) {  // -foo a b c
                            if ( getOptionType(arg) == Boolean.class ) {  // [-foo] [a b c]
                                result.addObject(arg, "true");
                                return addRemaining(result, args, i+1);
                            } else {
                                result.addObject(arg, arg2);  // [-foo a] [b c]
                                return addRemaining(result, args, i+2);
                            }
                        }
                        result.addObject(arg, arg2);  // -foo a -bar ...
                        i+=2;
                    }
                }
            } else {
                if ( i == iLastDash+1 ) {
                    result.addObject("--", null);
                }
                result.addObject(arg, null);
                i++;
            }
        }
        return result;
    }

    public String dump(){
        checkScan();
        return m_list.dump();
    }


    //==============================================================================
    //====               T E S T     C A S E S
    //==============================================================================

    private static String [] testArray1 = {"-foo", "3"};
    private static Opts testCase1(){
        Opts opts = new Opts(testArray1);
        opts.addOption("-foo", Integer.class, true);
        opts.addOption("-nothing", Boolean.class, true); //extra required param, should be error, since not provided.
        System.out.println("getOption(\"-foo\")="+opts.getOption("-foo"));
        System.out.println("getOptionInt(\"-foo\", 4)="+opts.getOptionInt("-foo", 4));
        return opts;
    }

    private static String [] testArray2 = {"-foo", "notInt", "-list", "a", "b", "c"};
    private static Opts testCase2(){
        Opts opts = new Opts(testArray2);
        opts.addOption("-foo", Integer.class, true);  //foo in testArray2 is not an int: should be error

        System.out.println("getOption(\"-foo\")="+opts.getOption("-foo"));
        System.out.println("getOptionInt(\"-foo\", 4)="+opts.getOptionInt("-foo", 4));

        return opts;
    }

    private static String [] testArray3 = {"-foo", "3", "-bar", "mo", "-yo", "mojo", "nixon",
                                  "-minnie", "--mouse", "-list", "a", "b", "c",
                                  "--", "the", "end", "of", "args"};
    private static Opts testCase3(){
        Opts opts = new Opts(testArray3);
        opts.addOption("--mouse", Boolean.class, true);
        opts.addOption("-foo", Integer.class, true);
        opts.addOption("-yo", Boolean.class, false);
        opts.addOption("-minnie", Boolean.class, true);

        System.out.println("getOption(\"-foo\")="+opts.getOption("-foo"));
        System.out.println("getOptionInt(\"-foo\", 4)="+opts.getOptionInt("-foo", 4));
        System.out.println("getOptionInt(\"-bar\", 4)="+opts.getOptionInt("-bar", 4));
        System.out.println("getOptionBool(\"-bar\")="+opts.getOptionBool("-bar"));
        System.out.println("getOptionBool(\"-moxie\")="+opts.getOptionBool("-moxie"));
        return opts;
    }

    private static String [] testArray4 = {"-foo", "-i", "a", "b", "c"};
    private static Opts testCase4(){
        Opts opts = new Opts(testArray4);
        System.out.println("-foo and -i are addOption'd to be booleans...");
        opts.addOption("-foo", Boolean.class, false);
        opts.addOption("-i", Boolean.class, false);
        System.out.println("getOption(\"-i\")="+opts.getOption("-i"));
        System.out.println("getOptionBool(\"-i\")="+opts.getOptionBool("-i"));
        System.out.println("getRemainingArgs()="+opts.getRemainingArgs());
        return opts;
    }

    private static String [] testArray5 = {"-list", "a", "--", "-list", "b", "c"};
    private static Opts testCase5(){
        Opts opts = new Opts(testArray5);
        System.out.println("Test whether -- allows values in list that begin with dashes.");
        return opts;
    }

    private static String [] testArray6 = {"-a", "--", "b", "c"};
    private static Opts testCase6(){
        Opts opts = new Opts(testArray6);
        System.out.println("Test whether -- messes up -a");
        System.out.println("getOption(\"-a\")="+opts.getOption("-a"));
        System.out.println("getOptionBool(\"-a\")="+opts.getOptionBool("-a"));
        return opts;
    }



    /** Calls lots of test cases if called with no command line args, else processes
     *  command line args and shows some tests and a dump of processed state.
     */
    public static void main(String[] args){
        Opts opts;
        System.out.println("Self Test...");
        if (args.length>0){
            testheader("User Test");
            test(new Opts(args));
        } else {
            testheader("testCase1");
            test(testCase1());
            testheader("testCase2");
            test(testCase2());
            testheader("testCase3");
            test(testCase3());
            testheader("testCase4");
            test(testCase4());
            testheader("testCase5");
            test(testCase5());
            testheader("testCase6");
            test(testCase6());
        }
    }

    private static void testheader(String caseName){
        System.out.println("");
        System.out.println("=======================================");
        System.out.println("        "+caseName);
    }

    private static void test(Opts opts){
        System.out.println("------args------------------------------");
        System.out.println(StringTools.printArray(opts.m_args, " "));
        System.out.println("------dump-----------------------------");
        System.out.println(opts.dump());
        System.out.println("------validate-------------------------");
        String err = opts.validate();
        if ( err.length()>0 ) {
            System.out.println(err);
        }
        System.out.println("------remaining------------------------");
        String[] rem = opts.getRemainingArgs();
        for ( int i = 0; i<rem.length; i++ ) {
            System.out.println("remaining["+i+"] "+rem[i]);
        }
        System.out.println("------getOptionList()------------------");
        String[] list = opts.getOptionList();
        for ( int j = 0; j<list.length; j++ ) {
            System.out.println("list["+j+"] "+list[j]);
        }
        System.out.println("------getOptionList(\"\")--------------");
        list = opts.getOptionList("");
        for ( int j = 0; j<list.length; j++ ) {
            System.out.println("list["+j+"] "+list[j]);
        }
        System.out.println("------getOptionList(\"-list\")---------");
        list = opts.getOptionList("-list");
        for ( int j = 0; j<list.length; j++ ) {
            System.out.println("list["+j+"] "+list[j]);
        }
        System.out.println("=======================================");
        System.out.println("");
    }

}
