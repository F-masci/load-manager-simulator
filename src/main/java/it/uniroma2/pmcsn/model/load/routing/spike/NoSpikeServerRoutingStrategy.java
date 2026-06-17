package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;

/**
 * Strategy that never routes to the spike server.
 */
public class NoSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {

    /**
     * Always returns false as this strategy disables spike server routing.
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return always false
     */
    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        return false;
    }
}
