package com.dynamide.db;

import java.util.Iterator;
import java.util.Map;

import com.dynamide.Field;
import com.dynamide.datatypes.DatatypeException;

/**
 * All datasources should implement this interface, which allows them to plug into the Dynamide framework.
 * All the Dynamide Widgets get their values from an IDatasource, so by implementing this interface, all the
 * Dynamide Widgets are data-aware for your particular datasource implementation.
 *
 * <p>Implementations are expected to handle their own lazy initialization if desired.  Since there is
 * no open() call, the underlying data provider should be opened when getFieldValue(), next() or go() is called.
 * Alternatively, open the underlying data during construction.
 *
 * <p>Implementations should notify all Fields by using Field.set("value", value), etc.,
 * when the underlying data changes.   The Fields are registered
 * with the Session, and will continue to hold their values until the Field.setValue() method is called.  It is not
 * necessary to notify Widgets, Pages or the Session when changing Field values, as long as the Field object
 * reference itself is not changed.
 *
 * <p>Implementations may wish to provide the following method or constructor arguments
 *  which are not part of the interface:
 *     <ul>
 *      <li> setColumnNames -- set list of columns, or update this when data are available.</li>
 *      <li> a constructor or open() method to retrieve the data.  Once constructed and registered
 *           with the Session, the datasource is assumed to be on the first "row", with data available.
 *           Implementors are responsible for lazy or delayed initialization.
 *     </ul>
 *
 */
public interface IDatasource extends IDatasourceBasic {

    /** Constant used in go(int) to navigate to the first record.
      */
    public static final int BEFORE_FIRST = Integer.MIN_VALUE+2;

    /** Constant used in go(int) to navigate to the first record.
      */
    public static final int BEGIN = Integer.MIN_VALUE;

    /** Constant used in go(int) to navigate to the last record.
      */
    public static final int END = Integer.MAX_VALUE;

    /** Constant used in insertRow(int) to specify that the new row should come before the current row.
      */
    public static final int BEFORE = Integer.MIN_VALUE+1;

    /** Constant used in insertRow(int) to specify that the new row should come after the current row.
      */
    public static final int AFTER = Integer.MAX_VALUE-1;

    /** Constant returned by getCurrentRowIndex() if row indices are not known or are not supported.
      */
    public static final int ROW_INDEX_UNKNOWN = -1;

    /** Constant returned by getRowCount() if row counts are not supported, e.g. large SQL datasets.
      */
    public static final int ROW_COUNT_NOT_ALLOWED = -1;

    //==================== Methods ==================================================================

    /** By default, simply return a reference to "this", since the implementing class is an instance of
     * IDatasource.  This method exists so that IDatasourceBasic implementations
     * can be wrapped in an IDatasource interface.
     *
     * @see IDatasourceBasic
     */
    public IDatasource getDatasourceHelper();

    /** A unique name within the Application for this datasource, it becomes the ID by which
     *  Widgets can discover the datasource.
     */
    public String getID();

    /**  Maintain a list of Field objects; return the live Field
     * when asked by this method. IDatasource supports the concept of the
     * "current row", so that getField indexes into a tabular dataset by the
     * fieldName of the column, for the current row.  The caller gets different
     * cell values by a) calling next() or some other navigation method, or
     * b) calling getField(fieldName, fieldIndex) if indexed fields are supported.
     */
    public Field getField(String fieldName);

    /** This class can support indexed Fields, by any arbitrary String index, which will for
     *  tabular datasets, be the zero-based row index; however the index can be any valid string
     *  which could itself be a search specifier that is used by this method.
     */
    public Field getField(String fieldName, String fieldIndex);

    /** @return a Map of all the fields owned. */
    public Map getFields();

    /*
     * Implementors can return an Object from this method, but make sure that toString() makes sense.
     *  Dynamide won't automatically call toString() on the returned Object, but WebMacro will
     *  default to doing this if, for example, $datasource.getField("foo") is in a Page or Widget template.
     */
    //ublic Object getFieldValue(String fieldName);

    //** Provide a toString method if the value can be an object, and return "" if the field is null -- DO NOT return null
    // * in any case.
    // * @return a non-null String.
    // */
    //ublic String getFieldStringValue(String fieldName);

