package com.dynamide;

import java.io.IOException;

import com.dynamide.resource.Assembly;
import com.dynamide.resource.ContextNode;
import com.dynamide.resource.IContext;
import com.dynamide.resource.ObjectAlreadyBoundException;

import org.jdom.JDOMException;

public class Application extends Persistent {
    public Application(DynamideObject owner, Session session)
    throws JDOMException, IOException {
        super(owner, session);
    }

    public Application(DynamideObject owner, String filename, Session session)
    throws JDOMException, IOException {
        super(owner, filename, session);
    }

    private final String CACHE_NAME = "cache:Application-content";

    private IContext checkCache(){
        IContext result = getContext(CACHE_NAME);
        if ( result == null ) {
            result = new ContextNode(CACHE_NAME);
            rebind(CACHE_NAME, result);
        }
        return result;
    }

    public IContext putInCache(String key, Object obj)
    throws ObjectAlreadyBoundException {
        IContext item = getFromCache(key);
        if ( item == null ) {
            item = checkCache().rebind(key, new ContextNode(key));
        }
        item.rebindAttribute(Assembly.CONTENT, obj);
        Assembly.accessed(item);
        return item;
    }

    public IContext getFromCache(String key){
        IContext result = checkCache().getContext(key);
        Assembly.accessed(result);
        return result;
    }

    public Object getContentFromCache(String key){
        IContext item = getFromCache(key);
        if ( item != null ) {
            return item.getAttribute(Assembly.CONTENT);
        }
        return null;
    }



}
