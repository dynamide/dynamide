package com.tomitribe.security;


import com.mongodb.*;
import com.mongodb.DBCursor;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

import java.net.UnknownHostException;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;


import java.io.*;

public class MongoRealm extends RealmBase {

    
    protected boolean logDebug = false;
    
    protected boolean logSystemlog = false;

    protected String mongoClientURI = "";

    protected String database = "security_realm";

    protected String userCollection = "user";

    protected String usernameField = "username";

    protected String credentialsField = "credentials";

    protected String rolesField = "roles";

    protected String roleNameField = "name";

    protected static MongoClient mongoClient = null;
    
    public boolean DEBUG = false;
    public void setDEBUG(boolean newValue){
        DEBUG = newValue;
    }
    protected void debug(String msg) {
        if (logDebug) System.out.println("MongoRealm [debug]  =========:::: "+msg);
    }
    protected void systemlog(String msg) {
        if (logSystemlog) System.out.println("MongoRealm [system] =========:::: "+msg);
    }

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String info =
            "com.tomitribe.security.MongoRealm/1.0";

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "MongoRealm";


    /**
     * Return the URI to use to connect to the database.
     */
    public String getMongoClientURI() {
        return mongoClientURI;
    }
    
    public String getLogDebug() {
        return ""+logDebug;
    }
    
    public String getLogSystemlog() {
        return ""+logSystemlog;
    }

    /**
     * Set the URI to use to connect to the database.
     *
     * @param mongoClientURI The new connection URI
     */
    public void setMongoClientURI(String mongoClientURI) {
        this.mongoClientURI = mongoClientURI;
    }
    
    public void setLogDebug(String newProp) {
        this.logDebug = Boolean.valueOf(newProp);
    }
    
