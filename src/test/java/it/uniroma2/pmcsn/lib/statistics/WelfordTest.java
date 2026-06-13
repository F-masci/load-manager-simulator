package it.uniroma2.pmcsn.lib.statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WelfordTest {

    @Test
    public void testWelfordWithEmptyDataset() {
        Welford welford = new Welford();
        assertEquals(0, welford.getCount());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
        assertEquals(0.0, welford.getPopulationVariance());
        assertEquals(0.0, welford.getStandardDeviation());
        assertEquals(0.0, welford.getPopulationStandardDeviation());
    }

    @Test
    public void testWelfordWithSingleValue() {
        Welford welford = new Welford();
        welford.update(5.0);
        assertEquals(1, welford.getCount());
        assertEquals(5.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());
        assertEquals(0.0, welford.getPopulationVariance());
        assertEquals(0.0, welford.getStandardDeviation());
        assertEquals(0.0, welford.getPopulationStandardDeviation());
    }

    @Test
    public void testWelfordWithMultipleValues() {
        Welford welford = new Welford();
        welford.update(2.0);
        welford.update(4.0);
        welford.update(9.0);

        assertEquals(3, welford.getCount());
        assertEquals(5.0, welford.getMean(), 1e-9);
        assertEquals(13.0, welford.getVariance(), 1e-9); // sample variance: 26 / 2 = 13
        assertEquals(26.0 / 3.0, welford.getPopulationVariance(), 1e-9); // population variance: 26 / 3
        assertEquals(Math.sqrt(13.0), welford.getStandardDeviation(), 1e-9);
        assertEquals(Math.sqrt(26.0 / 3.0), welford.getPopulationStandardDeviation(), 1e-9);
    }

    @Test
    public void testWelfordReset() {
        Welford welford = new Welford();
        welford.update(10.0);
        welford.update(20.0);
        
        welford.reset();
        assertEquals(0, welford.getCount());
        assertEquals(0.0, welford.getMean());
        assertEquals(0.0, welford.getVariance());

        welford.update(5.0);
        assertEquals(1, welford.getCount());
        assertEquals(5.0, welford.getMean());
    }
}