    /** The editable Dynamide Widgets can use this to modify the underlying Fields.  If the underlying
     * dataset does not support updates, you may wish to implement this as a no-op.
     */
    public void setFieldValue(String fieldName, Object value)
    throws DatatypeException;

    /** Optional operation: Dynamide Widgets can use this to modify the underlying datasource if the
     *  datasource supports indexed Fields.  If the underlying
     *  dataset does not support indexed Fields, you can return false from the method.
     */
    public boolean setFieldValue(String fieldName, Object value, String fieldIndex);

    /** Method to work with things like WebMacro, that automatically call Foo.get("bar") when
     *  you invoke $foo.bar if foo is of class Foo.
     *  The Dynamide Session search order should be useful for most implementations:
     *  get(String) searches Property objects then Fields.
     */
    public Object get(String what) throws Exception ;

    //ublic String getString(String what);

    /** Rather than having a complicated interface to IDatasource, specialized behaviors
     *  can be set/retrieved using setProperty/getProperty, for example, "isMultiRowEditable".
     *  Most implementations should also just return the Property String value from any calls
     *  to IDatasource.get(String).
     *  @see #get(String)
     */
    public com.dynamide.Property getProperty(String propertyName)
    throws Exception;

    /** Rather than having a complicated interface to IDatasource, specialized behaviors
     *  can be set/retrieved using setProperty/getProperty, for example, "isMultiRowEditable".
     */
    public void setProperty(String name, String value)
    throws com.dynamide.datatypes.DatatypeException;


    /**
     *  @see com.dynamide.db.IDatasourceBasic#iterator()
     */
    public Iterator iterator();


    /** Updateable datasets should return false.
     */
    public boolean isReadOnly();

    /** If isReadOnly() returns false, and the underlying data can be updated, return true.  Otherwise return false.
     */
    public boolean post();

    public void clear();
    
    public void cancel();
    
    public void reload() throws Exception;

    /** go(0) should go to the first row in the set, if supported,
     *  go(-1) should go back a row, if supported,
     *  go(1) should go forward a row if supported,
     *  go(IDatasource.END) should go to the last row in the set,
     *  leaving the last row active, that is, not after the last row,
     *  and all unsupported actions should simply be no-ops.
     *
     * @return true if navigation to a different row was achieved.
     */
    public boolean go(int distance);

    public boolean seekBegin();

    public boolean seekEnd();

    /** Jump to the absolute zero based index.
     */
    public boolean seek(int zeroBasedIndex);

    /** Provides notification that a seek or go operation has occured.
     */
    public void onRowChanged();


    /** @param index Is one of IDatasource.BEGIN, IDatasource.END, IDatasource.AFTER, IDatasource.BEFORE
     *  or is an absolute index the new row will occupy.  For examle, if there is one row in the dataset,
     *  then insertRow(0) would insert the new row at index 0, before the current row, while insertRow(1)
     *  would place the new row at index 1, after the current row.  IDatasource.AFTER would place the
     *  new row after the current row, regardless of absolute index, and IDatasource.BEFORE would place the row
     *  before the current row.
     *
     *  @return false if operation is not supported or raises an error.
     */
    public boolean insertRow(int index);

    /** Report if calling getRowCount() will be allowed.
     *  For some datasets it may not be advisable, e.g. for large SQL datasets, to allow getRowCount(),
     *  but this method simply tells the caller whether calling getRowCount() will return meaningful data.
     *  getRowCount() may return IDatasource.ROW_COUNT_NOT_ALLOWED, but does not throw Exceptions.
     *
     *  @return true if calling getRowCount() is allowed
     */
    public boolean isRowCountAllowed();

    /** The row count of the current dataset, or IDatasource.ROW_COUNT_NOT_ALLOWED if the operation
     * is not supported.  If not supported, be sure to return false from isRowCountAllowed().
     *
     * @return the current row count, or IDatasource.ROW_COUNT_NOT_ALLOWED
     */
    public int getRowCount();

    /** The zero-based index of the current row: single-row implementations can always return 0,
     * implementations that don't support row indices should return IDatasource.ROW_INDEX_UNKNOWN;
     */
    public int getCurrentRowIndex();


    public String dumpErrorsHTML();

}
