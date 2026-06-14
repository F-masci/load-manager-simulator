package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.BaseTest;
import it.uniroma2.pmcsn.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for EventSource implementations, verifying workload generation logic.
 */
public class EventSourceTest extends BaseTest {

    /**
     * Verifies the Hyperexponential distribution workload generator.
     */
    @Test
    public void testHyperexponentialGenerator() {
        logTestStep("Testing Hyperexponential workload generator");
        HyperexponentialEventSource source = new HyperexponentialEventSource(123456789L, 2.0, 1.5);
        
        Job firstJob = source.getNextJob(0.0);
        assertNotNull(firstJob);
        logDebug("First Job - Arrival: {}, Service: {}", firstJob.getArrivalTime(), firstJob.getServiceTime());
        assertTrue(firstJob.getArrivalTime() > 0.0);
        
        Job secondJob = source.getNextJob(firstJob.getArrivalTime());
        assertNotNull(secondJob);
        assertTrue(secondJob.getArrivalTime() > firstJob.getArrivalTime());
    }

    /**
     * Verifies the Exponential distribution workload generator.
     */
    @Test
    public void testExponentialGenerator() {
        logTestStep("Testing Exponential workload generator");
        ExponentialEventSource source = new ExponentialEventSource(123456789L, 2.0, 1.5);
        
        Job firstJob = source.getNextJob(0.0);
        assertNotNull(firstJob);
        logDebug("First Job - Arrival: {}, Service: {}", firstJob.getArrivalTime(), firstJob.getServiceTime());
        
        Job secondJob = source.getNextJob(firstJob.getArrivalTime());
        assertNotNull(secondJob);
        assertTrue(secondJob.getArrivalTime() > firstJob.getArrivalTime());
    }
}
