package eu.dm2e.task.services;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
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

import eu.dm2e.task.model.NS;
import eu.dm2e.task.util.CatchallJerseyException;
import eu.dm2e.task.util.RabbitConnection;
import eu.dm2e.ws.grafeo.GResource;
import eu.dm2e.ws.grafeo.Grafeo;
import eu.dm2e.ws.grafeo.jena.GrafeoImpl;
import eu.dm2e.ws.services.data.AbstractRDFService;

@Path("/service/xslt")
public class XsltService extends AbstractRDFService {
	
	private Logger log = Logger.getLogger(getClass().getName());
	private static final String SERVICE_DESCRIPTION_RESOURCE = "/xslt-service-description.ttl";
	private static final String SERVICE_RABBIT_QUEUE = "eu.dm2e.task.worker.XsltWorker";
	
	private static final String NS_XSLT_SERVICE = "http://omnom.dm2e.eu/service/xslt#";
	private static final String PROPERTY_XML_SOURCE = NS_XSLT_SERVICE + "xmlSource";
	private static final String PROPERTY_XSLT_SOURCE = NS_XSLT_SERVICE + "xsltSource";
	private static final String PROPERTY_HAS_WEB_SERVICE_CONFIG = NS.DM2E + "hasWebServiceConfig";
	
	// TODO shouldnot be hardwired
	private static final String URI_JOB_SERVICE = "http://localhost:9110/job";
	private static final String URI_CONFIG_SERVICE = "http://localhost:9998/data/configurations";
	
	/**
	 * Describes this service.
	 */
	@GET
	public Response getDescription(@Context UriInfo uriInfo) throws CatchallJerseyException {
        Grafeo g = new GrafeoImpl();
        
        InputStream sampleDataStream = this.getClass().getResourceAsStream(SERVICE_DESCRIPTION_RESOURCE);
        if (null == sampleDataStream) {
            log.severe("Couldn't open " + SERVICE_DESCRIPTION_RESOURCE);
            throw new CatchallJerseyException("Couldn't open " + SERVICE_DESCRIPTION_RESOURCE);
        }
        ((GrafeoImpl) g).getModel().read(sampleDataStream, null, "TURTLE");
        GResource blank = g.findTopBlank();
        String uri = uriInfo.getRequestUri().toString();
        if (blank!=null) blank.rename(uri);
        return getResponse(g);
	}
	
	@PUT
	@Consumes(MediaType.TEXT_PLAIN)
	public Response putTransformation(String configURI) throws CatchallJerseyException {
		
		WebResource jobResource = Client.create().resource(URI_JOB_SERVICE);	
		
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
		try {
			channel = RabbitConnection.getChannel();
			channel.basicPublish("", SERVICE_RABBIT_QUEUE, null, messageBytes);
			channel.close();
		} catch (Exception e) {
			throw new CatchallJerseyException(e);
		}
		
		
		// return location of the job
		return Response.created(jobUri).entity(getResponseEntity(g)).build();
	}
	


	@POST
	@Consumes(MediaType.WILDCARD)
	public Response postTransformation(@Context UriInfo uriInfo, File body) throws CatchallJerseyException {
		
		WebResource configResource = Client.create().resource(URI_CONFIG_SERVICE);
		
		// post the config
		log.info("Persisting config.");
		ClientResponse configResponse = configResource
			.accept("text/turtle")
			.post(ClientResponse.class, body);
		URI configUri = configResponse.getLocation();
		
		return putTransformation(configUri.toString());
	}

}
	