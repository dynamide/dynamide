package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

/**  <p>The Widget class calls DynamideObject.expand(String) to expand all properties, but that doesn't dig into
 *   enumerated types or complex types that contain multiple attributes, so this subclass calls the expansion
 *   whenever its get(String) method is called.  For example, the com.dynamide.datatypes.Link class is an
 *   ExpandableDatatype, and it has attributes "key", "href", and "text".  These need to be expanded, since
 *   they support calls such as get("href").  Simply relying on get("value") won't work.  However, StringDatatype does <b>not</b>
 *   descend from ExpandableDatatype since it has a simple value that will already be expanded by the Widget class.</p>
 *
 *   <p>If you write Datatype descendants that have multiple attributes or are contained by EnumeratedDatatype, then
 *   subclass this class.  com.dynamide.datatypes.StringDatatype can be contained in an enumeration but will not
 *   be expanded, so if you want macros to expand in your enumerated property, use com.dynamide.datatypes.Caption
 *   instead.  The expansion does <b>not</b> handle internationionalization replacements, that is handled through the session.
 *   Each datatype publishes it's "DotName", which for complex types includes the "key" attribute.  The full DotName is
 *   looked up and expanded, regardless of the superclass.</p>
 *
 *   <p>The expansions allow you to store any webmacro directives, variable lookups, and escape sequences in the
 *   stored property values.  You must escape any #, $ or \ characters.</p>
 */
public abstract class ExpandableDatatype extends Datatype {
    public ExpandableDatatype(){
    }

    public ExpandableDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public Object get(String what){
        try {
            return expand(super.get(what).toString());
        } catch (Exception e){
            return "ERROR: [110] "+e.getMessage();
        }
    }
    
    

}
