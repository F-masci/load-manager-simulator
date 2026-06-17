package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.SimulationConsoleUtils;

/**
 * Interface for the simulation engine, supporting decorators for metrics collection and state monitoring.
 */
public interface Simulator {
    /** Logger for simulation engine events. */
    LogFactory.ModuleLogger logger = LogFactory.getLogger(Simulator.class, "SIM");

    /**
     * Processes the next event in the queue.
     *
     * @return true if an event was processed, false if the queue is empty
     */
    boolean processNextEvent();

    /**
     * Resets all accumulated statistics and counters.
     */
    void resetStatistics();

    /**
     * Schedules the initial events to start the simulation.
     */
    void scheduleInitialEvents();

    /**
     * Performs final cleanup and data consolidation after the simulation loop ends.
     */
    void finalizeSimulation();
    
    /**
     * Gets the current simulation clock time.
     *
     * @return current time
     */
    double getClock();

    /**
     * Gets the total number of jobs that have arrived at the system.
     *
     * @return total arrived jobs
     */
    int getTotalJobsArrived();

    /**
     * Gets the total number of jobs diverted to the spike server.
     *
     * @return total diverted jobs
     */
    int getTotalJobsDiverted();

    /**
     * Gets the total number of jobs that have completed service.
     *
     * @return total completed jobs
     */
    int getTotalJobsCompleted();

    /**
     * Gets the average response time of completed jobs.
     *
     * @return average response time
     */
    double getAverageResponseTime();

    /**
     * Gets the average number of jobs in the system.
     *
     * @return average jobs in system
     */
    double getAverageJobsInSystem();

    /**
     * Gets the overall system utilization.
     *
     * @return system utilization
     */
    double getSystemUtilization();

    /**
     * Gets the system throughput (jobs completed per unit time).
     *
     * @return system throughput
     */
    double getThroughput();
    
    /**
     * Gets the web server cluster instance.
     *
     * @return web server cluster
     */
    WebServerCluster getWebServerCluster();

    /**
     * Gets the spike server instance.
     *
     * @return spike server
     */
    SpikeServer getSpikeServer();

    /**
     * Gets the load manager instance.
     *
     * @return load manager
     */
    LoadManager getLoadManager();

    /**
     * Gets the random number generator seed used.
     *
     * @return simulation seed
     */
    long getSeed();

    /**
     * Gets the type of the last event processed.
     *
     * @return last event type
     */
    EventType getLastEventType();

    /**
     * Runs the simulation loop until a stop condition is met.
     *
     * @param condition criteria to stop the simulation
     */
    default void run (SimulationController.StopCondition condition) {
        run(condition, false);
    }

    /**
     * Runs the simulation loop until a stop condition is met, optionally showing progress.
     *
     * @param condition criteria to stop the simulation
     * @param progress true to show a progress bar in console
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
                SimulationConsoleUtils.printJobProgressBar(currentPercentage, getTotalJobsCompleted(), getClock());
                lastPrintedPercentage = currentPercentage;
            }
        }
        if(progress) {
            SimulationConsoleUtils.printJobProgressBar(100, getTotalJobsCompleted(), getClock());
            System.out.println();
        }

        finalizeSimulation();
    }

    /**
     * Resumes or runs the simulation loop for a specific number of additional jobs.
     *
     * @param remainingJobs number of additional jobs to process
     */
    default void resumableRun(int remainingJobs) {
        int targetJobs = getTotalJobsCompleted() + remainingJobs;
        while (getTotalJobsCompleted() < targetJobs && processNextEvent()) {
            // Process events continuously until the target completed jobs limit is reached
        }
    }

}
