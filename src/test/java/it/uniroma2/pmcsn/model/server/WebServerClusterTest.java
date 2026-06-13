package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.model.Job;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests specifically verifying WebServerCluster scaling and draining behaviors.
 */
public class WebServerClusterTest {

    @Test
    public void testWebServerClusterScalingAndDraining() {
        WebServerCluster cluster = new WebServerCluster(2, 4);
        assertEquals(2, cluster.getActiveServers().size());
        assertEquals(2, cluster.getAllServers().size());

        // Scale Up
        assertTrue(cluster.scaleOut(10.0));
        assertEquals(3, cluster.getActiveServers().size());
        assertEquals(3, cluster.getAllServers().size());
        
        // Scale Up to Max
        assertTrue(cluster.scaleOut(20.0));
        assertEquals(4, cluster.getActiveServers().size());
        
        // Cannot scale up beyond max
        assertFalse(cluster.scaleOut(30.0));
        assertEquals(4, cluster.getActiveServers().size());

        // Let's add an active job on the last server to test draining
        WebServer lastWs = cluster.getActiveServers().get(3);
        Job job = new Job(1, 20.0, 5.0);
        lastWs.acceptJob(job, 20.0);

        // Scale Down
        assertTrue(cluster.scaleIn(25.0));
        assertEquals(3, cluster.getActiveServers().size());
        assertEquals(1, cluster.getDrainingServers().size());
        assertEquals(lastWs.getId(), cluster.getDrainingServers().get(0).getId());

        // Process jobs on cluster
        cluster.processActiveJobs(5.0, 30.0);
        // Draining server is still there because job is not completed yet
        assertEquals(1, cluster.getDrainingServers().size());

        // Complete the job on draining server
        lastWs.completeJob(job, 30.0);
        
        // Next processActiveJobs will clean it up
        cluster.processActiveJobs(1.0, 31.0);
        assertEquals(0, cluster.getDrainingServers().size());
    }
}
