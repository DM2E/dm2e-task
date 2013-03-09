package eu.dm2e.task.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;

import eu.dm2e.task.model.JobStatus;
import eu.dm2e.task.model.LogLevel;
import eu.dm2e.task.model.NS;
import eu.dm2e.task.util.CatchallJerseyException;
import eu.dm2e.task.util.DM2E_MediaType;
import eu.dm2e.task.util.SparqlConstruct;
import eu.dm2e.task.util.SparqlUpdate;
import eu.dm2e.ws.grafeo.GLiteral;
import eu.dm2e.ws.grafeo.GResource;
import eu.dm2e.ws.grafeo.Grafeo;
import eu.dm2e.ws.grafeo.jena.GrafeoImpl;
import eu.dm2e.ws.services.data.AbstractRDFService;
//import java.util.ArrayList;

@Path("/job")
public class JobRdfService extends AbstractRDFService {

	private Logger log = Logger.getLogger(getClass().getName());
	//@formatter:off
	private static final String
			JOB_STATUS_PROP = NS.DM2E + "status",
			JOB_LOGENTRY_PROP = NS.DM2E + "hasLogEntry";
	//@formatter:on

	@GET
	@Path("/{resourceID}")
	@Consumes(MediaType.WILDCARD)
	public Response getJob(@PathParam("resourceID") String resourceID)
			throws CatchallJerseyException {
		// kb: need to use Jena model
		GrafeoImpl g = new GrafeoImpl();
		g.readFromEndpoint(NS.ENDPOINT, getRequestUriString());
		Model jenaModel = g.getModel();
		NodeIterator iter = jenaModel.listObjectsOfProperty(jenaModel.createProperty(NS.DM2E
				+ "status"));
		if (null == iter || !iter.hasNext()) {
			throw new CatchallJerseyException("No Job Status in this one. Not good.");
		}
		String jobStatus = iter.next().toString();
		int httpStatus;
		if (jobStatus.equals(JobStatus.NOT_STARTED.toString())) httpStatus = 202;
		else if (jobStatus.equals(JobStatus.NOT_STARTED.toString())) httpStatus = 202;
		else if (jobStatus.equals(JobStatus.FAILED.toString())) httpStatus = 409;
		else if (jobStatus.equals(JobStatus.FINISHED.toString())) httpStatus = 200;
		else httpStatus = 400;

		return Response.status(httpStatus).entity(getResponseEntity(g)).build();
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	public Response newJob(File bodyAsFile)
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
		g.addTriple(uri, "rdf:type", NS.DM2E + "Job");
		g.addTriple(uri, NS.DM2E + "status", g.literal(JobStatus.NOT_STARTED.toString()));
		g.writeToEndpoint(NS.ENDPOINT_STATEMENTS, uri);
		return Response.created(URI.create(uri)).entity(getResponseEntity(g)).build();
	}

	@PUT
	@Consumes(MediaType.WILDCARD)
	@Path("/{id}")
	public Response replaceJob(@PathParam("id") String id, String body) {
		return null;
	}

	public JobStatus getJobStatusInternal(String jobUriStr)
			throws CatchallJerseyException {
		GrafeoImpl g = new GrafeoImpl();
		g.readFromEndpoint(NS.ENDPOINT, jobUriStr);
		Model jenaModel = g.getModel();
		NodeIterator iter = jenaModel.listObjectsOfProperty(jenaModel.createProperty(NS.DM2E
				+ "status"));
		if (null == iter || !iter.hasNext()) {
			throw new CatchallJerseyException("No Job Status in this one. Not good.");
		}
		String jobStatus = iter.next().toString();
		return Enum.valueOf(JobStatus.class, jobStatus);
	}

	@GET
	@Path("/{id}/status")
	public Response getJobStatus(@PathParam("id") String id)
			throws CatchallJerseyException {
		String resourceUriStr = getRequestUriString().replaceAll("/status$", "");
		return Response.ok().entity(getJobStatusInternal(resourceUriStr).toString()).build();
	}

	@PUT
	@Path("/{id}/status")
	@Consumes(MediaType.WILDCARD)
	public Response updateJobStatus(@PathParam("id") String id, String newStatus)
			throws CatchallJerseyException {
		String resourceUriStr = getRequestUriString().replaceAll("/status$", "");

		// validate if this is a valid status
		try {
			if (null == newStatus) throw new CatchallJerseyException("No status sent.");
			Enum.valueOf(JobStatus.class, newStatus);
		} catch (Exception e) {
			throw new CatchallJerseyException("Invalid status type: " + newStatus);
		}

		// get the old status
		String oldStatus;
		try {
			oldStatus = getJobStatusInternal(resourceUriStr).toString();
			if (null == oldStatus) throw new CatchallJerseyException(
					"No status for this job found. Bad.");
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}

		// replace the job status
		String clauseDelete = String.format("<%s> <%s> ?old_status.", resourceUriStr,
				JOB_STATUS_PROP);
		String clauseInsert = String.format("<%s> <%s> \"%s\".", resourceUriStr, JOB_STATUS_PROP,
				newStatus);
		new SparqlUpdate.Builder().graph(resourceUriStr).delete(clauseDelete).insert(clauseInsert)
				.where(clauseDelete).endpoint(NS.ENDPOINT_STATEMENTS).build().execute();

		// return the new status live
		return getJobStatus(id);
	}

