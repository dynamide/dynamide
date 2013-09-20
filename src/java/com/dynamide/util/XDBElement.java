package com.dynamide.util;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.jdom.Element;

/** The functions in this class that return XDBElement can be used in an introspection framework, specifically WebMacro.
 * 
 * @author Laramie Crocker
 */
public class XDBElement {
    private Element el;
    private XDB x;

    public XDBElement(XDB xdb, Element element){
        el = element;
        x = xdb;
    }

    public Element getElement(){
        return el;
    }

    /** Returns a List of org.jdom.Element.
     * 
     * @param xpathExpression
     * @return A List of org.jdom.Element
     */
    public List select(String xpathExpression){
        return x.select(el, xpathExpression);
    }
    
    /** Returns a List of XDBElement, rather than org.jdom.Element.
     * 
     * @param xpathExpression
     * @return A List of XDBElement
     */
    public List list(String xpathExpression){
    	List result = new ArrayList();
    	List l = x.select(el, xpathExpression);
    	for(Iterator it=l.iterator();it.hasNext();){
            Object obj = it.next();
            if ( obj instanceof org.jdom.Element ) {
                result.add(new XDBElement(x,(org.jdom.Element)obj));
            //} else if ( obj instanceof org.jdom.Attribute ) {
            //	result.add(((org.jdom.Attribute)obj).getValue());
            //} else if ( obj instanceof org.jdom.Document ) {
            //	result.add(x.outputElement(((org.jdom.Document)obj).getRootElement()));
            }
        }
    	return result;
    }

    public Element selectFirst(String xpathExpression){
        return x.selectFirst(el, xpathExpression);
    }

    public String valueOf(String xpathExpression){
        return x.valueOf(el, xpathExpression);
    }

    public String valueOfAll(String xpathExpression){
        return x.valueOfAll(el, xpathExpression);
    }
    
    /** Same as calling selectFirst(xpath) on the Element.
     */
    public XDBElement first(String xpath){
    	return new XDBElement(x, selectFirst(xpath));
    }
    
    /** Returns current node as XML, including the tag for the current node.
     */
    public String output(){
    	return x.outputElement(el);    	
    }
    
    /** Returns all the child nodes as XML, excluding the tag for the current node.
     */
    public String outputContent(){
    	return x.outputElementContent(el);    	
    }
    
    /**returns the Element.valueOf(key) where key gets turned into a String.
     */
    public String get(Object key){
    	String s = key.toString();  //get(Object) is called from webmacro and other introspection frameworks, so this allows any Object, but realy only works when the key is meaningful as a string.
    	return valueOf(s);    
    }
    
    /**This function does double-duty: with an Element, it returns getText(), 
     * which works with both the text of the node and with attribute values.
     */
    public String toString(){
    	return el.getText();
    }
}
