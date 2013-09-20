package com.dynamide.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/** Composite is the base class for any item found in a tree structure.

    A composite can own child Composites.  A Composite has a name, and any named attributes.

    Attributes are Objects.  The special attribute 'name' is just a String.

    A top level Composite will not have a parent.

    @author Laramie Crocker
 */

public abstract class CompositeAbstract implements IComposite {

    private CompositeAbstract(){}

    public CompositeAbstract(String name, IComposite parent){
        this.name = name; //don't call setName, it relies on m_parent, which isn't set yet.  setName calls notify, so do it next.
        try {
            setParent(parent);
        } catch (Exception e)  {
            System.err.println("Error calling Composite.setParent(...) in "+name+" in "+e);
        }
        IComposite theRoot = getRoot();
        if (theRoot != null && theRoot instanceof Composite){
            ((Composite)theRoot).notifyChangeListeners(NAME_CHANGED, this);  //now parent and name are valid, notify now.
        }
    }

    public void finalize() throws Throwable {
        if (null != m_parentComposite)
            m_parentComposite.removeChild(this);
        super.finalize();
    }

    //================= Member Variables =========================================

    public static final int ATTRIBUTE_NOT_FOUND = -1;

    public static final int NAME_CHANGED = 1;
    public static final int CHILDREN_CHANGED = 2;
    public static final int ATTRIBUTES_CHANGED = 3;

    private IComposite m_root = null;

    private void unsetRoot(){
        m_root = null;
    }

    public IComposite getRoot(){
        IComposite parent;
        if ( m_root != null ) {
            return m_root;
        }
        IComposite m_root = this;
        while(m_root != null){
            parent = m_root.getParent();
            if (parent == null){
                //System.out.println(getName()+".getRoot: "+dumpPath());
                return m_root;
            } else {
                m_root = parent;  //go through loop again.
            }
        }
        return null;
    }

    private IComposite m_parentComposite = null;
    public IComposite getParent(){return m_parentComposite;}

    public void detach(){
        if (m_parentComposite != null){
            m_parentComposite.removeChild(this);
        }
        m_parentComposite = null;
        unsetRoot();
    }

    private String name = "";
    public String getName(){return name;}
    public void setName(String new_name){
        name = new_name;
        IComposite theRoot = getRoot();
        if (theRoot != null && theRoot instanceof Composite){
            ((Composite)theRoot).notifyChangeListeners(NAME_CHANGED, this);
        }
    }

    //=================  Methods  =========================================
    public String dumpPath(){
        String path = getName();
        IComposite p = getParent();
        while ( p != null ) {
            path = p.getName()+"/"+path;
            p = p.getParent();
        }
        return path;
    }

    public String dump(){
        return dump(true, true, true, true, false);
    }

    public String dumpHTML(){
        return dump(true, true, true, true, true);
    }

    public String dump(boolean dumpPath, boolean dumpAttributes, boolean dumpIndent, boolean preorder, boolean html){
        String fill = html? "&nbsp;&nbsp;" : "     ";
        DumpWalker dumpWalker = new DumpWalker(dumpPath, dumpAttributes, dumpIndent, fill);
        if (preorder) {
            walkPreOrder(dumpWalker);
        } else {
            walkPostOrder(dumpWalker);
        }
        return dumpWalker.toString();
    }

    public class DumpWalker implements ICompositeWalker {
        public DumpWalker(boolean dumpPath, boolean dumpAttributes, boolean dumpIndent, String fillString){
            this.dumpPath = dumpPath;
            this.dumpAttributes = dumpAttributes;
            this.dumpIndent = dumpIndent;
            this.fillString = fillString;
        }
        boolean dumpPath;
        boolean dumpAttributes;
        boolean dumpIndent;
        String fillString;
        private StringBuffer m_buffer = new StringBuffer();
        public String toString(){
            return m_buffer.toString();
        }
        public boolean onNode(com.dynamide.util.IComposite node){
            if (dumpIndent) m_buffer.append(StringTools.fill(fillString, node.getDepth()));
            if (dumpPath) m_buffer.append(node.dumpPath());
            else m_buffer.append(node.getName());
            if (dumpAttributes) m_buffer.append(node.printAttributes());
            m_buffer.append("\r\n");
            return true;
        }
    }

