importClass(Packages.com.mongodb.Mongo);
importClass(Packages.com.mongodb.rhino.BSON);
delete JSON
importClass(Packages.com.threecrickets.rhino.JSON);
JSON = com.threecrickets.rhino.JSON
//importPackage(com.mongodb);
//importPackage(com.mongodb.rhino);

var connection = new Mongo()
var db = connection.getDB('tags')
var collection = db.getCollection('items')

var doc = {name: 'hello'}
collection.insert(BSON.to(doc))

var query = {name: 'hello'}
var update = {$push: {anArray: 'aValue3'}}
collection.update(BSON.to(query), BSON.to(update), false, true)

var query = {name: /he(.*)/i}
var doc = BSON.from(collection.findOne(BSON.to(query)))
java.lang.System.out.println(JSON.to(doc))
