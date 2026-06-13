package it.uniroma2.pmcsn.model.load.scaler;

import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HorizontalScalerTest {

    @Test
    public void testMovingWindowHorizontalScalerScaleUpAndCooldown() {
        // scaleUpLimit = 4.0, scaleDownLimit = 1.0, windowSize = 30.0, cooldown = 100.0
        MovingWindowHorizontalScaler scaler = new MovingWindowHorizontalScaler(4.0, 1.0, 30.0, 100.0);
        WebServerCluster cluster = new WebServerCluster(1, 5);

        // Record response times: average will be 5.0 (exceeds scaleUpLimit = 4.0)
        scaler.recordCompletion(1.0, 5.0);
        scaler.recordCompletion(2.0, 5.0);

        // First evaluation: should trigger scale up
        assertTrue(scaler.evaluateScaling(10.0, cluster));
        assertEquals(10.0, scaler.getLastScalingTime(), 1e-9);
        assertEquals(2, cluster.getActiveServers().size());

        // Subsequent evaluation within cooldown (clock = 20.0, since 20 - 10 = 10 < 100)
        // Even if average remains high, it should not scale
        scaler.recordCompletion(15.0, 5.0);
        assertFalse(scaler.evaluateScaling(20.0, cluster));
        assertEquals(10.0, scaler.getLastScalingTime(), 1e-9);
        assertEquals(2, cluster.getActiveServers().size());
    }

    @Test
    public void testMovingWindowHorizontalScalerScaleDown() {
        MovingWindowHorizontalScaler scaler = new MovingWindowHorizontalScaler(4.0, 1.0, 30.0, 10.0);
        WebServerCluster cluster = new WebServerCluster(1, 5);

        // Scale it up first manually so active servers count is 2
        assertTrue(cluster.scaleOut(0.0));
        assertEquals(2, cluster.getActiveServers().size());

        // Record low response times: average will be 0.5 (below scaleDownLimit = 1.0)
        scaler.recordCompletion(1.0, 0.5);
        scaler.recordCompletion(2.0, 0.5);

        // First evaluation: should trigger scale down
        assertTrue(scaler.evaluateScaling(10.0, cluster));
        assertEquals(10.0, scaler.getLastScalingTime(), 1e-9);
        assertEquals(1, cluster.getActiveServers().size());
    }

    @Test
    public void testNoHorizontalScaler() {
        NoHorizontalScaler scaler = new NoHorizontalScaler();
        WebServerCluster cluster = new WebServerCluster(2, 5);

        scaler.recordCompletion(1.0, 10.0);
        scaler.recordCompletion(2.0, 10.0);

        // Should never trigger scaling
        assertFalse(scaler.evaluateScaling(10.0, cluster));
        assertEquals(0.0, scaler.getLastScalingTime(), 1e-9);
        assertEquals(2, cluster.getActiveServers().size());
    }
}
