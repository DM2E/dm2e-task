package eu.dm2e.task.worker;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class XsltWorker extends AbstractWorker {

	@Override
	protected String getRabbitQueueName() {
		return "eu.dm2e.task.xslt";
	}
	
	public void run() {
		Connection rabbit = getRabbitConnection();
		Channel channel = null;
		try {
			channel = rabbit.createChannel();
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(getRabbitQueueName(), true, consumer);
			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());
				System.out.println(" [x] Received '" + message + "'");
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ShutdownSignalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println(this.getClass() + " has been interrupted.");
			return;
		}
		finally {
			try {
				channel.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
//	public static void main(String[] args) {
//		XsltWorker worker = new XsltWorker();
//		worker.run();
//	}

}
