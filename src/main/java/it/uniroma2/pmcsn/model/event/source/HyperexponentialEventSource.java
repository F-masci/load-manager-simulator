package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * Event source generating jobs with hyperexponentially distributed times.
 * Parameters are computed from mean and coefficient of variation using balanced means.
 */
public class HyperexponentialEventSource extends DistributionEventSource {
    // Streams for arrivals
    private static final int ARRIVAL_SELECT_STREAM = 0;
    private static final int ARRIVAL_EXP1_STREAM = 1;
    private static final int ARRIVAL_EXP2_STREAM = 2;

    // Streams for services
    private static final int SERVICE_SELECT_STREAM = 3;
    private static final int SERVICE_EXP1_STREAM = 4;
    private static final int SERVICE_EXP2_STREAM = 5;

    // Computed parameters for arrivals
    private final double arrivalP;
    private final double arrivalM1;
    private final double arrivalM2;

    // Computed parameters for services
    private final double serviceP;
    private final double serviceM1;
    private final double serviceM2;

    /**
     * Initializes the source with default coefficient of variation.
     *
     * @param seed the seed for random generation
     * @param arrivalMean mean interarrival time
     * @param serviceMean mean service time
     */
    public HyperexponentialEventSource(long seed, double arrivalMean, double serviceMean) {
        this(seed, arrivalMean, 2.0, serviceMean, 2.0);
    }

    /**
     * Initializes the source with specified means and coefficients of variation.
     *
     * @param seed the seed for random generation
     * @param arrivalMean mean interarrival time
     * @param arrivalCv coefficient of variation for arrivals
     * @param serviceMean mean service time
     * @param serviceCv coefficient of variation for services
     */
    public HyperexponentialEventSource(long seed, double arrivalMean, double arrivalCv, double serviceMean, double serviceCv) {
        super(seed);

        // Hyperexponential distribution always requires CV >= 1.0
        if (arrivalCv < 1.0 || serviceCv < 1.0) {
            throw new IllegalArgumentException("The Coefficient of Variation (CV) must be >= 1.0 for the Hyperexponential distribution.");
        }

        // Compute parameters using Balanced Means for arrivals
        double[] arrivalParams = computeBalancedMeans(arrivalMean, arrivalCv);
        this.arrivalP = arrivalParams[0];
        this.arrivalM1 = arrivalParams[1];
        this.arrivalM2 = arrivalParams[2];

        // Compute parameters using Balanced Means for services
        double[] serviceParams = computeBalancedMeans(serviceMean, serviceCv);
        this.serviceP = serviceParams[0];
        this.serviceM1 = serviceParams[1];
        this.serviceM2 = serviceParams[2];
    }

    /**
     * Computes hyperexponential parameters using the Balanced Means method.
     *
     * @param mean the target mean
     * @param cv the target coefficient of variation
     * @return array containing probability p and means m1, m2
     */
    private double[] computeBalancedMeans(double mean, double cv) {
        double cv2 = cv * cv;

        // Compute probability 'p' (using the negative root to get p < 0.5)
        double p = 0.5 * (1.0 - Math.sqrt((cv2 - 1.0) / (cv2 + 1.0)));

        // Compute the means of the two exponential branches
        double m1 = mean / (2.0 * p);
        double m2 = mean / (2.0 * (1.0 - p));

        return new double[]{p, m1, m2};
    }

    /**
     * Generates the next job using hyperexponential distributions.
     *
     * @param lastArrivalTime the arrival time of the previous job
     * @return the generated job
     */
    @Override
    public Job getNextJob(double lastArrivalTime) {
        jobCounter++;

        // Generate interarrival time
        double interarrival = rvgs.hyperExponential(
            arrivalP, arrivalM1, arrivalM2,
            ARRIVAL_SELECT_STREAM, ARRIVAL_EXP1_STREAM, ARRIVAL_EXP2_STREAM
        );
        double arrivalTime = lastArrivalTime + interarrival;

        // Generate service time (job size)
        double serviceTime = rvgs.hyperExponential(
            serviceP, serviceM1, serviceM2,
            SERVICE_SELECT_STREAM, SERVICE_EXP1_STREAM, SERVICE_EXP2_STREAM
        );

        return new Job(jobCounter, arrivalTime, serviceTime);
    }
}