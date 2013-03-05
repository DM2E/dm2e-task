package eu.dm2e.task.services;

/*
 * GET /xslt?resource=URL
 * 		
 * POST /xslt
 * PUT /xslt
 * 		Body contains URL of a transformation specification which must be dereferenceable
 */

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import eu.dm2e.task.model.Job;

@Path("/xslt")
public class XsltService extends AbstractTaskService {
	

	@Override
	protected String getRabbitQueueName() {
		return "eu.dm2e.task.xslt";
	}
	
	@PUT
	public Response post(String body) throws IOException {
		Logger log  = Logger.getLogger(getClass().getName());
		
	    String message = "Hello World!";
		
	    Connection rabbit = getRabbitConnection();
	    Channel channel = rabbit.createChannel();
	    channel.queueDeclare(getRabbitQueueName(), false, false, false, null);
	    channel.basicPublish("", getRabbitQueueName(), null, message.getBytes());
	    log.warning(" [x] Sent '" + message + "'");
	    channel.close();
	    rabbit.close();
	    
	    Job myJob = new Job();
	    log.warning(myJob.getStatus().toString());
	    
	    Jongo jongo = getMongoConnection();
	    MongoCollection jobs = jongo.getCollection("jobs");
	    log.warning(myJob.getJobID());
	    jobs.save(myJob);
//	    Job retrievedJob = jobs.findOne("{status: 'NOT_STARTED'}").as(Job.class);
	    log.warning(myJob.getJobID());
	    
	    return Response.ok(message).build();
	}

}
