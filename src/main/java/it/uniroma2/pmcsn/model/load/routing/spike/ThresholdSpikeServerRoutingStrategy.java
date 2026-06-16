package it.uniroma2.pmcsn.model.load.routing.spike;

import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of SpikeServerRoutingStrategy based on a threshold value (siMax) compared
 * to the target Web Server's Spike Indicator load.
 */
public class ThresholdSpikeServerRoutingStrategy implements SpikeServerRoutingStrategy {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ThresholdSpikeServerRoutingStrategy.class, "ROUTER");

    private final Map<Integer, Boolean> spikeModeMap = new HashMap<>();
    private final int siLow;
    private int stateChanges = 0;

    /**
     * Standard constructor. siLow will be equal to siMax.
     */
    public ThresholdSpikeServerRoutingStrategy() {
        this(-1);
    }

    /**
     * Constructor with explicit siLow.
     * @param siLow the lower threshold for deactivating spike routing.
     */
    public ThresholdSpikeServerRoutingStrategy(int siLow) {
        this.siLow = siLow;
    }

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
     * Returns the total number of state changes (activations/deactivations) occurred.
     */
    public int getStateChanges() {
        return stateChanges;
    }

    /**
     * Resets the state and counter for all servers.
     */
    public void reset() {
        spikeModeMap.clear();
        stateChanges = 0;
    }
}
