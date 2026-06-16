package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for statistical calculations on time series data.
 * Provides basic statistics (mean, stdDev, percentile) and Autocorrelation analysis.
 */
public class AutoCorrelation {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(AutoCorrelation.class, "STATS");

    /**
     * Calculates the mean of a dataset.
     */
    public static double calculateMean(List<Double> data) {
        if (data == null || data.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double val : data) {
            sum += val;
        }
        return sum / data.size();
    }

    /**
     * Calculates the standard deviation given a pre-computed mean.
     */
    public static double calculateStdDev(List<Double> data, double mean) {
        if (data == null || data.isEmpty()) return 0.0;
        double ss = calculateSumOfSquares(data, mean);
        return Math.sqrt(ss / data.size());
    }

    /**
     * Calculates the N-th percentile of a dataset.
     * Uses the "nearest rank" approach without interpolation.
     */
    public static double calculatePercentile(List<Double> data, double percentile) {
        if (data == null || data.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(data);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    /**
     * Calculates the stable cutoff lag for the Autocorrelation Function.
     * Optimized to pre-compute mean and sum of squares, delegating the calculation to the internal ACF method.
     *
     * @param data      The time series data.
     * @param threshold The ACF threshold (e.g., 0.05).
     * @return The first lag where ACF remains below the threshold consistently.
     * @throws RuntimeException if no stable cutoff lag is found.
     */
    public static int calculateCutoff(List<Double> data, final double threshold) {
        final int n = data.size();
        if (n == 0) return 0;

        // Pre-compute mean and SS ONCE for the entire dataset to avoid redundant O(n) calculations in the loop
        double mean = calculateMean(data);
        double ss = calculateSumOfSquares(data, mean);

        if (ss == 0.0) return 0;

        int consecutiveBelow = 0;
        int candidateLag = -1;
        int maxLag = n / 4;

        final int requiredConsecutiveLags = (int) Math.max(5, Math.round(2.5 * Math.log10(n)));

        logger.info("Evaluating ACF cutoff for {} consecutive lags below threshold", requiredConsecutiveLags);


        for (int lag = 1; lag < maxLag; lag++) {
            String output = String.format("\rCalculating ACF cutoff: lag=%d, consecutiveBelow=%d, candidateLag=%d", lag, consecutiveBelow, candidateLag);
            System.out.print(output);

            // Call the optimized internal ACF method
            double acf = Math.abs(calculateACF(data, lag, mean, ss));

            if (acf < threshold) {
                if (consecutiveBelow == 0) {
                    candidateLag = lag;
                }
                consecutiveBelow++;

                if (consecutiveBelow >= requiredConsecutiveLags) {
                    System.out.println();
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
        double ss = calculateSumOfSquares(data, mean);

        if (ss == 0.0) return 0.0;

        // Delegate to the optimized internal engine
        return calculateACF(data, lag, mean, ss);
    }

    /**
     * Calculates the ACF using pre-computed mean and sum of squares.
     * This avoids redundant O(n) computations when called repeatedly within loops.
     */
    private static double calculateACF(List<Double> data, int lag, double mean, double ss) {
        int n = data.size();
        double autocovarianceLagK = 0.0;

        for (int i = 0; i < n - lag; i++) {
            autocovarianceLagK += (data.get(i) - mean) * (data.get(i + lag) - mean);
        }

        return autocovarianceLagK / ss;
    }

    /**
     * Helper method to calculate the sum of squared deviations from the mean.
     */
    private static double calculateSumOfSquares(List<Double> data, double mean) {
        double ss = 0.0;
        for (double val : data) {
            ss += (val - mean) * (val - mean);
        }
        return ss;
    }
}
