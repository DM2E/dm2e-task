package eu.dm2e.task.worker;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.mongodb.BasicDBObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import eu.dm2e.task.model.JobStatus;
import eu.dm2e.task.model.NS;
import eu.dm2e.ws.grafeo.Grafeo;
import eu.dm2e.ws.grafeo.jena.GrafeoImpl;

/**
 * This worker transforms XML to XML using an XSLT style sheet and the Saxon XSL
 * transformation engine
 * 
 * @todo should think about where to store client and webresources (client
 *       creation is expensive and thread safe)
 *       http://stackoverflow.com/questions
 *       /8012680/jersey-client-connection-close-memory-leak-issue
 * @author kb
 * 
 */
public class XsltWorker extends AbstractWorker {

	private static final String SERVICE_DESCRIPTION_RESOURCE = "/xslt-service-description.ttl";
	private static final String SERVICE_RABBIT_QUEUE = "eu.dm2e.task.worker.XsltWorker";
	
	private static final String NS_XSLT_SERVICE = "http://omnom.dm2e.eu/service/xslt#";
	private static final String PROPERTY_XML_SOURCE = NS_XSLT_SERVICE + "xmlSource";
	private static final String PROPERTY_XSLT_SOURCE = NS_XSLT_SERVICE + "xsltSource";
	
	private Client client = new Client();
	
	Logger log = Logger.getLogger(getClass().getName());
	
	@Override
	String getRabbitQueueName() { return SERVICE_RABBIT_QUEUE; }

	@Override
	public void handleMessage(String jobUri) throws InterruptedException {
		WebResource jobRes = client.resource(jobUri);
		WebResource jobStatusRes = client.resource(jobUri + "/status");
		GrafeoImpl jobModel = new GrafeoImpl();
		GrafeoImpl configModel = new GrafeoImpl();
		jobModel.load(jobUri);
		String configUri;
		try {
			NodeIterator iter = jobModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(NS.DM2E + "hasWebServiceConfig"));
			configUri = iter.next().toString();
		} catch(Exception e) {
			log.severe("Job is missing hasWebServiceConfig");
			jobStatusRes.post("FAILED");
			return;
		}
//		= jobModel.get(jobUri).get(NS.DM2E + "hasWebServiceConfig").resource().getUri();
//		
        log.info("Config URL: " + configUri);
        configModel.load(configUri);
        
        String xmlUrl = null;
        String xsltUrl = null;
		try {
			NodeIterator iter;
			iter = configModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(PROPERTY_XML_SOURCE));
			xmlUrl = iter.next().toString();
			iter = configModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(PROPERTY_XSLT_SOURCE));
			xsltUrl = iter.next().toString();
		} catch(Exception e) {
			log.severe("Job is missing either xmlSource or xsltSource");
			jobStatusRes.post("FAILED");
			return;
		}
//		xmlUrl = jobModel.get(jobUri).get(PROPERTY_XML_SOURCE).resource().getUri();
//		xsltUrl = jobModel.get(jobUri).get(PROPERTY_XSLT_SOURCE).resource().getUri();

        log.info("XML URL: " + xmlUrl);
        log.info("XSL URL: " + xsltUrl);
		
		String response;
		System.out.println("Pretending to do work");
		response = jobStatusRes
				.accept("text/turtle")
				.type("text/plain")
				.put(String.class, "STARTED");
		Thread.sleep(10000);
		System.out.println("Done! ;-)");
		response = jobStatusRes
				.accept("text/turtle")
				.type("text/plain")
				.put(String.class, "FINISHED");
		System.out.println(response);
	}

}