package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.data.TimeSerieCollector;
import it.uniroma2.pmcsn.lib.statistics.AutoCorrelation;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Executive main class for steady-state output analysis using the Method of Batch Means.
 * Executes two independent and separate estimation procedures to evaluate and compare
 * analytical vs dynamic confidence intervals.
 */
public class BatchMeanEstimator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(BatchMeanEstimator.class, "SIM");

    // Statistical threshold configurations
    private static final double TARGET_PRECISION = 0.05;    // Relative precision epsilon
    private static final double CONFIDENCE_LEVEL = 1 - TARGET_PRECISION;
    private static final int SIZE_MULTIPLEIR = 10;          // Multiplier for batch size scaling from cutoff lag
    private static final int INITIAL_BATCH_SIZE = 32;       // Starting batch size for the dynamic method
    private static final int RE_EVALUATION_BLOCK = 10_000;  // Step size for extending the dynamic simulation
    private static final int MIN_BATCHES_THRESHOLD = 32;    // Recommended minimum k to avoid small-sample variation

    /**
     * Standardized data structure encapsulating output statistics.
     *
     * @param intervalResult   The calculated confidence interval.
     * @param totalObservations Total number of samples collected.
     * @param batchSize        The size of each batch (b).
     * @param numBatches       The number of batches (k).
     * @param finalLag1ACF     The Lag-1 autocorrelation of batch means.
     */
    public record EstimationReport(

            IntervalEstimator.IntervalResult intervalResult,
            int totalObservations,
            int batchSize,
            int numBatches,
            double finalLag1ACF
    ) {
        @Override
        @NotNull
        public String toString() {
            return String.format("%-25s | N = %d, b = %d, k = %d | Lag-1 ACF = %.4f",
                    intervalResult,
                    totalObservations,
                    batchSize,
                    numBatches,
                    finalLag1ACF);
        }
    }

    public final static ApplicationConfig[] APPLICATION_CONFIGS = new ApplicationConfig[]{
            // 0: Default
            new ApplicationConfig(),
            // 1: Simple System
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(WorkloadType.HYPEREXPONENTIAL, 0.40, 0.25, RoutingPolicy.DETERMINISTIC, 10),
                    ApplicationConfig.ClusterConfig.fixedServer(1, true),
                    ApplicationConfig.ScalingConfig.disabled(),
                    new ApplicationConfig.ExecutionConfig()
            ),
            // 2: Routing policy
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(
                            ApplicationConfig.WORKLOAD_TYPE,
                            ApplicationConfig.MEAN_INTERARRIVAL, ApplicationConfig.CV_SERVICE,
                            ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                            RoutingPolicy.ROUND_ROBIN
                    ),
                ApplicationConfig.ClusterConfig.fixedServer(4, false),
                ApplicationConfig.ScalingConfig.disabled(),
                new ApplicationConfig.ExecutionConfig()
            ),
            // 3: SI_max estimator
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(
                            ApplicationConfig.MEAN_INTERARRIVAL, ApplicationConfig.CV_SERVICE,
                            ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                            75, ApplicationConfig.SI_LOW,
                            ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, null
                    ),
                    ApplicationConfig.ClusterConfig.fixedServer(1, true),
                    ApplicationConfig.ScalingConfig.disabled(),
                    new ApplicationConfig.ExecutionConfig()
            ),
            // 4: Vertical Step Sizing
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(),
                    ApplicationConfig.ClusterConfig.fixedServer(1, true),
                    ApplicationConfig.ScalingConfig.onlyVertical(
                            ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                            ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                            ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                            1.0,
                            ApplicationConfig.COOLDOWN
                    ),
                    new ApplicationConfig.ExecutionConfig()
            ),
            // 5: Horizontal Scaling
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(),
                    ApplicationConfig.ClusterConfig.fixedServer(5, true),
                    ApplicationConfig.ScalingConfig.disabled(),
                    new ApplicationConfig.ExecutionConfig()
            ),
            // 6: Cost analysis
            new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(),
                    new ApplicationConfig.ClusterConfig(1, 1, 25, true),
                    new ApplicationConfig.ScalingConfig(
                            ApplicationConfig.SCALE_OUT_LIMIT, 3.5,
                            ApplicationConfig.WINDOW_SIZE, 50.0,
                            ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                            ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                            ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                            ApplicationConfig.VERTICAL_INCREMENT,
                            true, true
                    ),
                    new ApplicationConfig.ExecutionConfig()
            )
    };

    public static void main(String[] args) {

        // Target system setup under steady-state conditions
        ApplicationConfig config = APPLICATION_CONFIGS[0];
        run(config);
    }

    public static void run(ApplicationConfig config) {

        ApplicationConfig currentConfig = new ApplicationConfig(
            config.load(),
            config.cluster(),
            config.scaling(),
            config.execution()
            // Remove logging config
        );

        logger.info("Starting Batch Means Estimation Analysis...");

        logger.info("\n--- Phase 1: Analytical Method - ACF ---");
        EstimationReport analyticalReport = performAnalyticalEstimation(currentConfig);

        logger.info("\n--- Phase 2: Dynamic Method - Automated Stopping Rule ---");
        EstimationReport dynamicReport = performDynamicEstimation(currentConfig);

        // Print the completely standardized comparative final report
        printComparativeReport(analyticalReport, dynamicReport);
    }

    /**
     * Analytical Estimation.
     *
     * Runs a fixed-horizon pilot simulation, computes the autocorrelation cutoff,
     * scales the batch size conservatively, and calculates the confidence interval.
     *
     * @param config The system configuration parameters.
     * @return Standardized report containing static batch statistical results.
     */
    private static EstimationReport performAnalyticalEstimation(ApplicationConfig config) {
        int totalJobs = 5_000_000;

        // Instantiate simulator wrapped with an in-memory time-series decorator
        SimulationBuilder builder = new SimulationBuilder().config(config);
        Simulator baseSimulator = builder.build();
        TimeSerieCollector collector = new TimeSerieCollector(baseSimulator);

        logger.info("Running static pilot trace for {} jobs...", totalJobs);
        collector.run(SimulationController.StopCondition.untilJobsCompleted(totalJobs), true);

        List<Double> series = collector.getSeries();
        int n = series.size();

        // Calculate the statistical significance threshold via Chatfield's rule
        // => thr = 2 / sqrt(n)
        final double threshold = 2.0 / Math.sqrt(n);
        int cutoffLag = AutoCorrelation.calculateCutoff(series, threshold);
        logger.info("Cutoff lag detected: {} - Critical limit threshold = {}", cutoffLag, threshold);

        // Apply conservative multiplier to eliminate border/edge correlation effects
        int batchSize = SIZE_MULTIPLEIR * cutoffLag;
        if (batchSize < 1) batchSize = 1;
        int numBatches = n / batchSize;
        logger.info("Forming fixed structure: batch size (b) = {}, number of batches (k) = {}", batchSize, numBatches);

        // Map original time-series items into discrete batches
        List<Double> batchMeans = new ArrayList<>();
        Welford batchMeansWelford = new Welford();

        for (int i = 0; i < numBatches; i++) {
            double batchSum = 0.0;
            for (int j = 0; j < batchSize; j++) {
                batchSum += series.get(i * batchSize + j);
            }
            double bMean = batchSum / batchSize;
            batchMeans.add(bMean);
            batchMeansWelford.update(bMean);
        }

        // Compute structural metrics and ex-post Lag-1 correlation on the batch means array
        IntervalEstimator.IntervalResult intervalResult = IntervalEstimator.estimate(batchMeansWelford, CONFIDENCE_LEVEL);
        double finalLag1ACF = AutoCorrelation.calculateACF(batchMeans, 1);

        return new EstimationReport(intervalResult, n, batchSize, numBatches, finalLag1ACF);
    }

    /**
     * Dynamic Estimation.
     *
     * Advances the simulation iteratively, checks relative precision,
     * evaluates batch-level serial correlation, and dynamically doubles 'b' upon failure.
     *
     * @param config The system configuration parameters.
     * @return Standardized report containing dynamically converged statistical results.
     */
    private static EstimationReport performDynamicEstimation(ApplicationConfig config) {
        SimulationBuilder builder = new SimulationBuilder().config(config);
        Simulator baseSimulator = builder.build();
        TimeSerieCollector collector = new TimeSerieCollector(baseSimulator);

        // Prime the event priority queue without triggering the standard run() loop
        collector.scheduleInitialEvents();

        int b = INITIAL_BATCH_SIZE;
        boolean converged = false;
        EstimationReport finalReport = null;

        while (!converged && collector.processNextEvent()) {
            // Push the virtual clock forward by consuming a fixed chunk of completion events
            collector.resumableRun(RE_EVALUATION_BLOCK);

            List<Double> series = collector.getSeries();
            int n = series.size();
            int k = n / b; // Re-calculate number of batches (k drops when b doubles, rises as n grows)

            // Enforce minimum batch size guidelines to avoid small-sample variance instability [cite: 227, 228]
            if (k < MIN_BATCHES_THRESHOLD) {
                continue;
            }

            List<Double> batchMeans = new ArrayList<>();
            Welford welford = new Welford();

            // Construct temporary batch arrays on current accumulated memory
            for (int i = 0; i < k; i++) {
                double batchSum = 0.0;
                for (int j = 0; j < b; j++) {
                    batchSum += series.get(i * b + j);
                }
                double bMean = batchSum / b;
                batchMeans.add(bMean);
                welford.update(bMean);
            }

            // Evaluate stochastic independence using Lag-1 ACF on batch means
            double acf1 = AutoCorrelation.calculateACF(batchMeans, 1);
            boolean independent = Math.abs(acf1) < 0.05; // 0.05 absolute tolerance for uncorrelated luts

            // Evaluate precision convergence criterion (Half-Width / Mean <= epsilon)
            IntervalEstimator.IntervalResult currentResult = IntervalEstimator.estimate(welford, CONFIDENCE_LEVEL);
            double relativePrecision = currentResult.halfWidth() / currentResult.mean();
            boolean precise = relativePrecision < TARGET_PRECISION;

            // Keep refreshing structural data snapshot
            finalReport = new EstimationReport(currentResult, n, b, k, acf1);

            if (independent && precise) {
                logger.info("-> Convergence successfully reached!");
                logger.info("   Final dynamic state parameters: N = {}, Batch Size (b) = {}, Batches (k) = {}", n, b, k);
                logger.info(String.format("   Criteria validation: Lag-1 ACF = %.4f (< 0.05) | Rel. Precision = %.4f (< 0.05)", acf1, relativePrecision));
                converged = true;
            } else if (!independent) {
                // Serial correlation detected: trigger Batch Size Doubling to reduce lag influence
                logger.info(String.format("Lag-1 ACF = %.4f (>= 0.05). Independence failed. Doubling batch size to %d...", acf1, b * 2));
                b *= 2;
            } else {
                // Correlation passed but interval is too wide: fetch more data chunks from core
                logger.info(String.format("Relative Precision = %.4f (>= 0.05). Precision failed. Appending data chunks...", relativePrecision));
            }
        }

        // Gracefully finalize decorators, close streams, and flush records
        collector.finalizeSimulation();
        return finalReport;
    }

    /**
     * Logs the final standardized block.
     */
    private static void printReport(EstimationReport report) {
        logger.info("=======================================================================================================================");
        logger.info("                                                    FINAL ANALYSIS REPORT                                              ");
        logger.info("=======================================================================================================================");
        logger.info("{}", report);
        logger.info("=======================================================================================================================");
    }

    /**
     * Logs the final standardized comparison block.
     */
    private static void printComparativeReport(EstimationReport analytical, EstimationReport dynamic) {
        logger.info("=======================================================================================================================");
        logger.info("                                              FINAL COMPARATIVE ANALYSIS REPORT                                         ");
        logger.info("=======================================================================================================================");
        logger.info("Analytical : {}", analytical);
        logger.info("Dynamic    : {}", dynamic);
        logger.info("=======================================================================================================================");
    }
}
