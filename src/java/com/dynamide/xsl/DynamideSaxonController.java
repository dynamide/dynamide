package com.dynamide.xsl;

import com.dynamide.Session;

/**
 * THIS CLASS IS A DIRECT DESCENDANT OF com.icl.saxon.Controller.
 *
 * I EXTEND THAT CLASS BECAUSE I NEED TO SWITCH THE OUTPUT WRITTER, AND SAXON WON'T LET ME DO IT.
 * THE CONNECTION IS MADE IN com.dynamide.xsl.SaxonJDOMTransform, where I override all the factory stuff,
 * so look for this to break on new Saxon releases.  %%%%%%
 *
 *
 */

public class DynamideSaxonController extends com.icl.saxon.Controller {

    public DynamideSaxonController() {
        super();
    }

    public DynamideSaxonController(com.icl.saxon.TransformerFactoryImpl factory) {
        super(factory);
    }

    ///new, laramie
    private java.io.Writer m_rootWriter;

    public java.io.Writer getRootWriter(){
        return m_rootWriter;
    }

    public void setRootWriter(java.io.Writer writer){
        m_rootWriter = writer;
    }

    private Session m_session = null;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    //end laramie
    //
}   // end of outer class Controller
