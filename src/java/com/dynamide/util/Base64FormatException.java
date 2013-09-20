// Base64FormatException.java
// $Id: Base64FormatException.java,v 1.1 2010-07-31 18:38:43 laramiessh Exp $
// (c) COPYRIGHT MIT and INRIA, 1996.
// Please first read the full copyright statement in file COPYRIGHT.html

//package org.w3c.tools.codec ;
package com.dynamide.util;

/**
 * Exception for invalid BASE64 streams.
 */

public class Base64FormatException extends Exception {

  /**
   * Create that kind of exception
   * @param msg The associated error message 
   */

  public Base64FormatException(String msg) {
	super(msg) ;
  }

}
