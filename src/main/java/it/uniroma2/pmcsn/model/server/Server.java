package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.model.Job;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Base abstract class representing a Server node.
 * Supports Processor Sharing (PS) scheduling and processing rate scaling.
 * Servers have infinite capacity (infinite queue).
 */
public abstract class Server {
    protected final int id;
    protected double speedMultiplier;
    protected final List<Job> activeJobs = new ArrayList<>();
    protected final Queue<Job> queue = new LinkedList<>(); // Kept for interface consistency, empty in PS mode

    // Statistics and integrals using Welford and TimedWelford
    protected final TimedWelford activeJobsStat = new TimedWelford();
    protected final TimedWelford systemLengthStat = new TimedWelford();
    protected final TimedWelford busyStat = new TimedWelford();
    protected final Welford responseTimeStat = new Welford();

    protected double totalServiceTime = 0.0; // Kept as simple sum for now

    /**
     * Constructs a Server with a given ID and speed multiplier.
     *
     * @param id the unique identifier of the server
     * @param speedMultiplier the processing rate multiplier
     */
    protected Server(int id, double speedMultiplier) {
        this.id = id;
        this.speedMultiplier = speedMultiplier;
    }

    /**
     * Returns the server ID.
     *
     * @return the server identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the speed multiplier for the server.
     *
     * @param speedMultiplier the new speed multiplier value
     */
    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    /**
     * Returns the current speed multiplier.
     *
     * @return the speed multiplier value
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Returns the list of currently active jobs in execution.
     *
     * @return the list of active jobs
     */
    public List<Job> getActiveJobs() {
        return activeJobs;
    }

    /**
     * Returns the job queue (empty in PS mode).
     *
     * @return the job queue
     */
    public Queue<Job> getQueue() {
        return queue;
    }

    /**
     * Updates time-integrated statistics based on the current clock.
     *
     * @param currentClock current simulation time
     */
    public void updateStatistics(double currentClock) {
        activeJobsStat.updateToTime(currentClock, activeJobs.size());
        systemLengthStat.updateToTime(currentClock, activeJobs.size() + queue.size());
        busyStat.updateToTime(currentClock, activeJobs.isEmpty() ? 0.0 : 1.0);
    }

    /**
     * Simulates the execution of jobs in Processor Sharing for a given time interval.
     * Consumes the remaining service demand of all active jobs.
     *
     * @param elapsed time interval elapsed
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
     * Accepts a job. Under Processor Sharing with infinite queue, it enters execution immediately.
     *
     * @param job the job to accept
     * @param currentClock current simulation clock
     * @return the accepted job
     */
    public Job acceptJob(Job job, double currentClock) {
        updateStatistics(currentClock);
        job.setStartTime(currentClock);
        activeJobs.add(job);
        updateStatistics(currentClock);
        return job;
    }

    /**
     * Completes a specific job under Processor Sharing.
     *
     * @param job the job to complete
     * @param currentClock current simulation clock
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

    /**
     * Resets all performance statistics.
     *
     * @param currentClock current simulation clock
     */
    public void resetStatistics(double currentClock) {
        activeJobsStat.reset();
        activeJobsStat.updateToTime(currentClock, activeJobs.size());
        
        systemLengthStat.reset();
        systemLengthStat.updateToTime(currentClock, activeJobs.size() + queue.size());
        
        busyStat.reset();
        busyStat.updateToTime(currentClock, activeJobs.isEmpty() ? 0.0 : 1.0);

        responseTimeStat.reset();
        totalServiceTime = 0.0;
    }

    /**
     * Returns the time-integrated average of active jobs.
     *
     * @return the area under the active jobs curve
     */
    public double getTimeIntegratedActiveJobs() {
        return activeJobsStat.getMean() * activeJobsStat.getTotalDuration();
    }

    /**
     * Returns the time-integrated average of the queue length.
     *
     * @return the area under the queue length curve
     */
    public double getTimeIntegratedQueueLength() {
        return (systemLengthStat.getMean() - activeJobsStat.getMean()) * systemLengthStat.getTotalDuration();
    }

    /**
     * Returns the time-integrated average of the system length.
     *
     * @return the area under the system length curve
     */
    public double getTimeIntegratedSystemLength() {
        return systemLengthStat.getMean() * systemLengthStat.getTotalDuration();
    }

    /**
     * Returns the number of completed jobs.
     *
     * @return the completed jobs count
     */
    public int getCompletedJobsCount() {
        return (int) responseTimeStat.getCount();
    }

    /**
     * Returns the total waiting time (always zero in PS).
     *
     * @return the total waiting time
     */
    public double getTotalWaitingTime() {
        return 0.0; // Under PS, waiting time is 0 (accepted jobs start immediately)
    }

    /**
     * Returns the sum of all job response times.
     *
     * @return the total response time
     */
    public double getTotalResponseTime() {
        return responseTimeStat.getMean() * responseTimeStat.getCount();
    }

    /**
     * Returns the sum of all job service times.
     *
     * @return the total service time
     */
    public double getTotalServiceTime() {
        return totalServiceTime;
    }

    /**
     * Returns the average server utilization.
     *
     * @param totalTime total simulation time (unused, kept for consistency)
     * @return the average utilization
     */
    public double getAverageUtilization(double totalTime) {
        // Utilization is the fraction of time the server was busy (activeJobs > 0)
        return busyStat.getMean();
    }

    /**
     * Returns the average queue length over time.
     *
     * @param totalTime total simulation time (unused, kept for consistency)
     * @return the average queue length
     */
    public double getAverageQueueLength(double totalTime) {
        return systemLengthStat.getMean() - activeJobsStat.getMean();
    }

    /**
     * Returns the average system length over time.
     *
     * @param totalTime total simulation time (unused, kept for consistency)
     * @return the average system length
     */
    public double getAverageSystemLength(double totalTime) {
        return systemLengthStat.getMean();
    }

    /**
     * Returns the average waiting time per job.
     *
     * @return the average waiting time
     */
    public double getAverageWaitingTime() {
        return 0.0;
    }

    /**
     * Returns the average response time per job.
     *
     * @return the average response time
     */
    public double getAverageResponseTime() {
        return responseTimeStat.getMean();
    }

    /**
     * Checks if this server is equal to another object based on ID.
     *
     * @param o the object to compare
     * @return true if IDs match, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return id == server.id;
    }

    /**
     * Generates a hash code based on server ID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of the server.
     *
     * @return server details as string
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", active=" + activeJobs.size() +
                ", speedMult=" + speedMultiplier +
                '}';
    }
}
