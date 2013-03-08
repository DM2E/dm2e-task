package eu.dm2e.task.services;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;

import eu.dm2e.task.model.JobStatus;
import eu.dm2e.task.util.CatchallJerseyException;
import eu.dm2e.task.util.SparqlConstruct;
import eu.dm2e.task.util.SparqlUpdate;
import eu.dm2e.ws.grafeo.GLiteral;
import eu.dm2e.ws.grafeo.GResource;
import eu.dm2e.ws.grafeo.Grafeo;
import eu.dm2e.ws.grafeo.jena.GrafeoImpl;
import eu.dm2e.ws.services.data.AbstractRDFService;

@Path("/job")
public class JobRdfService extends AbstractRDFService {

	private Logger log = Logger.getLogger(getClass().getName());
	private static final String 
								NS_DM2E = "http://onto.dm2e.eu/",
								NS_DM2E_LOGGING = "http://onto.dm2e.eu/logging#",
								ENDPOINT = "http://lelystad.informatik.uni-mannheim.de:8080/openrdf-sesame/repositories/dm2etest",
								ENDPOINT_STATEMENTS = "http://lelystad.informatik.uni-mannheim.de:8080/openrdf-sesame/repositories/dm2etest/statements",
								JOB_STATUS_PROP = NS_DM2E + "status",
								JOB_LOGENTRY_PROP = NS_DM2E + "hasLogEntry";
	
	private @Context UriInfo uriInfo;
	
