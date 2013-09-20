package com.dynamide.resource;

import java.io.File;

import java.security.Permission;
import java.security.Permissions;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import com.dynamide.DynamideObject;

import com.dynamide.util.FileTools;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class ContextNode extends DynamideObject implements IContext {

    private ContextNode() throws Exception {
        super(null);
        throw new Exception("FORBIDDEN");
    }

    public ContextNode(DynamideObject owner){
        super(owner);
        //debug: debsetStackTrace();
    }

    public ContextNode(DynamideObject owner, String key){
        super(owner);
        setKey(key);
        //debug: setStackTrace();
    }

    public ContextNode(String key){
        super(null);
        setKey(key);
        //debug: setStackTrace();
    }

    private void setStackTrace(){
        rebindAttribute("STACK_TRACE", Tools.getStackTrace());
    }

    private String key = "";
    public String getKey(){return key;}
    public void setKey(String key){
        this.key = key;
    }

    private Map m_attributes = null;

    private void lazyInitAttributes(){
        if (m_attributes == null){
            m_attributes = Tools.createSortedCaseInsensitiveMap();
        }
    }

    private Map m_contexts = null;

    private void lazyInitContexts(){
        if (m_contexts == null){
            m_contexts = Tools.createSortedCaseInsensitiveMap();
        }
    }

    /** Once permissions are set using lockPermissions, impls should not allow them to be changed.
     * To change, you should rebind in the parent context.
     * But you have to have permission to rebind, so that is safe (you need the 'rebindable' permission).
     */
    public void lockPermissions(Permissions permissions) throws SecurityException {
        //lazyInit...
        //todo %%
    }

    public  void unlockPermissions(Permissions permissions) throws SecurityException {
        //lazyInit...
        //todo %%
    }

    public Permission getPermission(String permissionName){
        return null;
        //lazyInit...
        //todo %%
        //be sure to do a deep copy of permission so caller doesn't have any handles to actual permission object in use.
    }

    public Object get(String what)
    throws Exception {
        Object result = getContext(what);
        if ( result != null ) {
            return result;
        } else  {
            result = getAttribute(what);
            if ( result != null ) {
                return result;
            }
        }
        com.dynamide.util.IGet getter = super.getNextGetter();
        if (getter != null && getter != this){
            //logDebug("using super.getter ["+getter+"] for get(\""+what+"\")");
            return getter.get(what);
        }
        return null;
    }

    public Object getAttribute(String attributeName){
        if (m_attributes == null){
            return null;
        }
        return m_attributes.get(attributeName);
    }

    public boolean hasAttribute(String key){
        if (m_attributes== null){
            return false;
        }
        return m_attributes.containsKey(key);
    }

    public int getAttributeCount(){
        if (m_attributes == null){
            return 0;
        }
        return m_attributes.size();
    }

    public Map getAttributes(){
        lazyInitAttributes();
        return (Map)(((TreeMap)m_attributes).clone());
    }

    public void bindAttribute(String attributeName, Object value) throws SecurityException, ObjectAlreadyBoundException {
        lazyInitAttributes();
        m_attributes.put(attributeName, value);
    }

    public void rebindAttribute(String attributeName, Object value) throws SecurityException{
        lazyInitAttributes();
        m_attributes.put(attributeName, value);
    }

    /** Set the attributes en masse.
     */
    public void bindAllAttributes(Map attributes) throws SecurityException, ObjectAlreadyBoundException{
        lazyInitAttributes();
        m_attributes.putAll(attributes);
    }

    public  IContext firstContext(){
        if ( m_contexts == null ) {
            return null;
        }
        synchronized (m_contexts){
            if (m_contexts instanceof TreeMap){
                TreeMap tm = (TreeMap)m_contexts;
                return (IContext)tm.get(tm.firstKey());
            } else {
                logWarn("ContextNode unexpected type of context map: "+m_contexts.getClass().getName());
                Iterator it = m_contexts.entrySet().iterator();
                if ( it.hasNext() ) {
                    return (IContext)((Map.Entry)it.next()).getValue();
                }
                return null;
            }
        }
    }

    public IContext removeFirstContext(){
        if ( m_contexts == null ) {
            return null;
        }
        synchronized (m_contexts){
            if (m_contexts instanceof TreeMap){
                TreeMap tm = (TreeMap)m_contexts;
                if ( tm.size() == 0 ) {
                    return null;
                }
                return (IContext)tm.remove(tm.firstKey());
            } else {
                logWarn("ContextNode unexpected type of context map: "+m_contexts.getClass().getName());
                Iterator it = m_contexts.entrySet().iterator();
                if ( it.hasNext() ) {
                    IContext result = (IContext)((Map.Entry)it.next()).getValue();
                    try {
                        m_contexts.remove(result);
                    } catch (Exception e)  {
                        logWarn("concurrent modification: context was removed before ContextNode.removeFirstContext could do so: "+result);
                    }
                    return result;
                }
                return null;
            }
        }
    }


    public IContext getContext(String key){
        if ( key.equals(".") || key.length() == 0 ) {
            return this;
        }
        if (m_contexts== null){
            return null;
        }
        return (IContext)m_contexts.get(key);
    }

    public boolean hasContext(String key){
        if ( key.equals(".") || key.length() == 0 ) {
            return true;
        }
        if (m_contexts== null){
            return false;
        }
        return m_contexts.containsKey(key);
    }

    public int getContextCount(){
        if (m_contexts== null){
            return 0;
        }
        return m_contexts.size();
    }

    public Map getContexts(){
        lazyInitContexts();
        return (Map)((TreeMap)m_contexts).clone();
    }

    /** Add children en masse.
     */
    public void bindAll(Map children) throws SecurityException, ObjectAlreadyBoundException{
        lazyInitContexts();
        m_contexts.putAll(children);
        // %% todo: make it check for pre-exising.

    }
    /** Accepts Object and IContext items.  If IContext, then it can behave as a subcontext.
     *  Otherwise it won't be walked.
     *  Throws ObjectAlreadyBoundException if object exists as a child already -- use rebind instead.
     */
    public IContext bind(String key, IContext context)throws SecurityException, ObjectAlreadyBoundException{
        lazyInitContexts();
        synchronized (m_contexts){
            if ( m_contexts.get(key) != null ) {
                throw new ObjectAlreadyBoundException("Context "+getKey() +" already contains '"+ key+"'");
            }
            m_contexts.put(key, context);
            return context;
        }
    }

    public IContext bind(IContext context)throws SecurityException, ObjectAlreadyBoundException{
        lazyInitContexts();
        bind(context.getKey(), context);
        return context;
    }

    /** Accepts Object and IContext items.  If IContext, then it can behave as a subcontext.
     *  Otherwise it won't be walked.
     */
    public IContext rebind(String key, IContext context)throws SecurityException {
        lazyInitContexts();
        synchronized (m_contexts){
            m_contexts.remove(key);
            m_contexts.put(key, context);
        }
        return context;
    }

    public IContext rebind(IContext context)throws SecurityException{
        return rebind(context.getKey(), context);
    }

    public IContext remove(String key)throws SecurityException{
        if (m_contexts == null){
            return null;
        }
        IContext res = (IContext)m_contexts.remove(key);
        return res;
    }

    public Object removeAttribute(String key)throws SecurityException{
        Object result = getAttribute(key);
        if ( result != null ) {
            m_attributes.remove(key);
            return result;
        } else  {
            return null;
        }
    }


    /** @param path construct an array of strings, one for each context, starting from this object,
     *  for example: find({"web-apps", "/dynamide/demo"});
     *  then, to search from the root, use ResourceManager.find(String[]).
     *
     *  @return A reference to "this" if path is zero length.
     */
    public Object find(String [] path){
        //System.out.println("path: '"+Tools.arrayToString(path));
        boolean debug = false;
        if (debug) System.out.println("find: "+Tools.arrayToString(path));
        if (m_contexts == null && m_attributes == null){
             //logDebug("IContext.find was empty, so didn't find a match looking for '"+Tools.arrayToString(path));
            return null;
        }
        lazyInitContexts();
        lazyInitAttributes();
        Object att;
        String found = "";
        IContext prev = null;
        IContext current = this;
        int i = 0;
        int len = path.length;
        if ( len == 0 ) {
            return this;
        }
        while ( i < len ) {
            String pathi = path[i];
            if (debug) System.out.println("path["+i+"] == "+path[i]+ " current: "+current.getKey());
            IContext sub = current.getContext(pathi);
            if ( i == len-1 ) {
                if ( sub != null ) {
                    if (debug) System.out.println("return sub: "+sub);
                    return sub;
                }
                Object attr = current.getAttribute(pathi);
                if ( attr != null ) {
                    if (debug) System.out.println("return attr "+attr);
                    return attr;
                }
            }
            if ( sub == null ) {
                //no more
                if (debug) System.out.println("no more");
                if (found.length()>0){
                    if (debug) logDebug("IContext.find didn't find a match looking for '"+Tools.arrayToString(path)
                                +"' but found these while walking the context tree: "+found
                                //+"\r\nThe remaining context was: "
                                //+ResourceManager.dumpContext(current, "  ", false)
                             );
                } else {
                    if (debug) logDebug("IContext.find didn't find a match looking for '"+Tools.arrayToString(path)+'\'');
                }
                return null;
            }
            found += "::"+pathi;
            i++;
            current = sub;
        }
        if (debug) logDebug("didn't find terminal, but found these: "+found);
        //System.out.println("at end");
        return null;
    }

    /** Search using "/" as the separator, by simply calling find(path, "/").
     */
    public Object find(String path){
        return find(path, "/");
    }


    /**  @return A reference to "this" if path is empty.
    */
    public Object find(String path, String separator){
        //System.out.println("path: '"+path+"' separator: '"+separator+"'");
        if (path.equals(separator)){
            String[] emptyStringArray = {""};
            return find(emptyStringArray);
        }
        Vector v = StringTools.parseSeparatedValues(path, separator);
        if ( path.startsWith(separator) ) {
            v.removeElementAt(0);
        }
        String [] pathArray = Tools.vectorToStringArray(v);
        return find(pathArray);
    }

    /* %% todo: make something like this implementation of permissions work.
    public void addResource(IContext icontext)
    throws SecurityException {
        String key = icontext.getKey();
        IContext prev = (IContext)getResource(key);
        if ( prev != null) {
            if (prev.getPermission("REPLACEABLE") != null){
                // %% check that user has REPLACEABLE permission on this object, else throw exception
                throw new SecurityException("Resource not REPLACEABLE by user");
            }
            if (prev.getPermission("POOLABLE") != null){
                // %% check that user has POOLABLE permission on this object, else throw exception
                throw new SecurityException("Resource not POOLABLE by user");
            }
            m_resources.put(key, icontext);
        }
    }
     */

    /** @todo this class does nothing with this call.  Subclasses probably should be the correct place
     *  for implementations.
     */
    public void update(){
    }

    public String dump(){
        return getClass().getName()+'['+getKey()+']' //;
        + ResourceManager.dumpContext(this, "  ", false);
    }

    public String dumpContext(boolean html){
        return ResourceManager.dumpContext(this, "  ", html);
    }




    /*parent:IContext   -->   directory
       attributes     key  --> filename
                      object --> file contents or bytes
                            :: create file in current dir, name with key
       contexts            --> subdirectories
                                :: create subdirectory, pass new dir and context into this method.
    */
    public void writeCache(String prefix, File rootDirectory)
    throws Exception {
        if ( rootDirectory == null || !rootDirectory.exists() ) {
            throw new SecurityException("No permission on rootDirectory in writeCache");
        }
        if ( getKey().equals(ResourceManager.ROOT_KEY) && com.dynamide.security.DynamideSecurityManager.isCurrentThreadWorker() ) {
            throw new SecurityException("Forbidden [writeCache]");
        }
        Map.Entry entry;
        Object key;
        IContext value;
        String cachePrefixURI = getAttribute(ResourceManager.CACHE_PREFIX_URI).toString();
        Iterator it = getContexts().entrySet().iterator();
        while ( it.hasNext() ) {
            entry = (Map.Entry)it.next();
            key = entry.getKey();
            value = (IContext)entry.getValue();
            File f = FileTools.openFile(rootDirectory.getCanonicalPath(), Tools.fixFilename(cachePrefixURI+key), true);
            if (f != null){
                Object binaryAtt = value.getAttribute(Assembly.BINARY);
                if ( binaryAtt == null ) {
                    //context is not a file.
                } else {
                    if ( binaryAtt != null && binaryAtt.equals(Assembly.TRUE) ) {
                        byte[] buff = (byte[])((IContext)value).getAttribute(Assembly.CONTENT);
                        FileTools.saveFile("", f.getCanonicalPath(), buff);
                    } else {
                        Object content = value.getAttribute(Assembly.CONTENT);
                        if (content != null) FileTools.saveFile("", f.getCanonicalPath(), content.toString());
                    }
                }
            }
        }

    }

    public String toString(){
        return getClass().getName()+'['+getKey()+']' ;
    }

    //========== Helper methods for IContext ==================================

    public String listContextNames(IContext context, boolean sort, String lineBreak){
        return listContextNames(context, sort).dump(lineBreak);
    }

    public StringList listContextNames(IContext context, boolean sort){
        String key;
        StringList stringlist = new StringList();
        Iterator keys = context.getContexts().keySet().iterator();
        while ( keys.hasNext() ) {
            key = (String)keys.next();
            if (key != null){
                stringlist.add(key) ;
            }
        }
        if (sort) stringlist.sort();
        return stringlist;
    }

    public String listAttributeNames(IContext context, boolean sort, String lineBreak){
        return listAttributeNames(context, sort).dump(lineBreak);
    }

    public StringList listAttributeNames(IContext context, boolean sort){
        String key;
        StringList stringlist = new StringList();
        Iterator keys = context.getAttributes().keySet().iterator();
        while ( keys.hasNext() ) {
            key = (String)keys.next();
            if (key != null){
                stringlist.add(key) ;
            }
        }
        if (sort) stringlist.sort();
        return stringlist;
    }

    public String dumpAttributes(boolean html){
        StringBuffer result = new StringBuffer();
        String key;
        StringList stringlist = new StringList();
        Iterator keys = getAttributes().keySet().iterator();
        while ( keys.hasNext() ) {
            key = (String)keys.next();
            if (key != null){
                result.append(key);
                result.append(" :: "+getAttribute(key));
                result.append(html ? "<br/>\r\n" : "\r\n" );
            }
        }
        return result.toString();
    }

    //==================================================================================




}
