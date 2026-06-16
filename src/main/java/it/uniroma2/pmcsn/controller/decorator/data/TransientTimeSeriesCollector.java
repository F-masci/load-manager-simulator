package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;

import java.util.ArrayList;
import java.util.List;

/**
 * SimulatorDecorator to collect the time series of Response Time against Clock Time.
 * Also stores the seed used for the specific simulation run.
 */
public class TransientTimeSeriesCollector extends SimulatorDecorator {
    // Stores pairs of [ClockTime, R0]
    private final List<double[]> timeSeries = new ArrayList<>();
    private int lastJobsCompleted = 0;
    private final long startingSeed;

    public TransientTimeSeriesCollector(Simulator decorated) {
        super(decorated);
        this.startingSeed = decorated.getSeed();
    }

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

    public long getStartingSeed() {
        return startingSeed;
    }

    /**
     * Retrieves the collected time series data (Clock vs R0).
     */
    public List<double[]> getTimeSeries() {
        return timeSeries;
    }

    @Override
    public void resetStatistics() {
        super.resetStatistics();
        timeSeries.clear();
        lastJobsCompleted = 0;
    }
}
