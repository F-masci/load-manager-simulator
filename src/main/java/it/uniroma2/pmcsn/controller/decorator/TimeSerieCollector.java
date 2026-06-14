package it.uniroma2.pmcsn.controller.decorator;

import it.uniroma2.pmcsn.controller.Simulator;
import java.util.ArrayList;
import java.util.List;

/**
 * SimulatorDecorator to collect the time series of individual response times in-memory.
 */
public class TimeSerieCollector extends SimulatorDecorator {
    private final List<Double> series = new ArrayList<>();
    private int lastJobsCompleted = 0;
    private double lastSumOfResponseTimes = 0.0;

    public TimeSerieCollector(Simulator decorated) {
        super(decorated);
    }

    @Override
    public boolean processNextEvent() {
        boolean hasNext = super.processNextEvent();
        
        int currentJobsCompleted = getTotalJobsCompleted();
        if (currentJobsCompleted > lastJobsCompleted) {
            double currentSum = getAverageResponseTime() * currentJobsCompleted;
            // The difference is the sum of response times for the newly completed jobs
            double newResponseTimeSum = currentSum - lastSumOfResponseTimes;
            int newJobsCount = currentJobsCompleted - lastJobsCompleted;
            
            // Average it out if multiple jobs completed at exactly the same time
            double avgNewResponseTime = newResponseTimeSum / newJobsCount;
            
            for (int i = 0; i < newJobsCount; i++) {
                series.add(avgNewResponseTime);
            }
            
            lastJobsCompleted = currentJobsCompleted;
            lastSumOfResponseTimes = currentSum;
        }
        
        return hasNext;
    }

    /**
     * Retrieves the collected time series data.
     * @return List of response times.
     */
    public List<Double> getSeries() {
        return series;
    }

    @Override
    public void resetStatistics() {
        super.resetStatistics();
        series.clear();
        lastJobsCompleted = 0;
        lastSumOfResponseTimes = 0.0;
    }
}
