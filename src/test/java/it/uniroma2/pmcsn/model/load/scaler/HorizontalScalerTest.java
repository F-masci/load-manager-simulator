package it.uniroma2.pmcsn.model.load.scaler;

import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Horizontal Scalers, verifying scale-out and scale-in decisions based on response time.
 */
public class HorizontalScalerTest extends BaseTest {

    /**
     * Verifies that the MovingWindowHorizontalScaler correctly triggers scale-up
     * and respects the cooldown period.
     */
    @Test
    public void testMovingWindowHorizontalScalerScaleUpAndCooldown() {
        logTestStep("Testing Horizontal Scale-Up and Cooldown behavior");
        double upLimit = 4.0;
        double cooldown = 100.0;
        MovingWindowHorizontalScaler scaler = new MovingWindowHorizontalScaler(upLimit, 1.0, 30.0, cooldown);
        WebServerCluster cluster = new WebServerCluster(1, 5);

        // Record high response times: avg = 5.0 > 4.0
        scaler.recordCompletion(1.0, 5.0);
        scaler.recordCompletion(2.0, 5.0);

        assertTrue(scaler.evaluateScaling(10.0, cluster), "Should trigger scale-up");
        assertEquals(2, cluster.getActiveServers().size());

        // Within cooldown (10 + 10 = 20 < 110)
        assertFalse(scaler.evaluateScaling(20.0, cluster), "Should NOT scale during cooldown");
    }

    /**
     * Verifies that the MovingWindowHorizontalScaler correctly triggers scale-in when load is low.
     */
    @Test
    public void testMovingWindowHorizontalScalerScaleDown() {
        logTestStep("Testing Horizontal Scale-In behavior");
        MovingWindowHorizontalScaler scaler = new MovingWindowHorizontalScaler(4.0, 1.0, 30.0, 10.0);
        WebServerCluster cluster = new WebServerCluster(1, 5);
        cluster.scaleOut(0.0); // active = 2

        // Record low response times: avg = 0.5 < 1.0
        scaler.recordCompletion(1.0, 0.5);
        scaler.recordCompletion(2.0, 0.5);

        assertTrue(scaler.evaluateScaling(10.0, cluster), "Should trigger scale-in");
        assertEquals(1, cluster.getActiveServers().size());
    }

    /**
     * Verifies that NoHorizontalScaler never triggers any scaling action.
     */
    @Test
    public void testNoHorizontalScaler() {
        logTestStep("Verifying NoHorizontalScaler (static behavior)");
        NoHorizontalScaler scaler = new NoHorizontalScaler();
        WebServerCluster cluster = new WebServerCluster(2, 5);

        scaler.recordCompletion(1.0, 10.0);
        assertFalse(scaler.evaluateScaling(10.0, cluster));
        assertEquals(2, cluster.getActiveServers().size());
    }
}
