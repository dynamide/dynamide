package com.dynamide.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import com.dynamide.DynamideObject;
import com.dynamide.Field;
import com.dynamide.IPropertyProvider;
import com.dynamide.Property;
import com.dynamide.Session;
import com.dynamide.Widget;

import com.dynamide.datatypes.DatatypeException;

import com.dynamide.db.mappers.IColumnTypeMapper;

import com.dynamide.event.ChangeEvent;
import com.dynamide.event.IRowChangeListener;
import com.dynamide.event.ScriptEvent;

import com.dynamide.resource.ResourceManager;

import com.dynamide.util.FileTools;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class RDBDatasource extends SimpleDatasource implements IDatasource, IRowChangeListener {
    public RDBDatasource(){
        this(null);
    }

    public RDBDatasource(DynamideObject owner){
        super(null);
        //open table...
    }

    public RDBDatasource(DynamideObject owner, Session session, String id){
        super(owner);
        super.setID(id);
        setSession(session);
        registerWithSession();
    }

    public static class ColMeta {
        public ColMeta(String colname, String sqltype, boolean persistent, IColumnTypeMapper mapper){
            this.colname = colname;
            this.sqltype = sqltype;
            this.persistent = persistent;
            this.mapper = mapper;
        }
        public ColMeta(String colname, boolean persistent){
            this.colname = colname;
            this.sqltype = "";
            this.persistent = persistent;
            this.mapper = null;
        }
        public ColMeta(){
        }
        public String colname;
        public String sqltype = "";
        public boolean persistent = true;
        public IColumnTypeMapper mapper;
    }

    //keep these in sync with the string array below.
    public static final int STATE_LOADING = 0;
    public static final int STATE_DISPLAY = 1;
    public static final int STATE_UPDATE  = 2;
    public static final int STATE_INSERT  = 3;

    private int m_state = STATE_LOADING;

    //keep these in sync with the indices above.
    private String[] m_states = {"LOADING", "DISPLAY", "UPDATE", "INSERT"};

    public String getState(){
        return m_states[m_state];
    }

    private Map m_mappers = new TreeMap();

    private Widget m_widget = null;
    public Widget getWidget(){return m_widget;}
    public void setWidget(Widget new_value){m_widget = new_value;}

    public void logError(String msg, Exception e){
    	if (m_database!=null){
    		System.out.println("SQL_ERROR_STR:"+e.getMessage());
    		String location = m_widget != null ? m_widget.getDotName() : "RDBDatasource[widget-not-set]";
    		m_database.logDebugSQLError(location, "", e.toString());  //will end up in the handler log
    	}
    	super.logError(msg, e);
    }
    
    public void logWarn(String msg, Exception e){
    	if (m_database!=null){
    		System.out.println("SQL_ERROR_STR:"+e.getMessage());
    		String location = m_widget != null ? m_widget.getDotName() : "RDBDatasource[widget-not-set]";
    		m_database.logDebugSQLError(location, "", e.toString()); //will end up in the handler log    	
    	}
    	super.logError(msg, e);
    }
    
    public void registerWithSession(){
        Session session = getSession();
        if ( session != null ) {
            session.registerDatasource(this);
        } else {
            logError("getSession() returned null in RDBDatasource.registerWithSession(), couldn't register.");
        }
    }
    public void unregisterFromSession(){
        Session session = getSession();
        if ( session != null ) {
            session.unregisterDatasource(this);
        } else {
            logError("getSession() returned null in RDBDatasource.registerWithSession(), couldn't register.");
        }
    }

    public void registerColumnTypeMapper(String datatype, String classnameOfIColumnTypeMapper)
    throws Exception {
        IColumnTypeMapper mapper = (IColumnTypeMapper)Class.forName(classnameOfIColumnTypeMapper).newInstance();
        m_mappers.put(datatype, mapper);
    }

    private StringList m_oldRow = null;

    private IPropertyProvider m_propertyProvider = null;
    public IPropertyProvider getPropertyProvider(){return m_propertyProvider;}
    public void setPropertyProvider(IPropertyProvider new_value){
        m_propertyProvider = new_value;
        setLastError("");
    }

    public Property getProperty(String propertyName){
        Property result;
        if ( propertyName.equalsIgnoreCase("state") ) {
            try {
                return new Property(null, "state", getState());
            } catch (Exception e)  {
                logError("couldn't set 'state' property in RDBDatasource", e);
            }
        }
        if ( m_propertyProvider != null ) {
            result = m_propertyProvider.getProperty(propertyName);
            if ( result != null ) {
                return result;
            }
        }
        return super.getProperty(propertyName);
    }

    public void setProperty(String name, String value) throws DatatypeException {
        if ( m_propertyProvider != null ) {
            m_propertyProvider.setProperty(name, value);
        } else {
            super.setProperty(name, value);
        }
    }

    public String getPropertyStringValue(String what){
        String res = "";
        Property prop = getProperty(what);
        if ( prop != null ) {
            res = prop.getStringValue();
        }
        return res;
    }

    private RDBDatabase m_database = null;
    public RDBDatabase getDatabase(){return m_database;}
    public void setDatabase(RDBDatabase new_value){m_database = new_value;}

    public Field setFieldValues(String fieldName, Object value, String stringValue, String sqltype)
    throws DatatypeException {
        Field aField = getField(fieldName);
        aField.set("value", stringValue);
        aField.set("oldValue", stringValue);
        aField.set("object", value);
        aField.set("sqltype", sqltype);
        return aField;
    }

    public void insert(){
        m_state = STATE_INSERT;
        insertRow();
    }

    public void edit(){
        m_oldRow = getCurrentRow();
        m_state = STATE_UPDATE;
    }

    /** This method returns the state to DISPLAY, and doesn't unload the data; however,
     *  current edits will be lost.  Call post() to commit edits or inserts.
     */
    public void cancel(){
        if ( m_oldRow != null ) {
            //cancelling from UPDATE
            //System.out.println("cur row: "+dumpRow(getCurrentRow()));
            //System.out.println("old row: "+dumpRow(m_oldRow));
            super.setCurrentRow(m_oldRow);
            m_oldRow = null;
        } else {
            if (m_state == STATE_INSERT){
                //cancelling from INSERT
                super.deleteRow();
            } else {
                //cancelling, but not in UPDATE or INSERT
                // it is always safe to call cancel(), so don't delete anything.
            }
        }
        m_state = STATE_DISPLAY;
    }

    public boolean go(int distance){
        if (   (m_state==STATE_UPDATE) || (m_state==STATE_INSERT)  ) {
            String sError = "Can't go("+distance+") because datasource is in UPDATE or INSERT state.  Use post() or cancel() first.";
            logError(sError);
            setLastError("");
            return false;
        }
        return super.go(distance);
    }

    public void wireToMaster(){
        String master = getPropertyStringValue("masterDatasource");
        if ( ! Tools.isBlank(master)) {
        	logWarn("wireToMaster (INFO, really): "+getDotName()+" master: "+master);
            IDatasource masterDatasource = getSession().getDatasource(master);
            if ( masterDatasource instanceof RDBDatasource ) {
                ((RDBDatasource)masterDatasource).addRowChangeListener(this);
            }
        }
    }

    private boolean m_onRowChangedLocked = false;

    /** Fired from superclass, we hook this to fire widget events.
     *  Pass "this" as event.inputObject.
     */
    public void onRowChanged(){
        if (getWidget()!=null && (m_onRowChangedLocked == false)){
            //logDebug("RDBDatasource.onRowChanged firing");
            getWidget().fireEvent(this, "onRowChanged");
        }
        notifyRowChangeListeners("-1", "-1");
    }

    public void onRowChanged(ChangeEvent event){
        logWarn("RDBDatasource.onRowChanged "+getDotName()+" event: "+event);
        if ( event.sender == this ) {
            return;
        }
        // This uses notification so that you can have multiple detail listeners.
        String master = getPropertyStringValue("masterDatasource");
        if ( Tools.isBlank(master) ) {
            logWarn("master is blank");
        } else {
            IDatasource masterDatasource = getSession().getDatasource(master);
            if ( masterDatasource != null ) {
                String detailKey = getPropertyStringValue("detailKey");
                String detailTable = getPropertyStringValue("detailTable");
                String detailColumnClause = getPropertyStringValue("detailColumnClause");
                if ( Tools.isBlank(detailColumnClause) ) {
                    detailColumnClause = "*";
                }
                if ( ! Tools.isBlank(detailKey) && ! Tools.isBlank(detailTable)) {
                    String masterKey = getPropertyStringValue("masterKey");
                    if ( Tools.isBlank(masterKey) ) {
                        logWarn("masterKey is blank");
                    }
                    Object oid = ((RDBDatasource)event.sender).get(masterKey);
                    if ( oid != null ) {
                        String ID = oid.toString();
                        String sql = "SELECT "+detailColumnClause+" FROM "+detailTable+" WHERE "+detailKey+" = "+ID;
                        System.out.println("########################   UNIMPLEMENTED: detail sql: "+sql+" this: "+this);
                        try {
                            load(sql);
                            logWarn(getDotName()+" loaded "+getRowCount());
                        } catch (Exception e)  {
                            logError("RDBDatasource.onRowChanged caught error trying to reload based on sql: "+sql, e);
                        }
                    } else {
                        logWarn("masterKey "+masterKey+" retrieved null");
                    }
                } else {
                    logWarn("detailKey or detailTable is blank");
                }
            } else {
                logWarn("master is null");
            }
        }
    }

    private ArrayList m_rowChangeListeners = new ArrayList();

    public void addRowChangeListener(IRowChangeListener new_listener){
        logDebug("RDBDatasource.addRowChangeListener");
        m_rowChangeListeners.add(new_listener);
        ChangeEvent event = new ChangeEvent();
        event.oldValue = "";//%%
        event.newValue = "";//%%
        event.sender = this;
        new_listener.onRowChanged(event);
    }
    public void removeRowChangeListener(IRowChangeListener listener){
        m_rowChangeListeners.remove(listener);
    }
    protected void notifyRowChangeListeners(Object oldValue, Object newValue){
        ChangeEvent event = new ChangeEvent();
        event.oldValue = oldValue;
        event.newValue = newValue;
        event.sender = this;

        int iSize = m_rowChangeListeners.size();
        for (int i = 0; i < iSize; i++){
            ((IRowChangeListener)m_rowChangeListeners.get(i)).onRowChanged(event);
        }
    }




    /** Post the modified or inserted row to the database. The beforePost event is fired, then the post
     *  then the afterPost event.  To abort the post from the beforePost event,
     *  the ScriptEvent resultAction should be set to ScriptEvent.RA_THROW_EXCEPTION
     *  and the ScriptEvent.outputObject can be a java.lang.Exception, with an optional message.
     *  <pre>
     *  Example:
     *  &lt;event name="page1_datasource1_beforePost" language="beanshell">&lt;[!CDATA[
     *     if(...someBadCondition...){
     *         event.resultAction = RA_THROW_EXCEPTION;
     *         event.outputObject = new Exception("Bad Condition not allowed");
     *         return;
     *     }
     *  ]]>&lt;/event>
     */
    public boolean post(){
        int prevCurrent = getCurrentRowIndex();
        ScriptEvent beforePost = fireBeforePost();
        if ( beforePost != null && beforePost.resultAction == ScriptEvent.RA_THROW_EXCEPTION ) {
            String msg = (beforePost.outputObject != null)
                ? ((beforePost.outputObject instanceof Exception)
                    ? ((Exception)beforePost.outputObject).getMessage()
                    : beforePost.outputObject.toString())
                : "";
            msg = msg.length()>0 ? " Message: \""+msg+"\"" : "";
            Session session = getSession();
            if ( session != null ) {
                session.logHandlerProc(beforePost.eventName, "Event returned RA_THROW_EXCEPTION. Aborting post()."+msg);
            }
            return false;
        }
        try {
            String sError ;
            setLastError("");
            if ( m_database == null  ) {
                sError = "Could not post: RDBDatabase not set.";
                logError(sError);
                setLastError(sError);
                return false;
            }
            String sql = "";
            try {
                if (m_state == STATE_UPDATE){
                    sql = formatUpdateSQL();
                } else if (m_state == STATE_INSERT){
                    sql = formatInsertSQL();
                } else {
                    sError = "Could not post: datasource not in UPDATE or INSERT state";
                    logError(sError);
                    setLastError(sError);
                    return false;
                }
                if (sql.trim().length()==0){
                    return false;
                }
                if ( m_database.getDebugSQL() ) {
                    logDebug("RDBDatasource sql: "+sql);
                }

                //3/30/2004 9:21AM changed to new execute to use the real API results.
                Object count = m_database.execute(sql, getID());
                String sCount = ""+count;
                setProperty("lastCount", ""+sCount);  //%% could be a string with the value or the error.
                m_state = STATE_DISPLAY;
                try {
                    String reloadAfterPost = getPropertyStringValue("reloadAfterPost");
                    if ( Tools.isTrue(reloadAfterPost) ) {
                        reload();
                        if ( prevCurrent > -1 ) {
                            seek(prevCurrent);
                        }
                    }
                } catch( Exception e ){
                    setLastError("Error reloading after POST: "+e);
                    return false;
                }
                m_state = STATE_DISPLAY;
                setProperty("lastCount", sCount);
                return true;
            } catch (SQLException e){
                ResourceManager rm = ResourceManager.getRootResourceManager();
                String fn = "sql-error-"+ResourceManager.getNextErrorID()+".sql";
                FileTools.saveFile(rm.getErrorLogsDir(), fn, sql);
                logError("SQLException: "+e+", stored in "+fn+" sql:{"+sql+'}', e);
                setLastError("SQLException:"+e);
                return false;
            }
        } catch (Exception e2)  {
            logError("RDBDatabase.post error", e2);
            return false;
        } finally {
            fireAfterPost();
        }
    }

    private void setLastError(String err){
        try {
            setProperty("lastCount", "0");
            setProperty("lastError", err);
            if ( err.length()>0 ) {
                logError(err);
            }
        } catch (Exception e)  {
            logError("Couldn't set property, when setting lastError", e);
        }
    }

    public void reload() throws SQLException {
        load(formatSQL());
    }

    public void load() throws SQLException {
        load(formatSQL());
    }

    public void load(String sql) throws SQLException {
        if ( m_database == null  ) {
            String sError = "Could not load: RDBDatabase not set.";
            logError(sError);
            setLastError(sError);
            return;
        }
        ResultSet resultSet = null;
        try {
            resultSet = m_database.executeQuery(sql, getID());
            loadFromResultSetWReset(resultSet);
        }
        finally {
            try { if(resultSet!=null) resultSet.close(); } catch (Exception e){}
            resultSet = null;
        }
    }

    public void loadFromResultSetWReset(ResultSet resultSet)
    throws SQLException {
        clear();
        //clear calls clearColumns. // clearColumns();
        int rowcount = loadFromResultSet(resultSet);
        cancel(); //just leave current rows, but get out of insert state.
        go(IDatasource.BEGIN);
        try {setProperty("lastCount", ""+rowcount);} catch (Exception e)  {}
    }

    public int loadFromResultSet(ResultSet resultSet)
    throws SQLException {
        boolean DEBUG = false;
        boolean prev_onRowChangedLocked = m_onRowChangedLocked;
        m_onRowChangedLocked = true;
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int colcnt = rsmd.getColumnCount();
            ColMeta[] colmeta = new ColMeta[colcnt];
            try {
                rsmd.getColumnName(1);
                for (int i=0; i<colcnt; i++){
                    colmeta[i] = new ColMeta();
                    colmeta[i].colname = rsmd.getColumnName(i+1).toUpperCase();
                    String sqltype = rsmd.getColumnTypeName(i+1);
                    colmeta[i].sqltype = sqltype;
                    IColumnTypeMapper mapper = (IColumnTypeMapper)m_mappers.get(sqltype);
                    if ( mapper!=null ) {
                        colmeta[i].mapper = mapper;
                    }
                    addColumn(colmeta[i].colname, colmeta[i]);
                }
            } catch (Exception e){
                for (int i=0; i<colcnt; i++){
                    colmeta[i] = new ColMeta();
                    colmeta[i].colname = "COL" + i;
                }
            }
            Object o = null;
            int rowcount = 0;
            while (resultSet.next()){
                addRow();
                rowcount++;
                for (int i=0; i<colcnt; i++){
                    try {
                        o = resultSet.getObject(i+1);
                        boolean isNull = (o == null);
                        try {
                            if (resultSet.wasNull()) isNull = true;
                        } catch (Exception noWasNullEx){}
                        if (isNull){
                            o = new NullData();
                        } else {
                            if (DEBUG) System.out.println("=======o.class: "+o.getClass().getName()+" o.value: "+o);
                        }

                        IColumnTypeMapper mapper = colmeta[i].mapper;
                        String stringValue = (mapper!=null) ? mapper.onColumn(o) : ((o!=null)?o.toString():"");
                        setFieldValues(colmeta[i].colname, o, stringValue, colmeta[i].sqltype);

                    } catch (Exception e){
                        o = "ERROR:"  //11/27/2003 8:09PM _errPrefix
                        + e.getClass().getName() + ":"
                        + e.getMessage();
                        o = StringTools.escape(StringTools.escapeForWebmacro(o.toString()));
                        System.out.println(o.toString());
                    }
                }
            }
            return rowcount;
        } finally {
            m_onRowChangedLocked = prev_onRowChangedLocked;
        }
    }


    //when you load, you must save the metadata for each row, either in the current row,
    //and the SimpleDatasource must carry forward on insertRow()/createEmptyRow,
    //or you must have someone store one row of metadata and be able to set it on a newRow occurence/event.

    private Map getQuotableColumns()
    throws Exception {
        //looks for the dynamide-lib version, but allows per-application override
        Map props = null;
        try {
            String filename = "";
            String resourceName = "resources/sql/quoted-sqltypes.properties";
            Session session = getSession();
            if ( session != null ) {
                filename = session.getAppFilename(resourceName);
            } else {
                filename = com.dynamide.resource.ResourceManager.getRootResourceManager().getDynamideLibAssembly()
                              .getResourceFilename(resourceName);
            }
            props = FileTools.loadPropertiesFromFile("", filename);
        } catch (Exception e)  {
        }
        return props;
    }

    private String formatSQL(){
    //throws Exception {
        setLastError("");
        String macro = getPropertyStringValue("sql");
        if ( macro.trim().length()>0) {
            StringList variables = new StringList();
            variables.addObject("datasource", this);
            Session session = getSession();
            if ( session != null ) {
                return getSession().expandTemplate(variables, macro, getDotName());
            } else {
                logError("Could not formatSQL() because Session is null in "+toString());
                return "";
            }
        }
        String tableName = getPropertyStringValue("tableName");
        if ( tableName.length() == 0 ) {
            setLastError("tableName property not found in RDBDatasource");
            return "";
        }
        return "SELECT * from "+tableName+";";
    }

    private String formatInsertSQL()
    throws Exception {
        // INSERT INTO swarm_actions (id, level, topic, title, slots, filled, lastUpdate, updateUser)
        //  VALUES                   (12, NULL, 'events:s.f.20030427:BART', 'Coffee afterwards?', NULL, NULL, NULL, NULL)
        setLastError("");
        String sendEmptyColumns = getPropertyStringValue("sendEmptyColumns");
        boolean bSendEmptyColumns = Tools.isTrue(sendEmptyColumns);
        String insertMacro = getPropertyStringValue("insertSQL");
        if ( insertMacro.trim().length()>0) {
            StringList variables = new StringList();
            variables.addObject("datasource", this);
            variables.addObject("currentRow", getCurrentRow());
            Session session = getSession();
            if ( session != null ) {
                return getSession().expandTemplate(variables, insertMacro, getDotName());
            } else {
                logError("Could not formatInsertSQL() because Session is null in "+toString());
                return "";
            }
        }
        Property propTN = getProperty("tableName");
        String tableName = null;
        if (propTN!=null) tableName = propTN.getValue().toString();
        if ( tableName == null || tableName.length()==0 ) {
            setLastError("tableName property not found in RDBDatasource");
            return "";
        }
        Map quotableColumns = getQuotableColumns();

        StringList row = getCurrentRow();
        String sValues = "";
        String key;
        String val;
        String keyval;
        Field f;
        String columnsClause = "(";
        String sqltype;
        int colcount = row.size();
        if ( colcount == 0 ) {
            setLastError("No columns available. Can't INSERT");
            return "";
        }
        for(int i=0; i<colcount; i++){
            key = row.getString(i);
            f = (Field)row.getObjectAt(i);
            sqltype = "";
            Object o = getMetadataForColumn(key);
            if ( o != null && o instanceof ColMeta ) {
                ColMeta colmeta = (ColMeta)o;
                if ( colmeta.persistent = false ) {
                    continue;
                }
                sqltype = colmeta.sqltype.toUpperCase();
                //System.out.println("sqltype: "+sqltype+" for "+key);
            }
            val = f.toString();
            if ( val.length()>0 || bSendEmptyColumns ) {
                val = RDBDatabase.escapeForPostgres(val);  //%%%% dooop!  what about other SQL rules for other db's...use the mapper.
                columnsClause += key+',';
                if (quotableColumns != null && quotableColumns.get(sqltype)!=null) {
                    //System.out.println("quotableColumns val: '"+val+"'");
                    if ( val.length() == 0 ) {
                        keyval = "null";
                    } else {
                        keyval = "'"+val+"'";
                    }
                } else  {
                    if ( val.length() == 0 ) {
                        keyval = "null";
                    } else {
                        keyval = val;
                    }
                }
                sValues += keyval+',';
            }
        }
        if (sValues.endsWith(",")) sValues = sValues.substring(0, sValues.length()-1);
        if (columnsClause.endsWith(",")) columnsClause = columnsClause.substring(0, columnsClause.length()-1);
        columnsClause += ')';
        String sql = "INSERT INTO "+tableName+' '+columnsClause+" VALUES ("+sValues+");";
        return sql;
    }

    private String formatUpdateSQL()
    throws Exception {
            String sendEmptyColumns = getPropertyStringValue("sendEmptyColumns");
            boolean bSendEmptyColumns = Tools.isTrue(sendEmptyColumns);
            String updateMacro = getPropertyStringValue("updateSQL");
            if ( updateMacro.trim().length()>0) {
                StringList variables = new StringList();
                variables.addObject("datasource", this);
                variables.addObject("currentRow", getCurrentRow());
                Session session = getSession();
                if ( session != null ) {
                    return session.expandTemplate(variables, updateMacro, getDotName());
                } else {
                    logError("Could not formatUpdateSQL() because Session is null in "+toString());
                    return "";
                }
            }
            String tableName = getPropertyStringValue("tableName");
            if ( tableName.length() == 0 ) {
                setLastError("tableName property not found in RDBDatasource");
                return "";
            }
            //looks for the dynamide-lib version, but allows per-application override
            Map quotableColumns = getQuotableColumns();
            Vector v;
            String keyColumns = getPropertyStringValue("keyColumns");
            if ( keyColumns.length()>0 ) {
                keyColumns = keyColumns.toUpperCase();
                v = StringTools.parseSeparatedValues(keyColumns, ",");
            } else {
                v = new Vector();
            }
            //e.g.
            // UPDATE swarm_actions SET title= 'Finb', lastUpdate= 88 WHERE id=39
            StringList row = getCurrentRow();
            String sValues = "";
            String key;
            String val;
            String keyval;
            String whereClause = "";
            Field f;
            boolean whereClauseUpdated = false;
            int colcount = row.size();
            if ( colcount == 0 ) {
                setLastError("No columns available. Can't UPDATE");
                return "";
            }
            String sqltype;
            for(int i=0; i<colcount; i++){
                key = row.getString(i);
                sqltype = "";
                Object o = getMetadataForColumn(key);
                if ( o != null && o instanceof ColMeta ) {
                    ColMeta colmeta = (ColMeta)o;
                    if ( colmeta.persistent = false ) {
                        continue;
                    }
                    sqltype = colmeta.sqltype.toUpperCase();
                    System.out.println("sqltype: "+sqltype+" for "+key);
                }
                f = (Field)row.getObjectAt(i);
                System.out.println("sqltype: "+sqltype+" for "+key);
                val = f.toString();
                if ( val.length()>0 || bSendEmptyColumns ) {
                    val = StringTools.searchAndReplaceAll(val, "\\", "\\\\\\\\"); // postgres requires \\\\ for one slash, java requires doubling them
                    val = StringTools.searchAndReplaceAll(val, "'", "\\'");
                    if (quotableColumns != null && quotableColumns.get(sqltype)!=null) {
                        System.out.println("UPDATE quotableColumns val: '"+val+"'");
                        if ( val.length() == 0 ) {
                                keyval = key+"=null";
                        } else {
                            keyval = key+"='"+val+'\'';
                        }
                    } else  {
                        if ( val.length() == 0 ) {
                            keyval = key+"=null";
                        } else {
                            keyval = key+"="+val;
                        }
                    }
                    sValues += keyval+',';
                    if ( v.indexOf(key)>-1 ) {
                        if ( whereClauseUpdated ) {
                            whereClause += " AND ";
                            whereClauseUpdated = false;
                        }
                        whereClause = whereClause+keyval;
                        whereClauseUpdated = true;
                    }
                }

            }
            if ( whereClause.length()>0 ) {
                whereClause = " WHERE "+whereClause;
            }
            if (sValues.endsWith(",")) sValues = sValues.substring(0, sValues.length()-1);
            return "UPDATE "+tableName+" SET " + sValues + whereClause+';';
    }

    private ScriptEvent fireBeforePost(){
        if ( m_widget!=null ) {
            ScriptEvent evt = m_widget.fireEvent(this, "beforePost");
            return evt;
        } else {
            //System.out.println("fireBeforePost m_widget null");
            return null;
        }
    }

    private void fireAfterPost(){
        if ( m_widget!=null ) {
            m_widget.fireEvent(this, "afterPost");
        }
    }

}
