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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testHorizontalScalingBehavior() throws IOException {

        List<String> trace = new ArrayList<>();
        double currentTime = 0;

        // Phase 1: High Load (0-20s)
        for (; currentTime < 10; currentTime += 0.5) {
            trace.add(String.format(Locale.US, "%.4f 1.0", currentTime));
        }
        // Phase 2: Low Load (20-25s)
        for (; currentTime < 25; currentTime += 0.6) {
            trace.add(String.format(Locale.US, "%.4f 0.4", currentTime));
        }
        // Phase 3: Very Low Load (25-30s)
        for (; currentTime < 30; currentTime += 0.7) {
            trace.add(String.format(Locale.US, "%.4f 0.3", currentTime));
        }
        // Phase 4: Medium Load (30-50s)
        for (; currentTime < 50; currentTime += 1.5) {
            trace.add(String.format(Locale.US, "%.4f 1.5", currentTime));
        }
        // Phase 45: Low Load (50-75s)
        for (; currentTime < 75; currentTime += 0.5) {
            trace.add(String.format(Locale.US, "%.4f 0.6", currentTime));
        }
        // Phase 6: Very Low Load (75-100s)
        for (; currentTime < 100; currentTime += 0.5) {
            trace.add(String.format(Locale.US, "%.4f 0.1", currentTime));
        }

        String tracePath = createTraceFile(trace);
        
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String csvPath = "data/res/horizontal_scaling.csv";
        String chartPath = "data/chart/horizontal_scaling_chart.png";

        ApplicationConfig.ScalingConfig scaling = ApplicationConfig.ScalingConfig.onlyHorizontal(
            2.0, 0.5, 5.0
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(1, 1, 10, false),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(100.0),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        SimulationChartUtility.generateHorizontalScalingChart(csvPath, chartPath);

        logger.info("Horizontal Scaling Actions: Out={}, In={}", results.scaleOutActions().mean(), results.scaleInActions().mean());
        
        assertTrue(results.scaleOutActions().mean() > 0, "System should have scaled OUT");
        assertTrue(results.scaleInActions().mean() > 0, "System should have scaled IN");
    }

    @Test
    public void testVerticalScalingBehavior() throws IOException {

        List<String> trace = new ArrayList<>();
        final int cycleNum = 5;
        double currentTime = 0;
        
        // 5 Cycles of Burst and Low over 100s
        for (int cycle = 0; cycle < cycleNum; cycle++) {

            double endOfBurst = (cycle * 20.0) + 5.0;
            double endOfDrop  = (cycle * 20.0) + 10.0;
            double endOfCycle = (cycle * 20.0) + 20.0;

            // BURST: 5s
            for (; currentTime < endOfBurst; currentTime += 0.2) {
                trace.add(String.format(Locale.US, "%.4f 2.1", currentTime));
            }

            // LOW: 5s
            for (; currentTime < endOfDrop; currentTime += 1.0) {
                trace.add(String.format(Locale.US, "%.4f 0.2", currentTime));
            }

            // LOW: 10s
            for (; currentTime < endOfCycle; currentTime += 0.8) {
                trace.add(String.format(Locale.US, "%.4f 0.8", currentTime));
            }
        }
        
        String tracePath = createTraceFile(trace);
        
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String csvPath = "data/res/vertical_scaling.csv";
        String chartPath = "data/chart/vertical_scaling_chart.png";

        ApplicationConfig.ScalingConfig scaling = ApplicationConfig.ScalingConfig.onlyVertical(
            8.0, 2.0, 4.0, 2.0
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 0),
            ApplicationConfig.ClusterConfig.fixedServer(1, true),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(100.0),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        SimulationChartUtility.generateVerticalScalingChart(csvPath, chartPath);

        logger.info("Vertical Scaling Actions (5 cycles): Up={}, Down={}", results.scaleUpActions().mean(), results.scaleDownActions().mean());
        
        // Assertions
        assertTrue(results.scaleUpActions().mean() >= cycleNum, "System should have scaled UP multiple times");
        assertTrue(results.scaleDownActions().mean() >= cycleNum, "System should have scaled DOWN multiple times");
    }
}
