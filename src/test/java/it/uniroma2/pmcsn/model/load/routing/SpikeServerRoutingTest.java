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

    /**
     * Verifies that ThresholdSpikeServerRoutingStrategy operates statelessly
     * when the band mechanism is deactivated (siLow equals siMax or -1).
     */
    @Test
    public void testStatelessThresholdRoutingStrategy() {
        logTestStep("Testing stateless threshold routing (band deactivated)");
        WebServerCluster cluster = new WebServerCluster(1, 5);
        int siMax = 2;
        ThresholdSpikeServerRoutingStrategy strategy = new ThresholdSpikeServerRoutingStrategy(siMax);
        Router router = new Router(siMax, new RoundRobinRoutingStrategy(), strategy);
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);

        Job j1 = new Job(1, 0.0, 1.0);
        Server r1 = router.route(j1, cluster, spikeServer);
        assertEquals(ws1.getId(), r1.getId(), "Job 1 should go to WebServer");
        ws1.acceptJob(j1, 0.0);

        Job j2 = new Job(2, 0.0, 1.0);
        Server r2 = router.route(j2, cluster, spikeServer);
        assertEquals(ws1.getId(), r2.getId(), "Job 2 should go to WebServer (to reach siMax)");
        ws1.acceptJob(j2, 0.0);

        Job j3 = new Job(3, 0.0, 1.0);
        Server r3 = router.route(j3, cluster, spikeServer);
        assertEquals(spikeServer.getId(), r3.getId(), "Job 3 should go to SpikeServer");

        assertEquals(0, strategy.getStateChanges(), "Stateless strategy should have 0 state changes");
    }

    /**
     * Verifies that ThresholdSpikeServerRoutingStrategy implements the stateful band routing
     * (meccanismo a bande) correctly when the band is active (siLow < siMax).
     */
    @Test
    public void testBandRoutingStrategy() {
        logTestStep("Testing stateful band routing (meccanismo a bande)");
        WebServerCluster cluster = new WebServerCluster(1, 5);
        int siMax = 3;
        int siLow = 1;
        ThresholdSpikeServerRoutingStrategy strategy = new ThresholdSpikeServerRoutingStrategy(siLow);
        Router router = new Router(siMax, new RoundRobinRoutingStrategy(), strategy);
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);

        Job j1 = new Job(1, 0.0, 1.0);
        Server r1 = router.route(j1, cluster, spikeServer);
        assertEquals(ws1.getId(), r1.getId());
        ws1.acceptJob(j1, 0.0); // SI = 1

        Job j2 = new Job(2, 0.0, 1.0);
        Server r2 = router.route(j2, cluster, spikeServer);
        assertEquals(ws1.getId(), r2.getId());
        ws1.acceptJob(j2, 0.0); // SI = 2

        Job j3 = new Job(3, 0.0, 1.0);
        Server r3 = router.route(j3, cluster, spikeServer);
        assertEquals(ws1.getId(), r3.getId());
        ws1.acceptJob(j3, 0.0); // SI = 3 (reaches siMax)

        Job j4 = new Job(4, 0.0, 1.0);
        Server r4 = router.route(j4, cluster, spikeServer);
        assertEquals(spikeServer.getId(), r4.getId());
        assertEquals(1, strategy.getStateChanges());

        ws1.completeJob(j3, 0.0); // SI = 2
        Job j5 = new Job(5, 0.0, 1.0);
        Server r5 = router.route(j5, cluster, spikeServer);
        assertEquals(spikeServer.getId(), r5.getId(), "Should remain in spike mode at SI=2");
        assertEquals(1, strategy.getStateChanges());

        ws1.completeJob(j2, 0.0); // SI = 1
        Job j6 = new Job(6, 0.0, 1.0);
        Server r6 = router.route(j6, cluster, spikeServer);
        assertEquals(ws1.getId(), r6.getId(), "Should deactivate spike mode and return to WebServer at SI=1");
        assertEquals(2, strategy.getStateChanges());

        r6.acceptJob(j6, 0.0); // SI = 2
        Job j7 = new Job(7, 0.0, 1.0);
        Server r7 = router.route(j7, cluster, spikeServer);
        assertEquals(ws1.getId(), r7.getId(), "Should stay in ordinary mode at SI=2 (below siMax)");
        assertEquals(2, strategy.getStateChanges());
    }
}
