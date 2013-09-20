/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

public class DynamideException extends Exception{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public DynamideException(String message){
        super(message);
   }
   public DynamideException(Throwable t){
        //super(t);   // %% Java 1.4 only %%
        setCause(t);
   }

   private Throwable cause; //must store this for use in java 1.3 VMs.

   public Throwable getCause(){
        if (com.dynamide.util.Tools.isJVM13()){
            return this.cause;
        } else {
            return super.getCause();
        }
   }

   public void setCause(Throwable cause){
        if (com.dynamide.util.Tools.isJVM13()){
            this.cause = cause;
        } else {
            // %%%%%%%%%%%%%%%%%%%% illegal: super.setCause(cause);
        }
   }

}

