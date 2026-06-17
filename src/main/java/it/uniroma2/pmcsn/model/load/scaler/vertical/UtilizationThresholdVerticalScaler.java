package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * Concrete vertical scaler that dynamically adjusts SpikeServer speed multiplier
 * based on its utilization thresholds.
 */
public class UtilizationThresholdVerticalScaler extends ThresholdVerticalScaler {

    /**
     * Constructs a UtilizationThresholdVerticalScaler with specified thresholds, speeds, and cooldown.
     *
     * @param upperThreshold the value above which scale up is triggered
     * @param lowerThreshold the value below which scale down is triggered
     * @param baseSpeed the default speed multiplier
     * @param scaledSpeed the increased speed multiplier
     * @param cooldown the minimum time interval between scaling actions
     */
    public UtilizationThresholdVerticalScaler(double upperThreshold, double lowerThreshold, double baseSpeed, double scaledSpeed, double cooldown) {
        super(upperThreshold, lowerThreshold, baseSpeed, scaledSpeed, cooldown);
    }

    /**
     * Returns the current average utilization as the scaling metric.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return the average utilization of the server
     */
    @Override
    protected double getThresholdMetric(double clock, SpikeServer spikeServer) {
        return spikeServer.getAverageUtilization(clock);
    }

}
