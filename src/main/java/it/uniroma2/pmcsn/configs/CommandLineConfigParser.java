package it.uniroma2.pmcsn.configs;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;

/**
 * Command-line arguments parser for the Horizontal Scaler Simulator.
 * Decouples CLI parsing logic from the main application entry points.
 */
public class CommandLineConfigParser {

    /**
     * Parses command-line arguments and returns an ApplicationConfig instance.
     * Ends program execution if help is requested or configuration parsing fails.
     */
    public static ApplicationConfig parse(String[] args) {
        double maxTime = ApplicationConfig.MAX_TIME;
        long seed = ApplicationConfig.SEED;
        double interarrival = ApplicationConfig.MEAN_INTERARRIVAL;
        double service = ApplicationConfig.MEAN_SERVICE;
        int siMax = ApplicationConfig.SI_MAX;
        int webServers = ApplicationConfig.WEB_SERVER_COUNT;
        int webCap = ApplicationConfig.WEB_SERVER_CAPACITY;
        int spikeCap = ApplicationConfig.SPIKE_SERVER_CAPACITY;
        RoutingPolicy policy = ApplicationConfig.ROUTING_POLICY;
        String tracePath = ApplicationConfig.TRACE_PATH;
        double spikeCpu = ApplicationConfig.SPIKE_CPU_PERCENTAGE;
        WorkloadType workload = ApplicationConfig.WORKLOAD_TYPE;
        double scaleUpLimit = ApplicationConfig.SCALE_UP_LIMIT;
        double scaleDownLimit = ApplicationConfig.SCALE_DOWN_LIMIT;
        double scaleInterval = ApplicationConfig.SCALE_INTERVAL;
        double cooldown = ApplicationConfig.COOLDOWN;
        int minServers = ApplicationConfig.MIN_SERVERS;
        int maxServers = ApplicationConfig.MAX_SERVERS;
        double spikeUpperThreshold = ApplicationConfig.SPIKE_UPPER_THRESHOLD;
        double spikeLowerThreshold = ApplicationConfig.SPIKE_LOWER_THRESHOLD;
        SimulationMethod method = ApplicationConfig.SIMULATION_METHOD;
        int replications = ApplicationConfig.NUM_REPLICATIONS;
        int batches = ApplicationConfig.NUM_BATCHES;
        int batchSize = ApplicationConfig.BATCH_SIZE;
        int warmUp = ApplicationConfig.WARM_UP_JOBS;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-time":
                        maxTime = Double.parseDouble(args[++i]);
                        break;
                    case "-seed":
                        seed = Long.parseLong(args[++i]);
                        break;
                    case "-interarrival":
                        interarrival = Double.parseDouble(args[++i]);
                        break;
                    case "-service":
                        service = Double.parseDouble(args[++i]);
                        break;
                    case "-simax":
                        siMax = Integer.parseInt(args[++i]);
                        break;
                    case "-webservers":
                        webServers = Integer.parseInt(args[++i]);
                        break;
                    case "-webcap":
                        webCap = Integer.parseInt(args[++i]);
                        break;
                    case "-spikecap":
                        spikeCap = Integer.parseInt(args[++i]);
                        break;
                    case "-policy":
                        policy = RoutingPolicy.valueOf(args[++i].toUpperCase());
                        break;
                    case "-trace":
                        tracePath = args[++i];
                        workload = WorkloadType.TRACE;
                        break;
                    case "-share":
                        spikeCpu = Double.parseDouble(args[++i]);
                        break;
                    case "-workload":
                        workload = WorkloadType.valueOf(args[++i].toUpperCase());
                        break;
                    case "-scaleUpLimit":
                        scaleUpLimit = Double.parseDouble(args[++i]);
                        break;
                    case "-scaleDownLimit":
                        scaleDownLimit = Double.parseDouble(args[++i]);
                        break;
                    case "-scaleInterval":
                        scaleInterval = Double.parseDouble(args[++i]);
                        break;
                    case "-cooldown":
                        cooldown = Double.parseDouble(args[++i]);
                        break;
                    case "-minServers":
                        minServers = Integer.parseInt(args[++i]);
                        break;
                    case "-maxServers":
                        maxServers = Integer.parseInt(args[++i]);
                        break;
                    case "-spikeUpperThreshold":
                        spikeUpperThreshold = Double.parseDouble(args[++i]);
                        break;
                    case "-spikeLowerThreshold":
                        spikeLowerThreshold = Double.parseDouble(args[++i]);
                        break;
                    case "-method":
                        method = SimulationMethod.valueOf(args[++i].toUpperCase());
                        break;
                    case "-replications":
                        replications = Integer.parseInt(args[++i]);
                        break;
                    case "-batches":
                        batches = Integer.parseInt(args[++i]);
                        break;
                    case "-batchSize":
                        batchSize = Integer.parseInt(args[++i]);
                        break;
                    case "-warmUp":
                        warmUp = Integer.parseInt(args[++i]);
                        break;
                    case "-h":
                    case "--help":
                        printUsage();
                        System.exit(0);
                        break;
                    default:
                        System.err.println("Unknown parameter: " + args[i]);
                        printUsage();
                        System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printUsage();
            System.exit(1);
        }

