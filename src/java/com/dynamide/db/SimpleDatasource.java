// Test with dynamide/src/shell/tests/test-SimpleDatasource.bs
package com.dynamide.db;

import java.util.*;

import com.dynamide.AbstractWidget;
import com.dynamide.DynamideObject;
import com.dynamide.Field;
import com.dynamide.Property;
import com.dynamide.Session;

import com.dynamide.datatypes.DatatypeException;

import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

/** Implements the IDatasource interface as a simple grid; NOTE: Column names default to CASE-INSENSITIVE,
 * unless setCaseInsensitive(false) is called.  This class is not backed by any persistent store --
 * it is just for keeping values in memory.
 *
 * <p>Test with dynamide/src/shell/tests/test-SimpleDatasource.bs
 *
 * <p>Here's some notes on the implementation:
 * <pre>
 * Registered Fields:
    current row:       A      B     C

    rows:              A0     B0    D0
                       A1     B1    C1
                       A2     B2    C2


single row widget:
                  cb.field = "A"        edit.field = "B"        label.field = "C"
                  cb.fieldIndex = ""    edit.fieldIndex = ""    label.fieldIndex = ""

multi row input grid:
                  cb.field = "A"        edit.field = "B"        label.field = "C"
                  cb.fieldIndex = "0"   edit.fieldIndex = "0"   label.fieldIndex = "0"

                  cb.field = "A"        edit.field = "B"        label.field = "C"
                  cb.fieldIndex = "1"   edit.fieldIndex = "1"   label.fieldIndex = "1"

                  cb.field = "A"        edit.field = "B"        label.field = "C"
                  cb.fieldIndex = "2"   edit.fieldIndex = "2"   label.fieldIndex = "2"
  </pre>
*/
public class SimpleDatasource extends DynamideObject
implements IDatasource {

    public SimpleDatasource(DynamideObject owner){
        super(owner);
    }

    public SimpleDatasource(){
        this(null);
    }

    public SimpleDatasource(DynamideObject owner, Session session){
        this(owner);
        setSession(session);
    }

    public class SimpleDatasourceIterator implements Iterator {
        private int m_iterCurrentRowIndex = -1;
        private int m_iterRowCount = 0;
        private SimpleDatasource m_target;
        private StringList m_iterCurrentRow;
        public SimpleDatasourceIterator(SimpleDatasource target){
            m_iterRowCount = target.getRowCount();
            m_target = target;
        }
        public boolean goIter(int distance){
            int newi = SimpleDatasource.distanceToIndex(distance, m_iterCurrentRowIndex, m_iterRowCount);
            if ( newi  == -1){
                return false;
            }
            m_iterCurrentRowIndex = newi;
            m_iterCurrentRow = m_target.getRow(newi);
            //System.out.println("goIter: "+newi + " row: "+m_iterCurrentRow);
            return true;
        }
        public Object next(){
            if (goIter(1)){
                return m_iterCurrentRow;
            }
            return null;
        }
        public boolean hasNext(){
            return (m_iterCurrentRowIndex < (m_iterRowCount-1));
        }
        public void remove(){
            throw new UnsupportedOperationException();
        }

    }


    private Session m_session = null;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    private StringList m_currentRow = new StringList(false);

    /*
     * //debug only:
     * //public Map getCurrentRow(){return m_currentRow;}
     * you could look at this with:
     *
     *               <table border='1' cellpadding='4' cellspacing='0'>
     *               #set $st = $session.get("ROOT_WEBAPPS")
     *               #foreach $f in $st.getCurrentRow().entrySet() {
     *                 <tr>
     *                   <td>$f.getValue().getClass().getName()</td>
     *                   <td>$f.getKey()</td>
     *                   <td>$f.getValue().get("value")</td>
     *                 </tr>
     *               }
     *               </table>
     */


    private Vector m_rows = new Vector();

    public StringList getRow(int index){
        return (StringList)m_rows.get(index);
    }

    public StringList getCurrentRow(){
        return m_currentRow;
    }

    protected void setCurrentRow(StringList newRow){
        m_currentRow = newRow;
        // %% updateCurrentRowPointers() ?????
    }

    public StringList clearCurrentRow(){
        m_currentRow = createEmptyRow();
        updateCurrentRowPointers();
        return m_currentRow;
    }

    private StringList createEmptyRow(){
        Session session = getSession();
        StringList row = new StringList(false);
        try {
            Iterator it = m_columnNames.keysList().iterator();
            while ( it.hasNext() ) {
                String fieldName = (String)it.next();
                Field aField = new Field(this, session, fieldName, "");
                row.put(fieldName, aField);
            }
        } catch (DatatypeException e)  {
            logError("could not createEmptyRow", e);
        }
        return row;
    }

    private boolean m_readOnly = false;

    private boolean m_caseInsensitive = true;
    public boolean getCaseInsensitive(){return m_caseInsensitive;}
    public void setCaseInsensitive(boolean new_value){m_caseInsensitive = new_value;}

    private StringList m_columnNames = new StringList(false);
    public String  getColumnNames(){
        return Tools.collectionToString(m_columnNames.keysList(), ",");
    }
    /** Pass in a comma separated list of column names, and the columns will
     *  be created, without any associated metadata.  To set metadata, use
     *  addColumn() for each column, which should be called before addRow().
     */
    public void setColumnNames(String  new_value){
        Vector v = StringTools.parseSeparatedValues(new_value, ",");
        Iterator it = v.iterator();
        while ( it.hasNext() ) {
            String key = (String)it.next();
            addColumn(key, "");
        }
    }

    public List getColumnNamesList(){
        return m_columnNames.keysList();
    }

    public Object getMetadataForColumn(String fieldName){
        return m_columnNames.getObject(fieldName);
    }




    //================== Methods for SimpleDatasource not in IDatasource =============

    public String getCategoryID(){
        return getClass().getName()+"."+getID();
    }

    protected String fixCase(String fieldName){
        if ( m_caseInsensitive ) {
            return fieldName.toUpperCase();
        }
        return fieldName;
    }

    public void setReadOnly(boolean new_value){
        m_readOnly = new_value;
    }

    private boolean m_inUpdateCurrentRowPointers = false;

    protected void updateCurrentRowPointers(){
        if (m_inUpdateCurrentRowPointers){
            logError("Can't updateCurrentRowPointers because function was called recursively. "+Tools.getStackTrace());
            return;
        }
        m_inUpdateCurrentRowPointers = true;
        try {
            Session session = getSession();
            if ( session == null ) {
                logDebug("SimpleDatasource.updateCurrentRowPointers :: session is null");
                return;
            }
            if (m_currentRow!=null){
                for (Iterator it = m_currentRow.objectsCollection().iterator();it.hasNext();){
                    Field field = (Field)it.next();
                    String basename = Field.formatFullFieldName(getID(), field.getName(), ""); //register with no index.
                    session.setField(basename, field); //should replace previous, if any.
                }
            }
            onRowChanged();
        } finally {
            m_inUpdateCurrentRowPointers = false;
        }
    }

    /** Subclasses can override to hook this notification; nothing is done in this class.
     */
    public void onRowChanged(){
    }

    public String dump(){
        return dump(";");
    }

    public String dump(String nl){
        StringBuffer result = new StringBuffer();
        for (Iterator it = m_rows.iterator();it.hasNext();){
            StringList value = (StringList)it.next();
            result.append(value.dump());
            for (Iterator it2 = value.objectsCollection().iterator();it2.hasNext();){
                Object o = it2.next();
                result.append("\r\nobject:"+o.getClass().getName());
                if ( o instanceof DynamideObject ) {
                    result.append(" id: "+((DynamideObject)o).getObjectID());
                }
            }
            result.append(nl);
        }
        return result.toString();
    }

    public String dumpRow(){
        return dumpRow(""+getCurrentRowIndex(), m_currentRow.getMap());
    }

    public static String dumpRow(StringList row){
        return dumpRow("", row.getMap());
    }

    public static String dumpRow(String id, Map row){
        StringBuffer result = new StringBuffer();
        result.append(id).append("::");
        Iterator it = row.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry en = (Map.Entry)it.next();
            String fieldName = (String)en.getKey();
            Field field = (Field)en.getValue();
            result.append(fieldName)
                  .append(":")
                  .append(field.getValue())
                  //.append(":cn:")
                  //.append(field.getClass().getName())
                  .append(";");
        }
        return result.toString();
    }

    public String dumpErrorsHTML(){
        int idx = getCurrentRowIndex();
        Map cur = m_currentRow.getMap();
        if (idx>-1&&cur!=null){
            StringBuffer b = new StringBuffer();
            Iterator it = cur.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry en = (Map.Entry)it.next();
                String fieldName = (String)en.getKey();
                Field field = (Field)en.getValue();
                if ( field.getError() ) {
                    b.append(field.dumpHTML());
                }
            }
            return b.toString();
        }
        return "";
    }

    public String toString(){
        StringBuffer result = new StringBuffer();
        result.append(getClass().getName()+":"+getDotName()+":getCurrentRowIndex:"+getCurrentRowIndex()+";getRowCount:"+getRowCount());
        result.append(":Columns:");
        result.append(Tools.collectionToString(m_currentRow.keysList(), ","));
        return result.toString();
    }

    //=========== Methods that are not in IDatasource, but should be ========================

    /** Clear the columns and cached data rows from this object, not necessarily from the underlying back end data --
     *  calls clearColumns() and clearRows().
     */
    public void clear(){
        clearColumns();
        clearRows();
    }

    public void clearColumns(){
        m_columnNames.clear();
    }

    public void clearRows(){
        m_rows = new Vector();
        m_currentRow = createEmptyRow();
    }

    public boolean seekBegin(){
        return go(IDatasource.BEGIN);
    }
    public boolean seekEnd(){
        return go(IDatasource.END);
    }

    public void addRow(){
        insertRow(getRowCount());
    }

    public void insertRow(){
        int index = m_rows.indexOf(m_currentRow);
        if (index==-1){
            index=0;
        }
        insertRow(index);
    }

    public boolean insertRow(int index){
       // Session session = getSession();
        if ( m_columnNames.size()==0 ) {
            logWarn("SimpleDatasource.insertRow: column count is zero");
        }
        StringList row = createEmptyRow();
        m_rows.add(index, row);
        //todo: %% handle insert in middle, with renumbering.
        m_currentRow = row;
        updateCurrentRowPointers();
        return true;
    }

    public boolean deleteRow(){
        return deleteRow(getCurrentRowIndex());
    }

    public boolean deleteRow(int index){
        int size = m_rows.size();
        if ( index > size || index < -1 ) {
            return false;
        }
        m_rows.remove(index);
        size = m_rows.size();
        if (size == 0){
            m_currentRow = new StringList(); //null; //%% make sure this isn't null pointer exception.
        } else if ( index > size ) {
            m_currentRow = (StringList)m_rows.elementAt(size-1);
        } else {
            m_currentRow = (StringList)m_rows.elementAt(index);
        }
        updateCurrentRowPointers();
        return true;
    }

    //========================= Methods from IDatasource ========================

    public IDatasource getDatasourceHelper(){
        return this;
    }

    private int m_iteratorIndex = 0;

    private boolean m_iteratorIsBeforeFirst = false;

    public Iterator iterator(){
        return new SimpleDatasourceIterator(this);
        /*
        go(IDatasource.BEGIN);
        m_iteratorIsBeforeFirst = true;
        return this;
         * */
    }

    public void addColumn(String fieldName, Object metadata){
        fieldName = fixCase(fieldName);
        m_columnNames.addObject(fieldName, metadata);
    }

    public Field addField(String fieldName, Object value, Object metadata)
    throws DatatypeException {
        fieldName = fixCase(fieldName);
        Field aField = new Field(this, getSession(), fieldName, value);
        addColumn(fieldName, metadata);
        return aField;
    }

    public Field addField(String fieldName, Object value)
    throws DatatypeException {
        return addField(fieldName, value, null);
    }

    public String  getID(){return super.getID();}

    public Map getFields(){
        return m_currentRow.getMap(); //does a clone.
    }

    public Field getField(String fieldName){
        return (Field)m_currentRow.get(fixCase(fieldName));
    }

    public Field getField(String fieldName, String fieldIndex) {
        int iFieldIndex = Tools.stringToIntSafe(fieldIndex, -1);
        if ( iFieldIndex > -1 ) {
            StringList aRow = (StringList)m_rows.get(iFieldIndex);
            Field f = (Field)aRow.get(fixCase(fieldName));
            logDebug("SimpleDatasource.getField returning indexed Field: "+f);
            return f;
        } else {
            logError("============= SimpleDatasource.getField(String fieldName, String fieldIndex) did not find a row by index: "+fieldIndex);
        }
        return getField(fieldName);
    }


    public Object get(String what){
        Property p = getProperty(what);
        if ( p != null ) {
            return p.getValue();
        }
        Field f = getField(what);
        if ( f!=null ) {
            Object obj = f.get("value");
            String value = (obj == null ? "" : obj.toString());
            AbstractWidget.logFromInfo(this.getClass(), "field", getDotName()+"."+what, value);
            return value;
        }
        try {
            Object value = super.get(what);
            AbstractWidget.logFromInfo(this.getClass(), "super.get", getDotName()+"."+what, ""+value);
            return value;
        } catch( Exception e ){
            logError("in SimpleDatasource.get(\""+what+"\")", e);
            return null;
        }
    }

    public void setFieldValue(String fieldName, Object value){
        Field f = getField(fieldName);
        if ( f!=null ) {
            f.set("value", value);
        }
    }

    public boolean setFieldValue(String fieldName, Object value, String fieldIndex){
        try {
            int iFieldIndex = Tools.stringToInt(fieldIndex);
            StringList aRow = (StringList)m_rows.get(iFieldIndex);
            Field f = (Field)aRow.get(fixCase(fieldName));
            if ( f!=null ) {
                f.set("value", value);
            }
            return true; //means this method is supported.
        } catch (Exception e)  {
            logError("couldn't setFieldValue", e);
            return false;
        }
    }

    public void setFieldError(String fieldName, String errorMsg){
        Field f = getField(fieldName);
        if ( f!=null ) {
            f.setErrorMessage(errorMsg);
        }
    }

    public boolean hasMore(){
        return ( getCurrentRowIndex() < (getRowCount()-1) );
    }

    public boolean isCurrentRow(){
        return true;//%%%%% not clear if this is correct....
    }

    public boolean isReadOnly(){
        return m_readOnly;
    }

    public boolean post(){
        try {
            updateCurrentRowPointers();
            return true;
        } catch (Exception e)  {
            logError("Couldn't post() in SimpleDatatype", e);
            return false;
        }
    }

    public void cancel() {
    }

    public void reload() throws Exception {
    	seekBegin();
    }

    public boolean next(){
        return go(1);    
    }
    
    public boolean seek(int zeroBasedIndex){
        if ( zeroBasedIndex  < 0 || zeroBasedIndex > getRowCount()){
            return false;
        }
        m_currentRow = (StringList)m_rows.get(zeroBasedIndex);
        try {
            updateCurrentRowPointers();
        } catch (Exception e)  {
            logError("SimpleDatatype.seek() couldn't updateCurrentRowPointers()", e);
            return false;
        }
        return true;
    }

    public boolean go(int distance){
        int newi = distanceToIndex(distance);
        if ( newi  == -1){
            return false;
        }
        m_currentRow = (StringList)m_rows.get(newi);
        try {
            updateCurrentRowPointers();
        } catch (Exception e)  {
            logError("SimpleDatatype.go() couldn't updateCurrentRowPointers()", e);
            return false;
        }
        return true;
    }

    public int distanceToIndex(int distance){
        return distanceToIndex(distance, getCurrentRowIndex(), getRowCount());
    }

    public static int distanceToIndex(int distance, int currentRowIndex, int rowCount){
        int newi;
        if ( distance == IDatasource.END ) {
            newi = rowCount-1;  // isRowCountAllowed is always true for this class.
        } else if (distance == IDatasource.BEGIN) {
            newi = 0;
        } else {
            newi = currentRowIndex+distance;
        }
        if ( newi >= rowCount || newi < 0 ) {
            return -1;
        }
        return newi;
    }

    public boolean isRowCountAllowed(){
        return true;
    }

    public int getRowCount(){
        return m_rows.size();
    }

    public int getCurrentRowIndex(){
        return m_rows.indexOf(m_currentRow);
    }

    protected StringList m_propertiesTable = new StringList(false);
    protected StringList getProperties(){
        return m_propertiesTable;
    }

    public void setProperty(String name, String value) throws DatatypeException {
        m_propertiesTable.remove(name);
        m_propertiesTable.addObject(name, new Property(null, name, value));
    }

    /** %% this is funky, since right now all are stored as String name and value. isEvent is just lost, for example.*/
    public Property getProperty(String propertyName){
        return  (Property)m_propertiesTable.get(propertyName);
    }

}
