package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.routing.spike.NoSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleSystemSimulationTest {

    @Test
    public void testSimplifiedSimulationMetrics() {
        // Deterministic EventSource: 4 jobs arriving at 2.0, 4.0, 6.0, 8.0, each demanding 1.0 time unit of service
        EventSource source = new EventSource() {
            private int count = 0;

            @Override
            public Job getNextJob(double lastArrivalTime) {
                if (count >= 4) {
                    return null;
                }
                count++;
                double arrivalTime = count * 2.0;
                return new Job(count, arrivalTime, 1.0);
            }

            @Override
            public void reset() {
                count = 0;
            }
        };

        // 1 Web Server, capacity = 1
        WebServerCluster cluster = new WebServerCluster(1, 10, 1);
        // Spike Server (won't be routed to because of NoSpikeServerRoutingStrategy)
        SpikeServer spikeServer = new SpikeServer(0, 10, 1.0);

        // Router with NoSpikeServerRoutingStrategy to prevent routing to the Spike Server
        Router router = new Router(5, new RoundRobinRoutingStrategy(), new NoSpikeServerRoutingStrategy());
        
        // Use NoHorizontalScaler and NoVerticalScaler (Null Object pattern)
        NoHorizontalScaler horizontalScaler = new NoHorizontalScaler();
        NoVerticalScaler verticalScaler = new NoVerticalScaler();
        
        LoadManager loadController = new LoadManager(horizontalScaler, verticalScaler, router);

        // Run simulation for exactly 9.0 time units
        SimulationController controller = new SimulationController(9.0, source, cluster, spikeServer, loadController);
        controller.run();

        // System expected metrics calculation:
        // - Completed Jobs: 4
        // - Clock: 9.0
        // - Throughput: 4 / 9.0 = 0.4444...
        // - Average Response Time: 1.0 (each job completes 1.0 time unit after arrival)
        // - Server Utilization: Server is busy in [2,3], [4,5], [6,7], [8,9] (4.0 time units total). 4 / 9.0 = 0.4444...
        // - Average Jobs in System: 4 / 9.0 = 0.4444...

        assertEquals(4, controller.getTotalJobsCompleted());
        assertEquals(9.0, controller.getClock(), 1e-9);
        assertEquals(4.0 / 9.0, controller.getThroughput(), 1e-9);
        assertEquals(1.0, controller.getAverageResponseTime(), 1e-9);
        assertEquals(4.0 / 9.0, controller.getSystemUtilization(), 1e-9);
        assertEquals(4.0 / 9.0, controller.getAverageJobsInSystem(), 1e-9);
    }
}
