package com.dynamide.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** Composite is the base class for any item found in a tree structure.

    A composite can own child Composites.  A Composite has a name, and any named attributes.

    Attributes are Objects.  The special attribute 'name' is just a String.

    A top level Composite will not have a parent.

    @author Laramie Crocker
 */

public class Composite extends CompositeAbstract implements IComposite {

    public Composite(String name, IComposite parent){
        super(name, parent);
    }

    //================= Member Variables =========================================

    private boolean DEBUG = false;
    private Map children = null;
    private Map attributes = null;

    /* ===============  Handling Child Composites ============================ */

    /** Normally, you wouldn't call addChild from user classes.  Just call the constructor (String name, Composite parent).
      *  This sets the parent and adds the child to the tree.
      */
    public IComposite addChild(IComposite new_child)
    throws Exception {
        if (new_child == this){
            throw new Exception("Can't add self as child");
        }
        allocateChildrenHashtable();

        if (null == children.get(new_child.getName())) {
            children.put(new_child.getName(), new_child);
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


    private void allocateChildrenHashtable(){
        if (null == children) {
            children = Tools.createSortedCaseInsensitiveMap();
        }
    }

    /** @return An Enumeration of all direct children of this Composite.
      */
    public Collection getChildren(){
        allocateChildrenHashtable();
        return children.values();
    }

    public int getChildCount(){
        if (null == children)
            return 0;
        else
            return children.size();
    }

    public IComposite removeChild(IComposite old_child){
        IComposite old = null;
        if (null != children){
            old = (IComposite)children.remove(old_child.getName());
            IComposite theRoot = getRoot();
            if (theRoot instanceof Composite){
                ((Composite)theRoot).notifyChangeListeners(CHILDREN_CHANGED, this);
            }
        }
        return old;
    }

    public IComposite findComposite(String itemName)
    throws Exception {
        IComposite item, child;
        if ( itemName.equals(getName()) ) {
            return this;
        } else {
            if (children != null){
                for (Iterator it = children.values().iterator(); it.hasNext();) {
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

    private void allocateAttributesHashtable(){
        if (null == attributes){
            attributes = Tools.createSortedCaseInsensitiveMap();
        }
    }

    public void addAttribute(String attribute_name, Object attribute_value){
        allocateAttributesHashtable();
        attributes.put(attribute_name, attribute_value);
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
        return (Object)attributes.get(attributeName);
    }

    public Map getAttributesMap(){
        allocateAttributesHashtable();
        return attributes;
    }

    public Collection getAttributes(){
        allocateAttributesHashtable();
        return attributes.values();
    }


}
//=====================  Example Code: =====================================

/********
  m_menuRoot.addChangeListener(this);

//ICompositeChangeListener:
    public void onCompositeChanged(Composite node, int whatChanged){
        System.out.println("onCompositeChanged: "+node+" what: "+whatChanged);
    }
********/
