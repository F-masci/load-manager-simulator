package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.LoggingDataType;
import it.uniroma2.pmcsn.configs.LoggingFormat;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.chart.SimulationChartUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Individual tests for validating different routing policies.
 * Each test generates a specific chart to visualize the distribution behavior.
 */
public class RoutingSimulationTest {
    private static final Logger logger = LoggerFactory.getLogger(RoutingSimulationTest.class);
    private static final String BASE_RES_DIR = "data/res/routing/";
    private static final String BASE_CHART_DIR = "data/chart/routing/";
    private static String commonTracePath;

    @TempDir
    Path tempDir;

    @BeforeAll
    public static void setup() throws IOException {
        new File(BASE_RES_DIR).mkdirs();
        new File(BASE_CHART_DIR).mkdirs();

        List<String> trace = new ArrayList<>();
        
        trace.add("5.0 4.0");  // Job 1
        trace.add("6.0 4.0");  // Job 2
        trace.add("7.0 4.0");  // Job 3

        trace.add("15.0 15.0"); // Job 4

        trace.add("20.0 2.0");  // Job 5
        trace.add("21.0 2.0");  // Job 6
        trace.add("22.0 2.0");  // Job 7

        trace.add("40.0 3.0");  // Job 8
        trace.add("42.0 3.0");  // Job 9
        trace.add("45.0 3.0");  // Job 10

        Path tempTrace = Files.createTempFile("common_routing_trace", ".txt");
        Files.write(tempTrace, trace);
        commonTracePath = tempTrace.toAbsolutePath().toString();
    }

    private String createTraceFile(List<String> lines) throws IOException {
        Path traceFile = tempDir.resolve("trace_routing.txt");
        Files.write(traceFile, lines);
        return traceFile.toAbsolutePath().toString();
    }

    private void runRoutingTest(RoutingPolicy policy, String name) {
        String csvPath = BASE_RES_DIR + name + ".csv";
        String chartPath = BASE_CHART_DIR + name + ".png";

        final int numServer = 4;

        ApplicationConfig config = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(commonTracePath, policy),
            // new ApplicationConfig.LoadConfig(WorkloadType.EXPONENTIAL, ApplicationConfig.MEAN_INTERARRIVAL / numServer, ApplicationConfig.MEAN_SERVICE, policy),
            ApplicationConfig.ClusterConfig.fixedServer(numServer),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(60.0),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.ROUTING_BALANCE, csvPath)
        );

        logger.info("Starting routing simulation for policy: {}", policy);
        SimulationFacade facade = new SimulationFacade(config);
        facade.runSingleSimulation();

        SimulationChartUtility.generateRoutingBalanceChart(csvPath, chartPath);
    }

    @Test public void testRoundRobinRouting() { runRoutingTest(RoutingPolicy.ROUND_ROBIN, "round_robin"); }
    @Test public void testRandomRouting() { runRoutingTest(RoutingPolicy.RANDOM, "random"); }
    @Test public void testLeastLoadedRouting() { runRoutingTest(RoutingPolicy.LEAST_LOADED, "least_loaded"); }
    @Test public void testPowerOfTwoChoicesRouting() { runRoutingTest(RoutingPolicy.POWER_OF_TWO, "power_of_two"); }

    @Test
    public void testSpikeServerRoutingDiversion() throws IOException {
        // Parameters for distinct "sawtooth" cycles with idle periods
        final int SI_MAX = 5;
        List<String> trace = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            trace.add(String.format(Locale.US, "%.4f 2.0", (i * 0.4)));
        }
        trace.add("8.0 0.5");
        for (int i = 0; i < 8; i++) {
            trace.add(String.format(Locale.US, "%.4f 1.0", (20.0 + i * 0.6)));
        }

        final int TOTAL_JOBS = trace.size();
        String tracePath = createTraceFile(trace);

        // Paths for state logging and chart in project root
        Path resDir = Path.of("data", "res");
        Path chartDir = Path.of("data", "chart");
        Files.createDirectories(resDir);
        Files.createDirectories(chartDir);

        String csvPath = resDir.resolve("spike_diversion.csv").toAbsolutePath().toString();
        String chartPath = chartDir.resolve("spike_diversion_chart.png").toAbsolutePath().toString();

        ApplicationConfig testConfig = new ApplicationConfig(
                ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, SI_MAX),
                ApplicationConfig.ClusterConfig.fixedServer(1, true),
                ApplicationConfig.ScalingConfig.disabled(),
                ApplicationConfig.ExecutionConfig.singleRun(TOTAL_JOBS),
                new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.LOAD_COMPARISON, csvPath)
        );

        logger.info("Starting routing simulation for spike");
        SimulationFacade facade = new SimulationFacade(testConfig);
        facade.runSingleSimulation();

        SimulationChartUtility.generateLoadComparisonChart(csvPath, chartPath);

    }
}
