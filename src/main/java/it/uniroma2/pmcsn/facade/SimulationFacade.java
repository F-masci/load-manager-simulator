package it.uniroma2.pmcsn.facade;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
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

        rt.reset();
        jis.reset();
        util.reset();
        thr.reset();
    }

    /**
     * Runs the simulation experiment defined in the configuration.
     * @return The aggregated results of the experiment.
     */
    public AggregatedResults runSimulation() {
        AggregatedResults results = switch (config.simulationMethod()) {
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
        
        // Prevent early exit in Infinite horizon simulation
        builder.maxTime(Double.MAX_VALUE);
        SimulationController controller = builder.build();

        logger.info("Starting BATCH MEANS simulation (Infinite Horizon)... Warm-up: {} jobs", config.warmUpJobs());
        controller.run(SimulationController.StopCondition.untilJobsCompleted(config.warmUpJobs()));

        logger.info("Warm-up completed. Running {} batches of {} jobs...", config.numBatches(), config.batchSize());
        controller.resetStatistics();

        for (int i = 0; i < config.numBatches(); i++) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.batchSize()));
            updateAggregators(controller, rt, jis, util, thr);
            logger.info("Batch {}/{} completed", i + 1, config.numBatches());
            logger.debug("Batch {}/{} results:" +
                    "Response Time {} | " +
                    "Jobs in System {} | " +
                    "System Utilization {} | " +
                    "Throughput {}",
                    i + 1, config.numBatches(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            controller.resetStatistics();
        }

        return createResults("BATCH MEANS (STEADY STATE)", config.numBatches(), rt, jis, util, thr);
    }

    /**
     * Runs a terminating simulation using the Independent Replications method.
     *
     * @return Aggregated results over all replications.
     */
    public AggregatedResults runIndependentReplicationsSimulation() {

        logger.info("Starting {} independent replications...", config.numReplications());

        // Setting starting seed
        long currentSeed = config.seed();

        for (int i = 0; i < config.numReplications(); i++) {
            // Update seed for new run
            SimulationBuilder builder = new SimulationBuilder().config(config).seed(currentSeed);
            SimulationController controller = builder.build();

            logger.info("Running replication {}/{} with seed {}...", i + 1, config.numReplications(), currentSeed);
            controller.run(SimulationController.StopCondition.untilQueueEmpty());
            logger.debug("Running replication {}/{} completed", i + 1, config.numReplications());
            logger.debug("Batch {}/{} results:" +
                            "Response Time {} | " +
                            "Jobs in System {} | " +
                            "System Utilization {} | " +
                            "Throughput {}",
                    i + 1, config.numBatches(),
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            updateAggregators(controller, rt, jis, util, thr);

            // Get seed from old run
            currentSeed = controller.getSeed();
        }

        return createResults("INDEPENDENT REPLICATIONS (FINITE HORIZON)", config.numReplications(), rt, jis, util, thr);
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
        logger.info("Simulation results:\n{}", results.toString());
        this.lastResults = results;
        return results;
    }

    /**
     * Encapsulates the aggregated statistical results of a simulation experiment.
     */
    public record AggregatedResults(
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
