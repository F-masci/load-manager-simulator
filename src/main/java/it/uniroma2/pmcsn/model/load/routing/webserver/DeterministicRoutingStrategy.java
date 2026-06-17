package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Routing strategy that selects a web server based on a fixed index.
 */
public class DeterministicRoutingStrategy implements WebServerRoutingStrategy {
    private int targetServer;

    /**
     * Constructs a deterministic strategy targeting the first server by default.
     */
    public DeterministicRoutingStrategy() {
        this(1);
    }

    /**
     * Constructs a deterministic strategy targeting a specific server index.
     *
     * @param targetServer the index of the target server
     */
    public DeterministicRoutingStrategy(int targetServer) {
        this.targetServer = targetServer;
    }

    /**
     * Selects the web server at the fixed target index.
     *
     * @param job the job to route
     * @param cluster the web server cluster
     * @return the selected web server
     * @throws IllegalStateException if no active servers are found
     */
    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
        WebServer server = cluster.getActiveServers().get(targetServer-1);
        if (server == null) {
            throw new IllegalStateException("No active web servers in cluster.");
        }
        return server;
    }

    /**
     * Gets the current target server index.
     *
     * @return the target server index
     */
    public int getTargetServer() {
        return targetServer;
    }

    /**
     * Sets a new target server index.
     *
     * @param targetServer the new target server index
     */
    public void setTargetServer(int targetServer) {
        this.targetServer = targetServer;
    }
}
