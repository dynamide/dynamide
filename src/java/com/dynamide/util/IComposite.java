package com.dynamide.util;

import java.util.Collection;

public interface IComposite {
    public IComposite getRoot();
    public IComposite getParent();
    public IComposite setParent(IComposite parent) throws Exception;
    public IComposite findComposite(String itemName) throws Exception;
    public String getName();
    public IComposite addChild(IComposite child) throws Exception;
    public IComposite removeChild(IComposite child);
    public Collection getChildren();
    public void addAttribute(String attribute_name, Object attribute_value);
    public String dump();
    public String dumpHTML();
    public String dumpPath();
    public String printAttributes();
    public Collection getAttributes();
    public int getDepth();
    public Object get(String what);

    /**Use values of ICompositeWalker.PREORDER or ICompositeWalker.POSTORDER for order.*/
    public void walk(ICompositeWalker caller, int order);
}
