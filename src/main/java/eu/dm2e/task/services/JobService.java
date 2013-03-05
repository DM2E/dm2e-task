package eu.dm2e.task.services;

/*
 * GET /xslt?resource=URL
 * 		
 * POST /xslt
 * PUT /xslt
 * 		Body contains URL of a transformation specification which must be dereferenceable
 */

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import eu.dm2e.task.model.Job;

@Path("/job")
public class JobService extends AbstractTaskService {
	
	private static ObjectMapper mapper = new ObjectMapper();

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
		builder.entity(mapper.writeValueAsBytes(retrievedJob));
		return builder.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@Context UriInfo uriInfo, String body) throws IOException {
		Logger log  = Logger.getLogger(getClass().getName());
		Job job;
		try {
			job = mapper.readValue(body, Job.class);
		} catch (EOFException e) {
			return Response.status(400).entity("Bad JSON or no JSON").build();
		}
		catch (JsonParseException e) {
			return Response.status(400).entity(e.toString()).build();
		}
		
	    Jongo jongo = getMongoConnection();
	    MongoCollection jobs = jongo.getCollection("jobs");
	    jobs.save(job);
	    job.setJobURL(uriInfo.getRequestUri() + "/" + job.getJobID());
	    jobs.save(job);
	    closeMongoConnection(jongo);
	    
	    // TODO post to worker via RabbitMQ!
	    
		byte[] jobJSONstr = mapper.writeValueAsBytes(job);
	    return Response.status(202).entity(jobJSONstr).build();
	}

}
