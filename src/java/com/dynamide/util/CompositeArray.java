package com.dynamide.util;

import java.util.*;

public class CompositeArray extends CompositeAbstract implements IComposite {

    public CompositeArray(String name, IComposite parent){
        super(name, parent);
    }

    //================= Member Variables =========================================

    private boolean DEBUG = false;

    private List children = null;
    private List attributes = null;

    /** Normally, you wouldn't call addChild from user classes.  Just call the constructor (String name, Composite parent).
      *  This sets the parent and adds the child to the tree.
      */
    public IComposite addChild(IComposite new_child)
    throws Exception {
        if (new_child == this){
            throw new Exception("Can't add self as child");
        }
        allocateChildrenArray();
        if (null == getChild(new_child.getName())) {
            children.add(new_child);
            IComposite theRoot = getRoot();
            if (theRoot instanceof Composite){
                ((Composite)theRoot).notifyChangeListeners(CHILDREN_CHANGED, this);
            }
            if (DEBUG) {
                System.out.println(this.getName()+" adding child: "+new_child.getName()+" path: "+new_child.dumpPath());
                //Tools.printStackTrace();
            }
            return new_child;
        } else {
            if (DEBUG) {
                System.out.println(this.getName()+" not adding child: "+new_child.getName()+" path: "+new_child.dumpPath());
                //Tools.printStackTrace();
            }
            return null;
        }
    }


    private void allocateChildrenArray(){
        if (null == children) {
            children = new ArrayList();
        }
    }

    /** @return An Enumeration of all direct children of this Composite.
      */
    public Collection getChildren(){
        allocateChildrenArray();
        return children;
    }

    public int getChildCount(){
        if (null == children)
            return 0;
        else
            return children.size();
    }

    public IComposite removeChild(IComposite old_child){
        if (null != children){
            children.remove(old_child);
            IComposite theRoot = getRoot();
            if (theRoot instanceof Composite){
                ((Composite)theRoot).notifyChangeListeners(CHILDREN_CHANGED, this);
            }
        }
        return old_child;
    }

    public IComposite getChild(String name){
        for (Iterator it = children.iterator();it.hasNext();){
            IComposite value = (IComposite)it.next();
            if (name.equals(value.getName())){
                return value;
            }
        }
        return null;
    }

    public IComposite findComposite(String itemName)
    throws Exception {
        IComposite item, child;
        if ( itemName.equals(getName()) ) {
            return this;
        } else {
            if (children != null){
                for (Iterator it = children.iterator(); it.hasNext();) {
                    child = (IComposite)it.next();
                    if (child == this){
                        throw new Exception("recursion detected: "+dumpPath());
                    }
                    item = child.findComposite(itemName);
                    if (item != null){
                        return item;
                    }
                }
            }
        }
        return null;
    }

    /* ===============  Attributes ====================================== */

    private void allocateAttributesArray(){
        if (null == attributes){
            attributes = new ArrayList();
        }
    }

    public class Entry{
        public Entry(String k, Object v){
            key = k;
            value = v;
        }
        public String key;
        public Object value;
    }

    public void addAttribute(String attribute_name, Object attribute_value){
        allocateAttributesArray();
        Entry entry = new Entry(attribute_name, attribute_value);
        attributes.add(entry);
        IComposite theRoot = getRoot();
        if (theRoot instanceof Composite){
            ((Composite)theRoot).notifyChangeListeners(ATTRIBUTES_CHANGED, this);  //now parent and name are valid, notify now.
        }
    }

    /** @return The attribute with the requested name, or null if not found.
      */
    public Object attributeByName(String attributeName){
        if (attributes == null)
            return null;
        for (Iterator it = attributes.iterator();it.hasNext();){
            Entry entry = (Entry)it.next();
            if ( entry.key.equals(attributeName) ) {
                return entry.value;
            }
        }
        return null;
    }

    public Map getAttributesMap(){
        allocateAttributesArray();
        Map result = new HashMap();
        for (Iterator it = attributes.iterator();it.hasNext();){
            Entry entry = (Entry)it.next();
            result.put(entry.key, entry.value);
        }
        return result;
    }

    public Collection getAttributes(){
        allocateAttributesArray();
        return attributes;
    }


    public String printAttributes(){
        if (attributes == null) return "";
        StringBuffer result = new StringBuffer();
        for (Iterator it = attributes.iterator();it.hasNext();){
            Entry entry = (Entry)it.next();
            result.append("\r\n<font size='+1' color='blue'>"+entry.key+"</font>\r\n");
            result.append(entry.value);
        }
        return result.toString();
    }


}
