package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Specialized router that selects the Web Server with the lowest load (Spike Indicator).
 */
public class LeastLoadedRouter extends Router {

    public LeastLoadedRouter(int siMax) {
        super(siMax);
    }

    @Override
    protected WebServer selectWebServer(Job job, WebServerCluster cluster) {
        List<WebServer> active = cluster.getActiveServers();
        if (active.isEmpty()) {
            throw new IllegalStateException("No active web servers in cluster.");
        }
        WebServer bestServer = active.get(0);
        int minLoad = bestServer.getSpikeIndicator();
        for (int i = 1; i < active.size(); i++) {
            WebServer current = active.get(i);
            int currentLoad = current.getSpikeIndicator();
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                bestServer = current;
            }
        }
        return bestServer;
    }
}
