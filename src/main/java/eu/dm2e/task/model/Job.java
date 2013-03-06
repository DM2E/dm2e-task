/**
 * 
 */
package eu.dm2e.task.model;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.bson.BSONObject;
import org.bson.NewBSONDecoder;
import org.bson.types.ObjectId;
import org.jongo.marshall.jackson.id.Id;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author kb
 * 
 */
public class Job {

	@Id
	private String jobID;
	private JobStatus status = JobStatus.NOT_STARTED;
	private String jobQueue;
	private String jobURL;
	private LinkedHashMap<String, Object> jobConfig;

	public String getJobID() { return jobID; }
	public void setJobID(String jobID) { this.jobID = jobID; }

	public JobStatus getStatus() { return status; }
	public void setStatus(JobStatus status) { this.status = status; }

	public String getJobQueue() { return jobQueue; }
	public void setJobQueue(String jobQueue) { this.jobQueue = jobQueue; }
	
	public String getJobURL() { return jobURL; }
	public void setJobURL(String jobURL) { this.jobURL = jobURL; }

	public LinkedHashMap<String, Object> getJobConfig() { return jobConfig; }
	public void setJobConfig(LinkedHashMap<String, Object> jobConfig) { this.jobConfig = jobConfig; }

	@Override
	public String toString() {
		return "Job [" + getJobQueue() + ":" + getJobID() + "] " + getStatus();
	}
	
	/**
	 * Pointless constructor to make Jongo happy
	 */
	Job() { }
	
	@SuppressWarnings("unchecked")
	public static Job fromMongoDoc(DBObject doc) {
		Logger log = Logger.getLogger(Job.class.getName());
		Job job = new Job();
		if (doc.containsField("_id")) {
			log.warning(doc.toString());
			job.setJobID(doc.get("_id").toString());
		}
		if (doc.containsField("status")) {
			job.setStatus(JobStatus.valueOf((String) doc.get("status")));
		}
		if (doc.containsField("jobURL")) {
			job.setJobURL((String) doc.get("jobURL"));
		}
		if (doc.containsField("jobQueue")) {
			job.setJobQueue((String) doc.get("jobQueue"));
		}

		if (doc.containsField("jobConfig")) {
			Gson gson = new Gson();
			job.setJobConfig(gson.fromJson(doc.get("jobConfig").toString(), LinkedHashMap.class));
		}
		return job;
	}
	public DBObject toMongoDoc() {
		DBObject doc = new BasicDBObject();
		if (null != this.getJobID())
			doc.put("_id", new ObjectId(this.getJobID()));
		if (null != this.getStatus())
			doc.put("jobStatus", this.getStatus().toString());
		if (null != this.getJobQueue())
			doc.put("jobQueue", this.getJobQueue());
		if (null != this.getJobURL())
			doc.put("jobURL", this.getJobURL());
		if (null != this.getJobConfig())
			doc.put("jobConfig", this.getJobConfig());
		return doc;
	}
	
	public static Job fromJsonStr(String jsonStr) {
		Logger log = Logger.getLogger(Job.class.getName());
		log.warning(jsonStr);
		DBObject doc = (DBObject) JSON.parse(jsonStr);
		log.warning(doc.toString());
		if (doc.containsField("_id")) {
			doc.put("_id", new ObjectId((String)doc.get("_id")));
		}
		return fromMongoDoc(doc);
	}
	public String toJsonStr() {
		DBObject doc = this.toMongoDoc();
//		doc.put("_id", ((ObjectId) doc.get("_id")).toByteArray());
		doc.put("_id", this.getJobID());
		return doc.toString();
	}
	
	public boolean jsonIsValidJob(String jsonStr) {
		DBObject doc = (DBObject) JSON.parse(jsonStr);
		if (null == doc)
			return false;
		if (!doc.containsField("jobQueue"))
			return false;
		return true;
	}
}
