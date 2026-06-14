package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleSystemSimulationTest {

    private static final double ARRIVAL_MEAN = 0.40; // lambda = 2.5
    private static final double SERVICE_MEAN = 0.25; // mu = 4.0
    // rho = lambda / mu = 2.5 / 4 = 0.625
    // E[R] = 1 / (mu - lambda) = 1 / (4 - 2.5) = 0.6667
    // E[N] = lambda * E[R] = 2.5 * 0.6667 = 1.6667
    
    private static final double ARRIVAL_CV = 4;
    private static final double SERVICE_CV = 4;

    private static final ApplicationConfig.ExecutionConfig EXECUTION_CONFIG = ApplicationConfig.ExecutionConfig.batchRun(64, 256);
    private static final ApplicationConfig.ClusterConfig CLUSTER_CONFIG = ApplicationConfig.ClusterConfig.singleServer();
    private static final ApplicationConfig.ScalingConfig SCALING_CONFIG = ApplicationConfig.ScalingConfig.disabled();

    @Test
    public void testSimplifiedExponentialSimulationMetrics() {
        // mu = 4, lambda = 2.5, rho = 0.625
        final double mu = 1 / SERVICE_MEAN;
        final double lam = 1 / ARRIVAL_MEAN;

        final double expectedRt = 1 / (mu - lam);       // 0.6667
        final double expectedJis = lam * expectedRt;    // 1.6667
        final double expectedUtil = lam / mu;           // 0.625
        final double expectedThr = lam;                 // 2.5

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.singleExponentialServer(ARRIVAL_MEAN, SERVICE_MEAN),
            CLUSTER_CONFIG,
            SCALING_CONFIG,
            EXECUTION_CONFIG
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSimulation();

        assertEquals(expectedRt, results.responseTime().mean(), results.responseTime().halfWidth(), "Mean Response Time mismatch");
        assertEquals(expectedJis, results.jobsInSystem().mean(), results.jobsInSystem().halfWidth(), "Mean Jobs in System mismatch");
        assertEquals(expectedUtil, results.utilization().mean(), results.utilization().halfWidth(), "Mean Utilization mismatch");
        assertEquals(expectedThr, results.throughput().mean(), results.throughput().halfWidth(), "Mean Throughput mismatch");
    }

    @Test
    public void testSimplifiedHyperexponentialSimulationMetrics() {

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.singleHyperexponentialServer(ARRIVAL_MEAN, ARRIVAL_CV, SERVICE_MEAN, SERVICE_CV),
            CLUSTER_CONFIG,
            SCALING_CONFIG,
            EXECUTION_CONFIG
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSimulation();

        // Verify Little's Law holds
        double expectedJis = results.throughput().mean() * results.responseTime().mean();
        double realJis = results.jobsInSystem().mean();

        assertEquals(realJis, expectedJis, results.jobsInSystem().halfWidth(), "Little's Law does not hold");
    }
}
