package it.uniroma2.pmcsn.model.load.scaler;

import it.uniroma2.pmcsn.BaseTest;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Vertical Scalers, verifying server speed adjustments based on utilization.
 */
public class VerticalScalerTest extends BaseTest {

    /**
     * Verifies UtilizationThresholdVerticalScaler behavior (scaling speed up/down).
     * <p>
     * Utilization in this model is defined as the fraction of time the server was busy.
     * Thresholds: upper = 0.5, lower = 0.1.
     * Base speed = 1.0, Scaled speed = 2.0.
     * Cooldown = 10.0.
     */
    @Test
    public void testUtilizationThresholdVerticalScaler() {
        logTestStep("Testing Vertical Scaler (Utilization-based)");
        UtilizationThresholdVerticalScaler scaler = new UtilizationThresholdVerticalScaler(0.5, 0.1, 1.0, 2.0, 10.0);
        SpikeServer server = new SpikeServer(0, 1.0);

        // 1. Trigger Scale-Up
        logDebug("Phase 1: High utilization to trigger Scale-Up");
        Job job = new Job(1, 0.0, 10.0);
        server.acceptJob(job, 0.0);
        server.processJobs(5.0);
        server.updateStatistics(5.0);

        assertTrue(scaler.evaluateScaling(5.0, server), "Should scale up (utilization = 1.0 > 0.5)");
        assertEquals(2.0, server.getSpeedMultiplier(), 1e-9);

        // 2. Complete job and verify cooldown
        logDebug("Phase 2: Job completion and cooldown check");
        server.processJobs(1.0); // t=6.0
        server.completeJob(job, 6.0);
        server.updateStatistics(12.0); // utilization = 6/12 = 0.5 (on the edge)

        // Within cooldown (12 - 5 = 7 < 10)
        assertFalse(scaler.evaluateScaling(12.0, server), "Should NOT scale down during cooldown");

        // 3. Trigger Scale-Down after cooldown and low utilization
        logDebug("Phase 3: Low utilization after cooldown to trigger Scale-Down");
        // Total busy time = 6.0. At t=65.0, utilization = 6.0 / 65.0 ≈ 0.092 < 0.1
        server.updateStatistics(65.0);
        assertTrue(scaler.evaluateScaling(65.0, server), "Should scale down (utilization ≈ 0.09 < 0.1)");
        assertEquals(1.0, server.getSpeedMultiplier(), 1e-9);
    }

    /**
     * Verifies that NoVerticalScaler keeps the server speed constant regardless of load.
     */
    @Test
    public void testNoVerticalScaler() {
        logTestStep("Verifying NoVerticalScaler (constant speed)");
        NoVerticalScaler scaler = new NoVerticalScaler();
        SpikeServer server = new SpikeServer(0, 1.0);

        server.acceptJob(new Job(1, 0.0, 10.0), 0.0);
        server.processJobs(10.0);
        server.updateStatistics(10.0);

        assertFalse(scaler.evaluateScaling(10.0, server));
        assertEquals(1.0, server.getSpeedMultiplier(), 1e-9);
    }
}
