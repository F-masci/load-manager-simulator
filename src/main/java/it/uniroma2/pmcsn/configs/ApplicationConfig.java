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
    double spikeLowerThreshold,
    SimulationMethod simulationMethod,
    int numReplications,
    int numBatches,
    int batchSize,
    int warmUpJobs
) {

    // Base configuration and load
    public static final double MAX_TIME = 10000.0;
    public static final long SEED = 123456789L;
    public static final double MEAN_INTERARRIVAL = 2.0;
    public static final double MEAN_SERVICE = 1.5;
    public static final int SI_MAX = 5;

    // Server and routing configuration
    public static final int WEB_SERVER_COUNT = 3;
    public static final int WEB_SERVER_CAPACITY = 1;
    public static final int SPIKE_SERVER_CAPACITY = 10;
    public static final RoutingPolicy ROUTING_POLICY = RoutingPolicy.ROUND_ROBIN;
    public static final String TRACE_PATH = null; // Oppure String, se gestito come oggetto
    public static final double SPIKE_CPU_PERCENTAGE = 0.4;
    public static final WorkloadType WORKLOAD_TYPE = WorkloadType.HYPEREXPONENTIAL;

    // Autoscaling configuration
    public static final double SCALE_UP_LIMIT = 8.0;
    public static final double SCALE_DOWN_LIMIT = 2.0;
    public static final double SCALE_INTERVAL = 30.0;
    public static final double COOLDOWN = 30.0;
    public static final int MIN_SERVERS = 1;
    public static final int MAX_SERVERS = 10;
    public static final double SPIKE_UPPER_THRESHOLD = 0.70;
    public static final double SPIKE_LOWER_THRESHOLD = 0.30;

    // Simulation configuration
    public static final SimulationMethod SIMULATION_METHOD = SimulationMethod.INDEPENDENT_REPLICATIONS;
    public static final int NUM_REPLICATIONS = 10;
    public static final int NUM_BATCHES = 64;
    public static final int BATCH_SIZE = 1024;
    public static final int WARM_UP_JOBS = 1000;

    public ApplicationConfig() {
        this(MAX_TIME, SEED, MEAN_INTERARRIVAL, MEAN_SERVICE, SI_MAX, WEB_SERVER_COUNT, WEB_SERVER_CAPACITY,
                SPIKE_SERVER_CAPACITY, ROUTING_POLICY, TRACE_PATH, SPIKE_CPU_PERCENTAGE,
                WORKLOAD_TYPE.name());
    }

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
                SCALE_UP_LIMIT, SCALE_DOWN_LIMIT, SCALE_INTERVAL, COOLDOWN, webServersCount, MIN_SERVERS, SPIKE_UPPER_THRESHOLD, SPIKE_LOWER_THRESHOLD,
             SIMULATION_METHOD, NUM_REPLICATIONS, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
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
             scaleUpLimit, scaleDownLimit, scaleInterval, COOLDOWN, minServers, maxServers, SPIKE_UPPER_THRESHOLD, SPIKE_LOWER_THRESHOLD,
             SIMULATION_METHOD, NUM_REPLICATIONS, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
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
             scaleUpLimit, scaleDownLimit, scaleInterval, COOLDOWN, minServers, maxServers,
             spikeUpperThreshold, spikeLowerThreshold,
             SimulationMethod.INDEPENDENT_REPLICATIONS, NUM_REPLICATIONS, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
    }
}
