package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * Stores the configuration parameters for the simulation in a hierarchical structure of immutable records.
 *
 * @param load load configuration
 * @param cluster cluster configuration
 * @param scaling scaling configuration
 * @param execution execution configuration
 * @param logging logging configuration
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
    public static final double MEAN_INTERARRIVAL = 0.20;
    public static final double CV_INTERARRIVAL = 4.0;
    public static final double MEAN_SERVICE = 0.25;
    public static final double CV_SERVICE = 4.0;
    public static final int SI_MAX = 40;
    public static final int SI_LOW = -1;

    // Server and routing configuration constants
    public static final int WEB_SERVER_COUNT = 1;
    public static final RoutingPolicy ROUTING_POLICY = RoutingPolicy.LEAST_LOADED;
    public static final String TRACE_PATH = null;
    public static final double SPIKE_CPU_PERCENTAGE = 1.0;
    public static final WorkloadType WORKLOAD_TYPE = WorkloadType.HYPEREXPONENTIAL;
    public static final boolean SPIKE_ENABLED = true;

    // Autoscaling configuration constants
    public static final double SCALE_OUT_LIMIT = 6.0;
    public static final double SCALE_IN_LIMIT = 3.5;
    public static final double COOLDOWN = 10.0;
    public static final double WINDOW_SIZE = (double) SI_MAX * 2.5;
    public static final int MIN_SERVERS = 1;
    public static final int MAX_SERVERS = 25;
    public static final boolean HORIZONTAL_SCALER_ENABLED = true;
    public static final double SPIKE_UPPER_THRESHOLD = (double) SI_MAX / 2.5;
    public static final double SPIKE_LOWER_THRESHOLD = SPIKE_UPPER_THRESHOLD * 0.7;
    public static final double VERTICAL_INCREMENT = 1.0;
    public static final boolean VERTICAL_SCALER_ENABLED = true;

    // Simulation configuration constants
    public static final SimulationMethod SIMULATION_METHOD = SimulationMethod.BATCH_MEANS;
    public static final int NUM_REPLICATIONS = 10;
    public static final double MAX_TIME = Integer.MAX_VALUE;
    public static final int NUM_BATCHES = 64;
    public static final int BATCH_SIZE = 8_192;

    // First 5 batches are to warm up
    public static final int WARM_UP_JOBS = 5 * BATCH_SIZE;

    // Logging configuration constants
    public static final boolean LOGGING_ENABLED = false;
    public static final LoggingFormat LOGGING_FORMAT = LoggingFormat.CSV;
    public static final LoggingDataType LOGGING_DATA_TYPE = LoggingDataType.LOAD_COMPARISON;
    public static final String LOGGING_OUTPUT_PATH = "simulation_state.csv";

    public static final double SLA_THRESHOLD = SCALE_OUT_LIMIT;

    /**
     * Configuration parameters for workload and load routing.
     *
     * @param meanInterarrival average time between arrivals
     * @param cvInterarrival coefficient of variation for inter-arrivals
     * @param meanService average service time
     * @param cvService coefficient of variation for service times
     * @param siMax maximum service intensity
     * @param siLow low service intensity threshold
     * @param routingPolicy routing policy for jobs
     * @param workloadType type of workload to generate
     * @param tracePath path to workload trace file
     */
    public record LoadConfig(
        double meanInterarrival,
        double cvInterarrival,
        double meanService,
        double cvService,
        int siMax,
        int siLow,
        RoutingPolicy routingPolicy,
        WorkloadType workloadType,
        String tracePath
    ) {

        /**
         * Initializes load configuration with default values.
         */
        public LoadConfig() {
            this(MEAN_INTERARRIVAL, MEAN_SERVICE);
        }

        /**
         * Initializes load configuration with mean inter-arrival and service times.
         *
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         */
        public LoadConfig(double meanInterarrival, double meanService) {
            this(WORKLOAD_TYPE, meanInterarrival, meanService);
        }

        /**
         * Initializes load configuration with workload type and mean times.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService) {
            this(workloadType, meanInterarrival, meanService, ROUTING_POLICY);
        }

        /**
         * Initializes load configuration with workload type, mean times, and routing policy.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         * @param routingPolicy job routing policy
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService, RoutingPolicy routingPolicy) {
            this(workloadType, meanInterarrival, meanService, routingPolicy, SI_MAX);
        }

        /**
         * Initializes load configuration with workload type, mean times, routing policy, and maximum intensity.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         * @param routingPolicy job routing policy
         * @param siMax maximum service intensity
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double meanService, RoutingPolicy routingPolicy, int siMax) {
            this(meanInterarrival, CV_INTERARRIVAL, meanService, CV_SERVICE, siMax, SI_LOW, routingPolicy, workloadType, TRACE_PATH);
        }

        /**
         * Initializes load configuration with workload type, mean times, and coefficients of variation.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param cvInterarrival coefficient of variation for inter-arrivals
         * @param meanService average service time
         * @param cvService coefficient of variation for service times
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double cvInterarrival, double meanService, double cvService) {
            this(workloadType, meanInterarrival, cvInterarrival, meanService, cvService, ROUTING_POLICY);
        }

        /**
         * Initializes load configuration with workload type, mean times, coefficients of variation, and routing policy.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param cvInterarrival coefficient of variation for inter-arrivals
         * @param meanService average service time
         * @param cvService coefficient of variation for service times
         * @param routingPolicy job routing policy
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double cvInterarrival, double meanService, double cvService, RoutingPolicy routingPolicy) {
            this(workloadType, meanInterarrival, cvInterarrival, meanService, cvService, routingPolicy, SI_MAX);
        }

        /**
         * Initializes load configuration with workload type, mean times, coefficients of variation, routing policy, and maximum intensity.
         *
         * @param workloadType type of workload
         * @param meanInterarrival average time between arrivals
         * @param cvInterarrival coefficient of variation for inter-arrivals
         * @param meanService average service time
         * @param cvService coefficient of variation for service times
         * @param routingPolicy job routing policy
         * @param siMax maximum service intensity
         */
        public LoadConfig(WorkloadType workloadType, double meanInterarrival, double cvInterarrival, double meanService, double cvService, RoutingPolicy routingPolicy, int siMax) {
            this(meanInterarrival, cvInterarrival, meanService, cvService, siMax, SI_LOW, routingPolicy, workloadType, TRACE_PATH);
        }

        /**
         * Creates a load configuration for a single exponential server.
         *
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         * @return exponential load configuration
         */
        public static LoadConfig singleExponentialServer(double meanInterarrival, double meanService) {
            return new LoadConfig(WorkloadType.EXPONENTIAL, meanInterarrival, meanService, RoutingPolicy.DETERMINISTIC);
        }

        /**
         * Creates a load configuration for a single hyperexponential server with default CVs.
         *
         * @param meanInterarrival average time between arrivals
         * @param meanService average service time
         * @return hyperexponential load configuration
         */
        public static LoadConfig singleHyperexponentialServer(double meanInterarrival, double meanService) {
            return singleHyperexponentialServer(meanInterarrival, CV_INTERARRIVAL, meanService, CV_SERVICE);
        }

        /**
         * Creates a load configuration for a single hyperexponential server.
         *
         * @param meanInterarrival average time between arrivals
         * @param cvInterarrival coefficient of variation for inter-arrivals
         * @param meanService average service time
         * @param cvService coefficient of variation for service times
         * @return hyperexponential load configuration
         */
        public static LoadConfig singleHyperexponentialServer(double meanInterarrival, double cvInterarrival, double meanService, double cvService) {
            return new LoadConfig(WorkloadType.HYPEREXPONENTIAL, meanInterarrival, cvInterarrival, meanService, cvService, RoutingPolicy.DETERMINISTIC);
        }

        /**
         * Creates a trace-driven load configuration.
         *
         * @param tracePath path to trace file
         * @param policy job routing policy
         * @param siMax maximum service intensity
         * @return trace-driven load configuration
         */
        public static LoadConfig traceDriven(String tracePath, RoutingPolicy policy, int siMax) {
            return new LoadConfig(0.0, 0.0, 0.0, 0.0, siMax, SI_LOW, policy, WorkloadType.TRACE, tracePath);
        }

        /**
         * Creates a trace-driven load configuration with default maximum intensity.
         *
         * @param tracePath path to trace file
         * @param policy job routing policy
         * @return trace-driven load configuration
         */
        public static LoadConfig traceDriven(String tracePath, RoutingPolicy policy) {
            return traceDriven(tracePath, policy, SI_MAX);
        }
    }

    /**
     * Configuration parameters for the web server cluster.
     *
     * @param webServersCount initial number of web servers
     * @param minServers minimum number of servers in cluster
     * @param maxServers maximum number of servers in cluster
     * @param spikeEnabled whether spike servers are enabled
     */
    public record ClusterConfig(
        int webServersCount,
        int minServers,
        int maxServers,
        boolean spikeEnabled
    ) {
        /**
         * Initializes cluster configuration with default values.
         */
        public ClusterConfig() {
            this(WEB_SERVER_COUNT, MIN_SERVERS, MAX_SERVERS, SPIKE_ENABLED);
        }

        /**
         * Creates a configuration for a single server cluster.
         *
         * @return single server cluster configuration
         */
        public static ClusterConfig singleServer() {
            return fixedServer(1);
        }

        /**
         * Creates a configuration for a fixed number of servers without spikes.
         *
         * @param numServers number of servers
         * @return fixed cluster configuration
         */
        public static ClusterConfig fixedServer(int numServers) {
            return fixedServer(numServers, false);
        }

        /**
         * Creates a configuration for a fixed number of servers.
         *
         * @param numServers number of servers
         * @param spikeEnabled whether spike servers are enabled
         * @return fixed cluster configuration
         */
        public static ClusterConfig fixedServer(int numServers, boolean spikeEnabled) {
            return new ClusterConfig(numServers, numServers, numServers, spikeEnabled);
        }
    }

    /**
     * Configuration parameters for autoscaling and resource management.
     *
     * @param scaleOutLimit CPU threshold to trigger horizontal scale-out
     * @param scaleInLimit CPU threshold to trigger horizontal scale-in
     * @param windowSize number of completions used by the horizontal moving window
     * @param cooldown time to wait after a scaling action
     * @param spikeUpperThreshold upper threshold for spike server activation
     * @param spikeLowerThreshold lower threshold for spike server deactivation
     * @param spikeCpuPercentage target CPU percentage for spike servers
     * @param verticalIncrement increment step for vertical scaling
     * @param horizontalEnabled whether horizontal scaling is enabled
     * @param verticalEnabled whether vertical scaling is enabled
     */
    public record ScalingConfig(
        double scaleOutLimit,
        double scaleInLimit,
        double windowSize,
        double cooldown,
        double spikeUpperThreshold,
        double spikeLowerThreshold,
        double spikeCpuPercentage,
        double verticalIncrement,
        boolean horizontalEnabled,
        boolean verticalEnabled
    ) {
        /**
         * Initializes scaling configuration with default values.
         */
        public ScalingConfig() {
            this(SCALE_OUT_LIMIT, SCALE_IN_LIMIT, WINDOW_SIZE, COOLDOWN,
                 SPIKE_UPPER_THRESHOLD, SPIKE_LOWER_THRESHOLD, SPIKE_CPU_PERCENTAGE, VERTICAL_INCREMENT, true, true);
        }

        /**
         * Creates a configuration with only horizontal scaling enabled.
         *
         * @param scaleOutLimit scale-out threshold
         * @param scaleInLimit scale-in threshold
         * @param cooldown scaling cooldown period
         * @return horizontal-only scaling configuration
         */
        public static ScalingConfig onlyHorizontal(double scaleOutLimit, double scaleInLimit, double windowSize, double cooldown) {
            return new ScalingConfig(scaleOutLimit, scaleInLimit, windowSize, cooldown, 0, 0, 0, 0, true, false);
        }

        /**
         * Creates a configuration with only vertical scaling enabled.
         *
         * @param spikeUpperThreshold activation threshold
         * @param spikeLowerThreshold deactivation threshold
         * @param spikeCpuPercentage target CPU percentage
         * @param increment vertical increment step
         * @param cooldown scaling cooldown period
         * @return vertical-only scaling configuration
         */
        public static ScalingConfig onlyVertical(double spikeUpperThreshold, double spikeLowerThreshold, double spikeCpuPercentage, double increment, double cooldown) {
            return new ScalingConfig(0, 0, 0, cooldown, spikeUpperThreshold, spikeLowerThreshold, spikeCpuPercentage, increment, false, true);
        }

        /**
         * Creates a configuration with all scaling disabled.
         *
         * @return disabled scaling configuration
         */
        public static ScalingConfig disabled() {
            return new ScalingConfig(0, 0, 0, 0, 0, 0, SPIKE_CPU_PERCENTAGE, 0, false, false);
        }
    }

    /**
     * Configuration parameters for simulation execution and statistical methods.
     *
     * @param method simulation methodology
     * @param seed random number generator seed
     * @param numReplications number of independent replications
     * @param maxTime maximum simulation time
     * @param maxJobs maximum number of jobs to process
     * @param numBatches number of batches for batch means method
     * @param batchSize number of jobs per batch
     * @param warmUpJobs number of jobs to discard during warm-up phase
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
        /**
         * Initializes execution configuration with default values.
         */
        public ExecutionConfig() {
            this(SIMULATION_METHOD, SEED, NUM_REPLICATIONS, MAX_TIME, 0, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
        }

        /**
         * Creates a batch-means execution configuration with default warm-up.
         *
         * @param numBatches number of batches
         * @param batchSize jobs per batch
         * @return batch-means execution configuration
         */
        public static ExecutionConfig batchRun(int numBatches, int batchSize) {
            return batchRun(numBatches, batchSize, WARM_UP_JOBS);
        }

        /**
         * Creates a batch-means execution configuration with custom warm-up.
         *
         * @param numBatches number of batches
         * @param batchSize jobs per batch
         * @param warmUpJobs jobs for warm-up
         * @return batch-means execution configuration
         */
        public static ExecutionConfig batchRun(int numBatches, int batchSize, int warmUpJobs) {
            return batchRun(SEED, numBatches, batchSize, warmUpJobs);
        }

        /**
         * Creates a batch-means execution configuration with custom seed and warm-up.
         *
         * @param seed random seed
         * @param numBatches number of batches
         * @param batchSize jobs per batch
         * @param warmUpJobs jobs for warm-up
         * @return batch-means execution configuration
         */
        public static ExecutionConfig batchRun(long seed, int numBatches, int batchSize, int warmUpJobs) {
            return new ExecutionConfig(SimulationMethod.BATCH_MEANS, seed, 0, MAX_TIME, 0, numBatches, batchSize, warmUpJobs);
        }

        /**
         * Creates a single-run configuration based on time.
         *
         * @param maxTime maximum simulation time
         * @return single-run configuration
         */
        public static ExecutionConfig singleRun(double maxTime) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, 1, maxTime, 0, 0, 0, 0);
        }

        /**
         * Creates a single-run configuration based on job count.
         *
         * @param maxJobs maximum number of jobs
         * @return single-run configuration
         */
        public static ExecutionConfig singleRun(int maxJobs) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, 1, MAX_TIME, maxJobs, 0, 0, 0);
        }

        /**
         * Creates a configuration for multiple independent replications based on job count.
         *
         * @param numReplications number of replications
         * @param maxJobs maximum jobs per replication
         * @return replications configuration
         */
        public static ExecutionConfig replications(int numReplications, int maxJobs) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, numReplications, MAX_TIME, maxJobs, 0, 0, 0);
        }

        /**
         * Creates a configuration for multiple independent replications based on time.
         *
         * @param numReplications number of replications
         * @param maxTime maximum time per replication
         * @return replications configuration
         */
        public static ExecutionConfig replications(int numReplications, double maxTime) {
            return new ExecutionConfig(SimulationMethod.INDEPENDENT_REPLICATIONS, SEED, numReplications, maxTime, 0, 0, 0, 0);
        }
    }

    /**
     * Configuration parameters for simulation state logging.
     *
     * @param enabled whether logging is enabled
     * @param format output format
     * @param dataType type of data to log
     * @param outputPath path to the output file
     */
    public record LoggingConfig(boolean enabled, LoggingFormat format, LoggingDataType dataType, String outputPath) {
        /**
         * Initializes logging configuration with default values.
         */
        public LoggingConfig() {
            this(LOGGING_ENABLED, LOGGING_FORMAT, LOGGING_DATA_TYPE, LOGGING_OUTPUT_PATH);
        }
    }


    /* --- CONSTRUCTORS --- */

    /**
     * Initializes application configuration with custom load, cluster, scaling, and execution settings.
     *
     * @param load load configuration
     * @param cluster cluster configuration
     * @param scaling scaling configuration
     * @param execution execution configuration
     */
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
     * Creates a copy of this configuration with a modified random seed.
     *
     * @param newSeed new random seed
     * @return updated application configuration
     */
    public ApplicationConfig withSeed(long newSeed) {
        return new ApplicationConfig(load, cluster, scaling,
                new ExecutionConfig(execution.method, newSeed, execution.numReplications, execution.maxTime,
                        execution.maxJobs, execution.numBatches, execution.batchSize, execution.warmUpJobs),
                logging);
    }
}
