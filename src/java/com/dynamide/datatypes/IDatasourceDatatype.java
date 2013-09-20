package com.dynamide.datatypes;

import com.dynamide.DynamideObject;
import com.dynamide.Session;

/** Use this datatype to store pointers to widgets that expose IDatasource.
 *   <ul>
 *   <li>value</li>
 *   </ul>
 *</p>
 */
public class IDatasourceDatatype extends StringDatatype {
    public IDatasourceDatatype(){
    }

    public IDatasourceDatatype(DynamideObject owner, Session session){
        super(owner, session);
    }

    public IDatasourceDatatype(String value){
        this();
        set("value", value);
    }

}