	//@formatter:off
	@POST
	@Path("/{id}/log")
	@Consumes({ DM2E_MediaType.APPLICATION_RDF_TRIPLES,
				DM2E_MediaType.APPLICATION_RDF_XML,
				DM2E_MediaType.TEXT_RDF_N3, 
				DM2E_MediaType.TEXT_TURTLE, })
	public Response addLogEntryAsRDF(File logRdfStr) throws CatchallJerseyException {
	//@formatter:on

		String resourceUriStr = getRequestUriString().replaceAll("/log$", "");

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
		String logEntryUriStr = getRequestUriString() + "/log/" + timestamp;
		g.addTriple(resourceUriStr, JOB_LOGENTRY_PROP, logEntryUriStr);
		g.addTriple(logEntryUriStr, "rdf:type", NS.DM2ELOG + "LogEntry");
		g.addTriple(logEntryUriStr, NS.DM2ELOG + "timestamp", timestampLiteral);
		g.writeToEndpoint(NS.ENDPOINT_STATEMENTS, resourceUriStr);
		
		blank.rename(logEntryUriStr);

		return getResponse(g);
	}

	@POST
	@Path("/{id}/log")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response addLogEntryAsText(String logString)
			throws CatchallJerseyException {

		String resourceUriStr = getRequestUriString().replaceAll("/log$", "");

		Grafeo g = new GrafeoImpl();

		// Add timestamp, create uris and type them
		log.info("Adding timestamp");
		long timestamp = new Date().getTime();
		GLiteral timestampLiteral = g.date(timestamp);
		String logEntryUriStr = getRequestUriString() + "/" + timestamp;
		g.addTriple(resourceUriStr, JOB_LOGENTRY_PROP, logEntryUriStr);
		g.addTriple(logEntryUriStr, "rdf:type", NS.DM2ELOG + "LogEntry");
		g.addTriple(logEntryUriStr, NS.DM2ELOG + "timestamp", timestampLiteral);

		// split up messages of the "DEBUG: foo bar!" variety if applicable
		log.info("Splitting up messages");
		String[] logStringParts = logString.split("\\s*:\\s*", 2);
		if (logStringParts.length == 2) {
			g.addTriple(logEntryUriStr, NS.DM2ELOG + "level", g.literal(logStringParts[0]));
			g.addTriple(logEntryUriStr, NS.DM2ELOG + "message", g.literal(logStringParts[1]));
		} else {
			g.addTriple(logEntryUriStr, NS.DM2ELOG + "message", g.literal(logString));
		}

		// if the "Referer" HTTP field is set, use that for setting the context
		log.info("Try the referer dance");
		List<String> referers = headers.getRequestHeader("Referer");
		if (null != referers) {
			g.addTriple(logEntryUriStr, NS.DM2ELOG + "context", referers.get(0));
		} 

		// write the data
		log.info("Write out log message");
		g.writeToEndpoint(NS.ENDPOINT_STATEMENTS, resourceUriStr);

		log.info("Return the result");
		return getResponse(g);
		// return getJobStatus(uriInfo, id);
	}

	//
	@GET
	@Path("/{id}/log")
	public Response listLogEntries(
			@QueryParam("level") String level
			)
			throws CatchallJerseyException {

		String resourceUriStr = getRequestUriString().replaceAll("/log$", "");
		String whereClause = "?s ?p ?o.\n ?s a <" + NS.DM2ELOG + "LogEntry>. \n";	
		GrafeoImpl g = new GrafeoImpl();
		if (null != level && null != Enum.valueOf(LogLevel.class, level))  {
			LogLevel levelRequested = Enum.valueOf(LogLevel.class, level);
			StringBuilder levelRegexSb = new StringBuilder(level);
			for (LogLevel l : LogLevel.values()) {
				if (l.ordinal() <= levelRequested.ordinal())
					continue;
				levelRegexSb.append("|");
				levelRegexSb.append(l.toString());
			}
			whereClause = String.format("\n%s ?s <%s> ?level.\n FILTER regex(?level,\"%s\")",
					whereClause,
					NS.DM2ELOG + "level",
					levelRegexSb.toString());
		}
		log.info(whereClause);
		//@formatter:off
		try { new SparqlConstruct.Builder()
				.endpoint(NS.ENDPOINT)
				.graph(resourceUriStr)
				.construct("?s ?p ?o")
				.where(whereClause)
				.build()
				.execute(g);

		} catch (Exception e) { throw new CatchallJerseyException(e); }
		//@formatter:on
		return getResponse(g);
	}

}