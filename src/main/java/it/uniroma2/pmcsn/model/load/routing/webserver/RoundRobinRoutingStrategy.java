package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Implementation of WebServerRoutingStrategy selecting Web Servers in a round-robin (cyclic) fashion.
 */
public class RoundRobinRoutingStrategy implements WebServerRoutingStrategy {
    private int index = 0;

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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
