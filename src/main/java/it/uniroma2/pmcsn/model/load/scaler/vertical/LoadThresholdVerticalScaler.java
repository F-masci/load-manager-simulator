package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * Concrete vertical scaler that dynamically adjusts SpikeServer speed multiplier
 * based on its load (number of active jobs) thresholds.
 */
public class LoadThresholdVerticalScaler extends ThresholdVerticalScaler {

    /**
     * Constructs a LoadThresholdVerticalScaler using the provided application configuration.
     *
     * @param config the application configuration
     */
    public LoadThresholdVerticalScaler(ApplicationConfig config) {
        this(config.scaling());
    }

    /**
     * Constructs a LoadThresholdVerticalScaler using the provided scaling configuration and a default increment.
     *
     * @param scalingConfig the scaling configuration
     */
    public LoadThresholdVerticalScaler(ScalingConfig scalingConfig) {
        this(scalingConfig, ApplicationConfig.VERTICAL_INCREMENT);
    }

    /**
     * Constructs a LoadThresholdVerticalScaler using the provided scaling configuration and a specific increment.
     *
     * @param scalingConfig the scaling configuration
     * @param increment the speed multiplier increment
     */
    public LoadThresholdVerticalScaler(ScalingConfig scalingConfig, double increment) {
        this(scalingConfig.spikeUpperThreshold(), scalingConfig.spikeLowerThreshold(), scalingConfig.spikeCpuPercentage(), scalingConfig.spikeCpuPercentage() + increment, scalingConfig.cooldown());
    }

    /**
     * Constructs a LoadThresholdVerticalScaler with explicit thresholds, speeds, and cooldown.
     *
     * @param upperThreshold the value above which scale up is triggered
     * @param lowerThreshold the value below which scale down is triggered
     * @param baseSpeed the base speed multiplier
     * @param scaledSpeed the scaled speed multiplier
     * @param cooldown the minimum time interval between scaling actions
     */
    public LoadThresholdVerticalScaler(double upperThreshold, double lowerThreshold, double baseSpeed, double scaledSpeed, double cooldown) {
        super(upperThreshold, lowerThreshold, baseSpeed, scaledSpeed, cooldown);
    }

    /**
     * Returns the current load (number of active jobs) as the scaling metric.
     *
     * @param clock current simulation time
     * @param spikeServer the server whose metric is retrieved
     * @return the number of active jobs in the server
     */
    @Override
    protected double getThresholdMetric(double clock, SpikeServer spikeServer) {
        return (double) spikeServer.getActiveJobs().size();
    }

}
