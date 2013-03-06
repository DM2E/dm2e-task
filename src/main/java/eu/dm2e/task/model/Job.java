/**
 * 
 */
package eu.dm2e.task.model;

import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.jongo.marshall.jackson.id.Id;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import eu.dm2e.task.util.CatchallJerseyException;

/**
 * @author kb
 * 
 */
public class Job {
	
	private static final Logger logger = Logger.getLogger(Job.class);

	@Id
	private String _id;
	private JobStatus status = JobStatus.NOT_STARTED;
	private String jobQueue;
	private String jobURL;
	private LinkedHashMap<String, Object> jobConfig;

	public String get_id() { return _id; }
	public void set_id(String jobID) { this._id = jobID; }

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
		return "Job [" + getJobQueue() + ":" + get_id() + "] " + getStatus();
	}
	
	/**
	 * Pointless constructor to make Jongo happy
	 */
	Job() { }
	
	@SuppressWarnings("unchecked")
	public static Job fromMongoDoc(DBObject doc) {
		Job job = new Job();
		if (doc.containsField("_id")) {
//			logger.warn(doc.get("_id"));
			job.set_id(doc.get("_id").toString());
		}
		if (doc.containsField("jobStatus")) {
			job.setStatus(JobStatus.valueOf((String) doc.get("jobStatus")));
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
		if (null != this.get_id())
			doc.put("_id", new ObjectId(this.get_id()));
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
		logger.warn(jsonStr);
		DBObject doc = (DBObject) JSON.parse(jsonStr);
		logger.warn(doc.toString());
		if (doc.containsField("_id")) {
			doc.put("_id", new ObjectId((String)doc.get("_id")));
		}
		logger.warn(doc.toString());
		return fromMongoDoc(doc);
	}
	public String toJsonStr() {
		DBObject doc = this.toMongoDoc();
		logger.warn(doc.toString());
		doc.put("_id", this.get_id());
		logger.warn(doc.toString());
		return doc.toString();
	}
	
	public static void validateJsonJob(String jsonStr) throws CatchallJerseyException {
		DBObject doc = (DBObject) JSON.parse(jsonStr);
		if (null == doc)
			throw new CatchallJerseyException("Empty or unparseable JSON");
		if (!doc.containsField("jobQueue"))
			throw new CatchallJerseyException("Every Job must contain a jobQueue field.");
	}
}
