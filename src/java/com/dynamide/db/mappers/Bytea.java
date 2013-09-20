package com.dynamide.db.mappers;

import com.dynamide.db.NullData;

public class Bytea implements IColumnTypeMapper {
    public String onColumn(Object data){
        if (data instanceof NullData){
            return "";
        }
        String res = com.dynamide.util.StringTools.decodeBytea(data);
        //com.dynamide.util.Log.debug(Bytea.class, "Bytea mapper "+res);
        return res;
    }
}
