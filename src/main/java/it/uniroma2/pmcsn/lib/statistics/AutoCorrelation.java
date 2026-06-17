package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.LogFactory.ModuleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for statistical calculations on time series data.
 * Provides basic statistics (mean, stdDev, percentile) and Autocorrelation analysis.
 */
public class AutoCorrelation {
    /** The logger instance for statistical operations. */
    private static final ModuleLogger logger = LogFactory.getLogger(AutoCorrelation.class, "STATS");

    /**
     * Calculates the mean of a dataset.
     *
     * @param data the data series
     * @return the mean value
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
     * Calculates the standard deviation of a dataset.
     *
     * @param data the data series
     * @param mean the pre-computed mean
     * @return the standard deviation
     */
    public static double calculateStdDev(List<Double> data, double mean) {
        if (data == null || data.isEmpty()) return 0.0;
        double ss = calculateSumOfSquares(data, mean);
        return Math.sqrt(ss / data.size());
    }

    /**
     * Calculates the N-th percentile of a dataset.
     *
     * @param data the data series
     * @param percentile the percentile to calculate
     * @return the percentile value
     */
    public static double calculatePercentile(List<Double> data, double percentile) {
        if (data == null || data.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(data);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    /**
     * Calculates the stable cutoff lag for the autocorrelation function.
     *
     * @param data the time series data
     * @param threshold the acf threshold
     * @return the first lag where acf remains below the threshold
     */
    public static int calculateCutoff(List<Double> data, final double threshold) {
        final int n = data.size();
        if (n == 0) return 0;

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
     * Calculates the autocorrelation function at a specific lag.
     *
     * @param data the time series data
     * @param lag the lag for the calculation
     * @return the autocorrelation coefficient
     */
    public static double calculateACF(List<Double> data, int lag) {
        int n = data.size();
        if (n <= lag || n == 0) return 0.0;

        double mean = calculateMean(data);
        double ss = calculateSumOfSquares(data, mean);

        if (ss == 0.0) return 0.0;

        return calculateACF(data, lag, mean, ss);
    }

    /**
     * Internal acf calculation with pre-computed statistics.
     *
     * @param data the data series
     * @param lag the lag for the calculation
     * @param mean the pre-computed mean
     * @param ss the pre-computed sum of squares
     * @return the autocorrelation coefficient
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
     * Calculates the sum of squared deviations from the mean.
     *
     * @param data the data series
     * @param mean the mean value
     * @return the sum of squares
     */
    private static double calculateSumOfSquares(List<Double> data, double mean) {
        double ss = 0.0;
        for (double val : data) {
            ss += (val - mean) * (val - mean);
        }
        return ss;
    }
}
