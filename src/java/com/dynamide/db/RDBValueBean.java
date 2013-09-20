package com.dynamide.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.dynamide.IValueBeanTable;

public class RDBValueBean implements IValueBeanTable {

    public RDBValueBean(String conxStr, String tablename, String driver, String dbuser, String password){
        RDBDatabase d = new RDBDatabase(conxStr, driver, dbuser, password, null); //"jdbc:mysql:///dynamide", "org.gjt.mm.mysql.Driver", user, password
        RDBTable t = new RDBTable(d, tablename); //  "orders"
        boolean ok = t.open(null, null);
        if ( ok ) {
            m_table = t;
        }
    }

    private int m_row = 0;
    private int m_col = 0;
    private RDBTable m_table;

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
        try {
            ResultSet rs = m_table.getResultSet();
            if (c==0){
                if ( r==0 ) {
                    rs.beforeFirst();
                }
                if ( ! rs.isAfterLast() ) {
                    rs.next();
                }
            }
            if ( rs.isAfterLast() ) {
                return "";
            } else {
                return  rs.getString(c+1);
            }
        } catch (SQLException e){
            System.out.println("ERROR 3: "+e);
            return "ERROR 3"+e;
        }
    }

    public void setCellValue(int r, int c, Object value){
        //CELLS[r][c] = value.toString();
        System.out.println("Skipping setCellValue for table.");
    }

    public int getCurrentRow() {//default to 0
        return m_row;
    }

    public int getCurrentColumn(){   //default to 0
        return m_col;
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
        return 50;
    }

    public int getDisplayFirstRowNum(){
        return 0;
    }

    public Iterator getColumnNames(){
        Vector v = new Vector();
        try {
            ResultSetMetaData md = m_table.getResultSetMetaData();
            int count = md.getColumnCount();
            for (int i=0; i < count; i++) {
                v.add(md.getColumnName(i+1));
            }
        } catch (SQLException e){
            System.out.println("ERROR 1: "+e);
        }
        return v.listIterator();
    }

    public int getColumnCount(){
        try {
            ResultSetMetaData md = m_table.getResultSetMetaData();
            return md.getColumnCount();
        } catch (SQLException e){
            System.out.println("ERROR 2: "+e);
            return 0;
        }
    }

    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close(){
        if ( m_table != null ) {
            m_table.close();
        }
    }

    public void update(Map values, String conditions){
        //no params for now, just test it:
        m_table.update(values, conditions);
        m_table.close();
        m_table.open();
    }

}
