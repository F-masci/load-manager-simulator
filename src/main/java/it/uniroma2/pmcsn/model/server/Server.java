package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.model.Job;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Base abstract class representing a Server node.
 * Supports Processor Sharing (PS) scheduling and processing rate scaling.
 */
public abstract class Server {
    protected final int id;
    protected final int capacity; // Acts as SI_max for Web Servers
    protected double speedMultiplier;
    protected final List<Job> activeJobs = new ArrayList<>();
    protected final Queue<Job> queue = new LinkedList<>(); // Kept for interface consistency, empty in PS mode

    // Statistics and integrals using Welford and TimedWelford
    protected final TimedWelford activeJobsStat = new TimedWelford();
    protected final TimedWelford systemLengthStat = new TimedWelford();
    protected final Welford responseTimeStat = new Welford();

    protected double totalServiceTime = 0.0; // Kept as simple sum for now

    protected Server(int id, int capacity, double speedMultiplier) {
        this.id = id;
        this.capacity = capacity;
        this.speedMultiplier = speedMultiplier;
    }

    public int getId() {
        return id;
    }

    public void setLastEventTime(double lastEventTime) {
        // No longer needed as TimedWelford handles internal timing
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public List<Job> getActiveJobs() {
        return activeJobs;
    }

    public Queue<Job> getQueue() {
        return queue;
    }

    /**
     * Updates time-integrated statistics based on the current clock.
     */
    public void updateStatistics(double currentClock) {
        activeJobsStat.updateToTime(currentClock, activeJobs.size());
        systemLengthStat.updateToTime(currentClock, activeJobs.size() + queue.size());
    }

    /**
     * Simulates the execution of jobs in Processor Sharing for a given time interval.
     * Consumes the remaining service demand of all active jobs.
     *
     * @param elapsed Time interval elapsed
     */
    public void processJobs(double elapsed) {
        if (elapsed <= 0.0 || activeJobs.isEmpty()) {
            return;
        }
        // Under PS, the total CPU speed (speedMultiplier) is shared equally among all active jobs
        double workPerJob = (elapsed * speedMultiplier) / activeJobs.size();
        for (Job job : activeJobs) {
            job.decreaseRemainingDemand(workPerJob);
        }
    }

    /**
     * Accepts a job. Under Processor Sharing, it enters execution immediately if capacity permits.
     *
     * @param job The job to accept
     * @param currentClock The current simulation clock
     * @return The job if accepted, or null if server is at capacity.
     */
    public Job acceptJob(Job job, double currentClock) {
        updateStatistics(currentClock);
        if (activeJobs.size() < capacity) {
            job.setStartTime(currentClock);
            activeJobs.add(job);
            updateStatistics(currentClock);
            return job;
        } else {
            return null;
        }
    }

    /**
     * Completes a specific job under Processor Sharing.
     *
     * @param job The job to complete
     * @param currentClock The current simulation clock
     * @return null (no queue to advance under standard PS)
     */
    public Job completeJob(Job job, double currentClock) {
        updateStatistics(currentClock);
        
        if (!activeJobs.remove(job)) {
            throw new IllegalStateException("Job " + job.getId() + " is not currently active on server " + id);
        }

        job.setCompletionTime(currentClock);
        responseTimeStat.update(job.getResponseTime());
        totalServiceTime += job.getServiceTime();

        updateStatistics(currentClock);
        return null;
    }

    // Getters for statistics
    /**
     * Resets all performance statistics.
     * @param currentClock The current simulation clock.
     */
    public void resetStatistics(double currentClock) {
        activeJobsStat.reset();
        activeJobsStat.updateToTime(currentClock, activeJobs.size());
        
        systemLengthStat.reset();
        systemLengthStat.updateToTime(currentClock, activeJobs.size() + queue.size());
        
        responseTimeStat.reset();
        totalServiceTime = 0.0;
    }

    public double getTimeIntegratedActiveJobs() {
        return activeJobsStat.getMean() * activeJobsStat.getTotalDuration();
    }

    public double getTimeIntegratedQueueLength() {
        return (systemLengthStat.getMean() - activeJobsStat.getMean()) * systemLengthStat.getTotalDuration();
    }

    public double getTimeIntegratedSystemLength() {
        return systemLengthStat.getMean() * systemLengthStat.getTotalDuration();
    }

    public int getCompletedJobsCount() {
        return (int) responseTimeStat.getCount();
    }

    public double getTotalWaitingTime() {
        return 0.0; // Under PS, waiting time is 0 (accepted jobs start immediately)
    }

    public double getTotalResponseTime() {
        return responseTimeStat.getMean() * responseTimeStat.getCount();
    }

    public double getTotalServiceTime() {
        return totalServiceTime;
    }

    public double getAverageUtilization(double totalTime) {
        // In a PS server, average utilization is the average active jobs fraction of capacity
        return activeJobsStat.getMean() / capacity;
    }

    public double getAverageQueueLength(double totalTime) {
        return systemLengthStat.getMean() - activeJobsStat.getMean();
    }

    public double getAverageSystemLength(double totalTime) {
        return systemLengthStat.getMean();
    }

    public double getAverageWaitingTime() {
        return 0.0;
    }

    public double getAverageResponseTime() {
        return responseTimeStat.getMean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return id == server.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", active=" + activeJobs.size() +
                "/" + capacity +
                ", speedMult=" + speedMultiplier +
                '}';
    }
}
