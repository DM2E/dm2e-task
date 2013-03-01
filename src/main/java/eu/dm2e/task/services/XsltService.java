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

import org.jongo.MongoCollection;

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
		this.setupDbConnections();
		
	    String message = "Hello World!";
		
	    getRabbitChannel().basicPublish("", getRabbitQueueName(), null, message.getBytes());
	    log.warning(" [x] Sent '" + message + "'");
	    getRabbitChannel().close();
	    getRabbitConnection().close();
	    
	    Job myJob = new Job();
	    log.warning(myJob.getStatus());
	    
	    MongoCollection jobs = getMongoConnection().getCollection("jobs");
	    log.warning(myJob.get_id());
	    jobs.save(myJob);
//	    Job retrievedJob = jobs.findOne("{status: 'NOT_STARTED'}").as(Job.class);
	    log.warning(myJob.get_id());
	    
	    return Response.ok(message).build();
	    
	}

}
