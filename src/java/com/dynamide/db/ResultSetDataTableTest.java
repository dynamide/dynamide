package com.dynamide.db;

import java.sql.Connection;

import org.webmacro.datatable.ResultSetDataTable;

public class ResultSetDataTableTest {
    final static public boolean DEBUG = true;
    private Connection _conn = null;

    //final static public String jdbcUrl = "jdbc:protomatter:pool:hyperPool";

    public ResultSetDataTable test(RDBDatabase db) {
        try {
            String sql = "SELECT * FROM swarm_actions";
            int pageSize = 50;
            int skipRows = 0;
            String cmd = "";
            if ("next".equals(cmd)){
                skipRows += pageSize;
            } else if ("prev".equals(cmd)){
                skipRows -= pageSize;
            } else if ("top".equals(cmd)){
                skipRows = 0;
            }
            ResultSetDataTable rsdt = null;
            try {
                Connection conn = db.getConnection();
                rsdt = new ResultSetDataTable(conn, sql, pageSize, skipRows);
                //rsdt.registerRowCallback(new DemoRowCallback(conn));
                rsdt.load();
            } finally {
                db.close();
            }
            return rsdt;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args){
        try {
            RDBDatabase db
                 = new RDBDatabase(RDBDatabase.CONX_MY_SQL+"/net_roots",
                                    RDBDatabase.DRIVER_MY_SQL,
                                    args[0],
                                    args[1],
                                    null);
            ResultSetDataTableTest demo = new ResultSetDataTableTest();
            ResultSetDataTable rsdt = demo.test(db);
            System.out.println("rsdt: "+rsdt);
        } catch (Exception e){
            System.out.println("ERROR: "+e);
            System.out.println("Usage: java com.dynamide.db.RDBDatabase user password");
        }
    }
}
