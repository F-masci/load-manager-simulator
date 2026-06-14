package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * Concrete vertical scaler that dynamically adjusts SpikeServer speed multiplier
 * based on its utilization thresholds.
 */
public class LoadThresholdVerticalScaler extends ThresholdVerticalScaler {

    public LoadThresholdVerticalScaler(double upperThreshold, double lowerThreshold, double baseSpeed, double scaledSpeed, double cooldown) {
        super(upperThreshold, lowerThreshold, baseSpeed, scaledSpeed, cooldown);
    }

    @Override
    protected double getThresholdMetric(double clock, SpikeServer spikeServer) {
        return (double) spikeServer.getActiveJobs().size();
    }

}
