package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * ApplicationConfig stores the configuration parameters for the simulation.
 * Modeled as a hierarchical structure of immutable records to prevent state sharing.
 */
public record ApplicationConfig(
    LoadConfig load,
    ClusterConfig cluster,
    ScalingConfig scaling,
    ExecutionConfig execution
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
    public static final double SCALE_UP_LIMIT = 8.0;
    public static final double SCALE_DOWN_LIMIT = 2.0;
    public static final double SCALE_INTERVAL = 30.0;
    public static final double COOLDOWN = 30.0;
    public static final int MIN_SERVERS = 1;
    public static final int MAX_SERVERS = 10;
    public static final double SPIKE_UPPER_THRESHOLD = 20;
    public static final double SPIKE_LOWER_THRESHOLD = 2;

    // Simulation configuration constants
    public static final SimulationMethod SIMULATION_METHOD = SimulationMethod.INDEPENDENT_REPLICATIONS;
    public static final int NUM_REPLICATIONS = 10;
    public static final double MAX_TIME = Integer.MAX_VALUE;
    public static final int NUM_BATCHES = 64;
    public static final int BATCH_SIZE = 256;
    public static final int WARM_UP_JOBS = 0;



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
            return new ClusterConfig(1, 1, 1, false);
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
        int numBatches,
        int batchSize,
        int warmUpJobs
    ) {
        public ExecutionConfig() {
            this(SIMULATION_METHOD, SEED, NUM_REPLICATIONS, MAX_TIME, NUM_BATCHES, BATCH_SIZE, WARM_UP_JOBS);
        }


        public static ExecutionConfig batchRun(int numBatches, int batchSize) {
            return batchRun(numBatches, batchSize, WARM_UP_JOBS);
        }

        public static ExecutionConfig batchRun(int numBatches, int batchSize, int warmUpJobs) {
            return batchRun(SEED, numBatches, batchSize, warmUpJobs);
        }

        public static ExecutionConfig batchRun(long seed, int numBatches, int batchSize, int warmUpJobs) {
            return new ExecutionConfig(SimulationMethod.BATCH_MEANS, seed, 0, MAX_TIME, numBatches, batchSize, warmUpJobs);
        }
    }



    /* --- CONSTRUCTORS --- */

    /**
     * Default constructor using all predefined constants.
     */
    public ApplicationConfig() {
        this(new LoadConfig(), new ClusterConfig(), new ScalingConfig(), new ExecutionConfig());
    }

    /**
     * Creates a copy of this config with a modified seed.
     */
    public ApplicationConfig withSeed(long newSeed) {
        return new ApplicationConfig(load, cluster, scaling,
                new ExecutionConfig(execution.method, newSeed, execution.numReplications, execution.maxTime,
                        execution.numBatches, execution.batchSize, execution.warmUpJobs));
    }

    /* --- SCENARIOS --- */

}
