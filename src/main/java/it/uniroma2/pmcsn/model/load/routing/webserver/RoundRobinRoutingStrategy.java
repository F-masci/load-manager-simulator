package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Routing strategy that selects web servers in a circular order.
 */
public class RoundRobinRoutingStrategy implements WebServerRoutingStrategy {
    private int index = 0;

    /**
     * Selects the next web server in the circular sequence.
     *
     * @param job the job to route
     * @param cluster the web server cluster
     * @return the selected web server
     * @throws IllegalStateException if no active servers are found
     */
    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
        List<WebServer> active = cluster.getActiveServers();
        if (active.isEmpty()) {
            throw new IllegalStateException("No active web servers in cluster.");
        }
        WebServer server = active.get(index % active.size());
        index = (index + 1) % active.size();
        return server;
    }

    /**
     * Gets the current round-robin index.
     *
     * @return the current index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the round-robin index.
     *
     * @param index the new index
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
