package eu.dm2e.task.worker;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.dm2e.task.model.NS;
import eu.dm2e.ws.Config;
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

	// Constants from the configuration
	private static final String SERVICE_DESCRIPTION_RESOURCE = 
			Config.getString("dm2e.service.xslt.description_resource");
	private static final String SERVICE_RABBIT_QUEUE = 
			Config.getString("dm2e.service.xslt.worker_queue");
	private static final Object SERVICE_URI = Config.getString("dm2e.service.xslt.base_uri");
	private static final String NS_XSLT_SERVICE = Config.getString("dm2e.service.xslt.namespace");
	private static final String PROPERTY_XML_SOURCE = NS_XSLT_SERVICE + "xmlSource";
	private static final String PROPERTY_XSLT_SOURCE = NS_XSLT_SERVICE + "xsltSource";
	
	// The HTTP REST client
	private Client client = new Client();
	
	// Logging
	Logger log = Logger.getLogger(getClass().getName());
	
	// the worker queue
	@Override
	String getRabbitQueueName() { return SERVICE_RABBIT_QUEUE; }

	@Override
	public void handleMessage(String jobUri) throws InterruptedException {
		final HashMap<String, WebResource> jobResources = new HashMap<String, WebResource>();
		
		jobResources.put("job", client.resource(jobUri));
		jobResources.put("jobStatus", client.resource(jobUri + "/status"));
		jobResources.put("jobLog", client.resource(jobUri + "/log"));
		
		/**
		 * @todo this works but should be a class in it's own right ofc
		 *
		 */
		class InnerLogger {
			public void log(String msg) {
				log.info(msg);
				jobResources.get("jobLog")
					.header("Content-Type", "text/plain")
					.header("Referer", SERVICE_URI)
					.post(msg);
			}
		}
		InnerLogger innerlog = new InnerLogger();
		
		
		// Generate Grafeo for the current job
		GrafeoImpl jobModel = new GrafeoImpl();
		jobModel.load(jobUri);
		
		innerlog.log("TRACE: Starting to handle XSLT transformation job");
		
		// Find the configuration for this worker run
		GrafeoImpl configModel = new GrafeoImpl();
		String configUri;
		try {
			NodeIterator iter = jobModel.getModel().listObjectsOfProperty(
					jobModel.getModel().createProperty(NS.DM2E + "hasWebServiceConfig"));
			configUri = iter.next().toString();
		} catch(Exception e) {
			innerlog.log("FATAL: Job is missing hasWebServiceConfig");
			jobResources.get("status").put("FAILED");
			return;
		}
		
		// Populate the configuration model
        log.info("Config URL: " + configUri);
        configModel.load(configUri);
        
        // Get the input parameters
        String xmlUrl = null;
        String xsltUrl = null;
		try {
			NodeIterator iter;
			iter = configModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(PROPERTY_XML_SOURCE));
			xmlUrl = iter.next().toString();
			iter = configModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(PROPERTY_XSLT_SOURCE));
			xsltUrl = iter.next().toString();
		} catch(Exception e) {
			innerlog.log("FATAL: Job is missing either xmlSource or xsltSource");
			jobResources.get("jobStatus").put("FAILED");
			return;
		}
//		xmlUrl = jobModel.get(jobUri).get(PROPERTY_XML_SOURCE).resource().getUri();
//		xsltUrl = jobModel.get(jobUri).get(PROPERTY_XSLT_SOURCE).resource().getUri();

        log.info("XML URL: " + xmlUrl);
        log.info("XSL URL: " + xsltUrl);
        
        // Make sure that the resources are available
        ArrayList<WebResource> unreadyResources = new ArrayList<WebResource>();
        unreadyResources.add(client.resource(xsltUrl));
        unreadyResources.add(client.resource(xmlUrl));
        boolean allResourcesReady = false;
        innerlog.log("TRACE: Waiting for all resources to become ready.");
        while (! allResourcesReady){
	        innerlog.log("TRACE: Waiting for all resources to become ready.");
        	for (WebResource r : unreadyResources) {
        		if (! r.getURI().getScheme().matches("^(h|f)ttps?")) {
        			log.severe("Not an http/ftp link: " + r.getURI().getScheme());
        			innerlog.log("FATAL: Not an http/ftp link: " + r.getURI().getScheme());
        			jobResources.get("jobStatus").put("FAILED");
        			return;
        		}
        		innerlog.log("TRACE: Testing HEAD on " + r.getURI());
        		// TODO BUG here
        		ClientResponse resp = r.head();
        		if (resp.getStatus() == 200) {
        			unreadyResources.remove(r);
        			innerlog.log("TRACE: Resource " + r.getURI()  + " is ready now.");
        		}
        		else{
        			log.severe("Resource "+r.getURI()+" not available. Will croak for now.");
        			innerlog.log("FATAL: Resource "+r.getURI()+" not available. Will croak for now.");
        			jobResources.get("jobStatus").put("FAILED");
        			return;
        		}
        	}
        	allResourcesReady = true;
        }
		
		innerlog.log("INFO: Starting transformation");
		
        // update job status
		jobResources.get("jobStatus").put("STARTED");
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {

            StreamSource xmlSource = new StreamSource(new URL(xmlUrl).openStream());
            StreamSource xslSource = new StreamSource(new URL(xsltUrl).openStream());
            Transformer transformer = tFactory.newTransformer(xslSource);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            StreamResult xslResult = new StreamResult(outStream);

            transformer.transform(xmlSource, xslResult);
            log.info(xslResult.toString());
            // TODO do something with outstream
        } catch (Exception e) {
            innerlog.log("FATAL: Error during XSLT transformation: " + e);
        }
        
        // Update job status
		jobResources.get("jobStatus").put("FINISHED");
		innerlog.log("INFO: XSLT Transformation complete.");
	}
}