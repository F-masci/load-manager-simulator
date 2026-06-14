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

    protected HorizontalScaler(double scaleOutThreshold, double scaleInThreshold, double cooldown) {
        this.scaleOutThreshold = scaleOutThreshold;
        this.scaleInThreshold = scaleInThreshold;
        this.cooldown = cooldown;
    }

    /**
     * Records a job completion with its response time.
     */
    public abstract void recordCompletion(double clock, double responseTime);

    /**
     * Evaluates whether to scale the cluster out or in.
     * Returns true if a scaling action occurred, false otherwise.
     */
    public abstract boolean evaluateScaling(double clock, WebServerCluster cluster);

    /**
     * Returns the current value of the metric used for scaling decisions.
     */
    public abstract double getCurrentMetric(double clock);

    public double getScaleOutThreshold() {
        return scaleOutThreshold;
    }

    public double getScaleInThreshold() {
        return scaleInThreshold;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getLastScalingTime() {
        return lastScalingTime;
    }

    public double getRemainingCooldown(double clock) {
        return Math.round(Math.max(0.0, cooldown - (clock - lastScalingTime)) * 100.0) / 100.0;
    }

    public void resetStatistics() {
        // Default implementation for scalers without resettable metrics
    }
}
