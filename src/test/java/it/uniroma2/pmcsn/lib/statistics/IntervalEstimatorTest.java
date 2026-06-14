package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for IntervalEstimator, verifying confidence interval calculations.
 */
public class IntervalEstimatorTest extends BaseTest {

    /**
     * Verifies the half-width calculation using known values from a t-distribution table.
     * <p>
     * Formula: HW = t_{n-1, 1-α/2} * (s / sqrt(n))
     * Given: n=5 (df=4), confidence=0.95 (α=0.05), stdDev=1.0
     * t_{4, 0.975} ≈ 2.7764
     * HW = 2.7764 * (1.0 / sqrt(5)) ≈ 2.7764 * 0.4472 ≈ 1.2416
     */
    @Test
    public void testHalfWidthCalculationWithKnownValues() {
        logTestStep("Testing Half-Width calculation with N=5, stdDev=1.0, confidence=0.95");
        double count = 5;
        double stdDev = 1.0;
        double confidence = 0.95;
        
        double halfWidth = IntervalEstimator.estimateHalfWidth((long)count, stdDev, confidence);
        logDebug("Calculated Half-Width: {}", halfWidth);
        assertEquals(1.2416, halfWidth, 1e-4);
    }

    /**
     * Verifies the encapsulation of interval results and boundary calculations.
     * <p>
     * Given: n=10 (df=9), mean=100.0, stdDev=15.0, confidence=0.90 (α=0.10)
     * t_{9, 0.95} ≈ 1.8331
     * HW = 1.8331 * (15.0 / sqrt(10)) ≈ 1.8331 * 4.7434 ≈ 8.6952
     * Bounds: [100 - 8.6952, 100 + 8.6952] = [91.3048, 108.6952]
     */
    @Test
    public void testIntervalResultEncapsulation() {
        logTestStep("Testing IntervalResult encapsulation with N=10, mean=100.0, stdDev=15.0, confidence=0.90");
        long count = 10;
        double mean = 100.0;
        double stdDev = 15.0;
        double confidence = 0.90;
        
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(count, mean, stdDev, confidence);
        
        logDebug("Interval Result: {}", result);
        assertEquals(count, result.count());
        assertEquals(mean, result.mean());
        assertEquals(stdDev, result.stdDev());
        assertEquals(confidence, result.confidenceLevel());
        assertEquals(8.6952, result.halfWidth(), 1e-4);
        assertEquals(mean - 8.6952, result.lowerBound(), 1e-4);
        assertEquals(mean + 8.6952, result.upperBound(), 1e-4);
    }

    /**
     * Verifies estimation when providing a list of raw data points.
     * <p>
     * Data: {10, 20, 30, 40, 50}
     * n=5, mean=30.0
     * S² = Σ(xi - mean)² / (n-1) = 1000 / 4 = 250
     * s = sqrt(250) ≈ 15.8114
     * t_{4, 0.975} ≈ 2.7764
     * HW = 2.7764 * (15.8114 / sqrt(5)) ≈ 19.6324
     */
    @Test
    public void testEstimateWithList() {
        logTestStep("Testing estimation with raw data list: {10, 20, 30, 40, 50}");
        List<Double> data = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0);
        
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(data, 0.95);
        
        logDebug("Mean: {}, StdDev: {}, HW: {}", result.mean(), result.stdDev(), result.halfWidth());
        assertEquals(5, result.count());
        assertEquals(30.0, result.mean(), 1e-9);
        assertEquals(Math.sqrt(250.0), result.stdDev(), 1e-9);
        assertEquals(19.6324, result.halfWidth(), 1e-4);
    }

    /**
     * Verifies that the estimator handles insufficient data (N < 2) gracefully.
     * <p>
     * Expectation: Half-width should be 0.0, and bounds should equal the mean.
     */
    @Test
    public void testInsufficientData() {
        logTestStep("Testing behavior with insufficient data (N < 2)");
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(1, 50.0, 10.0, 0.95);
        assertEquals(0.0, result.halfWidth());
        assertEquals(50.0, result.lowerBound());
        assertEquals(50.0, result.upperBound());
        
        IntervalEstimator.IntervalResult resultEmpty = IntervalEstimator.estimate(Arrays.asList(10.0), 0.95);
        assertEquals(0.0, resultEmpty.halfWidth());
        assertEquals(10.0, resultEmpty.lowerBound());
    }
}
