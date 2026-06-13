package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;

/**
 * Strategy interface for deciding whether to route a request to the Spike Server.
 */
public interface SpikeServerRoutingStrategy {
    /**
     * Determines if the request should be routed to the Spike Server instead of the target Web Server.
     *
     * @param targetServer the Web Server that was selected by the Web Server routing strategy
     * @param siMax        the threshold configuration for Spike Indicator
     * @return true if the job should be routed to the Spike Server, false otherwise
     */
    boolean shouldRouteToSpike(WebServer targetServer, int siMax);
}
