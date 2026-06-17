package it.uniroma2.pmcsn.lib.statistics;

/**
 * An implementation of Welford's algorithm for online calculation of running mean
 * and variance of a sequence of observations.
 */
public class Welford {
    /** The number of observations. */
    private long count = 0;
    /** The running mean of the observations. */
    private double mean = 0.0;
    /** The running sum of squares of differences from the mean. */
    private double m2 = 0.0;

    /**
     * Adds a new observation to the running statistics.
     *
     * @param x the observed value
     */
    public void update(double x) {
        count++;
        double delta = x - mean;
        mean += delta / count;
        double delta2 = x - mean;
        m2 += delta * delta2;
    }

    /**
     * Gets the number of observations.
     *
     * @return the count of observations
     */
    public long getCount() {
        return count;
    }

    /**
     * Gets the running mean of the observations.
     *
     * @return the running mean, or 0.0 if no observations have been added
     */
    public double getMean() {
        return mean;
    }

    /**
     * Gets the running sample variance (divided by count - 1).
     *
     * @return the sample variance, or 0.0 if count < 2
     */
    public double getVariance() {
        if (count < 2) {
            return 0.0;
        }
        return m2 / (count - 1);
    }

    /**
     * Gets the running population variance (divided by count).
     *
     * @return the population variance, or 0.0 if count < 1
     */
    public double getPopulationVariance() {
        if (count < 1) {
            return 0.0;
        }
        return m2 / count;
    }

    /**
     * Gets the running sample standard deviation.
     *
     * @return the sample standard deviation
     */
    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

    /**
     * Gets the running population standard deviation.
     *
     * @return the population standard deviation
     */
    public double getPopulationStandardDeviation() {
        return Math.sqrt(getPopulationVariance());
    }

    /**
     * Resets the accumulator to its initial state.
     */
    public void reset() {
        count = 0;
        mean = 0.0;
        m2 = 0.0;
    }
}
