package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Server components, focusing on Processor Sharing behavior.
 */
public class ServerTest extends BaseTest {

    /**
     * Verifies Processor Sharing (PS) execution logic.
     * <p>
     * In PS, n active jobs share the CPU equally.
     * After Δt, each job receives (Δt / n) * speedMultiplier work units.
     */
    @Test
    public void testProcessorSharingServer() {
        logTestStep("Testing Processor Sharing (PS) with 2 jobs");
        WebServer server = new WebServer(1);
        Job job1 = new Job(1, 0.0, 10.0);
        Job job2 = new Job(2, 0.0, 5.0);

        server.acceptJob(job1, 0.0);
        server.acceptJob(job2, 0.0);

        // After 4.0s, each job gets 4.0 / 2 = 2.0 work units
        server.processJobs(4.0);
        
        logDebug("Job 1 remaining: {}, Job 2 remaining: {}", 
                 job1.getRemainingServiceDemand(), job2.getRemainingServiceDemand());
                 
        assertEquals(8.0, job1.getRemainingServiceDemand(), 1e-9);
        assertEquals(3.0, job2.getRemainingServiceDemand(), 1e-9);
    }
}
