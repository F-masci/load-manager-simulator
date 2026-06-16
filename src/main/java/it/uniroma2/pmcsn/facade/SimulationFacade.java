package it.uniroma2.pmcsn.facade;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.storage.CsvStorageDecorator;
import it.uniroma2.pmcsn.controller.decorator.storage.JsonStorageDecorator;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.utils.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static it.uniroma2.pmcsn.utils.SimulationConsoleUtils.printBatchProgressBar;

/**
 * Facade for the simulation system. 
 * Provides high-level methods to run different types of simulation experiments 
 * and extract aggregated performance metrics.
 */
public class SimulationFacade {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(SimulationFacade.class, "SIM");
    
    private final ApplicationConfig config;

    private final Welford rt = new Welford();
    private final Welford jis = new Welford();
    private final Welford util = new Welford();
    private final Welford thr = new Welford();
    
    private final Welford diverted = new Welford();
    private final Welford avgServers = new Welford();
    private final Welford scaleOuts = new Welford();
    private final Welford scaleIns = new Welford();
    private final Welford scaleUps = new Welford();
    private final Welford scaleDowns = new Welford();
    private final Welford spikeSpeedMult = new Welford();
    private final Welford spikeUtilization = new Welford();
    private final Map<Integer, Welford> serverCompletions = new HashMap<>();

    private Class<? extends SimulatorDecorator> customDecoratorClass = null;
    private final java.util.List<SimulatorDecorator> customDecorators = new java.util.ArrayList<>();

    /**
     * Sets a custom decorator class to be instantiated and wrapped around the controller 
     * during simulations.
     */
    public void setCustomDecorator(Class<? extends SimulatorDecorator> decoratorClass) {
        this.customDecoratorClass = decoratorClass;
    }

    /**
     * Retrieves the list of custom decorators instantiated during the runs 
     * (e.g., one per replication).
     */
    public java.util.List<SimulatorDecorator> getCustomDecorators() {
        return customDecorators;
    }

    private Simulator applyCustomDecorator(Simulator baseController) {
        if (customDecoratorClass != null) {
            try {
                SimulatorDecorator decorator = customDecoratorClass.getConstructor(Simulator.class).newInstance(baseController);
                customDecorators.add(decorator);
                return decorator;
            } catch (Exception e) {
                logger.error("Failed to instantiate custom decorator: {}", customDecoratorClass.getSimpleName(), e);
            }
        }
        return baseController;
    }

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
        Simulator controller = builder.build();

