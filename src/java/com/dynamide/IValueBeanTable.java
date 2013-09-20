package com.dynamide;

import java.util.Iterator;

public interface IValueBeanTable extends IValueBeanList {
    public Object cell(int r, int c);
    public int getCurrentColumn();
    public Iterator getColumnNames();
    public String /* %%: datatype*/ getType(int c);
}
