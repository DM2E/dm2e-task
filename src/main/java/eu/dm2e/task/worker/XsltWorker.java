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
	
	@Override
	public void doWork() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		Connection rabbit = getRabbitConnection();
		Channel channel = rabbit.createChannel();
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(getRabbitQueueName(), true, consumer);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		while (true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			System.out.println(" [x] Received '" + message + "'");
		}	
//		channel.close();
//		rabbit.close();
	}

	
	public static void main(String[] args) {
		XsltWorker worker = new XsltWorker();
		try {
			worker.doWork();
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
		}
	}



}
