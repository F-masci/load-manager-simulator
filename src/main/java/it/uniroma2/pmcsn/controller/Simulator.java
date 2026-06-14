package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Interface for the simulation engine, allowing decorators.
 */
public interface Simulator {
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
    default void run(SimulationController.StopCondition condition) {
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
        while (getTotalJobsCompleted() < targetJobs && getClock() < targetTime && processNextEvent());

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
}
