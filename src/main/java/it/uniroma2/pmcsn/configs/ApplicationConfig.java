package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * ApplicationConfig stores the configuration parameters for the simulation.
 * It is modeled as an immutable Java record.
 */
public record ApplicationConfig(
    double maxTime,
    long seed,
    double meanInterarrival,
    double meanService,
    int siMax,
    int webServersCount,
    int webServerCapacity,
    int spikeServerCapacity,
    RoutingPolicy routingPolicy,
    String tracePath,
    double spikeCpuPercentage,
    WorkloadType workloadType,
    double scaleUpLimit,
    double scaleDownLimit,
    double scaleInterval,
    double cooldown,
    int minServers,
    int maxServers,
    double spikeUpperThreshold,
    double spikeLowerThreshold
) {
    /**
     * Overloaded constructor for backward compatibility.
     */
    public ApplicationConfig(double maxTime, long seed, double meanInterarrival, double meanService, int siMax,
                             int webServersCount, int webServerCapacity, int spikeServerCapacity,
                             RoutingPolicy routingPolicy, String tracePath, double spikeCpuPercentage,
                             String workloadType) {
        this(maxTime, seed, meanInterarrival, meanService, siMax, webServersCount, webServerCapacity,
             spikeServerCapacity, routingPolicy, tracePath, spikeCpuPercentage,
             WorkloadType.valueOf(workloadType.toUpperCase()),
             8.0, 2.0, 30.0, 30.0, webServersCount, 10, 0.70, 0.30);
    }

    /**
     * Overloaded constructor for 17 parameters.
     */
    public ApplicationConfig(double maxTime, long seed, double meanInterarrival, double meanService, int siMax,
                             int webServersCount, int webServerCapacity, int spikeServerCapacity,
                             RoutingPolicy routingPolicy, String tracePath, double spikeCpuPercentage,
                             String workloadType, double scaleUpLimit, double scaleDownLimit,
                             double scaleInterval, int minServers, int maxServers) {
        this(maxTime, seed, meanInterarrival, meanService, siMax, webServersCount, webServerCapacity,
             spikeServerCapacity, routingPolicy, tracePath, spikeCpuPercentage,
             WorkloadType.valueOf(workloadType.toUpperCase()),
             scaleUpLimit, scaleDownLimit, scaleInterval, 30.0, minServers, maxServers, 0.70, 0.30);
    }

    /**
     * Overloaded constructor for 19 parameters (String workloadType support).
     */
    public ApplicationConfig(double maxTime, long seed, double meanInterarrival, double meanService, int siMax,
                             int webServersCount, int webServerCapacity, int spikeServerCapacity,
                             RoutingPolicy routingPolicy, String tracePath, double spikeCpuPercentage,
                             String workloadType, double scaleUpLimit, double scaleDownLimit,
                             double scaleInterval, int minServers, int maxServers,
                             double spikeUpperThreshold, double spikeLowerThreshold) {
        this(maxTime, seed, meanInterarrival, meanService, siMax, webServersCount, webServerCapacity,
             spikeServerCapacity, routingPolicy, tracePath, spikeCpuPercentage,
             WorkloadType.valueOf(workloadType.toUpperCase()),
             scaleUpLimit, scaleDownLimit, scaleInterval, 30.0, minServers, maxServers,
             spikeUpperThreshold, spikeLowerThreshold);
    }

    /**
     * Factory method creating a default configuration.
     */
    public static ApplicationConfig defaultConfiguration() {
        return new ApplicationConfig(
            10000.0,
            123456789L,
            2.0,
            1.5,
            5,
            3,
            1,
            10,
            RoutingPolicy.ROUND_ROBIN,
            null,
            0.4, // default 40% CPU share
            WorkloadType.DISTRIBUTION,
            8.0,   // scaleUpLimit
            2.0,   // scaleDownLimit
            30.0,  // scaleInterval (window size)
            30.0,  // cooldown (default 30 seconds)
            1,     // minServers
            10,    // maxServers
            0.70,  // spikeUpperThreshold
            0.30   // spikeLowerThreshold
        );
    }
}
