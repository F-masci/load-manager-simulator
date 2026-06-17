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

    /**
     * Constructs a VerticalScaler with specified thresholds and cooldown.
     *
     * @param upperThreshold the value above which scale up is triggered
     * @param lowerThreshold the value below which scale down is triggered
     * @param cooldown the minimum time interval between scaling actions
     */
    protected VerticalScaler(double upperThreshold, double lowerThreshold, double cooldown) {
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.cooldown = cooldown;
    }

    /**
     * Evaluates whether to vertically scale SpikeServer capacity (adjust speed multiplier).
     *
     * @param clock current simulation time
     * @param spikeServer the server to potentially scale
     * @return true if a scaling action occurred, false otherwise
     */
    public abstract boolean evaluateScaling(double clock, SpikeServer spikeServer);

    /**
     * Returns the current value of the metric used for scaling decisions.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return the current metric value
     */
    public abstract double getCurrentMetric(double clock, SpikeServer spikeServer);

    /**
     * Returns the upper threshold for scaling up.
     *
     * @return the upper threshold value
     */
    public double getUpperThreshold() {
        return upperThreshold;
    }

    /**
     * Returns the lower threshold for scaling down.
     *
     * @return the lower threshold value
     */
    public double getLowerThreshold() {
        return lowerThreshold;
    }

    /**
     * Returns the cooldown period between scaling actions.
     *
     * @return the cooldown duration
     */
    public double getCooldown() {
        return cooldown;
    }

    /**
     * Returns the timestamp of the last scaling action.
     *
     * @return the last scaling time
     */
    public double getLastScalingTime() {
        return lastScalingTime;
    }

    /**
     * Calculates the remaining cooldown time based on the current clock.
     *
     * @param clock current simulation time
     * @return the remaining cooldown time
     */
    public double getRemainingCooldown(double clock) {
        return Math.round(Math.max(0.0, cooldown - (clock - lastScalingTime)) * 100.0) / 100.0;
    }

    /**
     * Returns the number of times the system scaled up.
     *
     * @return the scale up count
     */
    public int getScaleUpCount() {
        return scaleUpCount;
    }

    /**
     * Returns the number of times the system scaled down.
     *
     * @return the scale down count
     */
    public int getScaleDownCount() {
        return scaleDownCount;
    }

    /**
     * Resets the scale up and scale down statistics.
     */
    public void resetStatistics() {
        scaleUpCount = 0;
        scaleDownCount = 0;
    }
}
