package com.dynamide;

//import com.dynamide.util.SearchLocations;

/** This class stands in place of a real session when things need to be expanded from a static page context.
 */
public class SessionLite {
    private String  m_URLPath = "";
    public String  getURLPath(){return m_URLPath;}
    public void setURLPath(String  new_value){m_URLPath = new_value;}

    //private SearchLocations m_searchLocations = null;
    //public SearchLocations getSearchLocations(){return m_searchLocations;}
    //public void setSearchLocations(SearchLocations new_value){m_searchLocations = new_value;}

    public String getInclude(String includeName){  // e.g. "js/page.js"
        //String res = m_searchLocations.getInclude(includeName);
        //System.out.println("SESSION_LITE: "+includeName +" ==> "+res);//+"     "+getTemplateDirs());
        //return res;
         return "";
    }

    public String getTemplateDirs(){
        return ""; //%% return m_searchLocations.getTemplatePaths();
    }


    public Object get(String what){
        return "";
    }
}

