package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator that collects a time series of response time against clock time.
 */
public class TransientTimeSeriesCollector extends SimulatorDecorator {
    // Stores pairs of [ClockTime, R0]
    private final List<double[]> timeSeries = new ArrayList<>();
    private int lastJobsCompleted = 0;
    private final long startingSeed;

    /**
     * Initializes the collector with a simulator and records its starting seed.
     *
     * @param decorated the simulator to decorate
     */
    public TransientTimeSeriesCollector(Simulator decorated) {
        super(decorated);
        this.startingSeed = decorated.getSeed();
    }

    /**
     * Processes the next event and records response time if a job was completed.
     *
     * @return true if an event was processed, false otherwise
     */
    @Override
    public boolean processNextEvent() {
        boolean hasNext = super.processNextEvent();
        
        int currentJobsCompleted = getTotalJobsCompleted();
        if (currentJobsCompleted > lastJobsCompleted) {
            // Record a data point for the completed job
            timeSeries.add(new double[]{getClock(), getAverageResponseTime()});
            lastJobsCompleted = currentJobsCompleted;
        }
        
        return hasNext;
    }

    /**
     * Gets the starting seed of the simulation.
     *
     * @return the starting seed
     */
    public long getStartingSeed() {
        return startingSeed;
    }

    /**
     * Retrieves the collected time series data as clock and response time pairs.
     *
     * @return list of double arrays containing clock and response time
     */
    public List<double[]> getTimeSeries() {
        return timeSeries;
    }

    /**
     * Resets the collected time series and completed jobs counter.
     */
    @Override
    public void resetStatistics() {
        super.resetStatistics();
        timeSeries.clear();
        lastJobsCompleted = 0;
    }
}
