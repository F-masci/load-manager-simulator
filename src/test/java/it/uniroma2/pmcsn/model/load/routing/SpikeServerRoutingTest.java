package it.uniroma2.pmcsn.model.load.routing;

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

public class SpikeServerRoutingTest {

    @Test
    public void testThresholdSpikeServerRoutingStrategy() {
        WebServerCluster cluster = new WebServerCluster(1, 5, 10);
        Router router = new Router(2, new RoundRobinRoutingStrategy(), new ThresholdSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 10, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        
        Job j1 = new Job(1, 0.0, 1.0);
        Job j2 = new Job(2, 0.0, 1.0);
        Job j3 = new Job(3, 0.0, 1.0);

        ws1.acceptJob(j1, 0.0);
        ws1.acceptJob(j2, 0.0); // ws1 load = 2 (reaches siMax = 2)

        // Since ws1 load is 2 >= siMax (2), routing should divert to SpikeServer
        Server routed = router.route(j3, cluster, spikeServer);
        assertEquals(spikeServer.getId(), routed.getId());
    }

    @Test
    public void testNoSpikeServerRoutingStrategy() {
        WebServerCluster cluster = new WebServerCluster(1, 5, 10);
        Router router = new Router(2, new RoundRobinRoutingStrategy(), new NoSpikeServerRoutingStrategy());
        SpikeServer spikeServer = new SpikeServer(0, 10, 1.0);

        WebServer ws1 = cluster.getActiveServers().get(0);
        
        Job j1 = new Job(1, 0.0, 1.0);
        Job j2 = new Job(2, 0.0, 1.0);
        Job j3 = new Job(3, 0.0, 1.0);

        ws1.acceptJob(j1, 0.0);
        ws1.acceptJob(j2, 0.0); // ws1 load = 2 (reaches siMax)

        // Even though ws1 load is 2 >= siMax, NoSpikeServerRoutingStrategy should block routing to SpikeServer
        Server routed = router.route(j3, cluster, spikeServer);
        assertEquals(ws1.getId(), routed.getId());
    }
}
