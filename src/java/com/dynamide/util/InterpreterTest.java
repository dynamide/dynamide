package com.dynamide.util;

import bsh.Interpreter;

public class InterpreterTest {
    String fireAppProgrammerEventTest(String procName, String args, String eventSource){
        if ( eventSource.length()==0 ) {
            return "";
        }
        StringList stringlist = new StringList();
        bsh.Interpreter interp = new Interpreter();
        interp.setClassLoader(this.getClass().getClassLoader());
        try{
            System.out.println("EVENT SOURCE: \r\n"+eventSource);
            //String eventSourceFilename = "c:/temp/mojo.bsh";
            //interp.source(eventSourceFilename);
            interp.eval(eventSource);
            interp.set("sender", stringlist);
            if (args.length()>0){
                interp.eval(procName+"(sender, \""+args+"\");");
            } else {
                interp.eval(procName+"(sender);");
            }
        } catch (bsh.EvalError e){
            System.out.println("bsh.EvalError: "+e+" ErrorText: "+e.toString());
            return "";
        } catch (Exception e2){
            System.out.println("Error eval'ing with bsh: "+e2);
            return "";
        }
        System.out.println("mojo:" +stringlist.toString());
        String result = "DONE";
        return result;
    }

    public void test2(String filename){
        if ( true ) {
            System.out.println("test2 is obviated: SessionDummy has been removed.");
            System.exit(1);
        }
        /*
        *try {
        *    SessionDummy theSession = new SessionDummy();
        *    theSession.setSessionID("theSession");
        *    SessionDummy theSession2 = new SessionDummy();
        *    theSession2.setSessionID("theSession2");
        *    ScriptEvent event = new ScriptEvent();
        *    event.session = theSession;
        **    bsh.Interpreter interp = new Interpreter();
         *   interp.setClassLoader(this.getClass().getClassLoader());
         *   System.out.println("this class loader: "+this.getClass().getClassLoader());
         *   String eventSource = Tools.readFile(filename);
         *   interp.eval(eventSource);
         *   interp.setVariable("event", event);
         *   interp.eval("test (event);");
        *} catch (bsh.EvalError e){
        *    System.out.println("bsh.EvalError: "+e+" ErrorText: "+e.toString());
        *} catch (Exception e2){
        *    System.out.println("Error eval'ing with bsh: "+e2);
        *}
         * */
    }

    public static void main(String [] args){
        try{
            InterpreterTest t = new InterpreterTest();
            Opts opts = new Opts(args);
            boolean test2 = opts.getOptionBool("-test2");
            if ( test2 ) {
                t.test2(args[1]);
                System.exit(0);
            }
            String eventSource = Tools.readFile(args[0]);
            String res = t.fireAppProgrammerEventTest("application_onStart", "", eventSource);
            System.out.println("event result: "+res);
        } catch (Exception e){
            System.out.println("Usage: InterpreterTest <event-file-source>");
            System.out.println("exception:"+e);
        }
        System.exit(0);
    }


}
