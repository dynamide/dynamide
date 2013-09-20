    package com.dynamide.datatypes;

import org.jdom.Element;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Session;

/** A simple datatype to wrap version information, as used by Dynamide Assemblies.
    Example:
        &lt;property name="items"&gt;
            &lt;datatype&gt;com.dynamide.datatypes.Enumeration&lt;/datatype&gt;
            &lt;value&gt;
                &lt;enumeration override="true"&gt;
                    &lt;item datatype="com.dynamide.datatypes.Version"&gt;
                        &lt;basename&gt;com-dynamide-lib&lt;/basename&gt;
                        &lt;interface&gt;1&lt;/interface&gt;
                        &lt;build&gt;59+&lt;/build&gt;
                    &lt;/item&gt;
             ...
 * <p>Defines the following elements:
 *
<ul>
<li>basename</li>
<li>interface</li>
<li>build</li>
</ul>
 * </p>
 */
public class Version extends ExpandableDatatype{
    public Version (){
    }


    public Version(DynamideObject owner, Session session){
        super(owner, session);
    }

    public Version (String basename, String interfaceString, String build){
        set("basename", basename);
        set("interface", interfaceString);
        set("build", build);
    }

    public void init(Element element, String ownerID) throws DatatypeException {   //we've been passed the <item> element.
        set("basename", JDOMFile.safeGetElementChildText(element, "basename"));
        set("interface", JDOMFile.safeGetElementChildText(element, "interface"));
        set("build", JDOMFile.safeGetElementChildText(element, "build"));
    }

    public void addXMLContentTo(Element container){
        Element basename = new Element("basename");
        basename.addContent(getAsStored("basename"));
        container.addContent(basename);

        Element build = new Element("build");
        build.addContent(getAsStored("build"));
        container.addContent(build);

        Element interfaceEl = new Element("interface");
        interfaceEl.addContent(getAsStored("interface"));
        container.addContent(interfaceEl);
    }

    public String toString(){
        return "com.dynamide.datatypes.Version: basename="+getAsStored("basename")+" interface="+getAsStored("interface")+" build="+getAsStored("build");
    }

    public ValidationResult validate(){
        /*check for:
         *   maxLength
         *   minLength
         *   mask
         */
        return new ValidationResult(true);
    }

    public String getDotName(){
        String dotname;
        String vs = getAsStored("basename")+","+getAsStored("interface")+","+getAsStored("build");
        if (getOwner()!=null){
            dotname = getOwner().getDotName()+'.'+vs;
        } else {
            dotname = "NO-OWNER"+'.'+vs;
        }
        return dotname;
    }

}
