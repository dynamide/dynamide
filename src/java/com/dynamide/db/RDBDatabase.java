/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide.db;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import com.bitmechanic.sql.ConnectionPool;
import com.bitmechanic.sql.ConnectionPoolManager;
import com.dynamide.IPropertyProvider;
import com.dynamide.Property;
import com.dynamide.Session;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.resource.ResourceManager;
import com.dynamide.util.Log;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

//jdbcpool:







public class RDBDatabase extends Database {

    public RDBDatabase(String uri, String driverName, String username, String password, com.dynamide.Session session) {
        m_session = session;
        m_uri = uri;
        m_driverName = driverName;
        m_username = username;
        m_password = password;
        try {
            Class.forName(m_driverName).newInstance();
        } catch (Exception e){
            System.out.println("ERROR: [55] driver class "+m_driverName+" not found for RDBDatabase.");
        }
    }

    public String toString(){
        return "RDBDatabase:uri:["+m_uri+"]context:["+m_contextName+"]";
    }

    private Session m_session = null;
    public Session getSession(){return m_session;}
    public void setSession(Session new_value){m_session = new_value;}

    private String m_username = "";

    private String m_password = "";

    /** To use for the Constructor's "uri" parameter, add "/databasename" to this, e.g. CONX_MY_SQL+"/mydatabase" .
     */
    public final static String CONX_MY_SQL = "jdbc:mysql://";

    /** For connection to localhost listening with "postmaster -i" so that it uses internet sockets.
     *  No host and port.  Just specify
     * <pre>
     *              new RDBDatabase(RDBDatabase.CONX_POSTGRESQL+"net_roots",
                                    RDBDatabase.DRIVER_MY_SQL_CONNECTORJ,
     *                              "myuser",
     *                              "mypassword");
     * </pre>
     * Note that postgresql does NOT use the leading slash before the database if no host:port is specified:
     * CORRECT:  RDBDatabase.CONX_POSTGRESQL+"net_roots"  INCORRECT:   RDBDatabase.CONX_POSTGRESQL+"/net_roots"
     */
    public final static String CONX_POSTGRESQL = "jdbc:postgresql:";

    public final static String DRIVER_MY_SQL = "org.gjt.mm.mysql.Driver";
    public final static String DRIVER_MY_SQL_CONNECTORJ = "com.mysql.jdbc.Driver";
    public final static String DRIVER_POSTGRESQL = "org.postgresql.Driver";

    private String m_driverName = "";
    public String getDriverName(){return m_driverName;}

    private String  m_uri = "";
    public String  getURI(){return m_uri;}

    private String m_contextName = "";
    public String getContextName(){return m_contextName;}
    public void setContextName(String new_value){m_contextName = new_value;}

    //private Connection Conn = null;
    private ResultSet RS = null;
    private Statement Stmt = null;

    public void logDebugSQL(String location, String sql){
        if (m_useLog4j) Log.debug(RDBDatabase.class, location+" >>"+sql+"<<\r\n");
        if (m_session != null){
            m_session.logHandlerProcCollapsed("SQL", "db:"+m_contextName+";caller:"+location+" {<code>"+sql+"</code>}");
        }
    }

    public void logDebugSQLError(String location, String sql, String error){
        if (m_useLog4j) Log.debug(RDBDatabase.class, location+" >>"+sql+"<<\r\n");
        if (m_session != null){
            m_session.logHandlerProcCollapsed("SQL-ERROR", "db:"+m_contextName+";caller:"+location+" <b>"+error+"</b> {<code>"+sql+"</code>}");
        }
    }

    public boolean getDebugSQL(){
        Property prop= getProperty("debugSQL");
        if ( prop!=null ) {
            return Tools.isTrue(prop.getStringValue());
        }
        return false;
    }
    public void setDebugSQL(boolean new_value){
        try {
            setProperty("debugSQL", ""+new_value);
        } catch (Exception e)  {
            if (m_useLog4j) Log.error(RDBDatabase.class, "couldn't set property in RDBDatabase: "+new_value+e);
            else System.out.println("couldn't set property in RDBDatabase: "+new_value+e);
        }
    }

    private boolean m_useLog4j = true;
    public boolean getUseLog4j(){return m_useLog4j;}
    public void setUseLog4j(boolean new_value){m_useLog4j = new_value;}

