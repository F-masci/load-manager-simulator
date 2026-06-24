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

    /**
     * Verifies Horizontal Scaling behavior (Scale-OUT and Scale-IN) driven by response time.
     *
     * @throws IOException If trace file creation fails.
     */
    @Test
    public void testHorizontalScalingBehavior() throws IOException {
        logTestStep("Starting Horizontal Scaling behavior test");
        
        List<String> trace = new ArrayList<>();
        trace.add("0.0 2.0"); trace.add("1.0 1.5"); trace.add("1.5 1.0"); trace.add("2.0 0.5");
        trace.add("11.0 2.5"); trace.add("11.0 2.5");
        trace.add("15.0 0.2"); trace.add("15.0 0.2"); trace.add("15.0 0.2");
        trace.add("25.0 0.5"); trace.add("25.0 0.5");
        trace.add("35.0 2.0"); trace.add("35.0 2.0"); trace.add("35.0 2.0");

        String tracePath = createTraceFile(trace);
        String csvPath = "data/vv/res/scaling/horizontal_scaling.csv";
        String chartPath = "data/vv/chart/scaling/horizontal_scaling_chart.png";
        
        ensureDirectories("data/vv/res/scaling", "data/vv/chart/scaling");

        ApplicationConfig testConfig = TestConfigs.horizontalScaling(tracePath, 2.0, 0.5, 4.0, 4.0, csvPath);
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        ScalingChartUtility.generateHorizontalScalingChart(csvPath, chartPath);

        logDebug("Actions: ScaleOut={}, ScaleIn={}", results.scaleOutActions().mean(), results.scaleInActions().mean());
        assertTrue(results.scaleOutActions().mean() > 0, "System should have scaled OUT");
        assertTrue(results.scaleInActions().mean() > 0, "System should have scaled IN");
    }

    /**
     * Verifies Vertical Scaling behavior (Speed-UP and Speed-DOWN) based on Spike Server utilization.
     *
     * @throws IOException If trace file creation fails.
     */
    @Test
    public void testVerticalScalingBehavior() throws IOException {
        logTestStep("Starting Vertical Scaling behavior test");

        List<String> trace = new ArrayList<>();
        for (double t = 1.0; t < 3.0; t += 0.3) trace.add(t + " 4.0");
        for (double t = 12.0; t < 25.0; t += 3.0) trace.add(t + " 0.5");
        
        String tracePath = createTraceFile(trace);
        String csvPath = "data/vv/res/scaling/vertical_scaling.csv";
        String chartPath = "data/vv/chart/scaling/vertical_scaling_chart.png";

        ensureDirectories("data/vv/res/scaling", "data/vv/chart/scaling");

        ApplicationConfig testConfig = TestConfigs.verticalScaling(tracePath, 3.0, 1.0, 4.0, 4.0, csvPath);
        SimulationFacade facade = new SimulationFacade(testConfig);
        SimulationFacade.AggregatedResults results = facade.runSingleSimulation();
        
        ScalingChartUtility.generateVerticalScalingChart(csvPath, chartPath);

        logDebug("Actions: ScaleUp={}, ScaleDown={}", results.scaleUpActions().mean(), results.scaleDownActions().mean());
        assertTrue(results.scaleUpActions().mean() > 0, "System should have scaled UP");
        assertTrue(results.scaleDownActions().mean() > 0, "System should have scaled DOWN");
    }
}
