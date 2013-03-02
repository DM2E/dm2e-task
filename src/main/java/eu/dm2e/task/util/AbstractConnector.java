package eu.dm2e.task.util;

import java.io.IOException;
import java.net.UnknownHostException;

import org.jongo.Jongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public abstract class AbstractConnector {
	protected Connection getRabbitConnection() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setPort(5672);
		Connection rabbitConnection = null;
		try {
			rabbitConnection = factory.newConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rabbitConnection;
	}

	protected Jongo getMongoConnection() {
		// set up mongo
	    MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient( "localhost" , 27017 );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    DB db = mongoClient.getDB("dm2e");
	    return new Jongo(db);
	}
	protected void closeMongoConnection(Jongo jongo) {
		jongo.getDatabase().getMongo().close();
	}

	protected abstract String getRabbitQueueName();

	/**
	 * Sets up connections to MongoDB (via Jongo) and RabbitMQ
	 * @throws IOException 
	 */
//	protected void setupDbConnections() throws IOException {
//		// set up rabbitmq
//		
//	}
	
//	protected void closeDbConnections() throws IOException {
//		getRabbitChannel().close();
//		getRabbitConnection().close();
//	}

}
