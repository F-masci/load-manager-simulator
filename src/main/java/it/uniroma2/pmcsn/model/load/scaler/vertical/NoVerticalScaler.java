package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * A null-object implementation of VerticalScaler that does nothing and never scales.
 */
public class NoVerticalScaler extends VerticalScaler {

    /**
     * Constructs a NoVerticalScaler with zeroed thresholds and cooldown.
     */
    public NoVerticalScaler() {
        super(0.0, 0.0, 0.0);
    }

    /**
     * Always returns false as this scaler performs no actions.
     *
     * @param clock current simulation time
     * @param spikeServer the server to potentially scale
     * @return false
     */
    @Override
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        return false;
    }

    /**
     * Always returns zero as this scaler tracks no metrics.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return 0.0
     */
    @Override
    public double getCurrentMetric(double clock, SpikeServer spikeServer) {
        return 0.0;
    }
}
