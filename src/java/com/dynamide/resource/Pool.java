package com.dynamide.resource;

import com.dynamide.DynamideObject;

import com.dynamide.util.Tools;

public class Pool extends ContextNode implements IContext {
    private Pool () throws Exception {
        this(null);
        throw new Exception("FORBIDDEN");
    }

    protected Pool (DynamideObject owner) {
        super(owner);
    }

    protected Pool (DynamideObject owner, String key) {
        super(owner, key);
    }

    public static final long DEFAULT_POOL_TIMEOUT = 1000*60*30;

    public int getPoolMax(){
        return Tools.stringToIntSafe(getAttribute("poolMax"), Integer.MAX_VALUE);
    }

    public void setPoolMax(String new_value){
        rebindAttribute("poolMax", new_value);
    }

    public void setPoolMax(int new_value){
        rebindAttribute("poolMax", ""+new_value);
    }

    /** Look in the default pool for the resource by key.
      */
    public Object getPooledObject(String key)
    throws SecurityException {
        Object result;
        IContext context = getContext(key);
        if ( context != null ) {
            result = context;
        } else {
            result = context.getAttribute(key);
            if ( result == null  ) {
                result = context.getAttribute("value");
            }
        }
        /*
        if ( result.getPermission("READ") != null ) {
            //check if user has read permission, else:
            throw new SecurityException("Resource not readable by user");
        }
         */

        //lock result in pool against re-use until recycled. %% todo...
        return result;
    }

    // I wonder if I can make all dynamide Persistents know how to store() and load()
    //  and if I can make them loadDefault() so that pages will load up their inherited



    //don't use Objects for keys, since you'll have to run their toString method, in our trusted space.


}
