package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import java.util.List;

/**
 * Routing strategy that selects the web server with the lowest spike indicator load.
 */
public class LeastLoadedRoutingStrategy implements WebServerRoutingStrategy {

    /**
     * Selects the active web server with the minimum load.
     *
     * @param job the job to route
     * @param cluster the web server cluster
     * @return the least loaded web server
     * @throws IllegalStateException if no active servers are found
     */
    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
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
