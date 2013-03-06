package eu.dm2e.task.worker;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import eu.dm2e.task.util.RabbitConnection;

public abstract class AbstractWorker implements Runnable {

	abstract void handleMessage(String message) throws InterruptedException;
	abstract String getRabbitQueueName();

	public void run() {
		Channel channel = RabbitConnection.getChannel();

		try {
			channel.queueDeclare(this.getRabbitQueueName(), true, false, false, null);
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(this.getRabbitQueueName(), true, consumer);
			System.out
					.println(" [*] Waiting for messages. To exit press CTRL+C");
			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());
				System.out.println(" [x] Received '" + message + "'");
				this.handleMessage(message);
			}
		} catch (ShutdownSignalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			RabbitConnection.closeChannel(channel);
		}
	}

}