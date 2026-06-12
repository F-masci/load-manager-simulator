package it.pmcsn;

import it.pmcsn.model.*;
import it.pmcsn.model.load.routing.*;
import it.pmcsn.model.server.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {

    @Test
    public void testRoundRobinRouter() {
        WebServerCluster cluster = new WebServerCluster(3, 5, 10);
        Router router = new RoundRobinRouter(5); // siMax = 5
        SpikeServer spikeServer = new SpikeServer(999, 10, 1.0);

        Job job1 = new Job(1, 0.0, 1.0);
        Job job2 = new Job(2, 0.0, 1.0);
        Job job3 = new Job(3, 0.0, 1.0);
        Job job4 = new Job(4, 0.0, 1.0);

        // Round Robin selection
        assertEquals(1, router.route(job1, cluster, spikeServer).getId());
        assertEquals(2, router.route(job2, cluster, spikeServer).getId());
        assertEquals(3, router.route(job3, cluster, spikeServer).getId());
        assertEquals(1, router.route(job4, cluster, spikeServer).getId());
    }

    @Test
    public void testLeastLoadedRouter() {
        WebServerCluster cluster = new WebServerCluster(3, 5, 10);
        Router router = new LeastLoadedRouter(5); // siMax = 5
        SpikeServer spikeServer = new SpikeServer(999, 10, 1.0);

        // Simulate some load on Server 1 and 2
        WebServer ws1 = cluster.getActiveServers().get(0);
        WebServer ws2 = cluster.getActiveServers().get(1);
        WebServer ws3 = cluster.getActiveServers().get(2);

        Job j1 = new Job(1, 0.0, 1.0);
        Job j2 = new Job(2, 0.0, 1.0);
        Job j3 = new Job(3, 0.0, 1.0);

        ws1.acceptJob(j1, 0.0);
        ws1.acceptJob(j2, 0.0); // ws1 load = 2
        ws2.acceptJob(j3, 0.0); // ws2 load = 1
        // ws3 load = 0

        // Least loaded should select ws3 (load = 0)
        assertEquals(ws3.getId(), router.route(new Job(4, 0.0, 1.0), cluster, spikeServer).getId());
    }

    @Test
    public void testSpikeServerDiverting() {
        WebServerCluster cluster = new WebServerCluster(1, 5, 10);
        Router router = new RoundRobinRouter(2); // siMax = 2
        SpikeServer spikeServer = new SpikeServer(999, 10, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        
        Job j1 = new Job(1, 0.0, 1.0);
        Job j2 = new Job(2, 0.0, 1.0);
        Job j3 = new Job(3, 0.0, 1.0);

        ws1.acceptJob(j1, 0.0);
        ws1.acceptJob(j2, 0.0); // ws1 load = 2 (reaches siMax)

        // Since ws1 load is 2 >= siMax (2), routing should divert to SpikeServer
        Server routed = router.route(j3, cluster, spikeServer);
        assertEquals(spikeServer.getId(), routed.getId());
    }
}
