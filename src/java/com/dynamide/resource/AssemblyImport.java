package com.dynamide.resource;

import com.dynamide.util.StringList;

public class AssemblyImport {
    public AssemblyImport(String basename, String build, StringList interfaceNumbers){
        this.basename = basename;
        if ( build == null ) {
            build = "";
        }
        this.build = build;
        this.interfaceNumbers = interfaceNumbers;
    }

    /** The basename of the assembly to be imported.
     */
    public String basename;

    /** A single build number to import, has precedence over interface numbers.
     */
    public String build;

    /** The list of acceptable import interface numbers, stored with 0 being the highest preference.
     */
    public StringList interfaceNumbers;

    /** Reference to the selected imported assembly, once it has been resolved.  May be null.
     */
    public Assembly assembly = null;

    public String toString(){
        return "AssemblyImport{basename:"+basename+";build:"+build+";interfaces:{"+interfaceNumbers+"};assembly:"+assembly+"}";
    }
}
