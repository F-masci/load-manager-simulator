package it.pmcsn.model.load.scaler.horizontal;

import it.pmcsn.model.server.WebServerCluster;

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
}
