package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Base abstract class representing a Horizontal WebServer Scaler.
 */
public abstract class HorizontalScaler {
    protected final double scaleUpThreshold;
    protected final double scaleDownThreshold;
    protected final double cooldown;
    protected double lastScalingTime = 0.0;

    protected HorizontalScaler(double scaleUpThreshold, double scaleDownThreshold, double cooldown) {
        this.scaleUpThreshold = scaleUpThreshold;
        this.scaleDownThreshold = scaleDownThreshold;
        this.cooldown = cooldown;
    }

    /**
     * Records a job completion with its response time.
     */
    public abstract void recordCompletion(double clock, double responseTime);

    /**
     * Evaluates whether to scale the cluster up or down.
     * Returns true if a scaling action occurred, false otherwise.
     */
    public abstract boolean evaluateScaling(double clock, WebServerCluster cluster);

    /**
     * Returns the current value of the metric used for scaling decisions.
     */
    public abstract double getCurrentMetric(double clock);

    public double getScaleUpThreshold() {
        return scaleUpThreshold;
    }

    public double getScaleDownThreshold() {
        return scaleDownThreshold;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getLastScalingTime() {
        return lastScalingTime;
    }

    public double getRemainingCooldown(double clock) {
        return Math.max(0.0, cooldown - (clock - lastScalingTime));
    }

    public void resetStatistics() {
        // Default implementation for scalers without resettable metrics
    }
}
