package eu.dm2e.task.util;


public class CatchallJerseyException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Exception wrappedException;
	public Exception getWrappedException() { return wrappedException; }
	public void setWrappedException(Exception wrappedException) {
		this.wrappedException = wrappedException;
	}
	
	private String message = "";
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	
	public CatchallJerseyException(Exception e) {
		this.setWrappedException(e);
	}
	public CatchallJerseyException(Exception e, String str) {
		this.setWrappedException(e);
		this.setMessage(str);
	}

}
