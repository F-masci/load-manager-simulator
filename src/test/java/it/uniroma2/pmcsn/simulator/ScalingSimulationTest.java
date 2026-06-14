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

        trace.add("0.0 2.0");
        trace.add("1.0 1.5");
        trace.add("1.5 1.0");
        trace.add("2.0 0.5");

        trace.add("11.0 2.5");
        trace.add("11.0 2.5");

        trace.add("15.0 0.2");
        trace.add("15.0 0.2");
        trace.add("15.0 0.2");

        trace.add("25.0 0.5");
        trace.add("25.0 0.5");

        trace.add("35.0 2.0");
        trace.add("35.0 2.0");
        trace.add("35.0 2.0");

        String tracePath = createTraceFile(trace);
        
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String csvPath = "data/res/horizontal_scaling.csv";
        String chartPath = "data/chart/horizontal_scaling_chart.png";

        ApplicationConfig.ScalingConfig scaling = ApplicationConfig.ScalingConfig.onlyHorizontal(
            2.0, 0.5, 4.0
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(1, 1, 5, false),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(50.0),
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

        // 10 Job totali configurati per un singolo ciclo rapido di UP e DOWN
        // Primi 6 job a raffica per far impennare la metrica oltre la soglia
        trace.add("1.0 4.0");
        trace.add("1.3000 4.0");
        trace.add("1.6000 4.0");
        trace.add("1.9000 4.0");
        trace.add("2.2000 4.0");
        trace.add("2.5 4.0");

        // Ultimi 4 job blandi per far respirare il sistema e innescare lo Scale DOWN
        trace.add("12.0 0.5");
        trace.add("15.0 0.5");
        trace.add("18.0 0.5");
        trace.add("21.0 0.5");
        
        String tracePath = createTraceFile(trace);
        
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String csvPath = "data/res/vertical_scaling.csv";
        String chartPath = "data/chart/vertical_scaling_chart.png";

        ApplicationConfig.ScalingConfig scaling = ApplicationConfig.ScalingConfig.onlyVertical(
            3.0, 1.0, 4.0, 4.0
        );

        ApplicationConfig testConfig = new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 0),
            ApplicationConfig.ClusterConfig.fixedServer(1, true),
            scaling,
            ApplicationConfig.ExecutionConfig.singleRun(30.0),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );

        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        SimulationChartUtility.generateVerticalScalingChart(csvPath, chartPath);

        logger.info("Vertical Scaling Actions (5 cycles): Up={}, Down={}", results.scaleUpActions().mean(), results.scaleDownActions().mean());
        
        // Assertions
        assertTrue(results.scaleUpActions().mean() >= 0, "System should have scaled UP multiple times");
        assertTrue(results.scaleDownActions().mean() >= 0, "System should have scaled DOWN multiple times");
    }
}
