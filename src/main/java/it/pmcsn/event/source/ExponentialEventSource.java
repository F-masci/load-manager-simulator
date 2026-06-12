package it.pmcsn.event.source;

import it.pmcsn.model.Job;

/**
 * An EventSource that generates jobs with standard exponential distributions.
 */
public class ExponentialEventSource extends DistributionEventSource {
    private final double meanInterarrival;
    private final double meanService;

    private static final int ARRIVAL_STREAM = 0;
    private static final int SERVICE_STREAM = 1;

    public ExponentialEventSource(long seed, double meanInterarrival, double meanService) {
        super(seed);
        this.meanInterarrival = meanInterarrival;
        this.meanService = meanService;
    }

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
