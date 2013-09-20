package com.dynamide.test.samplebeans;

import java.util.Iterator;
import java.util.Vector;

import com.dynamide.IValueBeanTable;

public class MyValueBean implements IValueBeanTable {

    private static String[][] CELLS = new String[][]{{"0,0", "0,1"},{"1,0","1,1"},{"2,0","2,1"}};

    public Object value(){
        return cell();
    }

    public Object cell(){
        return cell(getCurrentRow(), getCurrentColumn());
    }

    public Object cell(int r) {
        return cell(r, getCurrentColumn());
    }

    public Object cell(int r, int c)  {
        return CELLS[r][c];
    }

    public void setCellValue(int r, int c, Object value){
        CELLS[r][c] = value.toString();
    }

    public int getCurrentRow() {//default to 0
        return 0;
    }

    public int getCurrentColumn(){   //default to 0
        return 0;
    }

    public String toString(){
        return cell().toString();
    }

    public String getType(){
        return "java.lang.String";
    }

    public String getType(int c){
        return getType();
    }

    public int getDisplayRowCount(){
        return CELLS.length;
    }

    public int getDisplayFirstRowNum(){
        return 0;
    }

    public Iterator getColumnNames(){
        Vector v = new Vector();
        v.add("First");
        v.add("Second");
        return v.listIterator();
    }

    public int getColumnCount(){
        return CELLS[0].length;
    }

}
