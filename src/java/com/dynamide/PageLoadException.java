/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

public class PageLoadException extends DynamideException{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public PageLoadException(String message){
        super(message);
   }
   public PageLoadException(Throwable t){
        super(t);
   }
}

