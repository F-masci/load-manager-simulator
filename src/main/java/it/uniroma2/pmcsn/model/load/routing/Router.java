package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Base abstract class defining the Template Method for routing requests.
 */
public abstract class Router {
    protected final int siMax;

    protected Router(int siMax) {
        this.siMax = siMax;
    }

    /**
     * Template Method for routing a job.
     * Selects a target Web Server and decides whether to divert the request to the Spike Server.
     */
    public final Server route(Job job, WebServerCluster cluster, SpikeServer spikeServer) {
        WebServer targetServer = selectWebServer(job, cluster);
        if (shouldDivertToSpike(targetServer)) {
            return spikeServer;
        }
        return targetServer;
    }

    /**
     * Selects the target web server from the cluster according to a specific routing policy.
     */
    protected abstract WebServer selectWebServer(Job job, WebServerCluster cluster);

    /**
     * Returns true if the request should be diverted to the Spike Server based on the target Web Server's load.
     */
    protected boolean shouldDivertToSpike(WebServer server) {
        return server.getSpikeIndicator() >= siMax;
    }

    public int getSiMax() {
        return siMax;
    }
}
