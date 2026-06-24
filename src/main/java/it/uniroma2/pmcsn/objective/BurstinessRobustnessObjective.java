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
 * Objective 4.3: Robustness to Burstiness
 * 
 * Verify if the system maintains SLA under varying levels of traffic burstiness
 * and increasing load using default full-scaling config.
 */
public class BurstinessRobustnessObjective extends BaseObjective {

    /**
     * Initializes the burstiness robustness objective.
     */
    public BurstinessRobustnessObjective() {
        super(BurstinessRobustnessObjective.class, "OBJ3.3");
    }

    /**
     * Main entry point for Objective 4.3.
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
                        ApplicationConfig.SI_MAX, ApplicationConfig.SI_LOW,
                        ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, ApplicationConfig.TRACE_PATH
                ),
                new ClusterConfig(),
                new ScalingConfig(),
                ApplicationConfig.ExecutionConfig.batchRun(tunedBatchNums, tunedBatchSize, tunedWarmupJobs)
        );

        new BurstinessRobustnessObjective().start(config);
    }

    /**
     * Executes the burstiness robustness analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Burstiness Robustness Objective");
        
        double[] burstinessLevels = {1.0, 4.0, 8.0, 12.0};

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder(buildCsvHeader());

        report.append("\nBurstiness Robustness Report\n");
        report.append(String.format("%-10s | %-10s | %-12s | %-12s | %-12s\n", "CV", "Lambda", "R0", "StdDev", "HW"));
        report.append("----------------------------------------------------------------------------\n");

        for (double cv : burstinessLevels) {
            for (double lambda = 2.0; lambda <= 15.0; lambda += 1.5) {

                LoadConfig loadConfig = new LoadConfig(
                    1.0 / lambda,
                    cv, 
                    config.load().meanService(),
                    config.load().cvService(),
                    config.load().siMax(),
                    config.load().siLow(),
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
                
                IntervalResult r0 = results.responseTime();

                report.append(String.format(Locale.US, "%-10.1f | %-10.1f | %-12.4f | %-12.4f | %-12.4f\n",
                        cv, lambda, r0.mean(), r0.stdDev(), r0.halfWidth()));
                csv.append(buildCsvRow(cv, lambda, results));
            }
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("burstiness/burstiness_robustness.csv", csv.toString());
        try {
            ObjectiveChartUtility.regenerateBurstinessCharts(
                    "data/objective/burstiness/burstiness_robustness.csv",
                    "data/objective/burstiness/burstiness_robustness.png"
            );
        } catch (java.io.IOException e) {
            logger.error("Failed to generate burstiness charts", e);
        }
    }

    private static String buildCsvHeader() {
        StringBuilder header = new StringBuilder("CV_Interarrival,Arrival_Rate");
        appendMetricHeader(header, "response_time");
        appendMetricHeader(header, "jobs_in_system");
        appendMetricHeader(header, "system_utilization");
        appendMetricHeader(header, "throughput");
        appendMetricHeader(header, "diverted_jobs");
        appendMetricHeader(header, "avg_servers");
        appendMetricHeader(header, "scale_out_actions");
        appendMetricHeader(header, "scale_in_actions");
        appendMetricHeader(header, "scale_up_actions");
        appendMetricHeader(header, "scale_down_actions");
        appendMetricHeader(header, "spike_avg_speed");
        appendMetricHeader(header, "spike_utilization");
        header.append('\n');
        return header.toString();
    }

    private static void appendMetricHeader(StringBuilder header, String metricName) {
        header.append(',')
                .append(metricName).append("_mean,")
                .append(metricName).append("_std_dev,")
                .append(metricName).append("_hw,")
                .append(metricName).append("_ci_low,")
                .append(metricName).append("_ci_high");
    }

    private static String buildCsvRow(double cv, double lambda, AggregatedResults results) {
        StringBuilder row = new StringBuilder();
        row.append(String.format(Locale.US, "%.1f,%.1f", cv, lambda));
        appendMetric(row, results.responseTime());
        appendMetric(row, results.jobsInSystem());
        appendMetric(row, results.utilization());
        appendMetric(row, results.throughput());
        appendMetric(row, results.divertedJobs());
        appendMetric(row, results.avgServers());
        appendMetric(row, results.scaleOutActions());
        appendMetric(row, results.scaleInActions());
        appendMetric(row, results.scaleUpActions());
        appendMetric(row, results.scaleDownActions());
        appendMetric(row, results.spikeAvgSpeed());
        appendMetric(row, results.spikeUtilization());
        row.append('\n');
        return row.toString();
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
