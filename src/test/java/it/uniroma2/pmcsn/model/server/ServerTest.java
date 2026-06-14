package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for Server classes, verifying Processor Sharing CPU execution.
 */
public class ServerTest {

    @Test
    public void testProcessorSharingServer() {
        WebServer server = new WebServer(1);
        Job job1 = new Job(1, 0.0, 10.0);
        Job job2 = new Job(2, 0.0, 5.0);

        server.acceptJob(job1, 0.0);
        server.acceptJob(job2, 0.0);

        assertEquals(2, server.getActiveJobs().size());
        
        // After 4.0 seconds, each active job gets 4.0 / 2 = 2.0 work units (speedMultiplier = 1.0)
        server.processJobs(4.0);
        assertEquals(8.0, job1.getRemainingServiceDemand(), 1e-9);
        assertEquals(3.0, job2.getRemainingServiceDemand(), 1e-9);
    }

    @Test
    public void testInfiniteCapacity() {
        WebServer server = new WebServer(1);
        for (int i = 0; i < 100; i++) {
            assertNotNull(server.acceptJob(new Job(i, 0.0, 1.0), 0.0));
        }
        assertEquals(100, server.getActiveJobs().size());
    }
}