    protected StringList m_propertiesTable = new StringList();

    private IPropertyProvider m_propertyProvider = null;
    public IPropertyProvider getPropertyProvider(){return m_propertyProvider;}
    public void setPropertyProvider(IPropertyProvider new_value){m_propertyProvider = new_value;}

    public Property getProperty(String propertyName){
        Property result;
        if ( m_propertyProvider != null ) {
            result = m_propertyProvider.getProperty(propertyName);
            if ( result != null ) {
                return result;
            }
        }
        return  (Property)m_propertiesTable.get(propertyName);
    }

    public void setProperty(String name, String value) throws DatatypeException {
        if ( m_propertyProvider != null ) {
            m_propertyProvider.setProperty(name, value);
        } else {
            m_propertiesTable.remove(name);
            m_propertiesTable.addObject(name, new Property(null, name, value));
        }
    }

    public static String escapeForPostgres(String val){
        return StringTools.escapeForPostgres(val);
    }

    private com.bitmechanic.sql.ConnectionPoolManager poolMgr = null;

    public Connection getConnection() throws SQLException {
        ResourceManager root = ResourceManager.getRootResourceManager();
        if ( poolMgr == null ) {
            poolMgr = ResourceManager.getConnectionPoolManager();
        }
        String alias = getURI()+':'+m_username;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(ConnectionPoolManager.URL_PREFIX + alias, null, null);
            return connection;
        } catch (Exception e)  {
            try {
                int maxConnections = Tools.stringToIntSafe(root.find("/conf/DBConnectionPool/maxConnections"), 32);
                int idleTimeout = Tools.stringToIntSafe(root.find("/conf/DBConnectionPool/idleTimeout"),5*60);
                int checkoutTimeout = Tools.stringToIntSafe(root.find("/conf/DBConnectionPool/checkoutTimeout"), 60);
                //Log.debug(RDBDatabase.class, "RDBDatabase.getConnection:new :: "+alias);
                poolMgr.addAlias(alias,
                                m_driverName,
                                getURI(),
                                m_username,
                                m_password,
                                maxConnections, //maxConn,
                                idleTimeout,    // seconds before idle/reuseable connections reaped
                                checkoutTimeout // seconds before long queries timeout.
                );
                ConnectionPool pool = poolMgr.getPool(alias);
                pool.setCacheStatements(true);
                //pool.setPrefetchSize(4);  //go ahead and get 4 rows.
                //Log.debug(RDBDatabase.class, "RDBDatabase.getConnection :: "+alias);
                connection = DriverManager.getConnection(ConnectionPoolManager.URL_PREFIX + alias, null, null);
                return connection;
            } catch (Exception e2)  {
                if (m_useLog4j) Log.error(RDBDatabase.class, "com.bitmechanic.sql.ConnectionPoolManager would not add alias", e2);
                else System.out.println("com.bitmechanic.sql.ConnectionPoolManager would not add alias "+e2);
                throw new SQLException("RDBDatabase.getConnection() raised exception: "+e2.getClass().getName()+" message: "+e2.getMessage());
                //3/12/2004 return null;
            }
        }
    }

    public void close(){
        //Log.debug(RDBDatabase.class, "RDBDatabase.close :: "+getURI()+" user: "+getUsername());
        if (RS != null) {
        try {
            RS.close();
        }
            catch (SQLException SQLE) {}
        }
        if (Stmt != null) {
        try {
            Stmt.close();
        }
            catch (SQLException SQLE) {}
        }
    }

    public org.webmacro.datatable.DataTable readTable(String sql, String debugLocation)
    throws Exception {
        return readTable(sql, org.webmacro.datatable.ResultSetDataTable.DEFAULT_MAX_ROWS, 0, debugLocation);
    }

    public org.webmacro.datatable.DataTable readTable(String sql, int pageSize, int skipRows, String debugLocation)
    throws Exception {
        org.webmacro.datatable.ResultSetDataTable rsdt = null;
        if (getDebugSQL()) {
            logDebugSQL("readTable:"+debugLocation, sql);
        }
        Connection conn = getConnection();
        try {
            rsdt = new org.webmacro.datatable.ResultSetDataTable(conn, sql, pageSize, skipRows);
            rsdt.load();
        } finally {
            conn.close();//load sets internal ref to conn to null, but doesn't close connection.
            close();
        }
        return rsdt;
    }

    public TableTree readTableTree(String sql,
                                   String detailIDColumnName,
                                   String detailParentIDColumnName,
                                   boolean zeroIsNull,
                                   String debugLocation)
    throws Exception{
        return readTableTree(sql, detailIDColumnName, detailParentIDColumnName, zeroIsNull,
                             org.webmacro.datatable.ResultSetDataTable.DEFAULT_MAX_ROWS, 0, debugLocation);
    }

    public TableTree readTableTree(String sql,
                                   String detailIDColumnName,
                                   String detailParentIDColumnName,
                                   boolean zeroIsNull,
                                   int pageSize,
                                   int skipRows,
                                   String debugLocation)
    throws Exception{
        org.webmacro.datatable.DataTable rsdt =
            readTable(sql, pageSize, skipRows, debugLocation);
        TableTree tt = new TableTree(rsdt, detailIDColumnName, detailParentIDColumnName, zeroIsNull);
        return tt;
    }

    public RDBTable openRDBTable(String tableName) {
        return new RDBTable(this, tableName);
    }

    /** @return -2 on error, -1 on a ResultSet returned or no count returned,
     * otherwise 0-n for the result of Statement.getUpdateCount().
     */
    public SQLExecuteResult execute(String sql, String debugLocation)
    throws SQLException {
        return execute(sql, m_useLog4j, "", debugLocation);
    }

    public SQLExecuteResult execute(String sql, String resultColumn, String debugLocation)
    throws SQLException {
        return execute(sql, m_useLog4j, resultColumn, debugLocation);
    }

    public SQLExecuteResult execute(String sql, boolean use_log4j, String debugLocation)
    throws SQLException {
        return execute(sql, use_log4j, "", debugLocation);
    }

    /* From the API:
     *      public boolean execute(String sql)
            throws SQLException

            Executes the given SQL statement, which may return multiple results. In some (uncommon) situations, a
            single SQL statement may return multiple result sets and/or update counts. Normally you can ignore this
            unless you are (1) executing a stored procedure that you know may return multiple results or (2) you are
            dynamically executing an unknown SQL string. The execute method executes an SQL statement and indicates the
            form of the first result. You must then use the methods getResultSet or getUpdateCount to retrieve the
            result, and getMoreResults to move to any subsequent result(s).

            Parameters:
                sql - any SQL statement
            Returns:
                true if the first result is a ResultSet object;
                false if it is an update count or there are no results
            Throws:
                SQLException - if a database access error occurs
            See Also:
                getResultSet(), getUpdateCount(), getMoreResults()

      */

    public class SQLExecuteResult {
        public static final int OK = 0;
        public static final int COUNT = 1;
        public static final int ERROR = 2;
        public String message = "";
        public int code = OK;
        public int count = -1;
        public Object object;
        public String toString(){
            if (code==OK){
                if (object != null){
                    return object.toString();
                }
                return "object was null";
            } else if (code==COUNT){
                return ""+count;
            } else {
                return message;
            }
        }
    }

    /** @return Integer if count, otherwise String if value or error. */
    public SQLExecuteResult execute(String sql, boolean use_log4j, String resultColumn, String debugLocation)
    throws SQLException {
        SQLExecuteResult result = new SQLExecuteResult();
        Connection con = getConnection();
        if ( con == null ) {
            if(use_log4j){
                Log.error(RDBDatabase.class, "ERROR Connection is null.");
            } else {
                System.err.println("ERROR: RDBDatabase.execute(\""+StringTools.truncate(sql, 200, "...")+"\") has null Connection");
            }
            if (m_session!= null){
                m_session.logHandlerProc("ERROR",
                                         "RDBDatabase cannot execute, connection is null. location: "
                                             +debugLocation+" sql: \r\n"+sql);
            }
            result.code = SQLExecuteResult.ERROR;
            result.message = "ERROR: [RDBDatabase.execute] Connection is null";
            if (getDebugSQL()) {
                logDebugSQL("execute.result:"+debugLocation, result.message);
            }
            return result;
        }
        try {
            Statement st = con.createStatement();
            if (getDebugSQL()) {
                logDebugSQL(debugLocation, sql);
            }
            boolean firstResultIsResultSet = st.execute(sql);
            if (firstResultIsResultSet){
                ResultSet rs = st.getResultSet();
                if (rs == null){
                    result.code = SQLExecuteResult.ERROR;
                    result.message = "ERROR: [RDBDatabase.execute] ResultSet was null: "+StringTools.truncate(sql, 200, "...");
                    if (getDebugSQL()) {
                        logDebugSQL("execute.result:"+debugLocation, result.message);
                    }
                    return result;
                } else {
                    rs.next();
                    if (resultColumn.length()>0){
                        result.object = rs.getObject(resultColumn);
                    } else {
                        result.object = rs.getObject(1);
                    }
                }
            } else {
                result.count = st.getUpdateCount();
                result.code = SQLExecuteResult.COUNT;
            }
            if (getDebugSQL()) {
                logDebugSQL("execute.result:"+debugLocation, result.toString());
            }
            return result;
        } finally {
            con.close();
        }
    }

    public ResultSet executeQuery(String sql, String debugLocation)
    throws SQLException {
        Connection con = getConnection();
        if ( con == null ) {
            if (m_useLog4j) Log.error(RDBDatabase.class, "ERROR Connection is null.");
            else System.out.println("ERROR Connection is null.");
            if (m_session!=null){
                m_session.logHandlerProc("ERROR", "RDBDatabase.executeQuery connection is null. location: "+debugLocation+" sql: "+sql);
            }
            return null;
        }
        try {
            Statement st = con.createStatement();
            if (getDebugSQL()){
                logDebugSQL(debugLocation, sql);
            }
            ResultSet rs  = st.executeQuery( sql );
            return rs;
        } finally {
            if (con!=null) con.close();
            con = null;
        }
    }

    /** Executes the query and returns the named column value of the first row.
     */
    public Object executeQueryToObject(String sql, String column, String debugLocation)
    throws SQLException {
        Connection con = getConnection();
        if ( con == null ) {
            if (m_useLog4j) Log.error(RDBDatabase.class, "ERROR Connection is null.");
            else System.out.println("ERROR Connection is null.");
            if (m_session!=null){
                m_session.logHandlerProc("ERROR", "RDBDatabase.executeQueryToObject connection is null. location: "+debugLocation+" sql: "+sql);
            }
            return null;
        }
        try {
            Statement st = con.createStatement();
            if (getDebugSQL()){
                logDebugSQL("executeQueryToObject:"+debugLocation, sql);
            }
            ResultSet rs  = st.executeQuery( sql );  //api doco sez: result is never null.
            rs.next();
            Object result;
            if (column.length()>0){
                result = rs.getObject(column);
            } else {
                result = rs.getObject(1);
            }
            /*if (result != null){
                String dmsg = "executeQueryToObject: "+result.getClass().getName();
                if (m_useLog4j) Log.debug(RDBDatabase.class, dmsg);
                else System.out.println(dmsg);
                return result;
                //com.dynamide.util.StringTools.decodeBytea(result);
            }
            return "";
            */
            return result;
        } finally {
            if (con!=null) con.close();
            con = null;
        }
    }

    /** Uses com.dynamide.util.StringTools.decodeBytea(Object) to decode the
     *  result.  This is a Postgres type.
     */
    public String executeQueryToString(String sql, String column, String debugLocation)
    throws SQLException {
        Object result = executeQueryToObject(sql, column, debugLocation);
        if (result != null){
            if (getDebugSQL()) {
                logDebugSQL("executeQueryToString(string,string):"+debugLocation, result.getClass().getName());
            } else {
                System.out.println("executeQueryToString:"+result.getClass().getName());
            }
            return com.dynamide.util.StringTools.decodeBytea(result);
        }
        return "";
    }


    public int executeUpdate(String sql, String debugLocation)
    throws SQLException {
        Connection con = getConnection();
        if ( con == null ) {
            if (m_useLog4j) Log.error(RDBDatabase.class, "ERROR Connection is null.");
            else System.out.println("ERROR Connection is null.");
            if (m_session != null){
                m_session.logHandlerProc("ERROR", "RDBDatabase.executeUpdate cannot execute: connection is null.  sql was: \""+sql+"\"");
            }
            if (m_session!=null){
                m_session.logHandlerProc("ERROR", "RDBDatabase.executeUpdate connection is null. location: "+debugLocation+" sql: "+sql);
            }
            return 0;
        }
        try {
            logDebugSQL(debugLocation, sql);
            Statement st = con.createStatement();
            return st.executeUpdate(sql);
        } finally {
            if (con!=null) con.close();
            con = null;
        }
    }

    public void load(String sql, String debugLocation) throws SQLException {
        boolean DEBUG = true;
        ResultSet _rs = null;
        try {
            int _skipRows = 0;
            _rs = executeQuery(sql, debugLocation);
            ResultSetMetaData rsmd = _rs.getMetaData();
            int colcnt = rsmd.getColumnCount();
            String[] cols = new String[colcnt];
            String[] _types = new String[colcnt];
            boolean hasColMeta = true;
            try {
                rsmd.getColumnName(1);
            } catch (Exception e){
                hasColMeta = false;
                // if getColumnName doesn't work, use this kludge
                for (int i=1; i<=colcnt; i++){
                    cols[i-1] = "COL" + i;
                }
            }
            if (hasColMeta){
                for (int i=1; i<=colcnt; i++){
                    cols[i-1] = rsmd.getColumnName(i).toUpperCase();
                    _types[i-1] = rsmd.getColumnTypeName(i);
                    if (DEBUG) System.out.println("_types[i-1]: "+_types[i-1]);
                }
            }
          //  setColumnNames(cols);
            // skip skipRows rows of result set
            for (int i=0; i<_skipRows && _rs.next(); i++);
            // copy the data from the resultset into this table
            Object o = null;
            java.io.BufferedReader clobReader = null;
            while (/*size() < _maxRows &&*/ _rs.next()){
                Object[] vals = new Object[colcnt];
                for (int i=1; i<=colcnt; i++){
                    try {
                        o = _rs.getObject(i);
                        boolean isNull = (o == null);
                        try {
                            if (_rs.wasNull()) isNull = true;
                        } catch (Exception noWasNullEx){}
                        if (isNull){
                            if (DEBUG) System.out.println("=================== NULL ================");
                            o = "null"; //11/27/2003 8:09PM _nullDisplay;
                        } else if (_types[i-1].equalsIgnoreCase("ARRAY")){
                            if (DEBUG) System.out.println("=================== ARRAY ================");
                            // TODO: handle SQL3 array types
                            java.sql.Array arr = (java.sql.Array)o;
                            o = arr.getArray();
                        } else if (_types[i-1].equalsIgnoreCase("CLOB")){// && (o instanceof Clob)){
                            Clob clob = _rs.getClob(i-1);//(Clob)o;
                            if (DEBUG) System.out.println("=================== CLOB ================");

                            clobReader = new java.io.BufferedReader(clob.getCharacterStream());
                            String s;
                            StringBuffer sb = new StringBuffer();
                            while ((s = clobReader.readLine()) != null){
                                sb.append(s);
                            }
                            o = sb.toString();
                        } else if (_types[i-1].equalsIgnoreCase("BLOB")){// && (o instanceof Blob)){
                            if (DEBUG) System.out.println("=================== BLOB ================");
                            //laramie. Blob blob = (Blob)o;
                            Blob blob = _rs.getBlob(i);
                            byte[] blobBytes = blob.getBytes(1l, (int)blob.length());
                            o = new String(blobBytes);
                            if (DEBUG) System.out.println("blobBytes.length:  "+blobBytes.length+" blob: "+o);
                        } else {
                            if (DEBUG) System.out.println("=======o.class: "+o.getClass().getName()+" o.value: "+o);
                        }
                    } catch (Exception e){
                        o = "ERROR"  //11/27/2003 8:09PM _errPrefix
                        + "<!-- " + e.getClass().getName() + ": "
                        + e.getMessage() + " -->";
                        if (DEBUG) System.out.println(o.toString());
                    } finally {
                        vals[i-1] = o;
                        if (clobReader != null){
                            try { clobReader.close(); }
                            catch (Exception e){}
                        }
                    }
                }
               System.out.println("would add row here: "+vals);  //11/27/2003 8:10PM this.addRow(vals);
            }
        }
        catch (SQLException sqlE){
            throw sqlE;
        }
        finally {
            try { if(_rs!=null) _rs.close(); } catch (Exception e){}
            _rs = null;
        }
    }


    public static void main(String[] args) throws Exception {
        RDBDatabase d = null;
        try {
            getTestDatabase(args[0], args[1]);
        } catch (Exception e){
            System.out.println("Usage: java com.dynamide.db.RDBDatabase user password");
        }
        d.test();
    }

    public static RDBDatabase getTestDatabase(String user, String password){
        RDBDatabase d = new RDBDatabase("jdbc:mysql:///test", "org.gjt.mm.mysql.Driver", user, password, null);
        return d;
    }

    public void test() throws Exception {
        Connection Conn = null;
        try {
            Conn = DriverManager.getConnection(m_uri);
            Stmt = Conn.createStatement();
            System.out.print("Create test data: ");
            boolean create_ok = createTestData();
            System.out.println(create_ok ? "passed" : "failed");
            System.out.println("Selecting Rows");
            RS = Stmt.executeQuery("SELECT * FROM TRAVERSAL ORDER BY pos");
            int count = 0;
            System.out.println("Positioning before start of result set");
            RS.beforeFirst();
            System.out.print("Traversing forward: ");
            boolean forward_ok = true;
            while (RS.next()) {
            int pos = RS.getInt("POS");
            // test case-sensitive column names
            pos = RS.getInt("pos");
            pos = RS.getInt("Pos");
            pos = RS.getInt("POs");
            pos = RS.getInt("PoS");
            pos = RS.getInt("pOS");
            pos = RS.getInt("pOs");
            pos = RS.getInt("poS");
            if (pos == count) {
                //System.out.print("+");
            }
            else {
                //System.out.print("-");
                forward_ok = false;
            }
            count++;
            }
            //System.out.println();
            if (forward_ok) {
            System.out.println("OK");
            }
            else {
            System.out.println("FAILED! Only traversed " + count + "/100 rows");
            }

            boolean is_last = RS.isLast();
            System.out.println("Checking ResultSet.isLast(): " + (is_last ? "OK" : "FAILED!"));
            System.out.print("Positioning after end of result set: ");
            try {
                RS.afterLast();
            System.out.println("OK");
            }
            catch (SQLException E) {
            System.out.println("FAILED! (" + E.toString() + ")");
            }

            System.out.print("Scrolling backwards: ");
            count = 99;
            boolean reverse_ok = true;
            while (RS.previous()) {
                int pos = RS.getInt("pos");
                if (pos == count) {
                    //System.out.print("+");
                }
                else {
                    //System.out.print("-");
                    reverse_ok = false;
                }
                count--;
            }

            if (reverse_ok) {
                System.out.println("OK");
            } else {
                System.out.println("FAILED!");
            }
            boolean is_first = RS.isFirst();
            System.out.println("Checking ResultSet.isFirst(): " + (is_first ? "OK" : "FAILED!"));

            System.out.println("Absolute positioning");
            RS.absolute(50);
            int pos = RS.getInt("pos");
            System.out.println(pos);
            boolean on_result_set = RS.absolute(200);
            System.out.println(on_result_set);
            System.out.println(RS.isAfterLast());
            RS.absolute(100);
            System.out.println(RS.getInt("pos"));
            System.out.println(RS.isLast());
            RS.absolute(-99);
            System.out.println(RS.getInt("pos"));
        }
        catch (SQLException E) {
            throw E;
        }
        finally {
            if (RS != null) {
            try {
                RS.close();
            }
                catch (SQLException SQLE) {}
            }
            if (Stmt != null) {
            try {
                Stmt.close();
            }
                catch (SQLException SQLE) {}
            }

            if (Conn != null) {
            try {
                Conn.close();
            }
            catch (SQLException SQLE) {}
            }
        }
    }

    private boolean createTestData() throws java.sql.SQLException {
        try {
            // Catch the error, the table might exist
            try {
                Stmt.executeUpdate("DROP TABLE TRAVERSAL");
            } catch (SQLException SQLE) {}
                Stmt.executeUpdate("CREATE TABLE TRAVERSAL (pos int PRIMARY KEY, stringdata char(32))");
                for (int i = 0; i < 100; i++) {
                Stmt.executeUpdate("INSERT INTO TRAVERSAL VALUES (" + i + ", 'StringData "+i+"')");
            }
        } catch (SQLException E) {
            E.printStackTrace();
            return false;
        }
        return true;
    }
}
