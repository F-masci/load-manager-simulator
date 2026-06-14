package it.uniroma2.pmcsn.facade;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.model.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Facade for the simulation system. 
 * Provides high-level methods to run different types of simulation experiments 
 * and extract aggregated performance metrics.
 */
public class SimulationFacade {
    private static final Logger logger = LoggerFactory.getLogger(SimulationFacade.class);
    
    private final ApplicationConfig config;
    private AggregatedResults lastResults;

    private final Welford rt = new Welford();
    private final Welford jis = new Welford();
    private final Welford util = new Welford();
    private final Welford thr = new Welford();
    
    private final Welford diverted = new Welford();
    private final Welford scaleOuts = new Welford();
    private final Welford scaleIns = new Welford();
    private final Welford scaleUps = new Welford();
    private final Welford scaleDowns = new Welford();
    private final Welford spikeSpeedMult = new Welford();
    private final Map<Integer, Welford> serverCompletions = new HashMap<>();

    /**
     * Creates a facade with default configuration.
     */
    public SimulationFacade() {
        this(new ApplicationConfig());
    }

    /**
     * Creates a facade with the specified configuration.
     * @param config The application configuration.
     */
    public SimulationFacade(ApplicationConfig config) {
        this.config = config;
    }

    /**
     * Runs the simulation experiment defined in the configuration.
     * @return The aggregated results of the experiment.
     */
    public AggregatedResults runSimulation() {
        AggregatedResults results = switch (config.execution().method()) {
            case BATCH_MEANS -> runBatchMeansSimulation();
            case INDEPENDENT_REPLICATIONS -> runIndependentReplicationsSimulation();
        };
        this.lastResults = results;
        return results;
    }

    /**
     * Runs a steady-state simulation using the Batch Means method.
     * @return Aggregated results over all batches.
     */
    public AggregatedResults runBatchMeansSimulation() {
        return runBatchMeansSimulation(new SimulationBuilder().config(config));
    }