        logger.info("Starting BATCH MEANS simulation (Infinite Horizon)... Warm-up: {} jobs", config.execution().warmUpJobs());
        if (config.execution().warmUpJobs() > 0) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().warmUpJobs()));
        }

        logger.info("Warm-up completed. Running {} batches of {} jobs...", config.execution().numBatches(), config.execution().batchSize());
        controller.resetStatistics();

        for (int i = 0; i < config.execution().numBatches(); i++) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().batchSize()));
            updateAggregators(controller);
            
            int currentBatch = i + 1;
            int totalBatches = config.execution().numBatches();

            printBatchProgressBar(currentBatch, totalBatches);
            
            logger.debug("Batch {}/{} results: " +
                    "Response Time {} | " +
                    "Jobs in System {} | " +
                    "System Utilization {} | " +
                    "Throughput {}",
                    currentBatch, totalBatches,
                    controller.getAverageResponseTime(), controller.getAverageJobsInSystem(),
                    controller.getSystemUtilization(), controller.getThroughput());
            controller.resetStatistics();
        }
        System.out.println(); // New line after progress bar

        return createResults("BATCH MEANS (STEADY STATE)", config.execution().numBatches());
    }

    private final java.util.List<Double> runningMeans = new java.util.ArrayList<>();

    /**
     * Runs a terminating simulation using the Independent Replications method with a custom builder.
     * @return Aggregated results over all replications.
     */
    public AggregatedResults runIndependentReplicationsSimulation() {

        logger.info("Starting {} independent replications...", config.execution().numReplications());

        // Setting starting seed
        long currentSeed = config.execution().seed();
        runningMeans.clear();
        customDecorators.clear(); // Ensure clean state for new runs

        for (int i = 0; i < config.execution().numReplications(); i++) {

            // Ensure independence by creating a fresh controller with a specific seed for each replication
            Simulator controller = new SimulationBuilder()
                .config(config.withSeed(currentSeed))
                .build();

            controller = applyCustomDecorator(controller);

            logger.info("Running replication {}/{} with seed {}...", i + 1, config.execution().numReplications(), currentSeed);

            if (config.execution().maxJobs() > 0) {
                controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().maxJobs()));
            } else {
                controller.run(SimulationController.StopCondition.untilTimeElapsed(config.execution().maxTime()), true);
            }

            logger.debug("Running replication {}/{} completed", i + 1, config.execution().numReplications());
            updateAggregators(controller);
            runningMeans.add(rt.getMean());

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

        Simulator controller = new SimulationBuilder().config(config).build();
        if (config.execution().maxJobs() > 0) {
            controller.run(SimulationController.StopCondition.untilJobsCompleted(config.execution().maxJobs()));
        } else {
            controller.run(SimulationController.StopCondition.untilTimeElapsed(config.execution().maxTime()));
        }
        updateAggregators(controller);
        AggregatedResults results = createResults("SINGLE RUN", 1);

        // Finalize decorators
        // Since decorators are wrapping the controller, we should find them
        findAndCloseStorage(controller);
        
        return results;
    }

    private void findAndCloseStorage(Simulator s) {
        if (s instanceof CsvStorageDecorator d) d.finalizeSimulation();
        else if (s instanceof JsonStorageDecorator d) d.finalizeSimulation();
        else if (s instanceof SimulatorDecorator sd) findAndCloseStorage(sd.getDecorated());
    }

    /**
     * Updates statistical aggregators with metrics from a completed simulation segment.
     */
    private void updateAggregators(Simulator c) {
        rt.update(c.getAverageResponseTime());
        jis.update(c.getAverageJobsInSystem());
        util.update(c.getSystemUtilization());
        thr.update(c.getThroughput());
        
        diverted.update(c.getTotalJobsDiverted());
        avgServers.update(c.getWebServerCluster().getAverageActiveServers(c.getClock()));
        
        // Horizontal Scaling
        scaleOuts.update(c.getWebServerCluster().getScaleOutCount());
        scaleIns.update(c.getWebServerCluster().getScaleInCount());

        // Vertical Scaling
        scaleUps.update(c.getLoadManager().getVerticalScaler().getScaleUpCount());
        scaleDowns.update(c.getLoadManager().getVerticalScaler().getScaleDownCount());
        
        spikeSpeedMult.update(c.getSpikeServer().getAverageSpeedMultiplier(c.getClock()));
        spikeUtilization.update(c.getSpikeServer().getAverageUtilization(c.getClock()));

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
            IntervalEstimator.estimate(avgServers.getCount(), avgServers.getMean(), avgServers.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleOuts.getCount(), scaleOuts.getMean(), scaleOuts.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleIns.getCount(), scaleIns.getMean(), scaleIns.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleUps.getCount(), scaleUps.getMean(), scaleUps.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(scaleDowns.getCount(), scaleDowns.getMean(), scaleDowns.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(spikeSpeedMult.getCount(), spikeSpeedMult.getMean(), spikeSpeedMult.getStandardDeviation(), 0.95),
            IntervalEstimator.estimate(spikeUtilization.getCount(), spikeUtilization.getMean(), spikeUtilization.getStandardDeviation(), 0.95),
            serverStats
        );
        logger.info("\n {}", results.toString());
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
        IntervalEstimator.IntervalResult throughput,
        IntervalEstimator.IntervalResult divertedJobs,
        IntervalEstimator.IntervalResult avgServers,
        IntervalEstimator.IntervalResult scaleOutActions,
        IntervalEstimator.IntervalResult scaleInActions,
        IntervalEstimator.IntervalResult scaleUpActions,
        IntervalEstimator.IntervalResult scaleDownActions,
        IntervalEstimator.IntervalResult spikeAvgSpeed,
        IntervalEstimator.IntervalResult spikeUtilization,
        Map<Integer, IntervalEstimator.IntervalResult> serverCompletions
    ) {
        @Override
        @NotNull
        public String toString() {
            return "==================================================================\n" +
                    "                   AGGREGATED SIMULATION REPORT                  \n" +
                    String.format("METHOD: %s\n", methodLabel) +
                    String.format("SAMPLES (N): %d\n", sampleCount) +
                    "==================================================================\n" +
                    String.format("%-25s: %s\n", "Response Time", responseTime) +
                    String.format("%-25s: %s\n", "Jobs in System", jobsInSystem) +
                    String.format("%-25s: %s\n", "System Utilization", utilization) +
                    String.format("%-25s: %s\n", "Throughput", throughput) +
                    String.format("%-25s: %s\n", "Diverted Jobs", divertedJobs) +
                    String.format("%-25s: %s\n", "Avg Active Servers (N)", avgServers) +
                    "------------------------------------------------------------------\n" +
                    String.format("%-25s: %s\n", "Scale OUT Actions", scaleOutActions) +
                    String.format("%-25s: %s\n", "Scale IN  Actions", scaleInActions) +
                    String.format("%-25s: %s\n", "Scale UP  Actions", scaleUpActions) +
                    String.format("%-25s: %s\n", "Scale DOWN Actions", scaleDownActions) +
                    String.format("%-25s: %s\n", "Spike Server Avg Speed", spikeAvgSpeed) +
                    String.format("%-25s: %s\n", "Spike Server Utilization", spikeUtilization) +
                    "==================================================================";
        }
    }
}
