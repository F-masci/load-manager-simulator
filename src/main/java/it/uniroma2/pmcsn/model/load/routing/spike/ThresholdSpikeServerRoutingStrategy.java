package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;

/**
 * Implementation of SpikeServerRoutingStrategy based on a threshold value (siMax) compared
 * to the target Web Server's Spike Indicator load.
 */
public class ThresholdSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {

    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        return targetServer.getSpikeIndicator() >= siMax;
    }
}
