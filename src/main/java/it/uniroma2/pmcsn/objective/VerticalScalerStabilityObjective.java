package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.lib.statistics.IntervalEstimator.IntervalResult;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;

import java.util.Locale;

/**
 * Objective 4.1: Vertical Scaler Stability Analysis
 * Analyzes the impact of different bands (siMax - siLow) on system stability
 * (measured by spike server utilization) and performance across varying arrival rates.
 */
public class VerticalScalerStabilityObjective extends BaseObjective {

    /**
     * Initializes the vertical scaler stability objective.
     */
    public VerticalScalerStabilityObjective() {
        super(VerticalScalerStabilityObjective.class, "OBJ4.1");
    }

    /**
     * Main entry point for Objective 4.1.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        final int tunedBatchSize  = 4_096;
        final int tunedBatchNums  = 1_024;
        final int tunedWarmupJobs = 5 * 100000;

        ApplicationConfig config = new ApplicationConfig(
                new LoadConfig(
                        0.0, 0.0,
                        ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                        ApplicationConfig.SI_MAX, 0,
                        ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, ApplicationConfig.TRACE_PATH
                ),
                new ClusterConfig(),
                new ScalingConfig(),
                ApplicationConfig.ExecutionConfig.batchRun(tunedBatchNums, tunedBatchSize, tunedWarmupJobs)
        );

        new VerticalScalerStabilityObjective().start(config);
    }

    /**
     * Executes the vertical scaler stability analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Vertical Scaler Stability Objective...");

        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("data/objective/band"));
        } catch (java.io.IOException e) {
            logger.error("Could not create band objective directory");
        }

        int siMax = config.load().siMax();
        int[] bands = {0, 5, 10, 20};
        double[] cvs = {1.0, 4.0, 8.0, 12.0};

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Band,CV,Lambda");
        appendMetricHeader(csv, "spike_utilization");
        appendMetricHeader(csv, "system_utilization");
        appendMetricHeader(csv, "response_time");
        csv.append('\n');

        report.append("\nVertical Scaler Stability Report\n");
        report.append(String.format("%-15s | %-5s | %-10s | %-20s | %-20s | %-10s\n", "Band", "CV", "Lambda", "Spike Utilization", "System Utilization", "R0"));
        report.append("--------------------------------------------------------------------------------------------------------\n");

        for (int band : bands) {
            int siLow = Math.max(0, siMax - band);

            for (double cv : cvs) {
                for (double lambda = 2.0; lambda <= 15.0; lambda += 1.5) {
                    LoadConfig loadConfig = new LoadConfig(
                            1.0 / lambda,
                            cv,
                            config.load().meanService(),
                            config.load().cvService(),
                            config.load().siMax(),
                            siLow,
                            config.load().routingPolicy(),
                            config.load().workloadType(),
                            config.load().tracePath()
                    );

                    // Use default full-system scaling configurations
                    ApplicationConfig objectiveConfig = new ApplicationConfig(
                            loadConfig,
                            new ClusterConfig(),
                            new ScalingConfig(),
                            config.execution(),
                            config.logging()
                    );

                    SimulationFacade facade = new SimulationFacade(objectiveConfig);
                    AggregatedResults results = facade.runSimulation();

                    double spikeUtil = results.spikeUtilization().mean();
                    double systemUtil = results.utilization().mean();
                    double r0 = results.responseTime().mean();

                    report.append(String.format("%-15d | %-5.1f | %-10.1f | %-20.4f | %-20.4f | %-10.4f\n",
                            band, cv, lambda, spikeUtil, systemUtil, r0));
                    csv.append(String.format(Locale.US, "%d,%.1f,%.1f", band, cv, lambda));
                    appendMetric(csv, results.spikeUtilization());
                    appendMetric(csv, results.utilization());
                    appendMetric(csv, results.responseTime());
                    csv.append('\n');
                }
            }
            logger.info("Completed analysis for Band = {}", band);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("band/vertical_scaler_stability.csv", csv.toString());
        try {
            ObjectiveChartUtility.regenerateVerticalStabilityChart(
                    "data/objective/band/vertical_scaler_stability.csv",
                    "data/objective/band/vertical_scaler_stability.png"
            );
        } catch (java.io.IOException e) {
            logger.error("Could not generate vertical scaler stability charts", e);
        }
    }

    private static void appendMetricHeader(StringBuilder header, String metricName) {
        header.append(',')
                .append(metricName).append("_mean,")
                .append(metricName).append("_std_dev,")
                .append(metricName).append("_hw,")
                .append(metricName).append("_ci_low,")
                .append(metricName).append("_ci_high");
    }

    private static void appendMetric(StringBuilder row, IntervalResult result) {
        if (result == null) {
            row.append(",NaN,NaN,NaN,NaN,NaN");
            return;
        }
        row.append(String.format(Locale.US, ",%.6f,%.6f,%.6f,%.6f,%.6f",
                result.mean(),
                result.stdDev(),
                result.halfWidth(),
                result.lowerBound(),
                result.upperBound()));
    }
}

