package it.uniroma2.pmcsn.model;

import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Job class, verifying state management and time tracking.
 */
public class JobTest extends BaseTest {

    /**
     * Verifies that the remaining service demand is correctly tracked and decreased.
     */
    @Test
    public void testJobRemainingDemand() {
        logTestStep("Testing Job remaining demand tracking");
        Job job = new Job(1, 0.0, 10.0);
        assertEquals(10.0, job.getRemainingServiceDemand());
        
        job.decreaseRemainingDemand(4.0);
        logDebug("Remaining demand after 4.0 decrease: {}", job.getRemainingServiceDemand());
        assertEquals(6.0, job.getRemainingServiceDemand());

        job.decreaseRemainingDemand(10.0);
        assertEquals(0.0, job.getRemainingServiceDemand());
    }

    /**
     * Verifies the calculation of waiting time and response time based on simulation timestamps.
     * <p>
     * Waiting Time = startTime - arrivalTime
     * Response Time = completionTime - arrivalTime
     */
    @Test
    public void testJobResponseTimeAndWaiting() {
        logTestStep("Testing Job waiting and response time calculations");
        Job job = new Job(1, 2.0, 5.0);
        
        job.setStartTime(3.0);
        job.setCompletionTime(8.0);

        logDebug("Waiting Time: {}, Response Time: {}", job.getWaitingTime(), job.getResponseTime());
        assertEquals(1.0, job.getWaitingTime()); // 3.0 - 2.0
        assertEquals(6.0, job.getResponseTime()); // 8.0 - 2.0
    }
}
