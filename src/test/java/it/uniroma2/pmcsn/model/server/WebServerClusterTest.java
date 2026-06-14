package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebServerCluster, focusing on dynamic scaling and server draining.
 */
public class WebServerClusterTest extends BaseTest {

    /**
     * Verifies the full lifecycle of a server in the cluster:
     * Active -> Scale In -> Draining -> Removed.
     */
    @Test
    public void testWebServerClusterScalingAndDraining() {
        logTestStep("Testing WebServerCluster Scale-Out, Scale-In and Draining lifecycle");
        WebServerCluster cluster = new WebServerCluster(2, 4);
        
        // Scale Out
        assertTrue(cluster.scaleOut(10.0));
        assertEquals(3, cluster.getActiveServers().size());
        
        // Add job to the last server to prevent immediate removal during scale-in
        WebServer targetWs = cluster.getActiveServers().get(2);
        Job job = new Job(1, 10.0, 5.0);
        targetWs.acceptJob(job, 10.0);

        // Scale In
        assertTrue(cluster.scaleIn(15.0));
        assertEquals(2, cluster.getActiveServers().size());
        assertEquals(1, cluster.getDrainingServers().size());
        logDebug("Server {} is now DRAINING", targetWs.getId());

        // Process jobs: job completes at t=20.0 (work=5.0, 1 job active on that server)
        cluster.processActiveJobs(5.0, 20.0);
        targetWs.completeJob(job, 20.0);
        
        // Cleanup happens on next processing call
        cluster.processActiveJobs(1.0, 21.0);
        assertEquals(0, cluster.getDrainingServers().size());
        logDebug("Draining server successfully removed");
    }
}
