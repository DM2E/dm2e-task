package eu.dm2e.task.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.rabbitmq.client.Channel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.dm2e.task.util.CatchallJerseyException;
import eu.dm2e.task.util.RabbitConnection;
import eu.dm2e.ws.Config;
import eu.dm2e.ws.NS;
import eu.dm2e.ws.grafeo.Grafeo;
import eu.dm2e.ws.grafeo.jena.GrafeoImpl;
import eu.dm2e.ws.services.data.AbstractRDFService;

@Path("/service/xslt")
public class XsltService extends AbstractRDFService {
	
	private Logger log = Logger.getLogger(getClass().getName());
	private static final String SERVICE_RABBIT_QUEUE = Config.getString("dm2e.service.xslt.worker_queue");
	private static final String SERVICE_DESCRIPTION_RESOURCE = Config.getString("dm2e.service.xslt.description_resource");
	
//	private static final String NS_XSLT_SERVICE = Config.getString("dm2e.service.xslt.namespace");
	private static final String PROPERTY_HAS_WEB_SERVICE_CONFIG = NS.DM2E + "hasWebServiceConfig";
	
	// TODO shouldnot be hardwired
	private static final String URI_JOB_SERVICE = Config.getString("dm2e.service.job.base_uri");
	private static final String URI_CONFIG_SERVICE = Config.getString("dm2e.service.config.base_uri");
	
	@Override
	public String getServiceDescriptionResourceName() {
		return SERVICE_DESCRIPTION_RESOURCE;
	}
	
	/**
	 * Describes this service.
	 */
	@GET
	public Response getDescription(@Context UriInfo uriInfo) throws CatchallJerseyException {
        Grafeo g;
		try {
			g = getServiceDescriptionGrafeo();
		} catch (Exception e) {
			log.severe(e.toString());
			return throwServiceError(e);
		}
        return getResponse(g);
	}
	
	@PUT
	@Consumes(MediaType.TEXT_PLAIN)
	public Response putTransformation(String configURI) throws CatchallJerseyException {
		
		WebResource jobResource = Client.create().resource(URI_JOB_SERVICE);	
		
		try{
			validateServiceInput(configURI);
		}
		catch(Exception e) {
			return throwServiceError(e);
		}
		
		// create the job
		log.info("Creating the job");
		GrafeoImpl g = new GrafeoImpl();
		Model m = g.getModel();
		Resource emptyResource = m.createResource();
		emptyResource.addProperty(m.createProperty(NS.RDF + "type"), m.createResource(NS.DM2E + "Job"));
		emptyResource.addLiteral(m.createProperty(NS.DM2E + "status"), "NOT_STARTED");
		emptyResource.addProperty(m.createProperty(NS.DM2E + "hasWebSerice"), m.createResource(uriInfo.getRequestUri().toASCIIString()));
		emptyResource.addProperty(m.createProperty(PROPERTY_HAS_WEB_SERVICE_CONFIG), m.createResource(configURI));
		ClientResponse jobResponse = jobResource
			.accept("text/turtle")
			.post(ClientResponse.class, g.getNTriples());
		URI jobUri = jobResponse.getLocation();
		g.findTopBlank().rename(jobUri.toString());
		
		
		// post the job to the worker
		log.info("Posting the job to the worker queue");
		byte[] messageBytes = jobUri.toString().getBytes();
		Channel channel;
		log.info(SERVICE_RABBIT_QUEUE);
		try {
			channel = RabbitConnection.getChannel();
			channel.basicPublish("", SERVICE_RABBIT_QUEUE, null, messageBytes);
			channel.close();
		} catch (Exception e) {
			return throwServiceError(e);
		}
		
		// return location of the job
		return Response.created(jobUri).entity(getResponseEntity(g)).build();
	}
	


	@POST
	@Consumes(MediaType.WILDCARD)
	public Response postTransformation(@Context UriInfo uriInfo, File body) throws CatchallJerseyException {
		
		WebResource configResource = Client.create().resource(URI_CONFIG_SERVICE);
		log.severe(URI_CONFIG_SERVICE);
		
		// post the config
		log.info("Persisting config.");
		ClientResponse configResponse = configResource
			.accept("text/turtle")
			.post(ClientResponse.class, body);
		URI configUri = configResponse.getLocation();
		
		return putTransformation(configUri.toString());
	}

}
	