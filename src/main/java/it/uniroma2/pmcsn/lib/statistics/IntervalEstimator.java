package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.lib.rng.Rvms;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Utility class for interval estimation using Student's T distribution.
 * Calculates confidence intervals for mean values obtained from simulation experiments.
 */
public class IntervalEstimator {
    /** The random variable models used for statistical distributions. */
    private static final Rvms rvms = new Rvms();

    /**
     * Result class encapsulating the confidence interval and its parameters.
     *
     * @param count the number of observations
     * @param mean the sample mean
     * @param stdDev the sample standard deviation
     * @param confidenceLevel the desired confidence level
     * @param halfWidth the calculated half-width of the interval
     * @param lowerBound the lower bound of the confidence interval
     * @param upperBound the upper bound of the confidence interval
     */
    public record IntervalResult(
        long count,
        double mean,
        double stdDev,
        double confidenceLevel,
        double halfWidth,
        double lowerBound,
        double upperBound
    ) {
        /**
         * Returns a string representation of the interval result.
         *
         * @return a formatted string containing mean, standard deviation, half-width, and confidence interval
         */
        @Override
        @NotNull
        public String toString() {
            return String.format("Mean = %.4f, StdDev = %.4f, HW=%.4f, CI = [%.4f, %.4f]",
                    mean, stdDev, halfWidth, lowerBound, upperBound);
        }
    }

    /**
     * Calculates the semi-interval of confidence.
     *
     * @param count           The number of observations.
     * @param stdDev          The sample standard deviation.
     * @param confidenceLevel The desired confidence level (e.g., 0.95).
     * @return The calculated half-width.
     */
    public static double estimateHalfWidth(long count, double stdDev, double confidenceLevel) {
        if (count < 2) {
            return 0.0;
        }

        double alpha = 1.0 - confidenceLevel;
        double quantile = 1.0 - alpha / 2.0;

        double t = rvms.idfStudent(count - 1, quantile);
        double standardError = stdDev / Math.sqrt(count);

        return t * standardError;
    }

    /**
     * Estimates the confidence interval from a list of observations.
     *
     * @param data            The list of observations.
     * @param confidenceLevel The desired confidence level (e.g., 0.95).
     * @return An IntervalResult containing the estimation details.
     */
    public static IntervalResult estimate(List<Double> data, double confidenceLevel) {
        Welford welford = new Welford();
        for (Double value : data)
            welford.update(value);

        return estimate(welford, confidenceLevel);
    }

    /**
     * Estimates the confidence interval from a Welford object.
     *
     * @param welford         A Welford object containing the count, mean, and standard deviation of the observations.
     * @param confidenceLevel The desired confidence level (e.g., 0.95).
     * @return An IntervalResult containing the estimation details.
     */
    public static IntervalResult estimate(Welford welford, double confidenceLevel) {
        return estimate(welford.getCount(), welford.getMean(), welford.getStandardDeviation(), confidenceLevel);
    }

    /**
     * Estimates the confidence interval from pre-calculated statistics.
     *
     * @param count           The number of observations.
     * @param mean            The sample mean.
     * @param stdDev          The sample standard deviation.
     * @param confidenceLevel The desired confidence level (e.g., 0.95).
     * @return An IntervalResult containing the estimation details.
     */
    public static IntervalResult estimate(long count, double mean, double stdDev, double confidenceLevel) {
        double halfWidth = estimateHalfWidth(count, stdDev, confidenceLevel);
        return new IntervalResult(
            count,
            mean,
            stdDev,
            confidenceLevel,
            halfWidth,
            mean - halfWidth,
            mean + halfWidth
        );
    }
}
