package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * An EventSource that generates jobs with hyperexponential distributions
 * for both interarrival and service times.
 */
public class HyperexponentialEventSource extends DistributionEventSource {
    // LCG streams for arrivals
    private static final int ARRIVAL_SELECT_STREAM = 0;
    private static final int ARRIVAL_EXP1_STREAM = 1;
    private static final int ARRIVAL_EXP2_STREAM = 2;

    // LCG streams for services
    private static final int SERVICE_SELECT_STREAM = 3;
    private static final int SERVICE_EXP1_STREAM = 4;
    private static final int SERVICE_EXP2_STREAM = 5;

    // Hyperexponential parameters for arrivals
    private static final double ARRIVAL_P = 0.030331781686;
    private static final double ARRIVAL_M1 = 1.0 / 0.404423755815;
    private static final double ARRIVAL_M2 = 1.0 / 12.928909577518;

    // Hyperexponential parameters for services
    private static final double SERVICE_P = 0.03;
    private static final double SERVICE_M1 = 1.0 / 0.379;
    private static final double SERVICE_M2 = 1.0 / 12.121;

    public HyperexponentialEventSource(long seed) {
        super(seed);
    }

    @Override
    public Job getNextJob(double lastArrivalTime) {
        jobCounter++;

        // Generate interarrival time
        double interarrival = rvgs.hyperExponential(
            ARRIVAL_P, ARRIVAL_M1, ARRIVAL_M2,
            ARRIVAL_SELECT_STREAM, ARRIVAL_EXP1_STREAM, ARRIVAL_EXP2_STREAM
        );
        double arrivalTime = lastArrivalTime + interarrival;

        // Generate service time (job size)
        double serviceTime = rvgs.hyperExponential(
            SERVICE_P, SERVICE_M1, SERVICE_M2,
            SERVICE_SELECT_STREAM, SERVICE_EXP1_STREAM, SERVICE_EXP2_STREAM
        );

        return new Job(jobCounter, arrivalTime, serviceTime);
    }
}
