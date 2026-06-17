package it.uniroma2.pmcsn.model;

import java.util.Objects;

/**
 * Represents a request in the simulation with tracking for timing and service demand.
 */
public class Job {
    private final int id;
    private final double arrivalTime;
    private final double serviceTime;
    private double remainingServiceDemand;
    private double startTime = -1.0;
    private double completionTime = -1.0;

    /**
     * Initializes a job with its identity and service requirements.
     *
     * @param id the unique job identifier
     * @param arrivalTime the time the job entered the system
     * @param serviceTime the total service demand required
     */
    public Job(int id, double arrivalTime, double serviceTime) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
        this.remainingServiceDemand = serviceTime;
    }

    /**
     * Gets the job identifier.
     *
     * @return the job id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the arrival time of the job.
     *
     * @return the arrival time
     */
    public double getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Gets the original service demand.
     *
     * @return the service time
     */
    public double getServiceTime() {
        return serviceTime;
    }

    /**
     * Gets the remaining service demand.
     *
     * @return the remaining demand
     */
    public double getRemainingServiceDemand() {
        return remainingServiceDemand;
    }

    /**
     * Sets the remaining service demand.
     *
     * @param remainingServiceDemand the new remaining demand
     */
    public void setRemainingServiceDemand(double remainingServiceDemand) {
        this.remainingServiceDemand = remainingServiceDemand;
    }

    /**
     * Decreases the remaining service demand by a given amount, clamped to zero.
     *
     * @param amount the amount to decrease
     */
    public void decreaseRemainingDemand(double amount) {
        this.remainingServiceDemand -= amount;
        if (this.remainingServiceDemand < 0.0) {
            this.remainingServiceDemand = 0.0;
        }
    }

    /**
     * Gets the time service started for this job.
     *
     * @return the start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Sets the time service started for this job.
     *
     * @param startTime the start time
     */
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the time the job was completed.
     *
     * @return the completion time
     */
    public double getCompletionTime() {
        return completionTime;
    }

    /**
     * Sets the time the job was completed.
     *
     * @param completionTime the completion time
     */
    public void setCompletionTime(double completionTime) {
        this.completionTime = completionTime;
    }

    /**
     * Calculates the time spent waiting in queues.
     *
     * @return the waiting time
     */
    public double getWaitingTime() {
        if (startTime < 0) return 0.0;
        return startTime - arrivalTime;
    }

    /**
     * Calculates the total time spent in the system.
     *
     * @return the response time
     */
    public double getResponseTime() {
        if (completionTime < 0) return 0.0;
        return completionTime - arrivalTime;
    }

    /**
     * Checks equality based on job identifier.
     *
     * @param o the object to compare
     * @return true if identifiers match
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return id == job.id;
    }

    /**
     * Generates hash code based on job identifier.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of the job state.
     *
     * @return formatted string
     */
    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", arrivalTime=" + arrivalTime +
                ", serviceTime=" + serviceTime +
                ", remaining=" + remainingServiceDemand +
                ", startTime=" + startTime +
                ", completionTime=" + completionTime +
                '}';
    }
}