    public void setLogSystemlog(String newProp) {
        this.logSystemlog = Boolean.valueOf(newProp);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUserCollection() {
        return userCollection;
    }

    public void setUserCollection(String userCollection) {
        this.userCollection = userCollection;
    }

    public String getUsernameField() {
        return usernameField;
    }

    public void setUsernameField(String usernameField) {
        this.usernameField = usernameField;
    }

    public String getCredentialsField() {
        return credentialsField;
    }

    public void setCredentialsField(String credentialsField) {
        this.credentialsField = credentialsField;
    }

    public String getRolesField() {
        return rolesField;
    }

    public void setRolesField(String rolesField) {
        this.rolesField = rolesField;
    }

    public String getRoleNameField() {
        return roleNameField;
    }

    public void setRoleNameField(String roleNameField) {
        this.roleNameField = roleNameField;
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    protected String getName() {
        return name;
    }

    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     * <p/>
     * If there are any errors with the MongoDB connection, executing
     * the query or anything we return null (don't authenticate). This
     * event is also logged, and the connection will be closed so that
     * a subsequent request will automatically re-open it.
     *
     * @param username    Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *                    authenticating this username
     */
    @Override
    public synchronized Principal authenticate(String username, String credentials) {
        debug("authenticate "+username);
        

        // No user or no credentials
        // Can't possibly authenticate, don't bother the database then
        if (username == null || credentials == null) {
            debug("null username or credentials");
            return null;
        }

        // Look up the user's credentials
        debug("dbCredentials...");
        String dbCredentials = getPassword(username);

        if (dbCredentials != null) {
            debug("credentials not null");

            // Validate the user's credentials
            boolean validated = false;
            if (hasMessageDigest()) {
                // Hex hashes should be compared case-insensitive
                validated = (digest(credentials).equalsIgnoreCase(dbCredentials));
            } else {
                validated = (digest(credentials).equals(dbCredentials));
            }

            if (!validated) {
                systemlog(String.format("Authentication Failure for %s, password mismatch", username));
                return (null);
            }

            ArrayList<String> roles = getRoles(username);
            systemlog(String.format("Authentication Success for %s, roles: %s", username, roles));

            // Create and return a suitable Principal for this user
            return (new GenericPrincipal(username, credentials, roles));

        } else {
            debug("dbCredentials == null");
            String msg = (String.format("Credentials for %s could not be located", username));
            containerLog.warn(msg);
            systemlog(msg);

            // Worst case is if there is no matching user record or password
            // The Tomcat API throws null around too much - this follows a pattern
            // established by the JDBCRealm
            return null;
        }
    }

    private DB openMongoDB() throws UnknownHostException {
        debug("openMongoDB");
        if (mongoClient == null) {
            systemlog("MongoRealm starting up, connecting to mongodb");
            mongoClient = new MongoClient(new MongoClientURI(getMongoClientURI()));
        }
        debug("mongo opened for mongoClientURI: "+getMongoClientURI());
        String theDB = getDatabase();
        debug("mongo db: "+theDB);
        DB result = mongoClient.getDB(theDB);
        debug("mongo DB handle: "+result);
        return result;
    }


    @Override
    protected String getPassword(String username) {

        String credentials = null;

        try {
            debug("getPassword");

            DB db = openMongoDB();
            DBCollection userCol = db.getCollection(userCollection);
            BasicDBObject query = new BasicDBObject(usernameField, username);
            debug("userCol.findOne("+query+") userCol: "+userCol);
            DBObject obj = null;
            try {
                obj = userCol.findOne(query);
            } catch (Exception e){
                systemlog("Exception trying to findOne("+query+") EX: "+e.getMessage());

                DBCursor cursor = userCol.find();
                while(cursor.hasNext()) {
                   DBObject objall = cursor.next();
                    systemlog("Find all: "+objall.toString());
                }

                return null;
            }
            debug("getPassword: "+(obj!=null?obj.toString():"user not found in db: "+username));


            if (obj == null) {
                // Log the problem for posterity
                containerLog.warn(String.format("Unknown username exception, Username: %s", username));
                return null;
            }

            credentials = (String) obj.get(getCredentialsField());

        } catch (UnknownHostException e) {

            // Log the problem for posterity
            containerLog.error(String.format("Unknown host exception, Mongo URI: %s", getMongoClientURI()), e);
            debug(String.format("Unknown host exception, Mongo URI: %s", getMongoClientURI())+ e);

        }

        return credentials;
    }

    @Override
    protected Principal getPrincipal(String username) {
        debug("getPrinciple");

        // Create and return a suitable Principal for this user
        return (new GenericPrincipal(username, getPassword(username), getRoles(username)));

    }

    /**
     * Return the roles associated with the gven user name.
     */
    protected ArrayList<String> getRoles(String username) {
        debug("getRoles");

        if (allRolesMode != AllRolesMode.STRICT_MODE && !isRoleStoreDefined()) {
            // Using an authentication only configuration and no role store has
            // been defined so don't spend cycles looking
            return null;
        }


        ArrayList<String> roles = new ArrayList<String>();

        try {

            DB db = openMongoDB();
            DBCollection userCol = db.getCollection(userCollection);
            BasicDBObject query = new BasicDBObject(usernameField, username);
            DBObject userObj = userCol.findOne(query);

            if (userObj == null) {
                // Log the problem for posterity
                String msg = String.format("Unknown username exception, Username: %s", username);
                containerLog.warn(msg);
                systemlog(msg);
                return roles;
            }

            BasicDBList rolesList = (BasicDBList) userObj.get(rolesField);

            Iterator roleIterator = rolesList.iterator();
            while (roleIterator.hasNext()) {
                BasicDBObject roleObj = (BasicDBObject) roleIterator.next();
                roles.add((String) roleObj.get(getRoleNameField()));
            }

        } catch (UnknownHostException e) {

            // Log the problem for posterity
            containerLog.error(String.format("Unknown host exception, Mongo URI: %s", getMongoClientURI()), e);
            debug(String.format("Unknown host exception, Mongo URI: %s", getMongoClientURI())+ e);


        }

        return roles;
    }

    private boolean isRoleStoreDefined() {
        return rolesField != null || roleNameField != null;
    }


}
