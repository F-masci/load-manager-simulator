package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * An EventSource that generates jobs with hyperexponential distributions
 * for both interarrival and service times.
 * Parameters are automatically computed from Mean and Coefficient of Variation.
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
     * Constructor that accepts Mean for arrivals and services.
     *
     * @param seed        The seed for the random number generators
     * @param arrivalMean Mean interarrival time
     * @param serviceMean Mean service time
     */
    public HyperexponentialEventSource(long seed, double arrivalMean, double serviceMean) {
        this(seed, arrivalMean, 2.0, serviceMean, 2.0);
    }


        /**
         * Constructor that accepts Mean and CV for arrivals and services.
         *
         * @param seed        The seed for the random number generators
         * @param arrivalMean Mean interarrival time
         * @param arrivalCv   Coefficient of variation for arrivals (must be >= 1.0)
         * @param serviceMean Mean service time
         * @param serviceCv   Coefficient of variation for services (must be >= 1.0)
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
     * Helper method to compute p, m1, and m2 using the Balanced Means method.
     *
     * The Balanced Means method calculates the parameters such that the probability
     * of choosing a branch is inversely proportional to its mean: p * m1 = (1 - p) * m2.
     *
     * Formulas used:
     * p  = 0.5 * (1 - sqrt((CV^2 - 1) / (CV^2 + 1)))
     * m1 = Mean / (2 * p)
     * m2 = Mean / (2 * (1 - p))
     *
     * @param mean The expected value (mean)
     * @param cv   The coefficient of variation (must be >= 1.0)
     * @return An array containing [p, m1, m2]
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