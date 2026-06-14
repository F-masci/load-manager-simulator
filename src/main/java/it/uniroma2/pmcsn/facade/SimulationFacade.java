package it.uniroma2.pmcsn.facade;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            updateAggregators(controller, rt, jis, util, thr);
            logger.info("Batch {}/{} completed", i + 1, config.execution().numBatches());
            logger.debug("Batch {}/{} results:" +
                    "Response Time {} | " +
                    "Jobs in System {} | " +
                    "System Utilization {} | " +
                    "Throughput {}",
                    i + 1, config.execution().numBatches(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            controller.resetStatistics();
        }

        return createResults("BATCH MEANS (STEADY STATE)", config.execution().numBatches(), rt, jis, util, thr);
    }

    /**
     * Runs a terminating simulation using the Independent Replications method.
     * @return Aggregated results over all replications.
     */
    public AggregatedResults runIndependentReplicationsSimulation() {
        return runIndependentReplicationsSimulation(new SimulationBuilder().config(config));
    }

    /**
     * Runs a terminating simulation using the Independent Replications method with a custom builder.
     * @param builder The configured builder.
     * @return Aggregated results over all replications.
     */
    public AggregatedResults runIndependentReplicationsSimulation(SimulationBuilder builder) {

        logger.info("Starting {} independent replications...", config.execution().numReplications());

        // Setting starting seed
        long currentSeed = config.execution().seed();

        for (int i = 0; i < config.execution().numReplications(); i++) {

            // Ensure independence by creating a fresh controller with a specific seed for each replication
            SimulationController controller = new SimulationBuilder()
                .config(config.withSeed(currentSeed))
                .build();

            logger.info("Running replication {}/{} with seed {}...", i + 1, config.execution().numReplications(), currentSeed);
            controller.run(SimulationController.StopCondition.untilTimeElapsed(config.execution().maxTime()));
            logger.debug("Running replication {}/{} completed", i + 1, config.execution().numReplications());
            logger.debug("Batch {}/{} results:" +
                            "Response Time {} | " +
                            "Jobs in System {} | " +
                            "System Utilization {} | " +
                            "Throughput {}",
                    i + 1, config.execution().numReplications(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            updateAggregators(controller, rt, jis, util, thr);

            // Get seed from old run
            currentSeed = controller.getSeed();
        }

        return createResults("INDEPENDENT REPLICATIONS (FINITE HORIZON)", config.execution().numReplications(), rt, jis, util, thr);
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
    private void updateAggregators(SimulationController c, Welford rt, Welford jis, Welford util, Welford thr) {
        double duration = c.getClockSinceReset();
        rt.update(c.getAverageResponseTime());
        jis.update(c.getAverageJobsInSystem());
        util.update(c.getSystemUtilization());
        thr.update(c.getThroughput());
    }

    /**
     * Creates an AggregatedResults object and logs the final report.
     */
    private AggregatedResults createResults(String label, int n, Welford rt, Welford jis, Welford util, Welford thr) {
        AggregatedResults results = new AggregatedResults(
            label, n,
            IntervalEstimator.estimate(rt.getCount(), rt.getMean(), rt.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(jis.getCount(), jis.getMean(), jis.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(util.getCount(), util.getMean(), util.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(thr.getCount(), thr.getMean(), thr.getStandardDeviation(), 0.95)
        );
        logger.info("\n" + results.toString());
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
        IntervalEstimator.IntervalResult throughput
    ) {
        @Override
        public String toString() {
            return String.format(
                "==================================================================\n" +
                "                   AGGREGATED SIMULATION REPORT                  \n" +
                "METHOD: %s\n" +
                "SAMPLES (N): %d\n" +
                "==================================================================\n" +
                "%-20s: %s\n" +
                "%-20s: %s\n" +
                "%-20s: %s\n" +
                "%-20s: %s\n" +
                "==================================================================",
                methodLabel, sampleCount,
                "Response Time", responseTime,
                "Jobs in System", jobsInSystem,
                "System Utilization", utilization,
                "Throughput", throughput
            );
        }
    }
}
