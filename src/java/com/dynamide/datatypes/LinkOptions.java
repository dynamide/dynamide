package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

import org.jdom.Element;

public class LinkOptions extends Datatype {
    public LinkOptions(){
    }

    public LinkOptions(DynamideObject owner, Session session){
        super(owner, session);
    }

    public void addXMLContentTo(Element container){
        //todo: add values as elements ...container.addContent(get("value").toString());
    }

    private boolean includeUser = true;
    public boolean getIncludeUser(){ return includeUser;}
    public void setIncludeUser(boolean new_includeUser){ includeUser = new_includeUser;}

    private boolean relative = true;
    public boolean getRelative(){ return relative;}
    public void setRelative(boolean new_relative){ relative = new_relative;}

    private boolean useSessionID = true;
    public boolean getUseSessionID(){ return useSessionID;}
    public void setUseSessionID(boolean new_useSessionID){ useSessionID = new_useSessionID;}

    private String extension = "";
    public String getExtension(){ return extension;}
    public void setExtension(String new_extension){ extension = new_extension;}


}
