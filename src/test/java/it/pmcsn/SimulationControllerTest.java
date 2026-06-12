package it.pmcsn;

import it.pmcsn.configs.ApplicationConfig;
import it.pmcsn.controller.SimulationController;
import it.pmcsn.builder.SimulationBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the SimulationController running full simulations.
 */
public class SimulationControllerTest {

    @Test
    public void testSimulationControllerRun() {
        ApplicationConfig config = new ApplicationConfig(
            100.0, 123456789L, 2.0, 1.5, 3, 2, 1, 10,
            SimulationController.RoutingPolicy.ROUND_ROBIN, null, 0.4, "HYPEREXPONENTIAL"
        );

        SimulationController controller = new SimulationBuilder().config(config).build();
        assertNotNull(controller);
        controller.run();
        
        assertTrue(controller.getClock() <= 100.0);
    }

    @Test
    public void testAutoscalingIntegration() {
        // Extremely low scaleUpLimit (0.01) to force scaling under load
        ApplicationConfig config = new ApplicationConfig(
            200.0, 123456789L, 0.5, 2.0, 5, 1, 5, 10,
            SimulationController.RoutingPolicy.ROUND_ROBIN, null, 0.4, "HYPEREXPONENTIAL",
            0.01, 0.005, 5.0, 1, 5
        );

        SimulationController controller = new SimulationBuilder().config(config).build();
        assertNotNull(controller);
        controller.run();

        // The cluster should have scaled up to handle the load
        assertTrue(controller.getWebServerCluster().getAllServers().size() > 1);
    }

    @Test
    public void testVerticalScalingIntegration() {
        // We configure the simulation to divert jobs to SpikeServer and set a low spikeUpperThreshold (e.g. 0.05)
        // so that it easily scales up under load.
        ApplicationConfig config = new ApplicationConfig(
            200.0, 123456789L, 0.5, 2.0, 1, 1, 1, 10,
            SimulationController.RoutingPolicy.ROUND_ROBIN, null, 0.4, "HYPEREXPONENTIAL",
            8.0, 2.0, 30.0, 1, 1,
            0.05, 0.01
        );

        SimulationController controller = new SimulationBuilder().config(config).build();
        assertNotNull(controller);
        
        // Initial speed should be 1.0 (since baseSpeed = 0.4 / 0.4 = 1.0)
        assertEquals(1.0, controller.getSpikeServer().getSpeedMultiplier(), 0.001);

        controller.run();

        // Verify that the SpikeServer scaled up, meaning its speedMultiplier was changed to 2.0
        // (Wait: let's verify if utilization exceeded 0.05. If yes, it should have scaled up).
        // Since we had 200 time units, 0.5 mean arrival, 2.0 mean service, and siMax = 1,
        // it is highly likely that SpikeServer got heavily utilized and scaled up.
        // Let's assert that the speed multiplier at the end is 2.0, or at least changed during simulation,
        // or we can inspect the UtilizationThresholdVerticalScaler instance state.
        it.pmcsn.model.load.scaler.vertical.VerticalScaler verticalScaler = controller.getLoadManager().getVerticalScaler();
        assertTrue(verticalScaler instanceof it.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler);
        it.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler utvs = 
            (it.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler) verticalScaler;
        
        // Let's check if it scaled up
        assertTrue(utvs.isScaled());
        assertEquals(2.0, controller.getSpikeServer().getSpeedMultiplier(), 0.001);
    }

    @Test
    public void testCooldownLogic() {
        // We set up a Horizontal Scaler with cooldown = 100.0
        it.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler scaler = 
            new it.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler(0.1, 0.05, 30.0, 100.0);
        
        it.pmcsn.model.server.WebServerCluster cluster = new it.pmcsn.model.server.WebServerCluster(1, 5, 1);
        
        // Record high response time completions
        scaler.recordCompletion(1.0, 5.0);
        scaler.recordCompletion(2.0, 5.0);
        
        // First evaluation: should scale up (since no previous scaling action occurred)
        boolean scaled = scaler.evaluateScaling(10.0, cluster);
        assertTrue(scaled);
        assertEquals(10.0, scaler.getLastScalingTime(), 0.001);
        assertEquals(2, cluster.getActiveServers().size());
        
        // Second evaluation at clock = 20.0 (within cooldown of 100s, i.e., 20 - 10 = 10 < 100)
        // Even if response times are high, it should not scale up again.
        scaler.recordCompletion(11.0, 5.0);
        scaler.recordCompletion(12.0, 5.0);
        
        boolean scaledDuringCooldown = scaler.evaluateScaling(20.0, cluster);
        assertFalse(scaledDuringCooldown);
        assertEquals(10.0, scaler.getLastScalingTime(), 0.001);
        assertEquals(2, cluster.getActiveServers().size());
        
        // Third evaluation at clock = 120.0 (outside cooldown, i.e., 120 - 10 = 110 >= 100)
        // It should scale up again.
        scaler.recordCompletion(110.0, 5.0);
        scaler.recordCompletion(115.0, 5.0);
        
        boolean scaledAfterCooldown = scaler.evaluateScaling(120.0, cluster);
        assertTrue(scaledAfterCooldown);
        assertEquals(120.0, scaler.getLastScalingTime(), 0.001);
        assertEquals(3, cluster.getActiveServers().size());
    }
}

