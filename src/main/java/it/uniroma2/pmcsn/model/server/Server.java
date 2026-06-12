package it.uniroma2.pmcsn.model.server;

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

    // Statistics and integrals
    protected double lastEventTime = 0.0;
    protected double timeIntegratedActiveJobs = 0.0;
    protected double timeIntegratedQueueLength = 0.0;
    protected double timeIntegratedSystemLength = 0.0;

    protected int completedJobsCount = 0;
    protected double totalWaitingTime = 0.0;
    protected double totalResponseTime = 0.0;
    protected double totalServiceTime = 0.0;

    protected Server(int id, int capacity, double speedMultiplier) {
        this.id = id;
        this.capacity = capacity;
        this.speedMultiplier = speedMultiplier;
    }

    public int getId() {
        return id;
    }

    public void setLastEventTime(double lastEventTime) {
        this.lastEventTime = lastEventTime;
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
        double duration = currentClock - lastEventTime;
        if (duration > 0) {
            timeIntegratedActiveJobs += duration * activeJobs.size();
            timeIntegratedQueueLength += duration * queue.size();
            timeIntegratedSystemLength += duration * (activeJobs.size() + queue.size());
            lastEventTime = currentClock;
        }
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
        completedJobsCount++;
        totalWaitingTime += job.getWaitingTime(); // 0 under PS
        totalResponseTime += job.getResponseTime();
        totalServiceTime += job.getServiceTime();

        return null;
    }

    // Getters for statistics
    public double getTimeIntegratedActiveJobs() {
        return timeIntegratedActiveJobs;
    }

    public double getTimeIntegratedQueueLength() {
        return timeIntegratedQueueLength;
    }

    public double getTimeIntegratedSystemLength() {
        return timeIntegratedSystemLength;
    }

    public int getCompletedJobsCount() {
        return completedJobsCount;
    }

    public double getTotalWaitingTime() {
        return totalWaitingTime;
    }

    public double getTotalResponseTime() {
        return totalResponseTime;
    }

    public double getTotalServiceTime() {
        return totalServiceTime;
    }

    public double getAverageUtilization(double totalTime) {
        if (totalTime <= 0) return 0.0;
        // In a PS server, average utilization is the average active jobs fraction of capacity
        return timeIntegratedActiveJobs / (totalTime * capacity);
    }

    public double getAverageQueueLength(double totalTime) {
        if (totalTime <= 0) return 0.0;
        return timeIntegratedQueueLength / totalTime;
    }

    public double getAverageSystemLength(double totalTime) {
        if (totalTime <= 0) return 0.0;
        return timeIntegratedSystemLength / totalTime;
    }

    public double getAverageWaitingTime() {
        if (completedJobsCount == 0) return 0.0;
        return totalWaitingTime / completedJobsCount;
    }

    public double getAverageResponseTime() {
        if (completedJobsCount == 0) return 0.0;
        return totalResponseTime / completedJobsCount;
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
