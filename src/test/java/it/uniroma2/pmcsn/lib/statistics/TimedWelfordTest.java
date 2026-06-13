package it.uniroma2.pmcsn.lib.statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimedWelfordTest {

    @Test
    public void testTimedWelfordWithEmptyDataset() {
        TimedWelford welford = new TimedWelford();
        assertEquals(0.0, welford.getTotalDuration());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
        assertEquals(0.0, welford.getStandardDeviation());
    }

    @Test
    public void testTimedWelfordDirectUpdate() {
        TimedWelford welford = new TimedWelford();
        welford.update(2.0, 3.0);
        welford.update(4.0, 1.0);
        welford.update(9.0, 2.0);

        assertEquals(6.0, welford.getTotalDuration(), 1e-9);
        assertEquals(28.0 / 6.0, welford.getMean(), 1e-9);
        assertEquals(59.333333333333336 / 6.0, welford.getVariance(), 1e-9);
        assertEquals(Math.sqrt(59.333333333333336 / 6.0), welford.getStandardDeviation(), 1e-9);
    }

    @Test
    public void testTimedWelfordUpdateToTime() {
        TimedWelford welford = new TimedWelford();
        
        // At t=0, queue size becomes 2
        welford.updateToTime(0.0, 2.0);
        // At t=3, queue size becomes 4 (meaning value 2 lasted for duration 3)
        welford.updateToTime(3.0, 4.0);
        // At t=4, queue size becomes 9 (meaning value 4 lasted for duration 1)
        welford.updateToTime(4.0, 9.0);
        // At t=6, simulation ends (we trigger an update to include the last duration)
        welford.updateToTime(6.0, 9.0);

        assertEquals(6.0, welford.getTotalDuration(), 1e-9);
        assertEquals(28.0 / 6.0, welford.getMean(), 1e-9);
        assertEquals(59.333333333333336 / 6.0, welford.getVariance(), 1e-9);
    }

    @Test
    public void testTimedWelfordInvalidDuration() {
        TimedWelford welford = new TimedWelford();
        welford.update(5.0, -1.0);
        welford.update(5.0, 0.0);
        assertEquals(0.0, welford.getTotalDuration());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
    }

    @Test
    public void testTimedWelfordReset() {
        TimedWelford welford = new TimedWelford();
        welford.update(10.0, 5.0);
        welford.reset();

        assertEquals(0.0, welford.getTotalDuration());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());

        welford.updateToTime(1.0, 5.0);
        welford.updateToTime(3.0, 10.0);
        assertEquals(2.0, welford.getTotalDuration(), 1e-9);
        assertEquals(5.0, welford.getMean(), 1e-9);
    }
}
