package eu.dm2e.task.util;

public class CatchallJerseyException extends Exception {

	private String msg;
	private static final long serialVersionUID = 1L;
//	
//	public CatchallJerseyException(String msg) {
//	}

	public CatchallJerseyException(Exception e) {
		this.msg = e.toString();
	}

	public CatchallJerseyException(String string) {
		this.msg = string;
	}

}