	@GET
	@Path("/{resourceID}")
	@Consumes(MediaType.WILDCARD)
	public Response getJob(@Context UriInfo uriInfo,
			@PathParam("resourceID") String resourceID)
			throws CatchallJerseyException {
		// kb: need to use Jena model
		GrafeoImpl g = new GrafeoImpl();
		g.readFromEndpoint(ENDPOINT, uriInfo.getRequestUri().toString());
		Model jenaModel = g.getModel();
		NodeIterator iter = jenaModel.listObjectsOfProperty(jenaModel
				.createProperty(NS_DM2E + "status"));
		if (null == iter || !iter.hasNext()) {
			throw new CatchallJerseyException(
					"No Job Status in this one. Not good.");
		}
		String jobStatus = iter.next().toString();
		int httpStatus;
		if (jobStatus.equals(JobStatus.NOT_STARTED.toString()))
			httpStatus = 202;
		else if (jobStatus.equals(JobStatus.NOT_STARTED.toString()))
			httpStatus = 202;
		else if (jobStatus.equals(JobStatus.FAILED.toString()))
			httpStatus = 409;
		else if (jobStatus.equals(JobStatus.FINISHED.toString()))
			httpStatus = 200;
		else
			httpStatus = 400;

		return Response.status(httpStatus).entity(getResponseEntity(g)).build();
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	public Response newJob(@Context UriInfo uriInfo, File bodyAsFile)
			throws CatchallJerseyException {
		log.info("Config posted.");
		// TODO use Exception to return proper HTTP response if input can not be
		// parsed as RDF
		Grafeo g;
		try {
			g = new GrafeoImpl(bodyAsFile);
			// } catch (MalformedURLException e) {
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}
		GResource blank = g.findTopBlank();
		if (blank == null) {
			throw new CatchallJerseyException(
					"No top blank node found. Check your job description.");
		}
		String id = "" + new Date().getTime();
		String uri = uriInfo.getRequestUri() + "/" + id;
		blank.rename(uri);
		g.addTriple(uri, "rdf:type", NS_DM2E + "Job");
		g.addTriple(uri, NS_DM2E + "status",
				g.literal(JobStatus.NOT_STARTED.toString()));
		g.writeToEndpoint(ENDPOINT_STATEMENTS, uri);
		return Response.created(URI.create(uri)).entity(getResponseEntity(g))
				.build();
	}
	
	@PUT
	@Consumes(MediaType.WILDCARD)
	@Path("/{id}")
	public Response replaceJob(@Context UriInfo uriInfo,
			@PathParam("id") String id, String body) {
		return null;
	}
	
	public JobStatus getJobStatusInternal(String jobUriStr) throws CatchallJerseyException {
		GrafeoImpl g = new GrafeoImpl();
		g.readFromEndpoint(ENDPOINT, jobUriStr);
		Model jenaModel = g.getModel();
		NodeIterator iter = jenaModel.listObjectsOfProperty(
				jenaModel.createProperty(NS_DM2E + "status"));
		if (null == iter || !iter.hasNext()) {
			throw new CatchallJerseyException(
					"No Job Status in this one. Not good.");
		}
		String jobStatus = iter.next().toString();
		return Enum.valueOf(JobStatus.class,jobStatus);
	}
	
	@GET
	@Path("/{id}/status")
	public Response getJobStatus(@Context UriInfo uriInfo, @PathParam("id") String id) throws CatchallJerseyException {
		String resourceUriStr = uriInfo.getRequestUri().toString().replaceAll("/status$", "");
		return Response.ok().entity(getJobStatusInternal(resourceUriStr).toString()).build();
	}
 	
	@PUT
	@Consumes(MediaType.WILDCARD)
	@Path("/{id}/status")
	public Response updateJobStatus(@Context UriInfo uriInfo,

			
			@PathParam("id") String id, String newStatus)
			throws CatchallJerseyException {
		String resourceUriStr = uriInfo.getRequestUri().toString().replaceAll("/status$", "");
		
		// validate if this is a valid status
		try {
			if (null == newStatus)
				throw new CatchallJerseyException("No status sent.");
			Enum.valueOf(JobStatus.class, newStatus);
		} catch (Exception e) {
			throw new CatchallJerseyException("Invalid status type: " + newStatus);
		}
		
		// get the old status
		String oldStatus;
		try {
			oldStatus = getJobStatusInternal(resourceUriStr).toString();
			if (null == oldStatus)
				throw new CatchallJerseyException("No status for this job found. Bad.");
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}
		
		// replace the job status
		String clauseDelete = String.format("<%s> <%s> ?old_status.", resourceUriStr, JOB_STATUS_PROP);
        String clauseInsert = String.format("<%s> <%s> \"%s\".", resourceUriStr, JOB_STATUS_PROP, newStatus);
        new SparqlUpdate.Builder()
        	.graph(resourceUriStr)
        	.delete(clauseDelete)
        	.insert(clauseInsert)
        	.where(clauseDelete)
        	.endpoint(ENDPOINT_STATEMENTS)
        	.build()
	        .execute()
        	;
        	
        // return the new status live
		return getJobStatus(uriInfo, id);
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("/{id}/log")
	public Response addLongEntry(@Context UriInfo uriInfo,
			@PathParam("id") String id, 
			File logRdfStr)
			throws CatchallJerseyException {
		
		String resourceUriStr = uriInfo.getRequestUri().toString().replaceAll("/log$", "");
		
		Grafeo g = new GrafeoImpl();
		long timestamp = new Date().getTime();
		GLiteral timestampLiteral = g.date(timestamp);
		log.info(new Long(timestamp).toString());
		try {
			g = new GrafeoImpl(logRdfStr);
			// TODO validate if this is a valid log entry
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}
		GResource blank = g.findTopBlank();
		if (null == blank) throw new CatchallJerseyException("Must contain blank node");
		String logEntryUriStr = uriInfo.getRequestUri().toString() + "/" + timestamp;
		blank.rename(logEntryUriStr);
		g.addTriple(resourceUriStr, "rdf:type", NS_DM2E_LOGGING + "LogEntry");
		g.addTriple(resourceUriStr, JOB_LOGENTRY_PROP, logEntryUriStr);
		g.addTriple(logEntryUriStr, NS_DM2E_LOGGING + "timestamp" , timestampLiteral);
		g.writeToEndpoint(ENDPOINT_STATEMENTS, resourceUriStr);
        	
		return getResponse(g);
//		return getJobStatus(uriInfo, id);
	}
//	
	@GET
	@Path("/{id}/log")
	public Response listLogEntries() throws CatchallJerseyException {
		
		
		String resourceUriStr = uriInfo.getRequestUri().toString().replaceAll("/log$", "");
		SparqlConstruct sparqlConstruct;
		Grafeo g;
		try {
			sparqlConstruct = new SparqlConstruct.Builder()
					.endpoint(ENDPOINT)
					.graph(resourceUriStr)
					.construct("?s ?p ?o")
					.where("?s a <" + NS_DM2E_LOGGING + "LogEntry> . ?s ?p ?o .")
//					.where("?s a <" + NS_DM2E + "LogEntry>")
					.build();
			g = sparqlConstruct.execute();
			
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}
//		throw new CatchallJerseyException("TODO");
		return getResponse(g);
	}

}
