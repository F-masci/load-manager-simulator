package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.data.ConvergenceCheckpointCollector;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator;
import it.uniroma2.pmcsn.lib.statistics.Welford;
import it.uniroma2.pmcsn.utils.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Support class for convergence analysis of a fixed simulation configuration.
 * Unlike {@link BatchMeanEstimator}, this class does not search for batch
 * parameters: it runs long finite-horizon independent replications, samples the
 * cumulative response-time curve at common checkpoints, and estimates where the
 * curves stabilize across replications.
 */
public class ConvergenceEstimator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ConvergenceEstimator.class, "SIM");

    // Convergence analysis configuration
    private static final int NUM_REPLICATIONS = 10;
    private static final int INITIAL_JOBS = 10_000;
    private static final int JOB_INCREMENT = INITIAL_JOBS;
    private static final int INCREMENT_GROWTH_EVERY_STEPS = 10;
    private static final int INCREMENT_GROWTH_FACTOR = 2;
    private static final int MAX_JOBS = 1_000_000;
    private static final int REQUIRED_STABLE_WINDOWS = 5;

    // Statistical thresholds
    private static final double RESPONSE_TIME_RELATIVE_TOLERANCE = 0.01;
    private static final double RESPONSE_TIME_RELATIVE_PRECISION = 0.05;
    private static final double RESPONSE_TIME_DIVERGENCE_TOLERANCE = 0.02;
    private static final int REQUIRED_DIVERGING_WINDOWS = 5;

    public enum ConvergenceStatus {
        CONVERGED,
        DIVERGING,
        INCONCLUSIVE
    }

    /**
     * Convergence snapshot observed at a specific simulation horizon.
     *
     * @param numReplications number of independent replications used at the horizon
     * @param completedJobs finite-horizon job count per replication
     * @param estimatedTime estimated simulation time at the current horizon
     * @param responseTime response-time confidence interval
     * @param throughput throughput confidence interval
     * @param relativeChange response-time relative change from previous horizon
     * @param relativeHalfWidth response-time relative half-width
     * @param stableWindows number of consecutive stable windows
     * @param divergingWindows number of consecutive worsening windows
     */
    public record ConvergencePoint(
            int numReplications,
            int completedJobs,
            double estimatedTime,
            IntervalEstimator.IntervalResult responseTime,
            IntervalEstimator.IntervalResult throughput,
            double relativeChange,
            double relativeHalfWidth,
            int stableWindows,
            int divergingWindows
    ) {
        @Override
        @NotNull
        public String toString() {
            return String.format(
                    "replications = %d, jobs = %d, time ~= %.4f, " +
                            "R0 = %.4f, HW = %.4f, rel.change = %.5f, rel.HW = %.5f, stable windows = %d, diverging windows = %d",
                    numReplications,
                    completedJobs,
                    estimatedTime,
                    responseTime.mean(),
                    responseTime.halfWidth(),
                    relativeChange,
                    relativeHalfWidth,
                    stableWindows,
                    divergingWindows
            );
        }
    }

    /**
     * Final convergence report.
     *
     * @param status convergence classification
     * @param point final point, or first converged point when available
     */
    public record ConvergenceReport(ConvergenceStatus status, ConvergencePoint point) {
        public boolean converged() {
            return status == ConvergenceStatus.CONVERGED;
        }

        public boolean diverging() {
            return status == ConvergenceStatus.DIVERGING;
        }

        @Override
        @NotNull
        public String toString() {
            if (point == null) {
                return "No convergence point available.";
            }

            return String.format("%s | %s", status, point);
        }
    }

    public static void main(String[] args) {
        ApplicationConfig config = new ApplicationConfig();
        run(config);
    }

    /**
     * Runs convergence analysis on the provided configuration.
     *
     * @param config target simulation configuration
     * @return convergence report
     */
    public static ConvergenceReport run(ApplicationConfig config) {
        logger.info("Starting long-run convergence estimation analysis...");
        logger.info("Independent replications = {}, initial jobs = {}, initial job increment = {}, max jobs = {}",
                NUM_REPLICATIONS, INITIAL_JOBS, JOB_INCREMENT, MAX_JOBS);
        logger.info("Increment schedule: grow by factor {} every {} evaluated horizons",
                INCREMENT_GROWTH_FACTOR, INCREMENT_GROWTH_EVERY_STEPS);
        logger.info("Criteria: relative change <= {} for {} consecutive windows and relative HW <= {}",
                RESPONSE_TIME_RELATIVE_TOLERANCE, REQUIRED_STABLE_WINDOWS, RESPONSE_TIME_RELATIVE_PRECISION);
        logger.info("Divergence rule: relative growth >= {} with non-overlapping worsening CI for {} consecutive windows",
                RESPONSE_TIME_DIVERGENCE_TOLERANCE, REQUIRED_DIVERGING_WINDOWS);

        int[] checkpoints = buildCheckpoints();
        List<ReplicationTrace> traces = collectLongRunTraces(config, checkpoints);

        ConvergencePoint lastPoint = null;
        double previousResponseTime = Double.NaN;
        int stableWindows = 0;
        int divergingWindows = 0;

        for (int checkpointIndex = 0; checkpointIndex < checkpoints.length; checkpointIndex++) {
            ConvergencePoint currentPoint = buildPoint(checkpoints[checkpointIndex], checkpointIndex, traces);
            double responseTime = currentPoint.responseTime().mean();
            double relativeChange = calculateRelativeChange(previousResponseTime, responseTime);
            double relativeHalfWidth = calculateRelativeHalfWidth(currentPoint.responseTime());

            boolean stableMean = !Double.isNaN(relativeChange)
                    && relativeChange <= RESPONSE_TIME_RELATIVE_TOLERANCE;
            boolean preciseEnough = relativeHalfWidth <= RESPONSE_TIME_RELATIVE_PRECISION;
            boolean worseningMean = isStatisticallyWorse(currentPoint.responseTime(), lastPoint)
                    && relativeChange >= RESPONSE_TIME_DIVERGENCE_TOLERANCE;

            if (stableMean && preciseEnough) {
                stableWindows++;
            } else {
                stableWindows = 0;
            }

            if (worseningMean) {
                divergingWindows++;
            } else {
                divergingWindows = 0;
            }

            lastPoint = new ConvergencePoint(
                    currentPoint.numReplications(),
                    currentPoint.completedJobs(),
                    currentPoint.estimatedTime(),
                    currentPoint.responseTime(),
                    currentPoint.throughput(),
                    relativeChange,
                    relativeHalfWidth,
                    stableWindows,
                    divergingWindows
            );

            logger.info("{}", lastPoint);

            if (stableWindows >= REQUIRED_STABLE_WINDOWS) {
                ConvergenceReport report = new ConvergenceReport(ConvergenceStatus.CONVERGED, lastPoint);
                printFinalReport(report);
                return report;
            }

            if (divergingWindows >= REQUIRED_DIVERGING_WINDOWS) {
                ConvergenceReport report = new ConvergenceReport(ConvergenceStatus.DIVERGING, lastPoint);
                printFinalReport(report);
                return report;
            }

            previousResponseTime = responseTime;
        }

        ConvergenceReport report = new ConvergenceReport(ConvergenceStatus.INCONCLUSIVE, lastPoint);
        printFinalReport(report);
        return report;
    }

    private static int[] buildCheckpoints() {
        List<Integer> checkpoints = new ArrayList<>();
        int completedJobs = INITIAL_JOBS;
        int jobIncrement = JOB_INCREMENT;
        int evaluatedHorizons = 0;

        while (completedJobs <= MAX_JOBS) {
            checkpoints.add(completedJobs);
            evaluatedHorizons++;

            if (evaluatedHorizons % INCREMENT_GROWTH_EVERY_STEPS == 0) {
                jobIncrement *= INCREMENT_GROWTH_FACTOR;
            }

            completedJobs += jobIncrement;
        }

        return checkpoints.stream().mapToInt(Integer::intValue).toArray();
    }

    private static List<ReplicationTrace> collectLongRunTraces(ApplicationConfig config, int[] checkpoints) {
        ConvergenceCheckpointCollector.configure(checkpoints);

        ApplicationConfig convergenceConfig = new ApplicationConfig(
                config.load(),
                config.cluster(),
                config.scaling(),
                new ApplicationConfig.ExecutionConfig(
                        SimulationMethod.INDEPENDENT_REPLICATIONS,
                        config.execution().seed(),
                        NUM_REPLICATIONS,
                        ApplicationConfig.MAX_TIME,
                        MAX_JOBS,
                        0,
                        0,
                        0
                ),
                config.logging()
        );

        SimulationFacade facade = new SimulationFacade(convergenceConfig);
        facade.setCustomDecorator(ConvergenceCheckpointCollector.class);
        facade.runSimulation();

        List<ReplicationTrace> traces = new ArrayList<>();
        for (SimulatorDecorator decorator : facade.getCustomDecorators()) {
            if (decorator instanceof ConvergenceCheckpointCollector collector) {
                traces.add(new ReplicationTrace(
                        collector.getResponseTimes(),
                        collector.getThroughputs(),
                        collector.getClocks()
                ));
            }
        }

        if (traces.isEmpty()) {
            throw new IllegalStateException("No convergence checkpoint collectors were created by SimulationFacade.");
        }

        return traces;
    }

    private static ConvergencePoint buildPoint(int completedJobs, int checkpointIndex, List<ReplicationTrace> traces) {
        Welford responseTimeStats = new Welford();
        Welford throughputStats = new Welford();
        Welford clockStats = new Welford();

        for (ReplicationTrace trace : traces) {
            responseTimeStats.update(trace.responseTimes[checkpointIndex]);
            throughputStats.update(trace.throughputs[checkpointIndex]);
            clockStats.update(trace.clocks[checkpointIndex]);
        }

        return new ConvergencePoint(
                traces.size(),
                completedJobs,
                clockStats.getMean(),
                IntervalEstimator.estimate(responseTimeStats, 0.95),
                IntervalEstimator.estimate(throughputStats, 0.95),
                Double.NaN,
                Double.NaN,
                0,
                0
        );
    }

    private static double calculateRelativeChange(double previous, double current) {
        if (Double.isNaN(previous) || previous == 0.0) {
            return Double.NaN;
        }
        return Math.abs(current - previous) / Math.abs(previous);
    }

    private static double calculateRelativeHalfWidth(IntervalEstimator.IntervalResult result) {
        if (result.mean() == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs(result.halfWidth() / result.mean());
    }

    private static boolean isStatisticallyWorse(IntervalEstimator.IntervalResult current, ConvergencePoint previousPoint) {
        if (previousPoint == null) {
            return false;
        }

        IntervalEstimator.IntervalResult previous = previousPoint.responseTime();
        return current.mean() > previous.mean()
                && current.lowerBound() > previous.upperBound();
    }

    private static void printFinalReport(ConvergenceReport report) {
        logger.info("=======================================================================================================================");
        logger.info("                                                CONVERGENCE ANALYSIS REPORT                                           ");
        logger.info("=======================================================================================================================");
        logger.info("{}", report);
        if (report.point() != null) {
            if (report.converged()) {
                logger.info("Convergence jobs: {}", report.point().completedJobs());
                logger.info("Convergence time: {}", report.point().estimatedTime());
            } else {
                logger.info("Last observed jobs: {}", report.point().completedJobs());
                logger.info("Last observed time: {}", report.point().estimatedTime());
            }
        }
        logger.info("=======================================================================================================================");
    }

    private static class ReplicationTrace {
        private final double[] responseTimes;
        private final double[] throughputs;
        private final double[] clocks;

        private ReplicationTrace(double[] responseTimes, double[] throughputs, double[] clocks) {
            this.responseTimes = responseTimes;
            this.throughputs = throughputs;
            this.clocks = clocks;
        }
    }
}