    /**
     * Runs a steady-state simulation using the Batch Means method with a custom builder.
     * @param builder The configured builder.
     * @return Aggregated results over all batches.
     */
    public AggregatedResults runBatchMeansSimulation(SimulationBuilder builder) {
        
        // Infinite horizon simulation
        SimulationController controller = builder.build();

        logger.info("Starting BATCH MEANS simulation (Infinite Horizon)... Warm-up: {} jobs", config.execution().warmUpJobs());
        if (config.execution().warmUpJobs() > 0) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().warmUpJobs()));
        }

        logger.info("Warm-up completed. Running {} batches of {} jobs...", config.execution().numBatches(), config.execution().batchSize());
        controller.resetStatistics();

        for (int i = 0; i < config.execution().numBatches(); i++) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().batchSize()));
            updateAggregators(controller);
            logger.info("Batch {}/{} completed", i + 1, config.execution().numBatches());
            logger.debug("Batch {}/{} results: " +
                    "Response Time {} | " +
                    "Jobs in System {} | " +
                    "System Utilization {} | " +
                    "Throughput {}",
                    i + 1, config.execution().numBatches(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            controller.resetStatistics();
        }

        return createResults("BATCH MEANS (STEADY STATE)", config.execution().numBatches());
    }

    /**
     * Runs a terminating simulation using the Independent Replications method with a custom builder.
     * @return Aggregated results over all replications.
     */
    public AggregatedResults runIndependentReplicationsSimulation() {

        logger.info("Starting {} independent replications...", config.execution().numReplications());

        // Setting starting seed
        long currentSeed = config.execution().seed();

        for (int i = 0; i < config.execution().numReplications(); i++) {

            // Ensure independence by creating a fresh controller with a specific seed for each replication
            SimulationController controller = new SimulationBuilder()
                .config(config.withSeed(currentSeed))
                .build();

            logger.info("Running replication {}/{} with seed {}...", i + 1, config.execution().numReplications(), currentSeed);

            if (config.execution().maxJobs() > 0) {
                controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().maxJobs()));
            } else {
                controller.run(SimulationController.StopCondition.untilTimeElapsed(config.execution().maxTime()));
            }

            logger.debug("Running replication {}/{} completed", i + 1, config.execution().numReplications());
            logger.debug("Replication {}/{} results:" +
                            "Response Time {} | " +
                            "Jobs in System {} | " +
                            "System Utilization {} | " +
                            "Throughput {}",
                    i + 1, config.execution().numReplications(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            updateAggregators(controller);

            // Get seed from old run
            currentSeed = controller.getSeed();
        }

        return createResults("INDEPENDENT REPLICATIONS (FINITE HORIZON)", config.execution().numReplications());
    }

    /**
     * Runs a single simulation and returns the aggregated results.
     * @return The aggregated results of the run.
     */
    public AggregatedResults runSingleSimulation() {
        if (config.execution().numReplications() != 1) {
            throw new IllegalStateException("Configuration must be set to 1 replication for a single simulation run.");
        }

        SimulationController controller = new SimulationBuilder().config(config).build();
        if (config.execution().maxJobs() > 0) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().maxJobs()));
        } else {
            controller.run(SimulationController.StopCondition.untilTimeElapsed(config.execution().maxTime()));
        }
        updateAggregators(controller);
        AggregatedResults results = createResults("SINGLE RUN", 1);
        this.lastResults = results;
        return results;
    }


    /**
     * Returns the results of the last simulation run.
     * @return Aggregated results, or null if no simulation has been run.
     */
    public AggregatedResults getLastResults() {
        return lastResults;
    }

    /**
     * Updates statistical aggregators with metrics from a completed simulation segment.
     */
    private void updateAggregators(SimulationController c) {
        rt.update(c.getAverageResponseTime());
        jis.update(c.getAverageJobsInSystem());
        util.update(c.getSystemUtilization());
        thr.update(c.getThroughput());
        
        diverted.update(c.getTotalJobsDiverted());
        
        // Horizontal Scaling
        scaleOuts.update(c.getWebServerCluster().getScaleUpCount());
        scaleIns.update(c.getWebServerCluster().getScaleDownCount());

        // Vertical Scaling
        scaleUps.update(c.getLoadManager().getVerticalScaler().getScaleUpCount());
        scaleDowns.update(c.getLoadManager().getVerticalScaler().getScaleDownCount());
        
        spikeSpeedMult.update(c.getSpikeServer().getAverageSpeedMultiplier(c.getClock()));

        for (WebServer ws : c.getWebServerCluster().getAllServers()) {
            serverCompletions.computeIfAbsent(ws.getId(), k -> new Welford()).update(ws.getCompletedJobsCount());
        }
    }

    /**
     * Creates an AggregatedResults object and logs the final report.
     */
    private AggregatedResults createResults(String label, int n) {
        Map<Integer, IntervalEstimator.IntervalResult> serverStats = new HashMap<>();
        serverCompletions.forEach((id, w) -> serverStats.put(id, IntervalEstimator.estimate(w.getCount(), w.getMean(), w.getStandardDeviation(), 0.95)));

        AggregatedResults results = new AggregatedResults(
            label, n,
            IntervalEstimator.estimate(rt.getCount(), rt.getMean(), rt.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(jis.getCount(), jis.getMean(), jis.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(util.getCount(), util.getMean(), util.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(thr.getCount(), thr.getMean(), thr.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(diverted.getCount(), diverted.getMean(), diverted.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleOuts.getCount(), scaleOuts.getMean(), scaleOuts.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleIns.getCount(), scaleIns.getMean(), scaleIns.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleUps.getCount(), scaleUps.getMean(), scaleUps.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleDowns.getCount(), scaleDowns.getMean(), scaleDowns.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(spikeSpeedMult.getCount(), spikeSpeedMult.getMean(), spikeSpeedMult.getStandardDeviation(), 0.95),
            serverStats
        );
        logger.info("\n {}", results.toString());
        return results;
    }

    /**
     * Encapsulates the aggregated statistical results of a simulation experiment.
     */
    public static record AggregatedResults(
        String methodLabel,
        int sampleCount,
        IntervalEstimator.IntervalResult responseTime,
        IntervalEstimator.IntervalResult jobsInSystem,
        IntervalEstimator.IntervalResult utilization,
        IntervalEstimator.IntervalResult throughput,
        IntervalEstimator.IntervalResult divertedJobs,
        IntervalEstimator.IntervalResult scaleOutActions,
        IntervalEstimator.IntervalResult scaleInActions,
        IntervalEstimator.IntervalResult scaleUpActions,
        IntervalEstimator.IntervalResult scaleDownActions,
        IntervalEstimator.IntervalResult spikeAvgSpeed,
        Map<Integer, IntervalEstimator.IntervalResult> serverCompletions
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("==================================================================\n");
            sb.append("                   AGGREGATED SIMULATION REPORT                  \n");
            sb.append(String.format("METHOD: %s\n", methodLabel));
            sb.append(String.format("SAMPLES (N): %d\n", sampleCount));
            sb.append("==================================================================\n");
            sb.append(String.format("%-25s: %s\n", "Response Time", responseTime));
            sb.append(String.format("%-25s: %s\n", "Jobs in System", jobsInSystem));
            sb.append(String.format("%-25s: %s\n", "System Utilization", utilization));
            sb.append(String.format("%-25s: %s\n", "Throughput", throughput));
            sb.append(String.format("%-25s: %s\n", "Diverted Jobs", divertedJobs));
            sb.append("------------------------------------------------------------------\n");
            sb.append(String.format("%-25s: %s\n", "Scale OUT Actions", scaleOutActions));
            sb.append(String.format("%-25s: %s\n", "Scale IN  Actions", scaleInActions));
            sb.append(String.format("%-25s: %s\n", "Scale UP  Actions", scaleUpActions));
            sb.append(String.format("%-25s: %s\n", "Scale DOWN Actions", scaleDownActions));
            sb.append(String.format("%-25s: %s\n", "Spike Server Avg Speed", spikeAvgSpeed));
            sb.append("------------------------------------------------------------------\n");
            sb.append("SERVER COMPLETIONS:\n");
            serverCompletions.forEach((id, res) -> 
                sb.append(String.format("  Server #%-12d: %s\n", id, res))
            );
            sb.append("==================================================================");
            return sb.toString();
        }
    }
}
