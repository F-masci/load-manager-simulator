package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Implementation of WebServerRoutingStrategy selecting Web Servers in a deterministic fashion based on a fixed target server index.
 */
public class DeterministicRoutingStrategy implements WebServerRoutingStrategy {
    private int targetServer;

    public DeterministicRoutingStrategy() {
        this(1);
    }
    public DeterministicRoutingStrategy(int targetServer) {
        this.targetServer = targetServer;
    }

    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
        WebServer server = cluster.getActiveServers().get(targetServer-1);
        if (server == null) {
            throw new IllegalStateException("No active web servers in cluster.");
        }
        return server;
    }

    public int getTargetServer() {
        return targetServer;
    }
    public void setTargetServer(int targetServer) {
        this.targetServer = targetServer;
    }

}
