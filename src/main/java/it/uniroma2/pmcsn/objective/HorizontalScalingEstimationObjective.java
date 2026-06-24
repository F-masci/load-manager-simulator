package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.ConvergenceEstimator;
import it.uniroma2.pmcsn.ConvergenceEstimator.ConvergenceReport;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ExecutionConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objective 1.4: Horizontal Scaler Parameter Estimation
 * Estimate optimal thresholds for horizontal scaling by analyzing fixed configurations.
 * Compares 1, 2, 3, 4, 5 Web Servers across different arrival rates.
 * Scalers are DISABLED to identify physical limits.
 */
public class HorizontalScalingEstimationObjective extends BaseObjective {

    /**
     * Initializes the horizontal scaling parameter estimation objective.
     */
    public HorizontalScalingEstimationObjective() {
        super(HorizontalScalingEstimationObjective.class, "OBJ1.4");
    }

    /**
     * Main entry point for Objective 1.4.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        LoadConfig baseLoad = new LoadConfig(
                0.0, ApplicationConfig.CV_INTERARRIVAL,
                ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                ApplicationConfig.SI_MAX, ApplicationConfig.SI_LOW,
                ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, null
        );

        ApplicationConfig config = new ApplicationConfig(
                baseLoad,
                new ClusterConfig(),
                ScalingConfig.disabled(),
                new ApplicationConfig.ExecutionConfig()
        );

        new HorizontalScalingEstimationObjective().start(config);
    }

    /**
     * Executes the horizontal scaling parameter estimation analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Horizontal Scaling Parameter Estimation Objective...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Arrival_CV,Lambda,Num_Servers,R0_Mean,R0_HW,Convergence_Status,Convergence_Jobs,Convergence_Time,Batch_Warmup_Jobs\n");
        Map<Double, Map<Integer, XYSeries>> cvResults = new LinkedHashMap<>();

        report.append("\nHorizontal Scaling Parameter Estimation Report\n");
        report.append("------------------------------------------------------------------------------------\n");

        Map<Integer, XYSeries> wsSeriesMap = new LinkedHashMap<>();
        cvResults.put(ApplicationConfig.CV_INTERARRIVAL, wsSeriesMap);

        final int[] tunedBatchSize = {8_192, 16_384, 4_096, 2_048, 512};
        final int[] tunedBatchNums = {1_024, 512, 4_096, 8_192, 16_384};

        for (int n = 1; n <= 5; n++) {
            XYSeries series = new XYSeries(n + " WS");
            XYSeries divergingSeries = new XYSeries(n + " WS - DIVERGING");
            XYSeries inconclusiveSeries = new XYSeries(n + " WS - INCONCLUSIVE");
            wsSeriesMap.put(n, series);
            wsSeriesMap.put(-n, divergingSeries);
            wsSeriesMap.put(-100 - n, inconclusiveSeries);

            final int batchSize  = tunedBatchSize[n-1];
            final int batchNums  = tunedBatchNums[n-1];
            boolean skipRemainingLambdas = false;
            String skipReason = null;

            for (double lambda = 2.0; lambda <= 12.0; lambda += 1.5) {
                LoadConfig loadConfig = config.load();
                ApplicationConfig convergenceConfig = new ApplicationConfig(
                        new LoadConfig(
                                loadConfig.workloadType(),
                                1.0 / lambda, loadConfig.cvInterarrival(),
                                loadConfig.meanService(), loadConfig.cvService(),
                                loadConfig.routingPolicy(), loadConfig.siMax()
                        ),
                        ClusterConfig.fixedServer(n, true),
                        config.scaling(),
                        config.execution()
                );

                if (skipRemainingLambdas) {
                    logger.info("Skipping lambda = {}, servers = {} because a lower lambda already failed convergence: {}",
                            lambda, n, skipReason);
                    if ("DIVERGING".equals(skipReason)) {
                        divergingSeries.add(lambda, ApplicationConfig.SLA_THRESHOLD * 6.0);
                    } else {
                        inconclusiveSeries.add(lambda, ApplicationConfig.SLA_THRESHOLD * 6.0);
                    }
                    csv.append(String.format("%.1f,%.2f,%d,NaN,NaN,%s,0,NaN,0\n",
                            ApplicationConfig.CV_INTERARRIVAL, lambda, n, "SKIPPED_AFTER_" + skipReason));
                    continue;
                }

                ConvergenceReport convergenceReport = ConvergenceEstimator.run(convergenceConfig);
                int convergenceJobs = convergenceReport.point() == null ? 0 : convergenceReport.point().completedJobs();
                double convergenceTime = convergenceReport.point() == null ? Double.NaN : convergenceReport.point().estimatedTime();

                if (!convergenceReport.converged()) {
                    logger.info("Skipping Batch Means for lambda = {}, servers = {}: {}", lambda, n, convergenceReport.status());
                    skipRemainingLambdas = true;
                    skipReason = convergenceReport.status().name();
                    if (convergenceReport.diverging()) {
                        divergingSeries.add(lambda, ApplicationConfig.SLA_THRESHOLD * 6.0);
                    } else {
                        inconclusiveSeries.add(lambda, ApplicationConfig.SLA_THRESHOLD * 6.0);
                    }
                    csv.append(String.format("%.1f,%.2f,%d,NaN,NaN,%s,%d,%.4f,0\n",
                            ApplicationConfig.CV_INTERARRIVAL, lambda, n,
                            convergenceReport.status(), convergenceJobs, convergenceTime));
                    continue;
                }

                final int warmupJobs = 5 * convergenceJobs;
                ExecutionConfig executionConfig = ExecutionConfig.batchRun(batchNums, batchSize, warmupJobs);
                ApplicationConfig currentConfig = new ApplicationConfig(
                        convergenceConfig.load(),
                        convergenceConfig.cluster(),
                        convergenceConfig.scaling(),
                        executionConfig
                );

                SimulationFacade facade = new SimulationFacade(currentConfig);
                AggregatedResults results = facade.runSimulation();

                double r0 = results.responseTime().mean();
                series.add(lambda, r0);

                csv.append(String.format("%.1f,%.2f,%d,%.4f,%.4f,%s,%d,%.4f,%d\n",
                        ApplicationConfig.CV_INTERARRIVAL, lambda, n, r0, results.responseTime().halfWidth(),
                        convergenceReport.status(), convergenceJobs, convergenceTime, warmupJobs));
            }
        }
        logger.info("Completed analysis for Arrival CV = {}", ApplicationConfig.CV_INTERARRIVAL);

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("horizontal_parameter_estimation.csv", csv.toString());
        ObjectiveChartUtility.generateHorizontalParameterEstimationChart(
                cvResults, "data/objective/horizontal_parameter_estimation.png", ApplicationConfig.SLA_THRESHOLD);
    }
}

