package it.uniroma2.pmcsn.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Job class.
 */
public class JobTest {

    @Test
    public void testJobRemainingDemand() {
        Job job = new Job(1, 0.0, 10.0);
        assertEquals(10.0, job.getRemainingServiceDemand());
        
        job.decreaseRemainingDemand(4.0);
        assertEquals(6.0, job.getRemainingServiceDemand());

        job.decreaseRemainingDemand(10.0);
        assertEquals(0.0, job.getRemainingServiceDemand());
    }

    @Test
    public void testJobResponseTimeAndWaiting() {
        Job job = new Job(1, 2.0, 5.0);
        assertEquals(0.0, job.getWaitingTime());
        assertEquals(0.0, job.getResponseTime());

        job.setStartTime(3.0);
        job.setCompletionTime(8.0);

        assertEquals(1.0, job.getWaitingTime());
        assertEquals(6.0, job.getResponseTime());
    }
}
