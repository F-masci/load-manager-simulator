package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;

/**
 * Implementation of SpikeServerRoutingStrategy that never routes to the Spike Server (always returns false).
 * Used for testing or running simulations in a simplified context without a Spike Server.
 */
public class NoSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {

    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        return false;
    }
}
