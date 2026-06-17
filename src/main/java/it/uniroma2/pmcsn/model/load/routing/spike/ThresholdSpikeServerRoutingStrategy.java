package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy based on threshold values for activating and deactivating spike server routing.
 */
public class ThresholdSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ThresholdSpikeServerRoutingStrategy.class, "ROUTER");

    private final Map<Integer, Boolean> spikeModeMap = new HashMap<>();
    private final int siLow;
    private int stateChanges = 0;

    /**
     * Constructs a strategy where the deactivation threshold equals the activation threshold.
     */
    public ThresholdSpikeServerRoutingStrategy() {
        this(-1);
    }

    /**
     * Constructs a strategy with an explicit deactivation threshold.
     *
     * @param siLow the lower threshold for deactivating spike routing
     */
    public ThresholdSpikeServerRoutingStrategy(int siLow) {
        this.siLow = siLow;
    }

    /**
     * Determines if a request should be routed to the spike server based on hysteresis.
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return true if the job should be routed to the spike server
     */
    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        int currentSiLow = (siLow == -1) ? siMax : siLow;
        boolean currentlyInSpike = spikeModeMap.getOrDefault(targetServer.getId(), false);

        boolean nextState;
        if (!currentlyInSpike) {
            // Activation condition
            nextState = targetServer.getSpikeIndicator() >= siMax;
        } else {
            // Deactivation condition
            nextState = targetServer.getSpikeIndicator() > currentSiLow;
        }

        if (nextState != currentlyInSpike) {
            spikeModeMap.put(targetServer.getId(), nextState);
            stateChanges++;
            logger.debug("WebServer {} changed Spike Mode to: {} (SI={} vs siMax={}, siLow={})",
                    targetServer.getId(), nextState, targetServer.getSpikeIndicator(), siMax, currentSiLow);
        }

        return nextState;
    }

    /**
     * Gets the total number of state changes occurred.
     *
     * @return the state changes count
     */
    public int getStateChanges() {
        return stateChanges;
    }

    /**
     * Resets the state and counters for all servers.
     */
    public void reset() {
        spikeModeMap.clear();
        stateChanges = 0;
    }
}
