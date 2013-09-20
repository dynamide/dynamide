package com.dynamide.db;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoURI;
import com.mongodb.util.JSON;

import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class MongoAutoSuggest {

    public static String testSerialization() throws Exception {
        Mongo m = getMongo();  //TODO: cache this properly.
        DB db = m.getDB("tags");
        db.getCollectionNames();
        DBCollection coll = db.getCollection("tags");
        Object myDoc = coll.findOne();
        String sr = JSON.serialize(myDoc);
        Object obj = JSON.parse(sr);
        if (obj instanceof BasicDBObject) {
            BasicDBObject newDoc = (BasicDBObject) obj;
            newDoc.put("mojo", "nixon");
            newDoc.put("_id", "todo.root2");
            coll.save(newDoc);
            return newDoc.toString();
        } else {
            return "ERROR: obj instanceof BasicDBObject false. obj.class = " + obj.getClass().getName();
        }
    }

    public static String storeNotesObject(String tag, String notesJSON) throws Exception {
        Mongo m = getMongo();     //TODO: cache this properly.
        DB db = m.getDB("tags");
        DBCollection coll = db.getCollection("tags");
        Object obj = JSON.parse(notesJSON);
        if (obj instanceof BasicDBObject) {
            BasicDBObject newDoc = (BasicDBObject) obj;
            coll.save(newDoc);


            //TODO: now also store the tag in the tags collection.... etc.


            return newDoc.toString();
        } else {
            return "ERROR: obj instanceof BasicDBObject false. obj.class = " + obj.getClass().getName();
        }
    }

    public static String storeNotes(String tag, String notes) throws Exception {
        Mongo m = getMongo();     //TODO: cache this properly.
        DB db = m.getDB("tags");
        DBCollection coll = db.getCollection("tags");
        DBObject item = new BasicDBObject();
        item.put("type", "notes");
        item.put("notes", notes);
        item.put("tag", tag);
        System.out.println("*************** tag: " + tag);
        System.out.println("*************** notes: " + notes);
        Object myDoc = coll.save(item);
        String sr = myDoc.toString();//JSON.serialize(myDoc);
        System.out.println("*************** result: " + sr);
        return sr;
    }

    public static DBCursor getNotes(String tag) throws Exception {
        Mongo m = getMongo();     //TODO: cache this properly.
        DB db = m.getDB("tags");
        DBCollection coll = db.getCollection("tags");
        DBObject query = new BasicDBObject();
        Pattern pattern = Pattern.compile(tag, Pattern.CASE_INSENSITIVE);
        query.put("tag", pattern);
        System.out.println("*************** tag: " + tag);
        DBCursor cursor = coll.find(query);
        System.out.println("*************** result: " + cursor.toString());

        return cursor;
    }

    public static String lookupAutocomplete(String term) throws Exception {
        Mongo m = getMongo();     //TODO: cache this properly.
        DB db = m.getDB("tags");
        DBCollection coll = db.getCollection("tags");
        if (true) {
            DBObject query = new BasicDBObject();
            Pattern pattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE);
            query.put("tag", pattern);

            Object myDoc = coll.distinct("tag", query);
            String sr = JSON.serialize(myDoc);
            return sr;
        }

        DBObject query = new BasicDBObject();
        query.put("type", "tag");
        //term = term.replaceAll("/", "\\/");

        Pattern pattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE);

        query.put("value", pattern);
        //System.out.println("*************** term: "+term);
        //System.out.println("*************** query: "+query);
        Object myDoc = coll.find(query);
        String sr = JSON.serialize(myDoc);
        //System.out.println("*************** result: "+sr);
        return sr;
    }

    public static void main(String args[]) throws Exception {
        testSerialization();
        lookupAutocomplete("/aa");
        storeNotes("/aa/bb", "This is my note\r\nline 2.");
        getNotes("/aa/bb");
        storeNotesObject("/a/b/c", "{note:'', tag:'/a/b/c'}");
    }

    private static Mongo mongo = null;

    public static Mongo getMongo() {
        if (mongo == null) {
            try {
                MongoURI mongoURI = new MongoURI("mongodb://127.0.0.1");
                mongo = (new Mongo.Holder()).connect(mongoURI);
            } catch (UnknownHostException e) {
                System.err.println("Error getting mongo: " + e);
            }
        }
        return mongo;
    }

}
