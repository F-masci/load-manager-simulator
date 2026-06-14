package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SpikeServerRoutingStrategy based on a threshold value (siMax) compared
 * to the target Web Server's Spike Indicator load.
 */
public class ThresholdSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ThresholdSpikeServerRoutingStrategy.class);

    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        boolean res = targetServer.getSpikeIndicator() >= siMax;
        if(res) logger.debug("Routing to Spike for WebServer {}: SpikeIndicator={} vs siMax={}",
                targetServer.getId(), targetServer.getSpikeIndicator(), siMax);
        return res;
    }
}
