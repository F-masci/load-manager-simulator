package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.LoggingDataType;
import it.uniroma2.pmcsn.configs.LoggingFormat;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.chart.SimulationChartUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoutingSimulationTest {
    private static final Logger logger = LoggerFactory.getLogger(RoutingSimulationTest.class);

    @TempDir
    Path tempDir;

    private String createTraceFile(List<String> lines) throws IOException {
        Path traceFile = tempDir.resolve("trace_routing.txt");
        Files.write(traceFile, lines);
        return traceFile.toAbsolutePath().toString();
    }

    @Test
    public void testRoundRobinRoutingDistribution() throws IOException {
        // 3 Servers, 18 Jobs.
        // Expect each server to complete exactly 6 jobs.
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            trace.add((i + 1.0) + " 10.0");
        }
        String tracePath = createTraceFile(trace);

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(3, 3, 3, false),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(18)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Round Robin Distribution with Trace (18 jobs)...");
        results.serverCompletions().forEach((id, res) -> {
            logger.info("Server #{} Completed Jobs: {}", id, res.mean());
            assertEquals(6.0, res.mean(), "Server " + id + " should have exactly 6 jobs");
        });
    }

    @Test
    public void testLeastLoadedRoutingDistribution() throws IOException {
        // 2 Servers. 20 Jobs.
        // If we send jobs at the same time, it should alternate.
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            // Pairs of jobs at almost same time
            trace.add((i / 2 * 10.0) + " 5.0");
        }
        String tracePath = createTraceFile(trace);

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.LEAST_LOADED),
            new ApplicationConfig.ClusterConfig(2, 2, 2, false),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(20)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Least Loaded Distribution with Trace (20 jobs)...");
        results.serverCompletions().forEach((id, res) -> {
            logger.info("Server #{} Completed Jobs: {}", id, res.mean());
            assertEquals(10.0, res.mean(), "Server " + id + " should have exactly 10 jobs");
        });
    }

    @Test
    public void testSpikeServerRoutingDiversion() throws IOException {
        // Parameters for distinct "sawtooth" cycles with idle periods
        final int SI_MAX = 5;
        List<String> trace = new ArrayList<>();

        // Wave 1: t=0. Rapid influx, clears by t=20.
        for (int i = 0; i < 12; i++) {
            trace.add((i * 0.5) + " 5.0"); 
        }

        // Wave 2: t=60. Smaller influx, clears by t=80.
        for (int i = 0; i < 8; i++) {
            trace.add((60.0 + i * 1.0) + " 4.0");
        }

        // Wave 3: t=120. Heavy burst, clears by t=150.
        for (int i = 0; i < 20; i++) {
            trace.add((120.0 + i * 0.3) + " 3.0");
        }

        // Wave 4: t=200. Long wave, clears by t=250.
        for (int i = 0; i < 15; i++) {
            trace.add((200.0 + i * 1.5) + " 8.0");
        }
        
        final int TOTAL_JOBS = trace.size();
        String tracePath = createTraceFile(trace);

        // Paths for state logging and chart in project root
        Path resDir = Path.of("data", "res");
        Path chartDir = Path.of("data", "chart");
        Files.createDirectories(resDir);
        Files.createDirectories(chartDir);

        String csvPath = resDir.resolve("spike_diversion_state.csv").toAbsolutePath().toString();
        String chartPath = chartDir.resolve("spike_diversion_chart.png").toAbsolutePath().toString();

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, SI_MAX),
            new ApplicationConfig.ClusterConfig(1, 1, 1, true),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(TOTAL_JOBS),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.LOAD_COMPARISON, csvPath)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Spike Server Diversion with cyclic trace (55 jobs)...");
        
        // Exact assertions based on the deterministic trace logic
        assertEquals(35.0, results.divertedJobs().mean(), 1e-4, "Exactly 35 jobs should be diverted");
        assertEquals(20.0, results.serverCompletions().get(1).mean(), 1e-4, "Web Server should handle exactly 20 jobs");
        
        // Exact performance metrics for this trace
        assertEquals(35.7273, results.responseTime().mean(), 1e-4, "Mean Response Time mismatch");
        assertEquals(0.1913, results.throughput().mean(), 1e-4, "Mean Throughput mismatch");

        // Generate chart as requested by default for this test
        SimulationChartUtility.generateLoadComparisonChart(csvPath, chartPath);
        logger.info("Cyclic load comparison chart generated at: {}", chartPath);
        
        assertTrue(Files.exists(Path.of(csvPath)), "Dataset CSV should exist");
        assertTrue(Files.exists(Path.of(chartPath)), "Chart PNG should exist");
    }
}
