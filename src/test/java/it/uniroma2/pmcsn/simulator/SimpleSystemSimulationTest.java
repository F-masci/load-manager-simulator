package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.event.source.ExponentialEventSource;
import it.uniroma2.pmcsn.model.event.source.HyperexponentialEventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.routing.spike.NoSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.DeterministicRoutingStrategy;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleSystemSimulationTest {

    private static final long SEED = 123456789L;
    private static final double ARRIVAL_MEAN = 0.40;
    private static final double ARRIVAL_CV = 4;
    private static final double SERVICE_MEAN = 0.25;
    private static final double SERVICE_CV = 4;

    private static final int MIN_SERVER = 1;
    private static final int MAX_SERVER = 1;
    private static final int TARGET_SERVER = 1;

    @Test
    public void testSimplifiedExponentialSimulationMetrics() {

        ApplicationConfig testConfig = new ApplicationConfig();

        EventSource source = new ExponentialEventSource(SEED, ARRIVAL_MEAN, SERVICE_MEAN);
        SimulationFacade.AggregatedResults results = runSimulation(testConfig, source);

        // With μ = 1/0.25 = 4 and λ = 1/0.40 = 2.5, we can compute the expected metrics:
        final double mu     = 1 / SERVICE_MEAN;
        final double lam    = 1 / ARRIVAL_MEAN;

        final double expectedRt     = 1 / (mu - lam);   // => R = 1 / (μ - λ) = 1 / (4 - 2.5) = 0.6667
        final double expectedJis    = lam * expectedRt; // => N = λ * R = 2.5 * 2 / 3 = 1.6667
        final double expectedUtil   = lam /mu;          // => U = λ / μ = 2.5 * 0.25 = 5 / 8 = 0.625
        final double expectedThr    = lam;              // => X = λ = 2.5

        // FIXME: Compute the correct parameters for batch means
        // The estimation is close but with large interval
        assertEquals(expectedRt, results.responseTime().mean(), results.responseTime().halfWidth(), "Mean Response Time mismatch");
        assertEquals(expectedJis, results.jobsInSystem().mean(), results.jobsInSystem().halfWidth(), "Mean Jobs in System mismatch");
        assertEquals(expectedUtil, results.utilization().mean(), results.utilization().halfWidth(), "Mean Utilization mismatch");
        assertEquals(expectedThr, results.throughput().mean(), results.throughput().halfWidth(), "Mean Throughput mismatch");
    }

    @Test
    public void testSimplifiedHyperexponentialSimulationMetrics() {

        ApplicationConfig testConfig = new ApplicationConfig();

        EventSource source = new HyperexponentialEventSource(SEED,
                ARRIVAL_MEAN, ARRIVAL_CV,
                SERVICE_MEAN, SERVICE_CV
        );
        SimulationFacade.AggregatedResults results = runSimulation(testConfig, source);

        // We don't have an expected result for this scenario
        // so we simply verify if Little Law is valid
        //
        // => N = X * R
        double expectedJis = results.throughput().mean() * results.responseTime().mean();
        double realJis = results.jobsInSystem().mean();

        // FIXME: Compute the correct parameters for batch means
        assertEquals(realJis, expectedJis, results.jobsInSystem().halfWidth(), "Little's Law does not hold");
    }

    /**
     * Run the simulation on the simplified system
     *
     * @param testConfig the application configuration
     * @param source the event source
     * @return the aggregated simulation results
     */
    private SimulationFacade.AggregatedResults runSimulation(ApplicationConfig testConfig, EventSource source) {
        // Use only 1 Web Server
        WebServerCluster cluster = new WebServerCluster(MIN_SERVER, MAX_SERVER);
        // Spike Server (won't be routed to because of NoSpikeServerRoutingStrategy)
        SpikeServer spikeServer = new SpikeServer(0, 1.0);

        // Router with NoSpikeServerRoutingStrategy to prevent routing to the Spike Server
        Router router = new Router(Integer.MAX_VALUE, new DeterministicRoutingStrategy(TARGET_SERVER), new NoSpikeServerRoutingStrategy());

        // Use NoHorizontalScaler and NoVerticalScaler to avoid scaling
        NoHorizontalScaler horizontalScaler = new NoHorizontalScaler();
        NoVerticalScaler verticalScaler = new NoVerticalScaler();

        // Run batch means simulation to observe steady-state results
        SimulationBuilder builder = new SimulationBuilder()
                .config(testConfig)
                .seed(SEED)
                .eventSource(source)
                .webServerCluster(cluster)
                .spikeServer(spikeServer)
                .router(router)
                .horizontalScaler(horizontalScaler)
                .verticalScaler(verticalScaler);
        SimulationFacade facade = new SimulationFacade(testConfig);
        return facade.runBatchMeansSimulation(builder);
    }
}
