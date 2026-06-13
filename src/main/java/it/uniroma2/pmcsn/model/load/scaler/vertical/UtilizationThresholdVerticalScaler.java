package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete vertical scaler that dynamically adjusts SpikeServer speed multiplier
 * based on its utilization thresholds.
 */
public class UtilizationThresholdVerticalScaler extends VerticalScaler {
    private static final Logger logger = LoggerFactory.getLogger(UtilizationThresholdVerticalScaler.class);

    private final double baseSpeed;
    private final double scaledSpeed;
    private boolean isScaled = false;

    public UtilizationThresholdVerticalScaler(double upperThreshold, double lowerThreshold,
                                             double baseSpeed, double scaledSpeed, double cooldown) {
        super(upperThreshold, lowerThreshold, cooldown);
        this.baseSpeed = baseSpeed;
        this.scaledSpeed = scaledSpeed;
    }

    @Override
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        if (clock - lastScalingTime < cooldown) {
            return false;
        }

        double utilization = spikeServer.getAverageUtilization(clock);
        if (!isScaled && utilization >= upperThreshold) {
            spikeServer.setSpeedMultiplier(scaledSpeed, clock);
            isScaled = true;
            lastScalingTime = clock;
            scaleUpCount++;
            return true;
        } else if (isScaled && utilization <= lowerThreshold) {
            spikeServer.setSpeedMultiplier(baseSpeed, clock);
            isScaled = false;
            lastScalingTime = clock;
            scaleDownCount++;
            return true;
        }
        return false;
    }

    public boolean isScaled() {
        return isScaled;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public double getScaledSpeed() {
        return scaledSpeed;
    }
}
