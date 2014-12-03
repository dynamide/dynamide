package com.dynamide.test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JavaScriptRhinoTest {
    public static void main(String args[]) throws Exception {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("javascript");
        Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("stdout", System.out);
        js.eval("stdout.println(Math.cos(Math.PI));");
        // Prints "-1.0" to the standard output stream.
    }
}
