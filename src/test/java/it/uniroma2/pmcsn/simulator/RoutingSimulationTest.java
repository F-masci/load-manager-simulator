package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
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
        // 1 Web Server, siMax = 1. 20 Jobs.
        // J1 at t=1, J2 at t=1.1 -> J1 to WS, J2 to SS.
        // We do this 10 times.
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            trace.add((i * 10.0 + 1.0) + " 5.0");
            trace.add((i * 10.0 + 1.1) + " 5.0");
        }
        String tracePath = createTraceFile(trace);

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 1),
            new ApplicationConfig.ClusterConfig(1, 1, 1, true),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(20)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Spike Server Diversion with Trace (20 jobs)...");
        assertEquals(10.0, results.divertedJobs().mean(), "Exactly 10 jobs should be diverted");
        assertEquals(10.0, results.serverCompletions().get(1).mean(), "Web Server should complete 10 jobs");
    }
}
