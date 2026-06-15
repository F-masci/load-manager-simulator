package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * Command-line arguments parser for the Horizontal Scaler Simulator.
 * Decouples CLI parsing logic from the main application entry points.
 */
public class CommandLineConfigParser {

    /**
     * Parses command-line arguments and returns an ApplicationConfig instance.
     */
    public static ApplicationConfig parse(String[] args) {
        // Initialize with default values from constants
        double maxTime = ApplicationConfig.MAX_TIME;
        long seed = ApplicationConfig.SEED;
        double meanInterarrival = ApplicationConfig.MEAN_INTERARRIVAL;
        double cvInterarrival = ApplicationConfig.CV_INTERARRIVAL;
        double meanService = ApplicationConfig.MEAN_SERVICE;
        double cvService = ApplicationConfig.CV_SERVICE;
        int siMax = ApplicationConfig.SI_MAX;
        int webServers = ApplicationConfig.WEB_SERVER_COUNT;
        RoutingPolicy policy = ApplicationConfig.ROUTING_POLICY;
        String tracePath = ApplicationConfig.TRACE_PATH;
        double spikeCpu = ApplicationConfig.SPIKE_CPU_PERCENTAGE;
        WorkloadType workload = ApplicationConfig.WORKLOAD_TYPE;
        double scaleOutLimit = ApplicationConfig.SCALE_OUT_LIMIT;
        double scaleInLimit = ApplicationConfig.SCALE_IN_LIMIT;
        double scaleInterval = ApplicationConfig.SCALE_INTERVAL;
        double cooldown = ApplicationConfig.COOLDOWN;
        int minServers = ApplicationConfig.MIN_SERVERS;
        int maxServers = ApplicationConfig.MAX_SERVERS;
        boolean horizontalScalerEnabled = ApplicationConfig.HORIZONTAL_SCALER_ENABLED;
        double spikeUpperThreshold = ApplicationConfig.SPIKE_UPPER_THRESHOLD;
        double spikeLowerThreshold = ApplicationConfig.SPIKE_LOWER_THRESHOLD;
        boolean verticalScalerEnabled = ApplicationConfig.VERTICAL_SCALER_ENABLED;
        SimulationMethod method = ApplicationConfig.SIMULATION_METHOD;
        int replications = ApplicationConfig.NUM_REPLICATIONS;
        int batches = ApplicationConfig.NUM_BATCHES;
        int batchSize = ApplicationConfig.BATCH_SIZE;
        int warmUp = ApplicationConfig.WARM_UP_JOBS;

        // Logging
        boolean logEnabled = ApplicationConfig.LOGGING_ENABLED;
        LoggingFormat logFormat = ApplicationConfig.LOGGING_FORMAT;
        LoggingDataType logType = ApplicationConfig.LOGGING_DATA_TYPE;
        String logPath = ApplicationConfig.LOGGING_OUTPUT_PATH;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-time" -> maxTime = Double.parseDouble(args[++i]);
                    case "-seed" -> seed = Long.parseLong(args[++i]);
                    case "-meanInterarrival" -> meanInterarrival = Double.parseDouble(args[++i]);
                    case "-cvInterarrival" -> cvInterarrival = Double.parseDouble(args[++i]);
                    case "-meanService" -> meanService = Double.parseDouble(args[++i]);
                    case "-cvService" -> cvService = Double.parseDouble(args[++i]);
                    case "-simax" -> siMax = Integer.parseInt(args[++i]);
                    case "-webservers" -> webServers = Integer.parseInt(args[++i]);
                    case "-policy" -> policy = RoutingPolicy.valueOf(args[++i].toUpperCase());
                    case "-trace" -> { tracePath = args[++i]; workload = WorkloadType.TRACE; }
                    case "-share" -> spikeCpu = Double.parseDouble(args[++i]);
                    case "-workload" -> workload = WorkloadType.valueOf(args[++i].toUpperCase());
                    case "-scaleOutLimit" -> scaleOutLimit = Double.parseDouble(args[++i]);
                    case "-scaleInLimit" -> scaleInLimit = Double.parseDouble(args[++i]);
                    case "-scaleInterval" -> scaleInterval = Double.parseDouble(args[++i]);
                    case "-cooldown" -> cooldown = Double.parseDouble(args[++i]);
                    case "-minServers" -> minServers = Integer.parseInt(args[++i]);
                    case "-maxServers" -> maxServers = Integer.parseInt(args[++i]);
                    case "-horizontalScalerEnabled" -> horizontalScalerEnabled = Boolean.parseBoolean(args[++i]);
                    case "-spikeUpperThreshold" -> spikeUpperThreshold = Double.parseDouble(args[++i]);
                    case "-spikeLowerThreshold" -> spikeLowerThreshold = Double.parseDouble(args[++i]);
                    case "-verticalScalerEnabled" -> verticalScalerEnabled = Boolean.parseBoolean(args[++i]);
                    case "-method" -> method = SimulationMethod.valueOf(args[++i].toUpperCase());
                    case "-replications" -> replications = Integer.parseInt(args[++i]);
                    case "-maxTime" -> maxTime = Long.parseLong(args[++i]);
                    case "-batches" -> batches = Integer.parseInt(args[++i]);
                    case "-batchSize" -> batchSize = Integer.parseInt(args[++i]);
                    case "-warmUp" -> warmUp = Integer.parseInt(args[++i]);
                    case "-logEnabled" -> logEnabled = Boolean.parseBoolean(args[++i]);
                    case "-logFormat" -> logFormat = LoggingFormat.valueOf(args[++i].toUpperCase());
                    case "-logType" -> logType = LoggingDataType.valueOf(args[++i].toUpperCase());
                    case "-logPath" -> logPath = args[++i];
                    case "-h", "--help" -> { printUsage(); System.exit(0); }
                    default -> { System.err.println("Unknown parameter: " + args[i]); printUsage(); System.exit(1); }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printUsage();
            System.exit(1);
        }

