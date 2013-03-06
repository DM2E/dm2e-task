package eu.dm2e.task.util;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class MongoConnection {
	
	// the singleton
	private static MongoClient instance;
	
	// default database
	// TODO this should be configurable, using properies or Preferences
	private static final String DEFAULT_DB_NAME = "dm2e";
	
	// forbid instantiation
	private MongoConnection() { }
	
	public static synchronized MongoClient getInstance(){
		if (instance == null) {
			try {
				instance = new MongoClient();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return instance;
	}
	
	public static synchronized DB getDB(String dbname) {
		return getInstance().getDB(dbname);
	}
	public static synchronized DB getDB() {
		return getDB(DEFAULT_DB_NAME);
	}
	public static synchronized DBCollection getCollection(String dbname, String colname) {
		return getDB(dbname).getCollection(colname);
	}
	public static synchronized DBCollection getCollection(String colname) {
		return getDB().getCollection(colname);
	}
}
