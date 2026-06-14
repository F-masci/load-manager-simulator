package it.uniroma2.pmcsn.lib.statistics;

import java.util.List;

/**
 * Utility class for statistical calculations on time series data.
 */
public class AutoCorrelation {

    public static int calculateCutoff(List<Double> data, final double threshold) {
        final int n = data.size();

        for (int lag = 1; lag < n / 4; lag++) {
            double acf = Math.abs(AutoCorrelation.calculateACF(data, lag));
            if (acf < threshold) {
                return lag;
            }
        }

        throw new RuntimeException("No cutoff lag found");
    }

    /**
     * Calculates the Autocorrelation Function (ACF) at a specific lag.
     * 
     * @param data The time series data.
     * @param lag  The lag for which to calculate the ACF.
     * @return The autocorrelation coefficient at the given lag.
     */
    public static double calculateACF(List<Double> data, int lag) {
        int n = data.size();
        if (n <= lag || n == 0) return 0.0;

        double mean = 0.0;
        for (double val : data) {
            mean += val;
        }
        mean /= n;

        double autocovarianceLag0 = 0.0; // Variance
        double autocovarianceLagK = 0.0;

        for (int i = 0; i < n; i++) {
            autocovarianceLag0 += (data.get(i) - mean) * (data.get(i) - mean);
        }
        
        if (autocovarianceLag0 == 0.0) return 0.0; // All elements are identical

        for (int i = 0; i < n - lag; i++) {
            autocovarianceLagK += (data.get(i) - mean) * (data.get(i + lag) - mean);
        }

        return autocovarianceLagK / autocovarianceLag0;
    }
}
