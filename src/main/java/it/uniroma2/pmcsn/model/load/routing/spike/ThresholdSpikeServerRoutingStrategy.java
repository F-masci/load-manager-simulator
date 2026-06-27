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
     * Determines if a request should be routed to the spike server based on the active strategy.
     * Delegates to stateless or stateful logic depending on whether the band mechanism is active.
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return true if the job should be routed to the spike server
     */
    @Override
    public boolean shouldRouteToSpike(WebServer targetServer, int siMax) {
        if (siLow == -1 || siLow == siMax) {
            return shouldRouteWithoutBand(targetServer, siMax);
        } else {
            return shouldRouteWithBand(targetServer, siMax, siLow);
        }
    }

    /**
     * Stateless decision logic for when the band mechanism is deactivated (B = 0).
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return true if the job should be routed to the spike server
     */
    private boolean shouldRouteWithoutBand(WebServer targetServer, int siMax) {
        return targetServer.getSpikeIndicator() >= siMax;
    }

    /**
     * Stateful decision logic with memory for when the band mechanism is active (B > 0).
     *
     * @param targetServer the initially selected web server
     * @param siMax the maximum spike indicator threshold
     * @return true if the job should be routed to the spike server
     */
    private boolean shouldRouteWithBand(WebServer targetServer, int siMax, int siLow) {
        boolean currentlyInSpike = spikeModeMap.getOrDefault(targetServer.getId(), false);

        boolean nextState;
        if (!currentlyInSpike) {
            // Activation condition
            nextState = targetServer.getSpikeIndicator() >= siMax;
        } else {
            // Deactivation condition
            // stays in spike if SI > siLow (releases when SI <= siLow)
            nextState = targetServer.getSpikeIndicator() > siLow;
        }

        if (nextState != currentlyInSpike) {
            spikeModeMap.put(targetServer.getId(), nextState);
            stateChanges++;
            logger.debug("WebServer {} changed Spike Mode to: {} (SI={} vs siMax={}, siLow={})",
                    targetServer.getId(), nextState, targetServer.getSpikeIndicator(), siMax, siLow);
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
