package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.TestConfigs;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.chart.ScalingChartUtility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end simulation tests for Horizontal and Vertical Autoscaling.
 */
public class ScalingSimulationTest extends BaseSimulationTest {

    @Test
    public void testHorizontalScalingBehavior() throws IOException {
        logTestStep("Starting Horizontal Scaling behavior test");
        
        // Define workload that forces scale-out and later allows scale-in
        List<String> trace = new ArrayList<>();
        trace.add("0.0 2.0"); trace.add("1.0 1.5"); trace.add("1.5 1.0"); trace.add("2.0 0.5");
        trace.add("11.0 2.5"); trace.add("11.0 2.5");
        trace.add("15.0 0.2"); trace.add("15.0 0.2"); trace.add("15.0 0.2");
        trace.add("25.0 0.5"); trace.add("25.0 0.5");
        trace.add("35.0 2.0"); trace.add("35.0 2.0"); trace.add("35.0 2.0");

        String tracePath = createTraceFile(trace);
        String csvPath = "data/res/scaling/horizontal_scaling.csv";
        String chartPath = "data/chart/scaling/horizontal_scaling_chart.png";
        
        // Prepare directories for scaling outputs
        ensureDirectories("data/res/scaling", "data/chart/scaling");

        // Run simulation with specific thresholds and logging path
        ApplicationConfig testConfig = TestConfigs.horizontalScaling(tracePath, 2.0, 0.5, 4.0, csvPath);
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        // Generate metrics chart from the saved CSV
        ScalingChartUtility.generateHorizontalScalingChart(csvPath, chartPath);

        // Verify that scaling actions actually occurred
        logDebug("Actions: ScaleOut={}, ScaleIn={}", results.scaleOutActions().mean(), results.scaleInActions().mean());
        assertTrue(results.scaleOutActions().mean() > 0, "System should have scaled OUT");
        assertTrue(results.scaleInActions().mean() > 0, "System should have scaled IN");
    }

    @Test
    public void testVerticalScalingBehavior() throws IOException {
        logTestStep("Starting Vertical Scaling behavior test");

        List<String> trace = new ArrayList<>();
        // Phase 1: Sudden spike to trigger Speed-UP
        for (double t = 1.0; t < 3.0; t += 0.3) trace.add(t + " 4.0");
        // Phase 2: Idle period to trigger Speed-DOWN
        for (double t = 12.0; t < 25.0; t += 3.0) trace.add(t + " 0.5");
        
        String tracePath = createTraceFile(trace);
        String csvPath = "data/res/scaling/vertical_scaling.csv";
        String chartPath = "data/chart/scaling/vertical_scaling_chart.png";

        // Prepare directories for scaling outputs
        ensureDirectories("data/res/scaling", "data/chart/scaling");

        // Execute vertical scaling simulation with custom logging path
        ApplicationConfig testConfig = TestConfigs.verticalScaling(tracePath, 3.0, 1.0, 4.0, 4.0, csvPath);
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        // Generate Speed-UP/DOWN chart
        ScalingChartUtility.generateVerticalScalingChart(csvPath, chartPath);

        // Verify speed adjustments
        logDebug("Actions: ScaleUp={}, ScaleDown={}", results.scaleUpActions().mean(), results.scaleDownActions().mean());
        assertTrue(results.scaleUpActions().mean() > 0, "System should have scaled UP");
        assertTrue(results.scaleDownActions().mean() > 0, "System should have scaled DOWN");
    }
}
