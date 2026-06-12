package it.pmcsn;

import it.pmcsn.event.source.HyperexponentialEventSource;
import it.pmcsn.event.source.ExponentialEventSource;
import it.pmcsn.model.Job;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventSource workload generators (Exponential and Hyperexponential).
 */
public class EventSourceTest {

    @Test
    public void testHyperexponentialGenerator() {
        HyperexponentialEventSource source = new HyperexponentialEventSource(123456789L);
        Job firstJob = source.getNextJob(0.0);
        assertNotNull(firstJob);
        assertTrue(firstJob.getArrivalTime() > 0.0);
        assertTrue(firstJob.getServiceTime() > 0.0);
        
        Job secondJob = source.getNextJob(firstJob.getArrivalTime());
        assertNotNull(secondJob);
        assertTrue(secondJob.getArrivalTime() > firstJob.getArrivalTime());
    }

    @Test
    public void testExponentialGenerator() {
        ExponentialEventSource source = new ExponentialEventSource(123456789L, 2.0, 1.5);
        Job firstJob = source.getNextJob(0.0);
        assertNotNull(firstJob);
        
        Job secondJob = source.getNextJob(firstJob.getArrivalTime());
        assertNotNull(secondJob);
    }
}
