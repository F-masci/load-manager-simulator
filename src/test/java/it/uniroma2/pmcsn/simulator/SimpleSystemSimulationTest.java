package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.TestConfigs;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validation tests against theoretical queuing models (M/M/1).
 */
public class SimpleSystemSimulationTest extends BaseSimulationTest {

    private static final double ARRIVAL_MEAN = 0.40; // lambda = 2.5
    private static final double SERVICE_MEAN = 0.25; // mu = 4.0
    
    /**
     * Validates simulation results against M/M/1 theoretical metrics.
     * <p>
     * Theoretical results for λ=2.5, μ=4.0:
     * <ul>
     *   <li>Utilization (ρ) = λ / μ = 0.625</li>
     *   <li>Response Time (E[R]) = 1 / (μ - λ) = 0.6667</li>
     *   <li>Jobs in System (E[N]) = λ * E[R] = 1.6667</li>
     *   <li>Throughput (X) = λ = 2.5</li>
     * </ul>
     */
    @Test
    public void testSimplifiedExponentialSimulationMetrics() {
        logTestStep("Validating M/M/1 metrics: lambda=2.5, mu=4.0");
        
        // Calculate theoretical expectations
        final double mu = 1 / SERVICE_MEAN;
        final double lam = 1 / ARRIVAL_MEAN;

        final double expectedRt = 1 / (mu - lam);
        final double expectedJis = lam * expectedRt;
        final double expectedUtil = lam / mu;
        final double expectedThr = lam;

        // Step 1: Configure M/M/1 simulation
        ApplicationConfig testConfig = TestConfigs.mm1(ARRIVAL_MEAN, SERVICE_MEAN);

        // Step 2: Run simulation using Facade (Batch Means)
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSimulation();

        logDebug("Results - RT: {}, JIS: {}, Util: {}, Thr: {}", 
                 results.responseTime().mean(), results.jobsInSystem().mean(), 
                 results.utilization().mean(), results.throughput().mean());

        // Step 3: Compare results with theoretical values within confidence intervals
        assertEquals(expectedRt, results.responseTime().mean(), results.responseTime().halfWidth(), "Mean Response Time mismatch");
        assertEquals(expectedJis, results.jobsInSystem().mean(), results.jobsInSystem().halfWidth(), "Mean Jobs in System mismatch");
        assertEquals(expectedUtil, results.utilization().mean(), results.utilization().halfWidth(), "Mean Utilization mismatch");
        assertEquals(expectedThr, results.throughput().mean(), results.throughput().halfWidth(), "Mean Throughput mismatch");
    }

    /**
     * Verifies Little's Law for a Hyperexponential system.
     * <p>
     * Little's Law: E[N] = λ * E[R]
     */
    @Test
    public void testSimplifiedHyperexponentialSimulationMetrics() {
        logTestStep("Validating Little's Law in Hyperexponential system");

        // Configure Hyperexponential simulation
        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.singleHyperexponentialServer(ARRIVAL_MEAN, 4.0, SERVICE_MEAN, 4.0),
            ApplicationConfig.ClusterConfig.singleServer(),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.batchRun(128, 8192)
        );

        // Run simulation
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSimulation();

        // Verify E[N] = X * E[R]
        double expectedJis = results.throughput().mean() * results.responseTime().mean();
        double realJis = results.jobsInSystem().mean();

        logDebug("Little's Law Check - Real JIS: {}, Expected JIS (X * R): {}", realJis, expectedJis);
        assertEquals(realJis, expectedJis, results.jobsInSystem().halfWidth(), "Little's Law does not hold");
    }
}
