package com.dynamide.util;

import java.io.File;

import com.dynamide.DynamideObject;

import com.dynamide.resource.Assembly;
import com.dynamide.resource.ContextNode;
import com.dynamide.resource.IContext;
import com.dynamide.resource.ResourceManager;

import org.webmacro.Broker;
import org.webmacro.InitException;
import org.webmacro.Provider;
import org.webmacro.ResourceException;
import org.webmacro.Template;

import org.webmacro.engine.FileTemplate;

import org.webmacro.util.Settings;

/**
  * A Provider is an object responsible or loading and managing
  * instances of a given type. The Provider is used by the Broker
  * to look up objects on demand.
  * <p>
  * By implementing new Provider types and registering them with
  * the broker via WebMacro.properties you can extend or change
  * WebMacro's behavior.
  */
public class TemplateProvider extends DynamideObject implements Provider {
  private TemplateProvider() throws Exception {
    super(null);
    throw new SecurityException("Forbidden");
  }

  public TemplateProvider(String type, String templateRoot, boolean cache){
    super(null);
    _type = type;
    _templateRoot = templateRoot;
    _cache = cache;
  }

  Broker _broker = null;

  String _type = "template";

  String _templateRoot = "";

  boolean _cache = true;

  /**
    * Return an array representing the types this provider serves up
    */
  public String getType(){
    logDebug ("TemplateProvider.getType called. "+_type);
    return _type;
  }

  /**
    * Initialize this provider based on the specified config.
    */
  public void init(Broker b, Settings config) throws InitException{
    _broker = b;
  }

  /**
    * Clear any cache this provider may be maintaining
    */
  public void flush(){

  }

  /**
    * Close down this provider, freeing any allocated resources.
    */
  public void destroy(){
    _broker = null;
  }

  /**
    * Get the object associated with the specified query
    */
  public Object get(String query) {
      try {
        logDebug("TemplateProvider.get("+query+") called");
        return getFromCache(query);
      } catch (Exception e){
        logError("error in get(String)" , e);
        return "";
      }
  }

    private static final String TEMPLATE_PREFIX = "template:";
    public static final String TEMPLATE_CACHE = "TemplateProvider";

    public static void clearTemplateCache(){
        //ResourceManager rm = ResourceManager.getRootResourceManager();
        //not implemented: rm.killSessionsStartingWith(TEMPLATE_PREFIX);
    }

    public Template getFromCache(String query) throws ResourceException{
        ResourceManager rm = ResourceManager.getRootResourceManager();
        String templateCacheName = TEMPLATE_PREFIX+query;
        IContext ctx = rm.getCachedResource(TEMPLATE_CACHE, templateCacheName);
        Template t=null;
        if ( ctx != null ) {
            t= (Template)ctx.getAttribute(Assembly.CONTENT);
        }
        if ((_cache == false) || t == null){
            File tFile = new File(_templateRoot, query);
            if (tFile != null) {
                try {
                    t = new FileTemplate(_broker, tFile);
                    ctx = new ContextNode(templateCacheName);
                    ctx.rebindAttribute(Assembly.CONTENT, t);
                    rm.putCachedResource(TEMPLATE_CACHE, templateCacheName, ctx); //safely calls rebind().
                    logDebug(query +" loaded from DISK: "+tFile.getCanonicalPath());
                    return t;
                } catch (Exception e) {
                    logWarn ("TemplateProvider: Error occured while parsing "+ query, e);
                    throw new ResourceException("Error parsing template "+query, e);
                }
            }
            throw new ResourceException("Couldn't find template: "+query);
        } else {
            logDebug(query +" loaded from CACHE." );
            return t;
        }
    }

}
