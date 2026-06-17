package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * Event source generating jobs with exponentially distributed interarrival and service times.
 */
public class ExponentialEventSource extends DistributionEventSource {
    private final double meanInterarrival;
    private final double meanService;

    private static final int ARRIVAL_STREAM = 0;
    private static final int SERVICE_STREAM = 1;

    /**
     * Initializes the source with means and a random seed.
     *
     * @param seed the seed for random generation
     * @param meanInterarrival the expected value for interarrival times
     * @param meanService the expected value for service times
     */
    public ExponentialEventSource(long seed, double meanInterarrival, double meanService) {
        super(seed);
        this.meanInterarrival = meanInterarrival;
        this.meanService = meanService;
    }

    /**
     * Generates the next job using exponential distributions.
     *
     * @param lastArrivalTime the arrival time of the previous job
     * @return the generated job
     */
    @Override
    public Job getNextJob(double lastArrivalTime) {
        jobCounter++;

        // Generate next arrival time
        rngs.selectStream(ARRIVAL_STREAM);
        double interarrival = rvgs.exponential(meanInterarrival);
        double arrivalTime = lastArrivalTime + interarrival;

        // Generate service time
        rngs.selectStream(SERVICE_STREAM);
        double serviceTime = rvgs.exponential(meanService);

        return new Job(jobCounter, arrivalTime, serviceTime);
    }
}
