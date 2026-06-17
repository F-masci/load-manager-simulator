package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;

/**
 * Strategy for deciding whether to route a request to the spike server.
 */
public interface SpikeServerRoutingStrategy {
    /**
     * Determines if a request should be routed to the spike server.
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return true if the job should be routed to the spike server
     */
    boolean shouldRouteToSpike(WebServer targetServer, int siMax);
}
