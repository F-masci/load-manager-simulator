package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the Spike Server (Layer 2) using Processor Sharing scheduling.
 */
public class SpikeServer extends Server {
    private static final Logger logger = LoggerFactory.getLogger(SpikeServer.class);
    private final TimedWelford speedMultiplierStat = new TimedWelford();

    public SpikeServer(int id, double speedMultiplier) {
        super(id, speedMultiplier);
    }

    public void setSpeedMultiplier(double speedMultiplier, double clock) {
        logger.info(this.speedMultiplier < speedMultiplier
                    ? "Scale Up: SpikeServer speed set to {} at clock={} (utilization={})"
                    : "Scale Down: SpikeServer speed restored to {} at clock={} (utilization={})", speedMultiplier, clock, getAverageUtilization(clock));
        super.setSpeedMultiplier(speedMultiplier);

    }

    @Override
    public void updateStatistics(double currentClock) {
        speedMultiplierStat.updateToTime(currentClock, speedMultiplier);
        super.updateStatistics(currentClock);
    }

    @Override
    public void resetStatistics(double currentClock) {
        super.resetStatistics(currentClock);
        this.speedMultiplierStat.reset();
        this.speedMultiplierStat.updateToTime(currentClock, speedMultiplier);
    }

    /**
     * Calculates the average speed multiplier (capacity multiplier) of the SpikeServer.
     */
    public double getAverageSpeedMultiplier(double totalTime) {
        return speedMultiplierStat.getMean();
    }
}
