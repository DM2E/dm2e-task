package eu.dm2e.task.worker; 
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.dm2e.task.util.JobLogger;
import eu.dm2e.task.util.RabbitConnection;

/**
 * Abstract Base class of all workers
 * @author kb
 *
 */
public abstract class AbstractWorker implements Runnable {

	/**
	 * Reacts to a message sent to the worker by interpreting it as a run
	 * configuration and doing its thing.
	 * 
	 * @param message
	 *            The message sent to the worker
	 * @throws InterruptedException
	 */
	abstract void handleMessage(String message) throws InterruptedException;
	
	public abstract String getServiceUri();
	
	public abstract Client getClient();
	
	public abstract String getRabbitQueueName();

	protected void ensureResourcesReady(List<WebResource> unreadyResources, JobLogger log) throws Throwable {
		Iterator<WebResource> resIter = unreadyResources.iterator();
		while (resIter.hasNext()) {
			WebResource r = resIter.next();
			if (!r.getURI().getScheme().matches("^(h|f)ttps?")) {
				throw new Exception("Not an http/ftp link: " + r.getURI().getScheme());
			}
			log.fine("Polling HEAD on " + r.getURI());
			// TODO think about a good strategy to do this
			ClientResponse resp = r.head();
			if (resp.getStatus() == 200) {
				log.fine("Resource " + r.getURI() + " is ready now.");
				resIter.remove();
			} else {
				throw new Exception("Resource " + r.getURI() + " not available. Will croak for now.");
			}
		}
	}


	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Channel channel = RabbitConnection.getChannel();

		try {
			channel.queueDeclare(this.getRabbitQueueName(), true, false, false,
					null);
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
	
	public void waitForResources(List<WebResource> unreadyResources) {
	}
}