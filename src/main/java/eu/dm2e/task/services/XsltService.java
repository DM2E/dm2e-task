package eu.dm2e.task.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.rabbitmq.client.Channel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
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
	
	@POST
	@Consumes(MediaType.WILDCARD)
	public Response postTransformation(@Context UriInfo uriInfo, File body) throws CatchallJerseyException {
		
		long timestamp = new Date().getTime();
		
		WebResource jobResource = Client.create().resource(URI_JOB_SERVICE);
		WebResource configResource = Client.create().resource(URI_CONFIG_SERVICE);
		
		// post the config
		log.info("Persisting config.");
		ClientResponse configResponse = configResource
			.accept("text/turtle")
			.post(ClientResponse.class, body);
		URI configUri = configResponse.getLocation();
		
		// create the job
		log.info("Creating the job");
		GrafeoImpl g = new GrafeoImpl();
		Model m = g.getModel();
		Resource emptyResource = m.createResource();
		emptyResource.addProperty(m.createProperty(NS.RDF + "type"), m.createResource(NS.DM2E + "Job"));
		emptyResource.addLiteral(m.createProperty(NS.DM2E + "status"), "NOT_STARTED");
		emptyResource.addProperty(m.createProperty(NS.DM2E + "hasWebSerice"), m.createResource(uriInfo.getRequestUri().toASCIIString()));
		emptyResource.addProperty(m.createProperty(PROPERTY_HAS_WEB_SERVICE_CONFIG), m.createResource(configUri.toString()));
		ClientResponse jobResponse = jobResource
			.accept("text/turtle")
			.post(ClientResponse.class, g.getNTriples());
		URI jobUri = jobResponse.getLocation();
		
		
		// TODO post the job to the worker
		log.info("Posting the job to the worker queue");
		byte[] messageBytes = jobUri.toString().getBytes();
		Channel channel;
		try {
			channel = RabbitConnection.getChannel();
			channel.basicPublish("", SERVICE_RABBIT_QUEUE, null, messageBytes);
			channel.close();
		} catch (IOException e) {
			throw new CatchallJerseyException(e);
		}
		
		
		// return location of the job
		return Response.created(jobUri).entity(getResponseEntity(g)).build();
		
//		g.lo
		
//		addProperty(g.resource("rdf:type").getResource(), g.resource("dm2e:Job").getResource());
//		g.addTriple(emptyResource, "rdf:blech", "foo");
//		return getResponse(g);
		
//        String xslUrl = null;
//        String xmlUrl = null;
//        log.info("Config URL: " + body);
//        Grafeo g = new GrafeoImpl(body);
//        log.info("Config content: " + g.getNTriples());
//        xmlUrl = g.get(body).get(PROPERTY_XML_SOURCE).resource().getUri();
//        xslUrl = g.get(body).get(PROPERTY_XSLT_SOURCE).resource().getUri();
//
//        log.info("XML URL: " + xmlUrl);
//        log.info("XSL URL: " + xslUrl);
//
//        if (null == xslUrl || null == xmlUrl) {
//        	throw new CatchallJerseyException("Error in configuration");
//        }
//        Grafeo jobG = new GrafeoImpl();
//        TransformerFactory tFactory = TransformerFactory.newInstance();
//        try {
//
//            StreamSource xmlSource = new StreamSource(new URL(xmlUrl).openStream());
//            StreamSource xslSource = new StreamSource(new URL(xslUrl).openStream());
//            Transformer transformer = tFactory.newTransformer(xslSource);
//
//            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
//            StreamResult xslResult = new StreamResult(outStream);
//
//            transformer.transform(xmlSource, xslResult);
//            log.info("Output to write: " + outStream.toString());
//            return Response.ok(outStream.toString()).build();
//        } catch (Exception e) {
//            log.severe("Error during XSLT transformation: " + e);
//            throw new CatchallJerseyException(e);
//        }
//		return getResponse(g);
	}
	

}
