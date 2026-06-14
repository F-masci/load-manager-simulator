package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * Utility class providing standardized ApplicationConfig instances for testing various system behaviors.
 * Centralizes the definition of common test scenarios to ensure consistency across the test suite.
 */
public class TestConfigs {

    final static private int NUM_BATCHES = ApplicationConfig.NUM_BATCHES;
    final static private int BATCH_SIZE = ApplicationConfig.BATCH_SIZE;

    final static private double ROUTING_MAX_TIME = 50.0;
    final static private double H_SCALING_MAX_TIME = 45.0;
    final static private double V_SCALING_MAX_TIME = 25.0;

    /**
     * Generates an M/M/1 configuration for simple system validation.
     */
    public static ApplicationConfig mm1(double arrivalMean, double serviceMean) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.singleExponentialServer(arrivalMean, serviceMean),
            ApplicationConfig.ClusterConfig.singleServer(),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.batchRun(NUM_BATCHES, BATCH_SIZE)
        );
    }

    /**
     * Generates a configuration for testing Horizontal Scaling (Scale-out/Scale-in).
     */
    public static ApplicationConfig horizontalScaling(String tracePath, double upThreshold, double downThreshold, double cooldown, String csvPath) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(1, 1, 5, false),
            ApplicationConfig.ScalingConfig.onlyHorizontal(upThreshold, downThreshold, cooldown),
            ApplicationConfig.ExecutionConfig.singleRun(H_SCALING_MAX_TIME),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );
    }

    /**
     * Generates a configuration for testing Vertical Scaling on a Spike Server.
     */
    public static ApplicationConfig verticalScaling(String tracePath, double upper, double lower, double speed, double cooldown, String csvPath) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 0),
            ApplicationConfig.ClusterConfig.fixedServer(1, true),
            ApplicationConfig.ScalingConfig.onlyVertical(upper, lower, speed, cooldown),
            ApplicationConfig.ExecutionConfig.singleRun(V_SCALING_MAX_TIME),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );
    }

    /**
     * Generates a configuration for testing Load Balancing across multiple servers.
     */
    public static ApplicationConfig routing(String tracePath, RoutingPolicy policy, int numServers, String csvPath) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, policy),
            ApplicationConfig.ClusterConfig.fixedServer(numServers),
            ApplicationConfig.ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.singleRun(ROUTING_MAX_TIME),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.ROUTING_BALANCE, csvPath)
        );
    }
}
