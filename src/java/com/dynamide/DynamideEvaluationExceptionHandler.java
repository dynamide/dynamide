package com.dynamide;

import org.webmacro.Broker;
import org.webmacro.Context;
import org.webmacro.PropertyException;

import org.webmacro.engine.CrankyEvaluationExceptionHandler;
import org.webmacro.engine.Variable;

import org.webmacro.util.Settings;

public class DynamideEvaluationExceptionHandler extends CrankyEvaluationExceptionHandler {

   public DynamideEvaluationExceptionHandler() {
   }

   public DynamideEvaluationExceptionHandler(Broker b) {
        super(b);
   }

   public void init(Broker b, Settings config) {
        super.init(b, config);
   }

   public void evaluate(Variable variable, Context context, Exception problem)
   throws PropertyException {
        com.dynamide.util.Log.debug(DynamideEvaluationExceptionHandler.class, "in evalute.  Variable: '"+variable.getVariableName()+"' problem: ==>"+problem+"<==");
        super.evaluate(variable, context, problem);
   }

   public String expand(Variable variable, Context context, Exception problem)
   throws PropertyException {
        com.dynamide.util.Log.debug(DynamideEvaluationExceptionHandler.class, "in evalute: Variable: '"+variable.getVariableName()+"' problem: ==>"+problem+"<==");
        return super.expand(variable, context, problem);
   }


   public String warningString(String warningText) throws PropertyException {
        com.dynamide.util.Log.debug(DynamideEvaluationExceptionHandler.class, "in warningString: "+warningText);
        return super.warningString(warningText);
   }


   public String errorString(String errorText) throws PropertyException {
        com.dynamide.util.Log.debug(DynamideEvaluationExceptionHandler.class, "in errorString: "+errorText);
        return super.errorString(errorText);
   }
}
