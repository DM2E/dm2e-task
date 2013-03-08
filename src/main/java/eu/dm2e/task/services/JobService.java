package eu.dm2e.task.services;

/*
 * GET /xslt?resource=URL
 * 		
 * POST /xslt
 * PUT /xslt
 * 		Body contains URL of a transformation specification which must be dereferenceable
 */

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.rabbitmq.client.Channel;

import eu.dm2e.task.model.Job;
import eu.dm2e.task.model.JobStatus;
import eu.dm2e.task.util.CatchallJerseyException;
import eu.dm2e.task.util.MongoConnection;
import eu.dm2e.task.util.RabbitConnection;

@Path("/job-json")
public class JobService {
	
	private static final Logger logger = Logger.getLogger(Job.class);

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJob(@PathParam("id") String id) throws CatchallJerseyException {
		Job retrievedJob;

		logger.warn("I LIVE?");
		DBCollection jobs = MongoConnection.getCollection("jobs");
		try {
			DBObject doc = jobs.findOne(new BasicDBObject("_id", new ObjectId( id)));
			retrievedJob = Job.fromMongoDoc(doc);
		} catch (IllegalArgumentException e) {
			throw new CatchallJerseyException(e, "Malformed job ID");
//		} catch (Exception e) {
//			throw new CatchallJerseyException(e);
		}

		ResponseBuilder builder;
		if (retrievedJob.getStatus() == JobStatus.FINISHED) {
			builder = Response.status(200);
		} else if (retrievedJob.getStatus() == JobStatus.NOT_STARTED) {
			builder = Response.status(202);
		} else {
			builder = Response.ok(201);
		}
		
		builder.entity(retrievedJob.toJsonStr());
		
		return builder.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newJob(@Context UriInfo uriInfo, String body) throws CatchallJerseyException {

		Job.validateJsonJob(body);
		DBObject bodyDoc = (DBObject) JSON.parse(body);

		DBCollection jobs = MongoConnection.getCollection("jobs");

		// first save to get an _id
		jobs.save(bodyDoc);

		// build Job POJO
		Job job = Job.fromMongoDoc(bodyDoc);
		
		// set the url
		job.setJobURL(uriInfo.getRequestUri() + "/" + job.get_id());
		
		// save again for the url
		jobs.save(job.toMongoDoc());

		// TODO post to worker via RabbitMQ!
		byte[] messageBytes = job.getJobURL().getBytes();
		Channel channel;
		try {
			channel = RabbitConnection.getChannel();
			channel.basicPublish("", job.getJobQueue(), null, messageBytes);
			channel.close();
		} catch (IOException e) {
			throw new CatchallJerseyException(e);
		}
		
		return Response.status(202).entity(job.toJsonStr()).build();
	}

//	@Target({ElementType.METHOD})
//	@Retention(RetentionPolicy.RUNTIME)
//	@HttpMethod("PATCH")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response updateJob(@Context UriInfo uriInfo, @PathParam("id") String id, String body) {
//		try {
			
			DBCollection jobs = MongoConnection.getCollection("jobs");
			
			// a document with just the id set, i.e. the query
			BasicDBObject idDoc = new BasicDBObject().append("_id", new ObjectId(id));
			
			// the changed doc
			DBObject doc = (DBObject) JSON.parse(body);
			
			// the doc that should be updated
			DBObject docToUpdate = jobs.findOne(new BasicDBObject().append("_id", new ObjectId(id)));
			
			// update the doc
			docToUpdate.putAll(doc);
			
			// and update it
			jobs.update(idDoc, docToUpdate);
			logger.warn(idDoc);
			logger.warn(docToUpdate);
			
			DBObject savedDoc = jobs.findOne(idDoc);
			logger.warn(savedDoc);
					
			// all went well
			return Response.status(202).entity(docToUpdate.toString()).build();
			
//		} catch (Exception e) {
//			throw new CatchallJerseyException(e);
//		}
	}

}
