package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.TestConfigs;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.chart.RoutingChartUtility;
import it.uniroma2.pmcsn.utils.chart.SystemChartUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * End-to-end simulation tests for validating Load Balancing and Spike Server diversion.
 * Generates visual charts for performance analysis.
 */
public class RoutingSimulationTest extends BaseSimulationTest {
    private static final String BASE_RES_DIR = "data/res/routing/";
    private static final String BASE_CHART_DIR = "data/chart/routing/";
    private static String commonTracePath;

    @BeforeAll
    public static void setup() throws IOException {
        // Ensure output directories exist using BaseTest utility
        ensureDirectories(BASE_RES_DIR, BASE_CHART_DIR);

        // Define a common workload for routing tests
        List<String> trace = new ArrayList<>();
        trace.add("5.0 4.0"); trace.add("6.0 4.0"); trace.add("7.0 4.0");
        trace.add("15.0 15.0");
        trace.add("20.0 2.0"); trace.add("21.0 2.0"); trace.add("22.0 2.0");
        trace.add("40.0 3.0"); trace.add("42.0 3.0"); trace.add("45.0 3.0");

        Path tempTrace = Files.createTempFile("common_routing_trace", ".txt");
        Files.write(tempTrace, trace);
        commonTracePath = tempTrace.toAbsolutePath().toString();
    }

    private void runRoutingTest(RoutingPolicy policy, String name) {
        logTestStep("Running Routing Simulation for policy: {}", policy);
        String csvPath = BASE_RES_DIR + name + ".csv";
        String chartPath = BASE_CHART_DIR + name + ".png";

        // Configure simulation
        ApplicationConfig config = TestConfigs.routing(commonTracePath, policy, 4, csvPath);

        // Execute simulation via Facade
        SimulationFacade facade = new SimulationFacade(config);
        facade.runSingleSimulation();

        // Generate visual chart
        logDebug("Generating chart for {}: {}", policy, chartPath);
        RoutingChartUtility.generateRoutingBalanceChart(csvPath, chartPath);
    }

    @Test public void testRoundRobinRouting() { runRoutingTest(RoutingPolicy.ROUND_ROBIN, "round_robin"); }
    @Test public void testRandomRouting() { runRoutingTest(RoutingPolicy.RANDOM, "random"); }
    @Test public void testLeastLoadedRouting() { runRoutingTest(RoutingPolicy.LEAST_LOADED, "least_loaded"); }
    @Test public void testPowerOfTwoChoicesRouting() { runRoutingTest(RoutingPolicy.POWER_OF_TWO, "power_of_two"); }

    @Test
    public void testSpikeServerRoutingDiversion() throws IOException {
        logTestStep("Testing Spike Server diversion with sawtooth workload");
        final int SI_MAX = 5;
        List<String> trace = new ArrayList<>();

        // Generate high-intensity jobs to trigger diversion
        for (int i = 0; i < 12; i++) trace.add(String.format(Locale.US, "%.4f 2.0", (i * 0.4)));
        trace.add("8.0 0.5");
        for (int i = 0; i < 8; i++) trace.add(String.format(Locale.US, "%.4f 1.0", (20.0 + i * 0.6)));

        String tracePath = createTraceFile(trace);
        String csvPath = "data/res/spike_diversion.csv";
        String chartPath = "data/chart/spike_diversion_chart.png";
        
        // Ensure main output directories exist
        ensureDirectories("data/res", "data/chart");

        // Custom config to compare Layer 1 and Layer 2 loads
        ApplicationConfig baseConfig = TestConfigs.routing(tracePath, RoutingPolicy.DETERMINISTIC, 1, csvPath);
        ApplicationConfig testConfig = new ApplicationConfig(
            new ApplicationConfig.LoadConfig(0.0, 0.0, 0.0, 0.0, SI_MAX, RoutingPolicy.DETERMINISTIC, it.uniroma2.pmcsn.configs.WorkloadType.TRACE, tracePath),
            ApplicationConfig.ClusterConfig.fixedServer(1, true),
            baseConfig.scaling(),
            ApplicationConfig.ExecutionConfig.singleRun(trace.size()),
            new ApplicationConfig.LoggingConfig(true, baseConfig.logging().format(), 
                                               it.uniroma2.pmcsn.configs.LoggingDataType.LOAD_COMPARISON, csvPath)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        facade.runSingleSimulation();

        // Verify diversion logic visually
        SystemChartUtility.generateLoadComparisonChart(csvPath, chartPath);
    }
}
