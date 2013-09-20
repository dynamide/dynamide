package com.dynamide.resource;

import com.dynamide.DynamideException;

public class ObjectAlreadyBoundException extends DynamideException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ObjectAlreadyBoundException(String msg){
        super(msg);
    }
}
