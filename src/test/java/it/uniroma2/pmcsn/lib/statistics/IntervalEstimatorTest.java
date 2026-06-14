package it.uniroma2.pmcsn.lib.statistics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntervalEstimatorTest {

    @Test
    public void testHalfWidthCalculationWithKnownValues() {
        // N=5 (df=4), confidence=0.95, stdDev=1.0
        // t_{4, 0.975} is approx 2.7764
        // HW = 2.7764 * (1.0 / sqrt(5)) = 2.7764 * 0.4472 = 1.2416
        double count = 5;
        double stdDev = 1.0;
        double confidence = 0.95;
        
        double halfWidth = IntervalEstimator.estimateHalfWidth((long)count, stdDev, confidence);
        assertEquals(1.2416, halfWidth, 1e-4);
    }

    @Test
    public void testIntervalResultEncapsulation() {
        long count = 10;
        double mean = 100.0;
        double stdDev = 15.0;
        double confidence = 0.90; // 90% confidence
        
        // df=9, t_{9, 0.95} is approx 1.8331
        // HW = 1.8331 * (15.0 / sqrt(10)) = 1.8331 * 4.7434 = 8.6952
        
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(count, mean, stdDev, confidence);
        
        assertEquals(count, result.count());
        assertEquals(mean, result.mean());
        assertEquals(stdDev, result.stdDev());
        assertEquals(confidence, result.confidenceLevel());
        assertEquals(8.6952, result.halfWidth(), 1e-4);
        assertEquals(mean - 8.6952, result.lowerBound(), 1e-4);
        assertEquals(mean + 8.6952, result.upperBound(), 1e-4);
    }

    @Test
    public void testEstimateWithList() {
        List<Double> data = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0);
        // Mean = 30.0
        // Sum of squares = (10-30)^2 + (20-30)^2 + (0)^2 + (10)^2 + (20)^2 = 400 + 100 + 0 + 100 + 400 = 1000
        // Sample Variance = 1000 / (5-1) = 250
        // Sample StdDev = sqrt(250) = 15.8114
        
        // N=5, df=4, confidence=0.95, t=2.7764
        // HW = 2.776445 * (15.811388 / sqrt(5)) = 2.776445 * 7.071067 = 19.6324
        
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(data, 0.95);
        
        assertEquals(5, result.count());
        assertEquals(30.0, result.mean(), 1e-9);
        assertEquals(Math.sqrt(250.0), result.stdDev(), 1e-9);
        assertEquals(19.6324, result.halfWidth(), 1e-4);
        assertEquals(30.0 - 19.6324, result.lowerBound(), 1e-4);
        assertEquals(30.0 + 19.6324, result.upperBound(), 1e-4);
    }

    @Test
    public void testInsufficientData() {
        // N < 2 should return HW=0 and lower=upper=mean
        IntervalEstimator.IntervalResult result = IntervalEstimator.estimate(1, 50.0, 10.0, 0.95);
        assertEquals(0.0, result.halfWidth());
        assertEquals(50.0, result.lowerBound());
        assertEquals(50.0, result.upperBound());
        
        IntervalEstimator.IntervalResult resultEmpty = IntervalEstimator.estimate(Arrays.asList(10.0), 0.95);
        assertEquals(0.0, resultEmpty.halfWidth());
        assertEquals(10.0, resultEmpty.lowerBound());
    }
}