        if (minServers == 1 && webServers != ApplicationConfig.WEB_SERVER_COUNT) {
            minServers = webServers;
        }

        return new ApplicationConfig(
            new ApplicationConfig.LoadConfig(meanInterarrival, cvInterarrival, meanService, cvService, siMax, policy, workload, tracePath),
            new ApplicationConfig.ClusterConfig(webServers, minServers, maxServers, true),
            new ApplicationConfig.ScalingConfig(scaleOutLimit, scaleInLimit, scaleInterval, cooldown, 
                                               spikeUpperThreshold, spikeLowerThreshold, spikeCpu, horizontalScalerEnabled, verticalScalerEnabled),
            new ApplicationConfig.ExecutionConfig(method, seed, replications, maxTime, 0, batches, batchSize, warmUp),
            new ApplicationConfig.LoggingConfig(logEnabled, logFormat, logType, logPath)
        );
    }

    public static void printUsage() {
        System.out.println("Usage: java it.pmcsn.LoadManagerSimulator [options]");
        System.out.println("Options:");
        System.out.println("  -time <double>                    Max simulation time");
        System.out.println("  -seed <long>                      RNG seed");
        System.out.println("  -interarrival <double>            Mean interarrival time");
        System.out.println("  -service <double>                 Mean service time");
        System.out.println("  -simax <int>                      SI_max threshold for spike server redirection");
        System.out.println("  -webservers <int>                 Number of active Web Servers");
        System.out.println("  -policy <ROUND_ROBIN|LEAST_LOADED> Load routing policy");
        System.out.println("  -trace <path>                     Path to trace file");
        System.out.println("  -share <double>                   Spike Server CPU capacity share");
        System.out.println("  -workload <DISTRIBUTION|HYPEREXPONENTIAL|TRACE> Workload type");
        System.out.println("  -scaleOutLimit <double>           System response time threshold to scale out");
        System.out.println("  -scaleInLimit <double>            System response time threshold to scale in");
        System.out.println("  -scaleInterval <double>           Moving window size for horizontal scaling");
        System.out.println("  -cooldown <double>                Minimum time between scaling actions");
        System.out.println("  -minServers <int>                 Minimum number of Web Servers");
        System.out.println("  -maxServers <int>                 Maximum number of Web Servers");
        System.out.println("  -spikeUpperThreshold <double>     Spike Server utilization threshold to scale up CPU");
        System.out.println("  -spikeLowerThreshold <double>     Spike Server utilization threshold to scale down CPU");
        System.out.println("  -method <INDEPENDENT_REPLICATIONS|BATCH_MEANS> Simulation method");
        System.out.println("  -replications <int>               Number of independent replications");
        System.out.println("  -maxTime <double>                 Max simulation time");
        System.out.println("  -batches <int>                    Number of batches");
        System.out.println("  -batchSize <int>                  Jobs per batch");
        System.out.println("  -warmUp <int>                     Jobs for warm-up period");
        System.out.println("  -logEnabled <boolean>             Enable/disable state logging");
        System.out.println("  -logFormat <CSV|JSON>             Output format for logs");
        System.out.println("  -logType <LOAD_COMPARISON|SYSTEM_METRICS> Type of data to log");
        System.out.println("  -logPath <path>                   Output file path for logs");
    }
}
