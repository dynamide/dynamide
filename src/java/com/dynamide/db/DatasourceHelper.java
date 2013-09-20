package com.dynamide.db;

import java.util.Iterator;
import java.util.Map;

import com.dynamide.Field;
import com.dynamide.datatypes.DatatypeException;

import com.dynamide.util.Log;

public class DatasourceHelper implements IDatasource {

    public DatasourceHelper(IDatasourceBasic datasource){
        this.datasource = datasource;
    }

    private IDatasourceBasic datasource = null;
    public IDatasourceBasic getDatasource(){return datasource;}

    /** @return A pointer to this.
     */
    public IDatasource getDatasourceHelper(){
        return this;
    }

    public Iterator iterator(){
        return datasource.iterator();
    }

    public String getID() {
        return datasource.getID();
    }

    /** If you want a String value, try get(String).
     */
    public Field getField(String fieldName) {
        return datasource.getField(fieldName);
    }

    public Field getField(String fieldName, String fieldIndex) {
        if ( ! (datasource instanceof IDatasource) ) {
            Log.warn(DatasourceHelper.class, "getField(String,String) called on IDatasourceBasic. Returning un-indexed Field for: "+fieldName);
            return datasource.getField(fieldName);
        }
        return ((IDatasource)datasource).getField(fieldName, fieldIndex);
    }


    public Object get(String what)
    throws Exception {
        return datasource.get(what);
    }

    public com.dynamide.Property getProperty(String propertyName)
    throws Exception{
        if ( ! (datasource instanceof IDatasource) ) {
            return null;
        }
        return ((IDatasource)datasource).getProperty(propertyName);
    }

    public void setProperty(String name, String value)
    throws com.dynamide.datatypes.DatatypeException{
        if ( (datasource instanceof IDatasource) ) {
            ((IDatasource)datasource).setProperty(name, value);
        }
    }


    public Map getFields() {
        return datasource.getFields();
    }

    public String dumpErrorsHTML(){
        if ( datasource instanceof IDatasource ) {
            return ((IDatasource)datasource).dumpErrorsHTML();
        }
        return "";
    }

    public void setFieldValue(String fieldName, Object value)
    throws DatatypeException{
        if ( datasource instanceof IDatasource ) {
            ((IDatasource)datasource).setFieldValue(fieldName, value);
        }
    }

    public boolean setFieldValue(String fieldName, Object value, String fieldIndex){
        return false;
    }

    public boolean isReadOnly() {
        if ( ! (datasource instanceof IDatasource) ) {
            return true;
        }
        return ((IDatasource)datasource).isReadOnly();
    }

    public boolean post() {
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).post();
    }

    public void cancel() {
        if ( ! (datasource instanceof IDatasource) ) {
            return;
        }
        ((IDatasource)datasource).cancel();
    }

    public void reload() throws Exception {
        if ( ! (datasource instanceof IDatasource) ) {
            return;
        }
        ((IDatasource)datasource).reload();
    }

    public void clear() {
        if ( ! (datasource instanceof IDatasource) ) {
            return;
        }
        ((IDatasource)datasource).clear();
    }

    public boolean go(int distance) {
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).go(distance);
    }

    public boolean seekBegin(){
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).seekBegin();
    }

    public boolean seekEnd(){
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).seekEnd();
    }

    public boolean seek(int zeroBasedIndex){
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).seek(zeroBasedIndex);
    }


    public boolean insertRow(int index){
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).insertRow(index);
    }

    public boolean isRowCountAllowed(){
        if ( ! (datasource instanceof IDatasource) ) {
            return false;
        }
        return ((IDatasource)datasource).isRowCountAllowed();
    }

    public int getRowCount(){
        if ( ! (datasource instanceof IDatasource) ) {
            return IDatasource.ROW_COUNT_NOT_ALLOWED;
        }
        return ((IDatasource)datasource).getRowCount();
    }

    public int getCurrentRowIndex(){
        if ( ! (datasource instanceof IDatasource) ) {
            return IDatasource.ROW_INDEX_UNKNOWN;
        }
        return ((IDatasource)datasource).getCurrentRowIndex();
    }

    public void onRowChanged(){
        if ( ! (datasource instanceof IDatasource) ) {
            return;
        }
        ((IDatasource)datasource).onRowChanged();
    }


}
