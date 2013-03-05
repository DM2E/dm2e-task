/**
 * 
 */
package eu.dm2e.task.model;

import java.util.LinkedHashMap;

import org.jongo.marshall.jackson.id.Id;

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

	public String getJobURL() {
		return jobURL;
	}

	public void setJobURL(String jobURL) {
		this.jobURL = jobURL;
	}

	public LinkedHashMap<String, Object> getJobConfig() {
		return jobConfig;
	}

	public void setJobConfig(LinkedHashMap<String, Object> jobConfig) {
		this.jobConfig = jobConfig;
	}

	public String getJobQueue() {
		return jobQueue;
	}

	public void setJobQueue(String jobQueue) {
		this.jobQueue = jobQueue;
	}

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Job [" + getJobQueue() + ":" + getJobID() + "] " + getStatus();
	}

}
