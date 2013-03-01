/**
 * 
 */
package eu.dm2e.task.model;

/**
 * @author kb
 *
 */
public class Job {

	private String _id;
	
	private String status = "NOT_STARTED";
	
	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
