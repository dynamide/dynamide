package com.dynamide;


public interface IValueBean {
    public Object value();
    public Object cell();
    public String getType();
    public void setCellValue(int r, int c, Object value);
}
