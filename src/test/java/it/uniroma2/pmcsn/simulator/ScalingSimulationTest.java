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

public class ScalingSimulationTest {
    private static final Logger logger = LoggerFactory.getLogger(ScalingSimulationTest.class);

    @TempDir
    Path tempDir;

    private String createTraceFile(List<String> lines) throws IOException {
        Path traceFile = tempDir.resolve("trace_scaling.txt");
        Files.write(traceFile, lines);
        return traceFile.toAbsolutePath().toString();
    }

    @Test
    public void testHorizontalScaleOut() throws IOException {
        // scaleUpLimit = 2.0 (RT). Window = 10.0.
        // Trace: 20 jobs, each with RT=5.
        // SCALE_CHECK at t=10. avgRT = 5.0 >= 2.0. Scale Out!
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            trace.add((i * 1.0) + " 5.0");
        }
        String tracePath = createTraceFile(trace);

        ApplicationConfig.ScalingConfig scaling = new ApplicationConfig.ScalingConfig(
            2.0, 0.1, 10.0, 10.0, 100.0, 0.0, 1.0, true, false
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(1, 1, 5, false),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(100.0) 
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Horizontal Scale Out with Trace (20 jobs)...");
        // Exact assertions
        assertEquals(4.0, results.scaleOutActions().mean(), 1e-4, "Exactly 4 horizontal scale-out should occur (1 -> 5 servers)");
        assertEquals(5, results.serverCompletions().size(), "Should have exactly 5 servers used");
        assertEquals(81.0, results.responseTime().mean(), 1e-4, "Exact Mean Response Time mismatch");
    }

    @Test
    public void testVerticalScaleUpOnSpikeServer() throws IOException {
        // SS threshold = 5 jobs. siMax = 0 (all to SS).
        // Trace: 20 jobs arriving close together, service 100.
        // SCALE_CHECK at t=10. SI will be high. Scale Up!
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            trace.add((i * 0.1) + " 100.0");
        }
        String tracePath = createTraceFile(trace);

        ApplicationConfig.ScalingConfig scaling = new ApplicationConfig.ScalingConfig(
            1000.0, 0.0, 10.0, 5.0, 5.0, 1.0, 4.0, true, true
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 0),
            new ApplicationConfig.ClusterConfig(1, 1, 1, true),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(15.0) 
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();

        logger.info("Verifying Vertical Scale Up with Trace (20 jobs)...");
        // Exact assertions
        assertEquals(1.0, results.scaleUpActions().mean(), 1e-4, "Exactly 1 vertical scale-up should occur");
        assertEquals(4.0000, results.spikeAvgSpeed().mean(), 1e-4, "Exact average speed multiplier mismatch");
    }
}
