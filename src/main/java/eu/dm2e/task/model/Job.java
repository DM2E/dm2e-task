/**
 * 
 */
package eu.dm2e.task.model;

import org.jongo.marshall.jackson.id.Id;

/**
 * @author kb
 *
 */
public class Job {

	@Id
	private String jobID;

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	private String status = "NOT_STARTED";

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Job ["+getJobID()+"] " + getStatus();
	}
	
}
