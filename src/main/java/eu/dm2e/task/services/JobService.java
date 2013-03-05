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

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

import eu.dm2e.task.model.Job;
import eu.dm2e.task.model.JobStatus;

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
		Logger log = Logger.getLogger(getClass().getName());
		Job retrievedJob;
		
		DB mongo  = getMongoConnection();
		DBCollection coll = mongo.getCollection("jobs");
//	    MongoCollection jobs = jongo.getCollection("jobs");
		try {
			DBObject doc = coll.findOne(new BasicDBObject("_id", new ObjectId(id)));
			retrievedJob = Job.fromMongoDoc(doc);
			log.warning(retrievedJob.toString()	);
		} catch (IllegalArgumentException e) {
			return Response.status(400).entity("Error: Malformed job ID").build();
		} catch (NullPointerException e) {
			return Response.status(404).entity(e.toString()).build();
		} finally {
//			closeMongoConnection(jongo);
		}
		
		ResponseBuilder builder;
		if (retrievedJob.getStatus() == JobStatus.FINISHED) {
			builder = Response.status(200);
		}
		else if (retrievedJob.getStatus() == JobStatus.NOT_STARTED) {
			builder = Response.status(202);
		}
		else {
			builder = Response.ok(202);
		}
		builder.entity(mapper.writeValueAsBytes(retrievedJob));
		return builder.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@Context UriInfo uriInfo, String body) throws IOException {
//		Logger log  = Logger.getLogger(getClass().getName());
		
		DBObject bodyDoc = (DBObject) JSON.parse(body);
//		log.warning(bodyDoc.toString());
		
	    DB mongo = getMongoConnection();
	    DBCollection jobs = mongo.getCollection("jobs");
	    
	    // first save to get an _id
	    jobs.save(bodyDoc);
	    
	    // build Job POJO
	    Job job = Job.fromMongoDoc(bodyDoc);
	    // set the url
	    job.setJobURL(uriInfo.getRequestUri() + "/" + job.getJobID());
	    // save again for the url
	    jobs.save(job.toMongoDoc());
	    
	    // TODO post to worker via RabbitMQ!
	    
	    return Response.status(202).entity(job.toJsonStr()).build();
	}

}
