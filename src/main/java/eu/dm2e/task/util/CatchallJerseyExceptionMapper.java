package eu.dm2e.task.util;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

@Provider
public class CatchallJerseyExceptionMapper implements
		ExceptionMapper<CatchallJerseyException> {

	private static final Logger logger = Logger
			.getLogger(CatchallJerseyExceptionMapper.class);
	
	// http://stackoverflow.com/questions/3227360/jax-rs-jersey-custom-exception-with-xml-or-json
    @Context
    private HttpHeaders headers;

	public Response toResponse( CatchallJerseyException e) {
//		String body = e.getWrappedException().toString();
		String body = toResponse_TEXT_PLAIN(e);
		String mediaType = "text/plain";
		List<MediaType> acceptableList = headers.getAcceptableMediaTypes();
		for (int i=0; i < acceptableList.size(); i++) {
			String thisType = acceptableList.get(i).toString();
			if (thisType.equals(MediaType.APPLICATION_JSON)) {
				logger.warn("OI");
				body = toResponse_APPLICATION_JSON(e);
				mediaType = MediaType.APPLICATION_JSON;
				break;
			}
		}
		return Response
				.status(e.getHttpStatus())
				.header("Content-Type", mediaType)
				.entity(body).build();
	}
	
	public String toResponse_TEXT_PLAIN(CatchallJerseyException e) {
		 return e.getWrappedException().getClass().getName()
				 + "\n"
				 + e.getMessage()
				 + "\n"
				 + e.getWrappedException().toString() 
//				 + "\n"
//				 + ExceptionUtils.getStackTrace(e)
				 ;
	}
	public String toResponse_APPLICATION_JSON(CatchallJerseyException e) {
		BasicDBObject doc = new BasicDBObject()
			.append("name", e.getWrappedException().getClass().getName())
			.append("message", e.getMessage())
			.append("wrapped_message", e.getWrappedException().toString())
//			.append("stacktrace", ExceptionUtils.getStackTrace(e.getWrappedException()))
			;
		return doc.toString();
	}

}
