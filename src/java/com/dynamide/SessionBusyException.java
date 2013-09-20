/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

public class SessionBusyException extends DynamideException{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public SessionBusyException(String message){
        super(message);
   }
   public SessionBusyException(Throwable t){
        super(t);
   }
}

