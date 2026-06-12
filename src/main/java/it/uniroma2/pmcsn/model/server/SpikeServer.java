package it.uniroma2.pmcsn.model.server;

/**
 * Represents the Spike Server (Layer 2) using Processor Sharing scheduling.
 */
public class SpikeServer extends Server {
    private double timeIntegratedSpeedMultiplier = 0.0;

    public SpikeServer(int id, int capacity, double speedMultiplier) {
        super(id, capacity, speedMultiplier);
    }

    @Override
    public void updateStatistics(double currentClock) {
        double duration = currentClock - lastEventTime;
        if (duration > 0) {
            timeIntegratedSpeedMultiplier += duration * speedMultiplier;
        }
        super.updateStatistics(currentClock);
    }

    /**
     * Calculates the average speed multiplier (capacity multiplier) of the SpikeServer.
     */
    public double getAverageSpeedMultiplier(double totalTime) {
        if (totalTime <= 0) {
            return speedMultiplier;
        }
        return timeIntegratedSpeedMultiplier / totalTime;
    }
}
