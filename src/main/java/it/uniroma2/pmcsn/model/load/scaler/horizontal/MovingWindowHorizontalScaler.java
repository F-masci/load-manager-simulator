package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Concrete sliding-window implementation of the HorizontalScaler.
 */
public class MovingWindowHorizontalScaler extends HorizontalScaler {
    private static final Logger logger = LoggerFactory.getLogger(MovingWindowHorizontalScaler.class);

    private final double windowSize;
    private final Queue<JobCompletionRecord> window = new LinkedList<>();
    private boolean hasNewDataSinceLastEvaluation = false;

    private record JobCompletionRecord(double completionTime, double responseTime) {}

    public MovingWindowHorizontalScaler(ApplicationConfig config) {
        this(config.scaling());
    }

    public MovingWindowHorizontalScaler(ApplicationConfig.ScalingConfig scalingConfig) {
        this(scalingConfig.scaleUpLimit(), scalingConfig.scaleDownLimit(),
                scalingConfig.scaleInterval(), scalingConfig.cooldown());
    }

    public MovingWindowHorizontalScaler(double scaleUpThreshold, double scaleDownThreshold, double windowSize, double cooldown) {
        super(scaleUpThreshold, scaleDownThreshold, cooldown);
        this.windowSize = windowSize;
        this.lastScalingTime = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void recordCompletion(double clock, double responseTime) {
        window.add(new JobCompletionRecord(clock, responseTime));
        while (window.size() > windowSize) {
            window.poll();
        }
        hasNewDataSinceLastEvaluation = true;
    }

    @Override
    public boolean evaluateScaling(double clock, WebServerCluster cluster) {
        if (!hasNewDataSinceLastEvaluation) {
            return false;
        }

        final double remainingCooldown = getRemainingCooldown(clock);
        if (remainingCooldown >= 0.01) return false;
        
        hasNewDataSinceLastEvaluation = false;

        double avgResponse = getCurrentMetric(clock);
        if (window.isEmpty()) {
            return false;
        }

        logger.debug("Horizontal scaling evaluation: avgResponseTime = {}, activeWindowSize = {}", avgResponse, window.size());

        if (avgResponse >= scaleUpThreshold) {
            boolean scaled = cluster.scaleOut(clock);
            if (scaled) {
                lastScalingTime = clock;
                return true;
            }
        } else if (avgResponse <= scaleDownThreshold) {
            boolean scaled = cluster.scaleIn(clock);
            if (scaled) {
                lastScalingTime = clock;
                return true;
            }
        }
        return false;
    }

    @Override
    public double getCurrentMetric(double clock) {
        if (window.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (JobCompletionRecord r : window) {
            sum += r.responseTime();
        }
        return sum / window.size();
    }

    @Override
    public void resetStatistics() {
        window.clear();
        hasNewDataSinceLastEvaluation = false;
    }

    public double getWindowSize() {
        return windowSize;
    }
}
