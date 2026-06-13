package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;

/**
 * Represents the Spike Server (Layer 2) using Processor Sharing scheduling.
 */
public class SpikeServer extends Server {
    private final TimedWelford speedMultiplierStat = new TimedWelford();

    public SpikeServer(int id, int capacity, double speedMultiplier) {
        super(id, capacity, speedMultiplier);
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
