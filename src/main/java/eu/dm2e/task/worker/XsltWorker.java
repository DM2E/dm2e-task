package eu.dm2e.task.worker;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

import eu.dm2e.task.util.JobLogger;
import eu.dm2e.ws.Config;
import eu.dm2e.ws.DM2E_MediaType;
import eu.dm2e.ws.NS;
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

	// Constants from the configuration
	private static final String SERVICE_RABBIT_QUEUE = Config
		.getString("dm2e.service.xslt.worker_queue");
	private static final String SERVICE_URI = Config.getString("dm2e.service.xslt.base_uri");

	private static final String NS_XSLT_SERVICE = Config.getString("dm2e.service.xslt.namespace");
	private static final String PROPERTY_XML_SOURCE = NS_XSLT_SERVICE + "xmlSource";
	private static final String PROPERTY_XSLT_SOURCE = NS_XSLT_SERVICE + "xsltSource";

	private static final String FILE_SERVICE_URI = Config.getString("dm2e.service.file.base_uri");

	// The HTTP REST client
	private Client client = new Client();

	// Logging
	Logger log = Logger.getLogger(getClass().getName());

	@Override
	public String getRabbitQueueName() {
		return SERVICE_RABBIT_QUEUE;
	}

	@Override
	public String getServiceUri() {
		return SERVICE_URI;
	}

	@Override
	public Client getClient() {
		return client;
	}

	@Override
	public void handleMessage(String jobUri)
			throws InterruptedException {

		// WebResource jobResource = client.resource(jobUri);
		WebResource jobStatusResource = getClient().resource(jobUri + "/status");
		WebResource fileResource = getClient().resource(FILE_SERVICE_URI);

		// create a logger that logs both to console our job resource
		JobLogger log = new JobLogger(this, jobUri);

		// Generate Grafeo for the current job
		GrafeoImpl jobModel = new GrafeoImpl();
		jobModel.load(jobUri);

		log.fine("Starting to handle XSLT transformation job");

		// Find the configuration for this worker run
		GrafeoImpl configModel = new GrafeoImpl();
		String configUri;
		try {
			NodeIterator iter = jobModel.getModel().listObjectsOfProperty(
					jobModel.getModel().createProperty(NS.DM2E + "hasWebServiceConfig"));
			configUri = iter.next().toString();
		} catch (Exception e) {
			log.severe("Job is missing hasWebServiceConfig: " + e.toString());
			jobStatusResource.put("FAILED");
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
			iter = configModel.getModel().listObjectsOfProperty(
					jobModel.getModel().createProperty(PROPERTY_XML_SOURCE));
			xmlUrl = iter.next().toString();
			log.info(xmlUrl);
			iter = configModel.getModel().listObjectsOfProperty(
					jobModel.getModel().createProperty(PROPERTY_XSLT_SOURCE));
			xsltUrl = iter.next().toString();
		} catch (Exception e) {
			log.severe("Job is missing either xmlSource or xsltSource" + e.toString());
			jobStatusResource.put("FAILED");
			return;
		}

		log.info("XML URL: " + xmlUrl);
		log.info("XSL URL: " + xsltUrl);

		// @TODO move this to the ABC
		// Make sure that the resources are available
		ArrayList<WebResource> unreadyResources = new ArrayList<WebResource>();
		unreadyResources.add(client.resource(xsltUrl));
		unreadyResources.add(client.resource(xmlUrl));
		boolean allResourcesReady = false;
		log.fine("Waiting for all resources to become ready.");
		while (!allResourcesReady) {
			log.fine("Waiting for all resources to become ready.");
			for (WebResource r : unreadyResources) {
				if (!r.getURI().getScheme().matches("^(h|f)ttps?")) {
					log.severe("Not an http/ftp link: " + r.getURI().getScheme());
					log.severe("Not an http/ftp link: " + r.getURI().getScheme());
					jobStatusResource.put("FAILED");
					return;
				}
				log.fine("Testing HEAD on " + r.getURI());
				// TODO BUG here
				ClientResponse resp = r.head();
				if (resp.getStatus() == 200) {
					unreadyResources.remove(r);
					log.fine("Resource " + r.getURI() + " is ready now.");
				} else {
					log.severe("Resource " + r.getURI() + " not available. Will croak for now.");
					log.severe("Resource " + r.getURI() + " not available. Will croak for now.");
					jobStatusResource.put("FAILED");
					return;
				}
			}
			allResourcesReady = true;
		}

		log.info("Starting transformation");

		// update job status
		jobStatusResource.put("STARTED");
		TransformerFactory tFactory = TransformerFactory.newInstance();
		StringWriter xslResultStrWriter = new StringWriter();
		try {

			StreamSource xmlSource = new StreamSource(new URL(xmlUrl).openStream());
			StreamSource xslSource = new StreamSource(new URL(xsltUrl).openStream());
			Transformer transformer = tFactory.newTransformer(xslSource);

			StreamResult xslResult = new StreamResult(xslResultStrWriter);

			transformer.transform(xmlSource, xslResult);

		} catch (Exception e) {
			log.severe("Error during XSLT transformation: " + e);
		}

		// TODO do something with outstream
		log.info("Writing result to file service.");
		String xslResultStr = xslResultStrWriter.toString();
		if (xslResultStr.length() > 0) {
			FormDataMultiPart form = new FormDataMultiPart();
			
			// add file part
			MediaType xml_type = MediaType.valueOf(MediaType.APPLICATION_XML);
			FormDataBodyPart fileFDBP = new FormDataBodyPart("file", xslResultStr, xml_type);
			form.bodyPart(fileFDBP);

			// add metadata part
			// FormDataBodyPart metaFDBP = new FormDataBodyPart("meta",
			// xslResult, xml_type);
			GrafeoImpl metaGrafeo = new GrafeoImpl();
			Model metaModel = metaGrafeo.getModel();
			Resource blank = metaModel.createResource();
			metaModel.add(blank, metaModel.createProperty(NS.DM2E + "generatedBy"), metaModel.createResource(jobUri));
			String metaNTriples = metaGrafeo.getNTriples().replaceAll("_[^\\s]+", "[]");
//			String metaNTriples = metaGrafeo.getNTriples();	
			MediaType n3_type = MediaType.valueOf(DM2E_MediaType.TEXT_RDF_N3);
			FormDataBodyPart metaFDBP = new FormDataBodyPart("meta", metaNTriples, n3_type);
			form.bodyPart(metaFDBP);
			

			log.info(fileResource.toString());
			log.info(fileResource.getURI().toString());
			log.info(form.toString());
			Builder builder = fileResource
					.type(MediaType.MULTIPART_FORM_DATA)
					.accept(DM2E_MediaType.TEXT_TURTLE)
					.entity(form);
			ClientResponse resp = builder.post(ClientResponse.class);
			if (resp.getStatus() >= 400) {
				log.severe("File storage failed: " + resp.getEntity(String.class));
			}
			else {
				log.info("File stored at: " + resp.getLocation());
				// store the file in the job
				jobModel.addTriple(jobUri, NS.DM2E + "resultResource", resp.getLocation().toString());
				
			}
			// write out jobModel
			jobModel.writeToEndpoint(NS.ENDPOINT_STATEMENTS, jobUri);
		}

		// Update job status
		jobStatusResource.put("FINISHED");
		log.info("XSLT Transformation complete.");
	}
}