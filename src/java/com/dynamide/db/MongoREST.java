package com.dynamide.db;

import com.mongodb.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.Arrays;

public class MongoREST {

    private MongoClient mongoClient;
    private DB db;

    //=========== Java-facing API =====================================================================================

    public void setup(String databaseName, String userName, String password) throws UnknownHostException {
        MongoCredential credential = MongoCredential.createMongoCRCredential(userName, databaseName, password.toCharArray());
        mongoClient = new MongoClient(new ServerAddress(), Arrays.asList(credential));
        getDB(databaseName);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    protected DB getDB(String databaseName) {
        if (db==null) {
            db = mongoClient.getDB(databaseName);
        }
        return db;
    }

    public ObjectId postGetId(String databaseName, String collection, String json){
        DBCollection col = getDB(databaseName).getCollection(collection);
        DBObject doc = (DBObject) JSON.parse(json);
        WriteResult result = col.insert(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        return id;
    }



    /**  In this version, we deal with ObjectId by using native java ObjectId objects.  This is used in the query to find
         the object to update, and also that id is set on the updated object.  Normally, your object should have it already.
    */
    public ObjectId putGetId(String databaseName, String collection, ObjectId id, String json, boolean upsert, boolean multi){
        DBCollection col = getDB(databaseName).getCollection(collection);

        DBObject query = (DBObject) JSON.parse("{}");
        query.put("_id", id);

        DBObject dbobj = (DBObject) JSON.parse(json);
        dbobj.put("_id", id);

        WriteResult result = col.update(query, dbobj, upsert, multi);
        ObjectId resObjectID = (ObjectId)dbobj.get("_id");
        //System.out.println("resObjectID: "+resObjectID);
        if (result.getUpsertedId()!=null) {
            String resID = result.getUpsertedId().toString();
            System.out.println("upserted resID: "+resID);
        }
        return resObjectID;
    }

    public GridFS getGridFS(){
        return new GridFS(db);
    }


    //=========== REST-facing API ====================================================================================

    public String get(String databaseName, String collection, String queryJson)  {
        DBCollection col = getDB(databaseName).getCollection(collection);
        DBObject query = (DBObject) JSON.parse(queryJson);
        DBObject result = col.findOne(query);
        if (result==null){
            return "";
        }
        String out = com.mongodb.util.JSON.serialize(result);
        //System.out.println("get ("+queryJson+") found::" + out);
        return out;
    }


    public String getAll(String databaseName, String collection, String queryJson) {
        return getAll(databaseName, collection, queryJson, 0, 0);
    }

    public String getAll(String databaseName, String collection, String queryJson, int skip, int limit)  {
        DBCollection col = getDB(databaseName).getCollection(collection);
        DBObject query = (DBObject) JSON.parse(queryJson);
        DBCursor cursor = col.find(query).skip(skip).limit(limit);
        //System.out.println("find cursor::" + cursor);
        String out = com.mongodb.util.JSON.serialize(cursor);
        //System.out.println("cursor found::" + out);
        return out;

    }
    /* you can also walk the cursor this way:
       while(cursor.hasNext()) {
            DBObject obj = cursor.next();
            System.out.println("cursor found::" + obj);
        }
    */

    public DBCursor getCursor(String databaseName, String collection, String queryJson, int skip, int limit)  {
        DBCollection col = getDB(databaseName).getCollection(collection);
        DBObject query = (DBObject) JSON.parse(queryJson);
        DBCursor cursor = col.find(query).skip(skip).limit(limit);
        return cursor;
    }


    /** @return In REST style, returns the document you posted, with _id field filled in.*/
    public String post(String databaseName, String collection, String json){
        DBCollection col = getDB(databaseName).getCollection(collection);
        DBObject doc = (DBObject) JSON.parse(json);
        WriteResult result = col.insert(doc);
        return doc.toString();
    }

    /** The _id field is extracted from the JSON you pass in,
     *    and a mongo query is constructed to look for that _id in the collection.
     *  @return In REST style, returns the document you put.*/
    public String put(String databaseName, String collection, String json, boolean upsert, boolean multi){
        DBCollection col = getDB(databaseName).getCollection(collection);

        DBObject dbobj = (DBObject) JSON.parse(json);
        Object id = dbobj.get("_id");
        if (id == null){
            return "{\"error\":\"_id not found in payload.  This is a required field for PUT\"}";
        }

        DBObject query = (DBObject) JSON.parse("{}");
        query.put("_id", id);

        WriteResult result = col.update(query, dbobj, upsert, multi);
        if (result.isUpdateOfExisting()){
            return dbobj.toString();
        } else {
            return result.toString();
        }

    }

    public String delete(String databaseName, String collection, String queryJson){
        DBCollection col = getDB(databaseName).getCollection(collection);

        DBObject dbobj = (DBObject) JSON.parse(queryJson);
        WriteResult wr = col.remove(dbobj);
        return wr.toString();
    }


    //================================================================================================================

    public final String RL = "\n===>    ";
    public final String HH = "\n===== ";

    public void testNativeAPI(){
        ObjectId id =
        postGetId("anarchia",
                "images",
                "{'img':'http://anarchia.us/laramie-wendi/vince.jpg', "
                +" 'author':'laramie',"
                +" '_type': 'Image',"
                +" 'text':{"
                +"         'title':'Vince', "
                +"         'caption':'Vince'"
                +"  }"
                +"}" );
        System.out.println(HH+"(native) POST, result id: "+id);

        String putBody = "{'img':'http://anarchia.us/laramie-wendi/vince.jpg', "
                +" 'author':'laramie',"
                +" '_type': 'Image',"
                +" 'text':{"
                +"         'title':'Vince', "
                +"         'caption':'Vince tries to stay in the moment'"
                +"  }"
                +"}";
        ObjectId updatedID =
        putGetId("anarchia",
                "images",
                id,
                putBody,
                true,
                false);
        System.out.println(HH+"(native) UPDATE("+putBody+"), result id: "+updatedID);
    }

    public void testREST_POST_PUT() {
        //without using our own _id:
        String jsonPostResult =
                post("anarchia",
                        "images",
                        "{'img':'http://anarchia.us/laramie-wendi/cafe.jpg', "
                                + " 'author':'laramie',"
                                + " '_type': 'Image',"
                                + " 'text':{"
                                + "         'title':'At the AnarchiaGeneralStore 1', "
                                + "         'caption':'Their eyes were watching Bob'"
                                + "  }"
                                + "}"
                );
                post("anarchia",
                        "images",
                        "{'img':'http://anarchia.us/laramie-wendi/cafe.jpg', "
                                + " 'author':'laramie',"
                                + " '_type': 'Image',"
                                + " 'text':{"
                                + "         'title':'At the AnarchiaGeneralStore 2', "
                                + "         'caption':'Their eyes were watching Bob'"
                                + "  }"
                                + "}"
                );
        System.out.println(HH+"POST result: " +RL+ jsonPostResult);

        jsonPostResult = jsonPostResult.replaceAll("AnarchiaGeneralStore", "Anarchia General Store");
        String updatedObj =
                put("anarchia",
                        "images",
                        jsonPostResult,
                        true,
                        false);
        System.out.println(HH+"PUT, updated obj: " +RL+ updatedObj);
    }

    public void testREST_POST_PUT_own_ids(){
        //Now with using our own _id:
        String sequence = ""+System.currentTimeMillis();
        String ourid = "'/laramie-wendi/ralph.jpg-"+sequence+"'";
        String body = "{'img':'http://anarchia.us/laramie-wendi/ralph.jpg', "
                +" 'author':'laramie',"
                +" '_id':"+ourid+","
                +" '_type': 'Image',"
                +" 'text':{"
                +"         'title':'Ralph', "
                +"         'caption':'Officer Ralph Shank"+sequence+"'"
                +"  }"
                +"}";
        String jsonPostResult3 =
        post("anarchia",
            "images",
            body
            );
        System.out.println(HH+"POST("+body+") with (our id: "+ourid+"), result: "+RL+jsonPostResult3);

        String  updatedObj3 =
        put("anarchia",
            "images",
            jsonPostResult3,
            true,
            false);
        System.out.println(HH+"UPDATE("+jsonPostResult3+") with our id: "+RL+updatedObj3);
    }

    public void testREST_DELETE_ALL() {
        String queryJson = "{'author':'laramie'}";
        String res = delete("anarchia", "images", queryJson);
        System.out.println(HH+"DELETE("+queryJson+"): "+RL+res);
    }

    public void testREST_DELETE() {
        post("anarchia",
                "images",
                "{'_id':'delete-me', 'img':'http://anarchia.us/laramie-wendi/cafe.jpg', "
                        + " 'author':'laramie',"
                        + " '_type': 'Image',"
                        + " 'text':{"
                        + "         'title':'This record should have been deleted', "
                        + "         'caption':'A test post for deletion'"
                        + "  }"
                        + "}"
        );
        String queryJson = "{'_id':'delete-me'}";
        String res = delete("anarchia", "images", queryJson);
        System.out.println(HH+"DELETE("+queryJson+"): "+RL+res);
    }

    public void testREST_GETALL(){
        String query = "{'author':'laramie'}";
        String json = getAll("anarchia", "images", query, 0, 1000);
        System.out.println(HH+"GETALL("+query+"): "+RL+json);
    }

    public void testREST_GET(){
        String query = "{'author':'laramie'}";
        String json = get("anarchia", "images", query);
        System.out.println(HH+"GET("+query+"): "+RL+json);
    }

    public static void unitTestAll() throws UnknownHostException {
        MongoREST backend = new MongoREST();
        try {
            backend.setup("anarchia", "bob", "MangoDog");
            backend.testNativeAPI();
            backend.testREST_POST_PUT();
            backend.testREST_POST_PUT_own_ids();
            backend.testREST_DELETE();
            backend.testREST_GET();
            backend.testREST_GETALL();
            backend.testREST_DELETE_ALL();
            backend.testREST_GETALL();
        } finally {
            backend.close();
        }
    }

    public static void populate() throws UnknownHostException {
        MongoREST backend = new MongoREST();
        try {
            backend.setup("anarchia", "bob", "MangoDog");
            backend.testREST_POST_PUT();
            backend.testREST_GETALL();
        } finally {
            backend.close();
        }
    }


    public static void main(String[] args)
    throws Exception {
        if (args.length > 0){
            if ("-populate".equalsIgnoreCase(args[0])){
                populate();
            } else {
                System.out.println("ARGS incorrect: "+args);
            }
        } else {
            unitTestAll();
        }
    }
}