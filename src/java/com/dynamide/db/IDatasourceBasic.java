package com.dynamide.db;

import java.util.Iterator;
import java.util.Map;

import com.dynamide.Field;

/** A one-way navigable datasource, that has read-only data.  Columns are wrapped in com.dynamide.Field objects
 *  that are registered with the com.dynamide.Session so that the Widgets can automatically get live data.
 *  If you wish to provide navigation such as seekBegin(), seekEnd(), go(relative) and to provide
 *  update capability such as setFieldValue(...) then use com.dynamide.db.IDatasource instead.
 *
 * <p>
 *  Implementations may wish to provide the following method or constructor arguments
 *  which are not part of the interface:
 *     <ul>
 *      <li> setColumnNames -- set list of columns, or update this when data are available.</li>
 *      <li> a constructor or open() method to retrieve the data.  Once constructed and registered
 *           with the Session, the datasource is assumed to be on the first "row", with data available.
 *           Implementors are responsible for lazy or delayed initialization.
 *     </ul>
 */
public interface IDatasourceBasic {

    /** A unique name within the Application for this datasource, it becomes the ID by which
     *  Widgets can discover the datasource.
     */
    public String getID();

    /** By default, return a com.dynamide.db.DatasourceHelper(IDatasourceBasic), e.g.
     * <pre>
     * private IDatasource m_datasourceHelper = null;
     *
     * public IDatasource getDatasourceHelper(){
     *     if (m_datasourceHelper == null){
     *        m_datasourceHelper = new DatasourceHelper(this);
     *     }
     *     return m_datasourceHelper;
     * }
     * </pre>
     * or return a subclass of DatasourceHelper or a class that implements IDatasource by forwarding correct
     * methods to this class, e.g. your own policy.  Under most circumstances, DatasourceHelper should
     * fulfil all requirements. DatasourceHelper simply wraps your IDatasourceBasic and makes it able
     *  to respond to the calls in IDatasource.
     */
    public IDatasource getDatasourceHelper();

    /** Maintain a list of Field objects, and register them with the Session; return the live Field
     * when asked by this method.
     */
    public Field getField(String fieldName);

    /** @return a Map of all the fields owned. */
    public Map getFields();

    /** Implementors can return an Object from this method, but make sure that toString() makes sense.
     *  Dynamide won't automatically call toString() on the returned Object, but WebMacro will
     *  default to doing this if, for example, $datasource.get("foo") is in a Page or Widget template.
     *  A standard strategy would be to search for Property objects, then Fields, returning the Field.getValue()
     *  or Field.getStringValue().
     */
    public Object get(String what) throws Exception;

    /**
     * Return an Iterator which knows how to properly iterate over your implementation.
     *  You can support remove(), or throw an UnsupportedOperationException.
     *  Single-row datasource would return an iterator that returned one row, then set hasNext to false.
     *
     * All implementations should provide one row of data, even if this row contains only empty strings
     * for values.  For single row datasets, here is a simple implementation, which can
     * be done in an inner class:
     * <pre>
     *  public class MyClass implements IDatasourceBasic{
     *      public class MyDatasourceIterator implements Iterator {
     *          private MyClass m_target;
     *          private int m_iterCount = 0;
     *          public SessionDatasourceIterator(MyClass target){
     *              m_target = target;
     *          }
     *          public Object next(){
     *              m_iterCount++;
     *              return m_target;
     *          }
     *          public boolean hasNext(){
     *              return (m_iterCount < 1);
     *          }
     *          public void remove(){
     *              throw new UnsupportedOperationException();
     *          }
     *      }
     *  }
     * </pre>
     */
    public Iterator iterator();


}
