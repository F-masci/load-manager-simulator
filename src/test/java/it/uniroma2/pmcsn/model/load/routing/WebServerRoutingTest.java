package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.spike.ThresholdSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.LeastLoadedRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Web Server routing strategies (Round Robin, Least Loaded).
 */
public class WebServerRoutingTest extends BaseTest {

    /**
     * Verifies that Round Robin routing correctly cycles through active servers.
     */
    @Test
    public void testRoundRobinRoutingStrategy() {
        logTestStep("Testing Round Robin routing logic across 3 servers");
        WebServerCluster cluster = new WebServerCluster(3, 5);
        Router router = new Router(5, new RoundRobinRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        assertEquals(1, router.route(new Job(1, 0.0, 1.0), cluster, spikeServer).getId());
        assertEquals(2, router.route(new Job(2, 0.0, 1.0), cluster, spikeServer).getId());
        assertEquals(3, router.route(new Job(3, 0.0, 1.0), cluster, spikeServer).getId());
        assertEquals(1, router.route(new Job(4, 0.0, 1.0), cluster, spikeServer).getId());
    }

    /**
     * Verifies that Least Loaded routing selects the server with the minimum number of active jobs.
     */
    @Test
    public void testLeastLoadedRoutingStrategy() {
        logTestStep("Testing Least Loaded routing logic");
        WebServerCluster cluster = new WebServerCluster(3, 5);
        Router router = new Router(5, new LeastLoadedRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        WebServer ws2 = cluster.getActiveServers().get(1);
        WebServer ws3 = cluster.getActiveServers().get(2);

        ws1.acceptJob(new Job(1, 0.0, 1.0), 0.0);
        ws1.acceptJob(new Job(2, 0.0, 1.0), 0.0); // ws1: 2 jobs
        ws2.acceptJob(new Job(3, 0.0, 1.0), 0.0); // ws2: 1 job
        // ws3: 0 jobs

        Server selected = router.route(new Job(4, 0.0, 1.0), cluster, spikeServer);
        logDebug("Selected server ID (should be 3): {}", selected.getId());
        assertEquals(ws3.getId(), selected.getId());
    }
}
