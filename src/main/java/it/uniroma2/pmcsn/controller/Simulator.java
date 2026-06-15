package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.utils.LogFactory;

/**
 * Interface for the simulation engine, allowing decorators.
 */
public interface Simulator {
    LogFactory.ModuleLogger logger = LogFactory.getLogger(Simulator.class, "SIM");

    boolean processNextEvent();
    void resetStatistics();
    void scheduleInitialEvents();
    void finalizeSimulation();
    
    // State accessors
    double getClock();
    int getTotalJobsArrived();
    int getTotalJobsDiverted();
    int getTotalJobsCompleted();
    double getAverageResponseTime();
    double getAverageJobsInSystem();
    double getSystemUtilization();
    double getThroughput();
    
    WebServerCluster getWebServerCluster();
    SpikeServer getSpikeServer();
    LoadManager getLoadManager();
    long getSeed();
    EventType getLastEventType();

    /**
     * Default implementation of the simulation loop.
     * By using the interface methods, it correctly triggers any decorators
     * wrapping the base controller.
     */
    default void run (SimulationController.StopCondition condition) {
        run(condition, false);
    }

    /**
     * Default implementation of the simulation loop.
     * By using the interface methods, it correctly triggers any decorators
     * wrapping the base controller.
     */
    default void run(SimulationController.StopCondition condition, boolean progress) {
        scheduleInitialEvents();

        int targetJobs = Integer.MAX_VALUE;
        double targetTime = Double.MAX_VALUE;

        // Get the stop condition
        switch (condition.criteria()) {
            case JOBS_COMPLETED -> targetJobs = getTotalJobsCompleted() + (int) condition.targetValue();
            case TIME_ELAPSED -> targetTime = getClock() + condition.targetValue();
            case QUEUE_EMPTY -> { /* Use default infinite value */ }
        }

        // Simulation loop - calls processNextEvent() which might be decorated
        int lastPrintedPercentage = -1;
        while (getTotalJobsCompleted() < targetJobs && getClock() < targetTime && processNextEvent()) {
            if(!progress) continue;

            double jobProgress = (targetJobs > 0 && targetJobs < Long.MAX_VALUE)
                    ? (double) getTotalJobsCompleted() / targetJobs
                    : 0.0;

            double timeProgress = (targetTime > 0 && targetTime < Double.MAX_VALUE)
                    ? getClock() / targetTime
                    : 0.0;

            double currentProgress = Math.max(jobProgress, timeProgress);
            int currentPercentage = (int) (currentProgress * 100);

            if (currentPercentage > lastPrintedPercentage) {
                printProgressBar(currentPercentage, getTotalJobsCompleted(), getClock());
                lastPrintedPercentage = currentPercentage;
            }
        }
        if(progress) {
            printProgressBar(100, getTotalJobsCompleted(), getClock());
            System.out.println();
        }

        finalizeSimulation();
    }

    /**
     * Resumes or runs the simulation loop for a specific number of additional jobs.
     * Useful for batch means dynamic estimation where the simulation is evaluated in chunks.
     *
     * @param remainingJobs The number of additional jobs to process.
     */
    default void resumableRun(int remainingJobs) {
        int targetJobs = getTotalJobsCompleted() + remainingJobs;
        while (getTotalJobsCompleted() < targetJobs && processNextEvent()) {
            // Process events continuously until the target completed jobs limit is reached
        }
    }

    /**
     * Print a dynamic progress bar on console.
     */
    default void printProgressBar(int percentage, long currentJobs, double currentTime) {
        int barLength = 50;
        int filledLength = (int) (barLength * percentage / 100.0);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("=");
            } else if (i == filledLength) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        // Output format: [====>     ]  50% | Jobs: 50000 | Clock: 120.5s
        String output = String.format("\r%s %3d%% | Jobs: %d | Clock: %.2fs",
                bar.toString(), percentage, currentJobs, currentTime);

        System.out.print(output);
    }
}
