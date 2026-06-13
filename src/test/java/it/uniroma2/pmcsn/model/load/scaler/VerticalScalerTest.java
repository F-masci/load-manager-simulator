package it.uniroma2.pmcsn.model.load.scaler;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VerticalScalerTest {

    @Test
    public void testUtilizationThresholdVerticalScaler() {
        // upperThreshold = 0.5, lowerThreshold = 0.1, baseSpeed = 1.0, scaledSpeed = 2.0, cooldown = 10.0
        // In the new model, utilization is the mean number of active jobs.
        UtilizationThresholdVerticalScaler scaler = new UtilizationThresholdVerticalScaler(0.5, 0.1, 1.0, 2.0, 10.0);
        SpikeServer server = new SpikeServer(0, 1.0);

        // Put a job to create utilization (mean jobs) > 0.5
        Job job = new Job(1, 0.0, 10.0);
        server.acceptJob(job, 0.0);
        
        // After 5.0 seconds, processing 5.0 time units means 1 job was active for 5s. Mean = 1.0
        server.processJobs(5.0);
        server.updateStatistics(5.0);

        // Evaluation: should scale up (since mean jobs 1.0 > 0.5)
        assertTrue(scaler.evaluateScaling(5.0, server));
        assertEquals(5.0, scaler.getLastScalingTime(), 1e-9);
        assertEquals(2.0, server.getSpeedMultiplier(), 1e-9);

        // Complete job to clear load
        server.completeJob(job, 6.0);
        server.updateStatistics(10.0); // mean jobs = 6.0/10.0 = 0.6 (still above 0.1)

        // Within cooldown (clock = 12.0, 12 - 5 = 7 < 10): should not scale down
        assertFalse(scaler.evaluateScaling(12.0, server));
        assertEquals(2.0, server.getSpeedMultiplier(), 1e-9);

        // Outside cooldown & mean jobs below 0.1 (clock = 65.0):
        // Total busy time = 6.0. At t=65.0, mean jobs = 6.0 / 65.0 = 0.0923 <= 0.1
        server.updateStatistics(65.0);
        assertTrue(scaler.evaluateScaling(65.0, server));
        assertEquals(1.0, server.getSpeedMultiplier(), 1e-9);
    }

    @Test
    public void testNoVerticalScaler() {
        NoVerticalScaler scaler = new NoVerticalScaler();
        SpikeServer server = new SpikeServer(0, 1.0);

        Job job = new Job(1, 0.0, 4.0);
        server.acceptJob(job, 0.0);
        server.processJobs(5.0);
        server.updateStatistics(5.0);

        // Should never scale
        assertFalse(scaler.evaluateScaling(5.0, server));
        assertEquals(1.0, server.getSpeedMultiplier(), 1e-9);
    }
}
