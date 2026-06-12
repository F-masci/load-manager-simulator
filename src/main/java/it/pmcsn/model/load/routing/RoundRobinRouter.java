package it.pmcsn.model.load.routing;

import it.pmcsn.model.Job;
import it.pmcsn.model.server.WebServer;
import it.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Specialized router that selects Web Servers in a round-robin (cyclic) fashion.
 */
public class RoundRobinRouter extends Router {
    private int index = 0;

    public RoundRobinRouter(int siMax) {
        super(siMax);
    }

    @Override
    protected WebServer selectWebServer(Job job, WebServerCluster cluster) {
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
