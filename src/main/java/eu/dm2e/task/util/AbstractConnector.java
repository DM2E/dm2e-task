package eu.dm2e.task.util;

import java.io.IOException;

import org.jongo.Jongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public abstract class AbstractConnector {
	private Connection rabbitConnection;
	private Channel rabbitChannel;
	private Jongo mongoConnection;
	
	protected Channel getRabbitChannel() {
		return rabbitChannel;
	}

	protected void setRabbitChannel(Channel rabbitChannel) {
		this.rabbitChannel = rabbitChannel;
	}


	protected Connection getRabbitConnection() {
		return rabbitConnection;
	}

	protected void setRabbitConnection(Connection rabbitConnection) {
		this.rabbitConnection = rabbitConnection;
	}

	protected Jongo getMongoConnection() {
		return mongoConnection;
	}

	protected void setMongoConnection(Jongo mongoConnection) {
		this.mongoConnection = mongoConnection;
	}
	
	protected abstract String getRabbitQueueName();

	/**
	 * Sets up connections to MongoDB (via Jongo) and RabbitMQ
	 * @throws IOException 
	 */
	protected void setupDbConnections() throws IOException {
		// set up rabbitmq
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setPort(5672);
		this.setRabbitConnection(factory.newConnection());
		this.setRabbitChannel(this.getRabbitConnection().createChannel());
	    this.getRabbitChannel().queueDeclare(getRabbitQueueName(), false, false, false, null);
		
		// set up mongo
	    MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
	    DB db = mongoClient.getDB("test");
	    this.setMongoConnection(new Jongo(db));
	}
	
	protected void closeDbConnections() throws IOException {
		getRabbitChannel().close();
		getRabbitConnection().close();
	}

}
