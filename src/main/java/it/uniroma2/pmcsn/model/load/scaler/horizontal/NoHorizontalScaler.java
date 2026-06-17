package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * A null-object implementation of HorizontalScaler that does nothing and never scales.
 */
public class NoHorizontalScaler extends HorizontalScaler {
    
    /**
     * Constructs a NoHorizontalScaler that does nothing.
     */
    public NoHorizontalScaler() {
        super(0.0, 0.0, 0.0);
    }

    /**
     * Does nothing.
     * @param clock The current simulation clock
     * @param responseTime The response time
     */
    @Override
    public void recordCompletion(double clock, double responseTime) {
        // Do nothing
    }

    /**
     * Always returns false as no scaling is performed.
     * @param clock The current simulation clock
     * @param cluster The cluster to evaluate
     * @return false
     */
    @Override
    public boolean evaluateScaling(double clock, WebServerCluster cluster) {
        return false;
    }

    /**
     * Always returns 0.0 as no metric is collected.
     * @param clock The current simulation clock
     * @return 0.0
     */
    @Override
    public double getCurrentMetric(double clock) {
        return 0.0;
    }
}
