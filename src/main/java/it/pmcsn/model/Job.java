package it.pmcsn.model;

/**
 * Represents a request (job) in the simulation.
 * Tracks arrival time, service time, remaining service demand, start time, and completion time.
 */
public class Job {
    private final int id;
    private final double arrivalTime;
    private final double serviceTime;
    private double remainingServiceDemand;
    private double startTime = -1.0;
    private double completionTime = -1.0;

    public Job(int id, double arrivalTime, double serviceTime) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
        this.remainingServiceDemand = serviceTime;
    }

    public int getId() {
        return id;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getServiceTime() {
        return serviceTime;
    }

    public double getRemainingServiceDemand() {
        return remainingServiceDemand;
    }

    public void setRemainingServiceDemand(double remainingServiceDemand) {
        this.remainingServiceDemand = remainingServiceDemand;
    }

    /**
     * Decreases the remaining service demand by a given amount.
     */
    public void decreaseRemainingDemand(double amount) {
        this.remainingServiceDemand -= amount;
        if (this.remainingServiceDemand < 0.0) {
            this.remainingServiceDemand = 0.0;
        }
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(double completionTime) {
        this.completionTime = completionTime;
    }

    public double getWaitingTime() {
        if (startTime < 0) return 0.0;
        return startTime - arrivalTime;
    }

    public double getResponseTime() {
        if (completionTime < 0) return 0.0;
        return completionTime - arrivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return id == job.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

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
