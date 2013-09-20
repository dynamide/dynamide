package com.dynamide.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.webmacro.datatable.SimpleTableRow;

import com.dynamide.util.Log;
import com.dynamide.util.Tools;

public class RowDetailAdapter {
    public RowDetailAdapter(){ //int columnCount){
        //for (int i=0; i < columnCount; i++) {
        //    Cell cell = new Cell();
        //    cells.add(cell);
        //}
    }

    public String toString(){
        StringBuffer b = new StringBuffer("RowDetailAdapter: {\r\n");
        for (Iterator i=cells.iterator(); i.hasNext();) {
            b.append("\t");
            b.append(i.next().toString());
            b.append("\r\n");
        }
        b.append("}");
        return b.toString();
    }

    private SimpleTableRow m_tableRow = null;
    public SimpleTableRow getTableRow(){return m_tableRow;}
    public void setTableRow(SimpleTableRow new_value){m_tableRow = new_value;}

    private String  m_updateConditions = "";
    public String  getUpdateConditions(){return m_updateConditions;}
    public void setUpdateConditions(String  new_value){m_updateConditions = new_value;}

    private List cells = new Vector();
    public List getCells(){
        return cells;
    }

    public int getColumnCount(){
        return cells.size();
    }

    public Cell getCell(int i){
        return (Cell)cells.get(i);
    }

    public Cell getCellByFieldName(String fieldName){
        if ( fieldName == null || fieldName.length()==0 ) {
            return null;
        }
        for (Iterator it=cells.iterator();it.hasNext();){
            Cell cell = (Cell)it.next();
            if (fieldName.equals(cell.fieldName)){
                return cell;
            }
        }
        return null;
    }

    public String getValue(String columnName){
        Cell cell = getCellByColumnName(columnName);
        if ( cell == null ) {
            Log.error(RowDetailAdapter.class, "Cell not found: '"+columnName+"' in: "+toString());
            return "";
        }
        return cell.value;
    }

    public Cell getCellByColumnName(String columnName){
        if ( columnName == null || columnName.length()==0 ) {
            return null;
        }
        for (Iterator it=cells.iterator();it.hasNext();){
            Cell cell = (Cell)it.next();
            if (columnName.equals(cell.columnName)){
                return cell;
            }
        }
        return null;
    }

    public Map getInsertMap(){
        Map v = Tools.createSortedCaseInsensitiveMap();
        for (Iterator it=cells.iterator();it.hasNext();){
            Cell cell = (Cell)it.next();
            if (cell.insert){
                v.put(cell.columnName, cell.value);
            }
        }
        return v;
    }


    public Cell createCell(String columnName, String label, String value, String widgetType){
        return createCell(columnName, label, value, widgetType, true);
    }

    /** @param insert Include this column when creating a list of values to INSERT into the DB.
     */
    public Cell createCell(String columnName, String label, String value, String widgetType, boolean insert){
        Cell cell = new Cell();
        cell.columnName = columnName;
        cell.label = label;
        cell.value = value;
        cell.widgetType = widgetType;
        cell.insert = insert;
        cells.add(cell);
        return cell;
    }

    public Cell createCellFromRow(String columnName, String label, String widgetType)
    throws Exception {
        return createCellFromRow(columnName, label, widgetType, true);
    }

    public Cell createCellFromRow(String columnName, String label, String widgetType, boolean insert)
    throws Exception {
        if (m_tableRow == null){
            throw new Exception("TableRow must be set in RowDetailAdapter");
        }
        Cell cell = new Cell();
        cell.columnName = columnName;
        cell.label = label;
        Object o = m_tableRow.get(columnName);
        try {
            if (o == null) {
                cell.value = "";
            } else {
                if (o.getClass().isArray() && java.lang.reflect.Array.getLength(o)>0){
                    cell.value = new String((byte[])o);
                    System.out.println("$$$$$$$$$$$$ cell.value: "+cell.value);
                } else {
                    cell.value = m_tableRow.get(columnName).toString();
                }
            }
        } catch (Exception e)  {
            Log.error(RowDetailAdapter.class, "error in createCellFromRow "+(o!=null?o.getClass().getName():"null"), e);
            cell.value = "";
        }
        cell.widgetType = widgetType;
        cell.insert = insert;
        cells.add(cell);
        return cell;
    }

    public class Cell {
        public String label = "";
        public String columnName = "";
        public String fieldName = "";
        public String value = "";
        //public String dbDatatype = "";
        public String widgetType = "";  //set to hidden, edit, label.  label would allow you to display but not edit.
        public boolean insert = true;
        public String toString(){
            return "Cell: {"+label+';'+columnName+';'+fieldName+';'+value+';'+widgetType+'}';
        }
    }

}
