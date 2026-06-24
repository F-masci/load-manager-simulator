package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.utils.LogFactory;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Concrete sliding-window implementation of the HorizontalScaler.
 */
public class MovingWindowHorizontalScaler extends HorizontalScaler {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(MovingWindowHorizontalScaler.class, "SCALER");

    private final double windowSize;
    private final Queue<JobCompletionRecord> window = new LinkedList<>();
    private boolean hasNewDataSinceLastEvaluation = false;

    /**
     * Record representing a single job completion.
     * @param completionTime The time the job completed
     * @param responseTime The response time of the job
     */
    private record JobCompletionRecord(double completionTime, double responseTime) {}

    /**
     * Constructs a MovingWindowHorizontalScaler using the provided application configuration.
     * @param config The application configuration
     */
    public MovingWindowHorizontalScaler(ApplicationConfig config) {
        this(config.scaling());
    }

    /**
     * Constructs a MovingWindowHorizontalScaler using the provided scaling configuration.
     * @param scalingConfig The scaling configuration
     */
    public MovingWindowHorizontalScaler(ApplicationConfig.ScalingConfig scalingConfig) {
        this(scalingConfig.scaleOutLimit(), scalingConfig.scaleInLimit(),
                scalingConfig.windowSize(), scalingConfig.cooldown());
    }

    /**
     * Constructs a MovingWindowHorizontalScaler with the specified thresholds, window size, and cooldown.
     * @param scaleOutThreshold The threshold for scaling out
     * @param scaleInThreshold The threshold for scaling in
     * @param windowSize The size of the moving window
     * @param cooldown The cooldown period
     */
    public MovingWindowHorizontalScaler(double scaleOutThreshold, double scaleInThreshold, double windowSize, double cooldown) {
        super(scaleOutThreshold, scaleInThreshold, cooldown);
        this.windowSize = windowSize;
        this.lastScalingTime = Double.NEGATIVE_INFINITY;
    }

    /**
     * Records a job completion and updates the moving window.
     * @param clock The current simulation clock
     * @param responseTime The response time of the completed job
     */
    @Override
    public void recordCompletion(double clock, double responseTime) {
        window.add(new JobCompletionRecord(clock, responseTime));
        while (window.size() > windowSize) {
            window.poll();
        }
        hasNewDataSinceLastEvaluation = true;
    }

    /**
     * Evaluates whether to scale the cluster based on the moving window average.
     * @param clock The current simulation clock
     * @param cluster The WebServerCluster to evaluate
     * @return true if a scaling action occurred, false otherwise
     */
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

        if (avgResponse >= scaleOutThreshold) {
            boolean scaled = cluster.scaleOut(clock);
            if (scaled) {
                lastScalingTime = clock;
                return true;
            }
        } else if (avgResponse <= scaleInThreshold) {
            boolean scaled = cluster.scaleIn(clock);
            if (scaled) {
                lastScalingTime = clock;
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the average response time from the current window.
     * @param clock The current simulation clock
     * @return The average response time
     */
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

    /**
     * Clears the moving window and resets evaluation flag.
     */
    @Override
    public void resetStatistics() {
        window.clear();
        hasNewDataSinceLastEvaluation = false;
    }

    /**
     * Gets the window size.
     * @return The window size
     */
    public double getWindowSize() {
        return windowSize;
    }
}
