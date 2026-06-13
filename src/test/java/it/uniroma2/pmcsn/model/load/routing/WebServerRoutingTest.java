package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.spike.ThresholdSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.LeastLoadedRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WebServerRoutingTest {

    @Test
    public void testRoundRobinRoutingStrategy() {
        WebServerCluster cluster = new WebServerCluster(3, 5, 10);
        Router router = new Router(5, new RoundRobinRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 10, 1.0);

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
    public void testLeastLoadedRoutingStrategy() {
        WebServerCluster cluster = new WebServerCluster(3, 5, 10);
        Router router = new Router(5, new LeastLoadedRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 10, 1.0);

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
}
