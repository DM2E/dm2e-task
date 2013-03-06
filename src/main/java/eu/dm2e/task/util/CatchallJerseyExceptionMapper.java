package eu.dm2e.task.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

@Provider
public class CatchallJerseyExceptionMapper implements
		ExceptionMapper<CatchallJerseyException> {

	private static final Logger logger = Logger
			.getLogger(CatchallJerseyExceptionMapper.class);

//	@Produces(MediaType.TEXT_PLAIN)
	public Response toResponse(CatchallJerseyException e) {
		logger.warn("hiya");
		e.getWrappedException().printStackTrace();
		return Response.status(Response.Status.BAD_REQUEST)
				.header("Content-Type", "text/plain")
				.entity( e.getWrappedException().getClass().getName()
						+ "\n"
						+ e.getWrappedException().toString() 
						+ "\n"
						+ e.getMessage()
						+ "\n"
					).build();
	}

}
