package it.uniroma2.pmcsn.facade;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationFacade {
    private static final Logger logger = LoggerFactory.getLogger(SimulationFacade.class);
    private final ApplicationConfig config;

    public SimulationFacade() {
        this(new ApplicationConfig());
    }

    public SimulationFacade(ApplicationConfig config) {
        this.config = config;
    }

    public void runSimulation() {
        switch (config.simulationMethod()) {
            case SimulationMethod.BATCH_MEANS -> runBatchMeansSimulation();
            case SimulationMethod.INDEPENDENT_REPLICATIONS -> runIndependentReplicationsSimulation();
            default -> throw new IllegalStateException("Unexpected simulation method: " + config.simulationMethod());
        }
    }

    public void runBatchMeansSimulation() {
        Welford rtWelford = new Welford();
        Welford jisWelford = new Welford();
        Welford utilWelford = new Welford();
        Welford thrWelford = new Welford();

        SimulationBuilder builder = new SimulationBuilder().config(config).maxTime(Double.MAX_VALUE);
        SimulationController controller = builder.build();

        logger.info("Starting BATCH MEANS simulation (Infinite Horizon)...");
        logger.info("Warm-up period: {} jobs", config.warmUpJobs());

        controller.runUntilWarmUp(config.warmUpJobs());

        logger.info("Warm-up completed. Starting {} batches of {} jobs each...", config.numBatches(), config.batchSize());
        controller.resetStatistics();

        for (int i = 0; i < config.numBatches(); i++) {
            controller.runBatch(config.batchSize());

            rtWelford.update(controller.getAverageResponseTime());
            jisWelford.update(controller.getAverageJobsInSystem());
            utilWelford.update(controller.getSystemUtilization());
            thrWelford.update(controller.getThroughput());

            logger.info("Batch {}/{} completed. Current Response Time Mean: {:.4f}", i + 1, config.numBatches(), rtWelford.getMean());
            controller.resetStatistics();
        }

        printFinalReport("BATCH MEANS (STEADY STATE)", rtWelford, jisWelford, utilWelford, thrWelford, config.numBatches());
    }

    public void runIndependentReplicationsSimulation() {
        Welford rtWelford = new Welford();
        Welford jisWelford = new Welford();
        Welford utilWelford = new Welford();
        Welford thrWelford = new Welford();

        logger.info("Starting {} independent replications...", config.numReplications());

        for (int i = 0; i < config.numReplications(); i++) {
            long currentSeed = config.seed() + i;
            SimulationBuilder builder = new SimulationBuilder().config(config).seed(currentSeed);
            SimulationController controller = builder.build();

            logger.info("Running replication {}/{} with seed {}...", i + 1, config.numReplications(), currentSeed);
            controller.run();

            rtWelford.update(controller.getAverageResponseTime());
            jisWelford.update(controller.getAverageJobsInSystem());
            utilWelford.update(controller.getSystemUtilization());
            thrWelford.update(controller.getThroughput());
        }

        printFinalReport("INDEPENDENT REPLICATIONS (FINITE HORIZON)", rtWelford, jisWelford, utilWelford, thrWelford, config.numReplications());
    }

    private void printFinalReport(String methodLabel, Welford rt, Welford jis, Welford util, Welford thr, int n) {
        logger.info("==================================================================");
        logger.info("                   AGGREGATED SIMULATION REPORT                  ");
        logger.info("METHOD: " + methodLabel);
        logger.info("SAMPLES (N): " + n);
        logger.info("==================================================================");

        printMetric("Response Time", rt);
        printMetric("Jobs in System", jis);
        printMetric("System Utilization", util);
        printMetric("Throughput", thr);

        logger.info("==================================================================");
    }

    private void printMetric(String name, Welford w) {
        double mean = w.getMean();
        double stdDev = w.getStandardDeviation();
        // 95% Confidence Interval using t-distribution approximation (z-score 1.96 for large N)
        double confidenceLevel = 1.96;
        double error = confidenceLevel * (stdDev / Math.sqrt(Math.max(1, w.getCount())));

        logger.info(String.format("%-20s: Mean = %.4f, StdDev = %.4f, 95%% CI = [%.4f, %.4f]",
                name, mean, stdDev, mean - error, mean + error));
    }

}
