package it.uniroma2.pmcsn.lib.statistics;

import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for TimedWelford, verifying time-weighted mean and variance calculations.
 */
public class TimedWelfordTest extends BaseTest {

    @Test
    public void testTimedWelfordWithEmptyDataset() {
        logTestStep("Verifying TimedWelford initialization with empty dataset");
        TimedWelford welford = new TimedWelford();
        
        // Initial state should be all zeros
        assertEquals(0.0, welford.getTotalDuration());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
    }

    @Test
    public void testTimedWelfordDirectUpdate() {
        logTestStep("Testing direct updates with value-duration pairs");
        TimedWelford welford = new TimedWelford();
        
        // Feed direct observations (value, duration)
        welford.update(2.0, 3.0);
        welford.update(4.0, 1.0);
        welford.update(9.0, 2.0);

        logDebug("Total Duration: {}, Mean: {}", welford.getTotalDuration(), welford.getMean());
        
        // Verify time-weighted mean: (2*3 + 4*1 + 9*2) / 6 = 28 / 6
        assertEquals(6.0, welford.getTotalDuration(), 1e-9);
        assertEquals(28.0 / 6.0, welford.getMean(), 1e-9);
    }

    @Test
    public void testTimedWelfordUpdateToTime() {
        logTestStep("Testing updates driven by simulation time transitions");
        TimedWelford welford = new TimedWelford();
        
        // Step-by-step state changes during simulation
        welford.updateToTime(0.0, 2.0);  // Value 2.0 starts at t=0
        welford.updateToTime(3.0, 4.0);  // At t=3, value changes to 4.0 (2.0 lasted 3s)
        welford.updateToTime(4.0, 9.0);  // At t=4, value changes to 9.0 (4.0 lasted 1s)
        welford.updateToTime(6.0, 9.0);  // At t=6, end of segment (9.0 lasted 2s)

        logDebug("Mean calculated from time transitions: {}", welford.getMean());
        assertEquals(6.0, welford.getTotalDuration(), 1e-9);
        assertEquals(28.0 / 6.0, welford.getMean(), 1e-9);
    }

    @Test
    public void testTimedWelfordReset() {
        logTestStep("Verifying TimedWelford reset functionality");
        TimedWelford welford = new TimedWelford();
        
        // Populate and then clear
        welford.update(10.0, 5.0);
        welford.reset();

        // Check if it's clean
        assertEquals(0.0, welford.getTotalDuration());
        assertEquals(0.0, welford.getMean());

        // Verify it works correctly after reset
        welford.updateToTime(1.0, 5.0);
        welford.updateToTime(3.0, 10.0);
        assertEquals(2.0, welford.getTotalDuration(), 1e-9);
        assertEquals(5.0, welford.getMean(), 1e-9);
    }
}
