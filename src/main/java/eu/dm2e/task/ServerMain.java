package eu.dm2e.task;


import com.sun.jersey.api.container.grizzly2.GrizzlyWebContainerFactory;

import eu.dm2e.ws.Config;

import org.glassfish.grizzly.http.server.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;


public class ServerMain {

	static URI getBaseURI() {
		return UriBuilder.fromUri(
				Config.config.getString("dm2e.task.base_uri", "http://localhost:9110/")).build();
	}
    
    protected static HttpServer startServer() throws IOException {
        final Map<String, String> initParams = new HashMap<String, String>();

		initParams.put("com.sun.jersey.config.property.packages",
				"eu.dm2e.task.services"
				+";"
				+ "eu.dm2e.task.util");

        System.out.println("Starting grizzly2...");
        return GrizzlyWebContainerFactory.create(getBaseURI(), initParams);
    }
    
    public static void main(String[] args) throws IOException {
    	
		if (null == Config.config) {
			System.err.println("No config was found. Create 'config.xml'.");
			System.exit(1);
		}
    	
        // Grizzly 2 initialization
        HttpServer httpServer = startServer();
        System.out.println(String.format("OmNom started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...",
                getBaseURI()));
        System.in.read();
        httpServer.stop();
        System.exit(0);
    }    
}
