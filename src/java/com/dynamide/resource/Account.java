package com.dynamide.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dynamide.DynamideObject;
import com.dynamide.Persistent;

import com.dynamide.util.Tools;

import org.jaxen.XPath;

import org.jdom.Element;

public class Account extends Persistent {
    public Account(DynamideObject owner, String filename, String accountName)
    throws Exception {
        super(owner, filename, null);
        m_accountName = accountName;
        /*
         <account>
            <allowAssemblySearch>true</allowAssemblySearch>
            <defaultAssemblies>
                <ASSEMBLY>
                   <name>com-dynamide-apps</name>
                   <interface>1</interface>
        */
        XPath xpAllowAssemblySearch = prepare("/account/allowAssemblySearch");
        XPath xpASSEMBLY = prepare("defaultAssemblies/ASSEMBLY");
        XPath xpName = prepare("name");
        XPath xpInterface = prepare("interface");

        Element rootEl = getRootElement();
        setAllowAssemblySearch(Tools.isTrue(xpAllowAssemblySearch.valueOf(rootEl)));

        String sName;
        String sInterface;

        List assemblyList = select(rootEl, xpASSEMBLY);
        for (Iterator iter = assemblyList.iterator(); iter.hasNext(); ) {
            Element assemblyEl = (Element)iter.next();
            sName = xpName.valueOf(assemblyEl);
            sInterface = xpInterface.valueOf(assemblyEl);
            sInterface = sInterface == null ? "" : sInterface.trim();
            if (sInterface.length()==0){
                sInterface = Assembly.DEFAULT_INTERFACE_NUMBER;
                logWarn("/account/defaultAssemblies/ASSEMBLY/interface element not found in account.xml for '"+sName+"' in "+filename
                +" Using default interface number: "+Assembly.DEFAULT_INTERFACE_NUMBER);
            }
            if ( sName != null && sName.length()>0) {
                assemblyDefaults.put(sName, sInterface);
            }
        }
    }

    /** Forbidden
     */
    public Object clone(){
        return null;
    }

    private Map assemblyDefaults = new HashMap();
    public String lookupAssemblyDefault(String assemblyName){
        return (String)assemblyDefaults.get(assemblyName);
    }

    private boolean allowAssemblySearch = false;
    public boolean getAllowAssemblySearch(){
        return allowAssemblySearch;
    }
    public void setAllowAssemblySearch(boolean new_allowAssemblySearch){
        allowAssemblySearch = new_allowAssemblySearch;
    }

    private String m_accountName = "";
    public String getAccountName(){return m_accountName;}
    public void setAccountName(String new_value){m_accountName = new_value;}

    public String getCategoryID(){
        return "com.dynamide.resource.Account."+m_accountName;
    }

    public String toString(){
        return "Account: "+m_accountName+','+assemblyDefaults+','+allowAssemblySearch;
    }

}
