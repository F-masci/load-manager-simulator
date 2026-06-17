package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.LogFactory.ModuleLogger;

/**
 * Concrete vertical scaler that dynamically adjusts SpikeServer speed multiplier
 * based on its utilization thresholds.
 */
public abstract class ThresholdVerticalScaler extends VerticalScaler {
    private static final ModuleLogger logger = LogFactory.getLogger(ThresholdVerticalScaler.class, "SCALER");

    private final double baseSpeed;
    private final double scaledSpeed;
    private boolean isScaled = false;

    /**
     * Constructs a ThresholdVerticalScaler with specified thresholds, speeds, and cooldown.
     *
     * @param upperThreshold the value above which scale up is triggered
     * @param lowerThreshold the value below which scale down is triggered
     * @param baseSpeed the default speed multiplier
     * @param scaledSpeed the increased speed multiplier
     * @param cooldown the minimum time interval between scaling actions
     */
    public ThresholdVerticalScaler(double upperThreshold, double lowerThreshold,
                                   double baseSpeed, double scaledSpeed, double cooldown) {
        super(upperThreshold, lowerThreshold, cooldown);
        this.baseSpeed = baseSpeed;
        this.scaledSpeed = scaledSpeed;
    }

    /**
     * Evaluates whether to vertically scale SpikeServer capacity based on thresholds.
     *
     * @param clock current simulation time
     * @param spikeServer the server to potentially scale
     * @return true if a scaling action occurred, false otherwise
     */
    @Override
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        final double remainingCooldown = getRemainingCooldown(clock);
        if (remainingCooldown >= 0.01) return false;

        double currentMetric = getCurrentMetric(clock, spikeServer);
        if (!isScaled && currentMetric >= upperThreshold) {
            spikeServer.setSpeedMultiplier(scaledSpeed, clock);
            isScaled = true;
            lastScalingTime = clock;
            this.scaleUpCount++;
            return true;
        } else if (isScaled && currentMetric <= lowerThreshold) {
            spikeServer.setSpeedMultiplier(baseSpeed, clock);
            isScaled = false;
            lastScalingTime = clock;
            this.scaleDownCount++;
            return true;
        }
        return false;
    }

    /**
     * Returns the current value of the metric used for scaling decisions.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return the current metric value
     */
    @Override
    public double getCurrentMetric(double clock, SpikeServer spikeServer) {
        return getThresholdMetric(clock, spikeServer);
    }

    /**
     * Internal method to retrieve the specific metric used for threshold evaluation.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return the threshold metric value
     */
    protected abstract double getThresholdMetric(double clock, SpikeServer spikeServer);

    /**
     * Checks if the system is currently in a scaled state.
     *
     * @return true if scaled, false otherwise
     */
    public boolean isScaled() {
        return isScaled;
    }

    /**
     * Returns the base speed multiplier.
     *
     * @return the base speed value
     */
    public double getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * Returns the scaled speed multiplier.
     *
     * @return the scaled speed value
     */
    public double getScaledSpeed() {
        return scaledSpeed;
    }
}
