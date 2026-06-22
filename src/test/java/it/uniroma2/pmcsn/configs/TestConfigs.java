package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * Utility class providing standardized ApplicationConfig instances for testing various system behaviors.
 */
public class TestConfigs {

    final static private int NUM_BATCHES = ApplicationConfig.NUM_BATCHES;
    final static private int BATCH_SIZE = ApplicationConfig.BATCH_SIZE;

    final static private double ROUTING_MAX_TIME = 50.0;
    final static private double H_SCALING_MAX_TIME = 45.0;
    final static private double V_SCALING_MAX_TIME = 25.0;

    /**
     * Generates a configuration for testing Horizontal Scaling (Scale-out/Scale-in).
     *
     * @param tracePath    Path to the trace file driving the workload.
     * @param outThreshold Response time threshold to trigger scale-out.
     * @param inThreshold  Response time threshold to trigger scale-in.
     * @param cooldown     Cooldown period between scaling actions.
     * @param csvPath      Output path for persistence.
     * @return A configuration for horizontal scaling lifecycle validation.
     */
    public static ApplicationConfig horizontalScaling(String tracePath, double outThreshold, double inThreshold, double cooldown, String csvPath) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.ROUND_ROBIN),
            new ApplicationConfig.ClusterConfig(1, 1, 5, false),
            ApplicationConfig.ScalingConfig.onlyHorizontal(outThreshold, inThreshold, cooldown),
            ApplicationConfig.ExecutionConfig.singleRun(H_SCALING_MAX_TIME),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );
    }

    /**
     * Generates a configuration for testing Vertical Scaling on a Spike Server.
     *
     * @param tracePath Path to the trace file.
     * @param upper     Utilization threshold to increase speed.
     * @param lower     Utilization threshold to decrease speed.
     * @param speed     Speed multiplier when scaled.
     * @param cooldown  Cooldown period.
     * @param csvPath   Output path for metrics.
     * @return A configuration for vertical scaling behavior analysis.
     */
    public static ApplicationConfig verticalScaling(String tracePath, double upper, double lower, double speed, double cooldown, String csvPath) {
        return new ApplicationConfig(
            ApplicationConfig.LoadConfig.traceDriven(tracePath, RoutingPolicy.DETERMINISTIC, 0),
            ApplicationConfig.ClusterConfig.fixedServer(1, true),
            ApplicationConfig.ScalingConfig.onlyVertical(upper, lower, speed, cooldown, ApplicationConfig.VERTICAL_INCREMENT),
            ApplicationConfig.ExecutionConfig.singleRun(V_SCALING_MAX_TIME),
            new ApplicationConfig.LoggingConfig(true, LoggingFormat.CSV, LoggingDataType.SCALING_METRICS, csvPath)
        );
    }

    /**
     * Generates a configuration for testing Load Balancing across multiple servers.
     *
     * @param tracePath  Path to the trace file.
     * @param policy     The routing policy to evaluate.
     * @param numServers Number of web servers in the cluster.
     * @param csvPath    Path where the routing balance metrics will be exported.
     * @return A configuration for routing balance evaluation.
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