    public String toString(){
        return "Composite:"+dumpPath()+"::"+printAttributes();
    }

    public Object get(String what){
        Object o = attributeByName(what);
        return o != null ? o : "";
    }

    public int getDepth(){
        String path = getName();
        int d = 0;
        IComposite p = getParent();
        while ( p != null ) {
            d++;
            p = p.getParent();
        }
        return d;
    }

    /* ===============  Handling Child Composites ============================ */
    /*   setParent
     *       parent.addChild
     *
     *   Constructor
     *      setParent
     *          parent.addChild
     *
     */

    public IComposite setParent(IComposite parent)
    throws Exception {
        m_parentComposite = parent;
        if (m_parentComposite!=null){
            m_parentComposite.addChild(this);
        } else {
            //System.out.println("parent was null, couldn't call parent.addChild()");
        }
        return parent;
    }
    /** Normally, you wouldn't call addChild from user classes.  Just call the constructor (String name, Composite parent).
      *  This sets the parent and adds the child to the tree.
      */
    public abstract IComposite addChild(IComposite new_child) throws Exception;

    /** @return An Enumeration of all direct children of this Composite.
      */
    public abstract Collection getChildren();

    public abstract int getChildCount();

    public abstract IComposite removeChild(IComposite old_child);

    public abstract IComposite findComposite(String itemName) throws Exception;

    /* ===============  Attributes ====================================== */

    public abstract void addAttribute(String attribute_name, Object attribute_value);

    /** @return The attribute with the requested name, or null if not found.
      */
    public abstract Object attributeByName(String attributeName);

    public abstract Map getAttributesMap();

    public abstract Collection getAttributes();

    public String printAttributes(){
        Map attributes = getAttributesMap();
        if (attributes == null) return "";
        return attributes.toString();
    }


    /*************************
        ICompositeChangeListener  -- fire for all children / attributes that change.
          Only the root node will manage this list.
     *************************/


    private Vector changeListeners = new Vector();

    public void addChangeListener(ICompositeChangeListener new_listener){
        IComposite root = getRoot();
        if (this==root){
            changeListeners.addElement(new_listener);
        } else {
            IComposite theRoot = getRoot();
            if (theRoot != null && theRoot instanceof CompositeAbstract){
                ((CompositeAbstract)theRoot).changeListeners.addElement(new_listener);
            }
        }
    }

    public void removeChangeListener(ICompositeChangeListener listener){
        IComposite root = getRoot();
        if (this==root){
            changeListeners.removeElement(listener);
        } else {
            IComposite theRoot = getRoot();
            if (theRoot != null && theRoot instanceof CompositeAbstract){
                ((CompositeAbstract)theRoot).changeListeners.removeElement(listener);
            }
        }

    }

    /** @param whatChanged - one of: NAME_CHANGED,CHILDREN_CHANGED, ATTRIBUTES_CHANGED.
    */
    protected void notifyChangeListeners(int whatChanged, IComposite node){
        int iSize = changeListeners.size();
        if (iSize > 0){
            for (int i = 0; i < iSize; i++){
                ((ICompositeChangeListener)changeListeners.elementAt(i)).onCompositeChanged(node, whatChanged);
            }
        }
    }

    //=================== ICompositeWalker Caller ================================

    public void walkPreOrder(ICompositeWalker caller){
        walk(caller, ICompositeWalker.PREORDER);
    }

    public void walkPostOrder(ICompositeWalker caller){
        walk(caller, ICompositeWalker.POSTORDER);
    }

    public void walk(ICompositeWalker caller, int order){
        boolean keepDiving = true;
        if (order==ICompositeWalker.PREORDER){
            keepDiving = caller.onNode(this);
        }
        Collection children = getChildren();
        if (keepDiving && null != children){
            IComposite child;
            for (Iterator it = children.iterator(); it.hasNext();) {
                child = (IComposite)it.next();
                child.walk(caller, order);
            }
        }
        if (order==ICompositeWalker.POSTORDER){
            caller.onNode(this);
        }
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
