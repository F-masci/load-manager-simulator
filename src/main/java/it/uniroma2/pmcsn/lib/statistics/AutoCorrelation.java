package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.List;

/**
 * Utility class for statistical calculations on time series data.
 */
public class AutoCorrelation {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(AutoCorrelation.class, "STATS");

    /**
     * Calculates the stable cutoff lag for the Autocorrelation Function.
     * Optimized to pre-compute mean and variance, delegating the calculation to the internal ACF method.
     *
     * @param data      The time series data.
     * @param threshold The ACF threshold (e.g., 0.05).
     * @return The first lag where ACF remains below the threshold consistently.
     * @throws RuntimeException if no stable cutoff lag is found.
     */
    public static int calculateCutoff(List<Double> data, final double threshold) {
        final int n = data.size();
        if (n == 0) return 0;

        // Pre-compute mean and variance ONCE for the entire dataset (O(n) complexity)
        double mean = calculateMean(data);
        double variance = calculateVariance(data, mean);

        if (variance == 0.0) return 0;

        int consecutiveBelow = 0;
        int candidateLag = -1;
        int maxLag = n / 4;

        final int requiredConsecutiveLags = (int) Math.max(5, Math.round(2.5 * Math.log10(n)));

        logger.info("Evaluating ACF cutoff for {} consecutive lags below threshold", requiredConsecutiveLags);


        for (int lag = 1; lag < maxLag; lag++) {
            String output = String.format("\rCalculating ACF cutoff: lag=%d, consecutiveBelow=%d, candidateLag=%d", lag, consecutiveBelow, candidateLag);
            System.out.print(output);

            // Call the optimized internal ACF method
            double acf = Math.abs(calculateACF(data, lag, mean, variance));

            if (acf < threshold) {
                if (consecutiveBelow == 0) {
                    candidateLag = lag;
                }
                consecutiveBelow++;

                if (consecutiveBelow >= requiredConsecutiveLags) {
                    return candidateLag;
                }
            } else {
                consecutiveBelow = 0;
            }
        }
        System.out.println();

        throw new RuntimeException("No stable cutoff lag found within the threshold.");
    }

    /**
     * Calculates the Autocorrelation Function at a specific lag.
     * Useful for standalone, single-lag evaluations.
     *
     * @param data The time series data.
     * @param lag  The lag for which to calculate the ACF.
     * @return The autocorrelation coefficient at the given lag.
     */
    public static double calculateACF(List<Double> data, int lag) {
        int n = data.size();
        if (n <= lag || n == 0) return 0.0;

        double mean = calculateMean(data);
        double variance = calculateVariance(data, mean);

        if (variance == 0.0) return 0.0;

        // Delegate to the optimized internal engine
        return calculateACF(data, lag, mean, variance);
    }

    /**
     * Calculates the ACF using pre-computed mean and variance.
     * This avoids redundant O(n) computations when called repeatedly within loops.
     */
    private static double calculateACF(List<Double> data, int lag, double mean, double variance) {
        int n = data.size();
        double autocovarianceLagK = 0.0;

        for (int i = 0; i < n - lag; i++) {
            autocovarianceLagK += (data.get(i) - mean) * (data.get(i + lag) - mean);
        }

        return autocovarianceLagK / variance;
    }

    /**
     * Helper method to calculate the mean of a dataset.
     */
    private static double calculateMean(List<Double> data) {
        double sum = 0.0;
        for (double val : data) {
            sum += val;
        }
        return sum / data.size();
    }

    /**
     * Helper method to calculate the variance.
     */
    private static double calculateVariance(List<Double> data, double mean) {
        double variance = 0.0;
        for (double val : data) {
            variance += (val - mean) * (val - mean);
        }
        return variance;
    }
}