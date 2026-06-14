package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.utils.LogFactory;

/**
 * Implementation of SpikeServerRoutingStrategy based on a threshold value (siMax) compared
 * to the target Web Server's Spike Indicator load.
 */
public class ThresholdSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ThresholdSpikeServerRoutingStrategy.class, "ROUTER");

    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        boolean res = targetServer.getSpikeIndicator() >= siMax;
        if(res) logger.debug("Routing to Spike for WebServer {}: SpikeIndicator={} vs siMax={}",
                targetServer.getId(), targetServer.getSpikeIndicator(), siMax);
        return res;
    }
}
