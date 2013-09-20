package com.dynamide.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.dynamide.DynamideObject;

import com.dynamide.util.Log;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class RDBTable extends DynamideObject {

    public RDBTable( RDBDatabase database, String tableName ) {
        super(null);
        m_database = database;
        m_tableName = tableName;
    }

    private RDBDatabase m_database;
    private String m_tableName;
    private ResultSet m_rs;
    private Connection m_con;
    private String m_lastSQL;

    private boolean m_debugSQL = false;
    public boolean getDebugSQL(){return m_debugSQL;}
    public void setDebugSQL(boolean new_value){m_debugSQL = new_value;}

    private void addError(String message, Throwable t){
    	errorLog.append(message);
    	errorLog.append("\r\n");
    	errorLog.append(Tools.errorToString(t, true));
    	errorLog.append("\r\nlast sql: ");
    	errorLog.append(m_lastSQL);
    	
        Log.error(RDBTable.class, "[table: "+m_tableName+"] "+message+"\r\nOpened with sql: "+m_lastSQL, t);
    }

    private StringBuffer errorLog = new StringBuffer();
    private void clearErrors(){
    	errorLog = new StringBuffer();
    }
    public String getErrors(){
    	return errorLog.toString();
    }
    
    private ResultSetMetaData m_rsmd;
    public ResultSetMetaData getResultSetMetaData(){
        return m_rsmd;
    }

    private boolean m_open = false;
    public boolean isOpen(){return m_open;}
    //public void setOpen(boolean new_value){m_open = new_value;}

    private StringList m_fields = new StringList();
    public StringList getFields(){return m_fields;}
    public void setFields(StringList new_value){m_fields = new_value;}

    private int m_columnCount = 0;
    public int getColumnCount(){return m_columnCount;}
    public void setColumnCount(int new_value){m_columnCount = new_value;}

    public Iterator getColumnNames(){
        Vector v = new Vector();
        try {
            int count = m_rsmd.getColumnCount();
            for (int i=0; i < count; i++) {
                v.add(m_rsmd.getColumnName(i+1));
            }
        } catch (SQLException e){
            logError("ERROR 1: ", e);
        }
        return v.listIterator();
    }


    public ResultSet getResultSet(){
        return m_rs;
    }

    /* public Object cell(int c)  {
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
            logError("ERROR 3: ", e);
            return "ERROR 3"+e;
        }
    } */

    public boolean open() {
    	clearErrors();
        return open(null, null);
    }

    //RowSet rows = new RowSet();
    /*
    look at these:
    C:\install\jdbc-rowsets\rowset1.0ea4\doc\index.html
    C:\install\jdbc-rowsets\rowset1.0ea4\examples
    http://developer.java.sun.com/developer/earlyAccess/crs/
    http://developer.java.sun.com/developer/Books/JDBCTutorial/chapter5.html
    */

    /** Caller is responsible for closing the connection by calling RDBTable.close after calling this method.
     */
    public boolean open( String criteria, String orderBy ) {
        try {
        	clearErrors();
        	if ( ! m_open ) {
                m_con = m_database.getConnection();
                if ( m_con == null ) {
                    logError("ERROR Connection is null.");
                    return false;
                }

                int pageSize = 4;
                int skipRows = 0;
                //test:
                //org.webmacro.datatable.ResultSetDataTable rsdt = new org.webmacro.datatable.ResultSetDataTable(m_con, sql, pageSize, skipRows);

                Statement st = m_con.createStatement();
                String sql = "SELECT * FROM "+m_tableName+
                    (criteria==null?"":(" WHERE "+criteria)) +
                    (orderBy == null?"":(" ORDER BY "+orderBy));

                m_lastSQL = sql;
                if (getDebugSQL())logDebug("attempting open SQL: >>"+sql+"<<");
                m_rs = st.executeQuery( sql );
                m_rsmd = m_rs.getMetaData();

                m_columnCount = m_rsmd.getColumnCount();
                for (int i=1; i <= m_columnCount; i++) {
                    m_fields.add(m_rsmd.getColumnName(i));
                }

                if ( m_rs != null ) {
                    m_rs.beforeFirst();
                }
            }
        } catch (SQLException e) {
            addError("SQLException: "+e, e);
            return false;
        }
        m_open = true;
        return true;
    }

    public boolean close(){
        try {
            if (m_con!=null) m_con.close();
            if (m_database != null) m_database.close();
        } catch (SQLException e){
            addError("SQLException: "+e, e);
            return false;
        }
        m_rsmd = null;
        m_con = null;
        m_open = false;
        return true;
    }

    public boolean edit(){
       return true;
    }

    public boolean cancel(){
       return true;
    }

    // %% todo: figure out the key columns, and manufacture a where clause on the current/new row.
    public String makeKeyConditions(){
        ///medatadata.getKeys... etc.
        return "";
    }

    public boolean post() {
        return post(null, ""); //   %% fix this.
    }

    public boolean post(Map values, String conditions) {
        if (conditions==null) {
            //return insert();
            return false;
        } else {
            return (update(values, conditions) > -1);
        }
    }

    public int insert(Map values){
        if ( m_database == null ) {
            addError("Could not insert: RDBDatabase not set.", null);
            return -1;
        }
        try {
            m_con = m_database.getConnection();
            try {

                // INSERT INTO swarm_actions (id, level, topic, title, slots, filled, lastUpdate, updateUser)
                //  VALUES                   (12, NULL, 'events:s.f.20030427:BART', 'Coffee afterwards?', NULL, NULL, NULL, NULL)

                String columnsClause = "(";
                int i =0;
                String key;
                for(Iterator keys=values.keySet().iterator();keys.hasNext();){
                    if (i>0){
                        columnsClause += ", ";
                    }
                    key = keys.next().toString();
                    columnsClause += key;
                    i++;
                }
                columnsClause += ')';

                String ss = "INSERT INTO "+m_tableName+' '+columnsClause+" VALUES (";
                i =0;
                String val;
                for(Iterator vals=values.values().iterator();vals.hasNext();){
                    if (i>0){
                        ss += ", ";
                    }
                    val = vals.next().toString();
                    val = StringTools.searchAndReplaceAll(val, "'", "\\'");
                    ss += "'"+val+"'";
                    i++;
                }
                ss += ")";
                m_lastSQL = ss;
                if (getDebugSQL())logDebug("attempting insert SQL: >>"+ss+"<<");
                Statement st =  m_con.createStatement();
                int code = st.executeUpdate( ss );
                if (m_database!=null) m_database.close();
                return code;
            } finally {
                if (m_con!=null) m_con.close();
                m_con = null;
            }
        } catch (SQLException e){
            addError("SQLException: "+e, e);
            return -1;
        }
    }

    public int update(Map values, String conditions){
        if ( m_database == null  ) {
            addError("Could not update: RDBDatabase not set.", null);
            return -1;
        }
        try {
            m_con = m_database.getConnection();
            try {
                //e.g.
                // UPDATE swarm_actions SET title= 'Finb', lastUpdate= 88 WHERE id=39

                String sValues = "";
                String key;
                String val;
                Map.Entry entry;
                int i =0;
                for(Iterator entries=values.entrySet().iterator();entries.hasNext();){
                    if (i>0){
                        sValues += ", ";
                    }

                    entry = (Map.Entry)entries.next();
                    key = (String)entry.getKey();
                    val = entry.getValue().toString();
                    val = StringTools.searchAndReplaceAll(val, "'", "\\'");
                    sValues += key+"='"+val+'\'';
                    i++;
                }

                String ss = "UPDATE "+m_tableName+" SET " + sValues + " WHERE " + conditions;
                m_lastSQL = ss;
                if (getDebugSQL())logDebug("attempting update SQL: >>"+ss+"<<");
                Statement st =  m_con.createStatement();
                int result = st.executeUpdate( ss );
                m_database.close();
                return result;
            } finally {
                m_con.close();
                m_con = null;
            }
        } catch (SQLException e){
            addError("SQLException: "+e, e);
            return -1;
        }
    }

    public int updateOrInsert(Map values, String conditions){
        int code = update(values, conditions);
        if (code <= 0){
            code = insert(values);
        }
        return code;
    }


    public static RDBTable getTestTable(String user, String password){
        RDBDatabase d = RDBDatabase.getTestDatabase(user, password);
        RDBTable t = new RDBTable(d, "TRAVERSAL");
        boolean ok = t.open(null, "pos");
        if ( ok ) {
            return t;
        }
        return null;
    }

    public static void main(String[] Args) throws Exception {
        RDBTable t = getTestTable(Args[0], Args[1]);
        try {
            if ( t == null ) {
                System.out.println("Couldn't get db connection"); //in main()
                System.exit(1);
            }
            int count = 0;
            System.out.println("Positioning before start of result set");//in main()
            System.out.println("Traversing forward.");//in main()
            boolean forward_ok = true;
            ResultSet rs = t.getResultSet();
            while (rs.next()) {
                int pos = rs.getInt("POS");
                System.out.println("POS: "+pos);
                count++;
            }
        }catch (SQLException E) {
            System.out.println("ERROR: [56] "+E);//in main()
            System.out.println("Usage: java com.dynamide.db.RDBTable user password");
        }
    }

}
