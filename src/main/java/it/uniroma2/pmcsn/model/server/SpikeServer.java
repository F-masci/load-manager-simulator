package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.LogFactory.ModuleLogger;

/**
 * Represents the Spike Server (Layer 2) using Processor Sharing scheduling.
 */
public class SpikeServer extends Server {
    private static final ModuleLogger logger = LogFactory.getLogger(SpikeServer.class, "SERVER");
    private final TimedWelford speedMultiplierStat = new TimedWelford();

    /**
     * Constructs a SpikeServer with a given ID and speed multiplier.
     *
     * @param id the unique identifier of the server
     * @param speedMultiplier the processing rate multiplier
     */
    public SpikeServer(int id, double speedMultiplier) {
        super(id, speedMultiplier);
    }

    /**
     * Sets the speed multiplier for the server and logs the scaling action.
     *
     * @param speedMultiplier the new speed multiplier value
     * @param clock current simulation time
     */
    public void setSpeedMultiplier(double speedMultiplier, double clock) {
        logger.debug(this.speedMultiplier < speedMultiplier
                    ? "Scale Up: SpikeServer speed set to {} at clock={} (load={})"
                    : "Scale Down: SpikeServer speed restored to {} at clock={} (load={})", speedMultiplier, clock, getActiveJobs().size());
        super.setSpeedMultiplier(speedMultiplier);
    }

    /**
     * Updates time-integrated statistics including the speed multiplier.
     *
     * @param currentClock current simulation time
     */
    @Override
    public void updateStatistics(double currentClock) {
        speedMultiplierStat.updateToTime(currentClock, speedMultiplier);
        super.updateStatistics(currentClock);
    }

    /**
     * Resets all performance statistics.
     *
     * @param currentClock current simulation clock
     */
    @Override
    public void resetStatistics(double currentClock) {
        super.resetStatistics(currentClock);
        this.speedMultiplierStat.reset();
        this.speedMultiplierStat.updateToTime(currentClock, speedMultiplier);
    }

    /**
     * Calculates the average speed multiplier (capacity multiplier) of the SpikeServer.
     *
     * @param totalTime total simulation time (unused, kept for consistency)
     * @return the average speed multiplier
     */
    public double getAverageSpeedMultiplier(double totalTime) {
        return speedMultiplierStat.getMean();
    }
}
