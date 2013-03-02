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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

import eu.dm2e.task.model.Job;

@Path("/job")
public class JobListingService extends AbstractTaskService {
	

	@Override
	protected String getRabbitQueueName() {
		return "eu.dm2e.task.jobs";
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJob(@PathParam("id") String id) throws IOException {
		Job retrievedJob;
		
		Jongo jongo = getMongoConnection();
	    MongoCollection jobs = jongo.getCollection("jobs");
		try {
			retrievedJob = jobs.findOne().as(Job.class);
		} catch (IllegalArgumentException e) {
			closeMongoConnection(jongo);
			return Response.status(400).entity("Error: Malformed job ID").build();
		} 
		closeMongoConnection(jongo);
		
		if (null == retrievedJob) {
			return Response.status(404).entity("Error: No such job").build();
		}
		
		ResponseBuilder builder = Response.ok();
		if ("NOT_STARTED".equals(retrievedJob.getStatus())) {
			builder.status(202);
		}
		builder.entity(retrievedJob);
		return builder.build();
	}

	@POST
	public Response post(String body) throws IOException {
		Logger log  = Logger.getLogger(getClass().getName());
		
	    Job myJob = new Job();
	    log.warning(myJob.getStatus());
	    
	    Jongo jongo = getMongoConnection();
	    MongoCollection jobs = jongo.getCollection("jobs");
	    jobs.save(myJob);
	    closeMongoConnection(jongo);
	    
	    return Response.status(202).entity(myJob).build();
	    
	}

}
