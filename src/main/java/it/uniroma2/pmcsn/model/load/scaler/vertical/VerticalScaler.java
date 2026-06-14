package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * Base abstract class representing a Vertical SpikeServer Scaler.
 */
public abstract class VerticalScaler {
    protected final double upperThreshold;
    protected final double lowerThreshold;
    protected final double cooldown;
    protected double lastScalingTime = Double.NEGATIVE_INFINITY;

    protected int scaleUpCount = 0;
    protected int scaleDownCount = 0;

    protected VerticalScaler(double upperThreshold, double lowerThreshold, double cooldown) {
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.cooldown = cooldown;
    }

    /**
     * Evaluates whether to vertically scale SpikeServer capacity (adjust speed multiplier).
     * Returns true if a scaling action occurred, false otherwise.
     */
    public abstract boolean evaluateScaling(double clock, SpikeServer spikeServer);

    /**
     * Returns the current value of the metric used for scaling decisions.
     */
    public abstract double getCurrentMetric(double clock, SpikeServer spikeServer);

    public double getUpperThreshold() {
        return upperThreshold;
    }

    public double getLowerThreshold() {
        return lowerThreshold;
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

    public int getScaleUpCount() {
        return scaleUpCount;
    }

    public int getScaleDownCount() {
        return scaleDownCount;
    }

    public void resetStatistics() {
        scaleUpCount = 0;
        scaleDownCount = 0;
    }
}
