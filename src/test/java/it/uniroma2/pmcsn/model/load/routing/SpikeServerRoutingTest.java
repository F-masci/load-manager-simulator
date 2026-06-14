package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.BaseTest;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.spike.NoSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.spike.ThresholdSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Spike Server routing strategies, verifying diversion logic when thresholds are reached.
 */
public class SpikeServerRoutingTest extends BaseTest {

    /**
     * Verifies that ThresholdSpikeServerRoutingStrategy diverts jobs to the Spike Server
     * when a WebServer's load reaches or exceeds siMax.
     */
    @Test
    public void testThresholdSpikeServerRoutingStrategy() {
        logTestStep("Testing Threshold Spike routing diversion");
        WebServerCluster cluster = new WebServerCluster(1, 5);
        int siMax = 2;
        Router router = new Router(siMax, new RoundRobinRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        
        ws1.acceptJob(new Job(1, 0.0, 1.0), 0.0);
        ws1.acceptJob(new Job(2, 0.0, 1.0), 0.0); // ws1 load = 2 (reaches siMax)

        Job incoming = new Job(3, 0.0, 1.0);
        Server routed = router.route(incoming, cluster, spikeServer);
        
        logDebug("Job routed to server ID: {}", routed.getId());
        assertEquals(spikeServer.getId(), routed.getId(), "Job should have been diverted to Spike Server");
    }

    /**
     * Verifies that NoSpikeServerRoutingStrategy never diverts jobs to the Spike Server.
     */
    @Test
    public void testNoSpikeServerRoutingStrategy() {
        logTestStep("Testing NoSpike routing strategy (no diversion)");
        WebServerCluster cluster = new WebServerCluster(1, 5);
        Router router = new Router(2, new RoundRobinRoutingStrategy(), new NoSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        ws1.acceptJob(new Job(1, 0.0, 1.0), 0.0);
        ws1.acceptJob(new Job(2, 0.0, 1.0), 0.0);

        Job incoming = new Job(3, 0.0, 1.0);
        Server routed = router.route(incoming, cluster, spikeServer);
        
        logDebug("Job routed to server ID: {}", routed.getId());
        assertEquals(ws1.getId(), routed.getId(), "Job should NOT have been diverted to Spike Server");
    }
}
