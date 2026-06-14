package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * ApplicationConfig stores the configuration parameters for the simulation.
 * Modeled as a hierarchical structure of immutable records.
 */
public record ApplicationConfig(
    LoadConfig load,
    ClusterConfig cluster,
    ScalingConfig scaling,
    ExecutionConfig execution,
    LoggingConfig logging
) {

    // Base configuration constants
    public static final long SEED = 123456789L;
    public static final double MEAN_INTERARRIVAL = 0.40;
    public static final double CV_INTERARRIVAL = 4.0;
    public static final double MEAN_SERVICE = 0.25;
    public static final double CV_SERVICE = 4.0;
    public static final int SI_MAX = 30;

    // Server and routing configuration constants
    public static final int WEB_SERVER_COUNT = 1;
    public static final RoutingPolicy ROUTING_POLICY = RoutingPolicy.ROUND_ROBIN;
    public static final String TRACE_PATH = null;
    public static final double SPIKE_CPU_PERCENTAGE = 1.0;
    public static final WorkloadType WORKLOAD_TYPE = WorkloadType.HYPEREXPONENTIAL;
    public static final boolean SPIKE_ENABLED = true;

    // Autoscaling configuration constants
    public static final double SCALE_UP_LIMIT = 1.0;
    public static final double SCALE_DOWN_LIMIT = 0.05;
    public static final double SCALE_INTERVAL = 300.0;
    public static final double COOLDOWN = 300.0;
    public static final int MIN_SERVERS = 1;
    public static final int MAX_SERVERS = 1_000;
    public static final double SPIKE_UPPER_THRESHOLD = 50;
    public static final double SPIKE_LOWER_THRESHOLD = 5;

    // Simulation configuration constants
    public static final SimulationMethod SIMULATION_METHOD = SimulationMethod.INDEPENDENT_REPLICATIONS;
    public static final int NUM_REPLICATIONS = 10;
    public static final double MAX_TIME = Integer.MAX_VALUE;
    public static final int NUM_BATCHES = 64;
    public static final int BATCH_SIZE = 256;
    public static final int WARM_UP_JOBS = 0;

    // Logging configuration constants
    public static final boolean LOGGING_ENABLED = false;
    public static final LoggingFormat LOGGING_FORMAT = LoggingFormat.CSV;
    public static final LoggingDataType LOGGING_DATA_TYPE = LoggingDataType.LOAD_COMPARISON;
    public static final String LOGGING_OUTPUT_PATH = "simulation_state.csv";

    /**
     * Group of parameters related to workload and basic load routing.
     */
    public record LoadConfig(
        double meanInterarrival,
        double cvInterarrival,
        double meanService,
        double cvService,
        int siMax,
        RoutingPolicy routingPolicy,
        WorkloadType workloadType,
        String tracePath
    ) {

        public LoadConfig() {
            this(MEAN_INTERARRIVAL, MEAN_SERVICE);
        }

        public LoadConfig(double meanInterarrival, double meanService) {
            this(WORKLOAD_TYPE, meanInterarrival, meanService);
        }

        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService) {
            this(workloadType, meanInterarrival, meanService, ROUTING_POLICY);
        }

        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService, RoutingPolicy routingPolicy) {
            this(workloadType, meanInterarrival, meanService, routingPolicy, SI_MAX);
        }

        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService, RoutingPolicy routingPolicy, int siMax) {
            this(meanInterarrival, CV_INTERARRIVAL, meanService, CV_SERVICE, siMax, routingPolicy, workloadType, TRACE_PATH);
        }

        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double cvInterarrival, double meanService, double cvService) {
            this(workloadType, meanInterarrival, cvInterarrival, meanService, cvService, ROUTING_POLICY);
        }

        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double cvInterarrival, double meanService, double cvService, RoutingPolicy routingPolicy) {
            this(meanInterarrival, cvInterarrival, meanService, cvService, SI_MAX, routingPolicy, workloadType, TRACE_PATH);
        }

        public static LoadConfig singleExponentialServer(double meanInterarrival, double meanService) {
            return new LoadConfig(WorkloadType.EXPONENTIAL, meanInterarrival, meanService, RoutingPolicy.DETERMINISTIC);
        }

        public static LoadConfig singleHyperexponentialServer(double meanInterarrival, double meanService) {
            return singleHyperexponentialServer(meanInterarrival, CV_INTERARRIVAL, meanService, CV_SERVICE);
        }

        public static LoadConfig singleHyperexponentialServer(double meanInterarrival, double cvInterarrival, double meanService, double cvService) {
            return new LoadConfig(WorkloadType.HYPEREXPONENTIAL, meanInterarrival, cvInterarrival, meanService, cvService, RoutingPolicy.DETERMINISTIC);
        }

        public static LoadConfig traceDriven(String tracePath, RoutingPolicy policy, int siMax) {
            return new LoadConfig(0.0, 0.0, 0.0, 0.0, siMax, policy, WorkloadType.TRACE, tracePath);
        }

        public static LoadConfig traceDriven(String tracePath, RoutingPolicy policy) {
            return traceDriven(tracePath, policy, SI_MAX);
        }
    }

    /**
     * Group of parameters related to the Web Server Cluster size.
     */
    public record ClusterConfig(
        int webServersCount,
        int minServers,
        int maxServers,
        boolean spikeEnabled
    ) {
        public ClusterConfig() {
            this(WEB_SERVER_COUNT, MIN_SERVERS, MAX_SERVERS, SPIKE_ENABLED);
        }

        public static ClusterConfig singleServer() {
            return fixedServer(1);
        }

        public static ClusterConfig fixedServer(int numServers) {
            return new ClusterConfig(numServers, numServers, numServers, false);
        }
    }

    /**
     * Group of parameters related to autoscaling thresholds and resource shares.
     */
    public record ScalingConfig(
        double scaleUpLimit,
        double scaleDownLimit,
        double scaleInterval,
        double cooldown,
        double spikeUpperThreshold,
        double spikeLowerThreshold,
        double spikeCpuPercentage,
        boolean horizontalEnabled,
        boolean verticalEnabled
    ) {
        public ScalingConfig() {
            this(SCALE_UP_LIMIT, SCALE_DOWN_LIMIT, SCALE_INTERVAL, COOLDOWN, 
                 SPIKE_UPPER_THRESHOLD, SPIKE_LOWER_THRESHOLD, SPIKE_CPU_PERCENTAGE, true, true);
        }

        public static ScalingConfig disabled() {
            return new ScalingConfig(0, 0, 0, 0, 0, 0, SPIKE_CPU_PERCENTAGE, false, false);
        }
    }

    /**
     * Group of parameters related to the simulation execution method and statistical sampling.
     */
    public record ExecutionConfig(
        SimulationMethod method,
        long seed,
        int numReplications,
        double maxTime,
        int maxJobs,
        int numBatches,
        int batchSize,
        int warmUpJobs
    ) {
        public ExecutionConfig() {
            this(SIMULATION_METHOD, SEED, NUM_REPLICATIONS, MAX_TIME, 0, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
        }

        public static ExecutionConfig batchRun(int numBatches, int batchSize) {
            return batchRun(numBatches, batchSize, WARM_UP_JOBS);
        }

        public static ExecutionConfig batchRun(int numBatches, int batchSize, int warmUpJobs) {
            return batchRun(SEED, numBatches, batchSize, warmUpJobs);
        }

        public static ExecutionConfig batchRun(long seed, int numBatches, int batchSize, int warmUpJobs) {
            return new ExecutionConfig(SimulationMethod.BATCH_MEANS, seed, 0, MAX_TIME, 0, numBatches, batchSize, warmUpJobs);
        }

        public static ExecutionConfig singleRun(double maxTime) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, 1, maxTime, 0, 0, 0, 0);
        }

        public static ExecutionConfig singleRun(int maxJobs) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, 1, MAX_TIME, maxJobs, 0, 0, 0);
        }
    }

    /**
     * Group of parameters related to simulation state logging.
     */
    public record LoggingConfig(boolean enabled, LoggingFormat format, LoggingDataType dataType, String outputPath) {
        public LoggingConfig() {
            this(LOGGING_ENABLED, LOGGING_FORMAT, LOGGING_DATA_TYPE, LOGGING_OUTPUT_PATH);
        }
    }


    /* --- CONSTRUCTORS --- */

    public ApplicationConfig(LoadConfig load, ClusterConfig cluster, ScalingConfig scaling, ExecutionConfig execution) {
        this(load, cluster, scaling, execution, new LoggingConfig());
    }

    /**
     * Default constructor using all predefined constants.
     */
    public ApplicationConfig() {
        this(new LoadConfig(), new ClusterConfig(), new ScalingConfig(), new ExecutionConfig(), new LoggingConfig());
    }

    /**
     * Creates a copy of this config with a modified seed.
     */
    public ApplicationConfig withSeed(long newSeed) {
        return new ApplicationConfig(load, cluster, scaling,
                new ExecutionConfig(execution.method, newSeed, execution.numReplications, execution.maxTime,
                        execution.maxJobs, execution.numBatches, execution.batchSize, execution.warmUpJobs),
                logging);
    }
}
