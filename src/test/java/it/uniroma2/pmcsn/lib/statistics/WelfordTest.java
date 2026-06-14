package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Welford's online algorithm for calculating mean and variance.
 */
public class WelfordTest extends BaseTest {

    /**
     * Verifies Welford initialization with an empty dataset.
     */
    @Test
    public void testWelfordWithEmptyDataset() {
        logTestStep("Verifying Welford initialization with empty dataset");
        Welford welford = new Welford();
        assertEquals(0, welford.getCount());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
    }

    /**
     * Verifies Welford behavior with a single data point.
     */
    @Test
    public void testWelfordWithSingleValue() {
        logTestStep("Testing Welford with a single value: 5.0");
        Welford welford = new Welford();
        welford.update(5.0);
        assertEquals(1, welford.getCount());
        assertEquals(5.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
    }

    /**
     * Verifies mean and variance calculations for multiple values.
     * <p>
     * Values: {2.0, 4.0, 9.0}
     * Mean = (2+4+9)/3 = 5.0
     * Sum of Squares (M2) = (2-5)² + (4-5)² + (9-5)² = 9 + 1 + 16 = 26
     * Sample Variance = M2 / (n-1) = 26 / 2 = 13.0
     * Population Variance = M2 / n = 26 / 3 ≈ 8.6667
     */
    @Test
    public void testWelfordWithMultipleValues() {
        logTestStep("Testing Welford with values: {2.0, 4.0, 9.0}");
        Welford welford = new Welford();
        welford.update(2.0);
        welford.update(4.0);
        welford.update(9.0);

        logDebug("Count: {}, Mean: {}, Sample Variance: {}", welford.getCount(), welford.getMean(), welford.getVariance());
        assertEquals(3, welford.getCount());
        assertEquals(5.0, welford.getMean(), 1e-9);
        assertEquals(13.0, welford.getVariance(), 1e-9);
        assertEquals(26.0 / 3.0, welford.getPopulationVariance(), 1e-9);
    }

    /**
     * Verifies the reset functionality.
     */
    @Test
    public void testWelfordReset() {
        logTestStep("Verifying Welford reset functionality");
        Welford welford = new Welford();
        welford.update(10.0);
        welford.update(20.0);
        
        welford.reset();
        assertEquals(0, welford.getCount());
        assertEquals(0.0, welford.getMean());

        welford.update(5.0);
        assertEquals(1, welford.getCount());
        assertEquals(5.0, welford.getMean());
    }
}
