package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Base abstract class representing a Horizontal WebServer Scaler.
 */
public abstract class HorizontalScaler {
    protected final double scaleOutThreshold;
    protected final double scaleInThreshold;
    protected final double cooldown;
    protected double lastScalingTime = 0.0;

    /**
     * Constructs a HorizontalScaler with the specified thresholds and cooldown.
     * @param scaleOutThreshold The threshold for scaling out
     * @param scaleInThreshold The threshold for scaling in
     * @param cooldown The cooldown period between scaling actions
     */
    protected HorizontalScaler(double scaleOutThreshold, double scaleInThreshold, double cooldown) {
        this.scaleOutThreshold = scaleOutThreshold;
        this.scaleInThreshold = scaleInThreshold;
        this.cooldown = cooldown;
    }

    /**
     * Records a job completion with its response time.
     * @param clock The current simulation clock
     * @param responseTime The response time of the completed job
     */
    public abstract void recordCompletion(double clock, double responseTime);

    /**
     * Evaluates whether to scale the cluster out or in.
     * @param clock The current simulation clock
     * @param cluster The WebServerCluster to evaluate
     * @return true if a scaling action occurred, false otherwise
     */
    public abstract boolean evaluateScaling(double clock, WebServerCluster cluster);

    /**
     * Returns the current value of the metric used for scaling decisions.
     * @param clock The current simulation clock
     * @return The current metric value
     */
    public abstract double getCurrentMetric(double clock);

    /**
     * Gets the threshold for scaling out.
     * @return The scale out threshold
     */
    public double getScaleOutThreshold() {
        return scaleOutThreshold;
    }

    /**
     * Gets the threshold for scaling in.
     * @return The scale in threshold
     */
    public double getScaleInThreshold() {
        return scaleInThreshold;
    }

    /**
     * Gets the cooldown period.
     * @return The cooldown period
     */
    public double getCooldown() {
        return cooldown;
    }

    /**
     * Gets the time of the last scaling action.
     * @return The last scaling time
     */
    public double getLastScalingTime() {
        return lastScalingTime;
    }

    /**
     * Calculates the remaining cooldown time.
     * @param clock The current simulation clock
     * @return The remaining cooldown time
     */
    public double getRemainingCooldown(double clock) {
        return Math.round(Math.max(0.0, cooldown - (clock - lastScalingTime)) * 100.0) / 100.0;
    }

    /**
     * Resets the statistics and metrics of the scaler.
     */
    public void resetStatistics() {
        // Default implementation for scalers without resettable metrics
    }
}
