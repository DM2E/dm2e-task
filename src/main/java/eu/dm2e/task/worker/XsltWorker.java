package eu.dm2e.task.worker;

import javax.ws.rs.core.MediaType;

import com.mongodb.BasicDBObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import eu.dm2e.task.model.JobStatus;

/**
 * This worker transforms XML to XML using an XSLT style sheet and the Saxon XSL
 * transformation engine
 * 
 * @todo should think about where to store client and webresources (client
 *       creation is expensive and thread safe)
 *       http://stackoverflow.com/questions
 *       /8012680/jersey-client-connection-close-memory-leak-issue
 * @author kb
 * 
 */
public class XsltWorker extends AbstractWorker {

	final static String rabbitQueueName = "eu.dm2e.task.worker.XsltWorker";

	private Client client = new Client();

	@Override
	public String getRabbitQueueName() {
		return rabbitQueueName;
	}

	@Override
	public void handleMessage(String message) throws InterruptedException {
		WebResource r = client.resource(message);
		String response;
		System.out.println("Pretending to do work");
		response = r
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.put(String.class,
						new BasicDBObject().append("jobStatus",
								JobStatus.STARTED.toString()).toString());
		Thread.sleep(10000);
		System.out.println("Done! ;-)");
		response = r
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.put(String.class,
						new BasicDBObject().append("jobStatus",
								JobStatus.FINISHED.toString()).toString());
		System.out.println(response);
	}
}