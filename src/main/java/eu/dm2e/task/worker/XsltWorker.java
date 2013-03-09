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
	private static final Object SERVICE_URI = "http://omnom.dm2e.eu/service/xslt";
	
	private static final String NS_XSLT_SERVICE = "http://omnom.dm2e.eu/service/xslt#";
	private static final String PROPERTY_XML_SOURCE = NS_XSLT_SERVICE + "xmlSource";
	private static final String PROPERTY_XSLT_SOURCE = NS_XSLT_SERVICE + "xsltSource";
	
	private Client client = new Client();
	
	Logger log = Logger.getLogger(getClass().getName());
	
	@Override
	String getRabbitQueueName() { return SERVICE_RABBIT_QUEUE; }

	@Override
	public void handleMessage(String jobUri) throws InterruptedException {
		final HashMap<String, WebResource> jobResources = new HashMap<String, WebResource>();
		
		jobResources.put("job", client.resource(jobUri));
		jobResources.put("status", client.resource(jobUri + "/status"));
		jobResources.put("logRDF", client.resource(jobUri + "/log"));
		jobResources.put("log", client.resource(jobUri + "/log"));
//		for (WebResource r : jobResources.values()) {
//			r.header("Referer", SERVICE_RABBIT_QUEUE);
//		}
		
		class InnerLogger {
			public void log(String msg) {
				log.info(msg);
				jobResources.get("log")
					.header("Content-Type", "text/plain")
					.header("Referer", SERVICE_URI)
					.post(msg);
			}
		}
		InnerLogger innerlog = new InnerLogger();
		
		
		GrafeoImpl jobModel = new GrafeoImpl();
		GrafeoImpl configModel = new GrafeoImpl();
		jobModel.load(jobUri);
		
		innerlog.log("TRACE: Starting to handle XSLT transformation job");
		
		String configUri;
		try {
			NodeIterator iter = jobModel.getModel().listObjectsOfProperty(jobModel.getModel().createProperty(NS.DM2E + "hasWebServiceConfig"));
			configUri = iter.next().toString();
		} catch(Exception e) {
			innerlog.log("FATAL: Job is missing hasWebServiceConfig");
			jobResources.get("status").put("FAILED");
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
			innerlog.log("FATAL: Job is missing either xmlSource or xsltSource");
			jobResources.get("status").put("FAILED");
			return;
		}
//		xmlUrl = jobModel.get(jobUri).get(PROPERTY_XML_SOURCE).resource().getUri();
//		xsltUrl = jobModel.get(jobUri).get(PROPERTY_XSLT_SOURCE).resource().getUri();

        log.info("XML URL: " + xmlUrl);
        log.info("XSL URL: " + xsltUrl);
        
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
        			jobResources.get("status").put("FAILED");
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
        			jobResources.get("status").put("FAILED");
        			return;
        		}
        	}
        	allResourcesReady = true;
        }
		
		innerlog.log("INFO: Starting transformation");
		jobResources.get("status").put("STARTED");
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {

            StreamSource xmlSource = new StreamSource(new URL(xmlUrl).openStream());
            StreamSource xslSource = new StreamSource(new URL(xsltUrl).openStream());
            Transformer transformer = tFactory.newTransformer(xslSource);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            StreamResult xslResult = new StreamResult(outStream);

            transformer.transform(xmlSource, xslResult);
            // TODO do something with outstream
        } catch (Exception e) {
            innerlog.log("FATAL: Error during XSLT transformation: " + e);
        }
		jobResources.get("status").put("FINISHED");
		innerlog.log("INFO: Done ;");
	}
}