        if (minServers == -1) {
            minServers = webServers;
        }

        return new ApplicationConfig(
            maxTime, seed, interarrival, service, siMax,
            webServers, webCap, spikeCap, policy, tracePath,
            spikeCpu, workload, scaleUpLimit, scaleDownLimit,
            scaleInterval, cooldown, minServers, maxServers,
            spikeUpperThreshold, spikeLowerThreshold,
            method, replications, batches, batchSize, warmUp
        );
    }

    /**
     * Prints the help instructions to standard output.
     */
    public static void printUsage() {
        System.out.println("Usage: java it.pmcsn.HorizontalScalerSimulator [options]");
        System.out.println("Options:");
        System.out.println("  -time <double>                    Max simulation time (default 10000.0)");
        System.out.println("  -seed <long>                      RNG seed (default 123456789)");
        System.out.println("  -interarrival <double>            Mean interarrival time (default 2.0)");
        System.out.println("  -service <double>                 Mean service time (default 1.5)");
        System.out.println("  -simax <int>                      SI_max threshold for spike server redirection (default 5)");
        System.out.println("  -webservers <int>                 Number of active Web Servers (default 3)");
        System.out.println("  -webcap <int>                     Capacity of each Web Server (default 1)");
        System.out.println("  -spikecap <int>                   Capacity of the Spike Server (default 10)");
        System.out.println("  -policy <ROUND_ROBIN|LEAST_LOADED> Load routing policy (default ROUND_ROBIN)");
        System.out.println("  -trace <path>                     Path to trace file (absolute or relative to workspace)");
        System.out.println("  -share <double>                   Spike Server CPU capacity share (e.g. 0.4 or 0.8, default 0.4)");
        System.out.println("  -workload <DISTRIBUTION|HYPEREXPONENTIAL|TRACE> Workload generation type (default DISTRIBUTION)");
        System.out.println("  -scaleUpLimit <double>            System response time threshold to scale up (default 8.0)");
        System.out.println("  -scaleDownLimit <double>          System response time threshold to scale down (default 2.0)");
        System.out.println("  -scaleInterval <double>           Moving window size for horizontal scaling (default 30.0)");
        System.out.println("  -cooldown <double>                Minimum time between scaling actions (default 30.0)");
        System.out.println("  -minServers <int>                 Minimum number of Web Servers (default: webservers value)");
        System.out.println("  -maxServers <int>                 Maximum number of Web Servers (default 10)");
        System.out.println("  -spikeUpperThreshold <double>     Spike Server utilization threshold to scale up CPU (default 0.70)");
        System.out.println("  -spikeLowerThreshold <double>     Spike Server utilization threshold to scale down CPU (default 0.30)");
        System.out.println("  -method <INDEPENDENT_REPLICATIONS|BATCH_MEANS> Simulation execution method (default INDEPENDENT_REPLICATIONS)");
        System.out.println("  -replications <int>               Number of independent replications for INDEPENDENT_REPLICATIONS method (default 1)");
        System.out.println("  -batches <int>                    Number of batches for BATCH_MEANS method (default 64)");
        System.out.println("  -batchSize <int>                  Jobs per batch for BATCH_MEANS method (default 1024)");
        System.out.println("  -warmUp <int>                     Jobs for warm-up period in BATCH_MEANS method (default 1000)");
    }
}
