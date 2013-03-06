package eu.dm2e.task.util;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitConnection {
	
	// TODO configurable!
	private static final String RABBIT_HOST = "localhost";
	private static final int RABBIT_PORT = 5672;
	
	public static synchronized Connection getConnection() throws IOException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(RABBIT_HOST);
		factory.setPort(RABBIT_PORT);
		Connection rabbitConnection = null;
		rabbitConnection = factory.newConnection();
		return rabbitConnection;
	}
	
	public static synchronized Channel getChannel() {
		Channel channel = null;
		try {
			Connection conn = getConnection();
			channel = conn.createChannel();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return channel;
	}
	
	public static synchronized void closeChannel(Channel channel) {
		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
