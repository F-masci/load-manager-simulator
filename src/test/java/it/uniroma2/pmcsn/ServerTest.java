package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Server classes, verifying Processor Sharing CPU execution.
 */
public class ServerTest {

    @Test
    public void testProcessorSharingServer() {
        WebServer server = new WebServer(1, 10);
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
    public void testServerCapacity() {
        WebServer server = new WebServer(1, 2); // capacity = 2
        Job job1 = new Job(1, 0.0, 1.0);
        Job job2 = new Job(2, 0.0, 1.0);
        Job job3 = new Job(3, 0.0, 1.0);

        assertNotNull(server.acceptJob(job1, 0.0));
        assertNotNull(server.acceptJob(job2, 0.0));
        assertNull(server.acceptJob(job3, 0.0)); // Over capacity
    }

    @Test
    public void testWebServerClusterScaling() {
        WebServerCluster cluster = new WebServerCluster(2, 4, 10);
        assertEquals(2, cluster.getActiveServers().size());
        assertEquals(2, cluster.getAllServers().size());

        // Scale Up
        assertTrue(cluster.scaleUp(10.0));
        assertEquals(3, cluster.getActiveServers().size());
        assertEquals(3, cluster.getAllServers().size());
        // Since setLastEventTime was called, its lastEventTime (retrieved via updateStatistics check or just the field if we want to check it)
        // is set to 10.0. To verify, we can process jobs at 15.0 and check stats.
        WebServer thirdWs = cluster.getActiveServers().get(2);
        
        // Scale Up to Max
        assertTrue(cluster.scaleUp(20.0));
        assertEquals(4, cluster.getActiveServers().size());
        
        // Cannot scale up beyond max
        assertFalse(cluster.scaleUp(30.0));
        assertEquals(4, cluster.getActiveServers().size());

        // Let's add an active job on the last server to test draining
        WebServer lastWs = cluster.getActiveServers().get(3);
        Job job = new Job(1, 20.0, 5.0);
        lastWs.acceptJob(job, 20.0);

        // Scale Down
        assertTrue(cluster.scaleDown(25.0));
        assertEquals(3, cluster.getActiveServers().size());
        assertEquals(1, cluster.getDrainingServers().size());
        assertEquals(lastWs.getId(), cluster.getDrainingServers().get(0).getId());

        // Process jobs on cluster
        cluster.processActiveJobs(5.0, 30.0);
        // Draining server is still there because job is not completed yet (service demand is 0 but it's not completed)
        assertEquals(1, cluster.getDrainingServers().size());

        // Complete the job on draining server
        lastWs.completeJob(job, 30.0);
        
        // Next processActiveJobs will clean it up
        cluster.processActiveJobs(1.0, 31.0);
        assertEquals(0, cluster.getDrainingServers().size());
    }
}
