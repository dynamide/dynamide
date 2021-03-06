package com.dynamide.interpreters;

public class InterpreterTools {

    public enum SCRIPT_LANGUAGE {BEANSHELL, TCL, JAVASCRIPT}

    public static SCRIPT_LANGUAGE mapScriptLanguage(String language) {
        if ( language.startsWith("beanshell") ) {
            return SCRIPT_LANGUAGE.BEANSHELL;
        }
        if ( language.toUpperCase().startsWith("TCL") ) {
            return SCRIPT_LANGUAGE.TCL;
        }
        if ( language.toUpperCase().startsWith("JAVASCRIPT") ) {
            return SCRIPT_LANGUAGE.JAVASCRIPT;
        }
        return SCRIPT_LANGUAGE.BEANSHELL;
    }

    public static String mapInterpreter(String language){
        SCRIPT_LANGUAGE lang = mapScriptLanguage(language);
        switch (lang){
            case BEANSHELL:
                return "com.dynamide.interpreters.BshInterpreter";
            case TCL:
                return "com.dynamide.interpreters.TclInterpreter";
            case JAVASCRIPT:
                return "com.dynamide.interpreters.RhinoInterpreter";
        }
        return "com.dynamide.interpreters.BshInterpreter";
    }

    public static String getEventSignature(String findName, String language){
        return getEventSignature(findName, mapScriptLanguage(language));
    }

    public static String getEventSignature(String findName, SCRIPT_LANGUAGE language){
        switch (language){
            case BEANSHELL:
                return "void "+findName+"(Object event)";
            case TCL:
                return "proc "+findName;//todo: haven't tested this yet...
            case JAVASCRIPT:
                return "function "+findName+"(event)";
        }
        return "void "+findName+"(Object event)";//default to beanshell.
    }
}
